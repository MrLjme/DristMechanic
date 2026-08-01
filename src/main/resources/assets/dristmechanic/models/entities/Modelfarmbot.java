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
public class Modelfarmbot<T extends Entity> extends EntityModel<T> {
	// This layer location should be baked with EntityRendererProvider.Context in
	// the entity renderer and passed into this model's constructor
	public static final ModelLayerLocation LAYER_LOCATION = new ModelLayerLocation(ResourceLocation.fromNamespaceAndPath("dristmechanic", "modelfarmbot"), "main");
	public final ModelPart head;
	public final ModelPart body;
	public final ModelPart right_arm;
	public final ModelPart right_arm_bottom;
	public final ModelPart left_arm;
	public final ModelPart right_leg;
	public final ModelPart middle_leg;
	public final ModelPart left_leg;
	public final ModelPart back_right_leg;
	public final ModelPart back_middle_leg;
	public final ModelPart back_left_leg;

	public Modelfarmbot(ModelPart root) {
		this.head = root.getChild("head");
		this.body = root.getChild("body");
		this.right_arm = root.getChild("right_arm");
		this.right_arm_bottom = this.right_arm.getChild("right_arm_bottom");
		this.left_arm = root.getChild("left_arm");
		this.right_leg = root.getChild("right_leg");
		this.middle_leg = root.getChild("middle_leg");
		this.left_leg = root.getChild("left_leg");
		this.back_right_leg = root.getChild("back_right_leg");
		this.back_middle_leg = root.getChild("back_middle_leg");
		this.back_left_leg = root.getChild("back_left_leg");
	}

