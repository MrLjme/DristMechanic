package com.dristmechanic.dristmechanic.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.client.particle.SpriteSet;
import net.minecraft.client.particle.TextureSheetParticle;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.resources.ResourceLocation;

public class FlashSmallParticle extends TextureSheetParticle {
    public FlashSmallParticle(ClientLevel level, double x, double y, double z, double xd, double yd, double zd, SpriteSet spriteSet) {
        super(level, x, y, z, xd, yd, zd);
        this.pickSprite(spriteSet);

        // Чуть увеличили время жизни, чтобы облако успело отыграть анимацию
        this.lifetime = 8 + this.random.nextInt(6);
        this.gravity = 0;
        this.hasPhysics = false;
        this.scale(0.1F);
        this.friction = 0.95F;

        // Гасим начальный импульс, чтобы они не улетали слишком далеко
        this.xd *= 0.3F;
        this.yd *= 0.3F;
        this.zd *= 0.3F;

        if (this.sprite == null) {
            TextureAtlas atlas = Minecraft.getInstance().getModelManager().getAtlas(TextureAtlas.LOCATION_PARTICLES);
            this.setSprite(atlas.getSprite(ResourceLocation.withDefaultNamespace("generic_0")));
            System.err.println("[DristMechanic] ОШИБКА РЕСУРСОВ: Текстура для частицы не найдена!");
        }
    }

    @Override
    public float getQuadSize(float partialTick) {
        float progress = (this.age + partialTick) / (float) this.lifetime;
        float scale = progress < 0.5f ? progress * 2.0f : (1.0f - progress) * 2.0f;
        return scale * 0.8F;
    }

    @Override
    public void tick() {
        super.tick();

        // ЭФФЕКТ ОБЛАКА: Добавляем легкую турбулентность (частицы "кипят" внутри)
        float turbulence = 0.03F;
        this.xd += (this.random.nextFloat() - 0.5F) * turbulence;
        this.yd += (this.random.nextFloat() - 0.5F) * turbulence;
        this.zd += (this.random.nextFloat() - 0.5F) * turbulence;

        // Сильно замедляем, чтобы они оставались кучными внутри и не разлетались
        this.xd *= 0.85F;
        this.yd *= 0.85F;
        this.zd *= 0.85F;

        float progress = (this.age) / (float) this.lifetime;
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