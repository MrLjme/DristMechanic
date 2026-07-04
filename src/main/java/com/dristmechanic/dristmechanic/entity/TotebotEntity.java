package com.dristmechanic.dristmechanic.entity;

import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
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

public class TotebotEntity extends Monster implements GeoEntity, IAnimatedAttacker {
    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);

    // Сетевая синхронизация флага атаки между сервером и клиентом
    private static final EntityDataAccessor<Boolean> ATTACKING = SynchedEntityData.defineId(TotebotEntity.class, EntityDataSerializers.BOOLEAN);

    public TotebotEntity(EntityType<? extends Monster> entityType, Level level) {
        super(entityType, level);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(ATTACKING, false);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Mob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 18.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.4D)
                .add(Attributes.ATTACK_DAMAGE, 7.0D)
                .add(Attributes.STEP_HEIGHT, 1.1D)
                .add(Attributes.KNOCKBACK_RESISTANCE, 0.8D)
                .add(Attributes.JUMP_STRENGTH, 0.42D)
                .add(Attributes.FOLLOW_RANGE, 32.0D);
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(0, new FloatGoal(this));

        // Единый умный Goal: Атака + Разрушение блоков
        this.goalSelector.addGoal(1, new SmartMeleeAttackGoal(
                this, 1.0D, true,
                getAttackAnimationLength(), // Берем длину из интерфейса (14)
                1.5, 2.5, 3.0,
                true // false = уничтожать блоки без дропа
        ));

        // Уничтожение урожая
        this.goalSelector.addGoal(2, new RemoveCropGoal(this, 0.8, 16, 2));

        // Бродилка
        this.goalSelector.addGoal(5, new WaterAvoidingRandomStrollGoal(this, 0.7));
        this.goalSelector.addGoal(6, new RandomLookAroundGoal(this));

        // Цели (кого атаковать)
        this.targetSelector.addGoal(1, new NearestAttackableTargetGoal<>(this, Player.class, false));
        this.targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, net.minecraft.world.entity.animal.Cow.class, false));
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        // ВАЖНО: transitionLengthTicks = 2.
        // Анимируется быстро, чтобы клиент не отставал от серверного удара.
        controllers.add(new AnimationController<>(this, "main_controller", 2, event -> {

            if (this.isAttacking()) {
                return event.setAndContinue(RawAnimation.begin().thenPlay("totebotattack"));
            }

            boolean isMoving = event.isMoving() || this.getDeltaMovement().horizontalDistanceSqr() > 1.0E-4D;

            if (isMoving) {
                if (this.isAggressive()) {
                    return event.setAndContinue(RawAnimation.begin().thenLoop("totebotrun"));
                } else {
                    return event.setAndContinue(RawAnimation.begin().thenLoop("totebotwalk"));
                }
            }

            return event.setAndContinue(RawAnimation.begin().thenLoop("totebotidle"));
        }));
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return this.cache;
    }

    public boolean isAttacking() {
        return this.entityData.get(ATTACKING);
    }



    public void setAttacking(boolean attacking) {
        this.entityData.set(ATTACKING, attacking);
    }

    @Override
    public void tick() {
        super.tick();
        double speed = this.isAggressive() ? 0.31 : 0.29;
        this.getAttribute(Attributes.MOVEMENT_SPEED).setBaseValue(speed);
    }



    // ==========================================================
    // РЕАЛИЗАЦИЯ ИНТЕРФЕЙСА IAnimatedAttacker (КРИТИЧЕСКИ ВАЖНО!)
    // ==========================================================

    @Override
    public void setAttackingState(boolean attacking) {
        this.setAttacking(attacking);
    }

    @Override
    public boolean isAttackingState() {
        return this.isAttacking();
    }

    @Override
    public int getAttackAnimationLength() {
        // 0.6875 сек * 20 = 13.75 -> 14 тиков
        return 14;
    }

    @Override
    public int getAttackImpactFrame() {
        // В BlockBench пик удара на 0.4375 сек = 8.75 тиков.
        // Добавляем +2 тика на блендинг анимации и +1 тик на сетевую задержку.
        // Итого: сервер нанесет урон на 11-м тике, что на экране игрока
        // идеально совпадет с визуальным пиком замаха.
        return 12;
    }
}