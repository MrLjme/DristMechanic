package com.dristmechanic.dristmechanic.handler;

import com.dristmechanic.dristmechanic.Config;
import com.dristmechanic.dristmechanic.Dristmechanic;
import com.dristmechanic.dristmechanic.entity.*;
import com.dristmechanic.dristmechanic.network.RaidDataPayload;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadLocalRandom;

import static software.bernie.geckolib.GeckoLibConstants.LOGGER;

@EventBusSubscriber(modid = Dristmechanic.MODID)
public class RaidManager {

    // Only mob tracking remains global (maps mob UUID to farm owner)
    private static final Map<UUID, FarmOwner> mobToFarmMap = new ConcurrentHashMap<>();
    private static final Map<ServerLevel, List<RaidSpawnTask>> activeSpawnTasks = new ConcurrentHashMap<>();

    private record FarmOwner(ServerLevel level, ChunkPos farmMainChunk) {}

    public static void syncFarmData(ServerLevel level, FarmManager.FarmData farm, int guiValue) {
        boolean hasValue = guiValue > 0;
        boolean raidActive = farm.isRaidActive();
        int maxValue = raidActive ? farm.getOriginalFarmValue() : Config.MAX_CROP_VALUE.get();
        syncToClients(level, farm, hasValue, guiValue, maxValue, raidActive);
    }

    public static void onRaidScheduled(ServerLevel level, FarmManager.FarmData farm, int value) {
        farm.setOriginalFarmValue(value);
        syncToClients(level, farm, false, value, Config.MAX_CROP_VALUE.get(), false);
        FarmManager.getData(level).setDirty();
    }

    public static void onRaidComplete(ServerLevel level, FarmManager.FarmData farm) {
        farm.resetRaidValues();
        syncToClients(level, farm, false, 0, Config.MAX_CROP_VALUE.get(), false);
        FarmManager.getData(level).setDirty();
    }

    public static int getDisplayValue(FarmManager.FarmData farm) {
        int currentRaidValue = farm.getCurrentRaidValue();
        int originalValue = farm.getOriginalFarmValue();
        int totalRaidValue = farm.getTotalRaidValue();

        if (currentRaidValue <= 0) return 0;
        if (totalRaidValue > 0 && originalValue > 0) {
            double ratio = (double) currentRaidValue / totalRaidValue;
            int displayValue = (int) Math.ceil(originalValue * ratio);
            return Math.max(1, displayValue);
        }
        return Math.max(1, currentRaidValue);
    }

    @SubscribeEvent
    public static void onMobDeath(LivingDeathEvent event) {
        if (event.getEntity().level().isClientSide()) return;
        ServerLevel level = (ServerLevel) event.getEntity().level();
        UUID mobUUID = event.getEntity().getUUID();
        FarmOwner owner = mobToFarmMap.remove(mobUUID);
        if (owner == null) return;

        FarmManager.FarmData farm = FarmManager.getData(level).getFarmByMainChunk(owner.farmMainChunk());
        if (farm == null) return;

        farm.removeSpawnedMob(mobUUID);
        int mobValue = calculateMobValue(event.getEntity());
        int current = farm.getCurrentRaidValue() - mobValue;
        current = Math.max(0, current);
        farm.setCurrentRaidValue(current);

        int displayValue = getDisplayValue(farm);
        syncToClients(level, farm, false, displayValue, Config.MAX_CROP_VALUE.get(), farm.isRaidActive());
        FarmManager.getData(level).setDirty();
    }

    private static int calculateMobValue(net.minecraft.world.entity.Entity mob) {
        if (mob instanceof FarmbotEntity) return 100;
        if (mob instanceof HaybotEntity) return 20;
        if (mob instanceof RedTapebotEntity) return 15;
        if (mob instanceof TapebotEntity) return 10;
        if (mob instanceof TotebotEntity) return 5;
        return 1;
    }

    private static int calculateMobValue(Enemy enemy) {
        if (enemy.entityId.contains("farmbot")) return 100;
        if (enemy.entityId.contains("haybot")) return 20;
        if (enemy.entityId.contains("red_tapebot")) return 15;
        if (enemy.entityId.contains("tapebot")) return 10;
        if (enemy.entityId.contains("totebot")) return 5;
        return 1;
    }

