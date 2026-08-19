package com.dristmechanic.dristmechanic.handler;

import com.dristmechanic.dristmechanic.Dristmechanic;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.FarmBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.level.block.CropGrowEvent;

@EventBusSubscriber(modid = Dristmechanic.MODID)
public class CropGrowthHandler {

    @SubscribeEvent
    public static void onCropGrowPre(CropGrowEvent.Pre event) {
        if (event.getLevel().isClientSide()) return;

        BlockPos pos = event.getPos();
        BlockState belowState = event.getLevel().getBlockState(pos.below());

        if (belowState.is(Blocks.FARMLAND) && belowState.getValue(FarmBlock.MOISTURE) == 0) {
            event.setResult(CropGrowEvent.Pre.Result.DO_NOT_GROW);
        }
    }
}