package com.dmaryanskiy.vanillareforged.client.elderwood;

import java.util.EnumMap;
import java.util.Locale;
import java.util.Map;

import com.dmaryanskiy.vanillareforged.VanillaReforged;
import com.dmaryanskiy.vanillareforged.elderwood.Elderwood;
import com.dmaryanskiy.vanillareforged.elderwood.ElderwoodHeart;
import com.mojang.blaze3d.vertex.PoseStack;

import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.block.WeatheringCopper;

public class ElderwoodRenderer extends MobRenderer<Elderwood, ElderwoodRenderState, ElderwoodModel> {
	public static final ModelLayerLocation LAYER = new ModelLayerLocation(VanillaReforged.id("elderwood"), "main");
	private static final Map<WeatheringCopper.WeatherState, Identifier> TEXTURES = new EnumMap<>(WeatheringCopper.WeatherState.class);
	// Only the lens is opaque in this texture.
	private static final Identifier LENS = texture("elderwood_lens");

	static {
		for (WeatheringCopper.WeatherState stage : WeatheringCopper.WeatherState.values()) {
			TEXTURES.put(stage, texture("elderwood_" + stage.name().toLowerCase(Locale.ROOT)));
		}
	}

	public ElderwoodRenderer(EntityRendererProvider.Context context) {
		super(context, new ElderwoodModel(context.bakeLayer(LAYER)), 2.0F);
		this.addLayer(new ElderwoodLensLayer(this, LENS));
		this.addLayer(new ElderwoodHeartsLayer(this));
	}

	private static Identifier texture(String name) {
		return VanillaReforged.id("textures/entity/elderwood/" + name + ".png");
	}

	@Override
	public ElderwoodRenderState createRenderState() {
		return new ElderwoodRenderState();
	}

	@Override
	public void extractRenderState(Elderwood entity, ElderwoodRenderState state, float partialTicks) {
		super.extractRenderState(entity, state, partialTicks);
		state.copperStage = entity.getCopperStage();
		for (ElderwoodHeart heart : ElderwoodHeart.values()) {
			state.heartCracks[heart.ordinal()] = entity.getCracks(heart);
			state.heartVulnerable[heart.ordinal()] = entity.isVulnerable(heart);
		}
		state.coreOpen = entity.isCoreOpen();
		state.slamPhase = entity.getSlamPhase();
		state.slamRightBranch = entity.isSlammingRightBranch();
		state.slamPhaseAge = entity.getSlamPhaseAge() + partialTicks;
		state.stunned = entity.isStunned();
		state.growth = entity.getGrowth(partialTicks);
	}

	@Override
	protected void scale(ElderwoodRenderState state, PoseStack poseStack) {
		float size = Math.max(state.growth, 0.02F);
		poseStack.scale(size, size, size);
	}

	@Override
	public Identifier getTextureLocation(ElderwoodRenderState state) {
		return TEXTURES.get(state.copperStage);
	}
}
