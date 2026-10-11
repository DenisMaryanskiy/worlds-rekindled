package com.dmaryanskiy.vanillareforged.client.sentry;

import com.dmaryanskiy.vanillareforged.sentry.RuinSentry;

import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.resources.Identifier;

/** The Heartwood Sentry's model and heart with a bark head: its one eye, a lens shard, always glows. */
public class RuinSentryRenderer extends MobRenderer<RuinSentry, HeartwoodSentryRenderState, HeartwoodSentryModel> {
	private static final Identifier TEXTURE = HeartwoodSentryRenderer.texture("ruin_sentry");
	private static final Identifier EYE_GLOW = HeartwoodSentryRenderer.texture("ruin_sentry_eye_glow");

	public RuinSentryRenderer(EntityRendererProvider.Context context) {
		super(context, new HeartwoodSentryModel(context.bakeLayer(HeartwoodSentryRenderer.LAYER)), 0.6F);
		this.addLayer(new SentryHeartCracksLayer(this, HeartwoodSentryRenderer.CRACKS));
		this.addLayer(new SentryGlowLayer(this, HeartwoodSentryRenderer.HEART_GLOW, EYE_GLOW));
	}

	@Override
	public HeartwoodSentryRenderState createRenderState() {
		return new HeartwoodSentryRenderState();
	}

	@Override
	public void extractRenderState(RuinSentry entity, HeartwoodSentryRenderState state, float partialTicks) {
		super.extractRenderState(entity, state, partialTicks);
		state.attackAnimationState.copyFrom(entity.attackAnimationState);
		state.statue = entity.isStatue();
		state.glowingFace = true;
		state.cracks = entity.getCracks();
		state.canMove = !state.statue;
	}

	@Override
	public Identifier getTextureLocation(HeartwoodSentryRenderState state) {
		return TEXTURE;
	}
}
