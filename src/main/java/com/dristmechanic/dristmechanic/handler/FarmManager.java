package com.dristmechanic.dristmechanic.handler;

import com.dristmechanic.dristmechanic.Config;
import com.dristmechanic.dristmechanic.Dristmechanic;
import com.dristmechanic.dristmechanic.init.ModAttachments;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.IntArrayTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.saveddata.SavedData;
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
import net.minecraft.core.HolderLookup;

@EventBusSubscriber(modid = Dristmechanic.MODID)
public class FarmManager {

    private static final Logger LOGGER = LoggerFactory.getLogger(Dristmechanic.MODID);
    private static MinecraftServer serverInstance = null;
    private static final Map<ServerLevel, Map<ChunkPos, FarmData>> farmsByLevel = new ConcurrentHashMap<>();

    // ========== SAVED DATA ==========

    public static class FarmSavedData extends SavedData {

        private static final String DATA_NAME = Dristmechanic.MODID + "_farms";

        private final Map<UUID, FarmEntry> farms = new HashMap<>();

        public static class FarmEntry {
            public UUID farmId;
            public int accumulatedValue;
            public long raidCountdown;
            public boolean raidActive;
            public boolean mobsSpawned;
            public double centerX, centerY, centerZ;
            public List<String> chunks = new ArrayList<>();
            public Set<UUID> spawnedMobs = new HashSet<>();
            public UUID hologramUUID;

            public FarmEntry(UUID farmId) {
                this.farmId = farmId;
            }
        }

        public FarmSavedData() {}

        public static FarmSavedData get(ServerLevel level) {
            MinecraftServer server = level.getServer();
            return server.overworld().getDataStorage()
                    .computeIfAbsent(new SavedData.Factory<FarmSavedData>(
                            FarmSavedData::new,
                            FarmSavedData::load
                    ), DATA_NAME);
        }

        public static FarmSavedData load(CompoundTag tag, HolderLookup.Provider registries) {
            FarmSavedData data = new FarmSavedData();

            ListTag farmsTag = tag.getList("farms", Tag.TAG_COMPOUND);

            for (int i = 0; i < farmsTag.size(); i++) {
                CompoundTag farmTag = farmsTag.getCompound(i);

                UUID farmId = farmTag.getUUID("farmId");
                FarmEntry entry = new FarmEntry(farmId);

                entry.accumulatedValue = farmTag.getInt("accumulatedValue");
                entry.raidCountdown = farmTag.getLong("raidCountdown");
                entry.raidActive = farmTag.getBoolean("raidActive");
                entry.mobsSpawned = farmTag.getBoolean("mobsSpawned");
                entry.centerX = farmTag.getDouble("centerX");
                entry.centerY = farmTag.getDouble("centerY");
                entry.centerZ = farmTag.getDouble("centerZ");
                entry.hologramUUID = farmTag.hasUUID("hologramUUID") ? farmTag.getUUID("hologramUUID") : null;

                ListTag chunksTag = farmTag.getList("chunks", Tag.TAG_STRING);
                for (int j = 0; j < chunksTag.size(); j++) {
                    entry.chunks.add(chunksTag.getString(j));
                }

                ListTag mobsTag = farmTag.getList("spawnedMobs", Tag.TAG_INT_ARRAY);
                for (int j = 0; j < mobsTag.size(); j++) {
                    int[] uuidArray = mobsTag.getIntArray(j);
                    if (uuidArray.length == 4) {
                        UUID mobUUID = new UUID(
                                ((long) uuidArray[0] << 32) | (uuidArray[1] & 0xFFFFFFFFL),
                                ((long) uuidArray[2] << 32) | (uuidArray[3] & 0xFFFFFFFFL)
                        );
                        entry.spawnedMobs.add(mobUUID);
                    }
                }

                data.farms.put(farmId, entry);
            }

            return data;
        }

