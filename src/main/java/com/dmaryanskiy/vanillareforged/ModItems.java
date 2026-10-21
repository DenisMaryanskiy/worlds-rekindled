package com.dmaryanskiy.vanillareforged;

import java.util.function.Function;

import net.fabricmc.fabric.api.creativetab.v1.CreativeModeTabEvents;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.MapItem;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.SmithingTemplateItem;
import net.minecraft.world.item.SpawnEggItem;
import net.minecraft.world.item.component.MapDecorations;
import net.minecraft.world.item.equipment.ArmorType;
import net.minecraft.world.level.saveddata.maps.MapDecorationType;

public final class ModItems {
	public static final Item ELDERWOOD_ARMOR_TRIM_SMITHING_TEMPLATE = register("elderwood_armor_trim_smithing_template",
			SmithingTemplateItem::createArmorTrimTemplate, new Item.Properties().rarity(Rarity.RARE));
	public static final Item GAZE_POTTERY_SHERD = register("gaze_pottery_sherd",
			Item::new, new Item.Properties().rarity(Rarity.UNCOMMON).potPattern(ModPotPatterns.GAZE));
	public static final Item CREAKING_POTTERY_SHERD = register("creaking_pottery_sherd",
			Item::new, new Item.Properties().rarity(Rarity.UNCOMMON).potPattern(ModPotPatterns.CREAKING));
	public static final Item GUARDIAN_POTTERY_SHERD = register("guardian_pottery_sherd",
			Item::new, new Item.Properties().rarity(Rarity.UNCOMMON).potPattern(ModPotPatterns.GUARDIAN));
	// Found in the Pale Ruin, as the Flow and Guster patterns are in the Trial Chambers: the lens's eye.
	public static final Item GAZE_BANNER_PATTERN = register("gaze_banner_pattern", Item::new, new Item.Properties()
			.stacksTo(1)
			.rarity(Rarity.RARE)
			.delayedComponent(DataComponents.PROVIDES_BANNER_PATTERNS,
					registries -> registries.getOrThrow(TagKey.create(Registries.BANNER_PATTERN, VanillaReforged.id("pattern_item/gaze")))));
	public static final Item ELDERWOOD_LENS_SHARD = register("elderwood_lens_shard",
			Item::new, new Item.Properties().rarity(Rarity.UNCOMMON));
	public static final Item ELDERWOOD_LENS = register("elderwood_lens",
			Item::new, new Item.Properties().rarity(Rarity.RARE));
	public static final Item CREAKING_EYE = register("creaking_eye",
			Item::new, new Item.Properties().rarity(Rarity.RARE));
	public static final Item PALE_MASK = register("pale_mask",
			Item::new, new Item.Properties().rarity(Rarity.UNCOMMON).humanoidArmor(ModArmorMaterials.PALE_MASK, ArmorType.HELMET));
	// Not in any creative tab: it only exists as the look of the Elderwood's projectile, like the fire charge
	// is for fireballs.
	public static final Item BURNING_RESIN = register("burning_resin", Item::new, new Item.Properties());
	// Only from a Cartographer, as the other explorer maps: one that points at the nearest Pale Ruin.
	public static final Item PALE_RUIN_MAP = register("pale_ruin_map",
			MapItem::new, new Item.Properties().component(DataComponents.MAP_DECORATIONS, MapDecorations.EMPTY));
	public static final Item HEARTWOOD_SENTRY_SPAWN_EGG = register("heartwood_sentry_spawn_egg",
			SpawnEggItem::new, new Item.Properties().spawnEgg(ModEntities.HEARTWOOD_SENTRY));
	public static final Item RUIN_SENTRY_SPAWN_EGG = register("ruin_sentry_spawn_egg",
			SpawnEggItem::new, new Item.Properties().spawnEgg(ModEntities.RUIN_SENTRY));

	/** The Pale Ruin's mark on an explorer map. */
	public static final Holder<MapDecorationType> PALE_RUIN_DECORATION = Registry.registerForHolder(BuiltInRegistries.MAP_DECORATION_TYPE,
			VanillaReforged.id("pale_ruin"), new MapDecorationType(VanillaReforged.id("pale_ruin"), true, false));

	private ModItems() {
	}

	public static void initialize() {
		CreativeModeTabEvents.modifyOutputEvent(CreativeModeTabs.INGREDIENTS).register(output -> {
			output.insertAfter(Items.SNORT_POTTERY_SHERD, GAZE_POTTERY_SHERD, CREAKING_POTTERY_SHERD, GUARDIAN_POTTERY_SHERD);
			output.insertAfter(Items.BOLT_ARMOR_TRIM_SMITHING_TEMPLATE, ELDERWOOD_ARMOR_TRIM_SMITHING_TEMPLATE);
			output.insertAfter(Items.GUSTER_BANNER_PATTERN, GAZE_BANNER_PATTERN);
			output.insertAfter(Items.RESIN_BRICK, ELDERWOOD_LENS_SHARD, ELDERWOOD_LENS, CREAKING_EYE);
		});
		CreativeModeTabEvents.modifyOutputEvent(CreativeModeTabs.COMBAT).register(output ->
				output.insertAfter(Items.TURTLE_HELMET, PALE_MASK));
		CreativeModeTabEvents.modifyOutputEvent(CreativeModeTabs.SPAWN_EGGS).register(output ->
				output.insertAfter(Items.IRON_GOLEM_SPAWN_EGG, HEARTWOOD_SENTRY_SPAWN_EGG, RUIN_SENTRY_SPAWN_EGG));
	}

	private static Item register(String name, Function<Item.Properties, Item> factory, Item.Properties properties) {
		ResourceKey<Item> key = ResourceKey.create(Registries.ITEM, VanillaReforged.id(name));
		return Registry.register(BuiltInRegistries.ITEM, key, factory.apply(properties.setId(key)));
	}
}
