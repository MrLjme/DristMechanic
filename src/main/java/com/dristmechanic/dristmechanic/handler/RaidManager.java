package com.dristmechanic.dristmechanic.handler;

import com.dristmechanic.dristmechanic.Dristmechanic;
import com.dristmechanic.dristmechanic.entity.TotebotEntity;
import com.dristmechanic.dristmechanic.init.ModAttachments;
import com.dristmechanic.dristmechanic.init.ModEntities;
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
import net.minecraft.world.level.chunk.LevelChunk;
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
    private static final String NBT_LOCKED_VALUE = "DristRaidLockedValue";
    private static final String NBT_CENTER_X = "DristRaidCenterX";
    private static final String NBT_CENTER_Y = "DristRaidCenterY";
    private static final String NBT_CENTER_Z = "DristRaidCenterZ";
    private static final String TAG_FARM_CENTER = "drist_farm_center";

    private static final long RAID_DELAY_TICKS = 1200L; // 1 минута
    private static final Map<ServerLevel, Set<UUID>> trackedStandsByLevel = new ConcurrentHashMap<>();

    @SubscribeEvent
    public static void onServerStopping(ServerStoppingEvent event) { trackedStandsByLevel.clear(); }

    @SubscribeEvent
    public static void onLevelUnload(LevelEvent.Unload event) {
        if (event.getLevel() instanceof ServerLevel sl) trackedStandsByLevel.remove(sl);
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
        for (ServerLevel level : event.getServer().getAllLevels()) tickRaids(level);
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

        // Стенд был просто таймером, удаляем его при начале рейда
        targetStand.discard();

        List<BlockPos> spawnPoints = findRaidSpawnPoints(level, farm, 5, 0.6F, 1.8F);
        if (spawnPoints.isEmpty()) {
            ThreadLocalRandom rnd = ThreadLocalRandom.current();
            for (int i = 0; i < 5; i++) {
                double angle = rnd.nextDouble() * 2 * Math.PI;
                double distance = rnd.nextDouble(32, 48);
                BlockPos pos = new BlockPos((int)(center.x + Math.cos(angle) * distance), (int)center.y, (int)(center.z + Math.sin(angle) * distance));
                spawnPoints.add(pos);
            }
        }

        int mobCount = Math.min(20, 5 + (farmValue / 1000));
        spawnMobs(level, spawnPoints, center, mobCount);
        notifyRaidStart(level, mobCount);
    }

    private static void spawnMobs(ServerLevel level, List<BlockPos> spawnPoints, Vec3 center, int count) {
        if (spawnPoints.isEmpty() || count <= 0) return;
        ThreadLocalRandom rnd = ThreadLocalRandom.current();
        BlockPos centerPos = new BlockPos((int)center.x, (int)center.y, (int)center.z);

        for (int i = 0; i < count; i++) {
            BlockPos pos = spawnPoints.get(rnd.nextInt(spawnPoints.size()));
            // Спавним твоего кастомного моба
            TotebotEntity mob = ModEntities.TOTEBOT.get().create(level);
            if (mob != null) {
                mob.moveTo(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5, rnd.nextFloat() * 360F, 0);
                mob.setPersistenceRequired();

                // Задаем цель бежать к центру огорода
                mob.setRaidTarget(centerPos);
                level.addFreshEntity(mob);
            }
        }
    }

    public static List<BlockPos> findRaidSpawnPoints(ServerLevel level, FarmManager.FarmData farm, int max, float w, float h) {
        List<BlockPos> res = new ArrayList<>();
        if (farm == null || farm.isEmpty() || farm.edgeChunks().isEmpty()) return res;

        Vec3 c = farm.center();
        int tY = Mth.floor(c.y);
        ThreadLocalRandom rnd = ThreadLocalRandom.current();

        boolean isSmallFarm = farm.edgeChunks().size() <= 4;
        List<BlockPos> allCandidates = new ArrayList<>();

        if (isSmallFarm) {
            for (int i = 0; i < max * 30 && allCandidates.size() < max * 2; i++) {
                double angle = rnd.nextDouble() * 2 * Math.PI;
                double spawnDistance = rnd.nextDouble(32, 48);
                int tX = (int) (c.x + Math.cos(angle) * spawnDistance);
                int tZ = (int) (c.z + Math.sin(angle) * spawnDistance);
                BlockPos sp = findClosestValidSpawn(level, tX, tZ, tY, w, h);
                if (sp != null) allCandidates.add(sp);
            }
        } else {
            Map<ChunkPos, List<BlockPos>> cands = new HashMap<>();
            Map<ChunkPos, Double> scores = new HashMap<>();

            for (ChunkPos ec : farm.edgeChunks()) {
                int eX = ec.x * 16 + 8, eZ = ec.z * 16 + 8;
                double dx = eX - c.x, dz = eZ - c.z;
                double dist = Math.sqrt(dx * dx + dz * dz);
                double dirX = dist < 1 ? Math.cos(rnd.nextDouble() * 2 * Math.PI) : dx / dist;
                double dirZ = dist < 1 ? Math.sin(rnd.nextDouble() * 2 * Math.PI) : dz / dist;
                double spawnDistance = rnd.nextDouble(48, 65);
                int tX = (int) (eX + dirX * spawnDistance);
                int tZ = (int) (eZ + dirZ * spawnDistance);

                if (CropScanningHandler.getChunkSafe(level, tX >> 4, tZ >> 4) == null) { tX = eX; tZ = eZ; }

                List<BlockPos> lc = new ArrayList<>();
                Set<Long> cs = new HashSet<>();
                double tYD = 0;

                for (int i = 0; i < max * 20 && lc.size() < max * 2; i++) {
                    BlockPos sp = findClosestValidSpawn(level, tX + rnd.nextInt(-16, 17), tZ + rnd.nextInt(-16, 17), tY, w, h);
                    if (sp != null && cs.add(sp.asLong())) { lc.add(sp); tYD += Math.abs(sp.getY() - tY); }
                }

                if (!lc.isEmpty()) { cands.put(ec, lc); scores.put(ec, tYD / lc.size()); }
            }

            if (cands.isEmpty()) return res;

            List<ChunkPos> vC = new ArrayList<>(cands.keySet());
            List<Double> wL = new ArrayList<>();
            double tW = 0;

            for (ChunkPos ch : vC) {
                double weight = 1.0 / (1.0 + scores.get(ch));
                wL.add(weight); tW += weight;
            }

            double r = rnd.nextDouble() * tW;
            double cum = 0;
            ChunkPos sel = vC.getFirst();

            for (int i = 0; i < vC.size(); i++) {
                cum += wL.get(i);
                if (r <= cum) { sel = vC.get(i); break; }
            }
            allCandidates.addAll(cands.get(sel));
        }

        Collections.shuffle(allCandidates, rnd);
        for (int i = 0; i < Math.min(max, allCandidates.size()); i++) res.add(allCandidates.get(i));
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

    private static void notifyRaidStart(ServerLevel level, int mobCount) {
        Component msg = Component.literal("НЕАВТОРИЗОВАННОЕ ЗЕМЛЕДЕЛИЕ ОБНАРУЖЕНО! РЕЙД НАЧАЛСЯ (" + mobCount + " мобов)").withStyle(ChatFormatting.RED, ChatFormatting.BOLD);
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
}