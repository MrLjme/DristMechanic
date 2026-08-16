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
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.server.ServerStoppingEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import net.neoforged.neoforge.network.PacketDistributor;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadLocalRandom;

@EventBusSubscriber(modid = Dristmechanic.MODID)
public class RaidManager {

    private static final Map<UUID, Integer> totalRaidValueByFarm = new ConcurrentHashMap<>();
    private static final Map<UUID, Integer> currentRaidValueByFarm = new ConcurrentHashMap<>();
    private static final Map<UUID, Integer> originalFarmValueByFarm = new ConcurrentHashMap<>();
    private static final Map<UUID, FarmManager.FarmData> mobToFarmMap = new ConcurrentHashMap<>();
    private static final Map<ServerLevel, List<RaidSpawnTask>> activeSpawnTasks = new ConcurrentHashMap<>();

    @SubscribeEvent
    public static void onServerStopping(ServerStoppingEvent e) {
        totalRaidValueByFarm.clear();
        currentRaidValueByFarm.clear();
        originalFarmValueByFarm.clear();
        mobToFarmMap.clear();
        activeSpawnTasks.clear();
    }

    public static void syncFarmData(ServerLevel level, FarmManager.FarmData farm, int guiValue) {
        boolean hasValue = guiValue > 0;
        boolean raidActive = farm.isRaidActive();
        int maxValue = raidActive ? getOriginalValue(farm) : Config.MAX_CROP_VALUE.get();
        syncToClients(level, hasValue, guiValue, maxValue, raidActive);
    }

    public static int getOriginalValue(FarmManager.FarmData farm) {
        return originalFarmValueByFarm.getOrDefault(farm.getFarmId(), 1);
    }

    public static int getCurrentRaidValue(FarmManager.FarmData farm) {
        return currentRaidValueByFarm.getOrDefault(farm.getFarmId(), 0);
    }

    public static void onRaidScheduled(ServerLevel level, FarmManager.FarmData farm, int value) {
        originalFarmValueByFarm.put(farm.getFarmId(), value);
        syncToClients(level, false, value, Config.MAX_CROP_VALUE.get(), false);
    }

    public static void onRaidComplete(ServerLevel level, FarmManager.FarmData farm) {
        originalFarmValueByFarm.remove(farm.getFarmId());
        currentRaidValueByFarm.remove(farm.getFarmId());
        totalRaidValueByFarm.remove(farm.getFarmId());
        syncToClients(level, false, 0, Config.MAX_CROP_VALUE.get(), false);
    }

    public static int getDisplayValue(FarmManager.FarmData farm) {
        int currentRaidValue = currentRaidValueByFarm.getOrDefault(farm.getFarmId(), 0);
        int originalValue = originalFarmValueByFarm.getOrDefault(farm.getFarmId(), 0);
        int totalRaidValue = totalRaidValueByFarm.getOrDefault(farm.getFarmId(), 0);

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

        FarmManager.FarmData farm = mobToFarmMap.get(mobUUID);

        if (farm != null) {
            farm.removeSpawnedMob(mobUUID);
            mobToFarmMap.remove(mobUUID);

            int mobValue = calculateMobValue(event.getEntity());
            int currentRaidValue = currentRaidValueByFarm.getOrDefault(farm.getFarmId(), 0);
            currentRaidValue -= mobValue;
            currentRaidValue = Math.max(0, currentRaidValue);
            currentRaidValueByFarm.put(farm.getFarmId(), currentRaidValue);

            int displayValue = getDisplayValue(farm);
            syncToClients(level, false, displayValue, Config.MAX_CROP_VALUE.get(), farm.isRaidActive());
        }
    }

    private static int calculateMobValue(Entity mob) {
        if (mob instanceof FarmbotEntity) return 100;
        if (mob instanceof HaybotEntity) return 20;
        if (mob instanceof RedTapebotEntity) return 15;
        if (mob instanceof TapebotEntity) return 10;
        if (mob instanceof TotebotEntity) return 5;
        return 1;
    }

