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
public class Modelred_tapebot<T extends Entity> extends EntityModel<T> {
	// This layer location should be baked with EntityRendererProvider.Context in
	// the entity renderer and passed into this model's constructor
	public static final ModelLayerLocation LAYER_LOCATION = new ModelLayerLocation(ResourceLocation.fromNamespaceAndPath("dristmechanic", "modelred_tapebot"), "main");
	public final ModelPart head;
	public final ModelPart body;
	public final ModelPart left_arm;
	public final ModelPart left_arm_bottom;
	public final ModelPart right_leg;
	public final ModelPart left_leg;

	public Modelred_tapebot(ModelPart root) {
		this.head = root.getChild("head");
		this.body = root.getChild("body");
		this.left_arm = root.getChild("left_arm");
		this.left_arm_bottom = this.left_arm.getChild("left_arm_bottom");
		this.right_leg = root.getChild("right_leg");
		this.left_leg = root.getChild("left_leg");
	}

	public static LayerDefinition createBodyLayer() {
		MeshDefinition meshdefinition = new MeshDefinition();
		PartDefinition partdefinition = meshdefinition.getRoot();
		PartDefinition head = partdefinition.addOrReplaceChild("head",
				CubeListBuilder.create().texOffs(33, 38).addBox(-3.0F, -6.8746F, -5.681F, 6.0F, 7.0F, 3.0F, new CubeDeformation(0.0F)).texOffs(0, 30).addBox(-3.0F, -6.8746F, -2.681F, 6.0F, 5.0F, 5.0F, new CubeDeformation(0.0F)).texOffs(30, 56)
						.addBox(-2.0F, -4.8746F, -6.681F, 4.0F, 4.0F, 1.0F, new CubeDeformation(0.0F)).texOffs(16, 20).addBox(-0.5F, -0.8746F, -0.681F, 1.0F, 3.0F, 1.0F, new CubeDeformation(0.0F)).texOffs(24, 32)
						.addBox(-4.0F, -5.8746F, -1.681F, 8.0F, 3.0F, 3.0F, new CubeDeformation(0.0F)).texOffs(0, 0).addBox(-2.0F, -8.3746F, -4.181F, 4.0F, 8.0F, 8.0F, new CubeDeformation(0.0F)).texOffs(62, 14)
						.addBox(-3.5F, -10.8746F, -1.181F, 7.0F, 6.0F, 6.0F, new CubeDeformation(0.0F)),
				PartPose.offset(0.0F, -4.1254F, 0.181F));
		PartDefinition rotation2_r1 = head.addOrReplaceChild("rotation2_r1", CubeListBuilder.create().texOffs(52, 57).addBox(-2.0F, -2.5F, 0.0F, 4.0F, 5.0F, 0.0F, new CubeDeformation(0.0F)),
				PartPose.offsetAndRotation(0.0F, -5.8746F, 4.069F, 0.2182F, 0.0F, 0.0F));
		PartDefinition rotation10_r1 = head.addOrReplaceChild("rotation10_r1", CubeListBuilder.create().texOffs(69, 31).addBox(-3.5F, -1.5F, -6.5F, 7.0F, 3.0F, 7.0F, new CubeDeformation(0.0F)),
				PartPose.offsetAndRotation(0.0F, -3.3746F, -1.681F, 0.7854F, 0.0F, 0.0F));
		PartDefinition body = partdefinition.addOrReplaceChild("body",
				CubeListBuilder.create().texOffs(40, 52).addBox(1.2348F, -6.2461F, 0.0188F, 3.0F, 3.0F, 3.0F, new CubeDeformation(0.0F)).texOffs(20, 56).addBox(0.7348F, -5.2461F, 3.5188F, 4.0F, 4.0F, 1.0F, new CubeDeformation(0.0F)).texOffs(37, 1)
						.addBox(4.7348F, -5.2461F, -2.4812F, 1.0F, 4.0F, 7.0F, new CubeDeformation(0.0F)).texOffs(14, 43).addBox(-1.2652F, -6.2461F, -1.9812F, 1.0F, 4.0F, 6.0F, new CubeDeformation(0.0F)).texOffs(16, 0)
						.addBox(-1.2652F, 0.2539F, 2.5188F, 8.0F, 2.0F, 6.0F, new CubeDeformation(0.0F)).texOffs(48, 34).addBox(0.7348F, -5.2461F, -2.4812F, 4.0F, 4.0F, 2.0F, new CubeDeformation(0.0F)).texOffs(37, 1)
						.addBox(-0.2652F, -5.2461F, -2.4812F, 1.0F, 4.0F, 7.0F, new CubeDeformation(0.0F)).texOffs(18, 24).addBox(-0.2652F, -1.2461F, -1.4812F, 6.0F, 2.0F, 6.0F, new CubeDeformation(0.0F)).texOffs(10, 53)
						.addBox(1.7348F, 0.7539F, 0.0188F, 2.0F, 3.0F, 3.0F, new CubeDeformation(0.0F)).texOffs(46, 0).addBox(1.2348F, 3.7539F, -0.4812F, 3.0F, 3.0F, 4.0F, new CubeDeformation(0.0F)).texOffs(59, 48)
						.addBox(-0.2652F, 4.2539F, 0.5188F, 2.0F, 2.0F, 2.0F, new CubeDeformation(0.0F)).texOffs(59, 48).addBox(3.7348F, 4.2539F, 0.5188F, 2.0F, 2.0F, 2.0F, new CubeDeformation(0.0F)).texOffs(8, 59)
						.addBox(-1.7652F, -4.2461F, 0.5188F, 2.0F, 2.0F, 2.0F, new CubeDeformation(0.0F)).texOffs(6, 40).addBox(-3.7652F, -3.7461F, 1.0188F, 2.0F, 1.0F, 1.0F, new CubeDeformation(0.0F)).texOffs(0, 40)
						.addBox(7.2348F, -3.7461F, 1.0188F, 2.0F, 1.0F, 1.0F, new CubeDeformation(0.0F)).texOffs(58, 32).addBox(5.2348F, -4.2461F, 0.5188F, 2.0F, 2.0F, 2.0F, new CubeDeformation(0.0F)).texOffs(20, 12)
						.addBox(-1.2652F, -6.7461F, 4.0188F, 8.0F, 8.0F, 4.0F, new CubeDeformation(0.0F)).texOffs(55, 11).addBox(6.7348F, -4.7461F, 4.5188F, 2.0F, 1.0F, 3.0F, new CubeDeformation(0.0F)).texOffs(52, 52)
						.addBox(-2.7652F, -8.2461F, 5.0188F, 4.0F, 3.0F, 2.0F, new CubeDeformation(0.0F)).texOffs(60, 15).addBox(1.2348F, -4.2461F, 8.0188F, 3.0F, 3.0F, 1.0F, new CubeDeformation(0.0F)).texOffs(47, 44)
						.addBox(-5.2652F, -5.2461F, -0.4812F, 2.0F, 4.0F, 4.0F, new CubeDeformation(0.0F)).texOffs(22, 43).addBox(-5.2652F, -1.2461F, 0.0188F, 2.0F, 2.0F, 3.0F, new CubeDeformation(0.0F)).texOffs(16, 0)
						.addBox(-4.7652F, 0.7539F, 0.5188F, 1.0F, 4.0F, 2.0F, new CubeDeformation(0.0F)).texOffs(62, 4).addBox(-5.5152F, -4.7461F, 0.0188F, 2.0F, 3.0F, 3.0F, new CubeDeformation(0.0F)).texOffs(0, 58)
						.addBox(-5.2652F, 3.2539F, 0.0188F, 2.0F, 2.0F, 2.0F, new CubeDeformation(0.0F)).texOffs(0, 16).addBox(-6.2652F, 1.2539F, -7.9812F, 4.0F, 6.0F, 8.0F, new CubeDeformation(0.0F)).texOffs(24, 38)
						.addBox(-6.2652F, 2.2539F, -8.9812F, 4.0F, 1.0F, 1.0F, new CubeDeformation(0.0F)).texOffs(37, 0).addBox(-6.2652F, 2.7539F, -9.9812F, 4.0F, 0.0F, 1.0F, new CubeDeformation(0.0F)).texOffs(24, 9)
						.addBox(-6.7652F, 5.2539F, -6.9812F, 5.0F, 1.0F, 1.0F, new CubeDeformation(0.0F)),
				PartPose.offset(-2.7348F, 2.2461F, -1.0188F));
		PartDefinition bone8_r1 = body.addOrReplaceChild("bone8_r1",
				CubeListBuilder.create().texOffs(55, 44).addBox(-1.5F, -1.0F, -3.0F, 3.0F, 2.0F, 2.0F, new CubeDeformation(0.0F)).texOffs(43, 30).addBox(-2.5F, -0.5F, -2.5F, 5.0F, 1.0F, 3.0F, new CubeDeformation(0.0F)),
				PartPose.offsetAndRotation(-4.2652F, 6.2539F, -6.7312F, 0.7854F, 0.0F, 0.0F));
		PartDefinition bone9_r1 = body.addOrReplaceChild("bone9_r1",
				CubeListBuilder.create().texOffs(16, 16).addBox(0.5F, -0.75F, -0.5F, 1.0F, 3.0F, 1.0F, new CubeDeformation(0.0F)).texOffs(0, 21).addBox(-2.5F, -1.25F, -0.5F, 3.0F, 1.0F, 1.0F, new CubeDeformation(0.0F)),
				PartPose.offsetAndRotation(-2.7652F, 1.0039F, -7.9812F, -0.2618F, 0.0F, 0.0F));
		PartDefinition left_arm = partdefinition.addOrReplaceChild("left_arm",
				CubeListBuilder.create().texOffs(47, 44).addBox(-0.05F, -2.05F, -2.0F, 2.0F, 4.0F, 4.0F, new CubeDeformation(0.0F)).texOffs(22, 43).addBox(-0.05F, 1.95F, -1.5F, 2.0F, 2.0F, 3.0F, new CubeDeformation(0.0F)).texOffs(16, 0)
						.addBox(0.45F, 3.95F, -1.0F, 1.0F, 4.0F, 2.0F, new CubeDeformation(0.0F)).texOffs(62, 4).addBox(0.2F, -1.55F, -1.5F, 2.0F, 3.0F, 3.0F, new CubeDeformation(0.0F)),
				PartPose.offsetAndRotation(6.05F, -0.95F, 0.5F, -0.3927F, 0.0F, 0.0F));
		PartDefinition left_arm_bottom = left_arm.addOrReplaceChild("left_arm_bottom", CubeListBuilder.create().texOffs(0, 58).addBox(-1.0F, -1.0F, -1.5F, 2.0F, 2.0F, 2.0F, new CubeDeformation(0.0F)),
				PartPose.offsetAndRotation(0.95F, 7.45F, 0.0F, -0.3927F, 0.0F, -3.1416F));
		PartDefinition scissors_top_r1 = left_arm_bottom.addOrReplaceChild("scissors_top_r1",
				CubeListBuilder.create().texOffs(13, 32).addBox(-0.5F, -4.1586F, -15.1568F, 1.0F, 2.0F, 9.0F, new CubeDeformation(0.0F)).texOffs(24, 3).addBox(0.0F, -2.6586F, -15.1568F, 0.0F, 1.0F, 5.0F, new CubeDeformation(0.0F)),
				PartPose.offsetAndRotation(0.0F, 0.0037F, -1.3673F, -0.3927F, 0.0F, -3.1416F));
		PartDefinition scissors_bottom_r1 = left_arm_bottom.addOrReplaceChild("scissors_bottom_r1",
				CubeListBuilder.create().texOffs(13, 32).addBox(-0.5F, -5.2392F, -12.0925F, 1.0F, 2.0F, 9.0F, new CubeDeformation(0.0F)).texOffs(24, 3).addBox(0.0F, -5.7392F, -12.0925F, 0.0F, 1.0F, 5.0F, new CubeDeformation(0.0F)),
				PartPose.offsetAndRotation(0.0F, 0.0037F, -1.3673F, 0.3927F, 0.0F, -3.1416F));
		PartDefinition scissors_r1 = left_arm_bottom.addOrReplaceChild("scissors_r1",
				CubeListBuilder.create().texOffs(44, 12).addBox(-2.0F, -6.0037F, -6.6327F, 4.0F, 4.0F, 3.0F, new CubeDeformation(0.0F)).texOffs(66, 0).addBox(-2.5F, -5.0037F, -6.1327F, 5.0F, 2.0F, 2.0F, new CubeDeformation(0.0F)).texOffs(38, 1)
						.addBox(-0.5F, -5.0037F, -3.6327F, 1.0F, 2.0F, 2.0F, new CubeDeformation(0.0F)).texOffs(40, 58).addBox(-1.5F, -1.0037F, -3.1327F, 3.0F, 3.0F, 1.0F, new CubeDeformation(0.0F)).texOffs(56, 0)
						.addBox(-1.5F, -5.0037F, -4.1327F, 3.0F, 2.0F, 2.0F, new CubeDeformation(0.0F)).texOffs(0, 49).addBox(-1.5F, -5.0037F, -2.1327F, 3.0F, 7.0F, 2.0F, new CubeDeformation(0.0F)),
				PartPose.offsetAndRotation(0.0F, 0.0037F, -1.3673F, 0.0F, 0.0F, -3.1416F));
		PartDefinition right_leg = partdefinition.addOrReplaceChild("right_leg",
				CubeListBuilder.create().texOffs(55, 16).addBox(-0.5F, 4.5625F, -1.5F, 1.0F, 2.0F, 3.0F, new CubeDeformation(0.0F)).texOffs(28, 48).addBox(-1.5F, 6.5625F, -1.5F, 3.0F, 5.0F, 3.0F, new CubeDeformation(0.0F)).texOffs(0, 0)
						.addBox(-1.0F, 11.5625F, -1.0F, 2.0F, 5.0F, 2.0F, new CubeDeformation(0.0F)).texOffs(36, 24).addBox(-0.5F, -0.9375F, -1.0F, 1.0F, 4.0F, 2.0F, new CubeDeformation(0.0F)).texOffs(57, 27)
						.addBox(0.5F, 4.0625F, -1.5F, 1.0F, 2.0F, 3.0F, new CubeDeformation(0.0F)).texOffs(57, 27).addBox(-1.5F, 4.0625F, -1.5F, 1.0F, 2.0F, 3.0F, new CubeDeformation(0.0F)).texOffs(51, 40)
						.addBox(-2.0F, 3.5625F, -1.0F, 4.0F, 2.0F, 2.0F, new CubeDeformation(0.0F)).texOffs(53, 7).addBox(-1.5F, 3.0625F, -1.5F, 3.0F, 1.0F, 3.0F, new CubeDeformation(0.0F)),
				PartPose.offset(-3.5F, 7.4375F, 0.5F));
		PartDefinition left_leg = partdefinition.addOrReplaceChild("left_leg",
				CubeListBuilder.create().texOffs(53, 7).addBox(-1.5F, 3.0625F, -1.5F, 3.0F, 1.0F, 3.0F, new CubeDeformation(0.0F)).texOffs(36, 24).addBox(-0.5F, -0.9375F, -1.0F, 1.0F, 4.0F, 2.0F, new CubeDeformation(0.0F)).texOffs(57, 27)
						.addBox(0.5F, 4.0625F, -1.5F, 1.0F, 2.0F, 3.0F, new CubeDeformation(0.0F)).texOffs(57, 27).addBox(-1.5F, 4.0625F, -1.5F, 1.0F, 2.0F, 3.0F, new CubeDeformation(0.0F)).texOffs(51, 40)
						.addBox(-2.0F, 3.5625F, -1.0F, 4.0F, 2.0F, 2.0F, new CubeDeformation(0.0F)).texOffs(28, 48).addBox(-1.5F, 6.5625F, -1.5F, 3.0F, 5.0F, 3.0F, new CubeDeformation(0.0F)).texOffs(0, 0)
						.addBox(-1.0F, 11.5625F, -1.0F, 2.0F, 5.0F, 2.0F, new CubeDeformation(0.0F)).texOffs(55, 16).addBox(-0.5F, 4.5625F, -1.5F, 1.0F, 2.0F, 3.0F, new CubeDeformation(0.0F)),
				PartPose.offset(3.5F, 7.4375F, 0.5F));
		return LayerDefinition.create(meshdefinition, 128, 128);
	}

