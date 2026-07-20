package com.dristmechanic.dristmechanic.handler;

import com.dristmechanic.dristmechanic.Config;
import com.dristmechanic.dristmechanic.Dristmechanic;
import com.dristmechanic.dristmechanic.init.ModAttachments;
import net.minecraft.commands.Commands;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.chunk.LevelChunkSection;
import net.minecraft.world.level.chunk.PalettedContainer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.event.level.BlockEvent;
import net.neoforged.neoforge.event.level.ChunkEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadLocalRandom;

@EventBusSubscriber(modid = Dristmechanic.MODID)
public class CropScanningHandler {

    // =========================================================================================================
    // REGION: CONFIGURATION & STATE
    // =========================================================================================================

    private record DirtyChunk(ServerLevel level, ChunkPos pos) {}
    private static final Set<DirtyChunk> dirtyChunks = ConcurrentHashMap.newKeySet();

    // Кэш для быстрого доступа к ценности блоков
    private static List<? extends String> lastConfigList = null;
    private static final Map<Block, Integer> cachedValues = new IdentityHashMap<>();

    private static int getCropValue(BlockState state) {
        List<? extends String> currentList = Config.CROP_VALUES.get();
        if (currentList != lastConfigList) {
            rebuildCache();
            lastConfigList = currentList;
        }
        return cachedValues.getOrDefault(state.getBlock(), 0);
    }

    private static void rebuildCache() {
        cachedValues.clear();
        for (String entry : Config.CROP_VALUES.get()) {
            String[] parts = entry.split("=");
            if (parts.length == 2) {
                ResourceLocation rl = ResourceLocation.tryParse(parts[0].trim());
                try {
                    int value = Integer.parseInt(parts[1].trim());
                    if (rl != null) {
                        Block block = BuiltInRegistries.BLOCK.get(rl);
                        if (block != Blocks.AIR) {
                            cachedValues.put(block, value);
                        }
                    }
                } catch (NumberFormatException ignored) {}
            }
        }
    }

    // =========================================================================================================
    // REGION: EVENT HANDLERS & CHUNK SCANNING
    // =========================================================================================================

    @SubscribeEvent
    public static void onChunkLoad(ChunkEvent.Load event) {
        if (event.getLevel().isClientSide()) return;
        scanChunk((LevelChunk) event.getChunk());
    }

    @SubscribeEvent
    public static void onBlockPlace(BlockEvent.EntityPlaceEvent event) {
        if (!event.getLevel().isClientSide() && getCropValue(event.getPlacedBlock()) > 0) {
            markDirty((ServerLevel) event.getLevel(), event.getPos());
        }
    }

    @SubscribeEvent
    public static void onBlockBreak(BlockEvent.BreakEvent event) {
        if (!event.getLevel().isClientSide() && getCropValue(event.getState()) > 0) {
            markDirty((ServerLevel) event.getLevel(), event.getPos());
        }
    }

    private static void markDirty(ServerLevel level, BlockPos pos) {
        dirtyChunks.add(new DirtyChunk(level, new ChunkPos(pos)));
    }

    @SubscribeEvent
    public static void onServerTick(ServerTickEvent.Post event) {
        if (dirtyChunks.isEmpty()) return;

        Set<DirtyChunk> toProcess = new HashSet<>(dirtyChunks);
        dirtyChunks.removeAll(toProcess);

        for (DirtyChunk dirty : toProcess) {
            if (dirty.level().getChunkSource().hasChunk(dirty.pos().x, dirty.pos().z)) {
                scanChunk(dirty.level().getChunk(dirty.pos().x, dirty.pos().z));
            }
        }
    }

    private static void scanChunk(LevelChunk chunk) {
        int actualValue = 0;
        for (LevelChunkSection section : chunk.getSections()) {
            if (section.hasOnlyAir()) continue;

            PalettedContainer<BlockState> states = section.getStates();
            if (!states.maybeHas(state -> getCropValue(state) > 0)) continue;

            int[] sectionValue = {0};
            states.count((state, count) -> {
                int val = getCropValue(state);
                if (val > 0) sectionValue[0] += val * count;
            });
            actualValue += sectionValue[0];
        }

        if (actualValue != chunk.getData(ModAttachments.CROP_COUNT.get())) {
            chunk.setData(ModAttachments.CROP_COUNT.get(), actualValue);
            chunk.setUnsaved(true);
        }
    }