    public static void restoreRaid(ServerLevel level, FarmManager.FarmData farm, FarmManager.FarmSavedData.FarmEntry entry) {
        originalFarmValueByFarm.put(farm.getFarmId(), entry.accumulatedValue);
        currentRaidValueByFarm.put(farm.getFarmId(), entry.accumulatedValue);
        totalRaidValueByFarm.put(farm.getFarmId(), entry.accumulatedValue);

        for (UUID mobUUID : entry.spawnedMobs) {
            Entity entity = level.getEntity(mobUUID);
            if (entity != null && entity.isAlive()) {
                farm.addSpawnedMob(mobUUID);
                mobToFarmMap.put(mobUUID, farm);
            }
        }

        if (farm.getSpawnedMobCount() == 0) {
            farm.setRaidActive(false);
            farm.unlockChunks(level);
            onRaidComplete(level, farm);
        }
    }

    public static boolean executeRaid(ServerLevel level, FarmManager.FarmData farm, int farmValue, Vec3 farmCenter) {
        Vec3 center = farmCenter != null ? farmCenter : new Vec3(0, 64, 0);
        int raidLevel = getRaidLevel(farmValue);
        int playerCount = level.players().size();
        int budget = calculateBudget(raidLevel, farmValue, playerCount);

        SpawnGroup guaranteedGroup = getGuaranteedSpawn(raidLevel);
        List<SpawnGroup> budgetGroups = selectBudgetGroups(budget, raidLevel);

        List<Enemy> allGeneratedEnemies = new ArrayList<>();

        if (guaranteedGroup != null) {
            allGeneratedEnemies.addAll(guaranteedGroup.enemies);
        }

        for (SpawnGroup bg : budgetGroups) {
            allGeneratedEnemies.addAll(bg.enemies);
        }

        if (allGeneratedEnemies.isEmpty()) {
            return false;
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
                int spaceInWave = mobsPerWave - currentWaveSize;
                int toAdd = Math.min(remaining, spaceInWave);

                if (toAdd > 0) {
                    Enemy existing = currentWave.stream()
                            .filter(e -> e.entityId.equals(entityId))
                            .findFirst()
                            .orElse(null);

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
                int mobValue = calculateMobValue(enemy);
                totalRaidValue += mobValue * enemy.qty;
            }
        }

        currentRaidValueByFarm.put(farm.getFarmId(), totalRaidValue);
        totalRaidValueByFarm.put(farm.getFarmId(), totalRaidValue);

        int desiredSpawnPoints = Math.max(15, Math.min(60, totalMobs / 3));
        List<BlockPos> spawnPoints = findRaidSpawnPoints(level, center, desiredSpawnPoints);

        if (spawnPoints.isEmpty()) {
            spawnPoints = findRaidSpawnPointsClose(level, center, desiredSpawnPoints);
        }

        if (spawnPoints.isEmpty()) {
            return false;
        }

        RaidSpawnTask task = new RaidSpawnTask();
        task.spawnGroups = waves;
        task.spawnPoints = spawnPoints;
        task.center = center;
        task.spawnIndex = 0;
        task.nextSpawnTime = level.getGameTime();
        task.farm = farm;

        activeSpawnTasks.computeIfAbsent(level, k -> new ArrayList<>()).add(task);

        int originalValue = originalFarmValueByFarm.getOrDefault(farm.getFarmId(), 0);
        double scaleFactor = originalValue > 0 && totalRaidValue > 0
                ? (double) originalValue / totalRaidValue
                : 1.0;
        int displayValue = (int) Math.round(totalRaidValue * scaleFactor);

        syncToClients(level, true, displayValue, Config.MAX_CROP_VALUE.get(), true);

        return true;
    }

