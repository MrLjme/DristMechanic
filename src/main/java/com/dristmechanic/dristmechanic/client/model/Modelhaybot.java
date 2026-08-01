package com.dristmechanic.dristmechanic.client.model;
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

public class Modelhaybot<T extends net.minecraft.world.entity.Entity> extends EntityModel<T> {
	public static final ModelLayerLocation LAYER_LOCATION = new ModelLayerLocation(ResourceLocation.fromNamespaceAndPath("dristmechanic", "modelhaybot"), "main");
	public final ModelPart head;
	public final ModelPart bone4;
	public final ModelPart bone3;
	public final ModelPart body;
	public final ModelPart body_haybot;
	public final ModelPart bone;
	public final ModelPart bone2;
	public final ModelPart left_leg_upper;
	public final ModelPart left_leg_bottom;
	public final ModelPart middle_leg;
	public final ModelPart middle_leg_bottom;
	public final ModelPart right_leg;
	public final ModelPart right_arm_rake;

	public Modelhaybot(ModelPart root) {
		this.head = root.getChild("head");
		this.bone4 = this.head.getChild("bone4");
		this.bone3 = this.head.getChild("bone3");
		this.body = root.getChild("body");
		this.body_haybot = this.body.getChild("body_haybot");
		this.bone = this.body_haybot.getChild("bone");
		this.bone2 = this.bone.getChild("bone2");
		this.left_leg_upper = root.getChild("left_leg_upper");
		this.left_leg_bottom = this.left_leg_upper.getChild("left_leg_bottom");
		this.middle_leg = root.getChild("middle_leg");
		this.middle_leg_bottom = this.middle_leg.getChild("middle_leg_bottom");
		this.right_leg = root.getChild("right_leg");
		this.right_arm_rake = this.right_leg.getChild("right_arm_rake");
	}