        @Override
        public CompoundTag save(CompoundTag tag, HolderLookup.Provider registries) {
            ListTag farmsTag = new ListTag();

            for (FarmEntry entry : farms.values()) {
                CompoundTag farmTag = new CompoundTag();

                farmTag.putUUID("farmId", entry.farmId);
                farmTag.putInt("accumulatedValue", entry.accumulatedValue);
                farmTag.putLong("raidCountdown", entry.raidCountdown);
                farmTag.putBoolean("raidActive", entry.raidActive);
                farmTag.putBoolean("mobsSpawned", entry.mobsSpawned);
                farmTag.putDouble("centerX", entry.centerX);
                farmTag.putDouble("centerY", entry.centerY);
                farmTag.putDouble("centerZ", entry.centerZ);

                if (entry.hologramUUID != null) {
                    farmTag.putUUID("hologramUUID", entry.hologramUUID);
                }

                ListTag chunksTag = new ListTag();
                for (String chunk : entry.chunks) {
                    chunksTag.add(StringTag.valueOf(chunk));
                }
                farmTag.put("chunks", chunksTag);

                ListTag mobsTag = new ListTag();
                for (UUID mobUUID : entry.spawnedMobs) {
                    int[] uuidArray = new int[] {
                            (int) (mobUUID.getMostSignificantBits() >> 32),
                            (int) mobUUID.getMostSignificantBits(),
                            (int) (mobUUID.getLeastSignificantBits() >> 32),
                            (int) mobUUID.getLeastSignificantBits()
                    };
                    mobsTag.add(new IntArrayTag(uuidArray));
                }
                farmTag.put("spawnedMobs", mobsTag);

                farmsTag.add(farmTag);
            }

            tag.put("farms", farmsTag);
            return tag;
        }

        public void saveFarm(FarmEntry entry) {
            farms.put(entry.farmId, entry);
            setDirty();
        }

        public FarmEntry getFarm(UUID farmId) {
            return farms.get(farmId);
        }

        public void removeFarm(UUID farmId) {
            farms.remove(farmId);
            setDirty();
        }

        public Collection<FarmEntry> getAllFarms() {
            return farms.values();
        }
    }

    // ========== ОСНОВНАЯ ЛОГИКА ==========

    @SubscribeEvent
    public static void onServerStarted(ServerStartedEvent e) {
        serverInstance = e.getServer();
        CropScanningHandler.initCache();

        for (ServerLevel level : e.getServer().getAllLevels()) {
            restoreFarmsFromSavedData(level);
        }
    }

    @SubscribeEvent
    public static void onServerStopping(ServerStoppingEvent e) {
        serverInstance = null;
        farmsByLevel.clear();
    }

    private static void restoreFarmsFromSavedData(ServerLevel level) {
        FarmSavedData savedData = FarmSavedData.get(level);

        for (FarmSavedData.FarmEntry entry : savedData.getAllFarms()) {
            if (entry.raidActive || entry.raidCountdown > 0) {
                ChunkPos mainChunk = new ChunkPos((int) entry.centerX, (int) entry.centerZ);

                FarmData farm = new FarmData(mainChunk, entry.farmId);
                farm.raidCountdown = entry.raidCountdown;
                farm.raidActive = entry.raidActive;
                farm.accumulatedValue = entry.accumulatedValue;
                farm.mobsSpawned = entry.mobsSpawned;
                farm.farmCenter = new Vec3(entry.centerX, entry.centerY, entry.centerZ);
                farm.hologramUUID = entry.hologramUUID;

                for (String chunkStr : entry.chunks) {
                    try {
                        String[] parts = chunkStr.split(",");
                        int cx = Integer.parseInt(parts[0]);
                        int cz = Integer.parseInt(parts[1]);
                        farm.addChunk(new ChunkPos(cx, cz));
                    } catch (Exception ignored) {}
                }

                Map<ChunkPos, FarmData> farms = farmsByLevel.computeIfAbsent(level, k -> new ConcurrentHashMap<>());
                for (ChunkPos chunk : farm.getChunks()) {
                    farms.put(chunk, farm);
                }

                if (entry.raidActive) {
                    RaidManager.restoreRaid(level, farm, entry);
                }
            }
        }
    }

    public static void onCropPlanted(ServerLevel level, BlockPos pos, int value) {
        ChunkPos chunkPos = new ChunkPos(pos);
        Map<ChunkPos, FarmData> farms = farmsByLevel.computeIfAbsent(level, k -> new ConcurrentHashMap<>());
        FarmData farm = findOrCreateFarm(farms, chunkPos);

        if (farm.isSpentCrop(pos)) return;
        if (farm.containsRawCrop(pos)) return;

        farm.addRawCrop(pos, level.getGameTime(), value);
        farm.resetStabilityTimer();
    }

    public static void onCropRemoved(ServerLevel level, BlockPos pos) {
        ChunkPos chunkPos = new ChunkPos(pos);
        Map<ChunkPos, FarmData> farms = farmsByLevel.get(level);

        if (farms == null) return;

        for (FarmData farm : farms.values()) {
            if (farm.containsRawCrop(pos)) {
                farm.removeRawCrop(pos);
                int guiValue = farm.getGuiValue(level);
                syncFarmData(level, farm, guiValue);
                return;
            }
            if (farm.isSpentCrop(pos)) {
                farm.removeSpentCrop(pos);
                return;
            }
        }
    }