    private static int calculateMobValue(Enemy enemy) {
        if (enemy.entityId.contains("farmbot")) return 100;
        if (enemy.entityId.contains("haybot")) return 20;
        if (enemy.entityId.contains("red_tapebot")) return 15;
        if (enemy.entityId.contains("tapebot")) return 10;
        if (enemy.entityId.contains("totebot")) return 5;
        return 1;
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
                    SpawnGroup group = task.spawnGroups.get(task.spawnIndex);
                    spawnGroup(level, group, task.spawnPoints, task.center, task.farm);
                    task.spawnIndex++;
                    task.nextSpawnTime = currentTime + Config.SPAWN_INTERVAL_TICKS.get();
                } else {
                    it.remove();
                }
            }
        }
    }

    private static void spawnGroup(ServerLevel level, SpawnGroup group, List<BlockPos> spawnPoints, Vec3 center, FarmManager.FarmData farm) {
        if (spawnPoints.isEmpty()) return;

        ThreadLocalRandom rnd = ThreadLocalRandom.current();
        BlockPos centerPos = new BlockPos((int) center.x, (int) center.y, (int) center.z);

        for (Enemy enemy : group.enemies) {
            ResourceLocation location = ResourceLocation.tryParse(enemy.entityId);
            if (location == null) continue;

            EntityType<?> entityType = BuiltInRegistries.ENTITY_TYPE.getOptional(location).orElse(null);
            if (entityType == null) continue;

            for (int i = 0; i < enemy.qty; i++) {
                BlockPos pos = spawnPoints.get(rnd.nextInt(spawnPoints.size()));

                Mob mob = (Mob) entityType.create(level);

                if (mob != null) {
                    mob.moveTo(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5, rnd.nextFloat() * 360F, 0);
                    mob.setPersistenceRequired();

                    UUID mobUUID = mob.getUUID();
                    farm.addSpawnedMob(mobUUID);
                    mobToFarmMap.put(mobUUID, farm);

                    if (mob instanceof TotebotEntity totebot) {
                        totebot.setRaidTarget(centerPos);
                    } else if (mob instanceof FarmbotEntity farmbot) {
                        farmbot.setRaidTarget(centerPos);
                    } else if (mob instanceof HaybotEntity haybot) {
                        haybot.setRaidTarget(centerPos);
                    } else if (mob instanceof TapebotEntity tapebot) {
                        tapebot.setRaidTarget(centerPos);
                    } else if (mob instanceof RedTapebotEntity redTapebot) {
                        redTapebot.setRaidTarget(centerPos);
                    }

                    level.addFreshEntity(mob);
                }
            }
        }
    }

    private static int getRaidLevel(int farmValue) {
        List<? extends Integer> thresholds = Config.LEVEL_THRESHOLDS.get();
        int level = 0;

        for (int i = 0; i < thresholds.size(); i++) {
            if (farmValue >= thresholds.get(i)) {
                level = i + 1;
            } else {
                break;
            }
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

        if (levelIndex > thresholds.size() || levelIndex > minBudgets.size() || levelIndex > maxBudgets.size()) {
            return 0;
        }

        int currentMin = thresholds.get(levelIndex - 1);
        int nextMin = (levelIndex < thresholds.size()) ? thresholds.get(levelIndex) : maxCropValue;

        float progress = 1.0f;
        if (nextMin > currentMin) {
            progress = (float) (farmValue - currentMin) / (float) (nextMin - currentMin);
            progress = Math.max(0.0f, Math.min(1.0f, progress));
        }

        float multiplier = 1.0f;
        if (playerCount == 1 && multipliers.size() > 0) {
            multiplier = multipliers.get(0).floatValue();
        } else if (playerCount == 2 && multipliers.size() > 1) {
            multiplier = multipliers.get(1).floatValue();
        } else if (playerCount >= 3 && multipliers.size() > 2) {
            multiplier = multipliers.get(2).floatValue();
        }

        int min = minBudgets.get(levelIndex - 1);
        int max = maxBudgets.get(levelIndex - 1);

        int baseBudget = (int) (min + (max - min) * progress);
        return Math.round(baseBudget * multiplier);
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
        List<SpawnGroup> availableGroups = getBudgetGroupsForLevel(levelIndex);
        int remaining = budget;

        ThreadLocalRandom random = ThreadLocalRandom.current();

        while (remaining > 0) {
            List<SpawnGroup> affordable = new ArrayList<>();

            for (SpawnGroup group : availableGroups) {
                if (group.cost <= remaining) {
                    affordable.add(group);
                }
            }

            if (affordable.isEmpty()) break;

            int totalWeight = 0;
            for (SpawnGroup group : affordable) {
                totalWeight += group.weight;
            }

            if (totalWeight <= 0) break;

            int roll = random.nextInt(totalWeight);
            SpawnGroup chosen = affordable.get(0);

            for (SpawnGroup group : affordable) {
                roll -= group.weight;
                if (roll < 0) {
                    chosen = group;
                    break;
                }
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
                        if (group != null) {
                            groups.add(group);
                        }
                    } catch (NumberFormatException ignored) {
                    }
                }
            }
        }

        return groups;
    }

    private static SpawnGroup parseSpawnGroup(String data, int cost, int weight) {
        List<Enemy> enemies = new ArrayList<>();
        String[] parts = data.split(",");

        for (String part : parts) {
            String[] enemyParts = part.trim().split(":");

            if (enemyParts.length >= 3) {
                String entityId = enemyParts[0] + ":" + enemyParts[1];

                try {
                    int qty = Integer.parseInt(enemyParts[2]);
                    enemies.add(new Enemy(entityId, qty));
                } catch (NumberFormatException ignored) {
                }
            }
        }

        return enemies.isEmpty() ? null : new SpawnGroup(enemies, cost, weight);
    }

    public static List<BlockPos> findRaidSpawnPoints(ServerLevel level, Vec3 center, int max) {
        List<BlockPos> res = new ArrayList<>();
        ThreadLocalRandom rnd = ThreadLocalRandom.current();

        for (int i = 0; i < max; i++) {
            double angle = rnd.nextDouble() * 2 * Math.PI;
            double distance = rnd.nextDouble(32, 65);

            int tX = (int) (center.x + Math.cos(angle) * distance);
            int tZ = (int) (center.z + Math.sin(angle) * distance);

            BlockPos sp = findClosestValidSpawn(level, tX, tZ, (int) center.y);
            if (sp != null) res.add(sp);
        }

        return res;
    }

    public static List<BlockPos> findRaidSpawnPointsClose(ServerLevel level, Vec3 center, int max) {
        List<BlockPos> res = new ArrayList<>();
        ThreadLocalRandom rnd = ThreadLocalRandom.current();

        for (int i = 0; i < max; i++) {
            double angle = rnd.nextDouble() * 2 * Math.PI;
            double distance = rnd.nextDouble(8, 20);

            int tX = (int) (center.x + Math.cos(angle) * distance);
            int tZ = (int) (center.z + Math.sin(angle) * distance);

            BlockPos sp = findClosestValidSpawn(level, tX, tZ, (int) center.y);
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

    public static void syncToClients(ServerLevel level, boolean active, int current, int max, boolean raidActive) {
        for (ServerPlayer player : level.players()) {
            PacketDistributor.sendToPlayer(player, new RaidDataPayload(active, current, max, raidActive));
        }
    }

    private static class Enemy {
        public String entityId;
        public int qty;

        public Enemy(String entityId, int qty) {
            this.entityId = entityId;
            this.qty = qty;
        }
    }

    private static class SpawnGroup {
        public final List<Enemy> enemies;
        public final int cost;
        public final int weight;

        public SpawnGroup(List<Enemy> enemies, int cost, int weight) {
            this.enemies = enemies;
            this.cost = cost;
            this.weight = weight;
        }
    }

    private static class RaidSpawnTask {
        public List<SpawnGroup> spawnGroups;
        public List<BlockPos> spawnPoints;
        public Vec3 center;
        public int spawnIndex;
        public long nextSpawnTime;
        public FarmManager.FarmData farm;
    }
}