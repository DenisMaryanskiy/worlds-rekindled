package com.dmaryanskiy.vanillareforged.client.elderwood;

import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;

/**
 * The Elderwood's placeholder geometry: boxes for the trunk, roots, crown, copper and hearts, with
 * their UVs on a 512x512 texture. Final art replaces this class and the textures; the part names
 * {@link ElderwoodModel} animates ("tree", "right_branch", "left_branch") are the contract.
 */
final class ElderwoodGeometry {
	private ElderwoodGeometry() {
	}

	static LayerDefinition create() {
		MeshDefinition mesh = new MeshDefinition();
		PartDefinition tree = mesh.getRoot().addOrReplaceChild("tree", CubeListBuilder.create()
				.texOffs(240, 0).addBox(-24.0F, -32.0F, -24.0F, 48.0F, 32.0F, 48.0F)
				.texOffs(0, 0).addBox(-20.0F, -96.0F, -20.0F, 40.0F, 64.0F, 40.0F)
				.texOffs(0, 104).addBox(-16.0F, -124.0F, -16.0F, 32.0F, 28.0F, 32.0F)
				.texOffs(68, 211).addBox(-10.0F, -134.0F, -10.0F, 20.0F, 10.0F, 20.0F)
				.texOffs(394, 164).addBox(-7.0F, -12.0F, -44.0F, 14.0F, 12.0F, 20.0F)
				.texOffs(0, 211).addBox(-7.0F, -12.0F, 24.0F, 14.0F, 12.0F, 20.0F)
				.texOffs(240, 211).addBox(-44.0F, -12.0F, -7.0F, 20.0F, 12.0F, 14.0F)
				.texOffs(308, 211).addBox(24.0F, -12.0F, -7.0F, 20.0F, 12.0F, 14.0F)
				.texOffs(0, 243).addBox(-36.0F, -8.0F, -36.0F, 13.0F, 8.0F, 13.0F)
				.texOffs(52, 243).addBox(23.0F, -8.0F, -36.0F, 13.0F, 8.0F, 13.0F)
				.texOffs(104, 243).addBox(-36.0F, -8.0F, 23.0F, 13.0F, 8.0F, 13.0F)
				.texOffs(156, 243).addBox(23.0F, -8.0F, 23.0F, 13.0F, 8.0F, 13.0F)
				.texOffs(224, 104).addBox(-21.0F, -40.0F, -21.0F, 42.0F, 5.0F, 42.0F)
				.texOffs(0, 164).addBox(-21.0F, -88.0F, -21.0F, 42.0F, 5.0F, 42.0F)
				.texOffs(192, 164).addBox(-17.0F, -112.0F, -17.0F, 34.0F, 4.0F, 34.0F)
				.texOffs(328, 164).addBox(-21.0F, -70.0F, -8.0F, 1.0F, 20.0F, 16.0F)
				.texOffs(376, 211).addBox(20.0F, -62.0F, -2.0F, 1.0F, 14.0F, 12.0F)
				.texOffs(434, 211).addBox(-8.0F, -74.0F, 20.0F, 16.0F, 22.0F, 1.0F)
				.texOffs(340, 243).addBox(-9.0F, -73.0F, -22.0F, 18.0F, 18.0F, 2.0F)
				.texOffs(198, 264).addBox(-6.0F, -70.0F, -23.0F, 12.0F, 12.0F, 1.0F)
				.texOffs(156, 264).addBox(-7.0F, -54.0F, -21.0F, 14.0F, 14.0F, 1.0F)
				.texOffs(224, 264).addBox(-5.0F, -30.0F, -25.0F, 10.0F, 10.0F, 1.0F)
				.texOffs(246, 264).addBox(-5.0F, -30.0F, 24.0F, 10.0F, 10.0F, 1.0F)
				.texOffs(380, 243).addBox(-25.0F, -30.0F, -5.0F, 1.0F, 10.0F, 10.0F)
				.texOffs(402, 243).addBox(24.0F, -30.0F, -5.0F, 1.0F, 10.0F, 10.0F)
				.texOffs(362, 164).addBox(-40.0F, -150.0F, 6.0F, 16.0F, 34.0F, 0.0F)
				.texOffs(148, 211).addBox(22.0F, -146.0F, -4.0F, 16.0F, 30.0F, 0.0F)
				.texOffs(212, 211).addBox(-6.0F, -140.0F, 34.0F, 14.0F, 28.0F, 0.0F), PartPose.offset(0.0F, 24.0F, 0.0F));
		PartDefinition rightBranch = tree.addOrReplaceChild("right_branch", CubeListBuilder.create()
				.texOffs(160, 0).addBox(-5.0F, -92.0F, -5.0F, 10.0F, 92.0F, 10.0F)
				.texOffs(460, 243).addBox(-6.0F, -30.0F, -6.0F, 12.0F, 6.0F, 12.0F)
				.texOffs(48, 264).addBox(-6.0F, -54.0F, -6.0F, 12.0F, 5.0F, 12.0F)
				.texOffs(208, 243).addBox(-6.0F, -69.0F, -6.0F, 12.0F, 9.0F, 12.0F), PartPose.offset(-20.0F, -72.0F, 0.0F));
		rightBranch.addOrReplaceChild("right_branch_twig_low", CubeListBuilder.create()
				.texOffs(180, 211).addBox(-2.0F, -26.0F, -2.0F, 4.0F, 26.0F, 4.0F), PartPose.offsetAndRotation(0.0F, -44.0F, 0.0F, 0.3F, 0.0F, -0.8F));
		rightBranch.addOrReplaceChild("right_branch_twig_mid", CubeListBuilder.create()
				.texOffs(402, 211).addBox(-2.0F, -22.0F, -2.0F, 4.0F, 22.0F, 4.0F), PartPose.offsetAndRotation(0.0F, -78.0F, 0.0F, -0.5F, 0.0F, 0.6F));
		rightBranch.addOrReplaceChild("right_branch_twig_tip", CubeListBuilder.create()
				.texOffs(468, 211).addBox(-1.5F, -20.0F, -1.5F, 3.0F, 20.0F, 3.0F), PartPose.offsetAndRotation(0.0F, -90.0F, 0.0F, 0.2F, 0.0F, -0.5F));
		PartDefinition leftBranch = tree.addOrReplaceChild("left_branch", CubeListBuilder.create()
				.texOffs(200, 0).addBox(-5.0F, -92.0F, -5.0F, 10.0F, 92.0F, 10.0F)
				.texOffs(0, 264).addBox(-6.0F, -30.0F, -6.0F, 12.0F, 6.0F, 12.0F)
				.texOffs(96, 264).addBox(-6.0F, -54.0F, -6.0F, 12.0F, 5.0F, 12.0F)
				.texOffs(256, 243).addBox(-6.0F, -69.0F, -6.0F, 12.0F, 9.0F, 12.0F), PartPose.offset(20.0F, -72.0F, 0.0F));
		leftBranch.addOrReplaceChild("left_branch_twig_low", CubeListBuilder.create()
				.texOffs(196, 211).addBox(-2.0F, -26.0F, -2.0F, 4.0F, 26.0F, 4.0F), PartPose.offsetAndRotation(0.0F, -44.0F, 0.0F, 0.3F, 0.0F, 0.8F));
		leftBranch.addOrReplaceChild("left_branch_twig_mid", CubeListBuilder.create()
				.texOffs(418, 211).addBox(-2.0F, -22.0F, -2.0F, 4.0F, 22.0F, 4.0F), PartPose.offsetAndRotation(0.0F, -78.0F, 0.0F, -0.5F, 0.0F, -0.6F));
		leftBranch.addOrReplaceChild("left_branch_twig_tip", CubeListBuilder.create()
				.texOffs(480, 211).addBox(-1.5F, -20.0F, -1.5F, 3.0F, 20.0F, 3.0F), PartPose.offsetAndRotation(0.0F, -90.0F, 0.0F, 0.2F, 0.0F, 0.5F));
		PartDefinition crownBack = tree.addOrReplaceChild("crown_back", CubeListBuilder.create()
				.texOffs(128, 104).addBox(-4.0F, -46.0F, -4.0F, 8.0F, 46.0F, 8.0F), PartPose.offsetAndRotation(0.0F, -116.0F, 10.0F, -0.7F, 0.0F, 0.0F));
		crownBack.addOrReplaceChild("crown_back_twig_0", CubeListBuilder.create()
				.texOffs(304, 243).addBox(-1.5F, -18.0F, -1.5F, 3.0F, 18.0F, 3.0F), PartPose.offsetAndRotation(0.0F, -26.0F, 0.0F, 0.0F, 0.0F, 0.7F));
		crownBack.addOrReplaceChild("crown_back_twig_1", CubeListBuilder.create()
				.texOffs(424, 243).addBox(-1.5F, -16.0F, -1.5F, 3.0F, 16.0F, 3.0F), PartPose.offsetAndRotation(0.0F, -40.0F, 0.0F, 0.5F, 0.0F, -0.6F));
		PartDefinition crownRight = tree.addOrReplaceChild("crown_right", CubeListBuilder.create()
				.texOffs(160, 104).addBox(-4.0F, -44.0F, -4.0F, 8.0F, 44.0F, 8.0F), PartPose.offsetAndRotation(-10.0F, -120.0F, 0.0F, 0.15F, 0.0F, -0.65F));
		crownRight.addOrReplaceChild("crown_right_twig_0", CubeListBuilder.create()
				.texOffs(316, 243).addBox(-1.5F, -18.0F, -1.5F, 3.0F, 18.0F, 3.0F), PartPose.offsetAndRotation(0.0F, -24.0F, 0.0F, -0.6F, 0.0F, 0.3F));
		crownRight.addOrReplaceChild("crown_right_twig_1", CubeListBuilder.create()
				.texOffs(436, 243).addBox(-1.5F, -16.0F, -1.5F, 3.0F, 16.0F, 3.0F), PartPose.offsetAndRotation(0.0F, -38.0F, 0.0F, 0.4F, 0.0F, -0.5F));
		PartDefinition crownLeft = tree.addOrReplaceChild("crown_left", CubeListBuilder.create()
				.texOffs(192, 104).addBox(-4.0F, -44.0F, -4.0F, 8.0F, 44.0F, 8.0F), PartPose.offsetAndRotation(10.0F, -120.0F, 0.0F, 0.15F, 0.0F, 0.65F));
		crownLeft.addOrReplaceChild("crown_left_twig_0", CubeListBuilder.create()
				.texOffs(328, 243).addBox(-1.5F, -18.0F, -1.5F, 3.0F, 18.0F, 3.0F), PartPose.offsetAndRotation(0.0F, -24.0F, 0.0F, -0.6F, 0.0F, -0.3F));
		crownLeft.addOrReplaceChild("crown_left_twig_1", CubeListBuilder.create()
				.texOffs(448, 243).addBox(-1.5F, -16.0F, -1.5F, 3.0F, 16.0F, 3.0F), PartPose.offsetAndRotation(0.0F, -38.0F, 0.0F, 0.4F, 0.0F, 0.5F));
		PartDefinition crownFront = tree.addOrReplaceChild("crown_front", CubeListBuilder.create()
				.texOffs(168, 164).addBox(-3.0F, -36.0F, -3.0F, 6.0F, 36.0F, 6.0F), PartPose.offsetAndRotation(0.0F, -124.0F, -8.0F, 0.4F, 0.0F, 0.1F));
		crownFront.addOrReplaceChild("crown_front_twig_0", CubeListBuilder.create()
				.texOffs(144, 264).addBox(-1.5F, -14.0F, -1.5F, 3.0F, 14.0F, 3.0F), PartPose.offsetAndRotation(0.0F, -20.0F, 0.0F, 0.0F, 0.0F, -0.7F));
		crownFront.addOrReplaceChild("crown_front_twig_1", CubeListBuilder.create()
				.texOffs(186, 264).addBox(-1.5F, -12.0F, -1.5F, 3.0F, 12.0F, 3.0F), PartPose.offsetAndRotation(0.0F, -32.0F, 0.0F, 0.3F, 0.0F, 0.6F));
		return LayerDefinition.create(mesh, 512, 512);
	}
}
