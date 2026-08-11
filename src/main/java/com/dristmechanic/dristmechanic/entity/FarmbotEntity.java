package com.dristmechanic.dristmechanic.entity;

import com.dristmechanic.dristmechanic.Dristmechanic;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
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
import org.jetbrains.annotations.NotNull;

public class FarmbotEntity extends Monster {
    public FarmbotEntity(EntityType<? extends Monster> entityType, Level level) {
        super(entityType, level);
    }

    @NotNull
    public static AttributeSupplier.Builder createAttributes() {
        return Mob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 18.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.4D)
                .add(Attributes.ATTACK_DAMAGE, 7.0D)
                .add(Attributes.FOLLOW_RANGE, 16.0D);
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(0, new FloatGoal(this));
        this.goalSelector.addGoal(1, new SmartMeleeAttackGoal(this, 1.0D, true, 14, 0.0, 1.4, 2.7, 90, true));
        this.goalSelector.addGoal(2, new RemoveCropGoal(this, 1.0D, 16, 1));
        this.goalSelector.addGoal(5, new WaterAvoidingRandomStrollGoal(this, 0.7));
        this.goalSelector.addGoal(6, new RandomLookAroundGoal(this));
        this.targetSelector.addGoal(1, new NearestAttackableTargetGoal<>(this, Player.class, false));
    }

    @Override
    public boolean hurt(DamageSource damageSource, float damage) {
        if (damageSource.isDirect() && damageSource.getEntity() instanceof LivingEntity) {
            return false;
        }
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
}