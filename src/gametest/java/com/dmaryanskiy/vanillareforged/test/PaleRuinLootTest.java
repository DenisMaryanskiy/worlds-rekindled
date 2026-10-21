package com.dmaryanskiy.vanillareforged.test;

import java.util.List;

import com.dmaryanskiy.vanillareforged.ModItems;
import com.dmaryanskiy.vanillareforged.VanillaReforged;
import com.dmaryanskiy.vanillareforged.sentry.RuinSentry;

import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceKey;
import net.minecraft.tags.PaintingVariantTags;
import net.minecraft.tags.TagKey;
import net.minecraft.tags.VillagerTradeTags;
import net.minecraft.world.entity.decoration.painting.PaintingVariant;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.phys.Vec3;

public class PaleRuinLootTest {
	private static final ResourceKey<PaintingVariant> PAINTING = ResourceKey.create(Registries.PAINTING_VARIANT, VanillaReforged.id("elderwood"));
	private static final int CHESTS = 30;

	@GameTest
	public void theHallsChestAlwaysHoldsAHeartAndTwoLensShards(GameTestHelper helper) {
		for (int seed = 0; seed < CHESTS; seed++) {
			List<ItemStack> loot = roll(helper, "chests/pale_ruin/hall", seed);

			helper.assertValueEqual(count(loot, Items.CREAKING_HEART), 1, "Creaking Hearts in chest " + seed);
			helper.assertTrue(count(loot, ModItems.ELDERWOOD_LENS_SHARD) >= 2, "The chest should hold two lens shards");
		}
		helper.succeed();
	}

	@GameTest
	public void theHallsChestHoldsValuablesAndUsuallyADiamond(GameTestHelper helper) {
		List<Item> valuables = List.of(Items.IRON_INGOT, Items.GOLD_INGOT, Items.DIAMOND, Items.EMERALD, Items.GOLDEN_APPLE,
				Items.DIAMOND_PICKAXE, Items.DIAMOND_SWORD, Items.ENCHANTED_GOLDEN_APPLE);
		int chests = 300;
		int withADiamond = 0;
		for (int seed = 0; seed < chests; seed++) {
			List<ItemStack> loot = roll(helper, "chests/pale_ruin/hall", seed);
			helper.assertTrue(loot.stream().filter(stack -> valuables.contains(stack.getItem())).count() >= 2,
					"The hall's chest should hold at least two valuables, not " + loot);
			for (ItemStack stack : loot) {
				if (stack.is(Items.DIAMOND_PICKAXE) || stack.is(Items.DIAMOND_SWORD)) {
					helper.assertTrue(stack.isEnchanted(), "A diamond tool in the hall's chest should be enchanted");
				}
			}
			withADiamond += count(loot, Items.DIAMOND) > 0 ? 1 : 0;
		}
		// Two chests in three, from a pool of its own.
		helper.assertTrue(withADiamond > chests * 0.55 && withADiamond < chests * 0.8,
				"About two hall chests in three should hold a diamond, not " + withADiamond + " of " + chests);
		helper.succeed();
	}

	@GameTest
	public void theToolChestAlwaysHoldsAWornSilkTouchAxeAndABrush(GameTestHelper helper) {
		var silkTouch = helper.getLevel().registryAccess().lookupOrThrow(Registries.ENCHANTMENT).getOrThrow(Enchantments.SILK_TOUCH);
		for (int seed = 0; seed < CHESTS; seed++) {
			List<ItemStack> loot = roll(helper, "chests/pale_ruin/tool_chest", seed);
			List<ItemStack> axes = loot.stream().filter(stack -> stack.is(Items.IRON_AXE)).toList();
			helper.assertValueEqual(count(loot, Items.BRUSH), 1, "brushes in chest " + seed);

			helper.assertValueEqual(axes.size(), 1, "axes in chest " + seed);
			ItemStack axe = axes.getFirst();
			helper.assertValueEqual(EnchantmentHelper.getItemEnchantmentLevel(silkTouch, axe), 1, "Silk Touch level");
			helper.assertTrue(axe.isDamaged(), "The builders' axe should be worn");
		}
		helper.succeed();
	}

	@GameTest
	public void theStudysChestAlwaysHoldsAnEnchantedBook(GameTestHelper helper) {
		for (int seed = 0; seed < CHESTS; seed++) {
			helper.assertValueEqual(count(roll(helper, "chests/pale_ruin/study", seed), Items.ENCHANTED_BOOK), 1, "enchanted books in chest " + seed);
		}
		helper.succeed();
	}

	@GameTest
	public void theTrimsHeapAlwaysHoldsTheTrimAndNoOtherHeapDoes(GameTestHelper helper) {
		for (int seed = 0; seed < CHESTS; seed++) {
			helper.assertValueEqual(count(roll(helper, "archaeology/pale_ruin_hall_trim", seed), ModItems.ELDERWOOD_ARMOR_TRIM_SMITHING_TEMPLATE), 1,
					"trims in heap " + seed);
			for (String table : List.of("archaeology/pale_ruin", "archaeology/pale_ruin_tomb", "archaeology/pale_ruin_hall")) {
				helper.assertValueEqual(count(roll(helper, table, seed), ModItems.ELDERWOOD_ARMOR_TRIM_SMITHING_TEMPLATE), 0, "trims from " + table);
			}
		}
		helper.succeed();
	}

