package com.dristmechanic.dristmechanic.client;

import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.blaze3d.vertex.VertexFormat;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleProvider;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderStateShard;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class FlashParticle extends Particle {

    private static final double AIR_DRAG = 0.92D;
    private static final float BASE_WIDTH = 0.3F;
    private static final float BASE_LENGTH = 0.4F;

    private static final float PERP_START = 0.25F;
    private static final float PERP_END = 0.85F;

    private static final float TINT_R = 0.15F;
    private static final float TINT_G = 0.45F;
    private static final float TINT_B = 1.00F;

    // Обе фазы теперь занимают одинаковое время и используют одинаковую механику
    private static final float FADE_IN_TICKS = 2.0F;
    private static final float FADE_OUT_TICKS = 2.0F;
    private static final int FULL_BRIGHT = 0xF000F0;

    private static final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath(
            "dristmechanic", "textures/particle/flash.png");

    private static final RenderType RENDER_TYPE = RenderType.create(
            "flash_particle",
            DefaultVertexFormat.NEW_ENTITY,
            VertexFormat.Mode.QUADS,
            256,
            false,
            false,
            RenderType.CompositeState.builder()
                    .setShaderState(new RenderStateShard.ShaderStateShard(
                            GameRenderer::getRendertypeEntityTranslucentEmissiveShader))
                    .setTextureState(new RenderStateShard.TextureStateShard(TEXTURE, false, false))
                    .setTransparencyState(new RenderStateShard.TransparencyStateShard("flash_additive",
                            () -> {
                                RenderSystem.enableBlend();
                                RenderSystem.blendFunc(
                                        GlStateManager.SourceFactor.ONE,
                                        GlStateManager.DestFactor.ONE);
                            },
                            () -> {
                                RenderSystem.disableBlend();
                                RenderSystem.defaultBlendFunc();
                            }))
                    .setWriteMaskState(new RenderStateShard.WriteMaskStateShard(true, false))
                    .setDepthTestState(new RenderStateShard.DepthTestStateShard("lequal", 515))
                    .setCullState(new RenderStateShard.CullStateShard(false))
                    .setLightmapState(new RenderStateShard.LightmapStateShard(true))
                    .setOverlayState(new RenderStateShard.OverlayStateShard(true))
                    .createCompositeState(false)
    );

    private final float initialSpeed;

    public FlashParticle(ClientLevel level, double x, double y, double z,
                         double xSpeed, double ySpeed, double zSpeed) {
        super(level, x, y, z, xSpeed, ySpeed, zSpeed);
        this.xd = xSpeed;
        this.yd = ySpeed;
        this.zd = zSpeed;
        this.lifetime = 7;
        this.setSize(BASE_WIDTH, BASE_WIDTH);
        this.hasPhysics = false;
        this.initialSpeed = (float) Math.sqrt(xSpeed * xSpeed + ySpeed * ySpeed + zSpeed * zSpeed);
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

        if (this.age++ >= this.lifetime) {
            this.remove();
            return;
        }

        this.xd *= AIR_DRAG;
        this.yd *= AIR_DRAG;
        this.zd *= AIR_DRAG;

        this.move(this.xd, this.yd, this.zd);
    }

    @Override
    public void render(@NotNull VertexConsumer buffer, @NotNull Camera camera, float partialTicks) {
        if (this.removed) {
            return;
        }

        PoseStack poseStack = new PoseStack();
        MultiBufferSource.BufferSource bufferSource = Minecraft.getInstance().renderBuffers().bufferSource();

        double px = Mth.lerp(partialTicks, this.xo, this.x) - camera.getPosition().x();
        double py = Mth.lerp(partialTicks, this.yo, this.y) - camera.getPosition().y();
        double pz = Mth.lerp(partialTicks, this.zo, this.z) - camera.getPosition().z();

        double speed = Math.sqrt(this.xd * this.xd + this.yd * this.yd + this.zd * this.zd);
        if (speed < 0.001D) speed = 0.001D;

        double nx = this.xd / speed;
        double ny = this.yd / speed;
        double nz = this.zd / speed;

        double vx = -px;
        double vy = -py;
        double vz = -pz;
        double vlen = Math.sqrt(vx * vx + vy * vy + vz * vz);
        if (vlen > 0.001D) {
            vx /= vlen;
            vy /= vlen;
            vz /= vlen;
        }

        double dot = vx * nx + vy * ny + vz * nz;

        float absDot = (float) Math.abs(dot);
        float perpFade = 1.0F - Mth.clamp(
                (absDot - PERP_START) / (PERP_END - PERP_START), 0.0F, 1.0F);
        if (perpFade <= 0.0F) {
            return;
        }

        float t = this.age + partialTicks;

        // Появление: плавное нарастание за 2 тика
        float fadeIn = smoothstep(t / FADE_IN_TICKS);

        // Угасание: начинается за 2 тика до конца жизни, использует ту же smoothstep функцию
        float fadeOutStart = this.lifetime - FADE_OUT_TICKS;
        float fadeOut = 1.0F;
        if (t > fadeOutStart) {
            fadeOut = smoothstep((this.lifetime - t) / FADE_OUT_TICKS);
        }

        float lifeFade = fadeIn * fadeOut;
        if (lifeFade <= 0.0F) {
            return;
        }

        float fade = lifeFade * perpFade;
        float r = TINT_R * fade;
        float g = TINT_G * fade;
        float b = TINT_B * fade;
        float a = 1.0F;

        float scale = fadeIn;
        float w = BASE_WIDTH * (1.0F + this.initialSpeed) * scale;
        float l = BASE_LENGTH * (1.0F + this.initialSpeed * 2.0F) * scale;

        double fx = vx - nx * dot;
        double fy = vy - ny * dot;
        double fz = vz - nz * dot;
        double flen = Math.sqrt(fx * fx + fy * fy + fz * fz);

        if (flen < 0.001D) {
            if (Math.abs(ny) > 0.99D) {
                fx = 1.0D; fy = 0.0D; fz = 0.0D;
            } else {
                fx = -nz; fy = 0.0D; fz = nx;
                double len = Math.sqrt(fx * fx + fz * fz);
                fx /= len; fz /= len;
            }
        } else {
            fx /= flen;
            fy /= flen;
            fz /= flen;
        }

        double rx = ny * fz - nz * fy;
        double ry = nz * fx - nx * fz;
        double rz = nx * fy - ny * fx;
        double rlen = Math.sqrt(rx * rx + ry * ry + rz * rz);
        if (rlen > 0.001D) {
            rx /= rlen;
            ry /= rlen;
            rz /= rlen;
        }

        double ux = ry * nz - rz * ny;
        double uy = rz * nx - rx * nz;
        double uz = rx * ny - ry * nx;

        float x0 = (float)(px + (-w) * rx + (-l) * nx);
        float y0 = (float)(py + (-w) * ry + (-l) * ny);
        float z0 = (float)(pz + (-w) * rz + (-l) * nz);

        float x1 = (float)(px + (-w) * rx + l * nx);
        float y1 = (float)(py + (-w) * ry + l * ny);
        float z1 = (float)(pz + (-w) * rz + l * nz);

        float x2 = (float)(px + w * rx + l * nx);
        float y2 = (float)(py + w * ry + l * ny);
        float z2 = (float)(pz + w * rz + l * nz);

        float x3 = (float)(px + w * rx + (-l) * nx);
        float y3 = (float)(py + w * ry + (-l) * ny);
        float z3 = (float)(pz + w * rz + (-l) * nz);

        float unx = (float) ux, uny = (float) uy, unz = (float) uz;

        VertexConsumer vc = bufferSource.getBuffer(RENDER_TYPE);

        vc.addVertex(poseStack.last().pose(), x0, y0, z0)
                .setColor(r, g, b, a)
                .setUv(0.0F, 1.0F)
                .setOverlay(OverlayTexture.NO_OVERLAY)
                .setLight(FULL_BRIGHT)
                .setNormal(poseStack.last(), unx, uny, unz);

        vc.addVertex(poseStack.last().pose(), x1, y1, z1)
                .setColor(r, g, b, a)
                .setUv(0.0F, 0.0F)
                .setOverlay(OverlayTexture.NO_OVERLAY)
                .setLight(FULL_BRIGHT)
                .setNormal(poseStack.last(), unx, uny, unz);

        vc.addVertex(poseStack.last().pose(), x2, y2, z2)
                .setColor(r, g, b, a)
                .setUv(1.0F, 0.0F)
                .setOverlay(OverlayTexture.NO_OVERLAY)
                .setLight(FULL_BRIGHT)
                .setNormal(poseStack.last(), unx, uny, unz);

        vc.addVertex(poseStack.last().pose(), x3, y3, z3)
                .setColor(r, g, b, a)
                .setUv(1.0F, 1.0F)
                .setOverlay(OverlayTexture.NO_OVERLAY)
                .setLight(FULL_BRIGHT)
                .setNormal(poseStack.last(), unx, uny, unz);
    }

    private static float smoothstep(float x) {
        x = Mth.clamp(x, 0.0F, 1.0F);
        return x * x * (3.0F - 2.0F * x);
    }

    public static class Factory implements ParticleProvider<SimpleParticleType> {
        @Override
        @Nullable
        public Particle createParticle(@NotNull SimpleParticleType type,
                                       @NotNull ClientLevel level,
                                       double x, double y, double z,
                                       double xS, double yS, double zS) {
            return new FlashParticle(level, x, y, z, xS, yS, zS);
        }
    }
}