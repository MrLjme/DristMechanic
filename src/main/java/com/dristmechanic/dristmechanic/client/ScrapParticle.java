package com.dristmechanic.dristmechanic.client;

import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.blaze3d.vertex.VertexFormat;
import com.mojang.math.Axis;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleProvider;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderStateShard;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
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

import java.util.EnumMap;
import java.util.Map;

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

        @Override public double getTick(Object object) { return 0; }
        @Override public AnimatableInstanceCache getAnimatableInstanceCache() { return cache; }

        @Override
        public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
            controllers.add(new AnimationController<>(this, "main", 0, state -> PlayState.STOP));
        }
    }

    public static class ScrapGeoModel extends GeoModel<ScrapAnimatable> {
        private static final ResourceLocation TEXTURE =
                ResourceLocation.fromNamespaceAndPath("dristmechanic", "textures/particle/scrap.png");
        private final ScrapType type;

        public ScrapGeoModel(ScrapType type) { this.type = type; }

        @Override public ResourceLocation getModelResource(ScrapAnimatable animatable) {
            return ResourceLocation.fromNamespaceAndPath("dristmechanic", "geo/" + type.name + ".geo.json");
        }
        @Override public ResourceLocation getTextureResource(ScrapAnimatable animatable) { return TEXTURE; }
        @Override public ResourceLocation getAnimationResource(ScrapAnimatable animatable) { return null; }
    }

    public static class ScrapRenderer implements GeoRenderer<ScrapAnimatable> {
        private final ScrapGeoModel model;
        private final ScrapAnimatable animatable;
        private BakedGeoModel bakedModel;
        private RenderType renderType;

        public ScrapRenderer(ScrapGeoModel model, ScrapAnimatable animatable) {
            this.model = model;
            this.animatable = animatable;
        }

        public BakedGeoModel getBaked() {
            if (this.bakedModel == null) {
                this.bakedModel = this.model.getBakedModel(this.model.getModelResource(this.animatable));
            }
            return this.bakedModel;
        }

        public RenderType getRenderType() {
            if (this.renderType == null) {
                ResourceLocation texture = this.model.getTextureResource(this.animatable);

                RenderType.CompositeState state = RenderType.CompositeState.builder()
                        .setShaderState(RenderStateShard.RENDERTYPE_ENTITY_CUTOUT_SHADER)
                        .setTextureState(new RenderStateShard.TextureStateShard(texture, false, false))
                        .setTransparencyState(RenderStateShard.NO_TRANSPARENCY)
                        .setCullState(RenderStateShard.NO_CULL)
                        .setLightmapState(RenderStateShard.LIGHTMAP)
                        .setOverlayState(RenderStateShard.OVERLAY)
                        .createCompositeState(false);

                this.renderType = RenderType.create(
                        "scrap_particle_cutout",
                        DefaultVertexFormat.NEW_ENTITY,
                        VertexFormat.Mode.QUADS,
                        256,
                        false,
                        false,
                        state
                );
            }
            return this.renderType;
        }

        @Override @NotNull public GeoModel<ScrapAnimatable> getGeoModel() { return model; }
        @Override @NotNull public ScrapAnimatable getAnimatable() { return animatable; }
        public void updateAnimatedTextureFrame(ScrapAnimatable animatable) { }
        @Override public void fireCompileRenderLayersEvent() { }
        @Override public boolean firePreRenderEvent(PoseStack poseStack, BakedGeoModel bakedModel, MultiBufferSource bufferSource, float partialTick, int packedLight) { return false; }
        @Override public void firePostRenderEvent(PoseStack poseStack, BakedGeoModel bakedModel, MultiBufferSource bufferSource, float partialTick, int packedLight) { }

        public void renderModel(PoseStack poseStack, BakedGeoModel model, RenderType renderType, MultiBufferSource bufferSource, float partialTicks, int light) {
            VertexConsumer buffer = bufferSource.getBuffer(renderType);
            actuallyRender(poseStack, this.animatable, model, renderType, bufferSource, buffer, false, partialTicks, light, OverlayTexture.NO_OVERLAY, -1);
        }
    }

    private static final Map<ScrapType, ScrapGeoModel> MODELS = new EnumMap<>(ScrapType.class);
    private static final Map<ScrapType, ScrapAnimatable> ANIMATABLES = new EnumMap<>(ScrapType.class);
    private static final Map<ScrapType, ScrapRenderer> RENDERERS = new EnumMap<>(ScrapType.class);

    static {
        for (ScrapType type : ScrapType.values()) {
            ScrapGeoModel model = new ScrapGeoModel(type);
            ScrapAnimatable anim = new ScrapAnimatable();
            MODELS.put(type, model);
            ANIMATABLES.put(type, anim);
            RENDERERS.put(type, new ScrapRenderer(model, anim));
        }
    }

    private final ScrapType type;
    private final float initialPitch;
    private final float initialYaw;
    private boolean cachedVisibility = true;
    private int visibilityCheckCounter = 0;

    public ScrapParticle(ClientLevel level, double x, double y, double z,
                         double xSpeed, double ySpeed, double zSpeed, ScrapType type) {
        super(level, x, y, z, xSpeed, ySpeed, zSpeed);
        this.xd = xSpeed; this.yd = ySpeed; this.zd = zSpeed;
        this.gravity = 1.0F;
        this.lifetime = 100 + level.random.nextInt(40);
        this.type = type;
        this.initialPitch = level.random.nextFloat() * Mth.TWO_PI;
        this.initialYaw = level.random.nextFloat() * Mth.TWO_PI;

        // УВЕЛИЧИВАЕМ ХИТБОКС для предотвращения проваливания сквозь землю
        // Дефолтный размер 0.2f слишком мал для быстрого падения
        this.setSize(0.2F, 0.8F);
    }

    @Override @NotNull public ParticleRenderType getRenderType() {
        return ParticleRenderType.CUSTOM;
    }

    @Override
    public void tick() {
        this.xo = this.x; this.yo = this.y; this.zo = this.z;
        if (this.age++ >= this.lifetime) { this.remove(); return; }

        this.yd -= 0.04D * this.gravity;
        this.move(this.xd, this.yd, this.zd);
        this.xd *= 0.98D; this.yd *= 0.98D; this.zd *= 0.98D;
        this.oRoll = this.roll;

        if (this.onGround) {
            if (this.yd < -0.1D) {
                this.yd *= -0.2D;
                this.onGround = false;
            } else {
                this.xd = 0;
                this.zd = 0;
            }
        } else {
            double speed = Math.sqrt(this.xd * this.xd + this.zd * this.zd);
            this.roll += (float)(speed * 8.0D);
        }

        if (++visibilityCheckCounter >= 5) {
            visibilityCheckCounter = 0;
            updateVisibility();
        }
    }

    private void updateVisibility() {
        if (this.level == null) {
            cachedVisibility = true;
            return;
        }

        Minecraft mc = Minecraft.getInstance();

        if (mc.levelRenderer != null) {
            Frustum frustum = mc.levelRenderer.getFrustum();
            if (frustum != null) {
                AABB aabb = new AABB(this.x - 0.5, this.y - 0.5, this.z - 0.5,
                        this.x + 0.5, this.y + 0.5, this.z + 0.5);
                if (!frustum.isVisible(aabb)) {
                    cachedVisibility = false;
                    return;
                }
            }
        }

        Entity camera = mc.getCameraEntity();
        if (camera != null) {
            Vec3 cameraPos = camera.getEyePosition();
            Vec3 particlePos = new Vec3(this.x, this.y, this.z);
            double distanceSq = cameraPos.distanceToSqr(particlePos);

            if (distanceSq > 16.0 && distanceSq < 1024.0) {
                if (isOccluded(cameraPos, particlePos)) {
                    cachedVisibility = false;
                    return;
                }
            }
        }

        cachedVisibility = true;
    }

    private boolean isOccluded(Vec3 from, Vec3 to) {
        double distance = from.distanceTo(to);
        if (distance < 1.0) return false;

        int steps = (int)distance;
        if (steps < 2) return false;

        double stepX = (to.x - from.x) / steps;
        double stepY = (to.y - from.y) / steps;
        double stepZ = (to.z - from.z) / steps;

        double x = from.x;
        double y = from.y;
        double z = from.z;

        for (int i = 0; i < steps - 1; i++) {
            x += stepX;
            y += stepY;
            z += stepZ;

            BlockPos blockPos = BlockPos.containing(x, y, z);
            BlockState state = this.level.getBlockState(blockPos);

            if (state.getLightBlock(this.level, blockPos) > 0) {
                return true;
            }
        }

        return false;
    }

    @Override
    protected int getLightColor(float partialTick) {
        BlockPos blockPos = BlockPos.containing(this.x, this.y, this.z);
        return LevelRenderer.getLightColor(this.level, blockPos);
    }

    public void renderCustom(PoseStack poseStack, MultiBufferSource bufferSource, Camera camera, float partialTicks) {
        if (!cachedVisibility) return;

        double x = Mth.lerp(partialTicks, this.xo, this.x) - camera.getPosition().x();
        double y = Mth.lerp(partialTicks, this.yo, this.y) - camera.getPosition().y();
        double z = Mth.lerp(partialTicks, this.zo, this.z) - camera.getPosition().z();

        poseStack.pushPose();
        poseStack.translate(x, y, z);
        poseStack.mulPose(Axis.XP.rotation(this.initialPitch));
        poseStack.mulPose(Axis.YP.rotation(this.initialYaw));
        poseStack.mulPose(Axis.ZP.rotation(Mth.lerp(partialTicks, this.oRoll, this.roll)));
        poseStack.scale(1.0F, 1.0F, 1.0F);

        ScrapRenderer renderer = RENDERERS.get(this.type);
        BakedGeoModel bakedModel = renderer.getBaked();

        if (bakedModel == null) {
            poseStack.popPose();
            return;
        }

        int light = getLightColor(partialTicks);
        RenderType renderType = renderer.getRenderType();

        MultiBufferSource.BufferSource globalBuffer = Minecraft.getInstance().renderBuffers().bufferSource();
        renderer.renderModel(poseStack, bakedModel, renderType, globalBuffer, partialTicks, light);

        poseStack.popPose();
        globalBuffer.endBatch(renderType);
    }

    @Override
    public void render(@NotNull VertexConsumer buffer, @NotNull Camera camera, float partialTicks) {
        if (!cachedVisibility) return;

        MultiBufferSource.BufferSource globalBuffer = Minecraft.getInstance().renderBuffers().bufferSource();

        double x = Mth.lerp(partialTicks, this.xo, this.x) - camera.getPosition().x();
        double y = Mth.lerp(partialTicks, this.yo, this.y) - camera.getPosition().y();
        double z = Mth.lerp(partialTicks, this.zo, this.z) - camera.getPosition().z();

        PoseStack pose = new PoseStack();
        pose.pushPose();
        pose.translate(x, y, z);
        pose.mulPose(Axis.XP.rotation(this.initialPitch));
        pose.mulPose(Axis.YP.rotation(this.initialYaw));
        pose.mulPose(Axis.ZP.rotation(Mth.lerp(partialTicks, this.oRoll, this.roll)));
        pose.scale(1.0F, 1.0F, 1.0F);

        ScrapRenderer renderer = RENDERERS.get(this.type);
        BakedGeoModel bakedModel = renderer.getBaked();

        if (bakedModel == null) {
            pose.popPose();
            return;
        }

        int light = getLightColor(partialTicks);
        RenderType renderType = renderer.getRenderType();

        VertexConsumer vertexConsumer = globalBuffer.getBuffer(renderType);

        renderer.actuallyRender(
                pose, ANIMATABLES.get(this.type), bakedModel, renderType,
                globalBuffer, vertexConsumer, false, partialTicks, light,
                OverlayTexture.NO_OVERLAY, -1
        );

        pose.popPose();
        globalBuffer.endBatch(renderType);
    }

    public static class Factory implements ParticleProvider<SimpleParticleType> {
        @Override @Nullable
        public Particle createParticle(@NotNull SimpleParticleType type, @NotNull ClientLevel level,
                                       double x, double y, double z,
                                       double xSpeed, double ySpeed, double zSpeed) {
            ScrapType[] types = ScrapType.values();
            ScrapType randomType = types[level.getRandom().nextInt(types.length)];
            return new ScrapParticle(level, x, y, z, xSpeed, ySpeed, zSpeed, randomType);
        }
    }
}