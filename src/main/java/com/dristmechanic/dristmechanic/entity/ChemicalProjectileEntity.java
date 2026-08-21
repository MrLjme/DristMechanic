package com.dristmechanic.dristmechanic.entity;

import com.dristmechanic.dristmechanic.init.ModEntities;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.AreaEffectCloud;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.alchemy.PotionContents;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;

import java.util.List;
import java.util.Optional;

public class ChemicalProjectileEntity extends AbstractArrow {

    public ChemicalProjectileEntity(EntityType<? extends ChemicalProjectileEntity> type, Level level) {
        super(type, level);
    }

    public ChemicalProjectileEntity(LivingEntity shooter, Level level) {
        super(ModEntities.CHEMICAL_PROJECTILE.get(), shooter, level, ItemStack.EMPTY, null);
    }

    @Override
    protected ItemStack getDefaultPickupItem() {
        return ItemStack.EMPTY;
    }

    @Override
    protected void onHitEntity(EntityHitResult result) {
        if (!this.level().isClientSide) {
            spawnChemicalCloud();
            this.discard();
        }
    }

    @Override
    protected void onHitBlock(BlockHitResult result) {
        if (!this.level().isClientSide) {
            spawnChemicalCloud();
            this.discard();
        }
    }

    private void spawnChemicalCloud() {
        AreaEffectCloud cloud = new AreaEffectCloud(this.level(), this.getX(), this.getY(), this.getZ());
        if (this.getOwner() instanceof LivingEntity owner) {
            cloud.setOwner(owner);
        }
        cloud.setRadius(3.0F);
        cloud.setDuration(100);
        cloud.setWaitTime(0);
        cloud.setRadiusPerTick((cloud.getRadius() - cloud.getRadiusOnUse()) / cloud.getDuration());
        cloud.setParticle(ParticleTypes.DRAGON_BREATH);
        PotionContents potionContents = new PotionContents(Optional.empty(), Optional.empty(), List.of(new MobEffectInstance(MobEffects.HARM, 1, 2)));
        cloud.setPotionContents(potionContents);
        this.level().addFreshEntity(cloud);
    }

    @Override
    public void tick() {
        super.tick();
        if (this.tickCount > 100) {
            if (!this.level().isClientSide) {
                spawnChemicalCloud();
            }
            this.discard();
        }
    }
}