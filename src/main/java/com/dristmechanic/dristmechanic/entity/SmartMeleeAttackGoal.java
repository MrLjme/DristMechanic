package com.dristmechanic.dristmechanic.entity;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
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

    // Динамический кадр удара - берется из моба!
    private final int attackImpactFrame;

    private final boolean dropBlockItems;
    private static final double RAYCAST_DISTANCE = 5.0D;
    private static final double SPREAD_XZ = 0.7D;
    private static final double SPREAD_Y_MIN = -0.1D;
    private static final double SPREAD_Y_MAX = 0.7D;
    private static final double SPEED_THRESHOLD = 0.12D;

    private PendingAction pendingAction = null;
    private BlockPos pendingBlockPos = null;
    private LivingEntity pendingTarget = null;

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
        this.attackInterval = animationTicks + 5;

        // Получаем кадр удара из моба (если он реализует интерфейс)
        if (mob instanceof IAnimatedAttacker attacker) {
            this.attackImpactFrame = attacker.getAttackImpactFrame();
        } else {
            // Fallback: удар в середине анимации
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

        if (attackAnimationTicks > 0 || !isCooldownFinished()) {
            return true;
        }

        double horizontalSpeed = mob.getDeltaMovement().multiply(1.0, 0.0, 1.0).length();
        return horizontalSpeed < SPEED_THRESHOLD;
    }

    @Override
    public boolean canContinueToUse() {
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
        this.pendingAction = null;
    }

    @Override
    public void stop() {
        this.mob.setAggressive(false);
        this.mob.getNavigation().stop();

        if (this.attackAnimationTicks > 0 && this.mob instanceof IAnimatedAttacker attacker) {
            attacker.setAttackingState(false);
            this.attackAnimationTicks = 0;
        }
        this.pendingAction = null;
    }

    @Override
    public boolean requiresUpdateEveryTick() {
        return true;
    }

    private boolean isCooldownFinished() {
        long gameTime = this.mob.level().getGameTime();
        return gameTime - this.lastCanUseTime >= (long) this.attackInterval;
    }

    @Override
    public void tick() {
        LivingEntity target = this.mob.getTarget();

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
        }

        if (this.attackAnimationTicks > 0) {
            this.attackAnimationTicks--;

            // ВЫЧИСЛЯЕМ ПРОШЕДШИЕ КАДРЫ
            int elapsedFrames = this.attackAnimationLength - this.attackAnimationTicks;

            // УДАР НА ДИНАМИЧЕСКОМ КАДРЕ!
            if (elapsedFrames == this.attackImpactFrame) {
                performPendingAction();
                this.pendingAction = null;
            }

            if (this.attackAnimationTicks == 0 && this.mob instanceof IAnimatedAttacker attacker) {
                attacker.setAttackingState(false);
            }
            return;
        }

        if (!isCooldownFinished()) {
            return;
        }

        planNextAttack(target);
    }

    private void planNextAttack(LivingEntity target) {
        Level level = this.mob.level();
        if (level.isClientSide()) return;

        Vec3 eyePos = this.mob.getEyePosition(1.0F);
        Vec3 aimVec;

        if (target != null && target.isAlive()) {
            Vec3 targetPos = target.position().add(0, target.getBbHeight() / 2.0, 0);
            aimVec = targetPos.subtract(eyePos).normalize();
        } else {
            Vec3 lookVec = this.mob.getViewVector(1.0F);
            RandomSource rand = this.mob.getRandom();
            aimVec = new Vec3(
                    lookVec.x + Mth.nextDouble(rand, -SPREAD_XZ, SPREAD_XZ),
                    lookVec.y + Mth.nextDouble(rand, SPREAD_Y_MIN, SPREAD_Y_MAX),
                    lookVec.z + Mth.nextDouble(rand, -SPREAD_XZ, SPREAD_XZ)
            ).normalize();
        }

        Vec3 rayEnd = eyePos.add(aimVec.scale(RAYCAST_DISTANCE));
        ClipContext clipContext = new ClipContext(eyePos, rayEnd, ClipContext.Block.OUTLINE, ClipContext.Fluid.NONE, this.mob);
        BlockHitResult hitResult = level.clip(clipContext);

        boolean hitBlock = false;
        BlockPos hitBlockPos = null;

        if (hitResult.getType() == HitResult.Type.BLOCK) {
            BlockState state = level.getBlockState(hitResult.getBlockPos());
            if (!state.isAir() && !state.liquid() && state.getDestroySpeed(level, hitResult.getBlockPos()) >= 0.0F) {
                double distToBlockSq = eyePos.distanceToSqr(hitResult.getLocation());
                if (target == null || distToBlockSq <= this.mob.distanceToSqr(target)) {
                    hitBlock = true;
                    hitBlockPos = hitResult.getBlockPos();
                }
            }
        }

        if (hitBlock) {
            this.pendingAction = PendingAction.BREAK_BLOCK;
            this.pendingBlockPos = hitBlockPos;
            startAnimation();
        } else if (target != null && target.isAlive()) {
            double distSqr = this.mob.distanceToSqr(target);
            if (canHitTarget(distSqr)) {
                this.pendingAction = PendingAction.ATTACK_TARGET;
                this.pendingTarget = target;
                startAnimation();
            }
        }
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
        if (this.mob instanceof IAnimatedAttacker attacker) {
            attacker.setAttackingState(true);
            this.attackAnimationTicks = this.attackAnimationLength;
            this.lastCanUseTime = this.mob.level().getGameTime();
        } else {
            performPendingAction();
            this.pendingAction = null;
            this.lastCanUseTime = this.mob.level().getGameTime();
        }
    }

    private void performPendingAction() {
        if (this.pendingAction == PendingAction.BREAK_BLOCK && this.pendingBlockPos != null) {
            attemptBreakBlock(this.pendingBlockPos);
        } else if (this.pendingAction == PendingAction.ATTACK_TARGET && this.pendingTarget != null) {
            if (this.pendingTarget.isAlive()) {
                this.mob.doHurtTarget(this.pendingTarget);
            }
        }
    }

    private void attemptBreakBlock(BlockPos pos) {
        Level level = this.mob.level();
        BlockState state = level.getBlockState(pos);
        if (state.isAir() || state.liquid()) return;

        float hardness = state.getDestroySpeed(level, pos);
        if (hardness < 0.0F) return;

        SoundType soundType = state.getSoundType();
        SoundEvent hitSound = soundType.getHitSound();

        level.playSound(null, pos, hitSound, SoundSource.BLOCKS,
                soundType.getVolume() * 0.5F, soundType.getPitch() * 0.875F);

        if (level instanceof ServerLevel serverLevel) {
            int particleCount = 25;
            float chancePercent = 100.0F / (1.0F + hardness * 2.5F);
            boolean blockBroken = this.mob.getRandom().nextFloat() * 100.0F < chancePercent;

            if (blockBroken) {
                particleCount = 25;
                level.destroyBlock(pos, dropBlockItems);
                level.levelEvent(2001, pos, Block.getId(state));
            }

            BlockParticleOption particleOption = new BlockParticleOption(ParticleTypes.BLOCK, state);
            double centerX = pos.getX() + 0.5D;
            double centerY = pos.getY() + 0.5D;
            double centerZ = pos.getZ() + 0.5D;

            serverLevel.sendParticles(particleOption, centerX, centerY, centerZ,
                    particleCount, 0.3D, 0.3D, 0.3D, 0.1D);
        } else {
            float chancePercent = 100.0F / (1.0F + hardness * 2.5F);
            if (this.mob.getRandom().nextFloat() * 100.0F < chancePercent) {
                level.destroyBlock(pos, dropBlockItems);
                level.levelEvent(2001, pos, Block.getId(state));
            }
        }
    }
}