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

public class ScrapParticle extends Particle {

    public enum ScrapType {
        BOLT("scrap_bolt"), GEAR("scrap_gear"), NUT("scrap_nut"),
        SCREW("scrap_screw"), WIRES("scrap_wires");

        public final String name;
        ScrapType(String name) { this.name = name; }
    }

    // ==================== ФИЗИКА (ванильные значения) ====================
    private static final double GRAVITY         = 0.04D;   // ванильная гравитация
    private static final double AIR_DRAG        = 0.98D;   // ванильный drag
    private static final double GROUND_FRICTION = 0.70D;   // ванильное трение XZ на земле
    private static final double RESTITUTION     = 0.40D;   // упругость отскока от пола
    private static final double MIN_BOUNCE_VEL  = 0.08D;   // мин. скорость для отскока
    private static final double WALL_BOUNCE     = 0.30D;   // потеря энергии при ударе о стену/потолок
    private static final double ROLL_AIR_DRAG   = 0.92D;
    private static final double ROLL_GROUND_DRAG= 0.70D;
    private static final double STOP_THRESHOLD  = 0.001D;

    // ==================== РЕНДЕРИНГ ====================
    private static final ScrapAnimatable SHARED_ANIMATABLE = new ScrapAnimatable();
    private static final ResourceLocation TEXTURE =
            ResourceLocation.fromNamespaceAndPath("dristmechanic", "textures/particle/scrap.png");
    private static final RenderType RENDER_TYPE = RenderType.entityCutoutNoCull(TEXTURE);
    private static final ScrapData[] DATA = new ScrapData[ScrapType.values().length];

    static {
        for (ScrapType type : ScrapType.values()) {
            DATA[type.ordinal()] = new ScrapData(type);
        }
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
        private BakedGeoModel baked;
        public ScrapRenderer(ScrapGeoModel model) { this.model = model; }
        public BakedGeoModel getBaked() {
            if (this.baked == null) {
                this.baked = this.model.getBakedModel(this.model.getModelResource(SHARED_ANIMATABLE));
            }
            return this.baked;
        }
        public void render(PoseStack poseStack, MultiBufferSource buffers, float partialTicks, int light) {
            BakedGeoModel model = getBaked();
            if (model == null) return;
            VertexConsumer buffer = buffers.getBuffer(RENDER_TYPE);
            actuallyRender(poseStack, SHARED_ANIMATABLE, model, RENDER_TYPE, buffers, buffer,
                    false, partialTicks, light, OverlayTexture.NO_OVERLAY, -1);
        }
        @Override @NotNull public GeoModel<ScrapAnimatable> getGeoModel() { return model; }
        @Override @NotNull public ScrapAnimatable getAnimatable() { return SHARED_ANIMATABLE; }
        @Override public void updateAnimatedTextureFrame(ScrapAnimatable a) {}
        @Override public void fireCompileRenderLayersEvent() {}
        @Override public boolean firePreRenderEvent(PoseStack ps, BakedGeoModel m, MultiBufferSource b, float pt, int l) { return false; }
        @Override public void firePostRenderEvent(PoseStack ps, BakedGeoModel m, MultiBufferSource b, float pt, int l) {}
    }

    private static class ScrapData {
        final ScrapGeoModel model;
        final ScrapRenderer renderer;
        ScrapData(ScrapType type) {
            this.model = new ScrapGeoModel(type);
            this.renderer = new ScrapRenderer(model);
        }
    }

    // ==================== INSTANCE ====================
    private final ScrapType type;
    private final float initialPitch;
    private final float initialYaw;
    private double rollSpeed = 0.0D;
    private boolean visible = true;
    private int visibilityTick;

    public ScrapParticle(ClientLevel level, double x, double y, double z,
                         double xSpeed, double ySpeed, double zSpeed, ScrapType type) {
        super(level, x, y, z, xSpeed, ySpeed, zSpeed);
        this.xd = xSpeed; this.yd = ySpeed; this.zd = zSpeed;
        this.lifetime = 100 + level.random.nextInt(40);
        this.type = type;
        this.initialPitch = level.random.nextFloat() * Mth.TWO_PI;
        this.initialYaw = level.random.nextFloat() * Mth.TWO_PI;
        this.setSize(0.2F, 0.8F);
        this.hasPhysics = true;
    }

