package com.dmaryanskiy.vanillareforged.client;

import net.minecraft.client.model.Model;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.client.renderer.entity.state.HumanoidRenderState;
import net.minecraft.client.renderer.rendertype.RenderTypes;

/**
 * Bark plate over the face with a protruding resin eye and a few twigs. The root child is named
 * "head" so it picks up the wearer's head pose via {@code copyTransforms}.
 */
public class PaleMaskModel extends Model<HumanoidRenderState> {
	public PaleMaskModel(ModelPart root) {
		super(root, RenderTypes::armorCutoutNoCull);
	}

	public static LayerDefinition createLayer() {
		MeshDefinition mesh = new MeshDefinition();
		PartDefinition head = mesh.getRoot().addOrReplaceChild("head", CubeListBuilder.create(), PartPose.ZERO);

		head.addOrReplaceChild("plate", CubeListBuilder.create()
				.texOffs(0, 0).addBox(-4.0F, -8.0F, -5.0F, 8.0F, 8.0F, 1.0F)
				.texOffs(0, 10).addBox(-1.0F, -7.0F, -6.0F, 2.0F, 2.0F, 1.0F), PartPose.ZERO);
		head.addOrReplaceChild("left_twig", CubeListBuilder.create()
				.texOffs(20, 0).addBox(-0.5F, -3.0F, -0.5F, 1.0F, 3.0F, 1.0F),
				PartPose.offsetAndRotation(-3.0F, -8.0F, -4.5F, -0.45F, 0.0F, -0.4F));
		head.addOrReplaceChild("right_twig", CubeListBuilder.create()
				.texOffs(24, 0).addBox(-0.5F, -2.0F, -0.5F, 1.0F, 2.0F, 1.0F),
				PartPose.offsetAndRotation(2.5F, -8.0F, -4.5F, -0.45F, 0.0F, 0.45F));
		head.addOrReplaceChild("side_twig", CubeListBuilder.create()
				.texOffs(20, 5).addBox(0.0F, -0.5F, -0.5F, 2.0F, 1.0F, 1.0F),
				PartPose.offsetAndRotation(4.0F, -5.5F, -4.5F, 0.0F, -0.5F, -0.35F));
		return LayerDefinition.create(mesh, 32, 16);
	}
}
