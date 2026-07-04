package com.dristmechanic.dristmechanic.entity;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
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

public class SmartMeleeAttackGoal extends Goal {
    protected final PathfinderMob mob;
    private final double speedModifier;
    private final boolean followingTargetEvenIfNotSeen;

    // Анимация
    private final int attackAnimationLength;
    private final int impactFrame;
    private int animationFrame = -1;
    private int cooldown = 0;

    // Зоны атаки сущностей (Кайтинг)
    private final double closeDistanceSq;
    private final double attackReachSq;
    private final double extendedReachSq;
    private boolean hasClosedIn = false;
    private boolean hasUsedExtendedAttack = false;

    // Разрушение блоков
    private final double blockBreakReach;
    private final boolean dropItems;
    private static final double SPEED_THRESHOLD = 0.12D;

    // Навигация
    private int ticksUntilNextPathRecalculation;

    public SmartMeleeAttackGoal(PathfinderMob mob, double speedModifier, boolean followingTargetEvenIfNotSeen,
                                int animationTicks, int impactFrame,
                                double closeDistance, double attackReach, double extendedReach,
                                double blockBreakReach, boolean dropItems) {
        this.mob = mob;
        this.speedModifier = speedModifier;
        this.followingTargetEvenIfNotSeen = followingTargetEvenIfNotSeen;

        this.attackAnimationLength = animationTicks;
        this.impactFrame = impactFrame;

        this.closeDistanceSq = closeDistance * closeDistance;
        this.attackReachSq = attackReach * attackReach;
        this.extendedReachSq = extendedReach * extendedReach;

        this.blockBreakReach = blockBreakReach;
        this.dropItems = dropItems;

        this.setFlags(EnumSet.of(Goal.Flag.MOVE, Goal.Flag.LOOK));
    }

    @Override
    public boolean canUse() {
        LivingEntity target = this.mob.getTarget();
        if (target != null && target.isAlive()) return true;
        // Если цели нет, но моб агрессивен и застрял - включаемся, чтобы ломать стены
        return this.mob.isAggressive() && isStuck() && hasBlockInFront();
    }

