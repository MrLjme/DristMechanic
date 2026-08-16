package com.dristmechanic.dristmechanic.handler;

import com.dristmechanic.dristmechanic.Config;
import com.dristmechanic.dristmechanic.Dristmechanic;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@EventBusSubscriber(modid = Dristmechanic.MODID)
public class DristCommands {

    private static final int MARKER_LIFE_TICKS = 200;
    private static final Map<ResourceKey<Level>, List<MarkerEntry>> activeMarkers = new ConcurrentHashMap<>();

    private static class MarkerEntry {
        final UUID uuid;
        final long removeAtTick;

        MarkerEntry(UUID uuid, long removeAtTick) {
            this.uuid = uuid;
            this.removeAtTick = removeAtTick;
        }
    }

    @SubscribeEvent
    public static void registerCommands(RegisterCommandsEvent event) {
        CommandDispatcher<CommandSourceStack> dispatcher = event.getDispatcher();

        dispatcher.register(
                Commands.literal("drist")
                        .then(Commands.literal("farm")
                                .executes(DristCommands::executeFarmInfo)
                        )
        );
    }

    @SubscribeEvent
    public static void onServerTick(ServerTickEvent.Post event) {
        for (ServerLevel level : event.getServer().getAllLevels()) {
            List<MarkerEntry> markers = activeMarkers.get(level.dimension());
            if (markers == null) continue;

            long now = level.getGameTime();
            Iterator<MarkerEntry> it = markers.iterator();
            while (it.hasNext()) {
                MarkerEntry entry = it.next();
                if (now >= entry.removeAtTick) {
                    Entity entity = level.getEntity(entry.uuid);
                    if (entity != null) entity.discard();
                    it.remove();
                }
            }
            if (markers.isEmpty()) {
                activeMarkers.remove(level.dimension());
            }
        }
    }

    private static int executeFarmInfo(CommandContext<CommandSourceStack> ctx) {
        CommandSourceStack source = ctx.getSource();

        ServerPlayer player = source.getPlayer();
        if (player == null) {
            source.sendFailure(Component.literal("Эту команду может использовать только игрок!")
                    .withStyle(ChatFormatting.RED));
            return 0;
        }

        ServerLevel level = player.serverLevel();
        BlockPos playerPos = player.blockPosition();

        FarmManager.FarmData nearest = findNearestFarm(level, playerPos);

        if (nearest == null) {
            source.sendFailure(Component.literal("Поблизости не обнаружено ни одного огорода!")
                    .withStyle(ChatFormatting.RED));
            return 0;
        }

        clearMarkers(level);
        spawnChunkMarkers(level, nearest, playerPos.getY() + 2);

        ChunkPos mainChunk = nearest.getMainChunk();
        int rawCount = nearest.getRawCropCount();
        int spentCount = nearest.getSpentCropCount();
        int accumulated = nearest.getAccumulatedValue();
        boolean raidActive = nearest.isRaidActive();
        long countdown = nearest.getRaidCountdown();
        Vec3 center = nearest.getFarmCenter();

        if (center == null) {
            center = new Vec3(
                    mainChunk.getMiddleBlockX(),
                    playerPos.getY(),
                    mainChunk.getMiddleBlockZ()
            );
        }

        double distance = Math.sqrt(center.distanceToSqr(
                playerPos.getX(), playerPos.getY(), playerPos.getZ()
        ));

        Component header = Component.literal("=== Информация об огороде ===")
                .withStyle(ChatFormatting.GREEN, ChatFormatting.BOLD);

        Component centerLine = Component.literal("Центр: ")
                .append(Component.literal(String.format("X: %.1f  Y: %.1f  Z: %.1f", center.x, center.y, center.z))
                        .withStyle(ChatFormatting.YELLOW));

        Component chunkLine = Component.literal("Главный чанк: ")
                .append(Component.literal(mainChunk.x + ", " + mainChunk.z)
                        .withStyle(ChatFormatting.YELLOW));

        Component cropsLine = Component.literal("Растений: ")
                .append(Component.literal(String.valueOf(rawCount)).withStyle(ChatFormatting.YELLOW))
                .append(Component.literal(" (выросло: ").withStyle(ChatFormatting.GRAY))
                .append(Component.literal(String.valueOf(spentCount)).withStyle(ChatFormatting.YELLOW))
                .append(Component.literal(")").withStyle(ChatFormatting.GRAY));

        Component valueLine = Component.literal("Накопленная ценность: ")
                .append(Component.literal(String.valueOf(accumulated)).withStyle(ChatFormatting.GOLD))
                .append(Component.literal(" / ").withStyle(ChatFormatting.GRAY))
                .append(Component.literal(String.valueOf(Config.MAX_CROP_VALUE.get())).withStyle(ChatFormatting.GREEN));

        Component raidLine;
        if (raidActive) {
            raidLine = Component.literal("Рейд: ")
                    .append(Component.literal(" АКТИВЕН ").withStyle(ChatFormatting.RED, ChatFormatting.BOLD));
        } else if (countdown > 0) {
            long seconds = countdown / 20;
            long mins = seconds / 60;
            long secs = seconds % 60;
            raidLine = Component.literal("Рейд через: ")
                    .append(Component.literal(String.format("%02d:%02d", mins, secs))
                            .withStyle(ChatFormatting.YELLOW, ChatFormatting.BOLD));
        } else if (accumulated > 0) {
            raidLine = Component.literal("Рейд: ")
                    .append(Component.literal("скоро начнётся").withStyle(ChatFormatting.YELLOW));
        } else {
            raidLine = Component.literal("Рейд: ")
                    .append(Component.literal("не запланирован").withStyle(ChatFormatting.GRAY));
        }

        Component distLine = Component.literal("Расстояние до тебя: ")
                .append(Component.literal(String.format("%.1f блоков", distance)).withStyle(ChatFormatting.AQUA));

        Component markerLine = Component.literal("Маркеры: ")
                .append(Component.literal("показаны по углам чанков (10 сек)").withStyle(ChatFormatting.GREEN));

        source.sendSuccess(() -> header, false);
        source.sendSuccess(() -> centerLine, false);
        source.sendSuccess(() -> chunkLine, false);
        source.sendSuccess(() -> cropsLine, false);
        source.sendSuccess(() -> valueLine, false);
        source.sendSuccess(() -> raidLine, false);
        source.sendSuccess(() -> distLine, false);
        source.sendSuccess(() -> markerLine, false);

        return 1;
    }

