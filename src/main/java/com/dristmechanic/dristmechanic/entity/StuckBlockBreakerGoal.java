package com.dristmechanic.dristmechanic.entity;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

public class StuckBlockBreakerGoal extends Goal {
    private final Mob mob;
    private final boolean dropItems;

    private static final double RAYCAST_DISTANCE = 5.0D;
    private static final double SPREAD_XZ = 0.7D;
    private static final double SPREAD_Y_MIN = -0.1D;
    private static final double SPREAD_Y_MAX = 0.7D;
    private static final double SPEED_THRESHOLD = 0.12D;

    private static final int BREAK_COOLDOWN = 10;

    // СИНХРОНИЗАЦИЯ КАДРА УДАРА (Hit Frame)
    // Длина анимации = 14 тиков. Пик удара в BlockBench = 9 тик.
    // 14 - 9 = 5. Значит, удар нужно наносить, когда до конца анимации осталось 5 тиков.
    private static final int HIT_FRAME_TICKS_REMAINING = 5;

    private int animationTicks = 0;
    private int breakCooldown = 0;
    private BlockPos targetBlock = null;

    public StuckBlockBreakerGoal(Mob mob, boolean dropItems) {
        this.mob = mob;
        this.dropItems = dropItems;
    }

    @Override
    public boolean canUse() {
        if (!mob.isAggressive() && mob.getTarget() == null) return false;
        if (animationTicks > 0 || breakCooldown > 0) return true;

        double horizontalSpeed = mob.getDeltaMovement().multiply(1.0, 0.0, 1.0).length();
        return horizontalSpeed < SPEED_THRESHOLD;
    }

    @Override
    public boolean canContinueToUse() {
        return canUse();
    }

    @Override
    public void stop() {
        if ((animationTicks > 0 || breakCooldown > 0) && mob instanceof IAnimatedAttacker attacker) {
            attacker.setAttackingState(false);
            animationTicks = 0;
            breakCooldown = 0;
            targetBlock = null;
        }
    }

    @Override
    public void tick() {
        Level level = mob.level();
        if (level.isClientSide()) return;

        // 1. ФАЗА КУЛДАУНА
        if (breakCooldown > 0) {
            breakCooldown--;
            return;
        }

        // 2. ФАЗА АНИМАЦИИ
        if (animationTicks > 0) {
            animationTicks--;

            // СИНХРОНИЗИРОВАННЫЙ УДАР:
            // Когда до конца анимации остается ровно 5 тиков (прошло 9 тиков),
            // мы проигрываем звук, спавним частицы и пытаемся сломать блок.
            if (animationTicks == HIT_FRAME_TICKS_REMAINING && targetBlock != null) {
                attemptBreakBlock(level, targetBlock);
                targetBlock = null; // Сбрасываем, чтобы не ударить дважды за один замах
            }

            // Конец анимации (14 тиков прошли)
            if (animationTicks == 0) {
                if (mob instanceof IAnimatedAttacker attacker) {
                    attacker.setAttackingState(false);
                }
                breakCooldown = BREAK_COOLDOWN;
            }
            return;
        }

        // 3. Защита от конфликта с атакой игрока
        if (mob instanceof IAnimatedAttacker attacker && attacker.isAttackingState()) {
            return;
        }

        // 4. ФАЗА ПРИЦЕЛИВАНИЯ
        Vec3 eyePos = mob.getEyePosition(1.0F);
        Vec3 lookVec = mob.getViewVector(1.0F);

        double targetX = eyePos.x + lookVec.x * RAYCAST_DISTANCE + Mth.nextDouble(mob.getRandom(), -SPREAD_XZ, SPREAD_XZ);
        double targetY = eyePos.y + lookVec.y * RAYCAST_DISTANCE + Mth.nextDouble(mob.getRandom(), SPREAD_Y_MIN, SPREAD_Y_MAX);
        double targetZ = eyePos.z + lookVec.z * RAYCAST_DISTANCE + Mth.nextDouble(mob.getRandom(), -SPREAD_XZ, SPREAD_XZ);

        Vec3 targetPos = new Vec3(targetX, targetY, targetZ);

        ClipContext clipContext = new ClipContext(eyePos, targetPos, ClipContext.Block.OUTLINE, ClipContext.Fluid.NONE, mob);
        BlockHitResult hitResult = level.clip(clipContext);

        if (hitResult.getType() == HitResult.Type.BLOCK) {
            BlockPos blockPos = hitResult.getBlockPos();
            BlockState state = level.getBlockState(blockPos);

            if (!state.isAir() && !state.liquid() && state.getDestroySpeed(level, blockPos) >= 0.0F) {
                if (mob instanceof IAnimatedAttacker attacker) {
                    attacker.setAttackingState(true);
                    this.animationTicks = attacker.getAttackAnimationLength();
                    this.targetBlock = blockPos;
                } else {
                    attemptBreakBlock(level, blockPos);
                    breakCooldown = BREAK_COOLDOWN;
                }
            }
        }
    }

    private void attemptBreakBlock(Level level, BlockPos pos) {
        BlockState state = level.getBlockState(pos);
        if (state.isAir() || state.liquid()) return;

        float hardness = state.getDestroySpeed(level, pos);
        if (hardness < 0.0F) return;

        // Звук удара по материалу блока
        SoundType soundType = state.getSoundType();
        SoundEvent hitSound = soundType.getHitSound();

        level.playSound(
                null,
                pos,
                hitSound,
                SoundSource.BLOCKS,
                soundType.getVolume() * 0.5F,
                soundType.getPitch() * 0.875F
        );

        // Частицы и разрушение
        if (level instanceof ServerLevel serverLevel) {
            int particleCount = 5;

            float chancePercent = 100.0F / (1.0F + hardness * 2.5F);
            boolean blockBroken = mob.getRandom().nextFloat() * 100.0F < chancePercent;

            if (blockBroken) {
                particleCount = 15;
                level.destroyBlock(pos, dropItems);
                level.levelEvent(2001, pos, Block.getId(state));
            }

            BlockParticleOption particleOption = new BlockParticleOption(ParticleTypes.BLOCK, state);
            double centerX = pos.getX() + 0.5D;
            double centerY = pos.getY() + 0.5D;
            double centerZ = pos.getZ() + 0.5D;

            serverLevel.sendParticles(
                    particleOption,
                    centerX, centerY, centerZ,
                    particleCount,
                    0.3D, 0.3D, 0.3D,
                    0.1D
            );
        } else {
            float chancePercent = 100.0F / (1.0F + hardness * 2.5F);
            if (mob.getRandom().nextFloat() * 100.0F < chancePercent) {
                level.destroyBlock(pos, dropItems);
                level.levelEvent(2001, pos, Block.getId(state));
            }
        }
    }
}