    // =========================================================================================================
    // REGION: FARM DISCOVERY (BFS & CENTER CALCULATION)
    // =========================================================================================================

    public record FarmData(List<ChunkPos> chunks, int totalValue, BlockPos center, List<ChunkPos> edgeChunks) {
        public boolean isEmpty() { return chunks.isEmpty() || totalValue == 0; }
    }

    public static FarmData findConnectedFarm(ServerLevel level, ChunkPos startChunk) {
        List<ChunkPos> farmChunks = new ArrayList<>();
        Set<Long> visited = new HashSet<>();
        Queue<ChunkPos> queue = new LinkedList<>();
        int totalValue = 0;

        queue.add(startChunk);
        visited.add(startChunk.toLong());

        while (!queue.isEmpty()) {
            ChunkPos current = queue.poll();
            if (!level.getChunkSource().hasChunk(current.x, current.z)) continue;

            LevelChunk chunk = level.getChunk(current.x, current.z);
            int value = chunk.getData(ModAttachments.CROP_COUNT.get());
            if (value <= 0) continue;

            farmChunks.add(current);
            totalValue += value;

            for (int dx = -1; dx <= 1; dx++) {
                for (int dz = -1; dz <= 1; dz++) {
                    if (dx == 0 && dz == 0) continue;
                    ChunkPos neighbor = new ChunkPos(current.x + dx, current.z + dz);
                    long neighborLong = neighbor.toLong();

                    if (visited.add(neighborLong) && level.getChunkSource().hasChunk(neighbor.x, neighbor.z)) {
                        if (level.getChunk(neighbor.x, neighbor.z).getData(ModAttachments.CROP_COUNT.get()) > 0) {
                            queue.add(neighbor);
                        }
                    }
                }
            }
        }

        BlockPos center = farmChunks.isEmpty() ? null : calculateExactCenter(level, farmChunks);
        List<ChunkPos> edgeChunks = findEdgeChunks(farmChunks);
        return new FarmData(farmChunks, totalValue, center, edgeChunks);
    }

    private static BlockPos calculateExactCenter(ServerLevel level, List<ChunkPos> chunks) {
        long sumX = 0, sumY = 0, sumZ = 0;
        long count = 0;

        for (ChunkPos chunkPos : chunks) {
            LevelChunk chunk = level.getChunk(chunkPos.x, chunkPos.z);
            LevelChunkSection[] sections = chunk.getSections();

            for (int i = 0; i < sections.length; i++) {
                LevelChunkSection section = sections[i];
                if (section.hasOnlyAir()) continue;

                PalettedContainer<BlockState> states = section.getStates();
                if (!states.maybeHas(state -> getCropValue(state) > 0)) continue;

                int sectionY = (i * 16) + level.getMinBuildHeight();
                int startX = chunkPos.x * 16;
                int startZ = chunkPos.z * 16;

                for (int y = 0; y < 16; y++) {
                    for (int z = 0; z < 16; z++) {
                        for (int x = 0; x < 16; x++) {
                            if (getCropValue(states.get(x, y, z)) > 0) {
                                sumX += startX + x;
                                sumY += sectionY + y;
                                sumZ += startZ + z;
                                count++;
                            }
                        }
                    }
                }
            }
        }
        return count > 0 ? new BlockPos((int) (sumX / count), (int) (sumY / count), (int) (sumZ / count)) : null;
    }

    private static List<ChunkPos> findEdgeChunks(List<ChunkPos> farmChunks) {
        List<ChunkPos> edgeChunks = new ArrayList<>();
        Set<ChunkPos> farmSet = new HashSet<>(farmChunks);

        for (ChunkPos chunkPos : farmChunks) {
            boolean isEdge = false;
            for (int dx = -1; dx <= 1 && !isEdge; dx++) {
                for (int dz = -1; dz <= 1 && !isEdge; dz++) {
                    if (dx == 0 && dz == 0) continue;
                    if (!farmSet.contains(new ChunkPos(chunkPos.x + dx, chunkPos.z + dz))) {
                        isEdge = true;
                    }
                }
            }
            if (isEdge) edgeChunks.add(chunkPos);
        }
        return edgeChunks;
    }

