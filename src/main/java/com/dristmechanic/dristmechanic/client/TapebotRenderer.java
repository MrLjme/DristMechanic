package com.dristmechanic.dristmechanic.client;

import com.dristmechanic.dristmechanic.Dristmechanic;
import com.dristmechanic.dristmechanic.entity.TapebotEntity;
import com.dristmechanic.dristmechanic.client.model.Modeltapebot;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.resources.ResourceLocation;

public class TapebotRenderer extends MobRenderer<TapebotEntity, Modeltapebot<TapebotEntity>> {
    public TapebotRenderer(EntityRendererProvider.Context context) {
        super(context, new Modeltapebot<>(context.bakeLayer(Modeltapebot.LAYER_LOCATION)), 0.45f);
    }

    @Override
    public ResourceLocation getTextureLocation(TapebotEntity entity) {
        return ResourceLocation.fromNamespaceAndPath(Dristmechanic.MODID, "textures/entity/tapebot.png");
    }
}