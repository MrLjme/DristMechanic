package com.dristmechanic.dristmechanic.handler;

import com.dristmechanic.dristmechanic.Dristmechanic;
import com.dristmechanic.dristmechanic.init.ModEntities;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.SpawnPlacementTypes;
import net.minecraft.world.entity.SpawnPlacements;
import net.minecraft.world.level.levelgen.Heightmap;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.RegisterSpawnPlacementsEvent;

@EventBusSubscriber(modid = Dristmechanic.MODID)
public class ModSpawns {

    @SubscribeEvent
    public static void registerSpawnPlacements(RegisterSpawnPlacementsEvent event) {
        event.register(ModEntities.TOTEBOT.get(), SpawnPlacementTypes.NO_RESTRICTIONS, Heightmap.Types.WORLD_SURFACE, surfacePredicate(), RegisterSpawnPlacementsEvent.Operation.REPLACE);
        event.register(ModEntities.HAYBOT.get(), SpawnPlacementTypes.NO_RESTRICTIONS, Heightmap.Types.WORLD_SURFACE, surfacePredicate(), RegisterSpawnPlacementsEvent.Operation.REPLACE);
        event.register(ModEntities.TAPEBOT.get(), SpawnPlacementTypes.NO_RESTRICTIONS, Heightmap.Types.WORLD_SURFACE, surfacePredicate(), RegisterSpawnPlacementsEvent.Operation.REPLACE);
        event.register(ModEntities.FARMBOT.get(), SpawnPlacementTypes.NO_RESTRICTIONS, Heightmap.Types.WORLD_SURFACE, surfacePredicate(), RegisterSpawnPlacementsEvent.Operation.REPLACE);
        event.register(ModEntities.RED_TAPEBOT.get(), SpawnPlacementTypes.NO_RESTRICTIONS, Heightmap.Types.WORLD_SURFACE, surfacePredicate(), RegisterSpawnPlacementsEvent.Operation.REPLACE);
    }

    private static <T extends Entity> SpawnPlacements.SpawnPredicate<T> surfacePredicate() {
        return (type, level, spawnType, pos, random) ->
                level.getBlockState(pos).isAir()
                        && level.getBlockState(pos.below()).isFaceSturdy(level, pos.below(), Direction.UP);
    }
}