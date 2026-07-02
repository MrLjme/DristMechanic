package com.dristmechanic.dristmechanic.entity;

import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
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
    private int attackTimer = 0; // таймер атаки (если >0, анимация атаки проигрывается)
    private static final int ATTACK_ANIMATION_DURATION = 14; // длительность анимации атаки в тиках

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
        // Используем кастомную атаку с задержкой, которая будет устанавливать swinging и таймер
        this.goalSelector.addGoal(1, new DelayedMeleeAttackGoal(this, 1.0, true, 8));

        this.targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, false));
        this.targetSelector.addGoal(3, new NearestAttackableTargetGoal<>(this, net.minecraft.world.entity.animal.Cow.class, false));
        this.targetSelector.addGoal(4, new NearestAttackableTargetGoal<>(this, net.minecraft.world.entity.monster.Zombie.class, false));

        this.goalSelector.addGoal(5, new WaterAvoidingRandomStrollGoal(this, 0.7));
        this.goalSelector.addGoal(6, new RandomLookAroundGoal(this));
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        // ОДИН контроллер для всех анимаций
        controllers.add(new AnimationController<>(this, "main_controller", 1, this::mainController));
    }

    private <E extends TotebotEntity> PlayState mainController(final AnimationState<E> event) {
        // Приоритет: атака
        if (this.attackTimer > 0) {
            // Если анимация атаки не запущена или остановилась, запускаем её
            if (event.getController().getAnimationState() == AnimationController.State.STOPPED) {
                event.getController().setAnimation(RawAnimation.begin().thenPlay("totebotattack"));
            }
            return PlayState.CONTINUE;
        }

        // Если swing true, значит началась новая атака – запускаем таймер и анимацию
        if (this.swinging) {
            this.attackTimer = ATTACK_ANIMATION_DURATION;
            event.getController().setAnimation(RawAnimation.begin().thenPlay("totebotattack"));
            this.swinging = false; // сбрасываем, чтобы не повторять
            return PlayState.CONTINUE;
        }

        // Движение (бег/ходьба)
        boolean isMoving = event.isMoving();
        boolean isAggressive = this.isAggressive();

        if (isMoving) {
            if (isAggressive) {
                event.getController().setAnimation(RawAnimation.begin().thenPlay("totebotrun"));
            } else {
                event.getController().setAnimation(RawAnimation.begin().thenPlay("totebotwalk"));
            }
            return PlayState.CONTINUE;
        }

        // Idle
        event.getController().setAnimation(RawAnimation.begin().thenPlay("totebotidle"));
        return PlayState.CONTINUE;
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return this.cache;
    }

    @Override
    public int getCurrentSwingDuration() {
        return ATTACK_ANIMATION_DURATION;
    }

    @Override
    public void tick() {
        super.tick();

        // Уменьшаем таймер атаки
        if (this.attackTimer > 0) {
            this.attackTimer--;
        }

        // Скорость
        if (this.isAggressive()) {
            this.getAttribute(Attributes.MOVEMENT_SPEED).setBaseValue(0.4);
        } else {
            this.getAttribute(Attributes.MOVEMENT_SPEED).setBaseValue(0.4);
        }
    }
}
