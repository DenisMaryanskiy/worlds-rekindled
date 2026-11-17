package com.dmaryanskiy.vanillareforged.client.sentry;

import java.util.List;

import com.dmaryanskiy.vanillareforged.ModItems;
import com.dmaryanskiy.vanillareforged.VanillaReforged;
import com.dmaryanskiy.vanillareforged.sentry.HeartwoodSentry;

import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;

public class HeartwoodSentryRenderer extends MobRenderer<HeartwoodSentry, HeartwoodSentryRenderState, HeartwoodSentryModel> {
	public static final ModelLayerLocation LAYER = new ModelLayerLocation(VanillaReforged.id("heartwood_sentry"), "main");
	private static final Identifier CARVED = texture("heartwood_sentry");
	private static final Identifier JACK_O_LANTERN = texture("heartwood_sentry_jack_o_lantern");
	private static final Identifier STATUE = texture("heartwood_sentry_statue");

	// Only the heart (or the face) is opaque in these textures.
	static final List<Identifier> HEART_GLOW = List.of(texture("heartwood_sentry_heart"),
			texture("heartwood_sentry_heart_cracked_1"), texture("heartwood_sentry_heart_cracked_2"),
			texture("heartwood_sentry_heart_cracked_3"), texture("heartwood_sentry_heart_cracked_4"));
	private static final Identifier JACK_O_LANTERN_GLOW = texture("heartwood_sentry_jack_o_lantern_glow");
	static final List<Identifier> CRACKS = List.of(texture("heartwood_sentry_cracks_1"), texture("heartwood_sentry_cracks_2"),
			texture("heartwood_sentry_cracks_3"), texture("heartwood_sentry_cracks_4"));
	private ItemStack lensItem = ItemStack.EMPTY;

	public HeartwoodSentryRenderer(EntityRendererProvider.Context context) {
		super(context, new HeartwoodSentryModel(context.bakeLayer(LAYER)), 0.6F);
		this.addLayer(new SentryHeartCracksLayer(this, CRACKS));
		this.addLayer(new SentryGlowLayer(this, HEART_GLOW, JACK_O_LANTERN_GLOW));
		this.addLayer(new SentryLensLayer(this));
	}

	static Identifier texture(String name) {
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
		state.glowingFace = entity.isJackOLantern();
		state.cracks = entity.getCracks();
		state.canMove = !state.statue;
		if (entity.isLensed()) {
			// Create it lazily: renderers are made before item components are bound.
			if (this.lensItem.isEmpty()) {
				this.lensItem = new ItemStack(ModItems.ELDERWOOD_LENS);
			}
			this.itemModelResolver.updateForLiving(state.lens, this.lensItem, ItemDisplayContext.FIXED, entity);
		} else {
			state.lens.clear();
		}
	}

	@Override
	public Identifier getTextureLocation(HeartwoodSentryRenderState state) {
		return state.statue ? STATUE : state.glowingFace ? JACK_O_LANTERN : CARVED;
	}
}