	@Override
	public void renderToBuffer(PoseStack poseStack, VertexConsumer vertexConsumer, int packedLight, int packedOverlay, int rgb) {
		head.render(poseStack, vertexConsumer, packedLight, packedOverlay, rgb);
		body.render(poseStack, vertexConsumer, packedLight, packedOverlay, rgb);
		left_arm.render(poseStack, vertexConsumer, packedLight, packedOverlay, rgb);
		right_leg.render(poseStack, vertexConsumer, packedLight, packedOverlay, rgb);
		left_leg.render(poseStack, vertexConsumer, packedLight, packedOverlay, rgb);
	}

	public void setupAnim(T entity, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch) {
		this.head.yRot = netHeadYaw / (180F / (float) Math.PI);
		this.head.xRot = headPitch / (180F / (float) Math.PI);
		this.left_leg.xRot = Mth.cos(limbSwing * 1.0F) * -1.0F * limbSwingAmount;
		this.left_arm.xRot = Mth.cos(limbSwing * 0.6662F) * limbSwingAmount;
		this.right_leg.xRot = Mth.cos(limbSwing * 1.0F) * 1.0F * limbSwingAmount;
		this.left_arm_bottom.xRot = Mth.cos(limbSwing * 0.6662F) * limbSwingAmount;
	}
}