package com.dristmechanic.dristmechanic.entity;

import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animation.*;
import software.bernie.geckolib.util.GeckoLibUtil;

public class TotebotEntity extends Monster implements GeoEntity {
    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);
    private boolean isAttacking = false; // флаг, что сейчас проигрывается атака

    public TotebotEntity(EntityType<? extends Monster> entityType, Level level) {
        super(entityType, level);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Mob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 18.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.4D)
                .add(Attributes.ATTACK_DAMAGE, 5.0D)
                .add(Attributes.STEP_HEIGHT, 1.1D)
                .add(Attributes.KNOCKBACK_RESISTANCE, 0.8D)
                .add(Attributes.JUMP_STRENGTH, 0.42D)
                .add(Attributes.FOLLOW_RANGE, 48.0D);
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(0, new FloatGoal(this));
        // Используем наш кастомный Goal с задержкой
        this.goalSelector.addGoal(1, new DelayedMeleeAttackGoal(this, 1.0, true, 8));

        this.targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, false));
        this.targetSelector.addGoal(3, new NearestAttackableTargetGoal<>(this, net.minecraft.world.entity.animal.Cow.class, false));
        this.targetSelector.addGoal(4, new NearestAttackableTargetGoal<>(this, net.minecraft.world.entity.monster.Zombie.class, false));

        this.goalSelector.addGoal(5, new WaterAvoidingRandomStrollGoal(this, 0.7));
        this.goalSelector.addGoal(6, new RandomLookAroundGoal(this));
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        AnimationController<TotebotEntity> controller = new AnimationController<>(this, "main_controller", 1, event -> {
            // Приоритет: анимация атаки
            if (isAttacking) {
                event.getController().setAnimation(RawAnimation.begin().thenPlay("totebotattack"));
                return PlayState.CONTINUE;
            }

            // Движение
            if (event.isMoving()) {
                if (this.isAggressive()) {
                    event.getController().setAnimation(RawAnimation.begin().thenPlay("totebotrun"));
                } else {
                    event.getController().setAnimation(RawAnimation.begin().thenPlay("totebotwalk"));
                }
                return PlayState.CONTINUE;
            }

            // Idle
            event.getController().setAnimation(RawAnimation.begin().thenPlay("totebotidle"));
            return PlayState.CONTINUE;
        });

        // Сбрасываем флаг, когда анимация атаки завершилась
        controller.setAnimationFinishCallback(event -> {
            if (isAttacking) {
                isAttacking = false;
            }
        });

        controllers.add(controller);
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return this.cache;
    }

    // Метод для включения атаки (вызывается из DelayedMeleeAttackGoal)
    public void startAttackAnimation() {
        this.isAttacking = true;
    }

    @Override
    public void tick() {
        super.tick();
        // Можно менять скорость в зависимости от агрессии (если нужно)
        double speed = this.isAggressive() ? 0.45 : 0.35;
        this.getAttribute(Attributes.MOVEMENT_SPEED).setBaseValue(speed);
    }
}