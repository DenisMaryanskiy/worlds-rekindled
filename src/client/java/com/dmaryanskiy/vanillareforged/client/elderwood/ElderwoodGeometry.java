package com.dmaryanskiy.vanillareforged.client.elderwood;

import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;

/**
 * The Elderwood's placeholder geometry: boxes for the trunk, roots, crown, copper and hearts, with
 * their UVs on a 512x512 texture. Final art replaces this class and the textures; the part names
 * {@link ElderwoodModel} uses ("tree", "upper", "crown", "right_branch", "left_branch") are the contract.
 */
final class ElderwoodGeometry {
	private ElderwoodGeometry() {
	}

	static LayerDefinition create() {
		MeshDefinition mesh = new MeshDefinition();
		PartDefinition tree = mesh.getRoot().addOrReplaceChild("tree", CubeListBuilder.create()
				.texOffs(80, 0).addBox(-24.0F, -32.0F, -24.0F, 48.0F, 32.0F, 48.0F)
				.texOffs(0, 102).addBox(-20.0F, -56.0F, -20.0F, 40.0F, 24.0F, 40.0F)
				.texOffs(66, 213).addBox(-7.0F, -12.0F, -44.0F, 14.0F, 12.0F, 20.0F)
				.texOffs(134, 213).addBox(-7.0F, -12.0F, 24.0F, 14.0F, 12.0F, 20.0F)
				.texOffs(374, 213).addBox(-44.0F, -12.0F, -7.0F, 20.0F, 12.0F, 14.0F)
				.texOffs(442, 213).addBox(24.0F, -12.0F, -7.0F, 20.0F, 12.0F, 14.0F)
				.texOffs(116, 249).addBox(-36.0F, -8.0F, -36.0F, 13.0F, 8.0F, 13.0F)
				.texOffs(168, 249).addBox(23.0F, -8.0F, -36.0F, 13.0F, 8.0F, 13.0F)
				.texOffs(220, 249).addBox(-36.0F, -8.0F, 23.0F, 13.0F, 8.0F, 13.0F)
				.texOffs(272, 249).addBox(23.0F, -8.0F, 23.0F, 13.0F, 8.0F, 13.0F)
				.texOffs(0, 166).addBox(-21.0F, -40.0F, -21.0F, 42.0F, 5.0F, 42.0F)
				.texOffs(280, 275).addBox(-7.0F, -54.0F, -21.0F, 14.0F, 14.0F, 1.0F)
				.texOffs(348, 275).addBox(-5.0F, -30.0F, -25.0F, 10.0F, 10.0F, 1.0F)
				.texOffs(370, 275).addBox(-5.0F, -30.0F, 24.0F, 10.0F, 10.0F, 1.0F)
				.texOffs(456, 249).addBox(-25.0F, -30.0F, -5.0F, 1.0F, 10.0F, 10.0F)
				.texOffs(478, 249).addBox(24.0F, -30.0F, -5.0F, 1.0F, 10.0F, 10.0F), PartPose.offset(0.0F, 24.0F, 0.0F));
		PartDefinition upper = tree.addOrReplaceChild("upper", CubeListBuilder.create()
				.texOffs(272, 0).addBox(-20.0F, -96.0F, -20.0F, 40.0F, 40.0F, 40.0F)
				.texOffs(160, 102).addBox(-16.0F, -124.0F, -16.0F, 32.0F, 28.0F, 32.0F)
				.texOffs(202, 213).addBox(-10.0F, -134.0F, -10.0F, 20.0F, 10.0F, 20.0F)
				.texOffs(168, 166).addBox(-21.0F, -88.0F, -21.0F, 42.0F, 5.0F, 42.0F)
				.texOffs(360, 166).addBox(-17.0F, -112.0F, -17.0F, 34.0F, 4.0F, 34.0F)
				.texOffs(0, 213).addBox(-21.0F, -78.0F, -8.0F, 1.0F, 20.0F, 16.0F)
				.texOffs(0, 249).addBox(20.0F, -72.0F, -2.0F, 1.0F, 14.0F, 12.0F)
				.texOffs(58, 249).addBox(-8.0F, -80.0F, 20.0F, 16.0F, 22.0F, 1.0F)
				.texOffs(0, 275).addBox(-9.0F, -73.0F, -22.0F, 18.0F, 18.0F, 2.0F)
				.texOffs(322, 275).addBox(-6.0F, -70.0F, -23.0F, 12.0F, 12.0F, 1.0F), PartPose.offset(0.0F, 0.0F, 0.0F));
		PartDefinition crown = upper.addOrReplaceChild("crown", CubeListBuilder.create()
				.texOffs(34, 213).addBox(-40.0F, -150.0F, 6.0F, 16.0F, 34.0F, 0.0F)
				.texOffs(282, 213).addBox(22.0F, -146.0F, -4.0F, 16.0F, 30.0F, 0.0F)
				.texOffs(346, 213).addBox(-6.0F, -140.0F, 34.0F, 14.0F, 28.0F, 0.0F), PartPose.offset(0.0F, 0.0F, 0.0F));
		PartDefinition rightBranch = crown.addOrReplaceChild("right_branch", CubeListBuilder.create()
				.texOffs(0, 0).addBox(-5.0F, -92.0F, -5.0F, 10.0F, 92.0F, 10.0F)
				.texOffs(76, 275).addBox(-6.0F, -30.0F, -6.0F, 12.0F, 6.0F, 12.0F)
				.texOffs(172, 275).addBox(-6.0F, -54.0F, -6.0F, 12.0F, 5.0F, 12.0F)
				.texOffs(324, 249).addBox(-6.0F, -69.0F, -6.0F, 12.0F, 9.0F, 12.0F), PartPose.offset(-20.0F, -72.0F, 0.0F));
		rightBranch.addOrReplaceChild("right_branch_twig_low", CubeListBuilder.create()
				.texOffs(314, 213).addBox(-2.0F, -26.0F, -2.0F, 4.0F, 26.0F, 4.0F), PartPose.offsetAndRotation(0.0F, -44.0F, 0.0F, 0.3F, 0.0F, -0.8F));
		rightBranch.addOrReplaceChild("right_branch_twig_mid", CubeListBuilder.create()
				.texOffs(26, 249).addBox(-2.0F, -22.0F, -2.0F, 4.0F, 22.0F, 4.0F), PartPose.offsetAndRotation(0.0F, -78.0F, 0.0F, -0.5F, 0.0F, 0.6F));
		rightBranch.addOrReplaceChild("right_branch_twig_tip", CubeListBuilder.create()
				.texOffs(92, 249).addBox(-1.5F, -20.0F, -1.5F, 3.0F, 20.0F, 3.0F), PartPose.offsetAndRotation(0.0F, -90.0F, 0.0F, 0.2F, 0.0F, -0.5F));
		PartDefinition leftBranch = crown.addOrReplaceChild("left_branch", CubeListBuilder.create()
				.texOffs(40, 0).addBox(-5.0F, -92.0F, -5.0F, 10.0F, 92.0F, 10.0F)
				.texOffs(124, 275).addBox(-6.0F, -30.0F, -6.0F, 12.0F, 6.0F, 12.0F)
				.texOffs(220, 275).addBox(-6.0F, -54.0F, -6.0F, 12.0F, 5.0F, 12.0F)
				.texOffs(372, 249).addBox(-6.0F, -69.0F, -6.0F, 12.0F, 9.0F, 12.0F), PartPose.offset(20.0F, -72.0F, 0.0F));
		leftBranch.addOrReplaceChild("left_branch_twig_low", CubeListBuilder.create()
				.texOffs(330, 213).addBox(-2.0F, -26.0F, -2.0F, 4.0F, 26.0F, 4.0F), PartPose.offsetAndRotation(0.0F, -44.0F, 0.0F, 0.3F, 0.0F, 0.8F));
		leftBranch.addOrReplaceChild("left_branch_twig_mid", CubeListBuilder.create()
				.texOffs(42, 249).addBox(-2.0F, -22.0F, -2.0F, 4.0F, 22.0F, 4.0F), PartPose.offsetAndRotation(0.0F, -78.0F, 0.0F, -0.5F, 0.0F, -0.6F));
		leftBranch.addOrReplaceChild("left_branch_twig_tip", CubeListBuilder.create()
				.texOffs(104, 249).addBox(-1.5F, -20.0F, -1.5F, 3.0F, 20.0F, 3.0F), PartPose.offsetAndRotation(0.0F, -90.0F, 0.0F, 0.2F, 0.0F, 0.5F));
		PartDefinition crownBack = crown.addOrReplaceChild("crown_back", CubeListBuilder.create()
				.texOffs(288, 102).addBox(-4.0F, -46.0F, -4.0F, 8.0F, 46.0F, 8.0F), PartPose.offsetAndRotation(0.0F, -116.0F, 10.0F, -0.7F, 0.0F, 0.0F));
		crownBack.addOrReplaceChild("crown_back_twig_0", CubeListBuilder.create()
				.texOffs(420, 249).addBox(-1.5F, -18.0F, -1.5F, 3.0F, 18.0F, 3.0F), PartPose.offsetAndRotation(0.0F, -26.0F, 0.0F, 0.0F, 0.0F, 0.7F));
		crownBack.addOrReplaceChild("crown_back_twig_1", CubeListBuilder.create()
				.texOffs(40, 275).addBox(-1.5F, -16.0F, -1.5F, 3.0F, 16.0F, 3.0F), PartPose.offsetAndRotation(0.0F, -40.0F, 0.0F, 0.5F, 0.0F, -0.6F));
		PartDefinition crownRight = crown.addOrReplaceChild("crown_right", CubeListBuilder.create()
				.texOffs(320, 102).addBox(-4.0F, -44.0F, -4.0F, 8.0F, 44.0F, 8.0F), PartPose.offsetAndRotation(-10.0F, -120.0F, 0.0F, 0.15F, 0.0F, -0.65F));
		crownRight.addOrReplaceChild("crown_right_twig_0", CubeListBuilder.create()
				.texOffs(432, 249).addBox(-1.5F, -18.0F, -1.5F, 3.0F, 18.0F, 3.0F), PartPose.offsetAndRotation(0.0F, -24.0F, 0.0F, -0.6F, 0.0F, 0.3F));
		crownRight.addOrReplaceChild("crown_right_twig_1", CubeListBuilder.create()
				.texOffs(52, 275).addBox(-1.5F, -16.0F, -1.5F, 3.0F, 16.0F, 3.0F), PartPose.offsetAndRotation(0.0F, -38.0F, 0.0F, 0.4F, 0.0F, -0.5F));
		PartDefinition crownLeft = crown.addOrReplaceChild("crown_left", CubeListBuilder.create()
				.texOffs(352, 102).addBox(-4.0F, -44.0F, -4.0F, 8.0F, 44.0F, 8.0F), PartPose.offsetAndRotation(10.0F, -120.0F, 0.0F, 0.15F, 0.0F, 0.65F));
		crownLeft.addOrReplaceChild("crown_left_twig_0", CubeListBuilder.create()
				.texOffs(444, 249).addBox(-1.5F, -18.0F, -1.5F, 3.0F, 18.0F, 3.0F), PartPose.offsetAndRotation(0.0F, -24.0F, 0.0F, -0.6F, 0.0F, -0.3F));
		crownLeft.addOrReplaceChild("crown_left_twig_1", CubeListBuilder.create()
				.texOffs(64, 275).addBox(-1.5F, -16.0F, -1.5F, 3.0F, 16.0F, 3.0F), PartPose.offsetAndRotation(0.0F, -38.0F, 0.0F, 0.4F, 0.0F, 0.5F));
		PartDefinition crownFront = crown.addOrReplaceChild("crown_front", CubeListBuilder.create()
				.texOffs(336, 166).addBox(-3.0F, -36.0F, -3.0F, 6.0F, 36.0F, 6.0F), PartPose.offsetAndRotation(0.0F, -124.0F, -8.0F, 0.4F, 0.0F, 0.1F));
		crownFront.addOrReplaceChild("crown_front_twig_0", CubeListBuilder.create()
				.texOffs(268, 275).addBox(-1.5F, -14.0F, -1.5F, 3.0F, 14.0F, 3.0F), PartPose.offsetAndRotation(0.0F, -20.0F, 0.0F, 0.0F, 0.0F, -0.7F));
		crownFront.addOrReplaceChild("crown_front_twig_1", CubeListBuilder.create()
				.texOffs(310, 275).addBox(-1.5F, -12.0F, -1.5F, 3.0F, 12.0F, 3.0F), PartPose.offsetAndRotation(0.0F, -32.0F, 0.0F, 0.3F, 0.0F, 0.6F));
		return LayerDefinition.create(mesh, 512, 512);
	}
}
