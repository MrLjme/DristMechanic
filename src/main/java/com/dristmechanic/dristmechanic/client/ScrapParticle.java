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
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import software.bernie.geckolib.animatable.GeoAnimatable;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.cache.object.BakedGeoModel;
import software.bernie.geckolib.model.GeoModel;
import software.bernie.geckolib.renderer.GeoRenderer;
import software.bernie.geckolib.util.GeckoLibUtil;

import java.util.Collections;

public class ScrapParticle extends Particle {

    public enum ScrapType {
        BOLT("scrap_bolt"), GEAR("scrap_gear"), NUT("scrap_nut"),
        SCREW("scrap_screw"), WIRES("scrap_wires");
        public final String name;
        ScrapType(String name) { this.name = name; }
    }

    private static final double GRAVITY = 0.04D, AIR_DRAG = 0.98D, GROUND_FRICTION = 0.70D;
    private static final double RESTITUTION = 0.40D, MIN_BOUNCE_VEL = 0.08D, WALL_BOUNCE = 0.30D;
    private static final double STOP_THRESHOLD = 0.001D;

    private static final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath("dristmechanic", "textures/particle/scrap.png");
    private static final RenderType RENDER_TYPE = RenderType.entityCutoutNoCull(TEXTURE);
    private static final ScrapData[] DATA = new ScrapData[ScrapType.values().length];

    static {
        for (ScrapType type : ScrapType.values()) DATA[type.ordinal()] = new ScrapData(type);
    }