    public static FarmData findNearestFarm(ServerLevel level, BlockPos origin, int searchRadius) {
        int startX = origin.getX() >> 4;
        int startZ = origin.getZ() >> 4;
        ChunkPos nearest = null;
        int minDist = Integer.MAX_VALUE;

        for (int x = -searchRadius; x <= searchRadius; x++) {
            for (int z = -searchRadius; z <= searchRadius; z++) {
                if (!level.getChunkSource().hasChunk(startX + x, startZ + z)) continue;
                LevelChunk chunk = level.getChunk(startX + x, startZ + z);
                if (chunk.getData(ModAttachments.CROP_COUNT.get()) > 0) {
                    int dist = Math.abs(x) + Math.abs(z);
                    if (dist < minDist) {
                        minDist = dist;
                        nearest = new ChunkPos(startX + x, startZ + z);
                    }
                }
            }
        }
        return nearest != null ? findConnectedFarm(level, nearest) : new FarmData(List.of(), 0, null, List.of());
    }

    // =========================================================================================================
    // REGION: RAID SPAWN LOGIC (CAVE & HEIGHT FIXES)
    // =========================================================================================================

    public static List<BlockPos> findRaidSpawnPoints(ServerLevel level, FarmData farm, int maxPoints) {
        List<BlockPos> finalSpawnPoints = new ArrayList<>();
        if (farm.isEmpty() || farm.edgeChunks().isEmpty()) return finalSpawnPoints;

        BlockPos center = farm.center();
        int targetY = center.getY();
        ThreadLocalRandom random = ThreadLocalRandom.current();

        Map<ChunkPos, List<BlockPos>> chunkCandidates = new HashMap<>();
        Map<ChunkPos, Double> chunkHeightScores = new HashMap<>();

        for (ChunkPos edgeChunk : farm.edgeChunks()) {
            int edgeCenterX = edgeChunk.x * 16 + 8;
            int edgeCenterZ = edgeChunk.z * 16 + 8;

            double dx = edgeCenterX - center.getX();
            double dz = edgeCenterZ - center.getZ();
            double distance = Math.sqrt(dx * dx + dz * dz);

            double dirX = distance < 1 ? Math.cos(random.nextDouble() * 2 * Math.PI) : dx / distance;
            double dirZ = distance < 1 ? Math.sin(random.nextDouble() * 2 * Math.PI) : dz / distance;

            double spawnDistance = random.nextInt(48, 65);
            int targetX = (int) (edgeCenterX + dirX * spawnDistance);
            int targetZ = (int) (edgeCenterZ + dirZ * spawnDistance);

            if (!level.getChunkSource().hasChunk(targetX >> 4, targetZ >> 4)) {
                targetX = edgeCenterX; targetZ = edgeCenterZ;
            }

            List<BlockPos> candidates = new ArrayList<>();
            Set<Long> candidateSet = new HashSet<>();
            double totalYDiff = 0;

            int attempts = maxPoints * 20;
            for (int i = 0; i < attempts && candidates.size() < maxPoints * 2; i++) {
                int offsetX = random.nextInt(-16, 17);
                int offsetZ = random.nextInt(-16, 17);

                BlockPos spawnPos = findClosestValidSpawn(level, targetX + offsetX, targetZ + offsetZ, targetY);
                if (spawnPos != null) {
                    long packed = spawnPos.asLong();
                    if (candidateSet.add(packed)) {
                        candidates.add(spawnPos);
                        totalYDiff += Math.abs(spawnPos.getY() - targetY);
                    }
                }
            }

            if (!candidates.isEmpty()) {
                chunkCandidates.put(edgeChunk, candidates);
                chunkHeightScores.put(edgeChunk, totalYDiff / candidates.size());
            }
        }

        if (chunkCandidates.isEmpty()) return finalSpawnPoints;

        List<ChunkPos> validChunks = new ArrayList<>(chunkCandidates.keySet());
        List<Double> weights = new ArrayList<>();
        double totalWeight = 0;

        for (ChunkPos chunk : validChunks) {
            double weight = 1.0 / (1.0 + chunkHeightScores.get(chunk));
            weights.add(weight);
            totalWeight += weight;
        }

        double rand = random.nextDouble() * totalWeight;
        double cumulative = 0;
        ChunkPos selectedChunk = validChunks.getFirst();

        for (int i = 0; i < validChunks.size(); i++) {
            cumulative += weights.get(i);
            if (rand <= cumulative) {
                selectedChunk = validChunks.get(i);
                break;
            }
        }

        List<BlockPos> selectedPoints = new ArrayList<>(chunkCandidates.get(selectedChunk));
        Collections.shuffle(selectedPoints, random);

        for (int i = 0; i < Math.min(maxPoints, selectedPoints.size()); i++) {
            finalSpawnPoints.add(selectedPoints.get(i));
        }

        return finalSpawnPoints;
    }

