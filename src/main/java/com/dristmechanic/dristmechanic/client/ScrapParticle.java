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
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import software.bernie.geckolib.animatable.GeoAnimatable;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.animation.AnimationController;
import software.bernie.geckolib.animation.PlayState;
import software.bernie.geckolib.cache.object.BakedGeoModel;
import software.bernie.geckolib.model.GeoModel;
import software.bernie.geckolib.renderer.GeoRenderer;
import software.bernie.geckolib.util.GeckoLibUtil;

@SuppressWarnings("SpellCheckingInspection")
public class ScrapParticle extends Particle {

    public enum ScrapType {
        BOLT("scrap_bolt"), GEAR("scrap_gear"), NUT("scrap_nut"),
        SCREW("scrap_screw"), WIRES("scrap_wires");

        public final String name;
        ScrapType(String name) { this.name = name; }
    }

    public static class ScrapAnimatable implements GeoAnimatable {
        private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);

        @Override
        public double getTick(Object object) { return 0; }

        @Override
        public AnimatableInstanceCache getAnimatableInstanceCache() { return cache; }

        @Override
        public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
            // ВАЖНО: GeckoLib требует хотя бы один контроллер для инициализации модели!
            controllers.add(new AnimationController<>(this, "main", 0, state -> PlayState.STOP));
        }
    }

    public static class ScrapGeoModel extends GeoModel<ScrapAnimatable> {
        private static final ResourceLocation TEXTURE =
                ResourceLocation.fromNamespaceAndPath("dristmechanic", "textures/particle/scrap.png");
        private final ScrapType type;

        public ScrapGeoModel(ScrapType type) { this.type = type; }

        @Override
        public ResourceLocation getModelResource(ScrapAnimatable animatable) {
            return ResourceLocation.fromNamespaceAndPath("dristmechanic", "geo/" + type.name + ".geo.json");
        }

        @Override
        public ResourceLocation getTextureResource(ScrapAnimatable animatable) { return TEXTURE; }

        @Override
        public ResourceLocation getAnimationResource(ScrapAnimatable animatable) { return null; }
    }

    public static class ScrapRenderer implements GeoRenderer<ScrapAnimatable> {
        private final ScrapGeoModel model;
        private final ScrapAnimatable animatable;

        public ScrapRenderer(ScrapGeoModel model, ScrapAnimatable animatable) {
            this.model = model;
            this.animatable = animatable;
        }

        @Override
        @NotNull
        public GeoModel<ScrapAnimatable> getGeoModel() { return model; }

        @Override
        @NotNull
        public ScrapAnimatable getAnimatable() { return animatable; }

        public void updateAnimatedTextureFrame(ScrapAnimatable animatable) { }

        @Override
        public void fireCompileRenderLayersEvent() { }

        @Override
        public boolean firePreRenderEvent(PoseStack poseStack, BakedGeoModel bakedModel, MultiBufferSource bufferSource, float partialTick, int packedLight) {
            return false;
        }

        @Override
        public void firePostRenderEvent(PoseStack poseStack, BakedGeoModel bakedModel, MultiBufferSource bufferSource, float partialTick, int packedLight) { }

        // Обертка для вызова actuallyRender, чтобы избежать трансформаций сущностей из defaultRender
        public void renderModel(PoseStack poseStack, BakedGeoModel model, RenderType renderType, MultiBufferSource bufferSource, float partialTicks, int light) {
            // Сигнатура: (PoseStack, T, BakedGeoModel, RenderType, MultiBufferSource, VertexConsumer, boolean, float, int, int, int)
            actuallyRender(poseStack, this.animatable, model, renderType, bufferSource, null, false, partialTicks, light, OverlayTexture.NO_OVERLAY, -1);
        }
    }

    private final ScrapGeoModel model;
    private final ScrapAnimatable animatable;
    private final float initialPitch;
    private final float initialYaw;

    public ScrapParticle(ClientLevel level, double x, double y, double z,
                         double xSpeed, double ySpeed, double zSpeed, ScrapType type) {
        super(level, x, y, z, xSpeed, ySpeed, zSpeed);
        this.xd = xSpeed; this.yd = ySpeed; this.zd = zSpeed;
        this.gravity = 1.0F;
        this.lifetime = 100 + level.random.nextInt(40);
        this.model = new ScrapGeoModel(type);
        this.animatable = new ScrapAnimatable();
        this.initialPitch = level.random.nextFloat() * Mth.TWO_PI;
        this.initialYaw = level.random.nextFloat() * Mth.TWO_PI;
    }

    @Override
    @NotNull
    public ParticleRenderType getRenderType() { return ParticleRenderType.CUSTOM; }

    @Override
    public void tick() {
        this.xo = this.x; this.yo = this.y; this.zo = this.z;
        if (this.age++ >= this.lifetime) { this.remove(); return; }

        this.yd -= 0.04D * this.gravity;
        this.move(this.xd, this.yd, this.zd);
        this.xd *= 0.98D; this.yd *= 0.98D; this.zd *= 0.98D;
        this.oRoll = this.roll;

        if (this.onGround) {
            if (this.yd < -0.1D) { this.yd *= -0.2D; this.onGround = false; }
            else { this.xd = 0; this.zd = 0; }
        } else {
            double speed = Math.sqrt(this.xd * this.xd + this.zd * this.zd);
            this.roll += (float)(speed * 8.0D);
        }
    }

    @Override
    public void render(@NotNull VertexConsumer buffer, @NotNull Camera camera, float partialTicks) {
        PoseStack pose = new PoseStack();
        Vec3 cam = camera.getPosition();

        double x = Mth.lerp(partialTicks, this.xo, this.x) - cam.x();
        double y = Mth.lerp(partialTicks, this.yo, this.y) - cam.y();
        double z = Mth.lerp(partialTicks, this.zo, this.z) - cam.z();

        pose.pushPose();
        pose.translate(x, y, z);
        pose.mulPose(Axis.XP.rotation(this.initialPitch));
        pose.mulPose(Axis.YP.rotation(this.initialYaw));
        pose.mulPose(Axis.ZP.rotation(Mth.lerp(partialTicks, this.oRoll, this.roll)));

        // Уменьшил масштаб до 0.5F, чтобы модели не застревали в блоках при спавне
        pose.scale(1.0F, 1.0F, 1.0F);

        MultiBufferSource.BufferSource buffers = Minecraft.getInstance().renderBuffers().bufferSource();
        ResourceLocation tex = this.model.getTextureResource(this.animatable);
        RenderType renderType = RenderType.entityCutoutNoCull(tex);

        // Fullbright свет (максимальная яркость)
        int light = 15728880;

        // Получаем запеченную модель
        BakedGeoModel bakedModel = this.model.getBakedModel(this.model.getModelResource(this.animatable));

        // ДИАГНОСТИКА: Если модель не загрузилась, выводим ошибку в консоль и выходим
        if (bakedModel == null) {
            System.err.println("[DristMechanic] ScrapParticle: BakedGeoModel is NULL! Resource: " + this.model.getModelResource(this.animatable));
            pose.popPose();
            return;
        }

        ScrapRenderer renderer = new ScrapRenderer(this.model, this.animatable);

        // Вызываем нашу обертку, которая использует actuallyRender напрямую
        renderer.renderModel(pose, bakedModel, renderType, buffers, partialTicks, light);

        buffers.endBatch(renderType);
        pose.popPose();
    }

    public static class Factory implements ParticleProvider<SimpleParticleType> {
        @Override
        @Nullable
        public Particle createParticle(@NotNull SimpleParticleType type, @NotNull ClientLevel level,
                                       double x, double y, double z,
                                       double xSpeed, double ySpeed, double zSpeed) {
            ScrapType[] types = ScrapType.values();
            ScrapType randomType = types[level.getRandom().nextInt(types.length)];
            return new ScrapParticle(level, x, y, z, xSpeed, ySpeed, zSpeed, randomType);
        }
    }
}