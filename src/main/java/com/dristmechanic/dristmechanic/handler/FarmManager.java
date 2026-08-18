package com.dristmechanic.dristmechanic.handler;

import net.minecraft.nbt.StringTag;
import com.dristmechanic.dristmechanic.Config;
import com.dristmechanic.dristmechanic.Dristmechanic;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.server.ServerStartedEvent;
import net.neoforged.neoforge.event.server.ServerStoppingEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@EventBusSubscriber(modid = Dristmechanic.MODID)
public class FarmManager {

    private static final Logger LOGGER = LoggerFactory.getLogger(Dristmechanic.MODID);
    private static MinecraftServer serverInstance = null;

    public static Map<ChunkPos, FarmData> getFarmsForLevel(ServerLevel level) {
        FarmSavedData data = getData(level);
        Map<ChunkPos, FarmData> result = new HashMap<>();
        for (FarmData farm : data.getAllFarms()) {
            result.put(farm.getMainChunk(), farm);
        }
        return result;
    }

    @SubscribeEvent
    public static void onServerStarted(ServerStartedEvent e) {
        serverInstance = e.getServer();
    }

    @SubscribeEvent
    public static void onServerStopping(ServerStoppingEvent e) {
        serverInstance = null;
    }

    public static FarmSavedData getData(ServerLevel level) {
        return level.getDataStorage().computeIfAbsent(
                new SavedData.Factory<>(FarmSavedData::new, FarmSavedData::load),
                Dristmechanic.MODID + "_farms"
        );
    }

    public static void onCropPlanted(ServerLevel level, BlockPos pos) {
        ChunkPos chunkPos = new ChunkPos(pos);
        FarmSavedData data = getData(level);
        FarmData farm = data.findOrCreateFarm(chunkPos);
        if (farm != null) {
            if (farm.isSpentCrop(pos) || farm.containsRawCrop(pos)) return;
            farm.addRawCrop(pos, level.getGameTime());
            farm.resetStabilityTimer();
            data.setDirty();
        }
    }

    public static void onCropRemoved(ServerLevel level, BlockPos pos) {
        FarmSavedData data = getData(level);
        FarmData farm = data.getFarmByCrop(pos);
        if (farm != null) {
            if (farm.containsRawCrop(pos)) {
                farm.removeRawCrop(pos);
                data.setDirty();
                int guiValue = farm.getGuiValue(level);
                RaidManager.syncFarmData(level, farm, guiValue);
                return;
            }
            if (farm.isSpentCrop(pos)) {
                farm.removeSpentCrop(pos);
                data.setDirty();
            }
        }
    }

    @SubscribeEvent
    public static void onServerTick(ServerTickEvent.Post event) {
        if (serverInstance == null) return;
        for (ServerLevel level : serverInstance.getAllLevels()) {
            FarmSavedData data = getData(level);
            data.tick(level);
        }
    }

    public static Collection<FarmData> getAllFarms(ServerLevel level) {
        return getData(level).getAllFarms();
    }

    public static class FarmSavedData extends SavedData {

        private final Map<ChunkPos, FarmData> farms = new ConcurrentHashMap<>();
        private final Map<ChunkPos, ChunkPos> chunkToFarmOwner = new ConcurrentHashMap<>();

        public FarmSavedData() {}

        public static FarmSavedData load(CompoundTag tag, HolderLookup.Provider provider) {
            FarmSavedData data = new FarmSavedData();
            ListTag list = tag.getList("farms", Tag.TAG_COMPOUND);
            for (int i = 0; i < list.size(); i++) {
                CompoundTag farmTag = list.getCompound(i);
                FarmData farm = FarmData.deserialize(farmTag);
                if (farm != null) {
                    data.farms.put(farm.getMainChunk(), farm);
                    for (ChunkPos c : farm.getChunks()) {
                        data.chunkToFarmOwner.put(c, farm.getMainChunk());
                    }
                }
            }
            LOGGER.info("[FARM_MANAGER] Loaded {} farms from disk", data.farms.size());
            return data;
        }

        @Override
        public @NotNull CompoundTag save(@NotNull CompoundTag tag, @NotNull HolderLookup.Provider provider) {
            ListTag list = new ListTag();
            for (FarmData farm : farms.values()) {
                list.add(farm.serialize());
            }
            tag.put("farms", list);
            return tag;
        }