	public static LayerDefinition createBodyLayer() {
		MeshDefinition meshdefinition = new MeshDefinition();
		PartDefinition partdefinition = meshdefinition.getRoot();
		PartDefinition head = partdefinition.addOrReplaceChild("head",
				CubeListBuilder.create().texOffs(86, 0).addBox(-6.0F, -8.0F, -7.0F, 12.0F, 12.0F, 6.0F, new CubeDeformation(0.0F)).texOffs(134, 134).addBox(-4.5F, -1.5F, -7.5F, 9.0F, 4.0F, 1.0F, new CubeDeformation(0.0F)).texOffs(92, 62)
						.addBox(-1.5F, -1.0F, -1.0F, 3.0F, 2.0F, 1.0F, new CubeDeformation(0.0F)).texOffs(40, 104).addBox(0.5F, -6.5F, -8.0F, 4.0F, 4.0F, 1.0F, new CubeDeformation(0.0F)),
				PartPose.offset(0.0F, -15.0F, -8.5F));
		PartDefinition body = partdefinition.addOrReplaceChild("body",
				CubeListBuilder.create().texOffs(0, 0).addBox(-14.0F, -18.45F, -12.025F, 6.0F, 20.0F, 2.0F, new CubeDeformation(0.0F)).texOffs(108, 40).addBox(-6.0F, -21.45F, -11.525F, 12.0F, 4.0F, 1.0F, new CubeDeformation(0.0F)).texOffs(0, 0)
						.addBox(-16.0F, -24.45F, -10.525F, 32.0F, 32.0F, 22.0F, new CubeDeformation(0.0F)).texOffs(0, 0).addBox(8.0F, -18.45F, -12.025F, 6.0F, 20.0F, 2.0F, new CubeDeformation(0.0F)).texOffs(0, 0)
						.addBox(-14.0F, -18.45F, 10.975F, 6.0F, 20.0F, 2.0F, new CubeDeformation(0.0F)).texOffs(0, 0).addBox(8.0F, -18.45F, 10.975F, 6.0F, 20.0F, 2.0F, new CubeDeformation(0.0F)).texOffs(80, 88)
						.addBox(-5.0F, 7.55F, -4.525F, 10.0F, 1.0F, 10.0F, new CubeDeformation(0.0F)).texOffs(104, 99).addBox(-4.0F, 8.05F, -3.525F, 8.0F, 2.0F, 8.0F, new CubeDeformation(0.0F)).texOffs(16, 102)
						.addBox(-2.0F, 10.05F, -1.525F, 4.0F, 2.0F, 4.0F, new CubeDeformation(0.0F)).texOffs(69, 116).addBox(-1.5F, 12.05F, -1.025F, 3.0F, 2.0F, 3.0F, new CubeDeformation(0.0F)).texOffs(86, 18)
						.addBox(-1.0F, 14.05F, -0.525F, 2.0F, 2.0F, 2.0F, new CubeDeformation(0.0F)).texOffs(108, 28).addBox(-20.0F, -22.45F, -2.525F, 6.0F, 6.0F, 6.0F, new CubeDeformation(0.0F)).texOffs(108, 28)
						.addBox(14.0F, -22.45F, -2.525F, 6.0F, 6.0F, 6.0F, new CubeDeformation(0.0F)).texOffs(16, 4).addBox(-0.5F, 16.05F, -0.025F, 1.0F, 2.0F, 1.0F, new CubeDeformation(0.0F)).texOffs(82, 54)
						.addBox(-18.0F, 2.55F, 9.475F, 4.0F, 4.0F, 4.0F, new CubeDeformation(0.0F)).texOffs(82, 54).addBox(14.0F, 2.55F, -12.525F, 4.0F, 4.0F, 4.0F, new CubeDeformation(0.0F)).texOffs(82, 54)
						.addBox(-18.0F, 2.55F, -12.525F, 4.0F, 4.0F, 4.0F, new CubeDeformation(0.0F)).texOffs(82, 54).addBox(14.0F, 2.55F, 9.475F, 4.0F, 4.0F, 4.0F, new CubeDeformation(0.0F)).texOffs(82, 54)
						.addBox(-2.0F, 2.55F, -12.525F, 4.0F, 4.0F, 4.0F, new CubeDeformation(0.0F)).texOffs(82, 54).addBox(-2.0F, 2.55F, 11.475F, 4.0F, 4.0F, 4.0F, new CubeDeformation(0.0F)),
				PartPose.offset(0.0F, 4.45F, 3.025F));
		PartDefinition right_arm = partdefinition.addOrReplaceChild("right_arm",
				CubeListBuilder.create().texOffs(110, 63).addBox(-8.0F, -6.5F, -3.0F, 6.0F, 2.0F, 6.0F, new CubeDeformation(0.0F)).texOffs(52, 54).addBox(-10.0F, -5.0F, -5.0F, 10.0F, 10.0F, 10.0F, new CubeDeformation(0.0F)),
				PartPose.offset(-18.0F, -15.0F, 3.5F));
		PartDefinition right_arm_bottom = right_arm.addOrReplaceChild("right_arm_bottom",
				CubeListBuilder.create().texOffs(24, 104).addBox(-8.0F, -16.05F, -4.0F, 4.0F, 8.0F, 8.0F, new CubeDeformation(0.0F)).texOffs(50, 98).addBox(-4.0F, -16.05F, -4.0F, 8.0F, 10.0F, 8.0F, new CubeDeformation(0.0F)),
				PartPose.offset(-5.0F, 0.05F, 0.0F));
		PartDefinition bone4_r1 = right_arm_bottom.addOrReplaceChild("bone4_r1",
				CubeListBuilder.create().texOffs(116, 0).addBox(-2.0F, -2.0F, -1.0F, 4.0F, 4.0F, 2.0F, new CubeDeformation(0.0F)).texOffs(104, 109).addBox(-28.0F, 17.0F, 0.0F, 13.0F, 9.0F, 0.0F, new CubeDeformation(0.0F)).texOffs(108, 18)
						.addBox(-15.0F, 19.0F, -1.0F, 12.0F, 8.0F, 2.0F, new CubeDeformation(0.0F)).texOffs(12, 166).addBox(-3.0F, 19.0F, 0.0F, 4.0F, 8.0F, 0.0F, new CubeDeformation(0.0F)).texOffs(116, 0)
						.addBox(-2.0F, -2.0F, -3.0F, 4.0F, 4.0F, 2.0F, new CubeDeformation(0.0F)).texOffs(113, 180).addBox(-9.0F, 7.0F, -3.5F, 4.0F, 16.0F, 7.0F, new CubeDeformation(0.0F)).texOffs(113, 151)
						.addBox(-9.0F, -14.0F, -3.5F, 7.0F, 21.0F, 7.0F, new CubeDeformation(0.0F)).texOffs(116, 0).addBox(-2.0F, -2.0F, 1.0F, 4.0F, 4.0F, 2.0F, new CubeDeformation(0.0F)),
				PartPose.offsetAndRotation(-10.0F, -12.05F, 0.0F, 3.1416F, 0.0F, 0.0F));
		PartDefinition left_arm = partdefinition.addOrReplaceChild("left_arm",
				CubeListBuilder.create().texOffs(82, 99).addBox(26.0F, -15.0F, 0.0F, 11.0F, 29.0F, 0.0F, new CubeDeformation(0.0F)).texOffs(0, 73).addBox(24.0F, 11.5F, -1.5F, 1.0F, 3.0F, 3.0F, new CubeDeformation(0.0F)).texOffs(155, 6)
						.addBox(16.0F, -16.5F, -3.5F, 7.0F, 25.0F, 7.0F, new CubeDeformation(0.0F)).texOffs(150, 56).addBox(15.0F, 8.5F, -4.5F, 9.0F, 8.0F, 9.0F, new CubeDeformation(0.0F)).texOffs(58, 74)
						.addBox(23.0F, -9.5F, -4.0F, 8.0F, 16.0F, 8.0F, new CubeDeformation(0.0F)).texOffs(38, 214).addBox(25.0F, -11.0F, -2.0F, 4.0F, 2.0F, 4.0F, new CubeDeformation(0.0F)).texOffs(0, 54)
						.addBox(0.0F, -4.5F, -5.0F, 16.0F, 9.0F, 10.0F, new CubeDeformation(0.0F)).texOffs(0, 61).addBox(26.0F, -12.0F, -1.0F, 2.0F, 1.0F, 2.0F, new CubeDeformation(0.0F)).texOffs(16, 0)
						.addBox(25.0F, 12.0F, -1.0F, 1.0F, 2.0F, 2.0F, new CubeDeformation(0.0F)),
				PartPose.offsetAndRotation(18.0F, -15.0F, 3.5F, 0.3927F, 0.0F, 0.0F));
		PartDefinition right_leg = partdefinition.addOrReplaceChild("right_leg",
				CubeListBuilder.create().texOffs(42, 54).addBox(-3.0F, -2.7F, -2.35F, 6.0F, 6.0F, 3.0F, new CubeDeformation(0.0F)).texOffs(58, 74).addBox(-0.5F, -0.7F, -16.35F, 1.0F, 2.0F, 3.0F, new CubeDeformation(0.0F)).texOffs(110, 81)
						.addBox(-3.0F, -2.7F, -13.35F, 6.0F, 7.0F, 4.0F, new CubeDeformation(0.0F)).texOffs(101, 47).addBox(-3.0F, -4.7F, -9.35F, 6.0F, 9.0F, 7.0F, new CubeDeformation(0.0F)).texOffs(90, 81)
						.addBox(-3.5F, -1.7F, -19.35F, 7.0F, 3.0F, 3.0F, new CubeDeformation(0.0F)).texOffs(60, 116).addBox(0.5F, -2.7F, -20.35F, 2.0F, 6.0F, 5.0F, new CubeDeformation(0.0F)).texOffs(60, 116)
						.addBox(-2.5F, -2.7F, -20.35F, 2.0F, 6.0F, 5.0F, new CubeDeformation(0.0F)).texOffs(51, 74).addBox(-0.5F, 2.3F, -20.35F, 1.0F, 1.0F, 5.0F, new CubeDeformation(0.0F)).texOffs(22, 73)
						.addBox(-1.5F, 3.3F, -19.35F, 3.0F, 3.0F, 3.0F, new CubeDeformation(0.0F)).texOffs(44, 116).addBox(-2.0F, 6.3F, -19.85F, 4.0F, 9.0F, 4.0F, new CubeDeformation(0.0F)),
				PartPose.offsetAndRotation(-16.0F, 8.7F, -8.15F, 0.0F, 0.7854F, 0.0F));
		PartDefinition middle_leg = partdefinition.addOrReplaceChild("middle_leg",
				CubeListBuilder.create().texOffs(42, 54).addBox(-3.0F, -2.7F, -2.35F, 6.0F, 6.0F, 3.0F, new CubeDeformation(0.0F)).texOffs(58, 74).addBox(-0.5F, -0.7F, -16.35F, 1.0F, 2.0F, 3.0F, new CubeDeformation(0.0F)).texOffs(110, 81)
						.addBox(-3.0F, -2.7F, -13.35F, 6.0F, 7.0F, 4.0F, new CubeDeformation(0.0F)).texOffs(101, 47).addBox(-3.0F, -4.7F, -9.35F, 6.0F, 9.0F, 7.0F, new CubeDeformation(0.0F)).texOffs(90, 81)
						.addBox(-3.5F, -1.7F, -19.35F, 7.0F, 3.0F, 3.0F, new CubeDeformation(0.0F)).texOffs(60, 116).addBox(0.5F, -2.7F, -20.35F, 2.0F, 6.0F, 5.0F, new CubeDeformation(0.0F)).texOffs(60, 116)
						.addBox(-2.5F, -2.7F, -20.35F, 2.0F, 6.0F, 5.0F, new CubeDeformation(0.0F)).texOffs(51, 74).addBox(-0.5F, 2.3F, -20.35F, 1.0F, 1.0F, 5.0F, new CubeDeformation(0.0F)).texOffs(22, 73)
						.addBox(-1.5F, 3.3F, -19.35F, 3.0F, 3.0F, 3.0F, new CubeDeformation(0.0F)).texOffs(44, 116).addBox(-2.0F, 6.3F, -19.85F, 4.0F, 9.0F, 4.0F, new CubeDeformation(0.0F)),
				PartPose.offset(0.0F, 8.7F, -10.15F));
		PartDefinition left_leg = partdefinition.addOrReplaceChild("left_leg",
				CubeListBuilder.create().texOffs(42, 54).addBox(-3.0F, -2.7F, -2.35F, 6.0F, 6.0F, 3.0F, new CubeDeformation(0.0F)).texOffs(58, 74).addBox(-0.5F, -0.7F, -16.35F, 1.0F, 2.0F, 3.0F, new CubeDeformation(0.0F)).texOffs(110, 81)
						.addBox(-3.0F, -2.7F, -13.35F, 6.0F, 7.0F, 4.0F, new CubeDeformation(0.0F)).texOffs(101, 47).addBox(-3.0F, -4.7F, -9.35F, 6.0F, 9.0F, 7.0F, new CubeDeformation(0.0F)).texOffs(90, 81)
						.addBox(-3.5F, -1.7F, -19.35F, 7.0F, 3.0F, 3.0F, new CubeDeformation(0.0F)).texOffs(60, 116).addBox(0.5F, -2.7F, -20.35F, 2.0F, 6.0F, 5.0F, new CubeDeformation(0.0F)).texOffs(60, 116)
						.addBox(-2.5F, -2.7F, -20.35F, 2.0F, 6.0F, 5.0F, new CubeDeformation(0.0F)).texOffs(51, 74).addBox(-0.5F, 2.3F, -20.35F, 1.0F, 1.0F, 5.0F, new CubeDeformation(0.0F)).texOffs(22, 73)
						.addBox(-1.5F, 3.3F, -19.35F, 3.0F, 3.0F, 3.0F, new CubeDeformation(0.0F)).texOffs(44, 116).addBox(-2.0F, 6.3F, -19.85F, 4.0F, 9.0F, 4.0F, new CubeDeformation(0.0F)),
				PartPose.offsetAndRotation(16.0F, 8.7F, -8.15F, 0.0F, -0.7854F, 0.0F));
		PartDefinition back_right_leg = partdefinition.addOrReplaceChild("back_right_leg",
				CubeListBuilder.create().texOffs(42, 54).addBox(-3.0F, -2.7F, -0.65F, 6.0F, 6.0F, 3.0F, new CubeDeformation(0.0F)).texOffs(58, 74).addBox(-0.5F, -0.7F, 13.35F, 1.0F, 2.0F, 3.0F, new CubeDeformation(0.0F)).texOffs(172, 147)
						.addBox(-3.0F, -2.7F, 9.35F, 6.0F, 7.0F, 4.0F, new CubeDeformation(0.0F)).texOffs(171, 129).addBox(-3.0F, -4.7F, 2.35F, 6.0F, 9.0F, 7.0F, new CubeDeformation(0.0F)).texOffs(44, 116)
						.addBox(-2.0F, 6.3F, 15.85F, 4.0F, 9.0F, 4.0F, new CubeDeformation(0.0F)).texOffs(90, 81).addBox(-3.5F, -1.7F, 16.35F, 7.0F, 3.0F, 3.0F, new CubeDeformation(0.0F)).texOffs(60, 116)
						.addBox(0.5F, -2.7F, 15.35F, 2.0F, 6.0F, 5.0F, new CubeDeformation(0.0F)).texOffs(60, 116).addBox(-2.5F, -2.7F, 15.35F, 2.0F, 6.0F, 5.0F, new CubeDeformation(0.0F)).texOffs(51, 74)
						.addBox(-0.5F, 2.3F, 15.35F, 1.0F, 1.0F, 5.0F, new CubeDeformation(0.0F)).texOffs(22, 73).addBox(-1.5F, 3.3F, 16.35F, 3.0F, 3.0F, 3.0F, new CubeDeformation(0.0F)),
				PartPose.offsetAndRotation(-16.0F, 8.7F, 15.15F, 0.0F, -0.7854F, 0.0F));
		PartDefinition back_middle_leg = partdefinition.addOrReplaceChild("back_middle_leg",
				CubeListBuilder.create().texOffs(42, 54).addBox(-3.0F, -2.7F, -0.65F, 6.0F, 6.0F, 3.0F, new CubeDeformation(0.0F)).texOffs(58, 74).addBox(-0.5F, -0.7F, 13.35F, 1.0F, 2.0F, 3.0F, new CubeDeformation(0.0F)).texOffs(172, 147)
						.addBox(-3.0F, -2.7F, 9.35F, 6.0F, 7.0F, 4.0F, new CubeDeformation(0.0F)).texOffs(171, 129).addBox(-3.0F, -4.7F, 2.35F, 6.0F, 9.0F, 7.0F, new CubeDeformation(0.0F)).texOffs(44, 116)
						.addBox(-2.0F, 6.3F, 15.85F, 4.0F, 9.0F, 4.0F, new CubeDeformation(0.0F)).texOffs(90, 81).addBox(-3.5F, -1.7F, 16.35F, 7.0F, 3.0F, 3.0F, new CubeDeformation(0.0F)).texOffs(60, 116)
						.addBox(0.5F, -2.7F, 15.35F, 2.0F, 6.0F, 5.0F, new CubeDeformation(0.0F)).texOffs(60, 116).addBox(-2.5F, -2.7F, 15.35F, 2.0F, 6.0F, 5.0F, new CubeDeformation(0.0F)).texOffs(51, 74)
						.addBox(-0.5F, 2.3F, 15.35F, 1.0F, 1.0F, 5.0F, new CubeDeformation(0.0F)).texOffs(22, 73).addBox(-1.5F, 3.3F, 16.35F, 3.0F, 3.0F, 3.0F, new CubeDeformation(0.0F)),
				PartPose.offset(0.0F, 8.7F, 17.15F));
		PartDefinition back_left_leg = partdefinition.addOrReplaceChild("back_left_leg",
				CubeListBuilder.create().texOffs(42, 54).addBox(-3.0F, -2.7F, -0.65F, 6.0F, 6.0F, 3.0F, new CubeDeformation(0.0F)).texOffs(58, 74).addBox(-0.5F, -0.7F, 13.35F, 1.0F, 2.0F, 3.0F, new CubeDeformation(0.0F)).texOffs(172, 147)
						.addBox(-3.0F, -2.7F, 9.35F, 6.0F, 7.0F, 4.0F, new CubeDeformation(0.0F)).texOffs(171, 129).addBox(-3.0F, -4.7F, 2.35F, 6.0F, 9.0F, 7.0F, new CubeDeformation(0.0F)).texOffs(44, 116)
						.addBox(-2.0F, 6.3F, 15.85F, 4.0F, 9.0F, 4.0F, new CubeDeformation(0.0F)).texOffs(90, 81).addBox(-3.5F, -1.7F, 16.35F, 7.0F, 3.0F, 3.0F, new CubeDeformation(0.0F)).texOffs(60, 116)
						.addBox(0.5F, -2.7F, 15.35F, 2.0F, 6.0F, 5.0F, new CubeDeformation(0.0F)).texOffs(60, 116).addBox(-2.5F, -2.7F, 15.35F, 2.0F, 6.0F, 5.0F, new CubeDeformation(0.0F)).texOffs(51, 74)
						.addBox(-0.5F, 2.3F, 15.35F, 1.0F, 1.0F, 5.0F, new CubeDeformation(0.0F)).texOffs(22, 73).addBox(-1.5F, 3.3F, 16.35F, 3.0F, 3.0F, 3.0F, new CubeDeformation(0.0F)),
				PartPose.offsetAndRotation(16.0F, 8.7F, 15.15F, 0.0F, 0.7854F, 0.0F));
		return LayerDefinition.create(meshdefinition, 256, 256);
	}

