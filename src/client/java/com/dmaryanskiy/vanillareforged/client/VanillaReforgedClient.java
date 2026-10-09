package com.dmaryanskiy.vanillareforged.client;

import com.dmaryanskiy.vanillareforged.ModEntities;
import com.dmaryanskiy.vanillareforged.ModItems;
import com.dmaryanskiy.vanillareforged.client.sentry.HeartwoodSentryModel;
import com.dmaryanskiy.vanillareforged.client.sentry.HeartwoodSentryRenderer;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.rendering.v1.ArmorRenderer;
import net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.ModelLayerRegistry;

public class VanillaReforgedClient implements ClientModInitializer {
	@Override
	public void onInitializeClient() {
		ModelLayerRegistry.registerModelLayer(PaleMaskRenderer.LAYER, PaleMaskModel::createLayer);
		ArmorRenderer.register(PaleMaskRenderer::new, ModItems.PALE_MASK);

		ModelLayerRegistry.registerModelLayer(HeartwoodSentryRenderer.LAYER, HeartwoodSentryModel::createBodyLayer);
		EntityRendererRegistry.register(ModEntities.HEARTWOOD_SENTRY, HeartwoodSentryRenderer::new);

		PetrificationOverlay.register();
	}
}
