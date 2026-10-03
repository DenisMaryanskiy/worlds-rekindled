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
 * jack o'lantern face glows too. There's a heart texture per crack count, since the additive glow
 * would otherwise light up the dark cracks.
 */
public class SentryGlowLayer extends RenderLayer<HeartwoodSentryRenderState, HeartwoodSentryModel> {
	private final List<Identifier> hearts;
	private final Identifier jackOLanternFace;

	public SentryGlowLayer(RenderLayerParent<HeartwoodSentryRenderState, HeartwoodSentryModel> renderer, List<Identifier> hearts,
			Identifier jackOLanternFace) {
		super(renderer);
		this.hearts = hearts;
		this.jackOLanternFace = jackOLanternFace;
	}

	@Override
	public void submit(PoseStack poseStack, SubmitNodeCollector submitNodeCollector, int lightCoords,
			HeartwoodSentryRenderState state, float yRot, float xRot) {
		if (state.statue || state.isInvisible) {
			return;
		}
		this.glow(this.hearts.get(Math.min(state.cracks, this.hearts.size() - 1)), poseStack, submitNodeCollector, lightCoords, state);
		if (state.jackOLantern) {
			this.glow(this.jackOLanternFace, poseStack, submitNodeCollector, lightCoords, state);
		}
	}

	private void glow(Identifier texture, PoseStack poseStack, SubmitNodeCollector submitNodeCollector, int lightCoords,
			HeartwoodSentryRenderState state) {
		submitNodeCollector.order(1).submitModel(this.getParentModel(), state, poseStack, RenderTypes.eyes(texture),
				lightCoords, OverlayTexture.NO_OVERLAY, state.outlineColor);
	}
}