        public FarmData findOrCreateFarm(ChunkPos startChunk) {
            ChunkPos existing = chunkToFarmOwner.get(startChunk);
            if (existing != null) return farms.get(existing);

            for (int dx = -1; dx <= 1; dx++) {
                for (int dz = -1; dz <= 1; dz++) {
                    if (dx == 0 && dz == 0) continue;
                    ChunkPos neighbor = new ChunkPos(startChunk.x + dx, startChunk.z + dz);
                    ChunkPos owner = chunkToFarmOwner.get(neighbor);
                    if (owner != null) {
                        FarmData farm = farms.get(owner);
                        farm.addChunk(startChunk);
                        chunkToFarmOwner.put(startChunk, owner);
                        setDirty();
                        return farm;
                    }
                }
            }

            FarmData newFarm = new FarmData(startChunk);
            newFarm.addChunk(startChunk);
            farms.put(startChunk, newFarm);
            chunkToFarmOwner.put(startChunk, startChunk);
            setDirty();
            LOGGER.info("[FARM_MANAGER] Creating new farm at chunk {}", startChunk);
            return newFarm;
        }

        public FarmData getFarmByCrop(BlockPos pos) {
            ChunkPos cp = new ChunkPos(pos);
            ChunkPos owner = chunkToFarmOwner.get(cp);
            return owner != null ? farms.get(owner) : null;
        }

        public FarmData getFarmByMainChunk(ChunkPos mainChunk) {
            return farms.get(mainChunk);
        }

        public void tick(ServerLevel level) {
            List<FarmData> toRemove = new ArrayList<>();
            for (FarmData farm : farms.values()) {
                farm.tick(level);
                if (farm.isEmpty() && !farm.isRaidActive() && farm.getRaidCountdown(level) <= 0) {
                    farm.cleanup(level);
                    toRemove.add(farm);
                }
            }
            if (!toRemove.isEmpty()) {
                for (FarmData farm : toRemove) {
                    for (ChunkPos c : farm.getChunks()) {
                        chunkToFarmOwner.remove(c);
                    }
                    farms.remove(farm.getMainChunk());
                }
                setDirty();
            }
        }

        public Collection<FarmData> getAllFarms() {
            return farms.values();
        }
    }

    public static class FarmData {

        private ChunkPos mainChunk;
        private final Set<ChunkPos> chunks = new HashSet<>();
        private final Map<BlockPos, Long> rawCrops = new HashMap<>();
        private final Set<BlockPos> spentCrops = new HashSet<>();
        private int accumulatedValue = 0;
        private long stabilityTimer = 0;
        private long raidCountdownEndTick = -1;
        private boolean raidActive = false;
        private boolean mobsSpawned = false;
        private final Set<UUID> spawnedMobs = new HashSet<>();
        private UUID hologramUUID = null;
        private Vec3 farmCenter = null;
        private int currentRaidValue = 0;
        private int totalRaidValue = 0;
        private int originalFarmValue = 0;

        public FarmData(ChunkPos mainChunk) {
            this.mainChunk = mainChunk;
        }

        public int getRawCropCount() {
            return rawCrops.size();
        }

        public long getRaidCountdown() {
            if (raidCountdownEndTick < 0) return 0;
            return Math.max(0, raidCountdownEndTick);
        }

        public Set<BlockPos> getSpentCrops() {
            return Collections.unmodifiableSet(spentCrops);
        }

        public Set<BlockPos> getRawCrops() {
            return Collections.unmodifiableSet(rawCrops.keySet());
        }

