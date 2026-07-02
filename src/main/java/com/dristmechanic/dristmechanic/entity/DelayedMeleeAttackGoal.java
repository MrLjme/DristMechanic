package com.dristmechanic.dristmechanic.entity;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.goal.Goal;
import java.util.EnumSet;

public class DelayedMeleeAttackGoal extends Goal {
    private final Mob mob;
    private final double speedModifier;
    private final boolean followingTargetEvenIfNotSeen;
    private int attackCooldown;
    private int attackDelay;
    private final int attackDelayTicks;
    private final double attackReachSqr;
    private int ticksUntilNextPathRecalculation;

    public DelayedMeleeAttackGoal(Mob mob, double speedModifier, boolean followingTargetEvenIfNotSeen, int attackDelayTicks) {
        this.mob = mob;
        this.speedModifier = speedModifier;
        this.followingTargetEvenIfNotSeen = followingTargetEvenIfNotSeen;
        this.attackDelayTicks = attackDelayTicks;
        this.attackReachSqr = 2.25; // 1.5 блока
        this.setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
    }

    @Override
    public boolean canUse() {
        LivingEntity target = this.mob.getTarget();
        return target != null && target.isAlive();
    }

    @Override
    public boolean canContinueToUse() {
        LivingEntity target = this.mob.getTarget();
        if (target == null || !target.isAlive()) {
            return false;
        }
        if (!this.followingTargetEvenIfNotSeen) {
            return !this.mob.getNavigation().isDone();
        }
        return this.mob.isWithinRestriction(target.blockPosition());
    }

    @Override
    public void start() {
        this.attackCooldown = 0;
        this.attackDelay = 0;
        this.ticksUntilNextPathRecalculation = 0;
        this.mob.setAggressive(true);
    }

    @Override
    public void stop() {
        this.mob.setAggressive(false);
        this.mob.getNavigation().stop();
    }

    @Override
    public void tick() {
        LivingEntity target = this.mob.getTarget();
        if (target == null) {
            return;
        }

        boolean canSee = this.mob.getSensing().hasLineOfSight(target);
        if (canSee) {
            this.mob.getLookControl().setLookAt(target, 30.0F, 30.0F);
        }

        if (--this.ticksUntilNextPathRecalculation <= 0) {
            this.ticksUntilNextPathRecalculation = 4 + this.mob.getRandom().nextInt(7);
            this.mob.getNavigation().moveTo(target, this.speedModifier);
        }

        double distanceSqr = this.mob.distanceToSqr(target.getX(), target.getY(), target.getZ());

        // Если в радиусе атаки и кулдаун прошёл – начинаем атаку
        if (distanceSqr <= this.attackReachSqr && this.attackCooldown <= 0) {
            this.attackDelay = this.attackDelayTicks;
            this.attackCooldown = 20; // базовый кулдаун между атаками

            // Включаем анимацию атаки через наш метод
            if (this.mob instanceof TotebotEntity totebot) {
                totebot.startAttackAnimation();
            }
            // Также вызываем swing, чтобы было видно взмах руки (опционально)
            this.mob.swing(this.mob.getUsedItemHand());
        }

        // Задержка перед нанесением урона
        if (this.attackDelay > 0) {
            this.attackDelay--;
            if (this.attackDelay == 0 && target.isAlive()) {
                double currentDist = this.mob.distanceToSqr(target.getX(), target.getY(), target.getZ());
                if (currentDist <= this.attackReachSqr) {
                    this.mob.doHurtTarget(target);
                }
            }
        }

        if (this.attackCooldown > 0) {
            this.attackCooldown--;
        }
    }
}