package com.dristmechanic.dristmechanic.handler;

import com.dristmechanic.dristmechanic.init.ModTags;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.state.BlockState;

public class CropScanningHandler {

    // Временный глобальный счетчик для отладки
    public static int totalCropsDetected = 0;

    public static void onCropPlaced(ServerLevel level, BlockPos pos, BlockState newState) {
        if (newState.is(ModTags.Blocks.RAIDABLE_CROPS)) {
            totalCropsDetected++;
            System.out.println(">> [MIXIN] Урожай посажен! (Позиция: " + pos.toShortString() + ") Счетчик: " + totalCropsDetected);
        }
    }

    public static void onCropBroken(ServerLevel level, BlockPos pos, BlockState oldState) {
        if (oldState.is(ModTags.Blocks.RAIDABLE_CROPS)) {
            totalCropsDetected = Math.max(0, totalCropsDetected - 1);
            System.out.println("<< [MIXIN] Урожай сломан! (Позиция: " + pos.toShortString() + ") Счетчик: " + totalCropsDetected);
        }
    }
}