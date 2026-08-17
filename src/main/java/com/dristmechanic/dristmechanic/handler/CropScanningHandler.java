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

    static { cachedValues.defaultReturnValue(0); }

    public static int getCropValue(BlockState state) {
        if (!cacheInitialized) rebuildCache(Config.CROP_VALUES.get());
        return cachedValues.getInt(state);
    }

    public static void rebuildCache(List<? extends String> list) {
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
                                for (BlockState state : block.getStateDefinition().getPossibleStates()) {
                                    newCache.put(state, value);
                                }
                            }
                        }
                    } catch (NumberFormatException ignored) {}
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

        if (isChunkLocked(level, chunkPos)) {
            event.setCanceled(true);
            return;
        }

        int value = getCropValue(event.getPlacedBlock());
        if (value > 0) {
            FarmManager.onCropPlanted(level, pos);
        }
    }

    @SubscribeEvent
    public static void onBlockBreak(BlockEvent.BreakEvent event) {
        if (event.getLevel().isClientSide()) return;
        ServerLevel level = (ServerLevel) event.getLevel();
        BlockPos pos = event.getPos();

        if (getCropValue(event.getState()) > 0) {
            FarmManager.onCropRemoved(level, pos);
        }
    }

    public static void onBlockChanged(
            net.minecraft.world.level.Level level, BlockPos pos,
            BlockState oldState, BlockState newState) {
        if (level.isClientSide()) return;
        ServerLevel serverLevel = (ServerLevel) level;
        int oldValue = getCropValue(oldState);
        int newValue = getCropValue(newState);

        if (oldValue > 0 && oldState.getBlock() == newState.getBlock()) return;

        if (newValue > 0) {
            FarmManager.onCropPlanted(serverLevel, pos);
        } else if (oldValue > 0) {
            FarmManager.onCropRemoved(serverLevel, pos);
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
    }

    public static void unlockChunk(ServerLevel level, ChunkPos chunkPos) {
        LevelChunk chunk = level.getChunk(chunkPos.x, chunkPos.z);
        chunk.setData(ModAttachments.LOCKED_UNTIL.get(), 0L);
        chunk.setUnsaved(true);
    }
}