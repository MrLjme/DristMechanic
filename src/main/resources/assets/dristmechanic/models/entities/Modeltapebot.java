package net.mcreator.dristmechanic.client.model;

import net.minecraft.world.entity.Entity;
import net.minecraft.util.Mth;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.CubeDeformation;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.EntityModel;

import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.blaze3d.vertex.PoseStack;

// Made with Blockbench 5.1.4
// Exported for Minecraft version 1.17 or later with Mojang mappings
// Paste this class into your mod and generate all required imports
public class Modeltapebot<T extends Entity> extends EntityModel<T> {
	// This layer location should be baked with EntityRendererProvider.Context in
	// the entity renderer and passed into this model's constructor
	public static final ModelLayerLocation LAYER_LOCATION = new ModelLayerLocation(ResourceLocation.fromNamespaceAndPath("dristmechanic", "modeltapebot"), "main");
	public final ModelPart head;
	public final ModelPart body;
	public final ModelPart left_leg;
	public final ModelPart right_leg;
	public final ModelPart left_hand;
	public final ModelPart left_hand_bottom;
	public final ModelPart scissors_bottom;
	public final ModelPart scissors_top;
	public final ModelPart right_hand;
	public final ModelPart right_hand_bottom;

	public Modeltapebot(ModelPart root) {
		this.head = root.getChild("head");
		this.body = root.getChild("body");
		this.left_leg = root.getChild("left_leg");
		this.right_leg = root.getChild("right_leg");
		this.left_hand = root.getChild("left_hand");
		this.left_hand_bottom = this.left_hand.getChild("left_hand_bottom");
		this.scissors_bottom = this.left_hand_bottom.getChild("scissors_bottom");
		this.scissors_top = this.left_hand_bottom.getChild("scissors_top");
		this.right_hand = root.getChild("right_hand");
		this.right_hand_bottom = this.right_hand.getChild("right_hand_bottom");
	}

