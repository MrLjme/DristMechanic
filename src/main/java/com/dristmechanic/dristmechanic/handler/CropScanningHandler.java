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
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.chunk.LevelChunkSection;
import net.minecraft.world.level.chunk.status.ChunkStatus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.config.ModConfigEvent;
import net.neoforged.neoforge.event.level.BlockEvent;
import net.neoforged.neoforge.event.level.ChunkEvent;
import net.neoforged.neoforge.event.level.ExplosionEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@EventBusSubscriber(modid = Dristmechanic.MODID)
public class CropScanningHandler {

    private static final Map<ChunkPos, ServerLevel> dirtyChunks = new ConcurrentHashMap<>();
    private static volatile boolean hasDirtyChunks = false;
    private static volatile List<? extends String> lastConfigList = null;
    private static volatile Object2IntMap<BlockState> cachedValues = new Object2IntOpenHashMap<>();

    static { cachedValues.defaultReturnValue(0); }

    public static int getCropValue(BlockState state) { return cachedValues.getInt(state); }

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
                                for (BlockState state : block.getStateDefinition().getPossibleStates()) newCache.put(state, value);
                            }
                        }
                    } catch (NumberFormatException ignored) {}
                }
            }
        }
        cachedValues = newCache;
    }

    public static LevelChunk getChunkSafe(ServerLevel level, int x, int z) {
        ChunkAccess chunk = level.getChunkSource().getChunk(x, z, ChunkStatus.FULL, false);
        return chunk instanceof LevelChunk lc ? lc : null;
    }

    @SubscribeEvent public static void onConfigReload(ModConfigEvent event) { lastConfigList = null; }
    @SubscribeEvent public static void onChunkLoad(ChunkEvent.Load event) {
        if (!event.getLevel().isClientSide() && event.getChunk() instanceof LevelChunk lc) scanChunk(lc);
    }
    @SubscribeEvent public static void onBlockPlace(BlockEvent.EntityPlaceEvent e) {
        if (!e.getLevel().isClientSide() && getCropValue(e.getPlacedBlock()) > 0) markDirty((ServerLevel) e.getLevel(), e.getPos());
    }
    @SubscribeEvent public static void onBlockBreak(BlockEvent.BreakEvent e) {
        if (!e.getLevel().isClientSide() && getCropValue(e.getState()) > 0) markDirty((ServerLevel) e.getLevel(), e.getPos());
    }
    @SubscribeEvent public static void onNeighborNotify(BlockEvent.NeighborNotifyEvent e) {
        if (!e.getLevel().isClientSide()) markDirty((ServerLevel) e.getLevel(), e.getPos());
    }
    @SubscribeEvent public static void onExplosionDetonate(ExplosionEvent.Detonate e) {
        if (!e.getLevel().isClientSide()) for (BlockPos pos : e.getAffectedBlocks()) markDirty((ServerLevel) e.getLevel(), pos);
    }

    public static void markDirty(ServerLevel level, BlockPos pos) {
        if (dirtyChunks.put(new ChunkPos(pos), level) == null) hasDirtyChunks = true;
    }

    @SubscribeEvent
    public static void onServerTick(ServerTickEvent.Post event) {
        if (hasDirtyChunks && !dirtyChunks.isEmpty()) {
            Iterator<Map.Entry<ChunkPos, ServerLevel>> it = dirtyChunks.entrySet().iterator();
            while (it.hasNext()) {
                Map.Entry<ChunkPos, ServerLevel> entry = it.next();
                it.remove();
                LevelChunk chunk = getChunkSafe(entry.getValue(), entry.getKey().x, entry.getKey().z);
                if (chunk != null) scanChunk(chunk);
            }
            if (dirtyChunks.isEmpty()) hasDirtyChunks = false;
        }
    }

    private static void scanChunk(LevelChunk chunk) {
        ServerLevel level = (ServerLevel) chunk.getLevel();
        List<? extends String> currentList = Config.CROP_VALUES.get();

        if (currentList != lastConfigList) {
            rebuildCache(currentList);
            lastConfigList = currentList;
        }

        long actualValue = 0, sumX = 0, sumY = 0, sumZ = 0, cropCount = 0;
        int baseX = chunk.getPos().getMinBlockX(), baseZ = chunk.getPos().getMinBlockZ();
        LevelChunkSection[] sections = chunk.getSections();

        for (int i = 0; i < sections.length; i++) {
            LevelChunkSection section = sections[i];
            if (section.hasOnlyAir()) continue;

            long[] sectionValue = {0};
            section.getStates().count((state, count) -> {
                int val = cachedValues.getInt(state);
                if (val > 0) sectionValue[0] += (long) val * count;
            });

            if (sectionValue[0] == 0) continue;
            actualValue += sectionValue[0];

            int baseY = (i << 4) + level.getMinBuildHeight();
            for (int x = 0; x < 16; x++) for (int y = 0; y < 16; y++) for (int z = 0; z < 16; z++) {
                if (cachedValues.getInt(section.getBlockState(x, y, z)) > 0) {
                    sumX += baseX + x; sumY += baseY + y; sumZ += baseZ + z; cropCount++;
                }
            }
        }

        int currentCropCount = (int) Math.min(actualValue, Integer.MAX_VALUE);

        if (currentCropCount != chunk.getData(ModAttachments.CROP_COUNT.get())) {
            chunk.setData(ModAttachments.CROP_COUNT.get(), currentCropCount);
            chunk.setData(ModAttachments.SUM_X.get(), sumX);
            chunk.setData(ModAttachments.SUM_Y.get(), sumY);
            chunk.setData(ModAttachments.SUM_Z.get(), sumZ);
            chunk.setData(ModAttachments.CROP_BLOCK_COUNT.get(), cropCount);
            chunk.setData(ModAttachments.LAST_CHANGE_TICK.get(), level.getGameTime());

            // Если общая ценность упала, уменьшаем "зарейженную" ценность
            int currentRaided = chunk.getData(ModAttachments.RAIDED_CROP_VALUE.get());
            if (currentCropCount < currentRaided) {
                chunk.setData(ModAttachments.RAIDED_CROP_VALUE.get(), currentCropCount);
            }

            chunk.setUnsaved(true);
            FarmManager.onChunkCropUpdate(level, chunk.getPos(), actualValue > 0);
        }
    }
}