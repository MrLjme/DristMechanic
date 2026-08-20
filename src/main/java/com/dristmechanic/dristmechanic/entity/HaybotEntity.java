package com.dristmechanic.dristmechanic.entity;

import com.dristmechanic.dristmechanic.Dristmechanic;
import com.dristmechanic.dristmechanic.init.ModSounds;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.RandomSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.ai.navigation.PathNavigation;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

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
                .add(Attributes.MAX_HEALTH, 25.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.4D)
                .add(Attributes.ATTACK_DAMAGE, 6.0D)
                .add(Attributes.STEP_HEIGHT, 1.1D)
                .add(Attributes.KNOCKBACK_RESISTANCE, 0.7D)
                .add(Attributes.FOLLOW_RANGE, 16.0D);
    }

    @Override
    public void tick() {
        super.tick();
        double speed = this.isAggressive() ? 0.35 : 0.3;
        var speedAttribute = this.getAttribute(Attributes.MOVEMENT_SPEED);
        if (speedAttribute != null) {
            speedAttribute.setBaseValue(speed);
        }
        float smoothFactor = 0.4F;
        this.yBodyRot = net.minecraft.util.Mth.rotLerp(smoothFactor, this.yBodyRotO, this.yBodyRot);
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(0, new FloatGoal(this));
        this.goalSelector.addGoal(2, new MoveToRaidCenterGoal(this, 1.0D));
        this.goalSelector.addGoal(1, new SmartMeleeAttackGoal(this, 1.0D, true, getAttackAnimationLength(), 0.0, 1.4, 2.7, 90, true));
        this.goalSelector.addGoal(3, new RemoveCropGoal(this, 1.0D, 1));
        this.goalSelector.addGoal(5, new WaterAvoidingRandomStrollGoal(this, 0.7));
        this.goalSelector.addGoal(6, new RandomLookAroundGoal(this));
        this.targetSelector.addGoal(0, new HurtByTargetGoal(this));
        this.targetSelector.addGoal(1, new NearestAttackableTargetGoal<>(this, Player.class, false) {
            @Override
            public boolean canContinueToUse() {
                LivingEntity target = this.mob.getTarget();
                if (target != null && HaybotEntity.this.getRaidTarget() != null && HaybotEntity.this.distanceToSqr(target) > 16.0) return false;
                return super.canContinueToUse();
            }
        });
        this.targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, net.minecraft.world.entity.animal.Cow.class, false) {
            @Override
            public boolean canContinueToUse() {
                LivingEntity target = this.mob.getTarget();
                if (target != null && HaybotEntity.this.getRaidTarget() != null && HaybotEntity.this.distanceToSqr(target) > 16.0) return false;
                return super.canContinueToUse();
            }
        });
        this.targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, net.minecraft.world.entity.npc.Villager.class, false) {
            @Override
            public boolean canContinueToUse() {
                LivingEntity target = this.mob.getTarget();
                if (target != null && HaybotEntity.this.getRaidTarget() != null && HaybotEntity.this.distanceToSqr(target) > 16.0) return false;
                return super.canContinueToUse();
            }
        });
        this.targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, net.minecraft.world.entity.animal.IronGolem.class, false) {
            @Override
            public boolean canContinueToUse() {
                LivingEntity target = this.mob.getTarget();
                if (target != null && HaybotEntity.this.getRaidTarget() != null && HaybotEntity.this.distanceToSqr(target) > 16.0) return false;
                return super.canContinueToUse();
            }
        });
    }

    @Override
    public boolean checkSpawnRules(LevelAccessor level, MobSpawnType spawnType) {
        BlockPos pos = this.blockPosition();

        // Проверяем только базовые требования
        BlockPos below = pos.below();
        if (!level.getBlockState(below).isFaceSturdy(level, below, Direction.UP)) {
            return false;
        }

        if (!level.getBlockState(pos).isAir() || !level.getBlockState(pos.above()).isAir()) {
            return false;
        }

        // Для естественного спавна проверяем свет >= 9
        if (spawnType == MobSpawnType.NATURAL) {
            int lightLevel = level.getMaxLocalRawBrightness(pos);
            if (lightLevel < 9) {
                return false;
            }
        }

        return true;
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

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        if (this.raidTarget != null) {
            tag.putInt("RaidTargetX", this.raidTarget.getX());
            tag.putInt("RaidTargetY", this.raidTarget.getY());
            tag.putInt("RaidTargetZ", this.raidTarget.getZ());
        }
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        if (tag.contains("RaidTargetX")) {
            this.raidTarget = new BlockPos(
                    tag.getInt("RaidTargetX"),
                    tag.getInt("RaidTargetY"),
                    tag.getInt("RaidTargetZ")
            );
        }
    }

    @Override
    protected @Nullable SoundEvent getAmbientSound() {
        return ModSounds.HAYBOT_AMBIENT.get();
    }

    @Override
    protected @Nullable SoundEvent getHurtSound(@NotNull DamageSource damageSource) {
        return ModSounds.HAYBOT_HURT.get();
    }

    @Override
    protected @Nullable SoundEvent getDeathSound() {
        return ModSounds.HAYBOT_DEATH.get();
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