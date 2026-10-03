package com.dmaryanskiy.vanillareforged.client.sentry;

import com.dmaryanskiy.vanillareforged.VanillaReforged;
import com.dmaryanskiy.vanillareforged.sentry.HeartwoodSentry;

import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.resources.Identifier;

public class HeartwoodSentryRenderer extends MobRenderer<HeartwoodSentry, HeartwoodSentryRenderState, HeartwoodSentryModel> {
	public static final ModelLayerLocation LAYER = new ModelLayerLocation(VanillaReforged.id("heartwood_sentry"), "main");
	private static final Identifier CARVED = texture("heartwood_sentry");
	private static final Identifier JACK_O_LANTERN = texture("heartwood_sentry_jack_o_lantern");
	private static final Identifier STATUE = texture("heartwood_sentry_statue");

	// Only the heart is opaque in this texture.
	private static final Identifier HEART = texture("heartwood_sentry_heart");

	public HeartwoodSentryRenderer(EntityRendererProvider.Context context) {
		super(context, new HeartwoodSentryModel(context.bakeLayer(LAYER)), 0.6F);
		this.addLayer(new SentryHeartGlowLayer(this, HEART));
	}

	private static Identifier texture(String name) {
		return VanillaReforged.id("textures/entity/heartwood_sentry/" + name + ".png");
	}

	@Override
	public HeartwoodSentryRenderState createRenderState() {
		return new HeartwoodSentryRenderState();
	}

	@Override
	public void extractRenderState(HeartwoodSentry entity, HeartwoodSentryRenderState state, float partialTicks) {
		super.extractRenderState(entity, state, partialTicks);
		state.attackAnimationState.copyFrom(entity.attackAnimationState);
		state.statue = entity.isStatue();
		state.jackOLantern = entity.isJackOLantern();
		state.canMove = !state.statue;
	}

	@Override
	public Identifier getTextureLocation(HeartwoodSentryRenderState state) {
		return state.statue ? STATUE : state.jackOLantern ? JACK_O_LANTERN : CARVED;
	}
}
