package com.dristmechanic.dristmechanic.client;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.client.particle.SpriteSet;
import net.minecraft.client.particle.TextureSheetParticle;

public class FlashBigParticle extends TextureSheetParticle {
    private static final double MAX_RENDER_DISTANCE_SQ = 400.0D; // 20 блоков

    public FlashBigParticle(ClientLevel level, double x, double y, double z, double xd, double yd, double zd, SpriteSet spriteSet) {
        super(level, x, y, z, xd, yd, zd);
        this.pickSprite(spriteSet);
        this.lifetime = 8;
        this.gravity = 0;
        this.hasPhysics = false;
        this.scale(0.1F);
    }

    @Override
    public float getQuadSize(float partialTick) {
        float progress = (this.age + partialTick) / (float) this.lifetime;
        float scale = progress < 0.5f ? progress * 2.0f : (1.0f - progress) * 2.0f;
        return scale * 1.5F;
    }

    @Override
    public void tick() {
        super.tick();
        float progress = this.age / (float) this.lifetime;
        this.alpha = progress < 0.5f ? progress * 2.0f : (1.0f - progress) * 2.0f;
    }

    @Override
    protected int getLightColor(float partialTick) {
        return 0xF000F0;
    }

    @Override
    public ParticleRenderType getRenderType() {
        return ParticleRenderType.PARTICLE_SHEET_TRANSLUCENT;
    }
}