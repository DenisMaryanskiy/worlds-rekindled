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
 * The heart glows like the Creaking's eyes while the Sentry is awake, marking its weak point. There's
 * a glow texture per crack count, since the additive glow would otherwise light up the dark cracks.
 */
public class SentryHeartGlowLayer extends RenderLayer<HeartwoodSentryRenderState, HeartwoodSentryModel> {
	private final List<Identifier> textures;

	public SentryHeartGlowLayer(RenderLayerParent<HeartwoodSentryRenderState, HeartwoodSentryModel> renderer, List<Identifier> textures) {
		super(renderer);
		this.textures = textures;
	}

	@Override
	public void submit(PoseStack poseStack, SubmitNodeCollector submitNodeCollector, int lightCoords,
			HeartwoodSentryRenderState state, float yRot, float xRot) {
		if (!state.statue && !state.isInvisible) {
			Identifier texture = this.textures.get(Math.min(state.cracks, this.textures.size() - 1));
			submitNodeCollector.order(1).submitModel(this.getParentModel(), state, poseStack, RenderTypes.eyes(texture),
					lightCoords, OverlayTexture.NO_OVERLAY, state.outlineColor);
		}
	}
}
