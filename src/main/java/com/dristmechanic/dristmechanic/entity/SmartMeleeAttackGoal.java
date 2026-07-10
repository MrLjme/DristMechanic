package com.dristmechanic.dristmechanic.entity;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import java.util.EnumSet;

@SuppressWarnings("resource")
public class SmartMeleeAttackGoal extends Goal {
    protected final PathfinderMob mob;
    private final double speedModifier;
    private final boolean followingTargetEvenIfNotSeen;
    private int ticksUntilNextPathRecalculation;
    private long lastCanUseTime;

    private final double closeDistanceSq;
    private final double attackReachSq;
    private final double extendedReachSq;

    private boolean hasClosedIn = false;
    private boolean hasUsedExtendedAttack = false;

    private final int attackAnimationLength;
    private final int attackInterval;
    private int attackAnimationTicks = 0;

    private final int attackImpactFrame;

    private final boolean dropBlockItems;
    private static final double RAY_CAST_DISTANCE = 3.0D;

    private int stuckTicks = 0;
    private Vec3 lastPos = null;
    private static final int STUCK_THRESHOLD = 15;

    private PendingAction pendingAction = null;
    private BlockPos pendingBlockPos = null;
    private LivingEntity pendingTarget = null;

    private boolean isAttacking = false;
    private boolean hasCompletedOneCycle = false;

    private Vec3 lastTargetVector = null;

    private enum PendingAction {
        BREAK_BLOCK, ATTACK_TARGET
    }

    public SmartMeleeAttackGoal(PathfinderMob mob, double speedModifier, boolean followingTargetEvenIfNotSeen,
                                int animationTicks, double closeDistance, double attackReach, double extendedReach,
                                boolean dropBlockItems) {
        this.mob = mob;
        this.speedModifier = speedModifier;
        this.followingTargetEvenIfNotSeen = followingTargetEvenIfNotSeen;
        this.closeDistanceSq = closeDistance * closeDistance;
        this.attackReachSq = attackReach * attackReach;
        this.extendedReachSq = extendedReach * extendedReach;
        this.dropBlockItems = dropBlockItems;

        this.attackAnimationLength = animationTicks;
        this.attackInterval = animationTicks;

        if (mob instanceof AnimatedAttacker attacker) {
            this.attackImpactFrame = attacker.getAttackImpactFrame();
        } else {
            this.attackImpactFrame = animationTicks / 2;
        }

        this.setFlags(EnumSet.of(Goal.Flag.MOVE, Goal.Flag.LOOK));
    }

    @Override
    public boolean canUse() {
        LivingEntity target = this.mob.getTarget();
        if (target != null && target.isAlive()) {
            return true;
        }
        return isAttacking;
    }

    @Override
    public boolean canContinueToUse() {
        if (isAttacking) return true;
        return canUse();
    }

    @Override
    public void start() {
        LivingEntity target = this.mob.getTarget();
        if (target != null) {
            this.mob.getNavigation().moveTo(target, this.speedModifier);
        }
        this.mob.setAggressive(true);
        this.ticksUntilNextPathRecalculation = 0;
        this.hasClosedIn = false;
        this.hasUsedExtendedAttack = false;
        this.attackAnimationTicks = 0;
        this.isAttacking = false;
        this.hasCompletedOneCycle = false;
        this.pendingAction = null;
        this.stuckTicks = 0;
        this.lastPos = null;
        this.lastTargetVector = null;
    }

    @Override
    public void stop() {
        this.mob.setAggressive(false);
        this.mob.getNavigation().stop();

        if (isAttacking && this.mob instanceof AnimatedAttacker attacker) {
            attacker.setAttackingState(false);
            this.isAttacking = false;
        }
        this.hasCompletedOneCycle = false;
        this.attackAnimationTicks = 0;
        this.pendingAction = null;
        this.stuckTicks = 0;
        this.lastPos = null;
        this.lastTargetVector = null;
    }

    @Override
    public boolean requiresUpdateEveryTick() {
        return true;
    }