	public static LayerDefinition createBodyLayer() {
		MeshDefinition meshdefinition = new MeshDefinition();
		PartDefinition partdefinition = meshdefinition.getRoot();
		PartDefinition head = partdefinition.addOrReplaceChild("head",
				CubeListBuilder.create().texOffs(33, 38).addBox(-3.0F, -4.7F, -4.35F, 6.0F, 7.0F, 3.0F, new CubeDeformation(0.0F)).texOffs(24, 11).addBox(-3.0F, 2.3F, -4.35F, 6.0F, 1.0F, 0.0F, new CubeDeformation(0.0F)).texOffs(0, 30)
						.addBox(-3.0F, -4.7F, -1.35F, 6.0F, 5.0F, 5.0F, new CubeDeformation(0.0F)).texOffs(30, 56).addBox(-2.0F, -2.7F, -5.35F, 4.0F, 4.0F, 1.0F, new CubeDeformation(0.0F)).texOffs(0, 43)
						.addBox(-4.0F, -0.7F, 0.15F, 8.0F, 4.0F, 2.0F, new CubeDeformation(0.0F)).texOffs(16, 20).addBox(-0.5F, 2.3F, 0.65F, 1.0F, 3.0F, 1.0F, new CubeDeformation(0.0F)).texOffs(24, 32)
						.addBox(-4.0F, -3.7F, -0.35F, 8.0F, 3.0F, 3.0F, new CubeDeformation(0.0F)).texOffs(52, 57).addBox(-2.0F, -6.2F, 5.15F, 4.0F, 5.0F, 0.0F, new CubeDeformation(0.0F)).texOffs(0, 0)
						.addBox(-2.0F, -6.2F, -2.85F, 4.0F, 8.0F, 8.0F, new CubeDeformation(0.0F)).texOffs(42, 22).addBox(-3.5F, -8.7F, 0.15F, 7.0F, 6.0F, 2.0F, new CubeDeformation(0.0F)),
				PartPose.offset(0.0F, -4.3F, -1.15F));
		PartDefinition body = partdefinition.addOrReplaceChild("body",
				CubeListBuilder.create().texOffs(40, 52).addBox(-1.7292F, -4.3958F, -2.6042F, 3.0F, 3.0F, 3.0F, new CubeDeformation(0.0F)).texOffs(20, 56).addBox(-2.2292F, -3.3958F, 0.8958F, 4.0F, 4.0F, 1.0F, new CubeDeformation(0.0F)).texOffs(37, 1)
						.addBox(1.7708F, -3.3958F, -5.1042F, 1.0F, 4.0F, 7.0F, new CubeDeformation(0.0F)).texOffs(14, 43).addBox(-4.2292F, -4.3958F, -4.6042F, 1.0F, 4.0F, 6.0F, new CubeDeformation(0.0F)).texOffs(16, 0)
						.addBox(-4.2292F, 2.1042F, -0.1042F, 8.0F, 2.0F, 6.0F, new CubeDeformation(0.0F)).texOffs(48, 34).addBox(-2.2292F, -3.3958F, -5.1042F, 4.0F, 4.0F, 2.0F, new CubeDeformation(0.0F)).texOffs(37, 1)
						.addBox(-3.2292F, -3.3958F, -5.1042F, 1.0F, 4.0F, 7.0F, new CubeDeformation(0.0F)).texOffs(18, 24).addBox(-3.2292F, 0.6042F, -4.1042F, 6.0F, 2.0F, 6.0F, new CubeDeformation(0.0F)).texOffs(10, 53)
						.addBox(-1.2292F, 2.6042F, -2.6042F, 2.0F, 3.0F, 3.0F, new CubeDeformation(0.0F)).texOffs(46, 0).addBox(-1.7292F, 5.6042F, -3.1042F, 3.0F, 3.0F, 4.0F, new CubeDeformation(0.0F)).texOffs(59, 48)
						.addBox(-3.2292F, 6.1042F, -2.1042F, 2.0F, 2.0F, 2.0F, new CubeDeformation(0.0F)).texOffs(59, 48).addBox(0.7708F, 6.1042F, -2.1042F, 2.0F, 2.0F, 2.0F, new CubeDeformation(0.0F)).texOffs(8, 59)
						.addBox(-4.7292F, -2.3958F, -2.1042F, 2.0F, 2.0F, 2.0F, new CubeDeformation(0.0F)).texOffs(0, 40).addBox(-6.7292F, -1.8958F, -1.6042F, 2.0F, 1.0F, 1.0F, new CubeDeformation(0.0F)).texOffs(0, 40)
						.addBox(4.2708F, -1.8958F, -1.6042F, 2.0F, 1.0F, 1.0F, new CubeDeformation(0.0F)).texOffs(8, 59).addBox(2.2708F, -2.3958F, -2.1042F, 2.0F, 2.0F, 2.0F, new CubeDeformation(0.0F)).texOffs(20, 12)
						.addBox(-4.2292F, -4.8958F, 1.3958F, 8.0F, 8.0F, 4.0F, new CubeDeformation(0.0F)).texOffs(55, 11).addBox(3.7708F, -2.8958F, 1.8958F, 2.0F, 1.0F, 3.0F, new CubeDeformation(0.0F)).texOffs(52, 52)
						.addBox(-5.7292F, -6.3958F, 2.3958F, 4.0F, 3.0F, 2.0F, new CubeDeformation(0.0F)).texOffs(60, 15).addBox(-1.7292F, -2.3958F, 5.3958F, 3.0F, 3.0F, 1.0F, new CubeDeformation(0.0F)).texOffs(14, 43).mirror()
						.addBox(2.7708F, -4.3958F, -4.6042F, 1.0F, 4.0F, 6.0F, new CubeDeformation(0.0F)).mirror(false).texOffs(0, 12).addBox(3.7708F, -1.8958F, 1.3958F, 0.0F, 5.0F, 4.0F, new CubeDeformation(0.0F)),
				PartPose.offsetAndRotation(0.2292F, 0.3714F, 1.4334F, 0.024F, 0.0F, 0.0F));
		PartDefinition left_leg = partdefinition.addOrReplaceChild("left_leg",
				CubeListBuilder.create().texOffs(53, 7).addBox(-1.5F, 2.9167F, -1.5F, 3.0F, 1.0F, 3.0F, new CubeDeformation(0.0F)).texOffs(36, 24).addBox(-0.5F, -1.0833F, -1.0F, 1.0F, 4.0F, 2.0F, new CubeDeformation(0.0F)).texOffs(57, 27)
						.addBox(0.5F, 3.9167F, -1.5F, 1.0F, 2.0F, 3.0F, new CubeDeformation(0.0F)).texOffs(57, 27).addBox(-1.5F, 3.9167F, -1.5F, 1.0F, 2.0F, 3.0F, new CubeDeformation(0.0F)).texOffs(51, 40)
						.addBox(-2.0F, 3.4167F, -1.0F, 4.0F, 2.0F, 2.0F, new CubeDeformation(0.0F)).texOffs(28, 48).addBox(-1.5F, 6.4167F, -1.5F, 3.0F, 5.0F, 3.0F, new CubeDeformation(0.0F)).texOffs(0, 0)
						.addBox(-1.0F, 11.4167F, -1.0F, 2.0F, 5.0F, 2.0F, new CubeDeformation(0.0F)).texOffs(55, 16).addBox(-0.5F, 4.4167F, -1.5F, 1.0F, 2.0F, 3.0F, new CubeDeformation(0.0F)),
				PartPose.offset(3.5F, 7.5833F, 0.5F));
		PartDefinition right_leg = partdefinition.addOrReplaceChild("right_leg",
				CubeListBuilder.create().texOffs(36, 24).addBox(-0.5F, -1.0833F, -1.0F, 1.0F, 4.0F, 2.0F, new CubeDeformation(0.0F)).texOffs(57, 27).addBox(0.5F, 3.9167F, -1.5F, 1.0F, 2.0F, 3.0F, new CubeDeformation(0.0F)).texOffs(57, 27)
						.addBox(-1.5F, 3.9167F, -1.5F, 1.0F, 2.0F, 3.0F, new CubeDeformation(0.0F)).texOffs(51, 40).addBox(-2.0F, 3.4167F, -1.0F, 4.0F, 2.0F, 2.0F, new CubeDeformation(0.0F)).texOffs(53, 7)
						.addBox(-1.5F, 2.9167F, -1.5F, 3.0F, 1.0F, 3.0F, new CubeDeformation(0.0F)).texOffs(0, 0).addBox(-1.0F, 11.4167F, -1.0F, 2.0F, 5.0F, 2.0F, new CubeDeformation(0.0F)).texOffs(28, 48)
						.addBox(-1.5F, 6.4167F, -1.5F, 3.0F, 5.0F, 3.0F, new CubeDeformation(0.0F)).texOffs(55, 16).addBox(-0.5F, 4.4167F, -1.5F, 1.0F, 2.0F, 3.0F, new CubeDeformation(0.0F)),
				PartPose.offset(-3.5F, 7.5833F, 0.5F));
		PartDefinition left_hand = partdefinition.addOrReplaceChild("left_hand",
				CubeListBuilder.create().texOffs(47, 44).addBox(-0.05F, -2.05F, -2.0F, 2.0F, 4.0F, 4.0F, new CubeDeformation(0.0F)).texOffs(22, 43).addBox(-0.05F, 1.95F, -1.5F, 2.0F, 2.0F, 3.0F, new CubeDeformation(0.0F)).texOffs(16, 0)
						.addBox(0.45F, 3.95F, -1.0F, 1.0F, 4.0F, 2.0F, new CubeDeformation(0.0F)).texOffs(62, 4).addBox(0.2F, -1.55F, -1.5F, 2.0F, 3.0F, 3.0F, new CubeDeformation(0.0F)),
				PartPose.offsetAndRotation(6.05F, -0.95F, 0.5F, -0.3927F, 0.0F, 0.0F));
		PartDefinition left_hand_bottom = left_hand.addOrReplaceChild("left_hand_bottom", CubeListBuilder.create().texOffs(0, 58).addBox(-1.0F, -1.0F, -1.5F, 2.0F, 2.0F, 2.0F, new CubeDeformation(0.0F)),
				PartPose.offsetAndRotation(0.95F, 7.45F, 0.0F, -0.3927F, 0.0F, 3.1416F));
		PartDefinition scissors_r1 = left_hand_bottom.addOrReplaceChild("scissors_r1",
				CubeListBuilder.create().texOffs(44, 12).addBox(-2.0F, -6.0803F, -6.518F, 4.0F, 4.0F, 3.0F, new CubeDeformation(0.0F)).texOffs(66, 0).addBox(-2.5F, -5.0803F, -6.018F, 5.0F, 2.0F, 2.0F, new CubeDeformation(0.0F)).texOffs(38, 1)
						.addBox(-0.5F, -5.0803F, -3.518F, 1.0F, 2.0F, 2.0F, new CubeDeformation(0.0F)).texOffs(40, 58).addBox(-1.5F, -1.0803F, -3.018F, 3.0F, 3.0F, 1.0F, new CubeDeformation(0.0F)).texOffs(56, 0)
						.addBox(-1.5F, -5.0803F, -4.018F, 3.0F, 2.0F, 2.0F, new CubeDeformation(0.0F)).texOffs(0, 49).addBox(-1.5F, -5.0803F, -2.018F, 3.0F, 7.0F, 2.0F, new CubeDeformation(0.0F)),
				PartPose.offsetAndRotation(0.0F, 0.0803F, -1.482F, 0.0F, 0.0F, -3.1416F));
		PartDefinition scissors_bottom = left_hand_bottom.addOrReplaceChild("scissors_bottom", CubeListBuilder.create(), PartPose.offsetAndRotation(0.0F, 3.3745F, -6.3488F, 0.3927F, 0.0F, 0.0F));
		PartDefinition scissors_bottom_r1 = scissors_bottom.addOrReplaceChild("scissors_bottom_r1",
				CubeListBuilder.create().texOffs(13, 32).addBox(-0.5F, -5.4948F, -12.1426F, 1.0F, 2.0F, 9.0F, new CubeDeformation(0.0F)).texOffs(24, 3).addBox(0.0F, -5.9948F, -12.1426F, 0.0F, 1.0F, 5.0F, new CubeDeformation(0.0F)),
				PartPose.offsetAndRotation(0.0F, -1.1818F, 5.7891F, 0.7854F, 0.0F, -3.1416F));
		PartDefinition scissors_top = left_hand_bottom.addOrReplaceChild("scissors_top", CubeListBuilder.create(), PartPose.offsetAndRotation(0.0F, 4.9223F, -6.3488F, -0.3927F, 0.0F, 0.0F));
		PartDefinition scissors_top_r1 = scissors_top.addOrReplaceChild("scissors_top_r1",
				CubeListBuilder.create().texOffs(13, 32).addBox(-0.5F, -4.0569F, -15.2041F, 1.0F, 2.0F, 9.0F, new CubeDeformation(0.0F)).texOffs(24, 3).addBox(0.0F, -2.5569F, -15.2041F, 0.0F, 1.0F, 5.0F, new CubeDeformation(0.0F)),
				PartPose.offsetAndRotation(0.0F, -6.3229F, 2.614F, -0.7854F, 0.0F, 3.1416F));
		PartDefinition right_hand = partdefinition.addOrReplaceChild("right_hand",
				CubeListBuilder.create().texOffs(47, 44).addBox(-2.0F, -2.0F, -2.0F, 2.0F, 4.0F, 4.0F, new CubeDeformation(0.0F)).texOffs(22, 43).addBox(-2.0F, 2.0F, -1.5F, 2.0F, 2.0F, 3.0F, new CubeDeformation(0.0F)).texOffs(16, 0)
						.addBox(-1.5F, 4.0F, -1.0F, 1.0F, 4.0F, 2.0F, new CubeDeformation(0.0F)).texOffs(62, 4).addBox(-2.25F, -1.5F, -1.5F, 2.0F, 3.0F, 3.0F, new CubeDeformation(0.0F)),
				PartPose.offset(-6.0F, -1.0F, 0.5F));
		PartDefinition right_hand_bottom = right_hand.addOrReplaceChild("right_hand_bottom",
				CubeListBuilder.create().texOffs(0, 58).addBox(-1.0F, -1.0F, -1.5F, 2.0F, 2.0F, 2.0F, new CubeDeformation(0.0F)).texOffs(0, 16).addBox(-2.0F, -3.0F, -9.5F, 4.0F, 6.0F, 8.0F, new CubeDeformation(0.0F)).texOffs(24, 38)
						.addBox(-2.0F, -2.0F, -10.5F, 4.0F, 1.0F, 1.0F, new CubeDeformation(0.0F)).texOffs(37, 0).addBox(-2.0F, -1.5F, -11.5F, 4.0F, 0.0F, 1.0F, new CubeDeformation(0.0F)).texOffs(24, 9)
						.addBox(-2.5F, 1.0F, -8.5F, 5.0F, 1.0F, 1.0F, new CubeDeformation(0.0F)),
				PartPose.offset(-1.0F, 7.5F, 0.0F));
		PartDefinition bone9_r1 = right_hand_bottom.addOrReplaceChild("bone9_r1",
				CubeListBuilder.create().texOffs(0, 21).addBox(-2.5F, -1.25F, -0.5F, 3.0F, 1.0F, 1.0F, new CubeDeformation(0.0F)).texOffs(16, 16).addBox(0.5F, -0.75F, -0.5F, 1.0F, 3.0F, 1.0F, new CubeDeformation(0.0F)),
				PartPose.offsetAndRotation(1.5F, -3.25F, -9.75F, -0.2182F, 0.0F, 0.0F));
		PartDefinition bone8_r1 = right_hand_bottom.addOrReplaceChild("bone8_r1",
				CubeListBuilder.create().texOffs(55, 44).addBox(-1.5F, -1.0F, -1.5F, 3.0F, 2.0F, 2.0F, new CubeDeformation(0.0F)).texOffs(43, 30).addBox(-2.5F, -0.5F, -1.0F, 5.0F, 1.0F, 3.0F, new CubeDeformation(0.0F)),
				PartPose.offsetAndRotation(0.0F, 2.5F, -9.75F, 0.5236F, 0.0F, 0.0F));
		return LayerDefinition.create(meshdefinition, 128, 128);
	}

