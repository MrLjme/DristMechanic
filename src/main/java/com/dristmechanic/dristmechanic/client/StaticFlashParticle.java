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

public class StaticFlashParticle extends Particle {

    // Установил одинаковые значения для идеальной квадратной формы.
    // Если визуально кажется иначе, проверьте саму картинку flash.png на наличие прозрачных полей.
    private static final float BASE_WIDTH = 0.3F;
    private static final float BASE_LENGTH = 0.3F;

    private static final float TINT_R = 0.15F;
    private static final float TINT_G = 0.45F;
    private static final float TINT_B = 1.00F;

    private static final float FADE_IN_TICKS = 2.0F;
    private static final float FADE_OUT_TICKS = 2.0F;
    private static final int FULL_BRIGHT = 0xF000F0;

    private static final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath(
            "dristmechanic", "textures/particle/flash_static.png");

    private static final RenderType RENDER_TYPE = RenderType.create(
            "static_flash_particle",
            DefaultVertexFormat.NEW_ENTITY,
            VertexFormat.Mode.QUADS,
            256,
            false,
            false,
            RenderType.CompositeState.builder()
                    .setShaderState(new RenderStateShard.ShaderStateShard(
                            GameRenderer::getRendertypeEntityTranslucentEmissiveShader))
                    .setTextureState(new RenderStateShard.TextureStateShard(TEXTURE, false, false))
                    .setTransparencyState(new RenderStateShard.TransparencyStateShard("static_flash_additive",
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

    public StaticFlashParticle(ClientLevel level, double x, double y, double z) {
        super(level, x, y, z, 0.0, 0.0, 0.0);
        this.lifetime = 7;
        this.setSize(BASE_WIDTH, BASE_LENGTH);
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

        if (this.age++ >= this.lifetime) {
            this.remove();
        }
    }

    @Override
    public void render(@NotNull VertexConsumer buffer, @NotNull Camera camera, float partialTicks) {
        if (this.removed) {
            return;
        }

        PoseStack poseStack = new PoseStack();
        MultiBufferSource.BufferSource bufferSource = Minecraft.getInstance().renderBuffers().bufferSource();

        // Позиция частицы относительно камеры
        float px = (float) (Mth.lerp(partialTicks, this.xo, this.x) - camera.getPosition().x());
        float py = (float) (Mth.lerp(partialTicks, this.yo, this.y) - camera.getPosition().y());
        float pz = (float) (Mth.lerp(partialTicks, this.zo, this.z) - camera.getPosition().z());

        float t = this.age + partialTicks;

        // Плавное появление
        float fadeIn = smoothstep(t / FADE_IN_TICKS);

        // Плавное исчезновение
        float fadeOutStart = this.lifetime - FADE_OUT_TICKS;
        float fadeOut = 1.0F;
        if (t > fadeOutStart) {
            fadeOut = smoothstep((this.lifetime - t) / FADE_OUT_TICKS);
        }

        float lifeFade = fadeIn * fadeOut;
        if (lifeFade <= 0.0F) {
            return;
        }

        // Цвет теперь зависит только от времени жизни.
        // (Искусственное затемнение perpFade убрано, так как частица всегда смотрит на камеру).
        // Если цвет кажется слишком ярким/белым из-за аддитивного блендинга, уменьшите значения TINT_* выше.
        float r = TINT_R * lifeFade;
        float g = TINT_G * lifeFade;
        float b = TINT_B * lifeFade;
        float a = 1.0F;

        float scale = fadeIn;
        float w = BASE_WIDTH * scale;
        float l = BASE_LENGTH * scale;

        poseStack.pushPose();
        // Перемещаемся в точку спавна частицы
        poseStack.translate(px, py, pz);
        // Поворачиваем квад строго на камеру по всем осям (стандартный 2D билборд Minecraft)
        poseStack.mulPose(camera.rotation());

        VertexConsumer vc = bufferSource.getBuffer(RENDER_TYPE);

        // Рендерим идеальный квадрат в локальных координатах (после поворота камеры)
        vc.addVertex(poseStack.last().pose(), -w, -l, 0.0F)
                .setColor(r, g, b, a)
                .setUv(0.0F, 1.0F)
                .setOverlay(OverlayTexture.NO_OVERLAY)
                .setLight(FULL_BRIGHT)
                .setNormal(poseStack.last(), 0.0F, 0.0F, 1.0F);

        vc.addVertex(poseStack.last().pose(), -w, l, 0.0F)
                .setColor(r, g, b, a)
                .setUv(0.0F, 0.0F)
                .setOverlay(OverlayTexture.NO_OVERLAY)
                .setLight(FULL_BRIGHT)
                .setNormal(poseStack.last(), 0.0F, 0.0F, 1.0F);

        vc.addVertex(poseStack.last().pose(), w, l, 0.0F)
                .setColor(r, g, b, a)
                .setUv(1.0F, 0.0F)
                .setOverlay(OverlayTexture.NO_OVERLAY)
                .setLight(FULL_BRIGHT)
                .setNormal(poseStack.last(), 0.0F, 0.0F, 1.0F);

        vc.addVertex(poseStack.last().pose(), w, -l, 0.0F)
                .setColor(r, g, b, a)
                .setUv(1.0F, 1.0F)
                .setOverlay(OverlayTexture.NO_OVERLAY)
                .setLight(FULL_BRIGHT)
                .setNormal(poseStack.last(), 0.0F, 0.0F, 1.0F);

        poseStack.popPose();
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
            // Игнорируем переданную скорость, создаем строго стоячую частицу
            return new StaticFlashParticle(level, x, y, z);
        }
    }
}