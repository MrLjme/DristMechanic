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

    private final int attackImpactFrame;

    private final boolean dropBlockItems;
    private static final double RAYCAST_DISTANCE = 5.0D;
    private static final double CONE_THRESHOLD = 0.6D; // ~53 градуса обзора

    private int stuckTicks = 0;
    private Vec3 lastPos = null;
    private static final int STUCK_THRESHOLD = 20; // 1 секунда без движения

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

        if (mob instanceof IAnimatedAttacker attacker) {
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
        if (attackAnimationTicks > 0 || !isCooldownFinished()) {
            return true;
        }
        return false;
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
        this.stuckTicks = 0;
        this.lastPos = null;
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
        this.stuckTicks = 0;
        this.lastPos = null;
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
        if (this.attackAnimationTicks > 0) {
            if (this.pendingAction == PendingAction.BREAK_BLOCK && this.pendingBlockPos != null) {
                this.mob.getLookControl().setLookAt(this.pendingBlockPos.getX() + 0.5, this.pendingBlockPos.getY() + 0.5, this.pendingBlockPos.getZ() + 0.5, 30.0F, 30.0F);
            } else if (this.pendingAction == PendingAction.ATTACK_TARGET && this.pendingTarget != null && this.pendingTarget.isAlive()) {
                this.mob.getLookControl().setLookAt(this.pendingTarget, 30.0F, 30.0F);
            } else {
                LivingEntity t = this.mob.getTarget();
                if (t != null && t.isAlive()) {
                    this.mob.getLookControl().setLookAt(t, 30.0F, 30.0F);
                }
            }

            this.attackAnimationTicks--;
            int elapsedFrames = this.attackAnimationLength - this.attackAnimationTicks;

            if (elapsedFrames == this.attackImpactFrame) {
                performPendingAction();
                this.pendingAction = null;
            }

            if (this.attackAnimationTicks == 0 && this.mob instanceof IAnimatedAttacker attacker) {
                attacker.setAttackingState(false);
            }
            return;
        }

        LivingEntity target = this.mob.getTarget();

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
        }

        if (!isCooldownFinished()) {
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

        double distSqr = this.mob.distanceToSqr(target);

        if (canHitTarget(distSqr)) {
            this.pendingAction = PendingAction.ATTACK_TARGET;
            this.pendingTarget = target;
            this.mob.getLookControl().setLookAt(target, 30.0F, 30.0F);
            startAnimation();
            return;
        }

        boolean isStuck = (this.stuckTicks >= STUCK_THRESHOLD);

        if (isStuck) {
            Vec3 eyePos = this.mob.getEyePosition(1.0F);
            Vec3 targetCenter = target.position().add(0, target.getBbHeight() / 2.0, 0);

            double dy = targetCenter.y - eyePos.y;
            double horizontalDistSq = Math.pow(targetCenter.x - eyePos.x, 2) + Math.pow(targetCenter.z - eyePos.z, 2);

            Vec3 aimVec;

            // --- УМНАЯ МЕХАНИКА ПРЕОДОЛЕНИЯ ПРЕПЯТСТВИЙ ---
            if (dy > 1.5D && horizontalDistSq > 1.0D) {
                // Цель ВЫСОКО. Включаем режим "Зигзагообразной Лестницы" (Zigzag Stairs)
                // Чтобы моб не вырыл узкий туннель 1x1 (в котором он застрянет),
                // мы заставляем его смещать точку копания влево и вправо каждые 1.5 секунды.
                // Это создаст широкую винтовую шахту с нормальными ступенями.
                Vec3 horizontalVec = new Vec3(targetCenter.x - eyePos.x, 0.0D, targetCenter.z - eyePos.z).normalize();

                int zigzagPhase = (int)(this.mob.level().getGameTime() / 30L) % 2;
                double sideOffset = (zigzagPhase == 0) ? 1.2D : -1.2D;

                // Вектор перпендикулярный направлению движения (направление "влево-вправо")
                Vec3 sideVec = new Vec3(-horizontalVec.z, 0.0D, horizontalVec.x);

                aimVec = new Vec3(
                        horizontalVec.x + sideVec.x * sideOffset,
                        1.0D, // Вверх
                        horizontalVec.z + sideVec.z * sideOffset
                ).normalize();
            } else if (dy < -1.5D && horizontalDistSq > 1.0D) {
                // Цель ГЛУБОКО ВНИЗУ. Делаем безопасный пандус.
                // Моб не будет ломать блок прямо под собой, а сделает пологий спуск (2 вперед, 1 вниз).
                Vec3 horizontalVec = new Vec3(targetCenter.x - eyePos.x, 0.0D, targetCenter.z - eyePos.z).normalize();
                aimVec = new Vec3(horizontalVec.x * 2.0D, -1.0D, horizontalVec.z * 2.0D).normalize();
            } else {
                // Цель примерно на одном уровне. Копаем прямо.
                aimVec = targetCenter.subtract(eyePos).normalize();
            }
            // ------------------------------------------------

            int range = (int) Math.ceil(RAYCAST_DISTANCE);
            BlockPos mobPos = this.mob.blockPosition();
            BlockPos.MutableBlockPos mutablePos = new BlockPos.MutableBlockPos();

            double nearestDistSq = Double.MAX_VALUE;
            BlockPos nearestBlockPos = null;
            double maxDistSq = RAYCAST_DISTANCE * RAYCAST_DISTANCE;

            for (int x = -range; x <= range; x++) {
                for (int y = -range; y <= range; y++) {
                    for (int z = -range; z <= range; z++) {
                        mutablePos.set(mobPos.getX() + x, mobPos.getY() + y, mobPos.getZ() + z);
                        Vec3 blockCenter = Vec3.atCenterOf(mutablePos);

                        double dx = blockCenter.x - eyePos.x;
                        double dyB = blockCenter.y - eyePos.y;
                        double dz = blockCenter.z - eyePos.z;
                        double blockDistSq = dx * dx + dyB * dyB + dz * dz;

                        if (blockDistSq > maxDistSq || blockDistSq < 0.25D) continue;

                        double dot = dx * aimVec.x + dyB * aimVec.y + dz * aimVec.z;
                        double cosAngle = dot / Math.sqrt(blockDistSq);
                        if (cosAngle < CONE_THRESHOLD) continue;

                        ClipContext clipContext = new ClipContext(eyePos, blockCenter, ClipContext.Block.OUTLINE, ClipContext.Fluid.NONE, this.mob);
                        BlockHitResult hitResult = level.clip(clipContext);

                        if (hitResult.getType() == HitResult.Type.BLOCK && hitResult.getBlockPos().equals(mutablePos)) {
                            BlockState state = level.getBlockState(mutablePos);
                            if (!state.isAir() && !state.liquid() && state.getDestroySpeed(level, mutablePos) >= 0.0F) {
                                if (blockDistSq < nearestDistSq) {
                                    nearestDistSq = blockDistSq;
                                    nearestBlockPos = mutablePos.immutable();
                                }
                            }
                        }
                    }
                }
            }

            if (nearestBlockPos != null) {
                this.pendingAction = PendingAction.BREAK_BLOCK;
                this.pendingBlockPos = nearestBlockPos;

                this.mob.getLookControl().setLookAt(nearestBlockPos.getX() + 0.5, nearestBlockPos.getY() + 0.5, nearestBlockPos.getZ() + 0.5, 30.0F, 30.0F);

                this.stuckTicks = 0;
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