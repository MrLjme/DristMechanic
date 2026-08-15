package com.dristmechanic.dristmechanic.mixin;

import com.dristmechanic.dristmechanic.handler.CropScanningHandler;
import com.dristmechanic.dristmechanic.Dristmechanic;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Level.class)
public abstract class ServerLevelMixin {
    private static final Logger LOGGER = LoggerFactory.getLogger(Dristmechanic.MODID);

    @Inject(method = "setBlock(Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/BlockState;II)Z", at = @At("HEAD"))
    private void dristmechanic$onSetBlock(BlockPos pos, BlockState newState, int flags, int recursionLeft, CallbackInfoReturnable<Boolean> cir) {
        Level level = (Level) (Object) this;
        if (!level.isClientSide()) {
            LOGGER.info("[MIXIN] setBlock called at {}, block: {}", pos, newState.getBlock());
            CropScanningHandler.onBlockChanged(level, pos, newState);
        }
    }
}