        public CompoundTag serialize() {
            CompoundTag tag = new CompoundTag();
            tag.putInt("mainX", mainChunk.x);
            tag.putInt("mainZ", mainChunk.z);

            ListTag chunkList = new ListTag();
            for (ChunkPos c : chunks) {
                CompoundTag ct = new CompoundTag();
                ct.putInt("x", c.x);
                ct.putInt("z", c.z);
                chunkList.add(ct);
            }
            tag.put("chunks", chunkList);

            ListTag rawList = new ListTag();
            for (Map.Entry<BlockPos, Long> e : rawCrops.entrySet()) {
                CompoundTag rt = new CompoundTag();
                BlockPos p = e.getKey();
                rt.putInt("x", p.getX());
                rt.putInt("y", p.getY());
                rt.putInt("z", p.getZ());
                rt.putLong("tick", e.getValue());
                rawList.add(rt);
            }
            tag.put("rawCrops", rawList);

            ListTag spentList = new ListTag();
            for (BlockPos p : spentCrops) {
                CompoundTag st = new CompoundTag();
                st.putInt("x", p.getX());
                st.putInt("y", p.getY());
                st.putInt("z", p.getZ());
                spentList.add(st);
            }
            tag.put("spentCrops", spentList);

            tag.putInt("accumulatedValue", accumulatedValue);
            tag.putLong("stabilityTimer", stabilityTimer);
            tag.putLong("raidCountdownEndTick", raidCountdownEndTick);
            tag.putBoolean("raidActive", raidActive);
            tag.putBoolean("mobsSpawned", mobsSpawned);

            ListTag mobsList = new ListTag();
            for (UUID u : spawnedMobs) {
                mobsList.add(StringTag.valueOf(u.toString()));
            }
            tag.put("spawnedMobs", mobsList);

            if (hologramUUID != null) {
                tag.putUUID("hologramUUID", hologramUUID);
            }

            if (farmCenter != null) {
                CompoundTag center = new CompoundTag();
                center.putDouble("x", farmCenter.x);
                center.putDouble("y", farmCenter.y);
                center.putDouble("z", farmCenter.z);
                tag.put("farmCenter", center);
            }

            tag.putInt("currentRaidValue", currentRaidValue);
            tag.putInt("totalRaidValue", totalRaidValue);
            tag.putInt("originalFarmValue", originalFarmValue);

            return tag;
        }