    @Override @NotNull public ParticleRenderType getRenderType() {
        return ParticleRenderType.CUSTOM;
    }

    // ==================== ЛЕГКОВЕСНАЯ КОЛЛИЗИЯ ====================
    /**
     * Проверяет только нужные точки вместо Entity.collideBoundingBox.
     * Стены/потолок работают, onGround выставляется ТОЛЬКО от пола.
     */
    @Override
    public void move(double x, double y, double z) {
        if (!this.hasPhysics) {
            this.setBoundingBox(this.getBoundingBox().move(x, y, z));
            this.setLocationFromBoundingbox();
            return;
        }

        double hw = this.bbWidth / 2.0;

        // --- X axis (стены) ---
        if (x != 0.0) {
            double edgeX = this.x + x + hw * Math.signum(x);
            if (isSolid(edgeX, this.y + 0.1, this.z) || isSolid(edgeX, this.y + this.bbHeight - 0.1, this.z)) {
                this.xd = -this.xd * WALL_BOUNCE;
                x = 0.0;
            } else {
                this.x += x;
            }
        }

        // --- Y axis (пол + потолок) ---
        if (y != 0.0) {
            if (y < 0.0) {
                // Падение — проверяем пол (5 точек по нижней грани хитбокса)
                double nextY = this.y + y;
                if (isSolid(this.x, nextY, this.z) ||
                        isSolid(this.x + hw, nextY, this.z) ||
                        isSolid(this.x - hw, nextY, this.z) ||
                        isSolid(this.x, nextY, this.z + hw) ||
                        isSolid(this.x, nextY, this.z - hw)) {
                    // Приземление: ставим ровно на поверхность блока
                    this.y = Math.floor(nextY) + 1.0;
                    this.onGround = true;
                    y = 0.0;
                } else {
                    this.y = nextY;
                    this.onGround = false;
                }
            } else {
                // Взлёт — проверяем потолок
                double topY = this.y + y + this.bbHeight;
                if (isSolid(this.x, topY, this.z)) {
                    this.yd = -this.yd * WALL_BOUNCE;
                    y = 0.0;
                } else {
                    this.y += y;
                    this.onGround = false;
                }
            }
        }

        // --- Z axis (стены) ---
        if (z != 0.0) {
            double edgeZ = this.z + z + hw * Math.signum(z);
            if (isSolid(this.x, this.y + 0.1, edgeZ) || isSolid(this.x, this.y + this.bbHeight - 0.1, edgeZ)) {
                this.zd = -this.zd * WALL_BOUNCE;
                z = 0.0;
            } else {
                this.z += z;
            }
        }

        // Синхронизируем хитбокс
        this.setBoundingBox(new AABB(
                this.x - hw, this.y, this.z - hw,
                this.x + hw, this.y + this.bbHeight, this.z + hw
        ));
    }

    private boolean isSolid(double x, double y, double z) {
        return this.level.getBlockState(BlockPos.containing(x, y, z)).blocksMotion();
    }