    public static void syncFarmData(ServerLevel level, FarmData farm, int guiValue) {
        boolean hasValue = guiValue > 0;
        boolean raidActive = farm.isRaidActive();
        int maxValue = raidActive ? farm.getAccumulatedValue() : Config.MAX_CROP_VALUE.get();

        RaidManager.syncToClients(level, hasValue, guiValue, maxValue, raidActive);

        saveFarmToDisk(level, farm);
    }

    private static void saveFarmToDisk(ServerLevel level, FarmData farm) {
        FarmSavedData savedData = FarmSavedData.get(level);

        FarmSavedData.FarmEntry entry = new FarmSavedData.FarmEntry(farm.farmId);
        entry.accumulatedValue = farm.accumulatedValue;
        entry.raidCountdown = farm.raidCountdown;
        entry.raidActive = farm.raidActive;
        entry.mobsSpawned = farm.mobsSpawned;
        entry.hologramUUID = farm.hologramUUID;

        if (farm.farmCenter != null) {
            entry.centerX = farm.farmCenter.x;
            entry.centerY = farm.farmCenter.y;
            entry.centerZ = farm.farmCenter.z;
        }

        for (ChunkPos chunk : farm.getChunks()) {
            entry.chunks.add(chunk.x + "," + chunk.z);
        }

        entry.spawnedMobs.addAll(farm.spawnedMobs);

        savedData.saveFarm(entry);
    }

    private static FarmData findOrCreateFarm(Map<ChunkPos, FarmData> farms, ChunkPos startChunk) {
        FarmData existing = farms.get(startChunk);
        if (existing != null) return existing;

        for (int dx = -1; dx <= 1; dx++) {
            for (int dz = -1; dz <= 1; dz++) {
                if (dx == 0 && dz == 0) continue;

                ChunkPos neighbor = new ChunkPos(startChunk.x + dx, startChunk.z + dz);
                FarmData farm = farms.get(neighbor);

                if (farm != null) {
                    farms.put(startChunk, farm);
                    farm.addChunk(startChunk);
                    return farm;
                }
            }
        }

        FarmData newFarm = new FarmData(startChunk, UUID.randomUUID());
        newFarm.addChunk(startChunk);
        farms.put(startChunk, newFarm);
        return newFarm;
    }

    @SubscribeEvent
    public static void onServerTick(ServerTickEvent.Post event) {
        if (serverInstance == null) return;

        for (ServerLevel level : serverInstance.getAllLevels()) {
            Map<ChunkPos, FarmData> farms = farmsByLevel.get(level);

            if (farms == null) continue;

            Set<FarmData> uniqueFarms = new HashSet<>(farms.values());

            for (FarmData farm : uniqueFarms) {
                farm.tick(level);
            }

            for (FarmData farm : uniqueFarms) {
                if (farm.isEmpty() && !farm.isRaidActive()) {
                    farm.cleanup(level);
                    RaidManager.syncToClients(level, false, 0, Config.MAX_CROP_VALUE.get(), false);
                    FarmSavedData.get(level).removeFarm(farm.farmId);
                }
            }

            farms.entrySet().removeIf(entry ->
                    entry.getValue().isEmpty() && !entry.getValue().isRaidActive());
        }
    }

    public static Map<ChunkPos, FarmData> getFarmsForLevel(ServerLevel level) {
        return farmsByLevel.get(level);
    }

    // ========== FARM DATA ==========

    public static class FarmData {

        public UUID getFarmId() {
            return farmId;
        }

        private static class CropRecord {
            final long plantedTick;
            final int value;

            CropRecord(long plantedTick, int value) {
                this.plantedTick = plantedTick;
                this.value = value;
            }
        }

        private final UUID farmId;
        private final ChunkPos mainChunk;
        private final Set<ChunkPos> chunks = new HashSet<>();
        private final Map<BlockPos, CropRecord> rawCrops = new HashMap<>();
        private final Set<BlockPos> spentCrops = new HashSet<>();
        private int accumulatedValue = 0;
        private long stabilityTimer = 0;
        private long raidCountdown = 0;
        private boolean raidActive = false;
        private boolean mobsSpawned = false;
        private final Set<UUID> spawnedMobs = new HashSet<>();
        private UUID hologramUUID = null;
        private Vec3 farmCenter = null;

