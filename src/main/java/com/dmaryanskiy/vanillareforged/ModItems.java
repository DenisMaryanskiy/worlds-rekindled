package com.dmaryanskiy.vanillareforged;

import java.util.function.Function;

import net.fabricmc.fabric.api.creativetab.v1.CreativeModeTabEvents;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.SmithingTemplateItem;

public final class ModItems {
	public static final Item WATCHER_ARMOR_TRIM_SMITHING_TEMPLATE = register("watcher_armor_trim_smithing_template",
			SmithingTemplateItem::createArmorTrimTemplate, new Item.Properties().rarity(Rarity.RARE));

	private ModItems() {
	}

	public static void initialize() {
		CreativeModeTabEvents.modifyOutputEvent(CreativeModeTabs.INGREDIENTS).register(output ->
				output.insertAfter(Items.BOLT_ARMOR_TRIM_SMITHING_TEMPLATE, WATCHER_ARMOR_TRIM_SMITHING_TEMPLATE));
	}

	private static Item register(String name, Function<Item.Properties, Item> factory, Item.Properties properties) {
		ResourceKey<Item> key = ResourceKey.create(Registries.ITEM, VanillaReforged.id(name));
		return Registry.register(BuiltInRegistries.ITEM, key, factory.apply(properties.setId(key)));
	}
}