	@Override
	public void renderToBuffer(PoseStack poseStack, VertexConsumer vertexConsumer, int packedLight, int packedOverlay, int rgb) {
		head.render(poseStack, vertexConsumer, packedLight, packedOverlay, rgb);
		body.render(poseStack, vertexConsumer, packedLight, packedOverlay, rgb);
		left_leg.render(poseStack, vertexConsumer, packedLight, packedOverlay, rgb);
		right_leg.render(poseStack, vertexConsumer, packedLight, packedOverlay, rgb);
		left_hand.render(poseStack, vertexConsumer, packedLight, packedOverlay, rgb);
		right_hand.render(poseStack, vertexConsumer, packedLight, packedOverlay, rgb);
	}

	public void setupAnim(T entity, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch) {
		this.head.yRot = netHeadYaw / (180F / (float) Math.PI);
		this.head.xRot = headPitch / (180F / (float) Math.PI);
		this.left_leg.xRot = Mth.cos(limbSwing * 1.0F) * -1.0F * limbSwingAmount;
		this.right_leg.xRot = Mth.cos(limbSwing * 1.0F) * 1.0F * limbSwingAmount;
		this.left_hand_bottom.xRot = Mth.cos(limbSwing * 0.6662F) * limbSwingAmount;
		this.left_hand.xRot = Mth.cos(limbSwing * 0.6662F) * limbSwingAmount;
	}
}