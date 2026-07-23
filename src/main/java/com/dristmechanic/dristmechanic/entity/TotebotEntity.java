package com.dristmechanic.dristmechanic.entity;

import com.dristmechanic.dristmechanic.Dristmechanic;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.ai.navigation.PathNavigation;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animation.*;
import software.bernie.geckolib.util.GeckoLibUtil;

import java.util.EnumSet;

public class TotebotEntity extends Monster implements GeoEntity, AnimatedAttacker {

    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);
    private static final EntityDataAccessor<Boolean> ATTACKING = SynchedEntityData.defineId(TotebotEntity.class, EntityDataSerializers.BOOLEAN);

    private BlockPos raidTarget;

    // Interface state implementation
    private int stuckTicks = 0;
    private Vec3 lastPos = null;
    private int attackTicks = 0;
    private BlockPos breakingBlock = null;

    @Override
    public int getStuckTicks() { return stuckTicks; }
    @Override
    public void setStuckTicks(int ticks) { this.stuckTicks = ticks; }
    @Override
    public Vec3 getLastPos() { return lastPos; }
    @Override
    public void setLastPos(Vec3 pos) { this.lastPos = pos; }
    @Override
    public int getAttackTicks() { return attackTicks; }
    @Override
    public void setAttackTicks(int ticks) { this.attackTicks = ticks; }
    @Override
    public BlockPos getBreakingBlock() { return breakingBlock; }
    @Override
    public void setBreakingBlock(BlockPos pos) { this.breakingBlock = pos; }

    public BlockPos getRaidTarget() { return raidTarget; }
    public void setRaidTarget(BlockPos raidTarget) { this.raidTarget = raidTarget; }

    public TotebotEntity(EntityType<? extends Monster> entityType, Level level) {
        super(entityType, level);
    }

    @Override
    protected void defineSynchedData(@NotNull SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(ATTACKING, false);
    }

    @Override
    @NotNull
    protected PathNavigation createNavigation(@NotNull Level level) {
        return new SmoothPathNavigation(this, level);
    }

    @NotNull
    public static AttributeSupplier.Builder createAttributes() {
        return Mob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 18.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.4D)
                .add(Attributes.ATTACK_DAMAGE, 7.0D)
                .add(Attributes.STEP_HEIGHT, 1.1D)
                .add(Attributes.KNOCKBACK_RESISTANCE, 0.7D)
                .add(Attributes.JUMP_STRENGTH, 0.0D)
                .add(Attributes.FOLLOW_RANGE, 16.0D);
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(0, new FloatGoal(this));
        // Унифицированная скорость 1.0D для всех
        this.goalSelector.addGoal(1, new MoveToRaidCenterGoal(this, 1.0D));
        this.goalSelector.addGoal(1, new SmartMeleeAttackGoal(this, 1.0D, true, getAttackAnimationLength(), 0.0, 1.4, 2.7, 90, true));
        this.goalSelector.addGoal(2, new RemoveCropGoal(this, 1.0D, 16, 1));
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
    public AnimatableInstanceCache getAnimatableInstanceCache() { return this.cache; }

    public boolean isAttacking() { return this.entityData.get(ATTACKING); }
    public void setAttacking(boolean attacking) { this.entityData.set(ATTACKING, attacking); }

    @Override
    public void tick() {
        super.tick();
        double speed = this.isAggressive() ? 0.31 : 0.29;
        var speedAttribute = this.getAttribute(Attributes.MOVEMENT_SPEED);
        if (speedAttribute != null) {
            speedAttribute.setBaseValue(speed);
        }
        float smoothFactor = 0.4F;
        this.yBodyRot = net.minecraft.util.Mth.rotLerp(smoothFactor, this.yBodyRotO, this.yBodyRot);
    }

    @Override public void setAttackingState(boolean attacking) { this.setAttacking(attacking); }
    @Override public boolean isAttackingState() { return this.isAttacking(); }
    @Override public int getAttackAnimationLength() { return 14; }
    @Override public int getAttackImpactFrame() { return 13; }

    @Override
    public boolean hurt(DamageSource damageSource, float damage) {
        boolean flag = super.hurt(damageSource, damage);
        if (!this.level().isClientSide) {
            ServerLevel serverLevel = (ServerLevel) this.level();
            serverLevel.sendParticles(Dristmechanic.SCRAP.get(), this.getX(), this.getY(0.3D), this.getZ(), 5, 0.1D, 0.5D, 0.1D, 0.15D);
        }
        return flag;
    }

    @Override
    public void die(DamageSource damageSource) {
        super.die(damageSource);
        if (!this.level().isClientSide) {
            ServerLevel serverLevel = (ServerLevel) this.level();
            serverLevel.sendParticles(Dristmechanic.FLASH.get(), this.getX(), this.getY(0.5D), this.getZ(), 25, 0.0D, 0.0D, 0.0D, 0.075D);
            serverLevel.sendParticles(Dristmechanic.SCRAP.get(), this.getX(), this.getY(0.3D), this.getZ(), 5, 0.0D, 0.0D, 0.0D, 0.15D);
        }
    }

    @Override
    protected void tickDeath() {
        ++this.deathTime;
        if (this.deathTime >= 3) this.discard();
    }

    public static class MoveToRaidCenterGoal extends Goal {
        private final TotebotEntity mob;
        private final double speedModifier;

        public MoveToRaidCenterGoal(TotebotEntity mob, double speedModifier) {
            this.mob = mob;
            this.speedModifier = speedModifier;
            this.setFlags(EnumSet.of(Goal.Flag.MOVE));
        }

        @Override
        public boolean canUse() {
            BlockPos target = this.mob.getRaidTarget();
            if (target == null) return false;
            return this.mob.distanceToSqr(target.getX() + 0.5, target.getY(), target.getZ() + 0.5) > 4.0;
        }

        @Override
        public boolean canContinueToUse() {
            BlockPos target = this.mob.getRaidTarget();
            if (target == null) return false;
            if (this.mob.distanceToSqr(target.getX() + 0.5, target.getY(), target.getZ() + 0.5) <= 4.0) {
                this.mob.setRaidTarget(null);
                return false;
            }
            return true;
        }

        @Override
        public void start() {
            BlockPos target = this.mob.getRaidTarget();
            if (target != null) {
                this.mob.getNavigation().moveTo(target.getX() + 0.5, target.getY(), target.getZ() + 0.5, this.speedModifier);
                this.mob.setAggressive(true); // Триггерим анимацию бега
                this.mob.resetStuckDetection();
            }
        }

        @Override
        public void stop() {
            this.mob.setAggressive(false);
            this.mob.resetStuckDetection();
            if (this.mob.getBreakingBlock() != null) {
                this.mob.setBreakingBlock(null);
                this.mob.setAttackingState(false);
            }
            this.mob.getNavigation().stop();
        }

        @Override
        public void tick() {
            BlockPos target = this.mob.getRaidTarget();
            if (target != null) {
                // Глобальная проверка препятствий и анимация пробивания
                if (this.mob.getBreakingBlock() != null) {
                    this.mob.tickBreakingBlock(this.mob, true);
                } else {
                    this.mob.tickStuckDetection(this.mob, Vec3.atBottomCenterOf(target), true);
                }

                if (this.mob.getNavigation().isDone()) {
                    this.mob.getNavigation().moveTo(target.getX() + 0.5, target.getY(), target.getZ() + 0.5, this.speedModifier);
                }
            }
        }
    }
}