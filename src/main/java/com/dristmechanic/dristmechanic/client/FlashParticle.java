package com.dristmechanic.dristmechanic.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleProvider;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class FlashParticle extends Particle {

    private static final double AIR_DRAG = 0.92D;
    private static final double DECAY_RATE = 0.02D;
    private static final float BASE_SIZE = 0.5F;

    private static final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath("dristmechanic", "textures/particle/flash.png");
    private static final RenderType RENDER_TYPE = RenderType.entityTranslucentEmissive(TEXTURE);

    private final float initialPitch, initialYaw;
    private float rotation = 0.0F;
    private float rotationO = 0.0F;
    private BlockPos lightPos = BlockPos.ZERO;
    private int lastLX = Integer.MIN_VALUE, lastLY = Integer.MIN_VALUE, lastLZ = Integer.MIN_VALUE;

    public FlashParticle(ClientLevel level, double x, double y, double z, double xSpeed, double ySpeed, double zSpeed) {
        super(level, x, y, z, xSpeed, ySpeed, zSpeed);
        this.xd = xSpeed;
        this.yd = ySpeed;
        this.zd = zSpeed;
        this.lifetime = 20 + level.random.nextInt(10);
        this.initialPitch = level.random.nextFloat() * Mth.TWO_PI;
        this.initialYaw = level.random.nextFloat() * Mth.TWO_PI;
        this.setSize(BASE_SIZE, BASE_SIZE);
        this.hasPhysics = false;
    }

    @Override
    @NotNull
    public ParticleRenderType getRenderType() {
        return ParticleRenderType.CUSTOM;
    }

    @Override
    public void tick() {
        this.xo = this.x;
        this.yo = this.y;
        this.zo = this.z;
        this.rotationO = this.rotation;

        if (this.age++ >= this.lifetime) {
            this.remove();
            return;
        }

        this.xd *= AIR_DRAG;
        this.yd *= AIR_DRAG;
        this.zd *= AIR_DRAG;

        this.move(this.xd, this.yd, this.zd);

        double speed = Math.sqrt(this.xd * this.xd + this.yd * this.yd + this.zd * this.zd);
        this.rotation += (float) (speed * 10.0D);

        this.alpha -= DECAY_RATE;
        if (this.alpha <= 0.0F) {
            this.remove();
        }
    }

    @Override
    protected int getLightColor(float partialTick) {
        int lx = Mth.floor(this.x);
        int ly = Mth.floor(this.y);
        int lz = Mth.floor(this.z);
        if (lx != lastLX || ly != lastLY || lz != lastLZ) {
            this.lightPos = new BlockPos(lx, ly, lz);
            lastLX = lx;
            lastLY = ly;
            lastLZ = lz;
        }
        return LevelRenderer.getLightColor(this.level, this.lightPos);
    }

    public void renderCustom(PoseStack poseStack, MultiBufferSource bufferSource, Camera camera, float partialTicks) {
        double x = Mth.lerp(partialTicks, this.xo, this.x) - camera.getPosition().x();
        double y = Mth.lerp(partialTicks, this.yo, this.y) - camera.getPosition().y();
        double z = Mth.lerp(partialTicks, this.zo, this.z) - camera.getPosition().z();

        float currentRoll = Mth.lerp(partialTicks, this.rotationO, this.rotation);

        double speed = Math.sqrt(this.xd * this.xd + this.yd * this.yd + this.zd * this.zd);
        if (speed < 0.001D) speed = 0.001D;

        double nx = this.xd / speed;
        double ny = this.yd / speed;
        double nz = this.zd / speed;

        float yaw = (float) Math.atan2(nx, nz);
        float pitch = (float) Math.asin(-ny);

        poseStack.pushPose();
        poseStack.translate(x, y, z);
        poseStack.mulPose(Axis.YP.rotation(yaw));
        poseStack.mulPose(Axis.XP.rotation(pitch));
        poseStack.mulPose(Axis.ZP.rotation(currentRoll));

        float size = BASE_SIZE * (1.0F + (float) speed * 2.0F);
        float u0 = 0.0F;
        float v0 = 0.0F;
        float u1 = 1.0F;
        float v1 = 1.0F;

        int light = getLightColor(partialTicks);

        VertexConsumer buffer = bufferSource.getBuffer(RENDER_TYPE);

        buffer.vertex(poseStack.last().pose(), -size, -size, 0.0F)
                .color(1.0F, 1.0F, 1.0F, this.alpha)
                .uv(u0, v1)
                .overlayCoords(OverlayTexture.NO_OVERLAY)
                .uv2(light)
                .normal(poseStack.last().normal(), 0.0F, 0.0F, 1.0F)
                .endVertex();

        buffer.vertex(poseStack.last().pose(), -size, size, 0.0F)
                .color(1.0F, 1.0F, 1.0F, this.alpha)
                .uv(u0, v0)
                .overlayCoords(OverlayTexture.NO_OVERLAY)
                .uv2(light)
                .normal(poseStack.last().normal(), 0.0F, 0.0F, 1.0F)
                .endVertex();

        buffer.vertex(poseStack.last().pose(), size, size, 0.0F)
                .color(1.0F, 1.0F, 1.0F, this.alpha)
                .uv(u1, v0)
                .overlayCoords(OverlayTexture.NO_OVERLAY)
                .uv2(light)
                .normal(poseStack.last().normal(), 0.0F, 0.0F, 1.0F)
                .endVertex();

        buffer.vertex(poseStack.last().pose(), size, -size, 0.0F)
                .color(1.0F, 1.0F, 1.0F, this.alpha)
                .uv(u1, v1)
                .overlayCoords(OverlayTexture.NO_OVERLAY)
                .uv2(light)
                .normal(poseStack.last().normal(), 0.0F, 0.0F, 1.0F)
                .endVertex();

        poseStack.popPose();
    }

    @Override
    public void render(@NotNull VertexConsumer buffer, @NotNull Camera camera, float partialTicks) {
        renderCustom(new PoseStack(), Minecraft.getInstance().renderBuffers().bufferSource(), camera, partialTicks);
    }

    public static class Factory implements ParticleProvider<SimpleParticleType> {
        @Override
        @Nullable
        public Particle createParticle(@NotNull SimpleParticleType type, @NotNull ClientLevel level, double x, double y, double z, double xS, double yS, double zS) {
            return new FlashParticle(level, x, y, z, xS, yS, zS);
        }
    }
}