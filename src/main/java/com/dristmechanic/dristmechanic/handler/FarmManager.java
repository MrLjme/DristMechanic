package com.dristmechanic.dristmechanic.handler;

import com.dristmechanic.dristmechanic.Config;
import com.dristmechanic.dristmechanic.Dristmechanic;
import com.dristmechanic.dristmechanic.init.ModAttachments;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.server.ServerStartedEvent;
import net.neoforged.neoforge.event.server.ServerStoppingEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@EventBusSubscriber(modid = Dristmechanic.MODID)
public class FarmManager {
    private static final Logger LOGGER = LoggerFactory.getLogger(Dristmechanic.MODID);

    private static MinecraftServer serverInstance = null;
    private static final Map<ServerLevel, Map<ChunkPos, FarmData>> farmsByLevel = new ConcurrentHashMap<>();
    private final Set<BlockPos> spentCrops = new HashSet<>();

    @SubscribeEvent
    public static void onServerStarted(ServerStartedEvent e) {
        serverInstance = e.getServer();
    }

    public boolean isSpentCrop(BlockPos pos) {
        return spentCrops.contains(pos);
    }

    public void removeSpentCrop(BlockPos pos) {
        spentCrops.remove(pos);
    }

    @SubscribeEvent
    public static void onServerStopping(ServerStoppingEvent e) {
        serverInstance = null;
        farmsByLevel.clear();
    }

    public static void onCropPlanted(ServerLevel level, BlockPos pos) {
        ChunkPos chunkPos = new ChunkPos(pos);
        Map<ChunkPos, FarmData> farms = farmsByLevel.computeIfAbsent(level, k -> new ConcurrentHashMap<>());

        FarmData farm = findOrCreateFarm(farms, chunkPos);
        if (farm != null) {
            if (farm.isSpentCrop(pos)) {
                return;
            }
            if (farm.containsRawCrop(pos)) {
                return;
            }
            farm.addRawCrop(pos, level.getGameTime());
            farm.resetStabilityTimer();
        }
    }

    public static void onCropRemoved(ServerLevel level, BlockPos pos) {
        ChunkPos chunkPos = new ChunkPos(pos);
        Map<ChunkPos, FarmData> farms = farmsByLevel.get(level);

        if (farms != null) {
            for (FarmData farm : farms.values()) {
                if (farm.containsRawCrop(pos)) {
                    farm.removeRawCrop(pos);
                    int guiValue = farm.getGuiValue(level);
                    RaidManager.syncFarmData(level, farm, guiValue);
                    return;
                }
                if (farm.isSpentCrop(pos)) {
                    farm.removeSpentCrop(pos);
                    return;
                }
            }
        }
    }

    private static FarmData findOrCreateFarm(Map<ChunkPos, FarmData> farms, ChunkPos startChunk) {
        FarmData existing = farms.get(startChunk);
        if (existing != null) {
            return existing;
        }

        for (int dx = -1; dx <= 1; dx++) {
            for (int dz = -1; dz <= 1; dz++) {
                if (dx == 0 && dz == 0) continue;
                ChunkPos neighbor = new ChunkPos(startChunk.x + dx, startChunk.z + dz);
                FarmData farm = farms.get(neighbor);
                if (farm != null) {
                    farms.put(startChunk, farm);
                    farm.addChunk(startChunk); // <-- добавляем чанк к ферме
                    return farm;
                }
            }
        }

        LOGGER.info("[FARM_MANAGER] Creating new farm at chunk {}", startChunk);
        FarmData newFarm = new FarmData(startChunk);
        newFarm.addChunk(startChunk); // <-- добавляем при создании
        farms.put(startChunk, newFarm);
        return newFarm;
    }

    @SubscribeEvent
    public static void onServerTick(ServerTickEvent.Post event) {
        if (serverInstance == null) return;

        for (ServerLevel level : serverInstance.getAllLevels()) {
            Map<ChunkPos, FarmData> farms = farmsByLevel.get(level);
            if (farms != null) {
                Set<FarmData> uniqueFarms = Collections.newSetFromMap(new IdentityHashMap<>());
                uniqueFarms.addAll(farms.values());

                for (FarmData farm : uniqueFarms) {
                    farm.tick(level);
                }

                for (FarmData farm : uniqueFarms) {
                    if (farm.isEmpty() && !farm.isRaidActive()) {
                        farm.cleanup(level);
                        RaidManager.syncToClients(level, false, 0, Config.MAX_CROP_VALUE.get(), false);
                    }
                }

                farms.entrySet().removeIf(entry -> entry.getValue().isEmpty() && !entry.getValue().isRaidActive());
            }
        }
    }

    public static Map<ChunkPos, FarmData> getFarmsForLevel(ServerLevel level) {
        return farmsByLevel.get(level);
    }

