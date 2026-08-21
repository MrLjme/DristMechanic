package com.dristmechanic.dristmechanic.client;

import com.dristmechanic.dristmechanic.Dristmechanic;
import com.dristmechanic.dristmechanic.client.model.ChemicalProjectile;
import com.dristmechanic.dristmechanic.entity.ChemicalProjectileEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;

public class ChemicalProjectileRenderer extends EntityRenderer<ChemicalProjectileEntity> {

    private static final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath(Dristmechanic.MODID, "textures/entity/chemical_projectile.png");

    private final ChemicalProjectile<ChemicalProjectileEntity> model;

    public ChemicalProjectileRenderer(EntityRendererProvider.Context context) {
        super(context);
        this.model = new ChemicalProjectile<>(context.bakeLayer(ChemicalProjectile.LAYER_LOCATION));
    }

    @Override
    public ResourceLocation getTextureLocation(ChemicalProjectileEntity entity) {
        return TEXTURE;
    }

    @Override
    public void render(ChemicalProjectileEntity entity, float entityYaw, float partialTicks, PoseStack poseStack, MultiBufferSource buffer, int packedLight) {
        poseStack.pushPose();

        float rot = entity.tickCount + partialTicks;
        poseStack.mulPose(Axis.YP.rotationDegrees(rot * 15.0F));
        poseStack.mulPose(Axis.XP.rotationDegrees(rot * 8.0F));

        this.model.renderToBuffer(poseStack, buffer.getBuffer(RenderType.entityTranslucent(TEXTURE)), packedLight, OverlayTexture.NO_OVERLAY, 0xFFFFFFFF);

        poseStack.popPose();
        super.render(entity, entityYaw, partialTicks, poseStack, buffer, packedLight);
    }
}