package com.dmaryanskiy.vanillareforged.client;

import com.dmaryanskiy.vanillareforged.ModEntities;
import com.dmaryanskiy.vanillareforged.ModItems;
import com.dmaryanskiy.vanillareforged.client.sentry.HeartwoodSentryModel;
import com.dmaryanskiy.vanillareforged.client.sentry.HeartwoodSentryRenderer;
import com.dmaryanskiy.vanillareforged.client.watcher.WatcherModel;
import com.dmaryanskiy.vanillareforged.client.watcher.WatcherRenderer;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.rendering.v1.ArmorRenderer;
import net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.ModelLayerRegistry;
import net.minecraft.client.renderer.entity.ThrownItemRenderer;

public class VanillaReforgedClient implements ClientModInitializer {
	@Override
	public void onInitializeClient() {
		ModelLayerRegistry.registerModelLayer(PaleMaskRenderer.LAYER, PaleMaskModel::createLayer);
		ArmorRenderer.register(PaleMaskRenderer::new, ModItems.PALE_MASK);

		ModelLayerRegistry.registerModelLayer(HeartwoodSentryRenderer.LAYER, HeartwoodSentryModel::createBodyLayer);
		EntityRendererRegistry.register(ModEntities.HEARTWOOD_SENTRY, HeartwoodSentryRenderer::new);

		ModelLayerRegistry.registerModelLayer(WatcherRenderer.LAYER, WatcherModel::createBodyLayer);
		EntityRendererRegistry.register(ModEntities.WATCHER, WatcherRenderer::new);
		EntityRendererRegistry.register(ModEntities.BURNING_RESIN, context -> new ThrownItemRenderer<>(context, 1.5F, true));

		PetrificationOverlay.register();
	}
}
