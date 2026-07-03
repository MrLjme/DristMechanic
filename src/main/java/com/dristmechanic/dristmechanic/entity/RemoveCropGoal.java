package com.dristmechanic.dristmechanic.entity;

import net.minecraft.core.BlockPos;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.goal.RemoveBlockGoal;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

public class RemoveCropGoal extends RemoveBlockGoal {
    private final int radius;
    private boolean hasDestroyed = false;
    private int attackAnimationTicks = 0; // Таймер анимации
    private boolean isAnimating = false;  // Флаг: проигрываем ли анимацию

    public RemoveCropGoal(PathfinderMob pMob, double pSpeedModifier, int pSearchRange, int radius) {
        super(Blocks.WHEAT, pMob, pSpeedModifier, pSearchRange);
        this.radius = radius;
    }

    @Override
    protected boolean isValidTarget(LevelReader level, BlockPos pos) {
        BlockState state = level.getBlockState(pos);
        return state.is(BlockTags.CROPS);
    }

    @Override
    public boolean canContinueToUse() {
        if (hasDestroyed) return false;

        BlockPos target = this.blockPos;
        if (target == null) return false;

        Level level = (Level) this.mob.level();
        return level.getBlockState(target).is(BlockTags.CROPS);
    }

    @Override
    public void start() {
        super.start();
        hasDestroyed = false;
        attackAnimationTicks = 0;
        isAnimating = false;
    }

    @Override
    public void stop() {
        super.stop();
        hasDestroyed = false;
        attackAnimationTicks = 0;

        // Сбрасываем анимацию, если она была включена
        if (isAnimating && this.mob instanceof IAnimatedAttacker attacker) {
            attacker.setAttackingState(false);
            isAnimating = false;
        }
    }

    @Override
    public void tick() {
        super.tick();
        if (hasDestroyed) return;

        BlockPos target = this.blockPos;
        if (target == null) return;

        // Если моб подошел к цели
        if (this.mob.distanceToSqr(target.getX() + 0.5, target.getY() + 0.5, target.getZ() + 0.5) <= 4.0) {

            // Если ещё не начали анимацию - начинаем
            if (!isAnimating) {
                if (this.mob instanceof IAnimatedAttacker attacker) {
                    attacker.setAttackingState(true);
                    this.attackAnimationTicks = attacker.getAttackAnimationLength();
                    this.isAnimating = true;
                } else {
                    // Если моб не поддерживает анимацию - сразу разрушаем
                    destroyCrops();
                    return;
                }
            }

            // Отсчитываем таймер анимации
            if (this.attackAnimationTicks > 0) {
                this.attackAnimationTicks--;

                // Когда анимация закончилась - разрушаем блоки
                if (this.attackAnimationTicks == 0) {
                    destroyCrops();

                    // Выключаем анимацию
                    if (this.mob instanceof IAnimatedAttacker attacker) {
                        attacker.setAttackingState(false);
                        this.isAnimating = false;
                    }
                }
            }
        }
    }

    private void destroyCrops() {
        Level world = (Level) this.mob.level();
        BlockPos center = this.mob.blockPosition();

        for (int dx = -radius; dx <= radius; dx++) {
            for (int dy = -radius; dy <= radius; dy++) {
                for (int dz = -radius; dz <= radius; dz++) {
                    if (dx * dx + dy * dy + dz * dz <= radius * radius) {
                        BlockPos pos = center.offset(dx, dy, dz);
                        BlockState state = world.getBlockState(pos);
                        if (state.is(BlockTags.CROPS)) {
                            // false = БЕЗ ДРОПА (культура уничтожается, а не собирается)
                            world.destroyBlock(pos, false, this.mob);
                        }
                    }
                }
            }
        }
        hasDestroyed = true;
    }
}