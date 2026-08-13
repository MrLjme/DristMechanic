package com.dristmechanic.dristmechanic.client;

import com.dristmechanic.dristmechanic.Dristmechanic;
import com.dristmechanic.dristmechanic.entity.HaybotEntity;
import com.dristmechanic.dristmechanic.client.model.Modelhaybot;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.resources.ResourceLocation;

public class HaybotRenderer extends MobRenderer<HaybotEntity, Modelhaybot<HaybotEntity>> {

    public HaybotRenderer(EntityRendererProvider.Context context) {
        super(context, new Modelhaybot<>(context.bakeLayer(Modelhaybot.LAYER_LOCATION)), 0.45f);
    }

    @Override
    public ResourceLocation getTextureLocation(HaybotEntity entity) {
        return ResourceLocation.fromNamespaceAndPath(Dristmechanic.MODID, "textures/entity/haybot.png");
    }
}