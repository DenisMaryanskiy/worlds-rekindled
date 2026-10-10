package com.dmaryanskiy.vanillareforged.client.watcher;

import java.util.EnumMap;
import java.util.Locale;
import java.util.Map;

import com.dmaryanskiy.vanillareforged.VanillaReforged;
import com.dmaryanskiy.vanillareforged.watcher.Watcher;
import com.mojang.blaze3d.vertex.PoseStack;

import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.block.WeatheringCopper;

public class WatcherRenderer extends MobRenderer<Watcher, WatcherRenderState, WatcherModel> {
	public static final ModelLayerLocation LAYER = new ModelLayerLocation(VanillaReforged.id("watcher"), "main");
	public static final float SCALE = 1.4F;
	private static final Map<WeatheringCopper.WeatherState, Identifier> TEXTURES = new EnumMap<>(WeatheringCopper.WeatherState.class);
	// Only the lens is opaque in this texture.
	private static final Identifier LENS = texture("watcher_lens");

	static {
		for (WeatheringCopper.WeatherState stage : WeatheringCopper.WeatherState.values()) {
			TEXTURES.put(stage, texture("watcher_" + stage.name().toLowerCase(Locale.ROOT)));
		}
	}

	public WatcherRenderer(EntityRendererProvider.Context context) {
		super(context, new WatcherModel(context.bakeLayer(LAYER)), 0.9F * SCALE);
		this.addLayer(new WatcherLensLayer(this, LENS));
	}

	private static Identifier texture(String name) {
		return VanillaReforged.id("textures/entity/watcher/" + name + ".png");
	}

	@Override
	public WatcherRenderState createRenderState() {
		return new WatcherRenderState();
	}

	@Override
	public void extractRenderState(Watcher entity, WatcherRenderState state, float partialTicks) {
		super.extractRenderState(entity, state, partialTicks);
		state.attackAnimationState.copyFrom(entity.attackAnimationState);
		state.copperStage = entity.getCopperStage();
		state.canMove = true;
	}

	@Override
	protected void scale(WatcherRenderState state, PoseStack poseStack) {
		poseStack.scale(SCALE, SCALE, SCALE);
	}

	@Override
	public Identifier getTextureLocation(WatcherRenderState state) {
		return TEXTURES.get(state.copperStage);
	}
}
