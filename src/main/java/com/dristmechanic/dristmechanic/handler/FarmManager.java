package com.dristmechanic.dristmechanic.handler;

import com.dristmechanic.dristmechanic.Dristmechanic;
import com.dristmechanic.dristmechanic.init.ModAttachments;
import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.level.LevelEvent;
import net.neoforged.neoforge.event.server.ServerStartedEvent;
import net.neoforged.neoforge.event.server.ServerStoppingEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@EventBusSubscriber(modid = Dristmechanic.MODID)
public class FarmManager {

    private static final Map<ServerLevel, Set<ChunkPos>> farmChunksByLevel = new ConcurrentHashMap<>();
    private static MinecraftServer serverInstance = null;

    @SubscribeEvent
    public static void onServerStarted(ServerStartedEvent e) { serverInstance = e.getServer(); }

    @SubscribeEvent
    public static void onServerStopping(ServerStoppingEvent e) {
        serverInstance = null;
        farmChunksByLevel.clear();
    }

    @SubscribeEvent
    public static void onLevelUnload(LevelEvent.Unload e) {
        if (e.getLevel() instanceof ServerLevel sl) farmChunksByLevel.remove(sl);
    }

    public static void onChunkCropUpdate(ServerLevel level, ChunkPos pos, boolean hasCrops) {
        if (hasCrops) farmChunksByLevel.computeIfAbsent(level, k -> ConcurrentHashMap.newKeySet()).add(pos);
        else {
            Set<ChunkPos> s = farmChunksByLevel.get(level);
            if (s != null) s.remove(pos);
        }
    }

    @SubscribeEvent
    public static void onServerTick(ServerTickEvent.Post event) {
        if (serverInstance != null && serverInstance.getTickCount() % 20 == 0) {
            for (ServerLevel level : serverInstance.getAllLevels()) checkFarmStability(level);
        }
    }

    private static void checkFarmStability(ServerLevel level) {
        Set<ChunkPos> farmChunks = farmChunksByLevel.get(level);
        if (farmChunks == null || farmChunks.isEmpty()) {
            RaidManager.updateHolograms(level, List.of());
            return;
        }

        long currentTick = level.getGameTime();
        Set<Long> visited = new HashSet<>(), validChunksLong = new HashSet<>(farmChunks.size());
        for (ChunkPos cp : farmChunks) validChunksLong.add(cp.toLong());

        List<FarmData> stableFarms = new ArrayList<>();

        for (ChunkPos cp : farmChunks) {
            if (visited.contains(cp.toLong())) continue;

            List<ChunkPos> connected = findConnectedComponent(cp, validChunksLong, visited);
            long maxLastChange = 0;
            int totalValue = 0;
            int totalRaidedValue = 0;

            for (ChunkPos c : connected) {
                LevelChunk chunk = CropScanningHandler.getChunkSafe(level, c.x, c.z);
                if (chunk != null) {
                    Long lastChange = chunk.getData(ModAttachments.LAST_CHANGE_TICK.get());
                    if (lastChange != null && lastChange > maxLastChange) maxLastChange = lastChange;

                    totalValue += chunk.getData(ModAttachments.CROP_COUNT.get());
                    totalRaidedValue += chunk.getData(ModAttachments.RAIDED_CROP_VALUE.get());
                }
            }

            if (currentTick - maxLastChange >= 60) { // Стабильна 3 секунды (60 тиков)
                Vec3 center = calculateExactCenter(level, connected);
                if (center != null) {
                    int unraidedValue = Math.max(0, totalValue - totalRaidedValue);
                    stableFarms.add(new FarmData(connected, totalValue, unraidedValue, center, findEdgeChunks(connected)));
                }
            }
        }
        RaidManager.updateHolograms(level, stableFarms);
    }

    public record FarmData(List<ChunkPos> chunks, int totalValue, int unraidedValue, Vec3 center, List<ChunkPos> edgeChunks) {
        public boolean isEmpty() { return chunks.isEmpty() || totalValue == 0; }
    }

