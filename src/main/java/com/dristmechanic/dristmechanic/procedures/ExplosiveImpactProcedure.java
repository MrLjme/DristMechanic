package com.dristmechanic.dristmechanic.procedures;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;

public class ExplosiveImpactProcedure {
    public static void execute(LevelAccessor world, double x, double y, double z, Entity immediatesourceentity) {
        if (immediatesourceentity == null) {
            return;
        }

        if (world instanceof Level level && !level.isClientSide()) {
            level.explode(null, x, y, z, 3.0F, Level.ExplosionInteraction.MOB);
        }

        if (!immediatesourceentity.level().isClientSide()) {
            immediatesourceentity.discard();
        }
    }
}