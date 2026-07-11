package com.dristmechanic.dristmechanic.entity;

import net.minecraft.core.BlockPos;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.goal.RemoveBlockGoal;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Blocks;
import org.jetbrains.annotations.NotNull;

public class RemoveCropGoal extends RemoveBlockGoal {
    private final int radius;
    private int ticksAnimating = 0;
    private boolean isAnimating = false;
    private final int attackImpactFrame;
    private final int animationLength;

    public RemoveCropGoal(PathfinderMob pMob, double pSpeedModifier, int pSearchRange, int radius) {
        super(Blocks.WHEAT, pMob, pSpeedModifier, pSearchRange);
        this.radius = radius;

        if (pMob instanceof AnimatedAttacker attacker) {
            this.attackImpactFrame = attacker.getAttackImpactFrame();
            this.animationLength = attacker.getAttackAnimationLength();
        } else {
            this.attackImpactFrame = 9;
            this.animationLength = 20;
        }
    }

    @Override
    protected boolean isValidTarget(@NotNull LevelReader level, @NotNull BlockPos pos) {
        return level.getBlockState(pos).is(BlockTags.CROPS);
    }

    @Override
    @SuppressWarnings("resource")
    public boolean canContinueToUse() {
        return this.mob.level().getBlockState(this.blockPos).is(BlockTags.CROPS);
    }

    @Override
    public void start() {
        super.start();
        ticksAnimating = 0;
        isAnimating = false;
    }

    @Override
    public void stop() {
        super.stop();
        ticksAnimating = 0;

        if (isAnimating && this.mob instanceof AnimatedAttacker attacker) {
            attacker.setAttackingState(false);
            isAnimating = false;
        }
    }

    @Override
    public void tick() {
        super.tick();

        double distSq = this.mob.distanceToSqr(this.blockPos.getX() + 0.5, this.blockPos.getY() + 0.5, this.blockPos.getZ() + 0.5);

        if (distSq <= 4.0) {
            this.mob.getNavigation().stop();
            this.mob.getLookControl().setLookAt(this.blockPos.getX() + 0.5, this.blockPos.getY() + 0.5, this.blockPos.getZ() + 0.5, 10.0F, 30.0F);

            if (!isAnimating) {
                if (this.mob instanceof AnimatedAttacker attacker) {
                    attacker.setAttackingState(true);
                    this.isAnimating = true;
                }
            }

            if (this.isAnimating) {
                this.ticksAnimating++;

                if (this.ticksAnimating == this.attackImpactFrame) {
                    destroyCrops(this.blockPos);
                }

                if (this.ticksAnimating >= this.animationLength) {
                    if (this.mob instanceof AnimatedAttacker attacker) {
                        attacker.setAttackingState(false);
                    }
                    this.isAnimating = false;
                    this.ticksAnimating = 0;
                }
            }
        } else {
            if (isAnimating && this.mob instanceof AnimatedAttacker attacker) {
                attacker.setAttackingState(false);
                isAnimating = false;
                ticksAnimating = 0;
            }
        }
    }

    @SuppressWarnings("resource")
    private void destroyCrops(BlockPos center) {
        for (int dx = -radius; dx <= radius; dx++) {
            for (int dy = -radius; dy <= radius; dy++) {
                for (int dz = -radius; dz <= radius; dz++) {
                    if (dx * dx + dy * dy + dz * dz <= radius * radius) {
                        BlockPos pos = center.offset(dx, dy, dz);
                        if (this.mob.level().getBlockState(pos).is(BlockTags.CROPS)) {
                            this.mob.level().destroyBlock(pos, false, this.mob);
                        }
                    }
                }
            }
        }
    }
}