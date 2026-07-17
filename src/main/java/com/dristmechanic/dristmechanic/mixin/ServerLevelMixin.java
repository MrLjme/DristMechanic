package com.dristmechanic.dristmechanic.mixin;

import com.dristmechanic.dristmechanic.handler.CropScanningHandler;
import com.dristmechanic.dristmechanic.init.ModTags;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Level.class)
public abstract class ServerLevelMixin {

    // ОСТАВЛЯЕМ ТОЛЬКО 4 АРГУМЕНТА! 3-аргументный вызывает этот внутри себя.
    @Inject(method = "setBlock(Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/BlockState;II)Z", at = @At("HEAD"))
    private void dristmechanic$onSetBlock4(BlockPos pos, BlockState newState, int flags, int recursionLeft, CallbackInfoReturnable<Boolean> cir) {
        Level level = (Level) (Object) this;
        if (level.isClientSide()) return;

        BlockState oldState = level.getBlockState(pos);
        String blockName = BuiltInRegistries.BLOCK.getKey(newState.getBlock()).toString();

        boolean wasCrop = oldState.is(ModTags.Blocks.RAIDABLE_CROPS) || oldState.is(Blocks.POTATOES);
        boolean isCrop = newState.is(ModTags.Blocks.RAIDABLE_CROPS) || newState.is(Blocks.POTATOES);

        if (blockName.contains("potato")) {
            System.out.println(">> [DEBUG] Block: " + blockName + " | In Tag? " + newState.is(ModTags.Blocks.RAIDABLE_CROPS));
        }

        if (level instanceof ServerLevel serverLevel) {
            if (wasCrop && !isCrop) {
                CropScanningHandler.onCropBroken(serverLevel, pos, oldState);
            } else if (!wasCrop && isCrop) {
                CropScanningHandler.onCropPlaced(serverLevel, pos, newState);
            }
        }
    }
}