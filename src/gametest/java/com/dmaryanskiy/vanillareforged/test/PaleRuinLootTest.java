package com.dmaryanskiy.vanillareforged.test;

import java.util.List;

import com.dmaryanskiy.vanillareforged.ModItems;
import com.dmaryanskiy.vanillareforged.VanillaReforged;

import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceKey;
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
