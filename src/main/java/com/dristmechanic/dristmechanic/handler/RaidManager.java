package com.dristmechanic.dristmechanic.handler;

import com.dristmechanic.dristmechanic.Config;
import com.dristmechanic.dristmechanic.Dristmechanic;
import com.dristmechanic.dristmechanic.entity.*;
import com.dristmechanic.dristmechanic.init.ModAttachments;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.level.LevelEvent;
import net.neoforged.neoforge.event.server.ServerStoppingEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadLocalRandom;

@EventBusSubscriber(modid = Dristmechanic.MODID)
public class RaidManager {

    private static final String NBT_TARGET_TIME = "DristRaidTargetTime";
    private static final String NBT_LOCKED_VALUE = "DristRaidLockedValue";
    private static final String NBT_CENTER_X = "DristRaidCenterX";
    private static final String NBT_CENTER_Y = "DristRaidCenterY";
    private static final String NBT_CENTER_Z = "DristRaidCenterZ";
    private static final String NBT_RAIDER_VALUE = "DristRaiderValue";
    private static final String NBT_RAID_TOTAL = "DristRaidTotal";
    private static final String NBT_RAID_FARM = "DristRaidFarm";
    private static final String TAG_FARM_CENTER = "drist_farm_center";
    private static final String TAG_RAIDER = "drist_raider";
    private static final long RAID_DELAY_TICKS = 1200L;

    private static final Map<ServerLevel, Set<UUID>> trackedStandsByLevel = new ConcurrentHashMap<>();
    private static final Map<ServerLevel, List<RaidSpawnTask>> activeSpawnTasks = new ConcurrentHashMap<>();
    private static final Map<ServerLevel, Integer> raidRemainingByLevel = new ConcurrentHashMap<>();
    private static final Map<ServerLevel, Integer> raidTotalByLevel = new ConcurrentHashMap<>();
    private static final Map<ServerLevel, Integer> raidFarmValueByLevel = new ConcurrentHashMap<>();

    private static volatile List<? extends String> lastMobList = null;
    private static volatile Map<String, Integer> mobValueCache = new HashMap<>();

    public record HudData(boolean isActive, int currentValue, int maxValue) {}

    @SubscribeEvent
    public static void onServerStopping(ServerStoppingEvent event) {
        trackedStandsByLevel.clear();
        activeSpawnTasks.clear();
        raidRemainingByLevel.clear();
        raidTotalByLevel.clear();
        raidFarmValueByLevel.clear();
    }

    @SubscribeEvent
    public static void onLevelUnload(LevelEvent.Unload event) {
        if (event.getLevel() instanceof ServerLevel sl) {
            trackedStandsByLevel.remove(sl);
            activeSpawnTasks.remove(sl);
            raidRemainingByLevel.remove(sl);
            raidTotalByLevel.remove(sl);
            raidFarmValueByLevel.remove(sl);
        }
    }

    @SubscribeEvent
    public static void onEntityJoinLevel(EntityJoinLevelEvent event) {
        if (!event.getLevel().isClientSide() && event.getEntity() instanceof ArmorStand as) {
            if (as.getTags().contains(TAG_FARM_CENTER) && event.getLevel() instanceof ServerLevel sl) {
                trackedStandsByLevel.computeIfAbsent(sl, k -> ConcurrentHashMap.newKeySet()).add(as.getUUID());
            }
        }
    }

    @SubscribeEvent
    public static void onLivingDeath(LivingDeathEvent event) {
        Entity entity = event.getEntity();
        if (entity.level().isClientSide() || !entity.getTags().contains(TAG_RAIDER)) return;
        if (!(entity.level() instanceof ServerLevel level)) return;

        int value = entity.getPersistentData().getInt(NBT_RAIDER_VALUE);
        Integer remaining = raidRemainingByLevel.get(level);
        if (remaining == null) return;

        int newVal = Math.max(0, remaining - value);
        raidRemainingByLevel.put(level, newVal);

        if (newVal <= 0 || !hasLivingRaiders(level)) {
            raidRemainingByLevel.remove(level);
            raidTotalByLevel.remove(level);
            raidFarmValueByLevel.remove(level);
            activeSpawnTasks.remove(level);
            notifyRaidEnd(level);
        }
    }

