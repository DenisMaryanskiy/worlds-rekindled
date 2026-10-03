package com.dmaryanskiy.vanillareforged.client;

import com.dmaryanskiy.vanillareforged.ModItems;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.rendering.v1.ArmorRenderer;
import net.fabricmc.fabric.api.client.rendering.v1.ModelLayerRegistry;

public class VanillaReforgedClient implements ClientModInitializer {
	@Override
	public void onInitializeClient() {
		ModelLayerRegistry.registerModelLayer(PaleMaskRenderer.LAYER, PaleMaskModel::createLayer);
		ArmorRenderer.register(PaleMaskRenderer::new, ModItems.PALE_MASK);
	}
}