    private boolean isOnCooldown() {
        long gameTime = this.mob.level().getGameTime();
        return gameTime - this.lastCanUseTime < (long) this.attackInterval;
    }

    @Override
    public void tick() {
        LivingEntity target = this.mob.getTarget();

        if (isAttacking) {
            boolean shouldStop = false;
            if (this.pendingAction == PendingAction.BREAK_BLOCK) {
                if (this.pendingBlockPos != null) {
                    BlockState state = this.mob.level().getBlockState(this.pendingBlockPos);
                    boolean isStillObstacle = !state.isAir() && state.getFluidState().isEmpty() && state.getDestroySpeed(this.mob.level(), this.pendingBlockPos) >= 0.0F;

                    if (!isStillObstacle) {
                        shouldStop = true;
                    } else if (target != null && this.lastTargetVector != null) {
                        double distSqr = this.mob.distanceToSqr(target);

                        if (distSqr <= this.closeDistanceSq) {
                            hasClosedIn = true;
                        }

                        if (canHitTarget(distSqr)) {
                            shouldStop = true;
                        } else {
                            Vec3 currentTargetVec = target.position().subtract(this.mob.position());
                            Vec3 currentHorizontalVec = new Vec3(currentTargetVec.x, 0.0D, currentTargetVec.z);

                            if (currentHorizontalVec.lengthSqr() > 0.01D) {
                                currentHorizontalVec = currentHorizontalVec.normalize();
                                double dot = this.lastTargetVector.dot(currentHorizontalVec);
                                if (dot < 0.65D) {
                                    shouldStop = true;
                                }
                            }
                        }
                    }
                } else {
                    shouldStop = true;
                }
            } else {
                if (target == null || !target.isAlive()) {
                    shouldStop = true;
                } else {
                    double distSqr = this.mob.distanceToSqr(target);
                    if (distSqr > this.extendedReachSq) {
                        shouldStop = true;
                    }
                }
            }

            if (this.pendingAction == PendingAction.BREAK_BLOCK && this.pendingBlockPos != null) {
                this.mob.getLookControl().setLookAt(this.pendingBlockPos.getX() + 0.5, this.pendingBlockPos.getY() + 0.5, this.pendingBlockPos.getZ() + 0.5, 30.0F, 30.0F);
            } else if (target != null) {
                this.mob.getLookControl().setLookAt(target, 30.0F, 30.0F);
            }

            this.attackAnimationTicks++;

            if (this.attackAnimationTicks == this.attackImpactFrame) {
                if (!isOnCooldown()) {
                    performPendingAction();
                    this.lastCanUseTime = this.mob.level().getGameTime();
                }
            }

            if (this.attackAnimationTicks >= this.attackAnimationLength) {
                this.attackAnimationTicks = 0;
                this.hasCompletedOneCycle = true;
            }

            if (shouldStop && this.hasCompletedOneCycle && this.attackAnimationTicks == 0) {
                isAttacking = false;
                if (this.mob instanceof AnimatedAttacker attacker) {
                    attacker.setAttackingState(false);
                }
                if (this.pendingAction == PendingAction.BREAK_BLOCK) {
                    this.stuckTicks = 0;
                }
                this.pendingAction = null;
                this.lastTargetVector = null;
            }

            return;
        }

        if (this.lastPos != null) {
            double moveDistSq = this.mob.position().distanceToSqr(this.lastPos);
            if (moveDistSq > 0.01D) {
                this.stuckTicks = 0;
            } else {
                this.stuckTicks++;
            }
        }
        this.lastPos = this.mob.position();

        if (target != null && target.isAlive()) {
            this.mob.getLookControl().setLookAt(target, 30.0F, 30.0F);
            double distSqr = this.mob.distanceToSqr(target);

            this.ticksUntilNextPathRecalculation = Math.max(this.ticksUntilNextPathRecalculation - 1, 0);
            if ((this.followingTargetEvenIfNotSeen || this.mob.getSensing().hasLineOfSight(target))
                    && this.ticksUntilNextPathRecalculation <= 0) {
                this.ticksUntilNextPathRecalculation = 4 + this.mob.getRandom().nextInt(7);
                if (distSqr > 1024.0D) this.ticksUntilNextPathRecalculation += 10;
                else if (distSqr > 256.0D) this.ticksUntilNextPathRecalculation += 5;

                if (!this.mob.getNavigation().moveTo(target, this.speedModifier)) {
                    this.ticksUntilNextPathRecalculation += 15;
                }
            }

            if (distSqr <= this.closeDistanceSq) {
                hasClosedIn = true;
                hasUsedExtendedAttack = false;
            } else if (distSqr > this.extendedReachSq) {
                hasClosedIn = false;
            }

            if (canHitTarget(distSqr)) {
                isAttacking = true;
                this.pendingAction = PendingAction.ATTACK_TARGET;
                this.pendingTarget = target;
                if (this.mob instanceof AnimatedAttacker attacker) {
                    attacker.setAttackingState(true);
                }
                this.attackAnimationTicks = 0;
                this.hasCompletedOneCycle = false;
                return;
            }
        } else {
            this.mob.getNavigation().stop();
        }

        if (isOnCooldown()) {
            return;
        }

        planNextAttack(target);
    }

