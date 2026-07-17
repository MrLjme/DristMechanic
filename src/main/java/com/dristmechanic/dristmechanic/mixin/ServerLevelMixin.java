package com.dristmechanic.dristmechanic.mixin;

import com.dristmechanic.dristmechanic.init.ModAttachments;
import com.dristmechanic.dristmechanic.init.ModTags;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Level.class)
public abstract class ServerLevelMixin {

    @Inject(method = "setBlock(Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/BlockState;II)Z", at = @At("HEAD"))
    private void dristmechanic$onSetBlock4(BlockPos pos, BlockState newState, int flags, int recursionLeft, CallbackInfoReturnable<Boolean> cir) {
        Level level = (Level) (Object) this;
        if (level.isClientSide()) return;

        BlockState oldState = level.getBlockState(pos);
        boolean wasCrop = oldState.is(ModTags.Blocks.RAIDABLE_CROPS);
        boolean isCrop = newState.is(ModTags.Blocks.RAIDABLE_CROPS);

        if (wasCrop == isCrop) return;

        if (level instanceof ServerLevel serverLevel) {
            LevelChunk chunk = serverLevel.getChunkAt(pos);
            int currentCount = chunk.getData(ModAttachments.CROP_COUNT);

            if (wasCrop) {
                chunk.setData(ModAttachments.CROP_COUNT, Math.max(0, currentCount - 1));
            } else {
                chunk.setData(ModAttachments.CROP_COUNT, currentCount + 1);
            }
            chunk.setUnsaved(true); // Говорим чанку сохраниться на диск
        }
    }
}