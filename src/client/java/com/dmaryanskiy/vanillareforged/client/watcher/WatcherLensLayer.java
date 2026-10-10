package com.dmaryanskiy.vanillareforged.client.watcher;

import com.mojang.blaze3d.vertex.PoseStack;

import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.Identifier;

/** The lens glows like the Creaking's eyes. */
public class WatcherLensLayer extends RenderLayer<WatcherRenderState, WatcherModel> {
	private final Identifier texture;

	public WatcherLensLayer(RenderLayerParent<WatcherRenderState, WatcherModel> renderer, Identifier texture) {
		super(renderer);
		this.texture = texture;
	}

	@Override
	public void submit(PoseStack poseStack, SubmitNodeCollector submitNodeCollector, int lightCoords,
			WatcherRenderState state, float yRot, float xRot) {
		if (!state.isInvisible) {
			submitNodeCollector.order(1).submitModel(this.getParentModel(), state, poseStack, RenderTypes.eyes(this.texture),
					lightCoords, OverlayTexture.NO_OVERLAY, state.outlineColor);
		}
	}
}