    public static List<BlockPos> findRaidSpawnPointsClose(ServerLevel level, Vec3 center, int max) {
        List<BlockPos> res = new ArrayList<>();
        ThreadLocalRandom rnd = ThreadLocalRandom.current();
        for (int i = 0; i < max; i++) {
            double angle = rnd.nextDouble() * 2 * Math.PI;
            double distance = rnd.nextDouble(8, 20);
            int tX = (int)(center.x + Math.cos(angle) * distance);
            int tZ = (int)(center.z + Math.sin(angle) * distance);
            BlockPos sp = findClosestValidSpawn(level, tX, tZ, (int)center.y);
            if (sp != null) res.add(sp);
        }
        return res;
    }

    public static void executeRaid(ServerLevel level, FarmManager.FarmData farm, int farmValue, Vec3 farmCenter) {
        Vec3 center = farmCenter != null ? farmCenter : new Vec3(0, 64, 0);
        int raidLevel = getRaidLevel(farmValue);
        int playerCount = level.players().size();
        int budget = calculateBudget(raidLevel, farmValue, playerCount);

        LOGGER.info("[EXECUTE_RAID] farmValue={}, raidLevel={}, playerCount={}, budget={}, center={}",
                farmValue, raidLevel, playerCount, budget, center);

        SpawnGroup guaranteedGroup = getGuaranteedSpawn(raidLevel);
        List<SpawnGroup> budgetGroups = selectBudgetGroups(budget, raidLevel);
        List<Enemy> allGeneratedEnemies = new ArrayList<>();

        if (guaranteedGroup != null) {
            allGeneratedEnemies.addAll(guaranteedGroup.enemies);
        } else {
            LOGGER.warn("[EXECUTE_RAID] No guaranteed group for level {}", raidLevel);
        }
        for (SpawnGroup bg : budgetGroups) {
            allGeneratedEnemies.addAll(bg.enemies);
        }
        if (allGeneratedEnemies.isEmpty()) {
            LOGGER.warn("[EXECUTE_RAID] No enemies generated, aborting");
            return;
        }

        Map<String, Integer> enemyCounts = new LinkedHashMap<>();
        for (Enemy e : allGeneratedEnemies) {
            enemyCounts.merge(e.entityId, e.qty, Integer::sum);
        }

        int totalMobs = enemyCounts.values().stream().mapToInt(Integer::intValue).sum();
        int mobsPerWave = Math.max(1, totalMobs / 3);

        List<SpawnGroup> waves = new ArrayList<>();
        List<Enemy> currentWave = new ArrayList<>();
        int currentWaveSize = 0;

        for (Map.Entry<String, Integer> entry : enemyCounts.entrySet()) {
            String entityId = entry.getKey();
            int remaining = entry.getValue();
            while (remaining > 0) {
                int space = mobsPerWave - currentWaveSize;
                int toAdd = Math.min(remaining, space);
                if (toAdd > 0) {
                    Enemy existing = currentWave.stream()
                            .filter(e -> e.entityId.equals(entityId))
                            .findFirst().orElse(null);
                    if (existing != null) {
                        existing.qty += toAdd;
                    } else {
                        currentWave.add(new Enemy(entityId, toAdd));
                    }
                    currentWaveSize += toAdd;
                    remaining -= toAdd;
                }
                if (currentWaveSize >= mobsPerWave) {
                    waves.add(new SpawnGroup(new ArrayList<>(currentWave), 0, 0));
                    currentWave.clear();
                    currentWaveSize = 0;
                }
            }
        }
        if (!currentWave.isEmpty()) {
            waves.add(new SpawnGroup(currentWave, 0, 0));
        }

        int totalRaidValue = 0;
        for (SpawnGroup wave : waves) {
            for (Enemy enemy : wave.enemies) {
                totalRaidValue += calculateMobValue(enemy) * enemy.qty;
            }
        }

        farm.setCurrentRaidValue(totalRaidValue);
        farm.setTotalRaidValue(totalRaidValue);

        LOGGER.info("[EXECUTE_RAID] Total raid value: {}, waves: {}", totalRaidValue, waves.size());

        int desiredSpawnPoints = Math.max(15, Math.min(60, totalMobs / 3));
        List<BlockPos> spawnPoints = findRaidSpawnPoints(level, center, desiredSpawnPoints);
        if (spawnPoints.isEmpty()) {
            spawnPoints = findRaidSpawnPointsClose(level, center, desiredSpawnPoints);
        }

        RaidSpawnTask task = new RaidSpawnTask();
        task.spawnGroups = waves;
        task.spawnPoints = spawnPoints;
        task.center = center;
        task.spawnIndex = 0;
        task.nextSpawnTime = level.getGameTime();
        task.farmMainChunk = farm.getMainChunk();
        activeSpawnTasks.computeIfAbsent(level, k -> new ArrayList<>()).add(task);

        int originalValue = farm.getOriginalFarmValue();
        double scaleFactor = originalValue > 0 && totalRaidValue > 0
                ? (double) originalValue / totalRaidValue : 1.0;
        int displayValue = (int) Math.round(totalRaidValue * scaleFactor);
        syncToClients(level, farm, true, displayValue, Config.MAX_CROP_VALUE.get(), true);
        FarmManager.getData(level).setDirty();
    }

