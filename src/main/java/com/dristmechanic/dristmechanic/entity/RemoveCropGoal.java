package com.dristmechanic.dristmechanic.entity;

import com.dristmechanic.dristmechanic.handler.FarmManager;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.phys.Vec3;

import java.util.EnumSet;

public class RemoveCropGoal extends Goal {
    private final PathfinderMob mob;
    private final double speedModifier;
    private final int radius;
    private final int attackImpactFrame;
    private final int animationLength;

    private int ticksAnimating = 0;
    private boolean isAnimating = false;
    private BlockPos targetPos;
    private int nextStartTick = 0;
    private int stuckCheckTicks = 0;

    public RemoveCropGoal(PathfinderMob mob, double speedModifier, int radius) {
        this.mob = mob;
        this.speedModifier = speedModifier;
        this.radius = radius;
        this.setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));

        if (mob instanceof AnimatedAttacker attacker) {
            this.attackImpactFrame = attacker.getAttackImpactFrame();
            this.animationLength = attacker.getAttackAnimationLength();
        } else {
            this.attackImpactFrame = 9;
            this.animationLength = 20;
        }
    }

    @Override
    public boolean canUse() {
        if (this.nextStartTick > 0) {
            --this.nextStartTick;
            return false;
        }

        BlockPos nearest = findNearestCrop();
        if (nearest != null) {
            this.targetPos = nearest;
            this.stuckCheckTicks = 0;
            return true;
        }

        this.nextStartTick = 20;
        return false;
    }

    private BlockPos findNearestCrop() {
        if (!(this.mob.level() instanceof ServerLevel serverLevel)) return null;

        BlockPos nearest = null;
        double nearestDistSq = Double.MAX_VALUE;
        BlockPos mobPos = this.mob.blockPosition();

        for (FarmManager.FarmData farm : FarmManager.getData(serverLevel).getAllFarms()) {
            for (BlockPos crop : farm.getSpentCrops()) {
                double distSq = mobPos.distSqr(crop);
                if (distSq < nearestDistSq) {
                    nearestDistSq = distSq;
                    nearest = crop;
                }
            }
            for (BlockPos crop : farm.getRawCrops()) {
                double distSq = mobPos.distSqr(crop);
                if (distSq < nearestDistSq) {
                    nearestDistSq = distSq;
                    nearest = crop;
                }
            }
        }
        return nearest;
    }

    @Override
    public boolean canContinueToUse() {
        if (this.targetPos == null) return false;

        if (!this.mob.level().getBlockState(this.targetPos).is(BlockTags.CROPS)) {
            return false;
        }

        double distSq = this.mob.distanceToSqr(this.targetPos.getX() + 0.5, this.targetPos.getY() + 0.5, this.targetPos.getZ() + 0.5);

        if (distSq <= 4.0 && this.isAnimating) {
            this.stuckCheckTicks = 0;
            return true;
        }

        if (this.mob.getNavigation().isDone() && distSq > 4.0) {
            this.stuckCheckTicks++;
            if (this.stuckCheckTicks > 40) {
                return false;
            }
        } else {
            this.stuckCheckTicks = 0;
        }

        return true;
    }

    @Override
    public void start() {
        ticksAnimating = 0;
        isAnimating = false;
        this.mob.setAggressive(true);
        if (this.mob instanceof AnimatedAttacker aa) aa.resetStuckDetection();

        navigateToTarget();
    }

    private void navigateToTarget() {
        if (this.targetPos != null) {
            this.mob.getNavigation().moveTo(
                    this.targetPos.getX() + 0.5,
                    this.targetPos.getY(),
                    this.targetPos.getZ() + 0.5,
                    this.speedModifier
            );
        }
    }

    @Override
    public void stop() {
        ticksAnimating = 0;
        this.targetPos = null;
        this.mob.setAggressive(false);
        this.mob.getNavigation().stop();
        this.nextStartTick = 0;
        this.stuckCheckTicks = 0;

        if (this.mob instanceof AnimatedAttacker aa) {
            aa.resetStuckDetection();
            if (aa.getBreakingBlock() != null) {
                aa.setBreakingBlock(null);
                aa.setAttackingState(false);
            }
        }
        if (isAnimating && this.mob instanceof AnimatedAttacker attacker) {
            attacker.setAttackingState(false);
            isAnimating = false;
        }
    }

    @Override
    public void tick() {
        if (this.targetPos == null) return;

        if (this.mob instanceof AnimatedAttacker aa) {
            if (aa.getBreakingBlock() != null) {
                aa.tickBreakingBlock(this.mob, false);
            } else {
                aa.tickStuckDetection(this.mob, Vec3.atBottomCenterOf(this.targetPos), false);
            }
        }

        double distSq = this.mob.distanceToSqr(this.targetPos.getX() + 0.5, this.targetPos.getY() + 0.5, this.targetPos.getZ() + 0.5);

        if (distSq <= 4.0) {
            this.mob.getNavigation().stop();
            this.mob.getLookControl().setLookAt(this.targetPos.getX() + 0.5, this.targetPos.getY() + 0.5, this.targetPos.getZ() + 0.5, 10.0F, 30.0F);

            if (!isAnimating) {
                if (this.mob instanceof AnimatedAttacker attacker) {
                    attacker.setAttackingState(true);
                    this.isAnimating = true;
                }
            }

            if (this.isAnimating) {
                this.ticksAnimating++;
                if (this.ticksAnimating == this.attackImpactFrame) {
                    destroyCrops(this.targetPos);
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
            if (this.mob.tickCount % 20 == 0) {
                navigateToTarget();
            }

            if (isAnimating && this.mob instanceof AnimatedAttacker attacker) {
                attacker.setAttackingState(false);
                isAnimating = false;
                ticksAnimating = 0;
            }
        }
    }

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