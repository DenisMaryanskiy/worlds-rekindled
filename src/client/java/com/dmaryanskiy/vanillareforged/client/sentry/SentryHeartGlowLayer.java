package com.dmaryanskiy.vanillareforged.client.sentry;

import com.mojang.blaze3d.vertex.PoseStack;

import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.Identifier;

/** The heart glows like the Creaking's eyes while the Sentry is awake, marking its weak point. */
public class SentryHeartGlowLayer extends RenderLayer<HeartwoodSentryRenderState, HeartwoodSentryModel> {
	private final Identifier texture;

	public SentryHeartGlowLayer(RenderLayerParent<HeartwoodSentryRenderState, HeartwoodSentryModel> renderer, Identifier texture) {
		super(renderer);
		this.texture = texture;
	}

	@Override
	public void submit(PoseStack poseStack, SubmitNodeCollector submitNodeCollector, int lightCoords,
			HeartwoodSentryRenderState state, float yRot, float xRot) {
		if (!state.statue && !state.isInvisible) {
			submitNodeCollector.order(1).submitModel(this.getParentModel(), state, poseStack, RenderTypes.eyes(this.texture),
					lightCoords, OverlayTexture.NO_OVERLAY, state.outlineColor);
		}
	}
}
