package com.dristmechanic.dristmechanic.entity;

import com.dristmechanic.dristmechanic.Dristmechanic;
import net.minecraft.core.BlockPos;
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

import java.util.EnumSet;

public class HaybotEntity extends Monster implements AnimatedAttacker {

    private int stuckTicks = 0;
    private Vec3 lastPos = null;
    private int attackTicks = 0;
    private BlockPos breakingBlock = null;
    private boolean isAttackingState = false;
    private BlockPos raidTarget;

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

    @Override
    public void setAttackingState(boolean attacking) { this.isAttackingState = attacking; }
    @Override
    public boolean isAttackingState() { return this.isAttackingState; }
    @Override
    public int getAttackAnimationLength() { return 14; }
    @Override
    public int getAttackImpactFrame() { return 13; }

    public BlockPos getRaidTarget() { return raidTarget; }
    public void setRaidTarget(BlockPos raidTarget) { this.raidTarget = raidTarget; }

    public HaybotEntity(EntityType<? extends Monster> entityType, Level level) {
        super(entityType, level);
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
                .add(Attributes.FOLLOW_RANGE, 16.0D);
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(0, new FloatGoal(this));
        this.goalSelector.addGoal(3, new MoveToRaidCenterGoal(this, 1.0D));
        this.goalSelector.addGoal(1, new SmartMeleeAttackGoal(this, 1.0D, true, getAttackAnimationLength(), 0.0, 1.4, 2.7, 90, true));
        this.goalSelector.addGoal(2, new RemoveCropGoal(this, 1.0D, 16, 1));
        this.goalSelector.addGoal(5, new WaterAvoidingRandomStrollGoal(this, 0.7));
        this.goalSelector.addGoal(6, new RandomLookAroundGoal(this));
        this.targetSelector.addGoal(1, new NearestAttackableTargetGoal<>(this, Player.class, false));
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
        private final HaybotEntity mob;
        private final double speedModifier;

        public MoveToRaidCenterGoal(HaybotEntity mob, double speedModifier) {
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