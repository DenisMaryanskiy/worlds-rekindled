package com.dmaryanskiy.vanillareforged.client.sentry;

import java.util.List;

import com.mojang.blaze3d.vertex.PoseStack;

import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.Identifier;

/**
 * While the Sentry is awake its heart glows like the Creaking's eyes, marking its weak point, and a
 * glowing face (a jack o'lantern's, or a Ruin Sentry's eye) does too. There's a heart texture per crack count, since the additive glow
 * would otherwise light up the dark cracks.
 */
public class SentryGlowLayer extends RenderLayer<HeartwoodSentryRenderState, HeartwoodSentryModel> {
	private final List<Identifier> hearts;
	private final Identifier face;

	public SentryGlowLayer(RenderLayerParent<HeartwoodSentryRenderState, HeartwoodSentryModel> renderer, List<Identifier> hearts,
			Identifier face) {
		super(renderer);
		this.hearts = hearts;
		this.face = face;
	}

	@Override
	public void submit(PoseStack poseStack, SubmitNodeCollector submitNodeCollector, int lightCoords,
			HeartwoodSentryRenderState state, float yRot, float xRot) {
		if (state.statue || state.isInvisible) {
			return;
		}
		this.glow(this.hearts.get(Math.min(state.cracks, this.hearts.size() - 1)), poseStack, submitNodeCollector, lightCoords, state);
		if (state.glowingFace) {
			this.glow(this.face, poseStack, submitNodeCollector, lightCoords, state);
		}
	}

	private void glow(Identifier texture, PoseStack poseStack, SubmitNodeCollector submitNodeCollector, int lightCoords,
			HeartwoodSentryRenderState state) {
		submitNodeCollector.order(1).submitModel(this.getParentModel(), state, poseStack, RenderTypes.eyes(texture),
				lightCoords, OverlayTexture.NO_OVERLAY, state.outlineColor);
	}
}