	@GameTest
	public void everyOtherTableOfTheRuinGivesSomething(GameTestHelper helper) {
		for (String table : List.of("chests/pale_ruin/store_room", "chests/pale_ruin/sentry_workshop", "pots/pale_ruin", "archaeology/pale_ruin",
				"archaeology/pale_ruin_tomb", "archaeology/pale_ruin_hall")) {
			helper.assertFalse(roll(helper, table, 0).isEmpty(), table + " should give something");
		}
		helper.succeed();
	}

	@GameTest
	public void theGazeBannerPatternAndThePaintingAreFoundOnlyInTheRuin(GameTestHelper helper) {
		var registries = helper.getLevel().registryAccess();
		var gaze = registries.lookupOrThrow(Registries.BANNER_PATTERN).getOrThrow(ResourceKey.create(Registries.BANNER_PATTERN, VanillaReforged.id("gaze")));
		var provided = new ItemStack(ModItems.GAZE_BANNER_PATTERN).get(DataComponents.PROVIDES_BANNER_PATTERNS);
		helper.assertTrue(provided != null && provided.contains(gaze), "The pattern item should put the Gaze on a loom");
		var painting = registries.lookupOrThrow(Registries.PAINTING_VARIANT).getOrThrow(PAINTING);
		helper.assertFalse(painting.is(PaintingVariantTags.PLACEABLE), "The painting shouldn't turn up when a painting is hung at random");

		int chests = 300;
		int patternsInTheStudy = 0;
		int patternsInTheHall = 0;
		int paintings = 0;
		for (int seed = 0; seed < chests; seed++) {
			patternsInTheStudy += count(roll(helper, "chests/pale_ruin/study", seed), ModItems.GAZE_BANNER_PATTERN);
			List<ItemStack> hall = roll(helper, "chests/pale_ruin/hall", seed);
			patternsInTheHall += count(hall, ModItems.GAZE_BANNER_PATTERN);
			for (ItemStack stack : hall) {
				if (stack.is(Items.PAINTING)) {
					helper.assertTrue(stack.get(DataComponents.PAINTING_VARIANT).is(PAINTING), "The hall's painting should be the Elderwood");
					paintings++;
				}
			}
		}
		// One time in three, one in four and one in three.
		helper.assertTrue(patternsInTheStudy > chests * 0.23 && patternsInTheStudy < chests * 0.43, "patterns in the study: " + patternsInTheStudy);
		helper.assertTrue(patternsInTheHall > chests * 0.17 && patternsInTheHall < chests * 0.33, "patterns in the hall: " + patternsInTheHall);
		helper.assertTrue(paintings > chests * 0.23 && paintings < chests * 0.43, "paintings in the hall: " + paintings);
		helper.succeed();
	}

	@GameTest
	public void journeymanCartographersSellAMapToThePaleRuin(GameTestHelper helper) {
		var trade = helper.getLevel().registryAccess().lookupOrThrow(Registries.VILLAGER_TRADE)
				.get(ResourceKey.create(Registries.VILLAGER_TRADE, VanillaReforged.id("cartographer/3/emerald_and_compass_pale_ruin_map")));
		helper.assertTrue(trade.isPresent(), "The Pale Ruin map trade should load");
		helper.assertTrue(trade.get().is(VillagerTradeTags.CARTOGRAPHER_LEVEL_3), "A Journeyman Cartographer should offer it");
		helper.assertTrue(helper.getLevel().registryAccess().lookupOrThrow(Registries.STRUCTURE).getOrThrow(RuinSentry.RUIN)
				.is(TagKey.create(Registries.STRUCTURE, VanillaReforged.id("on_pale_ruin_maps"))), "The map should lead to a Pale Ruin");
		helper.succeed();
	}

	@GameTest
	public void buriedGardenIsAnAdvancement(GameTestHelper helper) {
		helper.assertTrue(helper.getLevel().getServer().getAdvancements().get(VanillaReforged.id("adventure/buried_garden")) != null,
				"Buried Garden should exist");
		helper.succeed();
	}

	private static List<ItemStack> roll(GameTestHelper helper, String table, long seed) {
		LootTable lootTable = helper.getLevel().getServer().reloadableRegistries()
				.getLootTable(ResourceKey.create(Registries.LOOT_TABLE, VanillaReforged.id(table)));
		LootParams params = new LootParams.Builder(helper.getLevel())
				.withParameter(LootContextParams.ORIGIN, Vec3.atCenterOf(helper.absolutePos(net.minecraft.core.BlockPos.ZERO)))
				.create(LootContextParamSets.CHEST);
		return lootTable.getRandomItems(params, seed);
	}

	private static int count(List<ItemStack> loot, Item item) {
		return loot.stream().filter(stack -> stack.is(item)).mapToInt(ItemStack::getCount).sum();
	}
}