        public FarmData(ChunkPos mainChunk, UUID farmId) {
            this.mainChunk = mainChunk;
            this.farmId = farmId;
        }

        public void setRaidActive(boolean active) {
            this.raidActive = active;
        }

        public void unlockChunks(ServerLevel level) {
            unlockAllChunks(level);
        }

        public void addChunk(ChunkPos chunk) {
            chunks.add(chunk);
        }

        public Set<ChunkPos> getChunks() {
            return Collections.unmodifiableSet(chunks);
        }

        public void addRawCrop(BlockPos pos, long currentTick, int value) {
            rawCrops.put(pos, new CropRecord(currentTick, value));
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

        public ArmorStand getHologram(ServerLevel level) {
            if (hologramUUID == null) return null;
            Entity entity = level.getEntity(hologramUUID);
            return entity instanceof ArmorStand stand && stand.isAlive() ? stand : null;
        }

        public void tick(ServerLevel level) {
            if (raidActive) {
                if (level.getGameTime() % 20 == 0) {
                    int guiValue = getGuiValue(level);
                    syncFarmData(level, this, guiValue);
                }

                if (spawnedMobs.isEmpty()) {
                    raidActive = false;
                    mobsSpawned = false;
                    accumulatedValue = 0;
                    rawCrops.clear();
                    spentCrops.clear();
                    unlockAllChunks(level);
                    RaidManager.onRaidComplete(level, this);
                    removeHologram(level);
                }

                return;
            }

            if (level.getGameTime() % 20 == 0) {
                int guiValue = getGuiValue(level);
                syncFarmData(level, this, guiValue);
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
                return RaidManager.getDisplayValue(this);
            }

            int rawValue = 0;
            for (CropRecord record : rawCrops.values()) {
                rawValue += record.value;
            }

            return accumulatedValue + rawValue;
        }

        private void extractValue(ServerLevel level) {
            int totalValue = 0;
            Vec3 center = calculateCenter();

            for (CropRecord record : rawCrops.values()) {
                totalValue += record.value;
            }

            for (BlockPos pos : rawCrops.keySet()) {
                spentCrops.add(pos);
            }

            accumulatedValue += totalValue;
            rawCrops.clear();
            stabilityTimer = 0;

            if (center != null) {
                farmCenter = center;
            }

            if (accumulatedValue > 0 && raidCountdown <= 0) {
                raidCountdown = Config.RAID_COUNTDOWN_TICKS.get();
                long lockDuration = Config.RAID_COUNTDOWN_TICKS.get() + 24000L;
                lockAllChunks(level, level.getGameTime() + lockDuration);
                spawnHologram(level);
                RaidManager.onRaidScheduled(level, this, accumulatedValue);
            }
        }

        private Vec3 calculateCenter() {
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

                stand.setCustomName(Component.literal(timeStr)
                        .withStyle(ChatFormatting.RED, ChatFormatting.BOLD));

                level.addFreshEntity(stand);
                hologramUUID = stand.getUUID();
            }
        }

        private void updateHologram(ServerLevel level) {
            ArmorStand stand = getHologram(level);
            if (stand == null) return;

            long seconds = raidCountdown / 20;
            long minutes = seconds / 60;
            long secs = seconds % 60;
            String timeStr = String.format("%02d:%02d", minutes, secs);

            stand.setCustomName(Component.literal(timeStr)
                    .withStyle(ChatFormatting.RED, ChatFormatting.BOLD));
        }

        private void updateHologramForRaid(ServerLevel level) {
            ArmorStand stand = getHologram(level);

            if (stand == null) {
                spawnHologram(level);
                stand = getHologram(level);
            }

            if (stand != null) {
                stand.setCustomName(Component.literal("⚠ РЕЙД ⚠")
                        .withStyle(ChatFormatting.RED, ChatFormatting.BOLD));
            }
        }

        private void removeHologram(ServerLevel level) {
            ArmorStand stand = getHologram(level);
            if (stand != null) {
                stand.discard();
            }
            hologramUUID = null;
        }

        public void cleanup(ServerLevel level) {
            removeHologram(level);
        }

        private void startRaid(ServerLevel level) {
            raidActive = true;
            mobsSpawned = false;

            updateHologramForRaid(level);

            boolean success = RaidManager.executeRaid(level, this, accumulatedValue, farmCenter);

            if (!success) {
                raidActive = false;
                unlockAllChunks(level);
            }
        }

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
        }

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