package com.dristmechanic.dristmechanic.entity;

import com.dristmechanic.dristmechanic.Dristmechanic;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.RangedAttackGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.monster.RangedAttackMob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.NotNull;

import java.util.EnumSet;

public class RedTapebotEntity extends Monster implements RangedAttackMob {
    private BlockPos raidTarget;

    public RedTapebotEntity(EntityType<? extends Monster> entityType, Level level) {
        super(entityType, level);
    }

    @NotNull
    public static AttributeSupplier.Builder createAttributes() {
        return Mob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 20.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.35D)
                .add(Attributes.FOLLOW_RANGE, 32.0D);
    }

    public BlockPos getRaidTarget() {
        return raidTarget;
    }

    public void setRaidTarget(BlockPos raidTarget) {
        this.raidTarget = raidTarget;
    }

    @Override
    protected void registerGoals() {

        this.targetSelector.addGoal(0, new HurtByTargetGoal(this));
        this.goalSelector.addGoal(0, new FloatGoal(this));
        this.goalSelector.addGoal(1, new RangedAttackGoal(this, 1.0D, 20, 20.0F));
        this.targetSelector.addGoal(1, new NearestAttackableTargetGoal<>(this, Player.class, false));
        this.goalSelector.addGoal(2, new WaterAvoidingRandomStrollGoal(this, 0.7));
        this.goalSelector.addGoal(3, new MoveToRaidCenterGoal(this, 1.0D));
        this.goalSelector.addGoal(4, new RandomLookAroundGoal(this));
    }

    @Override
    public void performRangedAttack(LivingEntity target, float velocity) {
        RedTapeEntity.shoot(this, target);
    }

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
        private final RedTapebotEntity mob;
        private final double speedModifier;

        public MoveToRaidCenterGoal(RedTapebotEntity mob, double speedModifier) {
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
                this.mob.setAggressive(true);
            }
        }

        @Override
        public void stop() {
            this.mob.setAggressive(false);
            this.mob.getNavigation().stop();
        }

        @Override
        public void tick() {
            BlockPos target = this.mob.getRaidTarget();
            if (target != null) {
                if (this.mob.getNavigation().isDone()) {
                    this.mob.getNavigation().moveTo(target.getX() + 0.5, target.getY(), target.getZ() + 0.5, this.speedModifier);
                }
            }
        }
    }
}