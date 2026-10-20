package com.dmaryanskiy.vanillareforged.client.sentry;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;

import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.util.LightCoordsUtil;

/** The Elderwood Lens set in a player's Sentry, high on its chest beside the heart, lit as if it glowed. */
public class SentryLensLayer extends RenderLayer<HeartwoodSentryRenderState, HeartwoodSentryModel> {
	private static final float SCALE = 0.3F;

	public SentryLensLayer(RenderLayerParent<HeartwoodSentryRenderState, HeartwoodSentryModel> renderer) {
		super(renderer);
	}

	@Override
	public void submit(PoseStack poseStack, SubmitNodeCollector submitNodeCollector, int lightCoords,
			HeartwoodSentryRenderState state, float yRot, float xRot) {
		if (state.lens.isEmpty() || state.isInvisible) {
			return;
		}
		poseStack.pushPose();
		this.getParentModel().translateToLens(poseStack);
		// The model is drawn upside down; the item isn't.
		poseStack.rotateDegrees(Axis.ZP, 180.0F);
		poseStack.scale(SCALE, SCALE, SCALE);
		state.lens.submit(poseStack, submitNodeCollector, LightCoordsUtil.FULL_BRIGHT, OverlayTexture.NO_OVERLAY, state.outlineColor);
		poseStack.popPose();
	}
}
