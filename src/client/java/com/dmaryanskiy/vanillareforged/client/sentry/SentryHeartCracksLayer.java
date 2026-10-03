package com.dmaryanskiy.vanillareforged.client.sentry;

import java.util.List;

import com.mojang.blaze3d.vertex.PoseStack;

import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.resources.Identifier;

/** Crack stages over the heart, drawn like the iron golem's crackiness. */
public class SentryHeartCracksLayer extends RenderLayer<HeartwoodSentryRenderState, HeartwoodSentryModel> {
	private final List<Identifier> stages;

	public SentryHeartCracksLayer(RenderLayerParent<HeartwoodSentryRenderState, HeartwoodSentryModel> renderer, List<Identifier> stages) {
		super(renderer);
		this.stages = stages;
	}

	@Override
	public void submit(PoseStack poseStack, SubmitNodeCollector submitNodeCollector, int lightCoords,
			HeartwoodSentryRenderState state, float yRot, float xRot) {
		if (state.cracks > 0 && !state.statue && !state.isInvisible) {
			Identifier texture = this.stages.get(Math.min(state.cracks, this.stages.size()) - 1);
			renderColoredCutoutModel(this.getParentModel(), texture, poseStack, submitNodeCollector, lightCoords, state, -1, 1);
		}
	}
}
