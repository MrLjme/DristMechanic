package com.dristmechanic.dristmechanic.client;

import com.dristmechanic.dristmechanic.Dristmechanic;
import com.dristmechanic.dristmechanic.entity.FarmbotEntity;
import com.dristmechanic.dristmechanic.client.model.Modelfarmbot;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;

public class FarmbotRenderer extends MobRenderer<FarmbotEntity, Modelfarmbot<FarmbotEntity>> {

    public FarmbotRenderer(EntityRendererProvider.Context context) {
        super(context, new Modelfarmbot<>(context.bakeLayer(Modelfarmbot.LAYER_LOCATION)), 0.45f);
    }

    @Override
    public ResourceLocation getTextureLocation(FarmbotEntity entity) {
        return ResourceLocation.fromNamespaceAndPath(Dristmechanic.MODID, "textures/entity/farmbot.png");
    }
}