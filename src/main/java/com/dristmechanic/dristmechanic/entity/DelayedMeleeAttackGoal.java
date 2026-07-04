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

    private int attackAnimationTicks;

    /**
     * @param attackDelayTicks Задержка в тиках до нанесения урона (должна совпадать с кадром удара в BlockBench)
     * @param attackReach Радиус атаки в блоках (например, 1.5)
     */
    public DelayedMeleeAttackGoal(Mob mob, double speedModifier, boolean followingTargetEvenIfNotSeen, int attackDelayTicks, double attackReach) {
        this.mob = mob;
        this.speedModifier = speedModifier;
        this.followingTargetEvenIfNotSeen = followingTargetEvenIfNotSeen;
        this.attackDelayTicks = attackDelayTicks;
        this.attackReachSqr = attackReach * attackReach;
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
        this.attackAnimationTicks = 0;
        this.mob.setAggressive(true);
    }

    @Override
    public void stop() {
        this.mob.setAggressive(false);
        this.mob.getNavigation().stop();
        if (this.mob instanceof IAnimatedAttacker attacker) {
            attacker.setAttackingState(false);
        }
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

        double distanceSqr = this.mob.distanceToSqr(target.getX(), target.getY(), target.getZ());
        boolean isWithinReach = distanceSqr <= this.attackReachSqr;

        // Остановка навигации при входе в зону атаки (исправляет физический фриз)
        if (isWithinReach) {
            this.mob.getNavigation().stop();
        } else if (--this.ticksUntilNextPathRecalculation <= 0) {
            this.ticksUntilNextPathRecalculation = 4 + this.mob.getRandom().nextInt(7);
            this.mob.getNavigation().moveTo(target, this.speedModifier);
        }

        if (isWithinReach && this.attackCooldown <= 0) {
            this.attackDelay = this.attackDelayTicks;
            this.attackCooldown = 13;

            // Запуск анимации через универсальный интерфейс
            if (this.mob instanceof IAnimatedAttacker attacker) {
                attacker.setAttackingState(true);
                this.attackAnimationTicks = attacker.getAttackAnimationLength();
            }

            this.mob.swing(this.mob.getUsedItemHand());
        }

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

        // Автоматический сброс флага анимации по таймеру
        if (this.attackAnimationTicks > 0) {
            this.attackAnimationTicks--;
            if (this.attackAnimationTicks == 0 && this.mob instanceof IAnimatedAttacker attacker) {
                attacker.setAttackingState(false);
            }
        }
    }
}