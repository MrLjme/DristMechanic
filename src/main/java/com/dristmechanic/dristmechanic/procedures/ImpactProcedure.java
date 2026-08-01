package com.dristmechanic.dristmechanic.procedures;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Block;

public class ImpactProcedure {
    public static void execute(LevelAccessor world, double x, double y, double z) {
        if (!(world instanceof ServerLevel serverLevel)) {
            return;
        }

        BlockPos pos = BlockPos.containing(x, y, z);
        float hardness = serverLevel.getBlockState(pos).getDestroySpeed(serverLevel, pos);

        if (hardness > -0.1 && hardness < 30) {
            int chance = Mth.nextInt(serverLevel.getRandom(), 0, 100);
            int threshold = (int) (100 / (1 + hardness * 2.5));
            if (chance < threshold) {
                serverLevel.getServer().execute(() -> {
                    Block.dropResources(serverLevel.getBlockState(pos), serverLevel, pos, null);
                    serverLevel.destroyBlock(pos, false);
                });
            }
        }
    }
}