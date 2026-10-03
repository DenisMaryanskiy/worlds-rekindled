package com.dmaryanskiy.vanillareforged.client.sentry;

import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.client.model.monster.creaking.CreakingModel;

/**
 * The Creaking's body with a pumpkin head. Part names match {@link CreakingModel} so its walk and
 * attack animations apply; the UV layout is our own (128x64).
 */
public class HeartwoodSentryModel extends CreakingModel {
	public HeartwoodSentryModel(ModelPart root) {
		super(root);
	}

	public static LayerDefinition createBodyLayer() {
		MeshDefinition mesh = new MeshDefinition();
		PartDefinition root = mesh.getRoot().addOrReplaceChild("root", CubeListBuilder.create(), PartPose.offset(0.0F, 24.0F, 0.0F));
		PartDefinition upperBody = root.addOrReplaceChild("upper_body", CubeListBuilder.create(), PartPose.offset(-1.0F, -19.0F, 0.0F));
		upperBody.addOrReplaceChild("head", CubeListBuilder.create()
				.texOffs(0, 0).addBox(-4.0F, -8.0F, -4.0F, 8.0F, 8.0F, 8.0F), PartPose.offset(-1.0F, -11.0F, 0.0F));
		upperBody.addOrReplaceChild("body", CubeListBuilder.create()
				.texOffs(32, 0).addBox(0.0F, -3.0F, -3.0F, 6.0F, 13.0F, 5.0F)
				.texOffs(54, 0).addBox(-6.0F, -4.0F, -3.0F, 6.0F, 7.0F, 5.0F), PartPose.offset(0.0F, -7.0F, 1.0F));
		upperBody.addOrReplaceChild("right_arm", CubeListBuilder.create()
				.texOffs(76, 0).addBox(-2.0F, -1.5F, -1.5F, 3.0F, 21.0F, 3.0F)
				.texOffs(0, 24).addBox(-2.0F, 19.5F, -1.5F, 3.0F, 4.0F, 3.0F), PartPose.offset(-7.0F, -9.5F, 1.5F));
		upperBody.addOrReplaceChild("left_arm", CubeListBuilder.create()
				.texOffs(88, 0).addBox(0.0F, -1.0F, -1.5F, 3.0F, 16.0F, 3.0F)
				.texOffs(12, 24).addBox(0.0F, -5.0F, -1.5F, 3.0F, 4.0F, 3.0F)
				.texOffs(24, 24).addBox(0.0F, 15.0F, -1.5F, 3.0F, 4.0F, 3.0F), PartPose.offset(6.0F, -9.0F, 0.5F));
		root.addOrReplaceChild("left_leg", CubeListBuilder.create()
				.texOffs(112, 0).addBox(-1.5F, 0.0F, -1.5F, 3.0F, 16.0F, 3.0F)
				.texOffs(48, 24).addBox(-1.5F, 15.7F, -4.5F, 5.0F, 0.0F, 9.0F), PartPose.offset(1.5F, -16.0F, 0.5F));
		root.addOrReplaceChild("right_leg", CubeListBuilder.create()
				.texOffs(100, 0).addBox(-3.0F, -1.5F, -1.5F, 3.0F, 19.0F, 3.0F)
				.texOffs(76, 24).addBox(-5.0F, 17.2F, -4.5F, 5.0F, 0.0F, 9.0F)
				.texOffs(36, 24).addBox(-3.0F, -4.5F, -1.5F, 3.0F, 3.0F, 3.0F), PartPose.offset(-1.0F, -17.5F, 0.5F));
		return LayerDefinition.create(mesh, 128, 64);
	}
}
