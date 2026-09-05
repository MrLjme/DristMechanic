package com.dristmechanic.dristmechanic.entity;

import com.dristmechanic.dristmechanic.init.ModEntities;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.blaze3d.vertex.VertexFormat;
import com.mojang.math.Axis;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.RenderStateShard;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.util.RandomSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.AreaEffectCloud;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.alchemy.PotionContents;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class ChemicalProjectileEntity extends AbstractArrow {

    private static final RenderType POSITION_COLOR = RenderType.create(
            "chemical_cloud_color",
            DefaultVertexFormat.POSITION_COLOR,
            VertexFormat.Mode.QUADS,
            256,
            false,
            false,
            RenderType.CompositeState.builder()
                    .setShaderState(RenderStateShard.POSITION_COLOR_SHADER)
                    .setTransparencyState(RenderStateShard.TRANSLUCENT_TRANSPARENCY)
                    .setCullState(RenderStateShard.NO_CULL)
                    .setDepthTestState(RenderStateShard.LEQUAL_DEPTH_TEST)
                    .setWriteMaskState(RenderStateShard.COLOR_WRITE)
                    .setLightmapState(RenderStateShard.NO_LIGHTMAP)
                    .createCompositeState(false)
    );

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
        AreaEffectCloud cloud = new AreaEffectCloud(this.level(), this.getX(), this.getY(), this.getZ()) {
            @Override
            public void refreshDimensions() {
                super.refreshDimensions();
                float f = this.getRadius();
                this.setBoundingBox(new AABB(this.getX() - f, this.getY() - 2.5, this.getZ() - f, this.getX() + f, this.getY() + 2.5, this.getZ() + f));
            }

            @Override
            public net.minecraft.world.entity.EntityDimensions getDimensions(net.minecraft.world.entity.Pose pose) {
                return net.minecraft.world.entity.EntityDimensions.scalable(this.getRadius() * 2.0F, 5.0F);
            }
        };
        if (this.getOwner() instanceof LivingEntity owner) {
            cloud.setOwner(owner);
        }
        cloud.setRadius(3.5F);
        cloud.setDuration(100);
        cloud.setWaitTime(0);
        cloud.setRadiusPerTick((cloud.getRadius() - cloud.getRadiusOnUse()) / cloud.getDuration());
        cloud.setParticle(new DustParticleOptions(new Vector3f(0.0f, 0.0f, 0.0f), 0.0f));
        PotionContents potionContents = new PotionContents(Optional.empty(), Optional.empty(), List.of(new MobEffectInstance(MobEffects.HARM, 1, 0)));
        cloud.setPotionContents(potionContents);

        float initialRadius = 3.5F;
        cloud.setBoundingBox(new AABB(cloud.getX() - initialRadius, cloud.getY() - 2.5, cloud.getZ() - initialRadius, cloud.getX() + initialRadius, cloud.getY() + 2.5, cloud.getZ() + initialRadius));

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

    @EventBusSubscriber(value = Dist.CLIENT)
    public static class ClientEvents {
        private static final List<VolumetricParticleData> volumetricParticles = new ArrayList<>();

        @SubscribeEvent
        public static void onClientTick(ClientTickEvent.Post event) {
            for (int i = volumetricParticles.size() - 1; i >= 0; i--) {
                VolumetricParticleData p = volumetricParticles.get(i);
                p.tick();
                if (p.life <= 0) {
                    volumetricParticles.set(i, volumetricParticles.get(volumetricParticles.size() - 1));
                    volumetricParticles.remove(volumetricParticles.size() - 1);
                }
            }

            ClientLevel level = Minecraft.getInstance().level;
            if (level != null) {
                for (Entity entity : level.entitiesForRendering()) {
                    if (entity instanceof AreaEffectCloud cloud && cloud.isAlive()) {
                        float radius = cloud.getRadius();
                        int remainingDuration = cloud.getDuration();
                        if (remainingDuration > 50 && radius > 0.1f && level.random.nextFloat() < 0.2f) {
                            for (int i = 0; i < 8; i++) {
                                double u = level.random.nextDouble();
                                double v = level.random.nextDouble();
                                double theta = 2 * Math.PI * u;
                                double phi = Math.acos(2 * v - 1);
                                double r = radius * Math.cbrt(level.random.nextDouble());
                                double xOffset = r * Math.sin(phi) * Math.cos(theta);
                                double yOffset = r * Math.sin(phi) * Math.sin(theta) * (5.0 / 7.0);
                                double zOffset = r * Math.cos(phi);
                                volumetricParticles.add(new VolumetricParticleData(cloud.getX() + xOffset, cloud.getY() + yOffset, cloud.getZ() + zOffset, level.random));
                            }
                        }
                    }
                }
            }
        }

        @SubscribeEvent
        public static void onRenderLevelStage(RenderLevelStageEvent event) {
            if (event.getStage() == RenderLevelStageEvent.Stage.AFTER_PARTICLES) {
                Minecraft mc = Minecraft.getInstance();
                ClientLevel level = mc.level;
                if (level == null || volumetricParticles.isEmpty()) return;

                PoseStack poseStack = event.getPoseStack();
                Camera camera = event.getCamera();
                var bufferSource = mc.renderBuffers().bufferSource();
                var buffer = bufferSource.getBuffer(POSITION_COLOR);

                Vec3 camPos = camera.getPosition();
                float partialTick = event.getPartialTick().getGameTimeDeltaPartialTick(false);

                double cx = camPos.x, cy = camPos.y, cz = camPos.z;
                volumetricParticles.sort((p1, p2) -> {
                    double dx1 = p1.x - cx, dy1 = p1.y - cy, dz1 = p1.z - cz;
                    double dx2 = p2.x - cx, dy2 = p2.y - cy, dz2 = p2.z - cz;
                    return Double.compare(dx2 * dx2 + dy2 * dy2 + dz2 * dz2, dx1 * dx1 + dy1 * dy1 + dz1 * dz1);
                });

                poseStack.pushPose();

                for (VolumetricParticleData p : volumetricParticles) {
                    poseStack.pushPose();
                    double renderX = p.prevX + (p.x - p.prevX) * partialTick;
                    double renderY = p.prevY + (p.y - p.prevY) * partialTick;
                    double renderZ = p.prevZ + (p.z - p.prevZ) * partialTick;

                    poseStack.translate(renderX - camPos.x(), renderY - camPos.y(), renderZ - camPos.z());

                    float currentLife = p.prevLife + (p.life - p.prevLife) * partialTick;
                    float pulse = 1.0f + (float) Math.sin(currentLife * 0.3) * 0.2f;
                    float finalScale = p.scale * pulse;
                    poseStack.scale(finalScale, finalScale, finalScale);
                    poseStack.translate(-0.5, -0.5, -0.5);

                    float rotX = p.prevRotX + (p.rotX - p.prevRotX) * partialTick;
                    float rotY = p.prevRotY + (p.rotY - p.prevRotY) * partialTick;
                    float rotZ = p.prevRotZ + (p.rotZ - p.prevRotZ) * partialTick;

                    poseStack.mulPose(Axis.XP.rotationDegrees(rotX));
                    poseStack.mulPose(Axis.YP.rotationDegrees(rotY));
                    poseStack.mulPose(Axis.ZP.rotationDegrees(rotZ));

                    float alpha = 0.5f;
                    if (currentLife > p.maxLife - 10) {
                        alpha *= (float) (p.maxLife - currentLife) / 10.0f;
                    } else if (currentLife < 10) {
                        alpha *= (float) currentLife / 10.0f;
                    }

                    renderCube(poseStack, buffer, 0.988f, 0.165f, 1.0f, alpha);
                    poseStack.popPose();
                }

                bufferSource.endBatch(POSITION_COLOR);
                poseStack.popPose();
            }
        }

        private static void renderCube(PoseStack poseStack, VertexConsumer buffer, float r, float g, float b, float alpha) {
            PoseStack.Pose pose = poseStack.last();
            addVertex(buffer, pose, 0, 0, 0, r, g, b, alpha, 0, -1, 0);
            addVertex(buffer, pose, 1, 0, 0, r, g, b, alpha, 0, -1, 0);
            addVertex(buffer, pose, 1, 0, 1, r, g, b, alpha, 0, -1, 0);
            addVertex(buffer, pose, 0, 0, 1, r, g, b, alpha, 0, -1, 0);

            addVertex(buffer, pose, 0, 1, 1, r, g, b, alpha, 0, 1, 0);
            addVertex(buffer, pose, 1, 1, 1, r, g, b, alpha, 0, 1, 0);
            addVertex(buffer, pose, 1, 1, 0, r, g, b, alpha, 0, 1, 0);
            addVertex(buffer, pose, 0, 1, 0, r, g, b, alpha, 0, 1, 0);

            addVertex(buffer, pose, 0, 0, 0, r, g, b, alpha, 0, 0, -1);
            addVertex(buffer, pose, 0, 1, 0, r, g, b, alpha, 0, 0, -1);
            addVertex(buffer, pose, 1, 1, 0, r, g, b, alpha, 0, 0, -1);
            addVertex(buffer, pose, 1, 0, 0, r, g, b, alpha, 0, 0, -1);

            addVertex(buffer, pose, 1, 0, 1, r, g, b, alpha, 0, 0, 1);
            addVertex(buffer, pose, 1, 1, 1, r, g, b, alpha, 0, 0, 1);
            addVertex(buffer, pose, 0, 1, 1, r, g, b, alpha, 0, 0, 1);
            addVertex(buffer, pose, 0, 0, 1, r, g, b, alpha, 0, 0, 1);

            addVertex(buffer, pose, 0, 0, 1, r, g, b, alpha, -1, 0, 0);
            addVertex(buffer, pose, 0, 1, 1, r, g, b, alpha, -1, 0, 0);
            addVertex(buffer, pose, 0, 1, 0, r, g, b, alpha, -1, 0, 0);
            addVertex(buffer, pose, 0, 0, 0, r, g, b, alpha, -1, 0, 0);

            addVertex(buffer, pose, 1, 0, 0, r, g, b, alpha, 1, 0, 0);
            addVertex(buffer, pose, 1, 1, 0, r, g, b, alpha, 1, 0, 0);
            addVertex(buffer, pose, 1, 1, 1, r, g, b, alpha, 1, 0, 0);
            addVertex(buffer, pose, 1, 0, 1, r, g, b, alpha, 1, 0, 0);
        }

        private static void addVertex(VertexConsumer buffer, PoseStack.Pose pose, float x, float y, float z,
                                      float r, float g, float b, float a,
                                      float nx, float ny, float nz) {
            buffer.addVertex(pose, x, y, z)
                    .setColor(r, g, b, a)
                    .setNormal(pose, nx, ny, nz);
        }
    }

    public static class VolumetricParticleData {
        double x, y, z;
        double prevX, prevY, prevZ;
        int life;
        int prevLife;
        int maxLife;
        float rotX, rotY, rotZ;
        float prevRotX, prevRotY, prevRotZ;
        float scale;

        public VolumetricParticleData(double x, double y, double z, RandomSource random) {
            this.x = x; this.y = y; this.z = z;
            this.prevX = x; this.prevY = y; this.prevZ = z;
            this.maxLife = 45 + random.nextInt(15);
            this.life = this.maxLife;
            this.prevLife = this.life;
            this.rotX = random.nextFloat() * 360;
            this.rotY = random.nextFloat() * 360;
            this.rotZ = random.nextFloat() * 360;
            this.prevRotX = rotX; this.prevRotY = rotY; this.prevRotZ = rotZ;
            this.scale = 1.2f + random.nextFloat() * 1.5f;
        }

        public void tick() {
            this.prevX = this.x;
            this.prevY = this.y;
            this.prevZ = this.z;
            this.prevLife = this.life;
            this.prevRotX = this.rotX;
            this.prevRotY = this.rotY;
            this.prevRotZ = this.rotZ;

            this.y += 0.02;
            this.life--;
            this.rotY += 5;
        }
    }
}