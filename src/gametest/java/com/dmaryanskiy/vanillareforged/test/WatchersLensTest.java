package com.dmaryanskiy.vanillareforged.test;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import com.dmaryanskiy.vanillareforged.ModItems;

import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.RecipeType;

public class WatchersLensTest {
	@GameTest
	public void nineShardsCraftALens(GameTestHelper helper) {
		ItemStack result = craft(helper, Collections.nCopies(9, ModItems.WATCHERS_LENS_SHARD));

		helper.assertValueEqual(result.getItem(), ModItems.WATCHERS_LENS, "crafted item");
		helper.succeed();
	}

	@GameTest
	public void lensSetInResinCraftsACreakingEye(GameTestHelper helper) {
		ItemStack result = craft(helper, grid(
				null, Items.RESIN_CLUMP, null,
				Items.RESIN_CLUMP, ModItems.WATCHERS_LENS, Items.RESIN_CLUMP,
				null, Items.RESIN_CLUMP, null));

		helper.assertValueEqual(result.getItem(), ModItems.CREAKING_EYE, "crafted item");
		helper.assertValueEqual(result.getCount(), 1, "eyes per lens");
		helper.succeed();
	}

	@GameTest
	public void resinInTheCornersDoesNotMakeAnEye(GameTestHelper helper) {
		ItemStack result = craft(helper, grid(
				Items.RESIN_CLUMP, null, Items.RESIN_CLUMP,
				null, ModItems.WATCHERS_LENS, null,
				Items.RESIN_CLUMP, null, Items.RESIN_CLUMP));

		helper.assertTrue(result.isEmpty(), "Only the plus shape should set the lens in resin");
		helper.succeed();
	}

	private static List<Item> grid(Item... items) {
		return Arrays.asList(items);
	}

	private static ItemStack craft(GameTestHelper helper, List<Item> items) {
		CraftingInput input = CraftingInput.of(3, 3, items.stream()
				.map(item -> item == null ? ItemStack.EMPTY : new ItemStack(item))
				.toList());
		return helper.getLevel().getServer().getRecipeManager()
				.getRecipeFor(RecipeType.CRAFTING, input, helper.getLevel())
				.map(recipe -> recipe.value().assemble(input))
				.orElse(ItemStack.EMPTY);
	}
}