    private static BlockPos findClosestValidSpawn(ServerLevel level, int x, int z, int targetY) {
        int minY = level.getMinBuildHeight();
        int maxY = level.getMaxBuildHeight();

        for (int offset = 0; offset < 48; offset++) {
            BlockPos posUp = new BlockPos(x, targetY + offset, z);
            if (posUp.getY() < maxY && isValidSpawn(level, posUp)) return posUp;

            if (offset > 0) {
                BlockPos posDown = new BlockPos(x, targetY - offset, z);
                if (posDown.getY() >= minY && isValidSpawn(level, posDown)) return posDown;
            }
        }
        return null;
    }

    private static boolean isValidSpawn(ServerLevel level, BlockPos pos) {
        BlockState state = level.getBlockState(pos);
        BlockState below = level.getBlockState(pos.below());
        BlockState above = level.getBlockState(pos.above());

        boolean space1 = state.isAir() || !state.blocksMotion();
        boolean space2 = above.isAir() || !above.blocksMotion();
        boolean solidBelow = below.blocksMotion();
        boolean noFluid = state.getFluidState().isEmpty() && above.getFluidState().isEmpty();

        return space1 && space2 && solidBelow && noFluid;
    }

    // =========================================================================================================
    // REGION: COMMANDS
    // =========================================================================================================

    @SubscribeEvent
    public static void onRegisterCommands(RegisterCommandsEvent event) {
        event.getDispatcher().register(Commands.literal("drist").then(
                Commands.literal("center").executes(ctx -> {
                    ServerPlayer player = ctx.getSource().getPlayerOrException();
                    ServerLevel level = (ServerLevel) player.level();
                    FarmData farm = findNearestFarm(level, player.blockPosition(), 32);

                    if (farm.isEmpty()) {
                        ctx.getSource().sendSuccess(() -> Component.literal("§c[DRIST] Рядом нет ферм."), false);
                    } else {
                        List<BlockPos> spawnPoints = findRaidSpawnPoints(level, farm, 5);
                        StringBuilder spawnInfo = new StringBuilder();

                        if (spawnPoints.isEmpty()) {
                            spawnInfo.append("§cНе найдено");
                        } else {
                            for (int i = 0; i < spawnPoints.size(); i++) {
                                if (i > 0) spawnInfo.append("\n");
                                spawnInfo.append("§7").append(i + 1).append(". ").append(spawnPoints.get(i).toShortString());
                            }
                        }

                        ctx.getSource().sendSuccess(() -> Component.literal(
                                "§a[DRIST] Найдена связанная ферма!\n" +
                                        "§eТочный центр: §7" + farm.center().toShortString() + "\n" +
                                        "§eРазмер: §7" + farm.chunks().size() + " участков\n" +
                                        "§eОбщая ценность: §7" + farm.totalValue() + "\n" +
                                        "§eТочки появления рейда:\n" + spawnInfo
                        ), false);
                    }
                    return 1;
                })
        ));
    }
}