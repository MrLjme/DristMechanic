package com.dristmechanic.dristmechanic.entity;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockState;
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

    int getMiningTicks();
    void setMiningTicks(int ticks);
    int getMiningRequiredTicks();
    void setMiningRequiredTicks(int ticks);

    int STUCK_THRESHOLD = 15;
    double RAY_CAST_DISTANCE = 3.0D;
    float MOB_MINING_SPEED = 2.0F;

    default void startBreakingBlock(PathfinderMob mob, BlockPos pos) {
        if (!canBreakBlock(mob.level(), pos)) return;

        setBreakingBlock(pos);
        setMiningTicks(0);
        setMiningRequiredTicks(getMiningDurationTicks(mob, mob.level(), pos));
        setAttackingState(true);
        mob.getNavigation().stop();
    }

    default boolean tickBreakingBlock(PathfinderMob mob, boolean dropItems) {
        BlockPos pos = getBreakingBlock();
        if (pos == null) return false;

        int ticks = getMiningTicks() + 1;
        setMiningTicks(ticks);

        mob.getLookControl().setLookAt(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, 30.0F, 30.0F);

        int requiredTicks = getMiningRequiredTicks();
        int animationLength = getAttackAnimationLength();
        int impactFrame = getAttackImpactFrame();

        int animationCycle = (ticks - 1) % animationLength;
        if (animationCycle == impactFrame - 1) {
            playBreakSound(mob.level(), pos);
            spawnBreakParticles(mob.level(), pos);
        }

        if (ticks >= requiredTicks) {
            breakBlock(mob.level(), pos, dropItems);
            setAttackingState(false);
            setBreakingBlock(null);
            setMiningTicks(0);
            setMiningRequiredTicks(0);
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

        Vec3 mobPos = mob.position();
        Vec3 direction = target.subtract(mobPos).normalize();

        BlockPos.MutableBlockPos mutable = new BlockPos.MutableBlockPos();

        int searchRadius = 2;
        for (int dy = 0; dy <= 1; dy++) {
            for (int dx = -searchRadius; dx <= searchRadius; dx++) {
                for (int dz = -searchRadius; dz <= searchRadius; dz++) {
                    mutable.set(
                            (int) Math.floor(mobPos.x) + dx,
                            (int) Math.floor(mobPos.y) + dy,
                            (int) Math.floor(mobPos.z) + dz
                    );

                    if (canBreakBlock(level, mutable)) {
                        double dist = mobPos.distanceToSqr(Vec3.atBottomCenterOf(mutable));
                        if (dist <= RAY_CAST_DISTANCE * RAY_CAST_DISTANCE) {
                            return mutable.immutable();
                        }
                    }
                }
            }
        }

        return null;
    }

    default boolean canBreakBlock(Level level, BlockPos pos) {
        BlockState state = level.getBlockState(pos);
        if (state.isAir()) return false;
        float hardness = state.getDestroySpeed(level, pos);
        return hardness >= 0 && hardness <= 5.0F;
    }

    default int getMiningDurationTicks(PathfinderMob mob, Level level, BlockPos pos) {
        BlockState state = level.getBlockState(pos);
        if (state.isAir()) return 0;

        float hardness = state.getDestroySpeed(level, pos);
        if (hardness < 0) return Integer.MAX_VALUE;

        float digSpeed = MOB_MINING_SPEED;

        if (!mob.onGround()) {
            digSpeed /= 5.0F;
        }

        if (mob.isInWater()) {
            digSpeed /= 5.0F;
        }

        if (digSpeed <= 0) {
            return Integer.MAX_VALUE;
        }

        return (int) Math.ceil((1.0F / digSpeed) * hardness * 20.0F);
    }

    default void playBreakSound(Level level, BlockPos pos) {
        BlockState state = level.getBlockState(pos);
        if (state.isAir()) return;

        SoundType soundType = state.getSoundType(level, pos, null);
        level.playSound(null, pos, soundType.getHitSound(), SoundSource.BLOCKS,
                soundType.getVolume() * 0.5F, soundType.getPitch() * 0.875F);
    }

    default void spawnBreakParticles(Level level, BlockPos pos) {
        if (!(level instanceof ServerLevel serverLevel)) return;

        BlockState state = level.getBlockState(pos);
        if (state.isAir()) return;

        BlockParticleOption particleOption = new BlockParticleOption(ParticleTypes.BLOCK, state);
        serverLevel.sendParticles(particleOption,
                pos.getX() + 0.5D, pos.getY() + 0.5D, pos.getZ() + 0.5D,
                25, 0.3D, 0.3D, 0.3D, 0.1D);
    }

    default void breakBlock(Level level, BlockPos pos, boolean dropItems) {
        if (level.isClientSide()) return;

        BlockState state = level.getBlockState(pos);
        if (state.isAir()) return;

        level.destroyBlock(pos, dropItems);
        level.levelEvent(2001, pos, Block.getId(state));
    }
}