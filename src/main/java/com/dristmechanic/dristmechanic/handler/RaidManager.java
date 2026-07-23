package com.dristmechanic.dristmechanic.handler;

import com.dristmechanic.dristmechanic.Dristmechanic;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.neoforged.neoforge.event.level.LevelEvent;
import net.neoforged.neoforge.event.server.ServerStoppingEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadLocalRandom;

@EventBusSubscriber(modid = Dristmechanic.MODID)
public class RaidManager {

    private static final String NBT_TARGET_TIME = "DristRaidTargetTime";
    private static final String NBT_REMOVE_TIME = "DristRaidRemoveTime";
    private static final String NBT_LOCKED_VALUE = "DristRaidLockedValue";
    private static final String TAG_FARM_CENTER = "drist_farm_center";

    private static final Map<ServerLevel, Set<UUID>> trackedStandsByLevel = new ConcurrentHashMap<>();
    private static final Map<ServerLevel, Set<Long>> lastKnownFarmIds = new ConcurrentHashMap<>();

    @SubscribeEvent
    public static void onServerStopping(ServerStoppingEvent event) {
        trackedStandsByLevel.clear();
        lastKnownFarmIds.clear();
    }

    @SubscribeEvent
    public static void onLevelUnload(LevelEvent.Unload event) {
        if (event.getLevel() instanceof ServerLevel sl) {
            trackedStandsByLevel.remove(sl);
            lastKnownFarmIds.remove(sl);
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
    public static void onServerTick(ServerTickEvent.Post event) {
        for (ServerLevel level : event.getServer().getAllLevels()) {
            tickRaids(level);
        }
    }

    public static void updateHolograms(ServerLevel level, List<FarmManager.FarmData> stableFarms) {
        if (stableFarms == null || stableFarms.isEmpty()) {
            lastKnownFarmIds.put(level, new HashSet<>());
            return;
        }

        Set<Long> currentIds = new HashSet<>();
        for (FarmManager.FarmData farm : stableFarms) {
            currentIds.add(getFarmId(farm));
        }

        Set<Long> prevIds = lastKnownFarmIds.getOrDefault(level, new HashSet<>());

        // Спавним стенды ТОЛЬКО для новых ферм, которых не было в предыдущем слепке
        for (FarmManager.FarmData farm : stableFarms) {
            if (!prevIds.contains(getFarmId(farm))) {
                if (!hasStandNearby(level, farm.center())) {
                    spawnStandForFarm(level, farm);
                }
            }
        }

        lastKnownFarmIds.put(level, currentIds);
    }

    private static long getFarmId(FarmManager.FarmData farm) {
        Vec3 c = farm.center();
        long x = Mth.floor(c.x);
        long z = Mth.floor(c.z);
        return (x << 32) | (z & 0xFFFFFFFFL);
    }

    private static boolean hasStandNearby(ServerLevel level, Vec3 center) {
        Set<UUID> tracked = trackedStandsByLevel.get(level);
        if (tracked == null) return false;

        double searchDistSq = 16.0 * 16.0;
        for (UUID uuid : tracked) {
            Entity e = level.getEntity(uuid);
            if (e instanceof ArmorStand as && as.isAlive()) {
                if (as.distanceToSqr(center.x, center.y, center.z) < searchDistSq) {
                    return true;
                }
            }
        }
        return false;
    }

    private static void spawnStandForFarm(ServerLevel level, FarmManager.FarmData farm) {
        Vec3 center = farm.center();
        ArmorStand stand = createStand(level, center);

        if (stand != null) {
            long nextMidnight = calculateNextMidnight(level);

            // Утро (24000) всегда наступает через 6000 тиков после полуночи (18000).
            // Это гарантирует, что стенд удалится утром ПОСЛЕ рейда, когда бы он ни был создан.
            long nextRemove = nextMidnight + 6000L;

            setTargetTime(stand, nextMidnight);
            setRemoveTime(stand, nextRemove);
            setLockedValue(stand, farm.totalValue());
        }
    }

    private static void tickRaids(ServerLevel level) {
        Set<UUID> tracked = trackedStandsByLevel.get(level);
        if (tracked == null || tracked.isEmpty()) return;

        long currentTime = level.getGameTime();

        tracked.removeIf(uuid -> {
            Entity entity = level.getEntity(uuid);
            if (!(entity instanceof ArmorStand as) || !as.isAlive()) {
                return true;
            }

            // 1. Скрытый таймер: Удаление стенда (утро)
            if (as.getPersistentData().contains(NBT_REMOVE_TIME)) {
                long removeTime = as.getPersistentData().getLong(NBT_REMOVE_TIME);
                if (currentTime >= removeTime) {
                    as.discard();
                    return true; // Удаляем из tracked
                }
            }

            // 2. Видимый таймер: Рейд (полночь)
            if (as.getPersistentData().contains(NBT_TARGET_TIME)) {
                long targetTime = as.getPersistentData().getLong(NBT_TARGET_TIME);
                long timeLeft = targetTime - currentTime;

                if (timeLeft <= 0) {
                    executeRaid(level, as, getLockedValue(as));
                    as.getPersistentData().remove(NBT_TARGET_TIME);
                    hideCounterText(as); // Текст скрывается, стенд остается ждать утра
                } else if (currentTime % 20 == 0) {
                    updateHologramText(as, timeLeft);
                }
            }

            return false;
        });
    }

    private static void hideCounterText(ArmorStand as) {
        as.setCustomNameVisible(false);
    }

    private static void clearAllStands(ServerLevel level) {
        Set<UUID> tracked = trackedStandsByLevel.get(level);
        if (tracked == null || tracked.isEmpty()) return;

        for (UUID uuid : tracked) {
            Entity e = level.getEntity(uuid);
            if (e instanceof ArmorStand as && as.isAlive()) {
                as.discard();
            }
        }

        tracked.clear();
    }

    public static void clearAllHolograms(ServerLevel level) {
        clearAllStands(level);
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
        ChunkPos asChunk = new ChunkPos(targetStand.blockPosition());
        FarmManager.FarmData farm = FarmManager.findConnectedFarm(level, asChunk);

        List<BlockPos> spawnPoints = findRaidSpawnPoints(level, farm, 5, 0.6F, 1.8F);

        int mobCount = Math.min(20, 5 + (farmValue / 1000));

        spawnMobs(level, spawnPoints, targetStand, mobCount);
    }

    private static void spawnMobs(ServerLevel level, List<BlockPos> spawnPoints, ArmorStand target, int count) {
        if (spawnPoints.isEmpty() || count <= 0) return;

        ThreadLocalRandom rnd = ThreadLocalRandom.current();

        for (int i = 0; i < count; i++) {
            BlockPos pos = spawnPoints.get(rnd.nextInt(spawnPoints.size()));

            EntityType<?> type = rnd.nextBoolean() ? EntityType.ZOMBIE : EntityType.SKELETON;

            Entity entity = type.create(level);

            if (entity instanceof Mob mob) {
                mob.moveTo(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5, rnd.nextFloat() * 360F, 0);
                mob.setPersistenceRequired();
                level.addFreshEntity(mob);
                mob.setTarget(target);
            }
        }
    }

    public static List<BlockPos> findRaidSpawnPoints(ServerLevel level, FarmManager.FarmData farm, int max, float w, float h) {
        List<BlockPos> res = new ArrayList<>();

        if (farm == null || farm.isEmpty() || farm.edgeChunks().isEmpty()) return res;

        Vec3 c = farm.center();
        int tY = Mth.floor(c.y);
        ThreadLocalRandom rnd = ThreadLocalRandom.current();

        // Считаем ферму "маленькой", если она занимает 1-2 чанка
        // (количество граничных чанков <= 4)
        boolean isSmallFarm = farm.edgeChunks().size() <= 4;

        List<BlockPos> allCandidates = new ArrayList<>();

        if (isSmallFarm) {
            // ЛОГИКА ДЛЯ МАЛЕНЬКИХ ФЕРМ: Круговой спавн от центра
            // Пытаемся найти точки равномерно во всех направлениях
            for (int i = 0; i < max * 30 && allCandidates.size() < max * 2; i++) {
                double angle = rnd.nextDouble() * 2 * Math.PI; // Случайный угол (0 - 360 градусов)
                double spawnDistance = rnd.nextDouble(32, 48);

                int tX = (int) (c.x + Math.cos(angle) * spawnDistance);
                int tZ = (int) (c.z + Math.sin(angle) * spawnDistance);

                BlockPos sp = findClosestValidSpawn(level, tX, tZ, tY, w, h);
                if (sp != null) {
                    allCandidates.add(sp);
                }
            }
        } else {
            // ЛОГИКА ДЛЯ БОЛЬШИХ ФЕРМ: Спавн от границ наружу
            Map<ChunkPos, List<BlockPos>> cands = new HashMap<>();
            Map<ChunkPos, Double> scores = new HashMap<>();

            for (ChunkPos ec : farm.edgeChunks()) {
                int eX = ec.x * 16 + 8, eZ = ec.z * 16 + 8;

                double dx = eX - c.x, dz = eZ - c.z;
                double dist = Math.sqrt(dx * dx + dz * dz);

                double dirX = dist < 1 ? Math.cos(rnd.nextDouble() * 2 * Math.PI) : dx / dist;
                double dirZ = dist < 1 ? Math.sin(rnd.nextDouble() * 2 * Math.PI) : dz / dist;

                double spawnDistance = rnd.nextDouble(32, 48);

                int tX = (int) (eX + dirX * spawnDistance);
                int tZ = (int) (eZ + dirZ * spawnDistance);

                if (CropScanningHandler.getChunkSafe(level, tX >> 4, tZ >> 4) == null) {
                    tX = eX;
                    tZ = eZ;
                }

                List<BlockPos> lc = new ArrayList<>();
                Set<Long> cs = new HashSet<>();
                double tYD = 0;

                for (int i = 0; i < max * 20 && lc.size() < max * 2; i++) {
                    BlockPos sp = findClosestValidSpawn(level, tX + rnd.nextInt(-16, 17), tZ + rnd.nextInt(-16, 17), tY, w, h);
                    if (sp != null && cs.add(sp.asLong())) {
                        lc.add(sp);
                        tYD += Math.abs(sp.getY() - tY);
                    }
                }

                if (!lc.isEmpty()) {
                    cands.put(ec, lc);
                    scores.put(ec, tYD / lc.size());
                }
            }

            if (cands.isEmpty()) return res;

            List<ChunkPos> vC = new ArrayList<>(cands.keySet());
            List<Double> wL = new ArrayList<>();
            double tW = 0;

            for (ChunkPos ch : vC) {
                double weight = 1.0 / (1.0 + scores.get(ch));
                wL.add(weight);
                tW += weight;
            }

            double r = rnd.nextDouble() * tW;
            double cum = 0;
            ChunkPos sel = vC.getFirst();

            for (int i = 0; i < vC.size(); i++) {
                cum += wL.get(i);
                if (r <= cum) {
                    sel = vC.get(i);
                    break;
                }
            }

            allCandidates.addAll(cands.get(sel));
        }

        // Финальное перемешивание и ограничение количества точек
        Collections.shuffle(allCandidates, rnd);
        for (int i = 0; i < Math.min(max, allCandidates.size()); i++) {
            res.add(allCandidates.get(i));
        }

        return res;
    }

    private static BlockPos findClosestValidSpawn(ServerLevel level, int x, int z, int tY, float w, float h) {
        int minY = level.getMinBuildHeight();
        int maxY = level.getMaxBuildHeight();

        BlockPos.MutableBlockPos m = new BlockPos.MutableBlockPos();

        for (int o = 0; o < 48; o++) {
            int yU = tY + o;
            if (yU < maxY) {
                m.set(x, yU, z);
                if (isValidSpawn(level, m, w, h)) return m.immutable();
            }

            if (o > 0) {
                int yD = tY - o;
                if (yD >= minY) {
                    m.set(x, yD, z);
                    if (isValidSpawn(level, m, w, h)) return m.immutable();
                }
            }
        }

        return null;
    }

    private static boolean isValidSpawn(ServerLevel level, BlockPos p, float w, float h) {
        if (!level.isLoaded(p)) return false;

        double hw = w / 2.0;
        int mX = Mth.floor(p.getX() - hw);
        int MX = Mth.floor(p.getX() + hw);
        int mY = Mth.floor(p.getY());
        int MY = Mth.floor(p.getY() + h);
        int mZ = Mth.floor(p.getZ() - hw);
        int MZ = Mth.floor(p.getZ() + hw);

        BlockPos.MutableBlockPos m = new BlockPos.MutableBlockPos();

        for (int x = mX; x <= MX; x++) {
            for (int y = mY; y <= MY; y++) {
                for (int z = mZ; z <= MZ; z++) {
                    m.set(x, y, z);
                    BlockState s = level.getBlockState(m);

                    if (!s.getCollisionShape(level, m).isEmpty() || !s.getFluidState().isEmpty()) return false;
                }
            }
        }

        m.set(p.getX(), p.getY() - 1, p.getZ());

        return !level.getBlockState(m).getCollisionShape(level, m).isEmpty();
    }

    private static void updateHologramText(ArmorStand as, long ticksLeft) {
        long seconds = ticksLeft / 20;
        long hours = seconds / 3600;
        long minutes = (seconds % 3600) / 60;
        long secs = seconds % 60;

        String timeStr = (hours > 0)
                ? String.format("%d:%02d:%02d", hours, minutes, secs)
                : String.format("%02d:%02d", minutes, secs);

        as.setCustomName(Component.literal(timeStr).withStyle(ChatFormatting.RED));
        as.setCustomNameVisible(true);
    }

    private static long calculateNextMidnight(ServerLevel level) {
        long currentTime = level.getGameTime();
        long dayTime = level.getDayTime() % 24000L;

        long ticksUntilMidnight = (dayTime < 18000L) ? (18000L - dayTime) : ((24000L - dayTime) + 18000L);

        return currentTime + ticksUntilMidnight;
    }

    private static long getTargetTime(ArmorStand as) { return as.getPersistentData().getLong(NBT_TARGET_TIME); }
    private static void setTargetTime(ArmorStand as, long time) { as.getPersistentData().putLong(NBT_TARGET_TIME, time); }
    private static long getRemoveTime(ArmorStand as) { return as.getPersistentData().getLong(NBT_REMOVE_TIME); }
    private static void setRemoveTime(ArmorStand as, long time) { as.getPersistentData().putLong(NBT_REMOVE_TIME, time); }
    private static int getLockedValue(ArmorStand as) { return as.getPersistentData().getInt(NBT_LOCKED_VALUE); }
    private static void setLockedValue(ArmorStand as, int val) { as.getPersistentData().putInt(NBT_LOCKED_VALUE, val); }
    private static boolean hasRaidScheduled(ArmorStand as) { return as.getPersistentData().contains(NBT_TARGET_TIME); }
    private static void clearRaidData(ArmorStand as) {
        as.getPersistentData().remove(NBT_TARGET_TIME);
        as.getPersistentData().remove(NBT_REMOVE_TIME);
        as.getPersistentData().remove(NBT_LOCKED_VALUE);
    }
}