    public static class FarmData {
        private final ChunkPos mainChunk;
        private final Set<ChunkPos> chunks = new HashSet<>(); // <-- ВСЕ чанки фермы
        private final Map<BlockPos, Long> rawCrops = new HashMap<>();
        private final Set<BlockPos> spentCrops = new HashSet<>();
        private int accumulatedValue = 0;
        private long stabilityTimer = 0;
        private long raidCountdown = 0;
        private boolean raidActive = false;
        private boolean mobsSpawned = false;
        private final Set<UUID> spawnedMobs = new HashSet<>();
        private UUID hologramUUID = null;
        private Vec3 farmCenter = null;

        public FarmData(ChunkPos mainChunk) {
            this.mainChunk = mainChunk;
        }

        public void addChunk(ChunkPos chunk) {
            chunks.add(chunk);
        }

        public Set<ChunkPos> getChunks() {
            return Collections.unmodifiableSet(chunks);
        }

        public void addRawCrop(BlockPos pos, long currentTick) {
            rawCrops.put(pos, currentTick);
        }

        public void removeRawCrop(BlockPos pos) {
            rawCrops.remove(pos);
        }

        public boolean containsRawCrop(BlockPos pos) {
            return rawCrops.containsKey(pos);
        }

        public int getRawCropCount() {
            return rawCrops.size();
        }

        public boolean isSpentCrop(BlockPos pos) {
            return spentCrops.contains(pos);
        }

        public void removeSpentCrop(BlockPos pos) {
            spentCrops.remove(pos);
        }

        public void resetStabilityTimer() {
            stabilityTimer = 0;
        }

        public void tick(ServerLevel level) {
            if (raidActive) {
                if (level.getGameTime() % 20 == 0) {
                    int guiValue = getGuiValue(level);
                    RaidManager.syncFarmData(level, this, guiValue);
                }

                if (mobsSpawned && spawnedMobs.isEmpty()) {
                    LOGGER.info("[FARM_TICK] All mobs dead, ending raid");
                    raidActive = false;
                    mobsSpawned = false;
                    accumulatedValue = 0;
                    rawCrops.clear();
                    spentCrops.clear();
                    unlockAllChunks(level);
                    RaidManager.onRaidComplete(level);
                }
                return;
            }

            if (level.getGameTime() % 20 == 0) {
                int guiValue = getGuiValue(level);
                RaidManager.syncFarmData(level, this, guiValue);
            }

            if (raidCountdown > 0) {
                raidCountdown--;
                if (level.getGameTime() % 20 == 0) {
                    updateHologram(level);
                }
                if (raidCountdown <= 0) {
                    startRaid(level);
                }
                return;
            }

            if (!rawCrops.isEmpty()) {
                stabilityTimer++;
                if (stabilityTimer >= Config.STABILITY_DELAY_TICKS.get()) {
                    extractValue(level);
                }
            }
        }

        public int getGuiValue(ServerLevel level) {
            if (raidActive) {
                return RaidManager.getDisplayValue(level);
            }

            int rawValue = 0;
            for (BlockPos pos : rawCrops.keySet()) {
                BlockState state = level.getBlockState(pos);
                rawValue += CropScanningHandler.getCropValue(state);
            }

            return accumulatedValue + rawValue;
        }

        private void extractValue(ServerLevel level) {
            int totalValue = 0;
            Vec3 center = calculateCenter(level);

            for (BlockPos pos : rawCrops.keySet()) {
                BlockState state = level.getBlockState(pos);
                int value = CropScanningHandler.getCropValue(state);
                totalValue += value;
                spentCrops.add(pos);
            }

            accumulatedValue += totalValue;
            rawCrops.clear();
            stabilityTimer = 0;

            if (center != null) {
                farmCenter = center;
            }

            LOGGER.info("[EXTRACT] Extracted {} value, total accumulated: {}, center: {}", totalValue, accumulatedValue, farmCenter);

            if (accumulatedValue > 0 && raidCountdown <= 0) {
                raidCountdown = Config.RAID_COUNTDOWN_TICKS.get();

                long lockDuration = Config.RAID_COUNTDOWN_TICKS.get() + 24000L;
                lockAllChunks(level, level.getGameTime() + lockDuration);

                spawnHologram(level);
                RaidManager.onRaidScheduled(level, this, accumulatedValue);
            }
        }

        private Vec3 calculateCenter(ServerLevel level) {
            if (rawCrops.isEmpty()) return null;

            double sumX = 0, sumY = 0, sumZ = 0;
            int count = 0;

            for (BlockPos pos : rawCrops.keySet()) {
                sumX += pos.getX() + 0.5;
                sumY += pos.getY() + 1.5;
                sumZ += pos.getZ() + 0.5;
                count++;
            }

            if (count == 0) return null;

            return new Vec3(sumX / count, sumY / count, sumZ / count);
        }