    @Override
    public boolean canContinueToUse() {
        // Не прерываем анимацию и кулдаун, даже если цель исчезла
        if (animationFrame >= 0 || cooldown > 0) return true;

        LivingEntity target = this.mob.getTarget();
        if (target == null || !target.isAlive()) {
            return this.mob.isAggressive() && isStuck() && hasBlockInFront();
        }

        if (!this.followingTargetEvenIfNotSeen) {
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
    }

    @Override
    public void stop() {
        this.mob.setAggressive(false);
        this.mob.getNavigation().stop();
        if (this.animationFrame >= 0 && this.mob instanceof IAnimatedAttacker attacker) {
            attacker.setAttackingState(false);
            this.animationFrame = -1;
        }
        this.cooldown = 0;
    }

    @Override
    public boolean requiresUpdateEveryTick() {
        return true;
    }

    @Override
    public void tick() {
        Level level = mob.level();
        LivingEntity target = this.mob.getTarget();

        // 1. ФАЗА АНИМАЦИИ
        if (animationFrame >= 0) {
            animationFrame++;

            // МОМЕНТ ИСТИНЫ: Кадр удара (Impact Frame)
            if (animationFrame == impactFrame) {
                performStrike(level, target);
            }

            // Конец анимации
            if (animationFrame >= attackAnimationLength) {
                if (mob instanceof IAnimatedAttacker attacker) {
                    attacker.setAttackingState(false);
                }
                animationFrame = -1;
                cooldown = attackAnimationLength + 5; // Небольшая пауза перед следующим замахом
            }

            if (target != null) {
                this.mob.getLookControl().setLookAt(target, 30.0F, 30.0F);
            }
            return;
        }

        // 2. ФАЗА КУЛДАУНА
        if (cooldown > 0) {
            cooldown--;
            if (target != null) {
                this.mob.getLookControl().setLookAt(target, 30.0F, 30.0F);
                updatePathing(target);
            }
            return;
        }

        // 3. ФАЗА ПРИНЯТИЯ РЕШЕНИЯ (Бить или нет?)
        boolean canStrike = false;

        if (target != null && target.isAlive()) {
            this.mob.getLookControl().setLookAt(target, 30.0F, 30.0F);
            updatePathing(target);

            double distSqr = this.mob.distanceToSqr(target.getX(), target.getY(), target.getZ());

            // Логика кайтинга (сближение и вытягивание)
            if (distSqr <= this.closeDistanceSq) {
                hasClosedIn = true;
                hasUsedExtendedAttack = false;
            } else if (distSqr > this.extendedReachSq) {
                hasClosedIn = false;
            }

            if (hasClosedIn) {
                if (distSqr <= this.attackReachSq) {
                    canStrike = true;
                } else if (distSqr <= this.extendedReachSq && !hasUsedExtendedAttack) {
                    canStrike = true;
                    hasUsedExtendedAttack = true;
                }
            }
        } else {
            // Цели нет, но мы застряли - бьем стену
            if (this.mob.isAggressive() && isStuck() && hasBlockInFront()) {
                canStrike = true;
            }
        }

        if (canStrike) {
            startAnimation();
        }
    }

    private void startAnimation() {
        if (this.mob instanceof IAnimatedAttacker attacker) {
            attacker.setAttackingState(true);
            this.animationFrame = 0;
        }
    }

    private void updatePathing(LivingEntity target) {
        this.ticksUntilNextPathRecalculation = Math.max(this.ticksUntilNextPathRecalculation - 1, 0);
        if ((this.followingTargetEvenIfNotSeen || this.mob.getSensing().hasLineOfSight(target))
                && this.ticksUntilNextPathRecalculation <= 0) {
            this.ticksUntilNextPathRecalculation = 4 + this.mob.getRandom().nextInt(7);

            double distSqr = this.mob.distanceToSqr(target);
            if (distSqr > 1024.0D) this.ticksUntilNextPathRecalculation += 10;
            else if (distSqr > 256.0D) this.ticksUntilNextPathRecalculation += 5;

            if (!this.mob.getNavigation().moveTo(target, this.speedModifier)) {
                this.ticksUntilNextPathRecalculation += 15;
            }
        }
    }

    // ГЛАВНЫЙ МЕТОД: Куда смотрит, туда и бьет
    private void performStrike(Level level, LivingEntity target) {
        boolean hitEntity = false;

        // 1. Пытаемся ударить сущность
        if (target != null && target.isAlive()) {
            double distSqr = this.mob.distanceToSqr(target.getX(), target.getY(), target.getZ());
            double maxReach = this.extendedReachSq + (double)target.getBbWidth();
            if (distSqr <= maxReach) {
                this.mob.doHurtTarget(target);
                hitEntity = true;
            }
        }

        // 2. Если сущность не задета (или её нет), бьем блок перед собой
        if (!hitEntity && !level.isClientSide()) {
            Vec3 eyePos = this.mob.getEyePosition(1.0F);
            Vec3 lookVec = this.mob.getViewVector(1.0F);

            // Небольшой разброс, чтобы удар не был идеально "снайперским"
            double targetX = eyePos.x + lookVec.x * blockBreakReach + Mth.nextDouble(this.mob.getRandom(), -0.5, 0.5);
            double targetY = eyePos.y + lookVec.y * blockBreakReach + Mth.nextDouble(this.mob.getRandom(), -0.2, 0.5);
            double targetZ = eyePos.z + lookVec.z * blockBreakReach + Mth.nextDouble(this.mob.getRandom(), -0.5, 0.5);

            Vec3 targetPos = new Vec3(targetX, targetY, targetZ);
            ClipContext clipContext = new ClipContext(eyePos, targetPos, ClipContext.Block.OUTLINE, ClipContext.Fluid.NONE, this.mob);
            BlockHitResult hitResult = level.clip(clipContext);

            if (hitResult.getType() == HitResult.Type.BLOCK) {
                BlockPos pos = hitResult.getBlockPos();
                BlockState state = level.getBlockState(pos);
                if (!state.isAir() && !state.liquid() && state.getDestroySpeed(level, pos) >= 0.0F) {
                    attemptBreakBlock(level, pos, state);
                }
            }
        }
    }

    private void attemptBreakBlock(Level level, BlockPos pos, BlockState state) {
        float hardness = state.getDestroySpeed(level, pos);
        if (hardness < 0.0F) return;

        SoundType soundType = state.getSoundType();
        SoundEvent hitSound = soundType.getHitSound();

        level.playSound(null, pos, hitSound, SoundSource.BLOCKS,
                soundType.getVolume() * 0.5F, soundType.getPitch() * 0.875F);

        if (level instanceof ServerLevel serverLevel) {
            int particleCount = 5;
            float chancePercent = 100.0F / (1.0F + hardness * 2.5F);
            boolean blockBroken = this.mob.getRandom().nextFloat() * 100.0F < chancePercent;

            if (blockBroken) {
                particleCount = 15;
                level.destroyBlock(pos, dropItems);
                level.levelEvent(2001, pos, Block.getId(state));
            }

            BlockParticleOption particleOption = new BlockParticleOption(ParticleTypes.BLOCK, state);
            serverLevel.sendParticles(particleOption, pos.getX() + 0.5D, pos.getY() + 0.5D, pos.getZ() + 0.5D,
                    particleCount, 0.3D, 0.3D, 0.3D, 0.1D);
        } else {
            float chancePercent = 100.0F / (1.0F + hardness * 2.5F);
            if (this.mob.getRandom().nextFloat() * 100.0F < chancePercent) {
                level.destroyBlock(pos, dropItems);
                level.levelEvent(2001, pos, Block.getId(state));
            }
        }
    }

    private boolean isStuck() {
        double horizontalSpeed = mob.getDeltaMovement().multiply(1.0, 0.0, 1.0).length();
        return horizontalSpeed < SPEED_THRESHOLD;
    }

    private boolean hasBlockInFront() {
        Level level = mob.level();
        Vec3 eyePos = this.mob.getEyePosition(1.0F);
        Vec3 lookVec = this.mob.getViewVector(1.0F);

        Vec3 targetPos = eyePos.add(lookVec.scale(blockBreakReach));
        ClipContext clipContext = new ClipContext(eyePos, targetPos, ClipContext.Block.OUTLINE, ClipContext.Fluid.NONE, this.mob);
        BlockHitResult hitResult = level.clip(clipContext);

        if (hitResult.getType() == HitResult.Type.BLOCK) {
            BlockState state = level.getBlockState(hitResult.getBlockPos());
            return !state.isAir() && !state.liquid() && state.getDestroySpeed(level, hitResult.getBlockPos()) >= 0.0F;
        }
        return false;
    }
}