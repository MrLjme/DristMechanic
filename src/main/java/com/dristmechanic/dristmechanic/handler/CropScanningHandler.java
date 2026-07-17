package com.dristmechanic.dristmechanic.handler;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.state.BlockState;

public class CropScanningHandler {

    public static int totalCropsDetected = 0;

    public static void onCropPlaced(ServerLevel level, BlockPos pos, BlockState newState) {
        totalCropsDetected++;
        System.out.println(">> [SUCCESS] Crop planted! Counter: " + totalCropsDetected);
    }

    public static void onCropBroken(ServerLevel level, BlockPos pos, BlockState oldState) {
        totalCropsDetected = Math.max(0, totalCropsDetected - 1);
        System.out.println("<< [SUCCESS] Crop broken! Counter: " + totalCropsDetected);
    }
}