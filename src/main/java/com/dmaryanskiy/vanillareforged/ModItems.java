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
	public static final Item GAZE_POTTERY_SHERD = register("gaze_pottery_sherd",
			Item::new, new Item.Properties().rarity(Rarity.UNCOMMON).potPattern(ModPotPatterns.GAZE));
	public static final Item CREAKING_POTTERY_SHERD = register("creaking_pottery_sherd",
			Item::new, new Item.Properties().rarity(Rarity.UNCOMMON).potPattern(ModPotPatterns.CREAKING));

	private ModItems() {
	}

	public static void initialize() {
		CreativeModeTabEvents.modifyOutputEvent(CreativeModeTabs.INGREDIENTS).register(output -> {
			output.insertAfter(Items.SNORT_POTTERY_SHERD, GAZE_POTTERY_SHERD, CREAKING_POTTERY_SHERD);
			output.insertAfter(Items.BOLT_ARMOR_TRIM_SMITHING_TEMPLATE, WATCHER_ARMOR_TRIM_SMITHING_TEMPLATE);
		});
	}

	private static Item register(String name, Function<Item.Properties, Item> factory, Item.Properties properties) {
		ResourceKey<Item> key = ResourceKey.create(Registries.ITEM, VanillaReforged.id(name));
		return Registry.register(BuiltInRegistries.ITEM, key, factory.apply(properties.setId(key)));
	}
}