    public static List<BlockPos> findRaidSpawnPoints(ServerLevel level, Vec3 center, int max) {
        List<BlockPos> res = new ArrayList<>();
        ThreadLocalRandom rnd = ThreadLocalRandom.current();
        for (int i = 0; i < max; i++) {
            double angle = rnd.nextDouble() * 2 * Math.PI;
            double distance = rnd.nextDouble(32, 65);
            int tX = (int)(center.x + Math.cos(angle) * distance);
            int tZ = (int)(center.z + Math.sin(angle) * distance);
            BlockPos sp = findClosestValidSpawn(level, tX, tZ, (int)center.y);
            if (sp != null) res.add(sp);
        }
        return res;
    }

    private static BlockPos findClosestValidSpawn(ServerLevel level, int x, int z, int tY) {
        int minY = level.getMinBuildHeight();
        int maxY = level.getMaxBuildHeight();
        BlockPos.MutableBlockPos m = new BlockPos.MutableBlockPos();
        for (int o = 0; o < 48; o++) {
            int yU = tY + o;
            if (yU < maxY) {
                m.set(x, yU, z);
                if (isValidSpawn(level, m)) return m.immutable();
            }
            if (o > 0) {
                int yD = tY - o;
                if (yD >= minY) {
                    m.set(x, yD, z);
                    if (isValidSpawn(level, m)) return m.immutable();
                }
            }
        }
        return null;
    }

    private static boolean isValidSpawn(ServerLevel level, BlockPos p) {
        if (!level.isLoaded(p)) return false;
        BlockPos.MutableBlockPos m = new BlockPos.MutableBlockPos();
        for (int x = -1; x <= 1; x++)
            for (int y = 0; y <= 2; y++)
                for (int z = -1; z <= 1; z++) {
                    m.set(p.getX() + x, p.getY() + y, p.getZ() + z);
                    net.minecraft.world.level.block.state.BlockState s = level.getBlockState(m);
                    if (!s.getCollisionShape(level, m).isEmpty() || !s.getFluidState().isEmpty()) return false;
                }
        m.set(p.getX(), p.getY() - 1, p.getZ());
        return !level.getBlockState(m).getCollisionShape(level, m).isEmpty();
    }

    public static void syncToClients(ServerLevel level, FarmManager.FarmData farm,
                                     boolean active, int current, int max, boolean raidActive) {
        ChunkPos farmId = farm.getMainChunk();
        Vec3 center = farm.getFarmCenter();
        double cx = center != null ? center.x : farmId.getMiddleBlockX() + 0.5;
        double cz = center != null ? center.z : farmId.getMiddleBlockZ() + 0.5;
        int syncRangeSq = 160 * 160;

        for (ServerPlayer player : level.players()) {
            double dx = player.getX() - cx;
            double dz = player.getZ() - cz;
            if (dx * dx + dz * dz > syncRangeSq) continue;
            PacketDistributor.sendToPlayer(player, new RaidDataPayload(
                    farmId.x, farmId.z, cx, cz, active, current, max, raidActive));
        }
    }

    @SubscribeEvent
    public static void onServerTick(ServerTickEvent.Post event) {
        for (ServerLevel level : event.getServer().getAllLevels()) {
            tickSpawnTasks(level);
        }
    }