    private static void clearMarkers(ServerLevel level) {
        List<MarkerEntry> old = activeMarkers.remove(level.dimension());
        if (old != null) {
            for (MarkerEntry entry : old) {
                Entity entity = level.getEntity(entry.uuid);
                if (entity != null) entity.discard();
            }
        }
    }

    private static void spawnChunkMarkers(ServerLevel level, FarmManager.FarmData farm, int markerY) {
        Map<ChunkPos, FarmManager.FarmData> allFarms = FarmManager.getFarmsForLevel(level);
        if (allFarms == null) return;

        Set<ChunkPos> farmChunks = new HashSet<>();
        for (Map.Entry<ChunkPos, FarmManager.FarmData> entry : allFarms.entrySet()) {
            if (entry.getValue() == farm) {
                farmChunks.add(entry.getKey());
            }
        }

        if (farmChunks.isEmpty()) return;

        List<MarkerEntry> newMarkers = new ArrayList<>();
        long removeAt = level.getGameTime() + MARKER_LIFE_TICKS;

        for (ChunkPos chunk : farmChunks) {
            int baseX = chunk.x << 4;
            int baseZ = chunk.z << 4;
            int[][] corners = {{0, 0}, {15, 0}, {0, 15}, {15, 15}};

            for (int[] corner : corners) {
                ArmorStand marker = EntityType.ARMOR_STAND.create(level);
                if (marker == null) continue;

                marker.setPos(baseX + corner[0] + 0.5, markerY, baseZ + corner[1] + 0.5);
                marker.setInvisible(true);
                marker.setNoGravity(true);
                marker.setInvulnerable(true);
                marker.setNoBasePlate(true);
                marker.setItemSlot(EquipmentSlot.HEAD, new ItemStack(Items.GLOWSTONE));

                level.addFreshEntity(marker);
                newMarkers.add(new MarkerEntry(marker.getUUID(), removeAt));
            }
        }

        if (!newMarkers.isEmpty()) {
            activeMarkers.put(level.dimension(), newMarkers);
        }
    }

    private static FarmManager.FarmData findNearestFarm(ServerLevel level, BlockPos pos) {
        Map<ChunkPos, FarmManager.FarmData> farms = FarmManager.getFarmsForLevel(level);
        if (farms == null || farms.isEmpty()) return null;

        Set<FarmManager.FarmData> unique = Collections.newSetFromMap(new IdentityHashMap<>());
        unique.addAll(farms.values());

        FarmManager.FarmData nearest = null;
        double nearestDistSq = Double.MAX_VALUE;

        for (FarmManager.FarmData farm : unique) {
            Vec3 center = farm.getFarmCenter();
            if (center == null) {
                ChunkPos chunk = farm.getMainChunk();
                center = new Vec3(chunk.getMiddleBlockX(), pos.getY(), chunk.getMiddleBlockZ());
            }

            double distSq = center.distanceToSqr(pos.getX(), pos.getY(), pos.getZ());

            if (distSq < nearestDistSq) {
                nearestDistSq = distSq;
                nearest = farm;
            }
        }

        return nearest;
    }
}
