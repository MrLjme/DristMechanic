package com.dristmechanic.dristmechanic.client;

import com.dristmechanic.dristmechanic.Dristmechanic;
import com.dristmechanic.dristmechanic.entity.RedTapeEntity;
import com.dristmechanic.dristmechanic.client.model.Modeltape;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;

public class RedTapeRenderer extends EntityRenderer<RedTapeEntity> {
    private final Modeltape<RedTapeEntity> model;

    public RedTapeRenderer(EntityRendererProvider.Context context) {
        super(context);
        this.model = new Modeltape<>(context.bakeLayer(Modeltape.LAYER_LOCATION));
        this.shadowRadius = 0.25f;
    }

    @Override
    public ResourceLocation getTextureLocation(RedTapeEntity entity) {
        return ResourceLocation.fromNamespaceAndPath(Dristmechanic.MODID, "textures/entity/red_tape.png");
    }

    @Override
    public void render(RedTapeEntity entity, float entityYaw, float partialTicks, PoseStack poseStack, MultiBufferSource bufferSource, int packedLight) {
        poseStack.pushPose();

        poseStack.mulPose(Axis.YP.rotationDegrees(Mth.lerp(partialTicks, entity.yRotO, entity.getYRot()) - 90.0F));
        poseStack.mulPose(Axis.ZP.rotationDegrees(Mth.lerp(partialTicks, entity.xRotO, entity.getXRot()) + 90.0F));

        poseStack.translate(-0.25F, 0.0F, 0.0F);

        VertexConsumer vertexConsumer = bufferSource.getBuffer(this.model.renderType(this.getTextureLocation(entity)));

        float rotationAngle = (entity.tickCount + partialTicks) * 0.8F;
        this.model.setupAnim(entity, 0.0F, 0.0F, rotationAngle, 0.0F, 0.0F);

        this.model.renderToBuffer(poseStack, vertexConsumer, packedLight, OverlayTexture.NO_OVERLAY);

        poseStack.popPose();
        super.render(entity, entityYaw, partialTicks, poseStack, bufferSource, packedLight);
    }
}