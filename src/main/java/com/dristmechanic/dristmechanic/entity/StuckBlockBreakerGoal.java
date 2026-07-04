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

    private static final int BREAK_COOLDOWN = 0;

    // Точные значения из totebot.animation.json
    private static final int ATTACK_ANIMATION_LENGTH = 13; // 0.6875 сек * 20
    private static final int IMPACT_FRAME = 9;             // 0.4375 сек * 20 = 8.75 → 9

    private int animationFrame = -1; // -1 = не анимирует, 0..13 = кадр анимации
    private int breakCooldown = 0;
    private BlockPos targetBlock = null;

    public StuckBlockBreakerGoal(Mob mob, boolean dropItems) {
        this.mob = mob;
        this.dropItems = dropItems;
    }

    @Override
    public boolean canUse() {
        if (!mob.isAggressive() && mob.getTarget() == null) return false;
        if (animationFrame >= 0 || breakCooldown > 0) return true;

        double horizontalSpeed = mob.getDeltaMovement().multiply(1.0, 0.0, 1.0).length();
        return horizontalSpeed < SPEED_THRESHOLD;
    }

    @Override
    public boolean canContinueToUse() {
        return canUse();
    }

    @Override
    public void stop() {
        if ((animationFrame >= 0 || breakCooldown > 0) && mob instanceof IAnimatedAttacker attacker) {
            attacker.setAttackingState(false);
            animationFrame = -1;
            breakCooldown = 0;
            targetBlock = null;
        }
    }

    @Override
    public void tick() {
        Level level = mob.level();
        if (level.isClientSide()) return;

        // Фаза кулдауна
        if (breakCooldown > 0) {
            breakCooldown--;
            return;
        }

        // Фаза анимации: считаем кадры ВПЕРЁД от 0
        if (animationFrame >= 0) {
            animationFrame++;

            // УДАР СИНХРОНИЗИРОВАН: ровно на 9-м кадре (пик замаха в BlockBench)
            if (animationFrame == IMPACT_FRAME && targetBlock != null) {
                attemptBreakBlock(level, targetBlock);
                targetBlock = null;
            }

            // Конец анимации
            if (animationFrame >= ATTACK_ANIMATION_LENGTH) {
                if (mob instanceof IAnimatedAttacker attacker) {
                    attacker.setAttackingState(false);
                }
                animationFrame = -1;
                breakCooldown = BREAK_COOLDOWN;
            }
            return;
        }

        // Защита: не атакуем блоки, если моб уже бьёт игрока
        if (mob instanceof IAnimatedAttacker attacker && attacker.isAttackingState()) {
            return;
        }

        // Рейкаст для поиска блока
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
                    this.animationFrame = 0; // Начинаем отсчёт с 0
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

        SoundType soundType = state.getSoundType();
        SoundEvent hitSound = soundType.getHitSound();

        level.playSound(
                null, pos, hitSound, SoundSource.BLOCKS,
                soundType.getVolume() * 0.5F,
                soundType.getPitch() * 0.875F
        );

        if (level instanceof ServerLevel serverLevel) {
            int particleCount = 15;
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
                    particleOption, centerX, centerY, centerZ,
                    particleCount, 0.3D, 0.3D, 0.3D, 0.1D
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