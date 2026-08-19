package com.dristmechanic.dristmechanic.handler;

import com.dristmechanic.dristmechanic.Dristmechanic;
import com.dristmechanic.dristmechanic.init.ModEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.SpawnPlacementTypes;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.levelgen.Heightmap;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.RegisterSpawnPlacementsEvent;
import net.neoforged.neoforge.event.entity.living.MobSpawnEvent;

@EventBusSubscriber(modid = Dristmechanic.MODID)
public class ModSpawns {

    @SubscribeEvent
    public static void registerSpawnPlacements(RegisterSpawnPlacementsEvent event) {
        event.register(ModEntities.TOTEBOT.get(), SpawnPlacementTypes.ON_GROUND, Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, ModSpawns::checkCreatureSpawn, RegisterSpawnPlacementsEvent.Operation.REPLACE);
        event.register(ModEntities.HAYBOT.get(), SpawnPlacementTypes.ON_GROUND, Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, ModSpawns::checkCreatureSpawn, RegisterSpawnPlacementsEvent.Operation.REPLACE);
        event.register(ModEntities.TAPEBOT.get(), SpawnPlacementTypes.ON_GROUND, Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, ModSpawns::checkCreatureSpawn, RegisterSpawnPlacementsEvent.Operation.REPLACE);
        event.register(ModEntities.FARMBOT.get(), SpawnPlacementTypes.ON_GROUND, Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, ModSpawns::checkCreatureSpawn, RegisterSpawnPlacementsEvent.Operation.REPLACE);
        event.register(ModEntities.RED_TAPEBOT.get(), SpawnPlacementTypes.ON_GROUND, Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, ModSpawns::checkCreatureSpawn, RegisterSpawnPlacementsEvent.Operation.REPLACE);
    }

    private static <T extends net.minecraft.world.entity.Entity> boolean checkCreatureSpawn(EntityType<T> entityType, ServerLevelAccessor level, MobSpawnType spawnType, BlockPos pos, RandomSource random) {
        return true;
    }

    @SubscribeEvent
    public static void onSpawnPlacementCheck(MobSpawnEvent.SpawnPlacementCheck event) {
        EntityType<?> type = event.getEntityType();

        if (type == ModEntities.TOTEBOT.get() ||
                type == ModEntities.HAYBOT.get() ||
                type == ModEntities.TAPEBOT.get() ||
                type == ModEntities.FARMBOT.get() ||
                type == ModEntities.RED_TAPEBOT.get()) {

            ServerLevelAccessor level = event.getLevel();
            BlockPos pos = event.getPos();

            BlockPos below = pos.below();
            if (!level.getBlockState(below).isFaceSturdy(level, below, net.minecraft.core.Direction.UP)) {
                return;
            }

            if (!level.getBlockState(pos).isAir() || !level.getBlockState(pos.above()).isAir()) {
                return;
            }

            if (event.getSpawnType() == MobSpawnType.NATURAL) {
                int lightLevel = level.getMaxLocalRawBrightness(pos);
                if (lightLevel < 9) {
                    return;
                }
            }

            event.setResult(MobSpawnEvent.SpawnPlacementCheck.Result.SUCCEED);        }
    }
}