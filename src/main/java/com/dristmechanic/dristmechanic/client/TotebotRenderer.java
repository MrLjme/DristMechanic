package com.dristmechanic.dristmechanic.client;

import com.dristmechanic.dristmechanic.Dristmechanic;
import com.dristmechanic.dristmechanic.entity.TotebotEntity;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.DefaultedEntityGeoModel;
import software.bernie.geckolib.renderer.GeoEntityRenderer;

public class TotebotRenderer extends GeoEntityRenderer<TotebotEntity> {
    public TotebotRenderer(net.minecraft.client.renderer.entity.EntityRendererProvider.Context renderManager) {
        super(renderManager, new DefaultedEntityGeoModel<>(
                ResourceLocation.fromNamespaceAndPath(Dristmechanic.MODID, "totebot"), "headLook") {
            @Override
            public ResourceLocation getModelResource(TotebotEntity object) {
                return ResourceLocation.fromNamespaceAndPath(Dristmechanic.MODID, "geo/totebot.geo.json");
            }
            @Override
            public ResourceLocation getTextureResource(TotebotEntity object) {
                return ResourceLocation.fromNamespaceAndPath(Dristmechanic.MODID, "textures/entity/totebot.png");
            }
            @Override
            public ResourceLocation getAnimationResource(TotebotEntity animatable) {
                return ResourceLocation.fromNamespaceAndPath(Dristmechanic.MODID, "animations/totebot.animation.json");
            }
        });
    }
}