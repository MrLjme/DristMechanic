package com.dristmechanic.dristmechanic.handler;

import com.dristmechanic.dristmechanic.Config;
import com.dristmechanic.dristmechanic.Dristmechanic;
import com.dristmechanic.dristmechanic.init.ModAttachments;
import it.unimi.dsi.fastutil.objects.Object2IntMap;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.level.BlockEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@EventBusSubscriber(modid = Dristmechanic.MODID)
public class CropScanningHandler {
    private static final Logger LOGGER = LoggerFactory.getLogger(Dristmechanic.MODID);

    private static volatile Object2IntMap<BlockState> cachedValues = new Object2IntOpenHashMap<>();
    private static volatile boolean cacheInitialized = false;
    private static final Map<ChunkPos, ServerLevel> lockedChunks = new ConcurrentHashMap<>();

    static {
        cachedValues.defaultReturnValue(0);
    }

    public static int getCropValue(BlockState state) {
        if (!cacheInitialized) {
            rebuildCache(Config.CROP_VALUES.get());
        }
        return cachedValues.getInt(state);
    }

    public static void rebuildCache(List<? extends String> list) {
        LOGGER.info("[CROP_CACHE] Rebuilding cache with {} entries", list != null ? list.size() : 0);
        Object2IntMap<BlockState> newCache = new Object2IntOpenHashMap<>();
        newCache.defaultReturnValue(0);
        if (list != null) {
            for (String entry : list) {
                String[] parts = entry.split("=");
                if (parts.length == 2) {
                    ResourceLocation rl = ResourceLocation.tryParse(parts[0].trim());
                    try {
                        int value = Integer.parseInt(parts[1].trim());
                        if (rl != null) {
                            var block = BuiltInRegistries.BLOCK.get(rl);
                            if (block != Blocks.AIR) {
                                int statesCount = 0;
                                for (BlockState state : block.getStateDefinition().getPossibleStates()) {
                                    newCache.put(state, value);
                                    statesCount++;
                                }
                                LOGGER.info("[CROP_CACHE] Registered {} with value {} ({} states)", rl, value, statesCount);
                            } else {
                                LOGGER.warn("[CROP_CACHE] Block {} not found in registry", rl);
                            }
                        }
                    } catch (NumberFormatException ignored) {
                        LOGGER.warn("[CROP_CACHE] Invalid number format in entry: {}", entry);
                    }
                }
            }
        }
        cachedValues = newCache;
        cacheInitialized = true;
    }

    @SubscribeEvent
    public static void onBlockPlace(BlockEvent.EntityPlaceEvent event) {
        if (event.getLevel().isClientSide()) return;

        ServerLevel level = (ServerLevel) event.getLevel();
        BlockPos pos = event.getPos();
        ChunkPos chunkPos = new ChunkPos(pos);

        LOGGER.info("[BLOCK_PLACE] Block placed at {}: {}", pos, event.getPlacedBlock().getBlock());

        if (isChunkLocked(level, chunkPos)) {
            LOGGER.info("[BLOCK_PLACE] Chunk {} is locked, canceling placement", chunkPos);
            event.setCanceled(true);
            return;
        }

        int value = getCropValue(event.getPlacedBlock());
        LOGGER.info("[BLOCK_PLACE] Crop value: {}", value);

        if (value > 0) {
            LOGGER.info("[BLOCK_PLACE] Calling FarmManager.onCropPlanted");
            FarmManager.onCropPlanted(level, pos);
        }
    }

    @SubscribeEvent
    public static void onBlockBreak(BlockEvent.BreakEvent event) {
        if (event.getLevel().isClientSide()) return;

        ServerLevel level = (ServerLevel) event.getLevel();
        BlockPos pos = event.getPos();

        LOGGER.info("[BLOCK_BREAK] Block broken at {}: {}", pos, event.getState().getBlock());

        if (getCropValue(event.getState()) > 0) {
            FarmManager.onCropRemoved(level, pos);
        }
    }

    public static void onBlockChanged(net.minecraft.world.level.Level level, BlockPos pos, BlockState newState) {
        if (level.isClientSide()) return;

        ServerLevel serverLevel = (ServerLevel) level;
        int value = getCropValue(newState);

        LOGGER.info("[BLOCK_CHANGED] Block at {} changed to {}, value: {}", pos, newState.getBlock(), value);

        if (value > 0) {
            LOGGER.info("[BLOCK_CHANGED] Calling FarmManager.onCropPlanted");
            FarmManager.onCropPlanted(serverLevel, pos);
        }
    }

    public static boolean isChunkLocked(ServerLevel level, ChunkPos chunkPos) {
        LevelChunk chunk = level.getChunk(chunkPos.x, chunkPos.z);
        Long lockedUntil = chunk.getData(ModAttachments.LOCKED_UNTIL.get());
        return lockedUntil != null && lockedUntil > level.getGameTime();
    }

    public static void lockChunk(ServerLevel level, ChunkPos chunkPos, long until) {
        LevelChunk chunk = level.getChunk(chunkPos.x, chunkPos.z);
        chunk.setData(ModAttachments.LOCKED_UNTIL.get(), until);
        chunk.setUnsaved(true);
        LOGGER.info("[LOCK] Chunk {} locked until tick {}", chunkPos, until);
    }

    public static void unlockChunk(ServerLevel level, ChunkPos chunkPos) {
        LevelChunk chunk = level.getChunk(chunkPos.x, chunkPos.z);
        chunk.setData(ModAttachments.LOCKED_UNTIL.get(), 0L);
        chunk.setUnsaved(true);
        LOGGER.info("[UNLOCK] Chunk {} unlocked", chunkPos);
    }
}