        private void spawnHologram(ServerLevel level) {
            if (farmCenter == null) return;

            ArmorStand stand = EntityType.ARMOR_STAND.create(level);
            if (stand != null) {
                stand.setPos(farmCenter.x, farmCenter.y, farmCenter.z);
                stand.setInvisible(true);
                stand.setNoGravity(true);
                stand.setInvulnerable(true);
                stand.setCustomNameVisible(true);
                stand.setNoBasePlate(true);

                long seconds = raidCountdown / 20;
                long minutes = seconds / 60;
                long secs = seconds % 60;
                String timeStr = String.format("%02d:%02d", minutes, secs);
                stand.setCustomName(Component.literal(timeStr).withStyle(ChatFormatting.RED, ChatFormatting.BOLD));

                level.addFreshEntity(stand);
                hologramUUID = stand.getUUID();
                LOGGER.info("[HOLOGRAM] Spawned at {}", farmCenter);
            }
        }

        private void updateHologram(ServerLevel level) {
            if (hologramUUID == null) return;
            Entity entity = level.getEntity(hologramUUID);
            if (entity instanceof ArmorStand stand && stand.isAlive()) {
                long seconds = raidCountdown / 20;
                long minutes = seconds / 60;
                long secs = seconds % 60;
                String timeStr = String.format("%02d:%02d", minutes, secs);
                stand.setCustomName(Component.literal(timeStr).withStyle(ChatFormatting.RED, ChatFormatting.BOLD));
            }
        }

        private void removeHologram(ServerLevel level) {
            if (hologramUUID == null) return;
            Entity entity = level.getEntity(hologramUUID);
            if (entity != null && entity.isAlive()) {
                entity.discard();
            }
            hologramUUID = null;
        }

        public void cleanup(ServerLevel level) {
            removeHologram(level);
        }

        private void startRaid(ServerLevel level) {
            raidActive = true;
            mobsSpawned = false;
            removeHologram(level);
            LOGGER.info("[RAID_START] Starting raid with accumulated value: {}", accumulatedValue);
            RaidManager.executeRaid(level, this, accumulatedValue, farmCenter);
        }

        // ========== ИСПРАВЛЕННАЯ БЛОКИРОВКА: вокруг ВСЕХ чанков фермы ==========

        private void lockAllChunks(ServerLevel level, long until) {
            int radius = Config.LOCKDOWN_RADIUS.get();
            for (ChunkPos farmChunk : chunks) {
                for (int dx = -radius; dx <= radius; dx++) {
                    for (int dz = -radius; dz <= radius; dz++) {
                        ChunkPos chunkPos = new ChunkPos(farmChunk.x + dx, farmChunk.z + dz);
                        CropScanningHandler.lockChunk(level, chunkPos, until);
                    }
                }
            }
            LOGGER.info("[LOCK] Locked {} farm chunks (+{} radius each), total unique chunks affected", chunks.size(), radius);
        }

        private void unlockAllChunks(ServerLevel level) {
            int radius = Config.LOCKDOWN_RADIUS.get();
            for (ChunkPos farmChunk : chunks) {
                for (int dx = -radius; dx <= radius; dx++) {
                    for (int dz = -radius; dz <= radius; dz++) {
                        ChunkPos chunkPos = new ChunkPos(farmChunk.x + dx, farmChunk.z + dz);
                        CropScanningHandler.unlockChunk(level, chunkPos);
                    }
                }
            }
            LOGGER.info("[UNLOCK] Unlocked {} farm chunks (+{} radius each)", chunks.size(), radius);
        }

        // =====================================================================

        public boolean isEmpty() {
            return rawCrops.isEmpty() && accumulatedValue == 0 && !raidActive;
        }

        public boolean isRaidActive() {
            return raidActive;
        }

        public int getAccumulatedValue() {
            return accumulatedValue;
        }

        public ChunkPos getMainChunk() {
            return mainChunk;
        }

        public void addSpawnedMob(UUID mobUUID) {
            spawnedMobs.add(mobUUID);
            mobsSpawned = true;
        }

        public void removeSpawnedMob(UUID mobUUID) {
            spawnedMobs.remove(mobUUID);
        }

        public int getSpawnedMobCount() {
            return spawnedMobs.size();
        }

        public void setAccumulatedValue(int value) {
            this.accumulatedValue = value;
        }

        public Vec3 getFarmCenter() {
            return farmCenter;
        }

        public long getRaidCountdown() {
            return raidCountdown;
        }

        public int getSpentCropCount() {
            return spentCrops.size();
        }
    }
}