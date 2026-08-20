package com.dristmechanic.dristmechanic.client;

import com.dristmechanic.dristmechanic.Dristmechanic;
import com.dristmechanic.dristmechanic.entity.RedTapebotEntity;
import com.dristmechanic.dristmechanic.client.model.Modelred_tapebot;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.resources.ResourceLocation;

public class RedTapebotRenderer extends MobRenderer<RedTapebotEntity, Modelred_tapebot<RedTapebotEntity>> {

    public RedTapebotRenderer(EntityRendererProvider.Context context) {
        super(context, new Modelred_tapebot<>(context.bakeLayer(Modelred_tapebot.LAYER_LOCATION)), 0.6f);
    }

    @Override
    public ResourceLocation getTextureLocation(RedTapebotEntity entity) {
        return ResourceLocation.fromNamespaceAndPath(Dristmechanic.MODID, "textures/entity/red_tapebot.png");
    }
}