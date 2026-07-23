package com.dristmechanic.dristmechanic.entity;

import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.phys.Vec3;

import java.util.EnumSet;

@SuppressWarnings("resource")
public class SmartMeleeAttackGoal extends Goal {
    protected final PathfinderMob mob;
    private final double speedModifier;
    private final boolean followingTargetEvenIfNotSeen;
    private int ticksUntilNextPathRecalculation;
    private long lastCanUseTime;

    private final double approachDistanceSq;
    private final double attackReachSq;
    private final double extendedReachSq;
    private final double attackAngleCos;

    private final int attackAnimationLength;
    private final int attackInterval;
    private int attackAnimationTicks = 0;
    private final int attackImpactFrame;

    private final boolean dropBlockItems;

    private LivingEntity pendingTarget = null;

    private boolean isAttacking = false;
    private boolean hasCompletedOneCycle = false;

    public SmartMeleeAttackGoal(PathfinderMob mob, double speedModifier, boolean followingTargetEvenIfNotSeen,
                                int animationTicks, double approachDistance, double attackReach, double extendedReach,
                                double attackAngleDegrees, boolean dropBlockItems) {
        this.mob = mob;
        this.speedModifier = speedModifier;
        this.followingTargetEvenIfNotSeen = followingTargetEvenIfNotSeen;
        this.approachDistanceSq = approachDistance * approachDistance;
        this.attackReachSq = attackReach * attackReach;
        this.extendedReachSq = extendedReach * extendedReach;
        this.attackAngleCos = Math.cos(Math.toRadians(attackAngleDegrees));
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
        if (this.mob instanceof AnimatedAttacker aa && aa.getBreakingBlock() != null) return true;
        return canUse();
    }

    @Override
    public void start() {
        LivingEntity target = this.mob.getTarget();
        if (target != null) {
            this.mob.getNavigation().moveTo(target.getX(), target.getY(), target.getZ(), this.speedModifier);
        }
        this.mob.setAggressive(true);
        this.ticksUntilNextPathRecalculation = 0;
        this.attackAnimationTicks = 0;
        this.isAttacking = false;
        this.hasCompletedOneCycle = false;
        this.pendingTarget = null;
    }

    @Override
    public void stop() {
        this.mob.setAggressive(false);
        this.mob.getNavigation().stop();

        if (isAttacking && this.mob instanceof AnimatedAttacker attacker) {
            attacker.setAttackingState(false);
            this.isAttacking = false;
        }

        if (this.mob instanceof AnimatedAttacker aa) {
            if (aa.getBreakingBlock() != null) {
                aa.setBreakingBlock(null);
                aa.setAttackingState(false);
            }
            aa.resetStuckDetection();
        }

        this.hasCompletedOneCycle = false;
        this.attackAnimationTicks = 0;
        this.pendingTarget = null;
    }

    @Override
    public boolean requiresUpdateEveryTick() { return true; }

    private boolean isOnCooldown() {
        long gameTime = this.mob.level().getGameTime();
        return gameTime - this.lastCanUseTime < (long) this.attackInterval;
    }

    private boolean isInAttackAngle(LivingEntity target) {
        float bodyYaw = this.mob.yBodyRot;
        float rad = bodyYaw * ((float)Math.PI / 180.0F);

        Vec3 bodyVector = new Vec3((double)(-Mth.sin(rad)), 0.0D, (double)Mth.cos(rad));
        Vec3 toTarget = target.position().subtract(this.mob.position());
        Vec3 horizontalToTarget = new Vec3(toTarget.x, 0.0D, toTarget.z);

        if (horizontalToTarget.lengthSqr() > 0.01D) {
            horizontalToTarget = horizontalToTarget.normalize();
            return bodyVector.dot(horizontalToTarget) >= this.attackAngleCos;
        }
        return true;
    }

    @Override
    public void tick() {
        LivingEntity target = this.mob.getTarget();

        // 1. Глобальная обработка пробивания блоков (от интерфейса)
        if (this.mob instanceof AnimatedAttacker aa && aa.getBreakingBlock() != null) {
            this.mob.getNavigation().stop();
            aa.tickBreakingBlock(this.mob, this.dropBlockItems);
            return;
        }

        // 2. Обработка атаки по живой цели
        if (isAttacking) {
            this.mob.getNavigation().stop();

            boolean shouldStop = false;
            if (target == null || !target.isAlive()) {
                shouldStop = true;
            } else {
                double distSqr = this.mob.distanceToSqr(target);
                if (distSqr > this.extendedReachSq || !isInAttackAngle(target)) {
                    shouldStop = true;
                }
            }

            if (target != null) {
                this.mob.getLookControl().setLookAt(target, 30.0F, 30.0F);
            }

            this.attackAnimationTicks++;

            if (this.attackAnimationTicks == this.attackImpactFrame) {
                if (!isOnCooldown()) {
                    if (this.pendingTarget != null && this.pendingTarget.isAlive()) {
                        double distSqr = this.mob.distanceToSqr(this.pendingTarget);
                        if (distSqr <= this.extendedReachSq && isInAttackAngle(this.pendingTarget)) {
                            this.mob.doHurtTarget(this.pendingTarget);
                        }
                    }
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
                this.pendingTarget = null;
            }

            return;
        }

        // 3. Движение к цели и проверка застревания
        if (target != null && target.isAlive()) {
            this.mob.getLookControl().setLookAt(target, 30.0F, 30.0F);
            double distSqr = this.mob.distanceToSqr(target);

            this.ticksUntilNextPathRecalculation = Math.max(this.ticksUntilNextPathRecalculation - 1, 0);

            if (distSqr > this.approachDistanceSq) {
                if (this.ticksUntilNextPathRecalculation <= 0) {
                    this.ticksUntilNextPathRecalculation = 4 + this.mob.getRandom().nextInt(7);
                    if (distSqr > 1024.0D) this.ticksUntilNextPathRecalculation += 10;
                    else if (distSqr > 256.0D) this.ticksUntilNextPathRecalculation += 5;

                    if (!this.mob.getNavigation().moveTo(target.getX(), target.getY(), target.getZ(), this.speedModifier)) {
                        this.ticksUntilNextPathRecalculation += 15;
                    }
                } else if (this.mob.getNavigation().isDone()) {
                    this.mob.getNavigation().moveTo(target.getX(), target.getY(), target.getZ(), this.speedModifier);
                }
            }

            if (distSqr <= this.attackReachSq && isInAttackAngle(target)) {
                if (!isOnCooldown()) {
                    isAttacking = true;
                    this.mob.getNavigation().stop();
                    this.pendingTarget = target;
                    if (this.mob instanceof AnimatedAttacker attacker) {
                        attacker.setAttackingState(true);
                    }
                    this.attackAnimationTicks = 0;
                    this.hasCompletedOneCycle = false;
                    return;
                }
            }

            // Делегируем проверку препятствий интерфейсу
            Vec3 targetCenter = target.position().add(0, target.getBbHeight() / 2.0, 0);
            if (this.mob instanceof AnimatedAttacker aa) {
                aa.tickStuckDetection(this.mob, targetCenter, this.dropBlockItems);
            }

        } else {
            this.mob.getNavigation().stop();
        }
    }
}