    private static void tickSpawnTasks(ServerLevel level) {
        List<RaidSpawnTask> tasks = activeSpawnTasks.get(level);
        if (tasks == null || tasks.isEmpty()) return;

        Iterator<RaidSpawnTask> it = tasks.iterator();
        long currentTime = level.getGameTime();
        while (it.hasNext()) {
            RaidSpawnTask task = it.next();
            if (currentTime >= task.nextSpawnTime) {
                if (task.spawnIndex < task.spawnGroups.size()) {
                    FarmManager.FarmData farm = FarmManager.getData(level)
                            .getFarmByMainChunk(task.farmMainChunk);
                    if (farm != null) {
                        SpawnGroup group = task.spawnGroups.get(task.spawnIndex);
                        spawnGroup(level, group, task.spawnPoints, task.center, farm);
                    }
                    task.spawnIndex++;
                    task.nextSpawnTime = currentTime + Config.SPAWN_INTERVAL_TICKS.get();
                } else {
                    it.remove();
                }
            }
        }
    }

    private static void spawnGroup(ServerLevel level, SpawnGroup group,
                                   List<BlockPos> spawnPoints, Vec3 center,
                                   FarmManager.FarmData farm) {
        if (spawnPoints.isEmpty()) return;
        ThreadLocalRandom rnd = ThreadLocalRandom.current();
        BlockPos centerPos = new BlockPos((int)center.x, (int)center.y, (int)center.z);

        for (Enemy enemy : group.enemies) {
            ResourceLocation location = ResourceLocation.tryParse(enemy.entityId);
            if (location == null) continue;
            EntityType<?> entityType = BuiltInRegistries.ENTITY_TYPE.getOptional(location).orElse(null);
            if (entityType == null) continue;

            for (int i = 0; i < enemy.qty; i++) {
                BlockPos pos = spawnPoints.get(rnd.nextInt(spawnPoints.size()));
                Mob mob = (Mob) entityType.create(level);
                if (mob == null) continue;

                mob.moveTo(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5, rnd.nextFloat() * 360F, 0);
                mob.setPersistenceRequired();
                UUID mobUUID = mob.getUUID();
                farm.addSpawnedMob(mobUUID);
                mobToFarmMap.put(mobUUID, new FarmOwner(level, farm.getMainChunk()));

                if (mob instanceof TotebotEntity totebot) totebot.setRaidTarget(centerPos);
                else if (mob instanceof FarmbotEntity farmbot) farmbot.setRaidTarget(centerPos);
                else if (mob instanceof HaybotEntity haybot) haybot.setRaidTarget(centerPos);
                else if (mob instanceof TapebotEntity tapebot) tapebot.setRaidTarget(centerPos);
                else if (mob instanceof RedTapebotEntity redTapebot) redTapebot.setRaidTarget(centerPos);

                level.addFreshEntity(mob);
            }
        }
        FarmManager.getData(level).setDirty();
    }

    private static int getRaidLevel(int farmValue) {
        List<? extends Integer> thresholds = Config.LEVEL_THRESHOLDS.get();
        int level = 0;
        for (int i = 0; i < thresholds.size(); i++) {
            if (farmValue >= thresholds.get(i)) level = i + 1;
            else break;
        }
        return Math.min(level, thresholds.size());
    }

    private static int calculateBudget(int levelIndex, int farmValue, int playerCount) {
        if (levelIndex < 1) return 0;
        List<? extends Integer> thresholds = Config.LEVEL_THRESHOLDS.get();
        List<? extends Integer> minBudgets = Config.MIN_BUDGET.get();
        List<? extends Integer> maxBudgets = Config.MAX_BUDGET.get();
        List<? extends Double> multipliers = Config.PLAYER_MULTIPLIERS.get();
        int maxCropValue = Config.MAX_CROP_VALUE.get();

        if (levelIndex > thresholds.size() || levelIndex > minBudgets.size() || levelIndex > maxBudgets.size())
            return 0;

        int currentMin = thresholds.get(levelIndex - 1);
        int nextMin = (levelIndex < thresholds.size()) ? thresholds.get(levelIndex) : maxCropValue;

        float progress = 1.0f;
        if (nextMin > currentMin) {
            progress = (float)(farmValue - currentMin) / (float)(nextMin - currentMin);
            progress = Math.max(0.0f, Math.min(1.0f, progress));
        }

        float multiplier = 1.0f;
        if (playerCount == 1 && multipliers.size() > 0) multiplier = multipliers.get(0).floatValue();
        else if (playerCount == 2 && multipliers.size() > 1) multiplier = multipliers.get(1).floatValue();
        else if (playerCount >= 3 && multipliers.size() > 2) multiplier = multipliers.get(2).floatValue();

        int min = minBudgets.get(levelIndex - 1);
        int max = maxBudgets.get(levelIndex - 1);
        return Math.round(((max - min) * progress) + (min * multiplier));
    }

