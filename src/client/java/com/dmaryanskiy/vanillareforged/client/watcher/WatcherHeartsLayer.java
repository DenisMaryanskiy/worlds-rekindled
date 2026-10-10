package com.dmaryanskiy.vanillareforged.client.watcher;

import java.util.Locale;

import com.dmaryanskiy.vanillareforged.VanillaReforged;
import com.dmaryanskiy.vanillareforged.watcher.WatcherHeart;
import com.mojang.blaze3d.vertex.PoseStack;

import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.Identifier;

/**
 * Draws the state of each heart over the body: a heart that can be hit right now glows like the lens
 * (so weak points read in the dark, and nothing else is lit), a destroyed one is a dark husk, and
 * the core's copper shutter is replaced by the open heart once the others are gone.
 */
public class WatcherHeartsLayer extends RenderLayer<WatcherRenderState, WatcherModel> {
	private static final WatcherHeart[] HEARTS = WatcherHeart.values();
	private static final Identifier CORE_OPEN = texture("core_open");

	private final Identifier[] glowing = new Identifier[HEARTS.length];
	private final Identifier[] cracked = new Identifier[HEARTS.length];
	private final Identifier[] broken = new Identifier[HEARTS.length];

	public WatcherHeartsLayer(RenderLayerParent<WatcherRenderState, WatcherModel> renderer) {
		super(renderer);
		for (WatcherHeart heart : HEARTS) {
			String name = heart.name().toLowerCase(Locale.ROOT);
			this.glowing[heart.ordinal()] = texture(name);
			this.cracked[heart.ordinal()] = texture(name + "_cracked");
			this.broken[heart.ordinal()] = texture(name + "_broken");
		}
	}

	private static Identifier texture(String name) {
		return VanillaReforged.id("textures/entity/watcher/heart/" + name + ".png");
	}

	@Override
	public void submit(PoseStack poseStack, SubmitNodeCollector submitNodeCollector, int lightCoords,
			WatcherRenderState state, float yRot, float xRot) {
		if (state.isInvisible) {
			return;
		}
		for (WatcherHeart heart : HEARTS) {
			int i = heart.ordinal();
			if (state.heartCracks[i] >= heart.maxCracks()) {
				renderColoredCutoutModel(this.getParentModel(), this.broken[i], poseStack, submitNodeCollector, lightCoords, state, -1, 1);
				continue;
			}
			if (heart == WatcherHeart.CORE && state.coreOpen) {
				renderColoredCutoutModel(this.getParentModel(), CORE_OPEN, poseStack, submitNodeCollector, lightCoords, state, -1, 1);
			}
			if (state.heartVulnerable[i]) {
				Identifier glow = state.heartCracks[i] > 0 ? this.cracked[i] : this.glowing[i];
				submitNodeCollector.order(2).submitModel(this.getParentModel(), state, poseStack, RenderTypes.eyes(glow),
						lightCoords, OverlayTexture.NO_OVERLAY, state.outlineColor);
			}
		}
	}
}