	public static LayerDefinition createBodyLayer() {
		MeshDefinition meshdefinition = new MeshDefinition();
		PartDefinition partdefinition = meshdefinition.getRoot();
		PartDefinition head = partdefinition.addOrReplaceChild("head",
				CubeListBuilder.create().texOffs(0, 0).addBox(-5.0F, -3.5597F, -3.972F, 10.0F, 5.0F, 4.0F, new CubeDeformation(0.0F)).texOffs(52, 12).addBox(-4.0F, -1.5597F, -4.972F, 2.0F, 2.0F, 1.0F, new CubeDeformation(0.0F)).texOffs(52, 12)
						.addBox(2.0F, -1.5597F, -4.972F, 2.0F, 2.0F, 1.0F, new CubeDeformation(0.0F)).texOffs(31, 8).addBox(-4.5F, -2.0597F, -4.472F, 3.0F, 3.0F, 1.0F, new CubeDeformation(0.0F)).texOffs(31, 8)
						.addBox(1.5F, -2.0597F, -4.472F, 3.0F, 3.0F, 1.0F, new CubeDeformation(0.0F)),
				PartPose.offset(1.0F, 0.5597F, -2.028F));
		PartDefinition bone4_r1 = head.addOrReplaceChild("bone4_r1", CubeListBuilder.create().texOffs(0, 0).addBox(0.0F, -1.5F, -0.5F, 0.0F, 3.0F, 1.0F, new CubeDeformation(0.0F)),
				PartPose.offsetAndRotation(0.5F, 1.9403F, -4.472F, 0.0F, -0.3927F, 0.0F));
		PartDefinition bone3_r1 = head.addOrReplaceChild("bone3_r1", CubeListBuilder.create().texOffs(0, 0).addBox(0.0F, -1.5F, -0.5F, 0.0F, 3.0F, 1.0F, new CubeDeformation(0.0F)),
				PartPose.offsetAndRotation(-0.5F, 1.9403F, -4.472F, 0.0F, 0.3927F, 0.0F));
		PartDefinition head_sub_1_r1 = head.addOrReplaceChild("head_sub_1_r1", CubeListBuilder.create().texOffs(19, 55).mirror().addBox(-6.5F, -2.0F, 0.0F, 13.0F, 4.0F, 0.0F, new CubeDeformation(0.0F)).mirror(false),
				PartPose.offsetAndRotation(0.0F, -3.9189F, -2.6732F, 0.3927F, 0.0F, 0.0F));
		PartDefinition hay_back_r1 = head.addOrReplaceChild("hay_back_r1", CubeListBuilder.create().texOffs(19, 55).addBox(-6.5F, -2.0F, 0.0F, 13.0F, 4.0F, 0.0F, new CubeDeformation(0.0F)),
				PartPose.offsetAndRotation(0.0F, -4.2101F, -1.4952F, -0.2618F, 0.0F, 0.0F));
		PartDefinition bone4 = head.addOrReplaceChild("bone4", CubeListBuilder.create().texOffs(40, 4).addBox(-0.5F, 1.1667F, -2.3333F, 1.0F, 1.0F, 4.0F, new CubeDeformation(0.0F)).texOffs(38, 43)
				.addBox(-0.5F, -3.8333F, 1.6667F, 1.0F, 6.0F, 1.0F, new CubeDeformation(0.0F)).texOffs(22, 42).addBox(-1.0F, -2.8333F, -2.8333F, 2.0F, 4.0F, 2.0F, new CubeDeformation(0.0F)),
				PartPose.offsetAndRotation(-2.0F, 3.7736F, -4.6387F, 0.0F, 0.3927F, 0.0F));
		PartDefinition bone3 = head.addOrReplaceChild("bone3", CubeListBuilder.create().texOffs(22, 42).addBox(-1.0F, -2.8333F, -2.8333F, 2.0F, 4.0F, 2.0F, new CubeDeformation(0.0F)).texOffs(40, 4)
				.addBox(-0.5F, 1.1667F, -2.3333F, 1.0F, 1.0F, 4.0F, new CubeDeformation(0.0F)).texOffs(38, 43).addBox(-0.5F, -3.8333F, 1.6667F, 1.0F, 6.0F, 1.0F, new CubeDeformation(0.0F)),
				PartPose.offsetAndRotation(2.0F, 3.7736F, -4.6387F, 0.0F, -0.3927F, 0.0F));
		PartDefinition body = partdefinition.addOrReplaceChild("body", CubeListBuilder.create(), PartPose.offset(0.0F, 13.0F, 0.0F));
		PartDefinition body_haybot = body.addOrReplaceChild("body_haybot",
				CubeListBuilder.create().texOffs(20, 23).addBox(1.5F, -13.0F, 2.0F, 2.0F, 4.0F, 6.0F, new CubeDeformation(0.0F)).texOffs(18, 15).addBox(-3.5F, -14.0F, 3.0F, 7.0F, 4.0F, 4.0F, new CubeDeformation(0.0F)).texOffs(52, 0)
						.addBox(-3.0F, -16.0F, 4.0F, 3.0F, 2.0F, 3.0F, new CubeDeformation(0.0F)).texOffs(52, 13).addBox(-1.5F, -10.0F, 3.5F, 3.0F, 8.0F, 3.0F, new CubeDeformation(0.0F)).texOffs(48, 52)
						.addBox(-1.5F, -4.4F, 3.5F, 3.0F, 1.0F, 0.0F, new CubeDeformation(0.0F)).texOffs(0, 9).addBox(-3.0F, 0.0F, -4.0F, 6.0F, 5.0F, 5.0F, new CubeDeformation(0.0F)).texOffs(30, 43)
						.addBox(-1.5F, 2.0F, -5.0F, 3.0F, 3.0F, 1.0F, new CubeDeformation(0.0F)).texOffs(7, 46).addBox(0.0F, 6.0F, -4.0F, 0.0F, 6.0F, 6.0F, new CubeDeformation(0.0F)).texOffs(18, 20)
						.addBox(-2.5F, 5.0F, -2.0F, 5.0F, 1.0F, 2.0F, new CubeDeformation(0.0F)).texOffs(33, 37).addBox(-4.6F, 1.0F, -3.5F, 3.0F, 3.0F, 3.0F, new CubeDeformation(0.0F)).texOffs(28, 0)
						.addBox(-2.0F, -0.75F, -1.0F, 4.0F, 4.0F, 4.0F, new CubeDeformation(0.0F)).texOffs(42, 26).addBox(-2.0F, 0.0F, -1.0F, 4.0F, 1.0F, 1.0F, new CubeDeformation(0.0F)).texOffs(33, 37)
						.addBox(1.5F, 1.0F, -3.5F, 3.0F, 3.0F, 3.0F, new CubeDeformation(0.0F)).texOffs(46, 39).addBox(4.5F, 2.0F, -2.5F, 1.0F, 1.0F, 1.0F, new CubeDeformation(0.0F)).texOffs(45, 40)
						.addBox(-5.6F, 2.0F, -2.5F, 2.0F, 1.0F, 1.0F, new CubeDeformation(0.0F)).texOffs(18, 23).addBox(-7.6F, 1.5F, -3.0F, 2.0F, 2.0F, 2.0F, new CubeDeformation(0.0F)),
				PartPose.offset(0.0F, 0.0F, 0.0F));
		PartDefinition back_r1 = body_haybot.addOrReplaceChild("back_r1", CubeListBuilder.create().texOffs(52, 16).addBox(-1.5F, -2.5F, -1.5F, 3.0F, 5.0F, 3.0F, new CubeDeformation(0.0F)),
				PartPose.offsetAndRotation(0.0F, -0.682F, 3.7751F, -0.5672F, 0.0F, 0.0F));
		PartDefinition bone = body_haybot.addOrReplaceChild("bone", CubeListBuilder.create().texOffs(18, 15).addBox(-1.0F, 0.0F, 0.0F, 2.0F, 3.0F, 3.0F, new CubeDeformation(0.0F)).texOffs(18, 15)
				.addBox(-2.5F, 1.0F, -3.0F, 3.0F, 1.0F, 1.0F, new CubeDeformation(0.0F)).texOffs(18, 15).addBox(-2.0F, 0.5F, -3.5F, 1.0F, 2.0F, 6.0F, new CubeDeformation(0.0F)),
				PartPose.offsetAndRotation(-3.0F, -13.25F, 3.5F, 0.2319F, -0.7144F, 0.3253F));
		PartDefinition bone2 = bone.addOrReplaceChild("bone2", CubeListBuilder.create().texOffs(18, 15).addBox(-0.5F, -1.0F, -5.0F, 1.0F, 2.0F, 6.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(-0.5F, 1.5F, -2.5F, -0.6981F, 0.0F, 0.0F));
		PartDefinition left_leg_upper = partdefinition.addOrReplaceChild("left_leg_upper",
				CubeListBuilder.create().texOffs(18, 23).addBox(0.0F, -1.0F, -1.0F, 2.0F, 2.0F, 2.0F, new CubeDeformation(0.0F)).texOffs(35, 40).addBox(4.75F, -0.5F, -0.5F, 3.0F, 1.0F, 1.0F, new CubeDeformation(0.0F)).texOffs(30, 23)
						.addBox(2.75F, -1.5F, -1.5F, 2.0F, 3.0F, 3.0F, new CubeDeformation(0.0F)).texOffs(46, 39).addBox(1.75F, -0.5F, -0.5F, 1.0F, 1.0F, 1.0F, new CubeDeformation(0.0F)).texOffs(38, 13)
						.addBox(7.75F, -0.5F, -2.0F, 2.0F, 2.0F, 4.0F, new CubeDeformation(0.0F)),
				PartPose.offsetAndRotation(5.25F, 15.5F, -2.0F, 0.0361F, 0.3911F, 0.0944F));
		PartDefinition left_leg_bottom = left_leg_upper.addOrReplaceChild("left_leg_bottom",
				CubeListBuilder.create().texOffs(13, 32).addBox(-1.0F, 1.5F, 1.0F, 2.0F, 1.0F, 2.0F, new CubeDeformation(0.0F)).texOffs(52, 0).addBox(-1.5F, 2.5F, 0.5F, 3.0F, 2.0F, 3.0F, new CubeDeformation(0.0F)).texOffs(0, 0)
						.addBox(-0.5F, 4.5F, 1.5F, 1.0F, 3.0F, 1.0F, new CubeDeformation(0.0F)).texOffs(33, 37).addBox(-1.5F, -1.5F, 0.5F, 3.0F, 3.0F, 3.0F, new CubeDeformation(0.0F)),
				PartPose.offsetAndRotation(8.75F, 0.5F, -2.0F, 0.0F, 0.0F, -0.0873F));
		PartDefinition middle_leg = partdefinition.addOrReplaceChild("middle_leg",
				CubeListBuilder.create().texOffs(14, 39).addBox(-0.3714F, -0.6786F, 6.6071F, 1.0F, 1.0F, 4.0F, new CubeDeformation(0.0F)).texOffs(15, 40).addBox(-0.3714F, 1.3214F, 8.6071F, 1.0F, 1.0F, 3.0F, new CubeDeformation(0.0F)).texOffs(30, 23)
						.addBox(-1.3714F, -0.6786F, 3.6071F, 3.0F, 3.0F, 3.0F, new CubeDeformation(0.0F)).texOffs(22, 1).addBox(0.1286F, 1.3214F, 6.6071F, 0.0F, 2.0F, 3.0F, new CubeDeformation(0.0F)).texOffs(18, 23)
						.addBox(-0.8714F, -0.4286F, 1.6071F, 2.0F, 2.0F, 2.0F, new CubeDeformation(0.0F)).texOffs(38, 15).addBox(-1.8714F, 0.3214F, 9.6071F, 4.0F, 2.0F, 2.0F, new CubeDeformation(0.0F)).texOffs(45, 39)
						.addBox(-0.3714F, 0.0714F, -0.3929F, 1.0F, 1.0F, 2.0F, new CubeDeformation(0.0F)),
				PartPose.offsetAndRotation(-1.1286F, 16.1786F, 1.8929F, 0.0944F, -0.3911F, -0.0361F));
		PartDefinition middle_leg_bottom = middle_leg.addOrReplaceChild("middle_leg_bottom",
				CubeListBuilder.create().texOffs(30, 26).addBox(-1.0F, 1.45F, -1.0F, 2.0F, 1.0F, 2.0F, new CubeDeformation(0.0F)).texOffs(52, 0).addBox(-1.5F, 2.45F, -1.5F, 3.0F, 2.0F, 3.0F, new CubeDeformation(0.0F)).texOffs(0, 0)
						.addBox(-0.5F, 4.45F, -0.5F, 1.0F, 3.0F, 1.0F, new CubeDeformation(0.0F)).texOffs(33, 37).addBox(-1.5F, -1.55F, -1.5F, 3.0F, 3.0F, 3.0F, new CubeDeformation(0.0F)),
				PartPose.offsetAndRotation(0.1286F, 1.3714F, 10.6071F, -0.0436F, 0.0F, 0.0F));
		PartDefinition right_leg = partdefinition.addOrReplaceChild("right_leg",
				CubeListBuilder.create().texOffs(30, 23).addBox(-3.0F, -1.5F, -1.5F, 3.0F, 3.0F, 3.0F, new CubeDeformation(0.0F)).texOffs(12, 35).addBox(-12.5F, -1.0F, -1.0F, 10.0F, 2.0F, 2.0F, new CubeDeformation(0.0F)).texOffs(39, 20)
						.addBox(-15.5F, -1.5F, -1.5F, 3.0F, 3.0F, 3.0F, new CubeDeformation(0.0F)).texOffs(38, 13).addBox(-2.5F, -1.0F, -2.0F, 2.0F, 2.0F, 4.0F, new CubeDeformation(0.0F)),
				PartPose.offsetAndRotation(-7.5F, 15.5F, -2.0F, -0.2174F, -0.4802F, 0.4461F));
		PartDefinition right_arm_rake = right_leg.addOrReplaceChild("right_arm_rake",
				CubeListBuilder.create().texOffs(38, 13).addBox(-1.0F, -1.0F, -2.0F, 2.0F, 2.0F, 4.0F, new CubeDeformation(0.0F)).texOffs(38, 14).addBox(-4.5F, -1.0F, -1.5F, 3.0F, 2.0F, 3.0F, new CubeDeformation(0.0F)).texOffs(0, 19)
						.addBox(-4.0F, -16.0F, -1.0F, 2.0F, 19.0F, 2.0F, new CubeDeformation(0.0F)).texOffs(33, 30).addBox(-4.5F, 3.0F, -1.5F, 3.0F, 4.0F, 3.0F, new CubeDeformation(0.0F)).texOffs(8, 19)
						.addBox(-3.5F, 6.0F, -4.0F, 1.0F, 2.0F, 8.0F, new CubeDeformation(0.0F)).texOffs(16, 44).addBox(-3.5F, 8.0F, 2.0F, 1.0F, 2.0F, 2.0F, new CubeDeformation(0.0F)).texOffs(16, 44)
						.addBox(-3.5F, 8.0F, -4.0F, 1.0F, 2.0F, 2.0F, new CubeDeformation(0.0F)).texOffs(46, 13).addBox(-3.5F, 10.0F, -3.5F, 1.0F, 2.0F, 1.0F, new CubeDeformation(0.0F)).texOffs(46, 13)
						.addBox(-3.5F, 10.0F, 2.5F, 1.0F, 2.0F, 1.0F, new CubeDeformation(0.0F)).texOffs(46, 13).addBox(-3.5F, 10.0F, -0.5F, 1.0F, 2.0F, 1.0F, new CubeDeformation(0.0F)).texOffs(16, 44).mirror()
						.addBox(-3.5F, 8.0F, -1.0F, 1.0F, 2.0F, 2.0F, new CubeDeformation(0.0F)).mirror(false).texOffs(0, 18).addBox(-3.0F, 12.0F, 2.5F, 0.0F, 2.0F, 1.0F, new CubeDeformation(0.0F)).texOffs(0, 18)
						.addBox(-3.0F, 12.0F, -0.5F, 0.0F, 2.0F, 1.0F, new CubeDeformation(0.0F)).texOffs(0, 18).addBox(-3.0F, 12.0F, -3.5F, 0.0F, 2.0F, 1.0F, new CubeDeformation(0.0F)),
				PartPose.offsetAndRotation(-14.0F, 0.0F, 0.0F, 0.0F, 0.0F, -0.3491F));
		return LayerDefinition.create(meshdefinition, 64, 64);
	}

	@Override
	public void renderToBuffer(PoseStack poseStack, VertexConsumer vertexConsumer, int packedLight, int packedOverlay, int rgb) {
		head.render(poseStack, vertexConsumer, packedLight, packedOverlay, rgb);
		body.render(poseStack, vertexConsumer, packedLight, packedOverlay, rgb);
		left_leg_upper.render(poseStack, vertexConsumer, packedLight, packedOverlay, rgb);
		middle_leg.render(poseStack, vertexConsumer, packedLight, packedOverlay, rgb);
		right_leg.render(poseStack, vertexConsumer, packedLight, packedOverlay, rgb);
	}

	public void setupAnim(T entity, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch) {
		this.left_leg_bottom.zRot = Mth.cos(limbSwing * 0.6662F + (float) Math.PI) * limbSwingAmount;
		this.middle_leg.yRot = Mth.cos(limbSwing * 1.0F) * 1.0F * limbSwingAmount;
		this.middle_leg_bottom.zRot = Mth.cos(limbSwing * 1.0F) * 1.0F * limbSwingAmount;
		this.head.yRot = netHeadYaw / (180F / (float) Math.PI);
		this.head.xRot = headPitch / (180F / (float) Math.PI);
		this.right_leg.yRot = Mth.cos(limbSwing * 1.0F) * 1.0F * limbSwingAmount;
		this.right_arm_rake.zRot = Mth.cos(limbSwing * 1.0F) * 1.0F * limbSwingAmount;
		this.left_leg_upper.yRot = Mth.cos(limbSwing * 1.0F) * 1.0F * limbSwingAmount;
	}
}