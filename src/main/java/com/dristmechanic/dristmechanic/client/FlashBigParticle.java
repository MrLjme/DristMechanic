package com.dristmechanic.dristmechanic.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.client.particle.SpriteSet;
import net.minecraft.client.particle.TextureSheetParticle;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.resources.ResourceLocation;

public class FlashBigParticle extends TextureSheetParticle {
    public FlashBigParticle(ClientLevel level, double x, double y, double z, double xd, double yd, double zd, SpriteSet spriteSet) {
        super(level, x, y, z, xd, yd, zd);
        this.pickSprite(spriteSet); // ОБЯЗАТЕЛЬНО: подхватываем текстуру из JSON
        this.lifetime = 8;
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

    // В 1.21.1 метод называется getQuadSize, а не getScale
    @Override
    public float getQuadSize(float partialTick) {
        float progress = (this.age + partialTick) / (float) this.lifetime;
        // Треугольная волна: увеличение до 0.5, затем уменьшение
        float scale = progress < 0.5f ? progress * 2.0f : (1.0f - progress) * 2.0f;
        return scale * 1.5F;
    }

    @Override
    public void tick() {
        super.tick();
        // Прозрачность тоже меняется по той же логике
        float progress = (this.age) / (float) this.lifetime;
        this.alpha = progress < 0.5f ? progress * 2.0f : (1.0f - progress) * 2.0f;
    }

    @Override
    protected int getLightColor(float partialTick) {
        return 0xF000F0; // Полная яркость
    }

    @Override
    public ParticleRenderType getRenderType() {
        return ParticleRenderType.PARTICLE_SHEET_TRANSLUCENT;
    }
}