    private static boolean hasLivingRaiders(ServerLevel level) {
        for (Entity e : level.getAllEntities()) {
            if (e.isAlive() && e.getTags().contains(TAG_RAIDER)) {
                return true;
            }
        }
        List<RaidSpawnTask> tasks = activeSpawnTasks.get(level);
        return tasks != null && !tasks.isEmpty();
    }

    @SubscribeEvent
    public static void onServerTick(ServerTickEvent.Post event) {
        for (ServerLevel level : event.getServer().getAllLevels()) {
            if (level.getGameTime() % 20 == 0) {
                ensureRaidState(level);
            }
            tickRaids(level);
            tickSpawnTasks(level);
            checkRaidCompletion(level);
        }
    }

    private static void ensureRaidState(ServerLevel level) {
        if (raidRemainingByLevel.containsKey(level)) return;
        List<RaidSpawnTask> tasks = activeSpawnTasks.get(level);
        if (tasks != null && !tasks.isEmpty()) return;

        int remaining = 0;
        int total = 0;
        int farm = 0;

        for (Entity e : level.getAllEntities()) {
            if (e.isAlive() && e.getTags().contains(TAG_RAIDER)) {
                CompoundTag data = e.getPersistentData();
                remaining += data.getInt(NBT_RAIDER_VALUE);
                if (total <= 0) total = data.getInt(NBT_RAID_TOTAL);
                if (farm <= 0) farm = data.getInt(NBT_RAID_FARM);
            }
        }

        if (remaining > 0) {
            int safeTotal = Math.max(total, remaining);
            raidRemainingByLevel.put(level, remaining);
            raidTotalByLevel.put(level, safeTotal);
            raidFarmValueByLevel.put(level, farm > 0 ? farm : safeTotal);
        }
    }

    private static void checkRaidCompletion(ServerLevel level) {
        Integer remaining = raidRemainingByLevel.get(level);
        if (remaining == null || remaining <= 0) return;

        List<RaidSpawnTask> tasks = activeSpawnTasks.get(level);
        boolean spawningActive = tasks != null && !tasks.isEmpty();

        if (!spawningActive && !hasLivingRaiders(level)) {
            raidRemainingByLevel.remove(level);
            raidTotalByLevel.remove(level);
            raidFarmValueByLevel.remove(level);
            notifyRaidEnd(level);
        }
    }

    private static int getMobValue(EntityType<?> type) {
        List<? extends String> list = Config.MOB_VALUES.get();
        if (list != lastMobList) {
            Map<String, Integer> m = new HashMap<>();
            for (String entry : list) {
                String[] parts = entry.split("=");
                if (parts.length == 2) {
                    try {
                        m.put(parts[0].trim(), Integer.parseInt(parts[1].trim()));
                    } catch (NumberFormatException ignored) {}
                }
            }
            mobValueCache = m;
            lastMobList = list;
        }
        return mobValueCache.getOrDefault(BuiltInRegistries.ENTITY_TYPE.getKey(type).toString(), 0);
    }

    private static int getMobValueByEntityId(String entityId) {
        List<? extends String> list = Config.MOB_VALUES.get();
        if (list != lastMobList) {
            Map<String, Integer> m = new HashMap<>();
            for (String entry : list) {
                String[] parts = entry.split("=");
                if (parts.length == 2) {
                    try {
                        m.put(parts[0].trim(), Integer.parseInt(parts[1].trim()));
                    } catch (NumberFormatException ignored) {}
                }
            }
            mobValueCache = m;
            lastMobList = list;
        }
        return mobValueCache.getOrDefault(entityId, 0);
    }

    public static boolean isPlantingBlocked(ServerLevel level, ChunkPos cp) {
        for (int dx = -1; dx <= 1; dx++) {
            for (int dz = -1; dz <= 1; dz++) {
                ChunkPos n = new ChunkPos(cp.x + dx, cp.z + dz);
                FarmManager.FarmData farm = FarmManager.findConnectedFarm(level, n);
                if (!farm.chunks().isEmpty() && farm.center() != null && isRaidPendingNear(level, farm.center())) {
                    return true;
                }
            }
        }
        return false;
    }

