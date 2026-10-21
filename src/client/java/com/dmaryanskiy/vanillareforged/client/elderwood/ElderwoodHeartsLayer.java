package com.dmaryanskiy.vanillareforged.client.elderwood;

import java.util.Locale;

import com.dmaryanskiy.vanillareforged.VanillaReforged;
import com.dmaryanskiy.vanillareforged.elderwood.ElderwoodHeart;
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
public class ElderwoodHeartsLayer extends RenderLayer<ElderwoodRenderState, ElderwoodModel> {
	private static final Identifier CORE_OPEN = texture("core_open");

	private final Identifier[] glowing = new Identifier[ElderwoodHeart.ALL.size()];
	private final Identifier[] cracked = new Identifier[ElderwoodHeart.ALL.size()];
	private final Identifier[] broken = new Identifier[ElderwoodHeart.ALL.size()];

	public ElderwoodHeartsLayer(RenderLayerParent<ElderwoodRenderState, ElderwoodModel> renderer) {
		super(renderer);
		for (ElderwoodHeart heart : ElderwoodHeart.ALL) {
			String name = heart.name().toLowerCase(Locale.ROOT);
			this.glowing[heart.ordinal()] = texture(name);
			this.cracked[heart.ordinal()] = texture(name + "_cracked");
			this.broken[heart.ordinal()] = texture(name + "_broken");
		}
	}

	private static Identifier texture(String name) {
		return VanillaReforged.id("textures/entity/elderwood/heart/" + name + ".png");
	}

	@Override
	public void submit(PoseStack poseStack, SubmitNodeCollector submitNodeCollector, int lightCoords,
			ElderwoodRenderState state, float yRot, float xRot) {
		if (state.isInvisible) {
			return;
		}
		for (ElderwoodHeart heart : ElderwoodHeart.ALL) {
			int i = heart.ordinal();
			if (state.heartBroken[i]) {
				renderColoredCutoutModel(this.getParentModel(), this.broken[i], poseStack, submitNodeCollector, lightCoords, state, -1, 1);
				continue;
			}
			if (heart == ElderwoodHeart.CORE && state.coreOpen) {
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
