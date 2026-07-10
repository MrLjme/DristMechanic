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

public class TotebotEntity extends Monster implements GeoEntity, AnimatedAttacker {
    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);

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

        this.goalSelector.addGoal(1, new SmartMeleeAttackGoal(this, 1.0D, true, getAttackAnimationLength(), 1.5, 2.2, 3.0, true
        ));

        this.goalSelector.addGoal(2, new RemoveCropGoal(this, 0.8, 16, 2));

        this.goalSelector.addGoal(5, new WaterAvoidingRandomStrollGoal(this, 0.7));
        this.goalSelector.addGoal(6, new RandomLookAroundGoal(this));

        this.targetSelector.addGoal(1, new NearestAttackableTargetGoal<>(this, Player.class, false));
        this.targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, net.minecraft.world.entity.animal.Cow.class, false));
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {

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
        return 14;
    }

    @Override
    public int getAttackImpactFrame() {
        return 13;
    }
}