    private static List<ChunkPos> findConnectedComponent(ChunkPos start, Set<Long> validChunksLong, Set<Long> visited) {
        List<ChunkPos> connected = new ArrayList<>();
        Queue<ChunkPos> queue = new ArrayDeque<>();
        queue.add(start);
        visited.add(start.toLong());

        while (!queue.isEmpty()) {
            ChunkPos current = queue.poll();
            connected.add(current);
            for (int dx = -1; dx <= 1; dx++)
                for (int dz = -1; dz <= 1; dz++) {
                    if (dx == 0 && dz == 0) continue;
                    long nL = ChunkPos.asLong(current.x + dx, current.z + dz);
                    if (visited.add(nL) && validChunksLong.contains(nL))
                        queue.add(new ChunkPos(current.x + dx, current.z + dz));
                }
        }
        return connected;
    }

    private static Vec3 calculateExactCenter(ServerLevel level, List<ChunkPos> chunks) {
        long tX = 0, tY = 0, tZ = 0, tC = 0;
        for (ChunkPos cp : chunks) {
            LevelChunk chunk = CropScanningHandler.getChunkSafe(level, cp.x, cp.z);
            if (chunk != null) {
                tX += chunk.getData(ModAttachments.SUM_X.get());
                tY += chunk.getData(ModAttachments.SUM_Y.get());
                tZ += chunk.getData(ModAttachments.SUM_Z.get());
                tC += chunk.getData(ModAttachments.CROP_BLOCK_COUNT.get());
            }
        }
        return tC == 0 ? null : new Vec3((tX / (double) tC) + 0.5, (tY / (double) tC) + 0.5, (tZ / (double) tC) + 0.5);
    }

    private static List<ChunkPos> findEdgeChunks(List<ChunkPos> farmChunks) {
        List<ChunkPos> edges = new ArrayList<>();
        Set<Long> farmLongs = new HashSet<>();
        for (ChunkPos cp : farmChunks) farmLongs.add(cp.toLong());

        for (ChunkPos cp : farmChunks) {
            for (int dx = -1; dx <= 1; dx++)
                for (int dz = -1; dz <= 1; dz++) {
                    if (dx == 0 && dz == 0) continue;
                    if (!farmLongs.contains(ChunkPos.asLong(cp.x + dx, cp.z + dz))) {
                        edges.add(cp);
                        dx = 2;
                        break;
                    }
                }
        }
        return edges;
    }

    public static FarmData findConnectedFarm(ServerLevel level, ChunkPos startChunk) {
        Set<ChunkPos> fC = farmChunksByLevel.getOrDefault(level, Collections.emptySet());
        if (!fC.contains(startChunk)) return new FarmData(List.of(), 0, 0, null, List.of());

        Set<Long> vL = new HashSet<>();
        for (ChunkPos cp : fC) vL.add(cp.toLong());

        List<ChunkPos> connected = findConnectedComponent(startChunk, vL, new HashSet<>());
        int tV = 0, tRV = 0;
        for (ChunkPos cp : connected) {
            LevelChunk c = CropScanningHandler.getChunkSafe(level, cp.x, cp.z);
            if (c != null) {
                tV += c.getData(ModAttachments.CROP_COUNT.get());
                tRV += c.getData(ModAttachments.RAIDED_CROP_VALUE.get());
            }
        }
        int unraided = Math.max(0, tV - tRV);
        return new FarmData(connected, tV, unraided, calculateExactCenter(level, connected), findEdgeChunks(connected));
    }

    public static FarmData findNearestFarm(ServerLevel level, BlockPos origin, int radius) {
        int sX = origin.getX() >> 4, sZ = origin.getZ() >> 4;
        ChunkPos nearest = null;
        int minDist = Integer.MAX_VALUE;
        Set<ChunkPos> fC = farmChunksByLevel.getOrDefault(level, Collections.emptySet());

        for (int x = -radius; x <= radius; x++)
            for (int z = -radius; z <= radius; z++) {
                ChunkPos cp = new ChunkPos(sX + x, sZ + z);
                if (!fC.contains(cp)) continue;
                LevelChunk c = CropScanningHandler.getChunkSafe(level, cp.x, cp.z);
                if (c != null && c.getData(ModAttachments.CROP_COUNT.get()) > 0) {
                    int d = Math.abs(x) + Math.abs(z);
                    if (d < minDist) { minDist = d; nearest = cp; }
                }
            }
        return nearest != null ? findConnectedFarm(level, nearest) : new FarmData(List.of(), 0, 0, null, List.of());
    }
}