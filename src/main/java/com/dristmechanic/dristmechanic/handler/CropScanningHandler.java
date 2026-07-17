package com.dristmechanic.dristmechanic.handler;

import com.dristmechanic.dristmechanic.Dristmechanic;
import com.dristmechanic.dristmechanic.init.ModAttachments;
import com.dristmechanic.dristmechanic.init.ModTags;
import net.minecraft.commands.Commands;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.chunk.LevelChunkSection;
import net.minecraft.world.level.chunk.PalettedContainer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.event.level.BlockEvent;
import net.neoforged.neoforge.event.level.ChunkEvent;

import java.util.*;

@EventBusSubscriber(modid = Dristmechanic.MODID)
public class CropScanningHandler {

    @SubscribeEvent
    public static void onChunkLoad(ChunkEvent.Load event) {
        if (event.getLevel().isClientSide()) return;
        scanChunk((LevelChunk) event.getChunk());
    }

    @SubscribeEvent
    public static void onBlockPlace(BlockEvent.EntityPlaceEvent event) {
        if (!event.getLevel().isClientSide() && event.getPlacedBlock().is(ModTags.Blocks.RAIDABLE_CROPS)) {
            updateChunkCount(event.getLevel(), event.getPos());
        }
    }

    @SubscribeEvent
    public static void onBlockBreak(BlockEvent.BreakEvent event) {
        if (!event.getLevel().isClientSide() && event.getState().is(ModTags.Blocks.RAIDABLE_CROPS)) {
            updateChunkCount(event.getLevel(), event.getPos());
        }
    }

    private static void updateChunkCount(LevelAccessor level, BlockPos pos) {
        if (level instanceof ServerLevel serverLevel) {
            scanChunk((LevelChunk) serverLevel.getChunk(pos));
        }
    }

    private static void scanChunk(LevelChunk chunk) {
        int actualCount = 0;
        for (LevelChunkSection section : chunk.getSections()) {
            if (section.hasOnlyAir()) continue;

            PalettedContainer<BlockState> states = section.getStates();
            if (!states.maybeHas(state -> state.is(ModTags.Blocks.RAIDABLE_CROPS))) continue;

            int[] sectionCount = {0};
            states.count((state, count) -> {
                if (state.is(ModTags.Blocks.RAIDABLE_CROPS)) sectionCount[0] += count;
            });
            actualCount += sectionCount[0];
        }

        if (actualCount != chunk.getData(ModAttachments.CROP_COUNT)) {
            chunk.setData(ModAttachments.CROP_COUNT, actualCount);
            chunk.setUnsaved(true);
        }
    }

    public record FarmData(List<ChunkPos> chunks, int totalCrops, BlockPos center) {
        public boolean isEmpty() { return chunks.isEmpty() || totalCrops == 0; }
    }

    public static FarmData findConnectedFarm(ServerLevel level, ChunkPos startChunk) {
        List<ChunkPos> farmChunks = new ArrayList<>();
        Set<Long> visited = new HashSet<>();
        Queue<ChunkPos> queue = new LinkedList<>();
        int totalCrops = 0;

        queue.add(startChunk);
        visited.add(startChunk.toLong());

        while (!queue.isEmpty()) {
            ChunkPos current = queue.poll();
            if (!level.getChunkSource().hasChunk(current.x, current.z)) continue;

            LevelChunk chunk = level.getChunk(current.x, current.z);
            int count = chunk.getData(ModAttachments.CROP_COUNT);
            if (count <= 0) continue;

            farmChunks.add(current);
            totalCrops += count;

            for (int dx = -1; dx <= 1; dx++) {
                for (int dz = -1; dz <= 1; dz++) {
                    if (dx == 0 && dz == 0) continue;

                    ChunkPos neighbor = new ChunkPos(current.x + dx, current.z + dz);
                    long neighborLong = neighbor.toLong();

                    if (visited.add(neighborLong) && level.getChunkSource().hasChunk(neighbor.x, neighbor.z)) {
                        if (level.getChunk(neighbor.x, neighbor.z).getData(ModAttachments.CROP_COUNT) > 0) {
                            queue.add(neighbor);
                        }
                    }
                }
            }
        }

        BlockPos center = farmChunks.isEmpty() ? null : calculateExactCenter(level, farmChunks);
        return new FarmData(farmChunks, totalCrops, center);
    }

    private static BlockPos calculateExactCenter(ServerLevel level, List<ChunkPos> chunks) {
        long sumX = 0, sumY = 0, sumZ = 0;
        int count = 0;

        for (ChunkPos chunkPos : chunks) {
            LevelChunk chunk = level.getChunk(chunkPos.x, chunkPos.z);
            int sectionIdx = 0;
            for (LevelChunkSection section : chunk.getSections()) {
                if (section.hasOnlyAir()) continue;
                PalettedContainer<BlockState> states = section.getStates();
                if (!states.maybeHas(state -> state.is(ModTags.Blocks.RAIDABLE_CROPS))) continue;

                int sectionY = (sectionIdx * 16) + level.getMinBuildHeight();
                int startX = chunkPos.x * 16;
                int startZ = chunkPos.z * 16;

                for (int x = 0; x < 16; x++) {
                    for (int y = 0; y < 16; y++) {
                        for (int z = 0; z < 16; z++) {
                            if (states.get(x, y, z).is(ModTags.Blocks.RAIDABLE_CROPS)) {
                                sumX += startX + x;
                                sumY += sectionY + y;
                                sumZ += startZ + z;
                                count++;
                            }
                        }
                    }
                }
                sectionIdx++;
            }
        }
        return count > 0 ? new BlockPos((int) (sumX / count), (int) (sumY / count), (int) (sumZ / count)) : null;
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
                if (chunk.getData(ModAttachments.CROP_COUNT) > 0) {
                    int dist = Math.abs(x) + Math.abs(z);
                    if (dist < minDist) {
                        minDist = dist;
                        nearest = new ChunkPos(startX + x, startZ + z);
                    }
                }
            }
        }
        return nearest != null ? findConnectedFarm(level, nearest) : new FarmData(List.of(), 0, null);
    }

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
                        ctx.getSource().sendSuccess(() -> Component.literal(
                                "§a[DRIST] Найдена связанная ферма!\n" +
                                        "§eТочный центр: §7" + farm.center().toShortString() + "\n" +
                                        "§eРазмер: §7" + farm.chunks().size() + " чанков\n" +
                                        "§eВсего урожая: §7" + farm.totalCrops()
                        ), false);
                    }
                    return 1;
                })
        ));
    }
}