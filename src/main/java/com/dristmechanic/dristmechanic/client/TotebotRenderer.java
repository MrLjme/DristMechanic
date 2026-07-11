package com.dristmechanic.dristmechanic.client;

import com.dristmechanic.dristmechanic.Dristmechanic;
import com.dristmechanic.dristmechanic.entity.TotebotEntity;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.GeoModel;
import software.bernie.geckolib.renderer.GeoEntityRenderer;

public class TotebotRenderer extends GeoEntityRenderer<TotebotEntity> {
    public TotebotRenderer(net.minecraft.client.renderer.entity.EntityRendererProvider.Context renderManager) {
        super(renderManager, new GeoModel<>() {
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

    @Override
    protected float getDeathMaxRotation(TotebotEntity entity) {
        return 0.0F;
    }

    @Override
    public int getPackedOverlay(TotebotEntity animatable, float u, float partialTick) {
        return OverlayTexture.NO_OVERLAY;
    }
}