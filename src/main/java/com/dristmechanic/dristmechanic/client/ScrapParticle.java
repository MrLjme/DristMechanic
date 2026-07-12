package com.dristmechanic.dristmechanic.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.client.particle.TextureSheetParticle;
import net.minecraft.client.particle.SpriteSet;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.resources.ResourceLocation;

public class ScrapParticle extends TextureSheetParticle {

    // Объявляем свое поле для скорости вращения, так как в ванильном Particle его нет
    private float angularVelocity;

    public ScrapParticle(ClientLevel level, double x, double y, double z, double xd, double yd, double zd, SpriteSet spriteSet) {
        super(level, x, y, z, xd, yd, zd);
        this.lifetime = 40 + this.random.nextInt(20);
        this.gravity = 1.0F;
        this.hasPhysics = true;
        this.friction = 0.8F;
        float baseScale = 1.0F + this.random.nextFloat() * 1.6F;
        this.scale(baseScale);

        // Инициализируем нашу скорость вращения
        this.angularVelocity = (this.random.nextFloat() - 0.5F) * 0.4F;

        // Добавляем небольшой рандом в скорость движения
        this.xd *= 1.8F + this.random.nextFloat() * 0.4F;
        this.yd *= 1.8F + this.random.nextFloat() * 0.4F;
        this.zd *= 1.8F + this.random.nextFloat() * 0.4F;

        this.pickSprite(spriteSet);

        // ЗАЩИТА ОТ КРАША: Если текстура не найдена, используем ванильный фолбэк
        if (this.sprite == null) {
            TextureAtlas atlas = Minecraft.getInstance().getModelManager().getAtlas(TextureAtlas.LOCATION_PARTICLES);
            this.setSprite(atlas.getSprite(ResourceLocation.withDefaultNamespace("generic_0")));
            System.err.println("[DristMechanic] ОШИБКА РЕСУРСОВ: Текстура для частицы не найдена! Проверьте папки particles и textures/particle в resources.");
        }
    }

    @Override
    public void tick() {
        super.tick();
        // Вращение для эффекта падающих деталей
        this.oRoll = this.roll;
        this.roll += this.angularVelocity;
    }

    @Override
    public ParticleRenderType getRenderType() {
        return ParticleRenderType.PARTICLE_SHEET_OPAQUE;
    }
}