    private void planNextAttack(LivingEntity target) {
        Level level = this.mob.level();
        if (level.isClientSide()) return;

        if (target == null || !target.isAlive()) {
            return;
        }

        boolean isStuck = (this.stuckTicks >= STUCK_THRESHOLD);

        if (isStuck) {
            Vec3 mobBottom = this.mob.position();
            Vec3 targetCenter = target.position().add(0, target.getBbHeight() / 2.0, 0);

            double dxH = targetCenter.x - mobBottom.x;
            double dy = targetCenter.y - mobBottom.y;
            double dzH = targetCenter.z - mobBottom.z;
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

                double heightThreshold = 1.0D;
                if (dy > heightThreshold) {
                    aimVec = new Vec3(horizontalVec.x, 1.0D, horizontalVec.z).normalize();
                } else if (dy < -heightThreshold) {
                    aimVec = new Vec3(horizontalVec.x, -1.0D, horizontalVec.z).normalize();
                } else {
                    aimVec = horizontalVec;
                }
            }

            Vec3 startPosHead = this.mob.position().add(0.0D, 1.5D, 0.0D);
            Vec3 startPosLegs = this.mob.position().add(0.0D, 0.5D, 0.0D);

            Vec3 endPosHead = startPosHead.add(aimVec.scale(RAY_CAST_DISTANCE));
            Vec3 endPosLegs = startPosLegs.add(aimVec.scale(RAY_CAST_DISTANCE));

            ClipContext contextHead = new ClipContext(startPosHead, endPosHead, ClipContext.Block.OUTLINE, ClipContext.Fluid.NONE, this.mob);
            ClipContext contextLegs = new ClipContext(startPosLegs, endPosLegs, ClipContext.Block.OUTLINE, ClipContext.Fluid.NONE, this.mob);

            BlockHitResult hitResultHead = level.clip(contextHead);
            BlockHitResult hitResultLegs = level.clip(contextLegs);

            BlockPos posHead = null;
            BlockPos posLegs = null;
            double distHead = Double.MAX_VALUE;
            double distLegs = Double.MAX_VALUE;

            if (isValidBlockHit(hitResultHead, level)) {
                distHead = hitResultHead.getLocation().distanceToSqr(startPosHead);
                posHead = hitResultHead.getBlockPos();
            }
            if (isValidBlockHit(hitResultLegs, level)) {
                distLegs = hitResultLegs.getLocation().distanceToSqr(startPosLegs);
                posLegs = hitResultLegs.getBlockPos();
            }

            BlockPos bestPos;
            if (posHead == null) {
                bestPos = posLegs;
            } else if (posLegs == null) {
                bestPos = posHead;
            } else {
                bestPos = distHead < distLegs ? posHead : posLegs;
            }

            if (bestPos != null) {
                this.pendingAction = PendingAction.BREAK_BLOCK;
                this.pendingBlockPos = bestPos;

                this.mob.getLookControl().setLookAt(bestPos.getX() + 0.5, bestPos.getY() + 0.5, bestPos.getZ() + 0.5, 30.0F, 30.0F);

                Vec3 targetVec = target.position().subtract(this.mob.position());
                this.lastTargetVector = new Vec3(targetVec.x, 0.0D, targetVec.z).normalize();

                this.stuckTicks = 0;
                startAnimation();
            }
        }
    }

    private boolean isValidBlockHit(BlockHitResult hitResult, Level level) {
        if (hitResult.getType() != HitResult.Type.BLOCK) return false;
        BlockState state = level.getBlockState(hitResult.getBlockPos());
        return !state.isAir() && state.getFluidState().isEmpty() && state.getDestroySpeed(level, hitResult.getBlockPos()) >= 0.0F;
    }

    private boolean canHitTarget(double distSqr) {
        if (!hasClosedIn) return false;

        if (distSqr <= this.attackReachSq) {
            return true;
        } else if (distSqr <= this.extendedReachSq && !hasUsedExtendedAttack) {
            hasUsedExtendedAttack = true;
            return true;
        }
        return false;
    }

    private void startAnimation() {
        isAttacking = true;
        this.attackAnimationTicks = 0;
        this.hasCompletedOneCycle = false;

        if (this.mob instanceof AnimatedAttacker attacker) {
            attacker.setAttackingState(true);
        } else {
            performPendingAction();
            this.lastCanUseTime = this.mob.level().getGameTime();
            isAttacking = false;
            this.pendingAction = null;
            this.lastTargetVector = null;
        }
    }

    private void performPendingAction() {
        if (this.pendingAction == PendingAction.BREAK_BLOCK && this.pendingBlockPos != null) {
            attemptBreakBlock(this.pendingBlockPos);
        } else if (this.pendingAction == PendingAction.ATTACK_TARGET && this.pendingTarget != null) {
            if (this.pendingTarget.isAlive()) {
                double distSqr = this.mob.distanceToSqr(this.pendingTarget);
                double maxReach = Math.max(Math.sqrt(this.attackReachSq), Math.sqrt(this.extendedReachSq));
                double allowedReachSq = maxReach * maxReach;
                if (distSqr <= allowedReachSq) {
                    this.mob.doHurtTarget(this.pendingTarget);
                }
            }
        }
    }

    private void attemptBreakBlock(BlockPos pos) {
        Level level = this.mob.level();
        BlockState state = level.getBlockState(pos);
        if (state.isAir() || !state.getFluidState().isEmpty()) return;

        float hardness = state.getDestroySpeed(level, pos);
        if (hardness < 0.0F) return;

        SoundType soundType = state.getSoundType(level, pos, this.mob);
        SoundEvent hitSound = soundType.getHitSound();

        level.playSound(null, pos, hitSound, SoundSource.BLOCKS,
                soundType.getVolume() * 0.5F, soundType.getPitch() * 0.875F);

        float chancePercent = 100.0F / (1.0F + hardness * 2.5F);
        boolean blockBroken = this.mob.getRandom().nextFloat() * 100.0F < chancePercent;

        if (blockBroken) {
            level.destroyBlock(pos, dropBlockItems);
            level.levelEvent(2001, pos, Block.getId(state));
        }

        if (level instanceof ServerLevel serverLevel) {
            BlockParticleOption particleOption = new BlockParticleOption(ParticleTypes.BLOCK, state);
            double centerX = pos.getX() + 0.5D;
            double centerY = pos.getY() + 0.5D;
            double centerZ = pos.getZ() + 0.5D;

            serverLevel.sendParticles(particleOption, centerX, centerY, centerZ,
                    25, 0.3D, 0.3D, 0.3D, 0.1D);
        }
    }
}