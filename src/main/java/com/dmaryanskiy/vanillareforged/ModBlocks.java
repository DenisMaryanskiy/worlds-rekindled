package com.dmaryanskiy.vanillareforged;

import java.util.function.Function;

import net.fabricmc.fabric.api.creativetab.v1.CreativeModeTabEvents;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;

public final class ModBlocks {
	// The Elderwood's trophy, the heart that broke last, rimmed in the copper of the collar it was woken
	// with: one per tier. Decorative only, and they never weather.
	public static final Block ELDERWOOD_CORE = core("elderwood_core");
	public static final Block EXPOSED_ELDERWOOD_CORE = core("exposed_elderwood_core");
	public static final Block WEATHERED_ELDERWOOD_CORE = core("weathered_elderwood_core");
	public static final Block OXIDIZED_ELDERWOOD_CORE = core("oxidized_elderwood_core");

	private ModBlocks() {
	}

	public static void initialize() {
		CreativeModeTabEvents.modifyOutputEvent(CreativeModeTabs.NATURAL_BLOCKS).register(output ->
				output.insertAfter(Items.CREAKING_HEART, ELDERWOOD_CORE, EXPOSED_ELDERWOOD_CORE, WEATHERED_ELDERWOOD_CORE,
						OXIDIZED_ELDERWOOD_CORE));
	}

	private static Block core(String name) {
		return register(name, Block::new, BlockBehaviour.Properties.of()
				.mapColor(MapColor.COLOR_ORANGE)
				.strength(5.0F)
				.sound(SoundType.CREAKING_HEART)
				.lightLevel(state -> 5), new Item.Properties().rarity(Rarity.RARE));
	}

	private static Block register(String name, Function<BlockBehaviour.Properties, Block> factory, BlockBehaviour.Properties properties,
			Item.Properties itemProperties) {
		ResourceKey<Block> key = ResourceKey.create(Registries.BLOCK, VanillaReforged.id(name));
		Block block = Registry.register(BuiltInRegistries.BLOCK, key, factory.apply(properties.setId(key)));
		ResourceKey<Item> itemKey = ResourceKey.create(Registries.ITEM, VanillaReforged.id(name));
		Registry.register(BuiltInRegistries.ITEM, itemKey, new BlockItem(block, itemProperties.useBlockDescriptionPrefix().setId(itemKey)));
		return block;
	}
}
