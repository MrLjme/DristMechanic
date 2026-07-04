package com.dristmechanic.dristmechanic.entity;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.goal.Goal;

import java.util.EnumSet;

public class DelayedMeleeAttackGoal extends Goal {
    protected final PathfinderMob mob;
    private final double speedModifier;
    private final boolean followingTargetEvenIfNotSeen;
    private int ticksUntilNextPathRecalculation;
    private long lastCanUseTime;

    // Кастомные параметры зон
    private final double closeDistanceSq;
    private final double attackReachSq;
    private final double extendedReachSq;

    // Состояния ИИ
    private boolean hasClosedIn = false;
    private boolean hasUsedExtendedAttack = false;

    // Анимация и динамический кулдаун
    private final int attackAnimationLength;
    private final int attackInterval; // Теперь зависит от анимации!
    private int attackAnimationTicks = 0;

    /**
     * @param mob Моб (должен быть PathfinderMob)
     * @param speedModifier Модификатор скорости
     * @param followingTargetEvenIfNotSeen Преследовать ли без зрения
     * @param animationTicks Длина анимации в тиках
     * @param closeDistance Дистанция сближения (например, 1.0 блок)
     * @param attackReach Основной радиус атаки (например, 2.0 блока)
     * @param extendedReach Радиус вытягивания для 1 удара (например, 3.0 блока)
     */
    public DelayedMeleeAttackGoal(PathfinderMob mob, double speedModifier, boolean followingTargetEvenIfNotSeen,
                                  int animationTicks, double closeDistance, double attackReach, double extendedReach) {
        this.mob = mob;
        this.speedModifier = speedModifier;
        this.followingTargetEvenIfNotSeen = followingTargetEvenIfNotSeen;
        this.closeDistanceSq = closeDistance * closeDistance;
        this.attackReachSq = attackReach * attackReach;
        this.extendedReachSq = extendedReach * extendedReach;

        this.attackAnimationLength = animationTicks;
        // Частота атаки = длина анимации + 5 тиков (0.25 сек) на паузу между ударами
        this.attackInterval = animationTicks + 5;

        this.setFlags(EnumSet.of(Goal.Flag.MOVE, Goal.Flag.LOOK));
    }

    @Override
    public boolean canUse() {
        LivingEntity target = this.mob.getTarget();
        if (target == null || !target.isAlive()) {
            return false;
        }
        return this.getAttackReachSqr(target) >= this.mob.distanceToSqr(target.getX(), target.getY(), target.getZ())
                || this.mob.getNavigation().createPath(target, 0) != null;
    }

    @Override
    public boolean canContinueToUse() {
        LivingEntity target = this.mob.getTarget();
        if (target == null || !target.isAlive()) {
            return false;
        } else if (!this.followingTargetEvenIfNotSeen) {
            return !this.mob.getNavigation().isDone();
        } else {
            return this.mob.isWithinRestriction(target.blockPosition());
        }
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
    }

    @Override
    public void stop() {
        this.mob.setAggressive(false);
        this.mob.getNavigation().stop();

        if (this.attackAnimationTicks > 0 && this.mob instanceof IAnimatedAttacker attacker) {
            attacker.setAttackingState(false);
            this.attackAnimationTicks = 0;
        }
    }

    @Override
    public boolean requiresUpdateEveryTick() {
        return true;
    }

    @Override
    public void tick() {
        LivingEntity target = this.mob.getTarget();
        if (target == null) return;

        this.mob.getLookControl().setLookAt(target, 30.0F, 30.0F);
        double distSqr = this.mob.distanceToSqr(target.getX(), target.getY(), target.getZ());

        this.ticksUntilNextPathRecalculation = Math.max(this.ticksUntilNextPathRecalculation - 1, 0);
        if ((this.followingTargetEvenIfNotSeen || this.mob.getSensing().hasLineOfSight(target))
                && this.ticksUntilNextPathRecalculation <= 0) {
            this.ticksUntilNextPathRecalculation = 4 + this.mob.getRandom().nextInt(7);

            if (distSqr > 1024.0D) {
                this.ticksUntilNextPathRecalculation += 10;
            } else if (distSqr > 256.0D) {
                this.ticksUntilNextPathRecalculation += 5;
            }

            if (!this.mob.getNavigation().moveTo(target, this.speedModifier)) {
                this.ticksUntilNextPathRecalculation += 15;
            }
        }

        this.checkAndPerformAttack(target, distSqr);

        // Тик анимации
        if (this.attackAnimationTicks > 0) {
            this.attackAnimationTicks--;
            if (this.attackAnimationTicks == 0 && this.mob instanceof IAnimatedAttacker attacker) {
                attacker.setAttackingState(false);
            }
        }
    }

    protected void checkAndPerformAttack(LivingEntity target, double distToTargetSqr) {
        // 1. Логика сближения (Стейт-машина)
        if (distToTargetSqr <= this.closeDistanceSq) {
            hasClosedIn = true;
            hasUsedExtendedAttack = false;
        } else if (distToTargetSqr > this.extendedReachSq) {
            hasClosedIn = false;
        }

        // 2. Защита: не атакуем, если уже отыгрывается анимация замаха
        if (this.attackAnimationTicks > 0) {
            return;
        }

        // 3. Проверка кулдауна (ДИНАМИЧЕСКАЯ: зависит от длины анимации)
        long gameTime = this.mob.level().getGameTime();
        if (gameTime - this.lastCanUseTime < (long)this.attackInterval) {
            return;
        }

        double maxReach = this.getAttackReachSqr(target);
        if (distToTargetSqr <= maxReach) {
            boolean canPerformAttack = false;

            if (hasClosedIn) {
                if (distToTargetSqr <= this.attackReachSq) {
                    canPerformAttack = true; // Стандартный спам-удар
                } else if (distToTargetSqr <= this.extendedReachSq && !hasUsedExtendedAttack) {
                    canPerformAttack = true; // "Вытянутый" удар
                    hasUsedExtendedAttack = true;
                }
            }

            if (canPerformAttack) {
                this.lastCanUseTime = gameTime; // Запускаем отсчет кулдауна

                if (this.mob instanceof IAnimatedAttacker attacker) {
                    attacker.setAttackingState(true);
                    this.attackAnimationTicks = attacker.getAttackAnimationLength();
                }

                this.mob.doHurtTarget(target);
            }
        }
    }

    protected double getAttackReachSqr(LivingEntity target) {
        return this.extendedReachSq + (double)target.getBbWidth();
    }
}