    private static SpawnGroup getGuaranteedSpawn(int levelIndex) {
        List<? extends String> spawns = Config.GUARANTEED_SPAWNS.get();
        String prefix = levelIndex + ":";
        for (String spawn : spawns) {
            if (spawn.startsWith(prefix)) {
                String data = spawn.substring(prefix.length());
                if (data.startsWith("random:")) {
                    data = data.substring(7);
                    String[] options = data.split("\\|");
                    ThreadLocalRandom rnd = ThreadLocalRandom.current();
                    String chosen = options[rnd.nextInt(options.length)];
                    return parseSpawnGroup(chosen, 0, 0);
                } else {
                    return parseSpawnGroup(data, 0, 0);
                }
            }
        }
        return null;
    }

    private static List<SpawnGroup> selectBudgetGroups(int budget, int levelIndex) {
        List<SpawnGroup> selected = new ArrayList<>();
        List<SpawnGroup> available = getBudgetGroupsForLevel(levelIndex);
        int remaining = budget;
        ThreadLocalRandom random = ThreadLocalRandom.current();

        while (remaining > 0) {
            List<SpawnGroup> affordable = new ArrayList<>();
            for (SpawnGroup g : available) if (g.cost <= remaining) affordable.add(g);
            if (affordable.isEmpty()) break;

            int totalWeight = 0;
            for (SpawnGroup g : affordable) totalWeight += g.weight;
            if (totalWeight <= 0) break;

            int roll = random.nextInt(totalWeight);
            SpawnGroup chosen = affordable.get(0);
            for (SpawnGroup g : affordable) {
                roll -= g.weight;
                if (roll < 0) { chosen = g; break; }
            }
            selected.add(chosen);
            remaining -= chosen.cost;
        }
        return selected;
    }

    private static List<SpawnGroup> getBudgetGroupsForLevel(int levelIndex) {
        List<SpawnGroup> groups = new ArrayList<>();
        List<? extends String> budgetGroups = Config.BUDGET_GROUPS.get();
        String prefix = levelIndex + ":";
        for (String entry : budgetGroups) {
            if (entry.startsWith(prefix)) {
                String data = entry.substring(prefix.length());
                int firstColon = data.indexOf(':');
                int secondColon = data.indexOf(':', firstColon + 1);
                if (firstColon > 0 && secondColon > firstColon) {
                    try {
                        int cost = Integer.parseInt(data.substring(0, firstColon));
                        int weight = Integer.parseInt(data.substring(firstColon + 1, secondColon));
                        String enemyData = data.substring(secondColon + 1);
                        SpawnGroup group = parseSpawnGroup(enemyData, cost, weight);
                        if (group != null) groups.add(group);
                    } catch (NumberFormatException ignored) {}
                }
            }
        }
        return groups;
    }

    private static SpawnGroup parseSpawnGroup(String data, int cost, int weight) {
        List<Enemy> enemies = new ArrayList<>();
        String[] parts = data.split(",");
        for (String part : parts) {
            String[] ep = part.trim().split(":");
            if (ep.length >= 3) {
                String entityId = ep[0] + ":" + ep[1];
                try {
                    int qty = Integer.parseInt(ep[2]);
                    enemies.add(new Enemy(entityId, qty));
                } catch (NumberFormatException ignored) {}
            }
        }
        return enemies.isEmpty() ? null : new SpawnGroup(enemies, cost, weight);
    }

    private static class Enemy {
        public String entityId;
        public int qty;

        public Enemy(String entityId, int qty) {
            this.entityId = entityId;
            this.qty = qty;
        }
    }
    private record SpawnGroup(List<Enemy> enemies, int cost, int weight) {}

    private static class RaidSpawnTask {
        List<SpawnGroup> spawnGroups;
        List<BlockPos> spawnPoints;
        Vec3 center;
        int spawnIndex;
        long nextSpawnTime;
        ChunkPos farmMainChunk;
    }
}