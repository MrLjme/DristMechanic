package com.dristmechanic.dristmechanic.entity;

import net.minecraft.core.BlockPos;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.goal.RemoveBlockGoal;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

public class RemoveCropGoal extends RemoveBlockGoal {
    private final int radius;
    private boolean hasDestroyed = false;
    private int animationTicks = 0;
    private boolean isAnimating = false;
    private final int attackImpactFrame;

    public RemoveCropGoal(PathfinderMob pMob, double pSpeedModifier, int pSearchRange, int radius) {
        super(Blocks.WHEAT, pMob, pSpeedModifier, pSearchRange);
        this.radius = radius;

        // Получаем кадр удара из моба
        if (pMob instanceof AnimatedAttacker attacker) {
            this.attackImpactFrame = attacker.getAttackImpactFrame();
        } else {
            this.attackImpactFrame = 9; // Fallback
        }
    }

    @Override
    protected boolean isValidTarget(LevelReader level, BlockPos pos) {
        BlockState state = level.getBlockState(pos);
        return state.is(BlockTags.CROPS);
    }

    @Override
    public boolean canContinueToUse() {
        if (hasDestroyed) return false;

        BlockPos target = this.blockPos;
        if (target == null) return false;

        Level level = (Level) this.mob.level();
        return level.getBlockState(target).is(BlockTags.CROPS);
    }

    @Override
    public void start() {
        super.start();
        hasDestroyed = false;
        animationTicks = 0;
        isAnimating = false;
    }

    @Override
    public void stop() {
        super.stop();
        hasDestroyed = false;
        animationTicks = 0;

        if (isAnimating && this.mob instanceof AnimatedAttacker attacker) {
            attacker.setAttackingState(false);
            isAnimating = false;
        }
    }

    @Override
    public void tick() {
        super.tick();
        if (hasDestroyed) return;

        BlockPos target = this.blockPos;
        if (target == null) return;

        if (this.mob.distanceToSqr(target.getX() + 0.5, target.getY() + 0.5, target.getZ() + 0.5) <= 4.0) {

            if (!isAnimating) {
                if (this.mob instanceof AnimatedAttacker attacker) {
                    attacker.setAttackingState(true);
                    this.animationTicks = attacker.getAttackAnimationLength();
                    this.isAnimating = true;
                } else {
                    destroyCrops();
                    return;
                }
            }

            if (this.animationTicks > 0) {
                this.animationTicks--;

                // ВЫЧИСЛЯЕМ ПРОШЕДШИЕ КАДРЫ
                int animationLength = 0;
                if (this.mob instanceof AnimatedAttacker attacker) {
                    animationLength = attacker.getAttackAnimationLength();
                }
                int elapsedFrames = animationLength - this.animationTicks;

                // УДАР НА ДИНАМИЧЕСКОМ КАДРЕ!
                if (elapsedFrames == this.attackImpactFrame) {
                    destroyCrops();
                }

                if (this.animationTicks == 0) {
                    if (this.mob instanceof AnimatedAttacker attacker) {
                        attacker.setAttackingState(false);
                        this.isAnimating = false;
                    }
                }
            }
        }
    }

    private void destroyCrops() {
        Level world = (Level) this.mob.level();
        BlockPos center = this.mob.blockPosition();

        for (int dx = -radius; dx <= radius; dx++) {
            for (int dy = -radius; dy <= radius; dy++) {
                for (int dz = -radius; dz <= radius; dz++) {
                    if (dx * dx + dy * dy + dz * dz <= radius * radius) {
                        BlockPos pos = center.offset(dx, dy, dz);
                        BlockState state = world.getBlockState(pos);
                        if (state.is(BlockTags.CROPS)) {
                            world.destroyBlock(pos, false, this.mob);
                        }
                    }
                }
            }
        }
        hasDestroyed = true;
    }
}