        public static FarmData deserialize(CompoundTag tag) {
            ChunkPos main = new ChunkPos(tag.getInt("mainX"), tag.getInt("mainZ"));
            FarmData farm = new FarmData(main);

            ListTag chunkList = tag.getList("chunks", Tag.TAG_COMPOUND);
            for (int i = 0; i < chunkList.size(); i++) {
                CompoundTag ct = chunkList.getCompound(i);
                farm.chunks.add(new ChunkPos(ct.getInt("x"), ct.getInt("z")));
            }

            ListTag rawList = tag.getList("rawCrops", Tag.TAG_COMPOUND);
            for (int i = 0; i < rawList.size(); i++) {
                CompoundTag rt = rawList.getCompound(i);
                farm.rawCrops.put(
                        new BlockPos(rt.getInt("x"), rt.getInt("y"), rt.getInt("z")),
                        rt.getLong("tick")
                );
            }

            ListTag spentList = tag.getList("spentCrops", Tag.TAG_COMPOUND);
            for (int i = 0; i < spentList.size(); i++) {
                CompoundTag st = spentList.getCompound(i);
                farm.spentCrops.add(new BlockPos(st.getInt("x"), st.getInt("y"), st.getInt("z")));
            }

            farm.accumulatedValue = tag.getInt("accumulatedValue");
            farm.stabilityTimer = tag.getLong("stabilityTimer");
            farm.raidCountdownEndTick = tag.getLong("raidCountdownEndTick");
            farm.raidActive = tag.getBoolean("raidActive");
            farm.mobsSpawned = tag.getBoolean("mobsSpawned");

            ListTag mobsList = tag.getList("spawnedMobs", Tag.TAG_STRING);
            for (int i = 0; i < mobsList.size(); i++) {
                try {
                    farm.spawnedMobs.add(UUID.fromString(mobsList.getString(i)));
                } catch (IllegalArgumentException ignored) {}
            }

            if (tag.hasUUID("hologramUUID")) {
                farm.hologramUUID = tag.getUUID("hologramUUID");
            }

            if (tag.contains("farmCenter", Tag.TAG_COMPOUND)) {
                CompoundTag center = tag.getCompound("farmCenter");
                farm.farmCenter = new Vec3(
                        center.getDouble("x"),
                        center.getDouble("y"),
                        center.getDouble("z")
                );
            }

            farm.currentRaidValue = tag.getInt("currentRaidValue");
            farm.totalRaidValue = tag.getInt("totalRaidValue");
            farm.originalFarmValue = tag.getInt("originalFarmValue");

            return farm;
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

        public boolean isSpentCrop(BlockPos pos) {
            return spentCrops.contains(pos);
        }

        public void removeSpentCrop(BlockPos pos) {
            spentCrops.remove(pos);
        }

        public void resetStabilityTimer() {
            stabilityTimer = 0;
        }

        public long getRaidCountdown(ServerLevel level) {
            if (raidCountdownEndTick < 0) return 0;
            return Math.max(0, raidCountdownEndTick - level.getGameTime());
        }

        public void tick(ServerLevel level) {
            if (raidActive) {
                if (level.getGameTime() % 20 == 0) {
                    int guiValue = getGuiValue(level);
                    RaidManager.syncFarmData(level, this, guiValue);
                }
                if (mobsSpawned && spawnedMobs.isEmpty()) {
                    LOGGER.info("[FARM_TICK] All mobs dead, ending raid for farm {}", mainChunk);
                    raidActive = false;
                    mobsSpawned = false;
                    accumulatedValue = 0;
                    rawCrops.clear();
                    spentCrops.clear();
                    unlockAllChunks(level);
                    RaidManager.onRaidComplete(level, this);
                    FarmManager.getData(level).setDirty();
                }
                return;
            }

            if (level.getGameTime() % 20 == 0) {
                int guiValue = getGuiValue(level);
                RaidManager.syncFarmData(level, this, guiValue);
            }

            if (raidCountdownEndTick > 0) {
                long remaining = raidCountdownEndTick - level.getGameTime();
                if (remaining <= 0) {
                    startRaid(level);
                    return;
                }
                if (level.getGameTime() % 20 == 0) {
                    updateHologram(level);
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

            LOGGER.info("[EXTRACT] Extracted {} value, total accumulated: {}, center: {}",
                    totalValue, accumulatedValue, farmCenter);

            if (accumulatedValue > 0 && raidCountdownEndTick <= 0) {
                long countdown = Config.RAID_COUNTDOWN_TICKS.get();
                raidCountdownEndTick = level.getGameTime() + countdown;
                long lockDuration = countdown + 24000L;
                lockAllChunks(level, level.getGameTime() + lockDuration);
                spawnHologram(level);
                RaidManager.onRaidScheduled(level, this, accumulatedValue);
                FarmManager.getData(level).setDirty();
            }
        }

        private Vec3 calculateCenter(ServerLevel level) {
            if (rawCrops.isEmpty()) return null;
            double sumX = 0, sumY = 0, sumZ = 0;
            int count = 0;
            for (BlockPos pos : rawCrops.keySet()) {
                sumX += pos.getX() + 0.5;
                sumY += pos.getY();
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
                stand.setPos(farmCenter.x, farmCenter.y - 1.0, farmCenter.z);
                stand.setInvisible(true);
                stand.setNoGravity(true);
                stand.setInvulnerable(true);
                stand.setCustomNameVisible(true);
                stand.setNoBasePlate(true);
                long seconds = getRaidCountdown(level) / 20;
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
                long seconds = getRaidCountdown(level) / 20;
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
            LOGGER.info("[RAID_START] Starting raid for farm {} with value: {}", mainChunk, accumulatedValue);
            RaidManager.executeRaid(level, this, accumulatedValue, farmCenter);
            FarmManager.getData(level).setDirty();
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
            return rawCrops.isEmpty() && accumulatedValue == 0 && !raidActive && spawnedMobs.isEmpty();
        }

        public boolean isRaidActive() { return raidActive; }
        public int getAccumulatedValue() { return accumulatedValue; }
        public ChunkPos getMainChunk() { return mainChunk; }
        public void setAccumulatedValue(int value) { this.accumulatedValue = value; }
        public Vec3 getFarmCenter() { return farmCenter; }
        public int getSpentCropCount() { return spentCrops.size(); }

        public void addSpawnedMob(UUID mobUUID) {
            spawnedMobs.add(mobUUID);
            mobsSpawned = true;
        }

        public void removeSpawnedMob(UUID mobUUID) {
            spawnedMobs.remove(mobUUID);
        }

        public int getSpawnedMobCount() { return spawnedMobs.size(); }

        public int getCurrentRaidValue() { return currentRaidValue; }
        public void setCurrentRaidValue(int v) { this.currentRaidValue = v; }
        public int getTotalRaidValue() { return totalRaidValue; }
        public void setTotalRaidValue(int v) { this.totalRaidValue = v; }
        public int getOriginalFarmValue() { return originalFarmValue; }
        public void setOriginalFarmValue(int v) { this.originalFarmValue = v; }
        public void resetRaidValues() {
            this.currentRaidValue = 0;
            this.totalRaidValue = 0;
            this.originalFarmValue = 0;
        }
    }
}