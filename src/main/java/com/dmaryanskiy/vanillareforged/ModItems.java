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
import net.minecraft.world.item.SpawnEggItem;
import net.minecraft.world.item.equipment.ArmorType;

public final class ModItems {
	public static final Item WATCHER_ARMOR_TRIM_SMITHING_TEMPLATE = register("watcher_armor_trim_smithing_template",
			SmithingTemplateItem::createArmorTrimTemplate, new Item.Properties().rarity(Rarity.RARE));
	public static final Item GAZE_POTTERY_SHERD = register("gaze_pottery_sherd",
			Item::new, new Item.Properties().rarity(Rarity.UNCOMMON).potPattern(ModPotPatterns.GAZE));
	public static final Item CREAKING_POTTERY_SHERD = register("creaking_pottery_sherd",
			Item::new, new Item.Properties().rarity(Rarity.UNCOMMON).potPattern(ModPotPatterns.CREAKING));
	public static final Item WATCHERS_LENS_SHARD = register("watchers_lens_shard",
			Item::new, new Item.Properties().rarity(Rarity.UNCOMMON));
	public static final Item WATCHERS_LENS = register("watchers_lens",
			Item::new, new Item.Properties().rarity(Rarity.RARE));
	public static final Item CREAKING_EYE = register("creaking_eye",
			Item::new, new Item.Properties().rarity(Rarity.RARE));
	public static final Item PALE_MASK = register("pale_mask",
			Item::new, new Item.Properties().rarity(Rarity.UNCOMMON).humanoidArmor(ModArmorMaterials.PALE_MASK, ArmorType.HELMET));
	// Not in any creative tab: it only exists as the look of the Watcher's projectile, like the fire charge
	// is for fireballs.
	public static final Item BURNING_RESIN = register("burning_resin", Item::new, new Item.Properties());
	public static final Item HEARTWOOD_SENTRY_SPAWN_EGG = register("heartwood_sentry_spawn_egg",
			SpawnEggItem::new, new Item.Properties().spawnEgg(ModEntities.HEARTWOOD_SENTRY));

	private ModItems() {
	}

	public static void initialize() {
		CreativeModeTabEvents.modifyOutputEvent(CreativeModeTabs.INGREDIENTS).register(output -> {
			output.insertAfter(Items.SNORT_POTTERY_SHERD, GAZE_POTTERY_SHERD, CREAKING_POTTERY_SHERD);
			output.insertAfter(Items.BOLT_ARMOR_TRIM_SMITHING_TEMPLATE, WATCHER_ARMOR_TRIM_SMITHING_TEMPLATE);
			output.insertAfter(Items.RESIN_BRICK, WATCHERS_LENS_SHARD, WATCHERS_LENS, CREAKING_EYE);
		});
		CreativeModeTabEvents.modifyOutputEvent(CreativeModeTabs.COMBAT).register(output ->
				output.insertAfter(Items.TURTLE_HELMET, PALE_MASK));
		CreativeModeTabEvents.modifyOutputEvent(CreativeModeTabs.SPAWN_EGGS).register(output ->
				output.insertAfter(Items.IRON_GOLEM_SPAWN_EGG, HEARTWOOD_SENTRY_SPAWN_EGG));
	}

	private static Item register(String name, Function<Item.Properties, Item> factory, Item.Properties properties) {
		ResourceKey<Item> key = ResourceKey.create(Registries.ITEM, VanillaReforged.id(name));
		return Registry.register(BuiltInRegistries.ITEM, key, factory.apply(properties.setId(key)));
	}
}
