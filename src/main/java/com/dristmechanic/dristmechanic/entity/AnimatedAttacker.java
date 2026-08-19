package com.dristmechanic.dristmechanic.entity;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

public interface AnimatedAttacker {
    void setAttackingState(boolean attacking);
    boolean isAttackingState();
    int getAttackAnimationLength();
    int getAttackImpactFrame();

    int getStuckTicks();
    void setStuckTicks(int ticks);
    Vec3 getLastPos();
    void setLastPos(Vec3 pos);

    int getAttackTicks();
    void setAttackTicks(int ticks);
    BlockPos getBreakingBlock();
    void setBreakingBlock(BlockPos pos);

    int STUCK_THRESHOLD = 15;
    double RAY_CAST_DISTANCE = 3.0D;

    default void startBreakingBlock(PathfinderMob mob, BlockPos pos) {
        setBreakingBlock(pos);
        setAttackTicks(0);
        setAttackingState(true);
        mob.getNavigation().stop();
    }

    default boolean tickBreakingBlock(PathfinderMob mob, boolean dropItems) {
        BlockPos pos = getBreakingBlock();
        if (pos == null) return false;

        int ticks = getAttackTicks() + 1;
        setAttackTicks(ticks);

        mob.getLookControl().setLookAt(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, 30.0F, 30.0F);

        if (ticks == getAttackImpactFrame()) {
            attemptBreakBlock(mob, pos, dropItems);
        }

        if (ticks >= getAttackAnimationLength()) {
            setAttackingState(false);
            setBreakingBlock(null);
            setAttackTicks(0);
            return false;
        }
        return true;
    }

    default boolean tickStuckDetection(PathfinderMob mob, Vec3 target, boolean dropItems) {
        if (getBreakingBlock() != null) return false;

        Vec3 lastPos = getLastPos();
        int stuckTicks = getStuckTicks();

        if (lastPos != null) {
            if (mob.position().distanceToSqr(lastPos) > 0.01D) stuckTicks = 0;
            else stuckTicks++;
        }
        setLastPos(mob.position());
        setStuckTicks(stuckTicks);

        if (stuckTicks >= STUCK_THRESHOLD) {
            BlockPos blockingBlock = findBlockingBlock(mob, target);
            if (blockingBlock != null) {
                startBreakingBlock(mob, blockingBlock);
                return true;
            } else {
                setStuckTicks(0);
            }
        }
        return false;
    }

    default void resetStuckDetection() {
        setStuckTicks(0);
        setLastPos(null);
    }

    default BlockPos findBlockingBlock(PathfinderMob mob, Vec3 target) {
        Level level = mob.level();
        if (level.isClientSide()) return null;

        Vec3 mobBottom = mob.position();
        double dxH = target.x - mobBottom.x;
        double dy = target.y - mobBottom.y;
        double dzH = target.z - mobBottom.z;
        double horizontalDistSq = dxH * dxH + dzH * dzH;

        Vec3 aimVec;
        if (horizontalDistSq < 0.01D) {
            aimVec = new Vec3(0.0D, dy > 0 ? 1.0D : -1.0D, 0.0D);
        } else {
            Vec3 horizontalVec;
            if (Math.abs(dxH) >= Math.abs(dzH)) {
                horizontalVec = new Vec3(dxH > 0 ? 1.0D : -1.0D, 0.0D, 0.0D);
            } else {
                horizontalVec = new Vec3(0.0D, 0.0D, dzH > 0 ? 1.0D : -1.0D);
            }

            double heightThresholdUp = 1.0D;
            double heightThresholdDown = -0.5D; // Порог для рытья строго вниз

            if (dy > heightThresholdUp) {
                // Лесенка вверх
                aimVec = new Vec3(horizontalVec.x, 1.0D, horizontalVec.z).normalize();
            } else if (dy < heightThresholdDown) {
                // СТРОГО ВНИЗ
                aimVec = new Vec3(0.0D, -1.0D, 0.0D);
            } else {
                // Прямо
                aimVec = horizontalVec;
            }
        }

        Vec3 startPosHead = mob.position().add(0.0D, 1.5D, 0.0D);
        Vec3 startPosLegs = mob.position().add(0.0D, 0.5D, 0.0D);
        Vec3 endPosHead = startPosHead.add(aimVec.scale(RAY_CAST_DISTANCE));
        Vec3 endPosLegs = startPosLegs.add(aimVec.scale(RAY_CAST_DISTANCE));

        ClipContext contextHead = new ClipContext(startPosHead, endPosHead, ClipContext.Block.OUTLINE, ClipContext.Fluid.NONE, mob);
        ClipContext contextLegs = new ClipContext(startPosLegs, endPosLegs, ClipContext.Block.OUTLINE, ClipContext.Fluid.NONE, mob);

        BlockHitResult hitResultHead = level.clip(contextHead);
        BlockHitResult hitResultLegs = level.clip(contextLegs);

        BlockPos bestPos = null;
        double minDist = Double.MAX_VALUE;

        for (BlockHitResult hit : new BlockHitResult[]{hitResultHead, hitResultLegs}) {
            if (hit.getType() == HitResult.Type.BLOCK) {
                BlockPos pos = hit.getBlockPos();
                BlockState state = level.getBlockState(pos);
                if (!state.isAir() && state.getFluidState().isEmpty() && state.getDestroySpeed(level, pos) >= 0.0F) {
                    double dist = hit.getLocation().distanceToSqr(mob.position().add(0, 1.0, 0));
                    if (dist < minDist) {
                        minDist = dist;
                        bestPos = pos;
                    }
                }
            }
        }
        return bestPos;
    }

    default void attemptBreakBlock(PathfinderMob mob, BlockPos pos, boolean dropItems) {
        Level level = mob.level();
        if (level.isClientSide()) return;
        BlockState state = level.getBlockState(pos);
        if (state.isAir() || !state.getFluidState().isEmpty()) return;

        float hardness = state.getDestroySpeed(level, pos);
        if (hardness < 0.0F) return;

        SoundType soundType = state.getSoundType(level, pos, mob);
        level.playSound(null, pos, soundType.getHitSound(), SoundSource.BLOCKS, soundType.getVolume() * 0.5F, soundType.getPitch() * 0.875F);

        float chancePercent = 100.0F / (1.0F + hardness * 2.5F);
        boolean blockBroken = mob.getRandom().nextFloat() * 100.0F < chancePercent;

        if (blockBroken) {
            level.destroyBlock(pos, dropItems);
            level.levelEvent(2001, pos, Block.getId(state));
        }

        if (level instanceof ServerLevel serverLevel) {
            BlockParticleOption particleOption = new BlockParticleOption(ParticleTypes.BLOCK, state);
            serverLevel.sendParticles(particleOption, pos.getX() + 0.5D, pos.getY() + 0.5D, pos.getZ() + 0.5D, 25, 0.3D, 0.3D, 0.3D, 0.1D);
        }
    }
}