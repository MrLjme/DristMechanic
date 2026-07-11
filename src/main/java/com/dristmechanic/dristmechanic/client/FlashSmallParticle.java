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
        this.pickSprite(spriteSet); // ОБЯЗАТЕЛЬНО: подхватываем текстуру из JSON
        this.lifetime = 6;
        this.gravity = 0;
        this.hasPhysics = false;
        this.scale(0.1F);
        this.pickSprite(spriteSet);

        // ЗАЩИТА ОТ КРАША: Если текстура не найдена, используем ванильный фолбэк
        if (this.sprite == null) {
            TextureAtlas atlas = Minecraft.getInstance().getModelManager().getAtlas(TextureAtlas.LOCATION_PARTICLES);
            this.setSprite(atlas.getSprite(ResourceLocation.withDefaultNamespace("generic_0")));
            System.err.println("[DristMechanic] ОШИБКА РЕСУРСОВ: Текстура для частицы не найдена! Проверьте папки particles и textures/particle в resources.");
        }
    }

    // В 1.21.1 метод называется getQuadSize
    @Override
    public float getQuadSize(float partialTick) {
        float progress = (this.age + partialTick) / (float) this.lifetime;
        float scale = progress < 0.5f ? progress * 2.0f : (1.0f - progress) * 2.0f;
        return scale * 0.8F; // Максимальный размер мелких частиц
    }

    @Override
    public void tick() {
        super.tick();
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