    // ==================== ФИЗИКА ====================
    @Override
    public void tick() {
        this.xo = this.x; this.yo = this.y; this.zo = this.z;
        if (this.age++ >= this.lifetime) {
            this.remove();
            return;
        }

        // Ванильный порядок: гравитация → движение → drag
        this.yd -= GRAVITY;
        this.move(this.xd, this.yd, this.zd);

        // Обработка земли / отскока
        if (this.onGround) {
            if (this.yd < -MIN_BOUNCE_VEL) {
                // Упругий отскок
                this.yd *= -RESTITUTION;
                this.xd *= 0.6D;
                this.zd *= 0.6D;
                this.rollSpeed *= 0.3D;
                this.onGround = false;
            } else {
                // Упокоилось
                this.yd = 0.0D;
                this.xd *= GROUND_FRICTION;
                this.zd *= GROUND_FRICTION;
                this.rollSpeed *= ROLL_GROUND_DRAG;
                stopIfMicro();
            }
        } else {
            // Воздух: drag
            this.xd *= AIR_DRAG;
            this.yd *= AIR_DRAG;
            this.zd *= AIR_DRAG;

            double hSpeed = Math.sqrt(this.xd * this.xd + this.zd * this.zd);
            this.rollSpeed = hSpeed * 6.0D;
            this.rollSpeed *= ROLL_AIR_DRAG;
        }

        // Вращение
        this.oRoll = this.roll;
        this.roll += (float) this.rollSpeed;

        // Видимость
        if (++visibilityTick >= 5) {
            visibilityTick = 0;
            updateVisibility();
        }
    }

    private void stopIfMicro() {
        if (Math.abs(this.xd) < STOP_THRESHOLD) this.xd = 0.0D;
        if (Math.abs(this.zd) < STOP_THRESHOLD) this.zd = 0.0D;
        if (Math.abs(this.rollSpeed) < STOP_THRESHOLD) this.rollSpeed = 0.0D;
    }

    // ==================== ВИДИМОСТЬ ====================
    private void updateVisibility() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.levelRenderer == null || mc.levelRenderer.getFrustum() == null) {
            this.visible = true;
            return;
        }
        if (!mc.levelRenderer.getFrustum().isVisible(getBoundingBox().inflate(0.5))) {
            this.visible = false;
            return;
        }
        Vec3 camPos = mc.gameRenderer.getMainCamera().getPosition();
        if (camPos.distanceToSqr(this.x, this.y, this.z) > 1024.0) {
            this.visible = false;
            return;
        }
        this.visible = true;
    }

    @Override
    protected int getLightColor(float partialTick) {
        return LevelRenderer.getLightColor(this.level, BlockPos.containing(this.x, this.y, this.z));
    }

    // ==================== РЕНДЕРИНГ ====================
    public void renderCustom(PoseStack poseStack, MultiBufferSource bufferSource, Camera camera, float partialTicks) {
        if (!visible) return;
        applyPose(poseStack, camera, partialTicks);
        DATA[type.ordinal()].renderer.render(poseStack, bufferSource, partialTicks, getLightColor(partialTicks));
        poseStack.popPose();
    }

    @Override
    public void render(@NotNull VertexConsumer buffer, @NotNull Camera camera, float partialTicks) {
        if (!visible) return;
        PoseStack poseStack = new PoseStack();
        renderCustom(poseStack, Minecraft.getInstance().renderBuffers().bufferSource(), camera, partialTicks);
    }

    private void applyPose(PoseStack poseStack, Camera camera, float partialTicks) {
        double x = Mth.lerp(partialTicks, this.xo, this.x) - camera.getPosition().x();
        double y = Mth.lerp(partialTicks, this.yo, this.y) - camera.getPosition().y();
        double z = Mth.lerp(partialTicks, this.zo, this.z) - camera.getPosition().z();

        poseStack.pushPose();
        poseStack.translate(x, y, z);
        poseStack.mulPose(Axis.XP.rotation(initialPitch));
        poseStack.mulPose(Axis.YP.rotation(initialYaw));
        poseStack.mulPose(Axis.ZP.rotation(Mth.lerp(partialTicks, this.oRoll, this.roll)));
    }

    public static class Factory implements ParticleProvider<SimpleParticleType> {
        @Override @Nullable
        public Particle createParticle(@NotNull SimpleParticleType type, @NotNull ClientLevel level,
                                       double x, double y, double z,
                                       double xSpeed, double ySpeed, double zSpeed) {
            ScrapType[] types = ScrapType.values();
            return new ScrapParticle(level, x, y, z, xSpeed, ySpeed, zSpeed,
                    types[level.getRandom().nextInt(types.length)]);
        }
    }
}