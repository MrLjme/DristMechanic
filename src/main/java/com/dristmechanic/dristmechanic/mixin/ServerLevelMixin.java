package com.dristmechanic.dristmechanic.mixin;

import com.dristmechanic.dristmechanic.handler.CropScanningHandler;
import com.dristmechanic.dristmechanic.init.ModTags;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ServerLevel.class)
public abstract class ServerLevelMixin {

    // Перехватываем момент, когда игра пытается изменить любой блок в мире
    @Inject(method = "setBlock(Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/BlockState;II)Z", at = @At("HEAD"))
    private void dristmechanic$onSetBlock(BlockPos pos, BlockState newState, int flags, int recursionLeft, CallbackInfoReturnable<Boolean> cir) {
        ServerLevel level = (ServerLevel) (Object) this;
        BlockState oldState = level.getBlockState(pos);

        boolean wasCrop = oldState.is(ModTags.Blocks.RAIDABLE_CROPS);
        boolean isCrop = newState.is(ModTags.Blocks.RAIDABLE_CROPS);

        // Если был урожай, а теперь его нет (собрали, сломали, съели)
        if (wasCrop && !isCrop) {
            CropScanningHandler.onCropBroken(level, pos, oldState);
        }
        // Если урожая не было, а теперь он появился (посадили)
        else if (!wasCrop && isCrop) {
            CropScanningHandler.onCropPlaced(level, pos, newState);
        }
    }
}