    public static class ScrapAnimatable implements GeoAnimatable {
        private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);
        @Override public double getTick(Object object) { return 0; }
        @Override public AnimatableInstanceCache getAnimatableInstanceCache() { return cache; }
        @Override public void registerControllers(AnimatableManager.ControllerRegistrar c) {}
    }

    public static class ScrapGeoModel extends GeoModel<ScrapAnimatable> {
        private final ResourceLocation modelLoc;
        public ScrapGeoModel(ScrapType type) {
            this.modelLoc = ResourceLocation.fromNamespaceAndPath("dristmechanic", "geo/" + type.name + ".geo.json");
        }
        @Override public ResourceLocation getModelResource(ScrapAnimatable a) { return modelLoc; }
        @Override public ResourceLocation getTextureResource(ScrapAnimatable a) { return TEXTURE; }
        @Override public ResourceLocation getAnimationResource(ScrapAnimatable a) { return null; }
    }

    public static class ScrapRenderer implements GeoRenderer<ScrapAnimatable> {
        private final ScrapGeoModel model;
        private final ScrapAnimatable animatable;
        private BakedGeoModel baked;

        public ScrapRenderer(ScrapGeoModel model, ScrapAnimatable animatable) {
            this.model = model; this.animatable = animatable;
        }

        public void render(PoseStack poseStack, MultiBufferSource buffers, float partialTicks, int light) {
            if (baked == null) baked = model.getBakedModel(model.getModelResource(animatable));
            if (baked == null) return;
            actuallyRender(poseStack, animatable, baked, RENDER_TYPE, buffers, buffers.getBuffer(RENDER_TYPE),
                    false, partialTicks, light, OverlayTexture.NO_OVERLAY, -1);
        }

        @Override @NotNull public GeoModel<ScrapAnimatable> getGeoModel() { return model; }
        @Override @NotNull public ScrapAnimatable getAnimatable() { return animatable; }
        @Override public void updateAnimatedTextureFrame(ScrapAnimatable a) {}
        @Override public void fireCompileRenderLayersEvent() {}
        @Override public boolean firePreRenderEvent(PoseStack ps, BakedGeoModel m, MultiBufferSource b, float pt, int l) { return false; }
        @Override public void firePostRenderEvent(PoseStack ps, BakedGeoModel m, MultiBufferSource b, float pt, int l) {}
    }

    private record ScrapData(ScrapRenderer renderer) {
        ScrapData(ScrapType type) {
            this(new ScrapRenderer(new ScrapGeoModel(type), new ScrapAnimatable()));
        }
    }

    private final ScrapType type;
    private final float initialPitch, initialYaw;
    private float rotation = 0.0F;
    private float rotationO = 0.0F;
    private BlockPos lightPos = BlockPos.ZERO;
    private int lastLX = Integer.MIN_VALUE, lastLY = Integer.MIN_VALUE, lastLZ = Integer.MIN_VALUE;

    public ScrapParticle(ClientLevel level, double x, double y, double z, double xSpeed, double ySpeed, double zSpeed, ScrapType type) {
        super(level, x, y, z, xSpeed, ySpeed, zSpeed);
        this.xd = xSpeed; this.yd = ySpeed; this.zd = zSpeed;
        this.lifetime = 40 + level.random.nextInt(20);
        this.type = type;
        this.initialPitch = level.random.nextFloat() * Mth.TWO_PI;
        this.initialYaw = level.random.nextFloat() * Mth.TWO_PI;
        this.setSize(0.2F, 0.8F);
        this.hasPhysics = true;
    }

    @Override @NotNull public ParticleRenderType getRenderType() { return ParticleRenderType.CUSTOM; }

    @Override
    public void move(double x, double y, double z) {
        if (x == 0.0D && y == 0.0D && z == 0.0D) return;
        if (!this.hasPhysics) {
            setBoundingBox(getBoundingBox().move(x, y, z));
            setLocationFromBoundingbox();
            return;
        }
        AABB box = getBoundingBox();
        Vec3 adjusted = Entity.collideBoundingBox(null, new Vec3(x, y, z), box, this.level, Collections.emptyList());
        setBoundingBox(box.move(adjusted));
        setLocationFromBoundingbox();

        double dist = adjusted.length();
        this.rotation += (float) (dist * 5.0D);

        if (Math.abs(adjusted.y - y) > 1e-5) {
            if (y < 0) this.onGround = (adjusted.y <= 0);
            else this.yd = -this.yd * WALL_BOUNCE;
        } else {
            this.onGround = false;
        }
        if (Math.abs(adjusted.x - x) > 1e-5) this.xd = -this.xd * WALL_BOUNCE;
        if (Math.abs(adjusted.z - z) > 1e-5) this.zd = -this.zd * WALL_BOUNCE;
    }

    @Override
    public void tick() {
        this.xo = this.x; this.yo = this.y; this.zo = this.z;
        this.rotationO = this.rotation;
        if (this.age++ >= this.lifetime) { this.remove(); return; }
        if (!(this.onGround && this.xd == 0.0D && this.zd == 0.0D)) {
            this.yd -= GRAVITY;
            this.move(this.xd, this.yd, this.zd);
            if (this.onGround) {
                if (this.yd < -MIN_BOUNCE_VEL) {
                    this.yd *= -RESTITUTION;
                    this.xd *= 0.6D; this.zd *= 0.6D;
                    this.onGround = false;
                } else {
                    this.yd = 0.0D;
                    this.xd *= GROUND_FRICTION; this.zd *= GROUND_FRICTION;
                    if (Math.abs(this.xd) < STOP_THRESHOLD) this.xd = 0.0D;
                    if (Math.abs(this.zd) < STOP_THRESHOLD) this.zd = 0.0D;
                }
            } else {
                this.xd *= AIR_DRAG; this.yd *= AIR_DRAG; this.zd *= AIR_DRAG;
            }
        }
    }

    @Override protected int getLightColor(float partialTick) {
        int lx = Mth.floor(this.x);
        int ly = Mth.floor(this.y);
        int lz = Mth.floor(this.z);
        if (lx != lastLX || ly != lastLY || lz != lastLZ) {
            this.lightPos = new BlockPos(lx, ly, lz);
            lastLX = lx; lastLY = ly; lastLZ = lz;
        }
        return LevelRenderer.getLightColor(this.level, this.lightPos);
    }

    public void renderCustom(PoseStack poseStack, MultiBufferSource bufferSource, Camera camera, float partialTicks) {
        double x = Mth.lerp(partialTicks, this.xo, this.x) - camera.getPosition().x();
        double y = Mth.lerp(partialTicks, this.yo, this.y) - camera.getPosition().y();
        double z = Mth.lerp(partialTicks, this.zo, this.z) - camera.getPosition().z();

        float currentRoll = Mth.lerp(partialTicks, this.rotationO, this.rotation);

        poseStack.pushPose();
        poseStack.translate(x, y, z);
        poseStack.mulPose(Axis.XP.rotation(initialPitch));
        poseStack.mulPose(Axis.YP.rotation(initialYaw));
        poseStack.mulPose(Axis.ZP.rotation(currentRoll));
        DATA[type.ordinal()].renderer.render(poseStack, bufferSource, partialTicks, getLightColor(partialTicks));
        poseStack.popPose();
    }

    @Override
    public void render(@NotNull VertexConsumer buffer, @NotNull Camera camera, float partialTicks) {
        renderCustom(new PoseStack(), Minecraft.getInstance().renderBuffers().bufferSource(), camera, partialTicks);
    }

    public static class Factory implements ParticleProvider<SimpleParticleType> {
        @Override @Nullable
        public Particle createParticle(@NotNull SimpleParticleType type, @NotNull ClientLevel level, double x, double y, double z, double xS, double yS, double zS) {
            ScrapType[] types = ScrapType.values();
            return new ScrapParticle(level, x, y, z, xS, yS, zS, types[level.getRandom().nextInt(types.length)]);
        }
    }
}