    private static boolean isRaidPendingNear(ServerLevel level, Vec3 center) {
        double distSq = 16.0 * 16.0;
        Set<UUID> tracked = trackedStandsByLevel.get(level);
        if (tracked != null) {
            for (UUID uuid : tracked) {
                Entity e = level.getEntity(uuid);
                if (e instanceof ArmorStand as && as.isAlive() && hasRaidScheduled(as)) {
                    if (as.distanceToSqr(center.x, center.y, center.z) < distSq) return true;
                }
            }
        }
        List<RaidSpawnTask> tasks = activeSpawnTasks.get(level);
        if (tasks != null) {
            for (RaidSpawnTask t : tasks) {
                if (t.center != null && t.center.distanceToSqr(center.x, center.y, center.z) < distSq) return true;
            }
        }
        return raidRemainingByLevel.getOrDefault(level, 0) > 0;
    }

    public static HudData getNearestRaidData(ServerLevel level, Vec3 pos) {
        Integer remaining = raidRemainingByLevel.get(level);
        Integer totalRaidValue = raidTotalByLevel.get(level);
        boolean raidActive = (remaining != null && remaining > 0)
                || !activeSpawnTasks.getOrDefault(level, Collections.emptyList()).isEmpty();

        if (raidActive) {
            int lockedFarm = Math.max(1, raidFarmValueByLevel.getOrDefault(level, 0));
            int currentValue = lockedFarm;
            if (remaining != null && totalRaidValue != null && totalRaidValue > 0) {
                double scale = (double) lockedFarm / totalRaidValue;
                currentValue = (int) Math.ceil(remaining * scale);
                currentValue = Math.max(0, Math.min(currentValue, lockedFarm));
            }
            return new HudData(true, currentValue, lockedFarm);
        }

        BlockPos blockPos = new BlockPos(Mth.floor(pos.x), Mth.floor(pos.y), Mth.floor(pos.z));
        FarmManager.FarmData farm = FarmManager.findNearestFarm(level, blockPos, 16);

        if (farm == null || farm.isEmpty() || farm.totalValue() <= 0) {
            return new HudData(false, 0, 0);
        }

        int farmValue = farm.totalValue();
        return new HudData(false, farmValue, farmValue);
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
                    spawnGroup(level, group, task);
                    task.spawnIndex++;
                    task.nextSpawnTime = currentTime + Config.SPAWN_INTERVAL_TICKS.get();
                } else {
                    it.remove();
                }
            }
        }
    }

    public static void updateHolograms(ServerLevel level, List<FarmManager.FarmData> stableFarms) {
        Set<UUID> tracked = trackedStandsByLevel.getOrDefault(level, Collections.emptySet());
        Set<Vec3> activeCenters = new HashSet<>();

        for (FarmManager.FarmData farm : stableFarms) {
            if (farm.unraidedValue() > 0) {
                activeCenters.add(farm.center());
                ArmorStand stand = getStandNearby(level, farm.center());
                if (stand == null) {
                    spawnStandForFarm(level, farm);
                } else {
                    if (hasRaidScheduled(stand) && farm.unraidedValue() > getLockedValue(stand)) {
                        setLockedValue(stand, farm.unraidedValue());
                    }
                }
            }
        }

        tracked.removeIf(uuid -> {
            Entity e = level.getEntity(uuid);
            if (!(e instanceof ArmorStand as) || !as.isAlive()) return true;

            boolean isActive = false;
            for (Vec3 center : activeCenters) {
                if (as.distanceToSqr(center.x, center.y, center.z) < 16.0 * 16.0) {
                    isActive = true;
                    break;
                }
            }

            if (!isActive) {
                if (hasRaidScheduled(as)) {
                    return false;
                }
                as.discard();
                return true;
            }
            return false;
        });
    }

    private static ArmorStand getStandNearby(ServerLevel level, Vec3 center) {
        Set<UUID> tracked = trackedStandsByLevel.get(level);
        if (tracked == null) return null;

        double searchDistSq = 16.0 * 16.0;
        for (UUID uuid : tracked) {
            Entity e = level.getEntity(uuid);
            if (e instanceof ArmorStand as && as.isAlive()) {
                if (as.distanceToSqr(center.x, center.y, center.z) < searchDistSq) return as;
            }
        }
        return null;
    }

    private static void spawnStandForFarm(ServerLevel level, FarmManager.FarmData farm) {
        Vec3 center = farm.center();
        ArmorStand stand = createStand(level, center);
        if (stand != null) {
            setCenter(stand, center);
            long currentTime = level.getGameTime();
            long targetTime = currentTime + RAID_DELAY_TICKS;
            setTargetTime(stand, targetTime);
            setLockedValue(stand, farm.unraidedValue());
            notifyDetection(level);
        }
    }

    private static void tickRaids(ServerLevel level) {
        Set<UUID> tracked = trackedStandsByLevel.get(level);
        if (tracked == null || tracked.isEmpty()) return;

        long currentTime = level.getGameTime();
        tracked.removeIf(uuid -> {
            Entity entity = level.getEntity(uuid);
            if (!(entity instanceof ArmorStand as) || !as.isAlive()) return true;

            if (as.getPersistentData().contains(NBT_TARGET_TIME)) {
                long targetTime = as.getPersistentData().getLong(NBT_TARGET_TIME);
                long timeLeft = targetTime - currentTime;
                if (timeLeft <= 0) {
                    executeRaid(level, as, getLockedValue(as));
                    return true;
                } else if (currentTime % 20 == 0) {
                    updateHologramText(as, timeLeft);
                }
            }
            return false;
        });
    }

    private static ArmorStand createStand(ServerLevel level, Vec3 pos) {
        ArmorStand as = EntityType.ARMOR_STAND.create(level);
        if (as != null) {
            as.setPos(pos.x, pos.y, pos.z);
            as.setInvisible(true);
            byte flags = as.getEntityData().get(ArmorStand.DATA_CLIENT_FLAGS);
            as.getEntityData().set(ArmorStand.DATA_CLIENT_FLAGS, (byte) (flags | 16));
            as.setNoBasePlate(true);
            as.setInvulnerable(true);
            as.setNoGravity(true);
            as.addTag(TAG_FARM_CENTER);
            as.setCustomNameVisible(true);
            updateHologramText(as, 0);
            level.addFreshEntity(as);
            return as;
        }
        return null;
    }

    private static void executeRaid(ServerLevel level, ArmorStand targetStand, int farmValue) {
        Vec3 center = getCenter(targetStand);
        ChunkPos asChunk = new ChunkPos(targetStand.blockPosition());
        FarmManager.FarmData farm = FarmManager.findConnectedFarm(level, asChunk);

        if (farm != null && !farm.chunks().isEmpty()) {
            for (ChunkPos c : farm.chunks()) {
                LevelChunk chunk = CropScanningHandler.getChunkSafe(level, c.x, c.z);
                if (chunk != null) {
                    int currentCrop = chunk.getData(ModAttachments.CROP_COUNT.get());
                    chunk.setData(ModAttachments.RAIDED_CROP_VALUE.get(), currentCrop);
                    chunk.setUnsaved(true);
                }
            }
        } else {
            farm = new FarmManager.FarmData(List.of(asChunk), farmValue, 0, center, List.of(asChunk));
        }

        targetStand.discard();

        int raidLevel = getRaidLevel(farmValue);
        int playerCount = level.players().size();
        int budget = calculateBudget(raidLevel, farmValue, playerCount);

        SpawnGroup guaranteedGroup = getGuaranteedSpawn(raidLevel);
        List<SpawnGroup> budgetGroups = selectBudgetGroups(budget, raidLevel);
        List<Enemy> allGeneratedEnemies = new ArrayList<>();

        if (guaranteedGroup != null) allGeneratedEnemies.addAll(guaranteedGroup.enemies);
        for (SpawnGroup bg : budgetGroups) allGeneratedEnemies.addAll(bg.enemies);

        if (allGeneratedEnemies.isEmpty()) {
            SpawnGroup fallback = getGuaranteedSpawn(Math.max(1, raidLevel - 1));
            if (fallback != null) allGeneratedEnemies.addAll(fallback.enemies);
        }

        if (allGeneratedEnemies.isEmpty()) return;

        Map<String, Integer> enemyCounts = new LinkedHashMap<>();
        for (Enemy e : allGeneratedEnemies) {
            enemyCounts.merge(e.entityId, e.qty, Integer::sum);
        }

        int totalRaidValue = 0;
        for (Map.Entry<String, Integer> entry : enemyCounts.entrySet()) {
            String entityId = entry.getKey();
            int count = entry.getValue();
            int mobValue = getMobValueByEntityId(entityId);
            totalRaidValue += mobValue * count;
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
                    Enemy existing = currentWave.stream().filter(e -> e.entityId.equals(entityId)).findFirst().orElse(null);
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

        int desiredSpawnPoints = Math.max(15, Math.min(60, totalMobs / 3));
        List<BlockPos> spawnPoints = findRaidSpawnPoints(level, farm, desiredSpawnPoints, 0.9F, 1.8F);

        if (spawnPoints.size() < 10) {
            ThreadLocalRandom rnd = ThreadLocalRandom.current();
            int attempts = 0;
            while (spawnPoints.size() < desiredSpawnPoints && attempts < 300) {
                double angle = rnd.nextDouble() * 2 * Math.PI;
                double distance = rnd.nextDouble(32, 65);
                int tX = (int)(center.x + Math.cos(angle) * distance);
                int tZ = (int)(center.z + Math.sin(angle) * distance);

                BlockPos sp = findClosestValidSpawn(level, tX, tZ, (int)center.y, 0.9F, 1.8F);
                if (sp != null) spawnPoints.add(sp);
                attempts++;
            }
        }

        if (!waves.isEmpty()) {
            RaidSpawnTask task = new RaidSpawnTask();
            task.spawnGroups = waves;
            task.spawnPoints = spawnPoints;
            task.center = center;
            task.spawnIndex = 0;
            task.nextSpawnTime = level.getGameTime();
            task.totalRaidValue = totalRaidValue;
            task.farmValue = farmValue;
            activeSpawnTasks.computeIfAbsent(level, k -> new ArrayList<>()).add(task);

            raidRemainingByLevel.merge(level, totalRaidValue, Integer::sum);
            raidTotalByLevel.merge(level, totalRaidValue, Integer::sum);
            raidFarmValueByLevel.put(level, farmValue);
            notifyRaidStart(level, totalMobs, totalRaidValue);
        }
    }

    private static void spawnGroup(ServerLevel level, SpawnGroup group, RaidSpawnTask task) {
        List<BlockPos> spawnPoints = task.spawnPoints;
        Vec3 center = task.center;
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

                    int value = getMobValue(entityType);
                    if (value > 0) {
                        mob.addTag(TAG_RAIDER);
                        mob.getPersistentData().putInt(NBT_RAIDER_VALUE, value);
                        mob.getPersistentData().putInt(NBT_RAID_TOTAL, task.totalRaidValue);
                        mob.getPersistentData().putInt(NBT_RAID_FARM, task.farmValue);
                    }

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
            progress = (float)(farmValue - currentMin) / (float)(nextMin - currentMin);
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
            if (affordable.isEmpty()) {
                break;
            }

            int totalWeight = 0;
            for (SpawnGroup group : affordable) {
                totalWeight += group.weight;
            }
            if (totalWeight <= 0) {
                break;
            }

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
            String[] enemyParts = part.trim().split(":");
            if (enemyParts.length >= 3) {
                String entityId = enemyParts[0] + ":" + enemyParts[1];
                try {
                    int qty = Integer.parseInt(enemyParts[2]);
                    enemies.add(new Enemy(entityId, qty));
                } catch (NumberFormatException ignored) {}
            }
        }
        return enemies.isEmpty() ? null : new SpawnGroup(enemies, cost, weight);
    }

    public static List<BlockPos> findRaidSpawnPoints(ServerLevel level, FarmManager.FarmData farm, int max, float w, float h) {
        List<BlockPos> res = new ArrayList<>();
        if (farm == null || farm.isEmpty() || farm.edgeChunks().isEmpty()) return res;

        Vec3 c = farm.center();
        int tY = Mth.floor(c.y);
        ThreadLocalRandom rnd = ThreadLocalRandom.current();
        boolean isSmallFarm = farm.edgeChunks().size() <= 4;

        if (isSmallFarm) {
            double angleStep = (2 * Math.PI) / max;
            for (int i = 0; i < max; i++) {
                double baseAngle = i * angleStep;
                double angle = baseAngle + rnd.nextDouble(-angleStep / 4, angleStep / 4);
                double spawnDistance = rnd.nextDouble(32, 48);
                int tX = (int) (c.x + Math.cos(angle) * spawnDistance);
                int tZ = (int) (c.z + Math.sin(angle) * spawnDistance);
                BlockPos sp = findClosestValidSpawn(level, tX, tZ, tY, w, h);
                if (sp != null) res.add(sp);
            }
        } else {
            List<ChunkPos> edgeChunks = new ArrayList<>(farm.edgeChunks());
            edgeChunks.sort((c1, c2) -> {
                int eX1 = c1.x * 16 + 8, eZ1 = c1.z * 16 + 8;
                int eX2 = c2.x * 16 + 8, eZ2 = c2.z * 16 + 8;
                double angle1 = Math.atan2(eZ1 - c.z, eX1 - c.x);
                double angle2 = Math.atan2(eZ2 - c.z, eX2 - c.x);
                return Double.compare(angle1, angle2);
            });

            int chunksCount = edgeChunks.size();
            double chunkStep = (double) chunksCount / max;
            for (int i = 0; i < max; i++) {
                int chunkIndex = (int) (i * chunkStep + rnd.nextDouble(chunkStep));
                chunkIndex = Math.min(chunkIndex, chunksCount - 1);
                ChunkPos ec = edgeChunks.get(chunkIndex);

                int eX = ec.x * 16 + 8, eZ = ec.z * 16 + 8;
                double dx = eX - c.x, dz = eZ - c.z;
                double dist = Math.sqrt(dx * dx + dz * dz);
                double dirX = dist < 1 ? Math.cos(rnd.nextDouble() * 2 * Math.PI) : dx / dist;
                double dirZ = dist < 1 ? Math.sin(rnd.nextDouble() * 2 * Math.PI) : dz / dist;
                double spawnDistance = rnd.nextDouble(48, 65);
                int tX = (int) (eX + dirX * spawnDistance);
                int tZ = (int) (eZ + dirZ * spawnDistance);

                if (CropScanningHandler.getChunkSafe(level, tX >> 4, tZ >> 4) == null) { tX = eX; tZ = eZ; }

                BlockPos sp = findClosestValidSpawn(level, tX + rnd.nextInt(-8, 9), tZ + rnd.nextInt(-8, 9), tY, w, h);
                if (sp != null) {
                    res.add(sp);
                }
            }
        }
        return res;
    }

    private static BlockPos findClosestValidSpawn(ServerLevel level, int x, int z, int tY, float w, float h) {
        int minY = level.getMinBuildHeight();
        int maxY = level.getMaxBuildHeight();
        BlockPos.MutableBlockPos m = new BlockPos.MutableBlockPos();
        for (int o = 0; o < 48; o++) {
            int yU = tY + o;
            if (yU < maxY) { m.set(x, yU, z); if (isValidSpawn(level, m, w, h)) return m.immutable(); }
            if (o > 0) {
                int yD = tY - o;
                if (yD >= minY) { m.set(x, yD, z); if (isValidSpawn(level, m, w, h)) return m.immutable(); }
            }
        }
        return null;
    }

    private static boolean isValidSpawn(ServerLevel level, BlockPos p, float w, float h) {
        if (!level.isLoaded(p)) return false;
        double hw = w / 2.0;
        int mX = Mth.floor(p.getX() - hw), MX = Mth.floor(p.getX() + hw);
        int mY = Mth.floor(p.getY()), MY = Mth.floor(p.getY() + h);
        int mZ = Mth.floor(p.getZ() - hw), MZ = Mth.floor(p.getZ() + hw);
        BlockPos.MutableBlockPos m = new BlockPos.MutableBlockPos();
        for (int x = mX; x <= MX; x++) for (int y = mY; y <= MY; y++) for (int z = mZ; z <= MZ; z++) {
            m.set(x, y, z);
            BlockState s = level.getBlockState(m);
            if (!s.getCollisionShape(level, m).isEmpty() || !s.getFluidState().isEmpty()) return false;
        }
        m.set(p.getX(), p.getY() - 1, p.getZ());
        return !level.getBlockState(m).getCollisionShape(level, m).isEmpty();
    }

    private static void updateHologramText(ArmorStand as, long ticksLeft) {
        long seconds = ticksLeft / 20;
        long minutes = seconds / 60;
        long secs = seconds % 60;
        String timeStr = String.format("%02d:%02d", minutes, secs);
        as.setCustomName(Component.literal(timeStr).withStyle(ChatFormatting.RED));
        as.setCustomNameVisible(true);
    }

    private static void notifyDetection(ServerLevel level) {
        Component msg = Component.literal("НЕАВТОРИЗОВАННОЕ ЗЕМЛЕДЕЛИЕ ОБНАРУЖЕНО").withStyle(ChatFormatting.RED, ChatFormatting.BOLD);
        level.getServer().getPlayerList().broadcastSystemMessage(msg, true);
    }

    private static void notifyRaidStart(ServerLevel level, int mobCount, int raidValue) {
        Component msg = Component.literal("НЕАВТОРИЗОВАННОЕ ЗЕМЛЕДЕЛИЕ ОБНАРУЖЕНО! РЕЙД НАЧАЛСЯ ("
                        + mobCount + " мобов, ценность: " + raidValue + ")")
                .withStyle(ChatFormatting.RED, ChatFormatting.BOLD);
        level.getServer().getPlayerList().broadcastSystemMessage(msg, true);
    }

    private static void notifyRaidEnd(ServerLevel level) {
        Component msg = Component.literal("РЕЙД ОТБИТ").withStyle(ChatFormatting.GREEN, ChatFormatting.BOLD);
        level.getServer().getPlayerList().broadcastSystemMessage(msg, true);
    }

    private static Vec3 getCenter(ArmorStand as) {
        if (as.getPersistentData().contains(NBT_CENTER_X)) {
            return new Vec3(as.getPersistentData().getDouble(NBT_CENTER_X), as.getPersistentData().getDouble(NBT_CENTER_Y), as.getPersistentData().getDouble(NBT_CENTER_Z));
        }
        return as.position();
    }

    private static void setCenter(ArmorStand as, Vec3 center) {
        as.getPersistentData().putDouble(NBT_CENTER_X, center.x);
        as.getPersistentData().putDouble(NBT_CENTER_Y, center.y);
        as.getPersistentData().putDouble(NBT_CENTER_Z, center.z);
    }

    private static long getTargetTime(ArmorStand as) { return as.getPersistentData().getLong(NBT_TARGET_TIME); }
    private static void setTargetTime(ArmorStand as, long time) { as.getPersistentData().putLong(NBT_TARGET_TIME, time); }
    private static int getLockedValue(ArmorStand as) { return as.getPersistentData().getInt(NBT_LOCKED_VALUE); }
    private static void setLockedValue(ArmorStand as, int val) { as.getPersistentData().putInt(NBT_LOCKED_VALUE, val); }
    private static boolean hasRaidScheduled(ArmorStand as) { return as.getPersistentData().contains(NBT_TARGET_TIME); }

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
        public int totalRaidValue;
        public int farmValue;
    }
}