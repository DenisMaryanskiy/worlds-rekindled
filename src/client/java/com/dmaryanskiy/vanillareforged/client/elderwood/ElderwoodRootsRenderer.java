package com.dmaryanskiy.vanillareforged.client.elderwood;

import com.dmaryanskiy.vanillareforged.VanillaReforged;
import com.dmaryanskiy.vanillareforged.elderwood.ElderwoodRoots;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;

import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.Identifier;

/** A cluster of root spikes that rises out of the ground and sinks back. Placeholder geometry and texture. */
public class ElderwoodRootsRenderer extends EntityRenderer<ElderwoodRoots, ElderwoodRootsRenderer.State> {
	public static final ModelLayerLocation LAYER = new ModelLayerLocation(VanillaReforged.id("elderwood_roots"), "main");
	private static final Identifier TEXTURE = VanillaReforged.id("textures/entity/elderwood/elderwood_roots.png");
	// The tallest spike, in model pixels: sunk this far, nothing shows above the ground.
	private static final float HEIGHT = 30.0F;

	private final Model model;

	public ElderwoodRootsRenderer(EntityRendererProvider.Context context) {
		super(context);
		this.model = new Model(context.bakeLayer(LAYER));
	}

	public static LayerDefinition createLayer() {
		MeshDefinition mesh = new MeshDefinition();
		PartDefinition roots = mesh.getRoot().addOrReplaceChild("roots", CubeListBuilder.create()
				.texOffs(0, 0).addBox(-3.0F, -HEIGHT, -3.0F, 6.0F, HEIGHT, 6.0F), PartPose.offset(0.0F, 24.0F, 0.0F));
		roots.addOrReplaceChild("spike_left", CubeListBuilder.create()
				.texOffs(24, 0).addBox(-2.0F, -20.0F, -2.0F, 4.0F, 20.0F, 4.0F), PartPose.offsetAndRotation(4.0F, 0.0F, 2.0F, 0.0F, 0.0F, 0.35F));
		roots.addOrReplaceChild("spike_right", CubeListBuilder.create()
				.texOffs(40, 0).addBox(-2.0F, -22.0F, -2.0F, 4.0F, 22.0F, 4.0F), PartPose.offsetAndRotation(-4.0F, 0.0F, -2.0F, 0.2F, 0.0F, -0.4F));
		roots.addOrReplaceChild("spike_back", CubeListBuilder.create()
				.texOffs(0, 36).addBox(-1.5F, -16.0F, -1.5F, 3.0F, 16.0F, 3.0F), PartPose.offsetAndRotation(0.0F, 0.0F, 5.0F, -0.5F, 0.0F, 0.0F));
		return LayerDefinition.create(mesh, 64, 64);
	}

	@Override
	public void submit(State state, PoseStack poseStack, SubmitNodeCollector submitNodeCollector, CameraRenderState camera) {
		if (state.rise <= 0.0F) {
			return;
		}
		poseStack.pushPose();
		poseStack.rotateDegrees(Axis.YP, 90.0F - state.yRot);
		poseStack.scale(-1.0F, -1.0F, 1.0F);
		poseStack.translate(0.0F, EntityModel.MODEL_Y_OFFSET, 0.0F);
		submitNodeCollector.submitModel(this.model, state, poseStack, TEXTURE, state.lightCoords, OverlayTexture.NO_OVERLAY, state.outlineColor);
		poseStack.popPose();
		super.submit(state, poseStack, submitNodeCollector, camera);
	}

	@Override
	public State createRenderState() {
		return new State();
	}

	@Override
	public void extractRenderState(ElderwoodRoots entity, State state, float partialTicks) {
		super.extractRenderState(entity, state, partialTicks);
		state.yRot = entity.getYRot();
		state.rise = entity.getRise(partialTicks);
	}

	public static class State extends EntityRenderState {
		public float yRot;
		public float rise;
	}

	private static class Model extends EntityModel<State> {
		private final ModelPart roots;

		Model(ModelPart root) {
			super(root);
			this.roots = root.getChild("roots");
		}

		@Override
		public void setupAnim(State state) {
			super.setupAnim(state);
			this.roots.y += (1.0F - state.rise) * (HEIGHT + 2.0F);
		}
	}
}