	@Override
	public void renderToBuffer(PoseStack poseStack, VertexConsumer vertexConsumer, int packedLight, int packedOverlay, int rgb) {
		head.render(poseStack, vertexConsumer, packedLight, packedOverlay, rgb);
		body.render(poseStack, vertexConsumer, packedLight, packedOverlay, rgb);
		right_arm.render(poseStack, vertexConsumer, packedLight, packedOverlay, rgb);
		left_arm.render(poseStack, vertexConsumer, packedLight, packedOverlay, rgb);
		right_leg.render(poseStack, vertexConsumer, packedLight, packedOverlay, rgb);
		middle_leg.render(poseStack, vertexConsumer, packedLight, packedOverlay, rgb);
		left_leg.render(poseStack, vertexConsumer, packedLight, packedOverlay, rgb);
		back_right_leg.render(poseStack, vertexConsumer, packedLight, packedOverlay, rgb);
		back_middle_leg.render(poseStack, vertexConsumer, packedLight, packedOverlay, rgb);
		back_left_leg.render(poseStack, vertexConsumer, packedLight, packedOverlay, rgb);
	}

	public void setupAnim(T entity, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch) {
		this.head.yRot = netHeadYaw / (180F / (float) Math.PI);
		this.head.xRot = headPitch / (180F / (float) Math.PI);
		this.right_arm_bottom.yRot = Mth.cos(limbSwing * 0.6662F + (float) Math.PI) * limbSwingAmount;
		this.right_arm.xRot = Mth.cos(limbSwing * 0.6662F + (float) Math.PI) * limbSwingAmount;
		this.left_leg.zRot = Mth.cos(limbSwing * 1.0F) * -1.0F * limbSwingAmount;
		this.left_arm.xRot = headPitch / (180F / (float) Math.PI);
		this.right_leg.zRot = Mth.cos(limbSwing * 1.0F) * -1.0F * limbSwingAmount;
		this.middle_leg.zRot = Mth.cos(limbSwing * 1.0F) * 1.0F * limbSwingAmount;
		this.back_right_leg.zRot = Mth.cos(limbSwing * 1.0F) * -1.0F * limbSwingAmount;
		this.back_middle_leg.zRot = Mth.cos(limbSwing * 1.0F) * 1.0F * limbSwingAmount;
		this.back_left_leg.zRot = Mth.cos(limbSwing * 1.0F) * -1.0F * limbSwingAmount;
	}
}