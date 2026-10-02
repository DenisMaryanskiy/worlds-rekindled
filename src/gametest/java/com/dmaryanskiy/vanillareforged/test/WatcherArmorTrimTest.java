package com.dmaryanskiy.vanillareforged.test;

import java.util.List;

import com.dmaryanskiy.vanillareforged.ModItems;
import com.dmaryanskiy.vanillareforged.VanillaReforged;

import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.crafting.SmithingRecipeInput;
import net.minecraft.world.item.equipment.trim.ArmorTrim;
import net.minecraft.world.item.equipment.trim.TrimPattern;

public class WatcherArmorTrimTest {
	private static final ResourceKey<TrimPattern> WATCHER = ResourceKey.create(Registries.TRIM_PATTERN,
			VanillaReforged.id("watcher"));

	@GameTest
	public void smithingAppliesWatcherTrim(GameTestHelper helper) {
		SmithingRecipeInput input = new SmithingRecipeInput(new ItemStack(ModItems.WATCHER_ARMOR_TRIM_SMITHING_TEMPLATE),
				new ItemStack(Items.IRON_CHESTPLATE), new ItemStack(Items.RESIN_BRICK));

		var recipe = helper.getLevel().getServer().getRecipeManager()
				.getRecipeFor(RecipeType.SMITHING, input, helper.getLevel());

		helper.assertTrue(recipe.isPresent(), "No smithing recipe matched the Watcher template");
		ArmorTrim trim = recipe.get().value().assemble(input).get(DataComponents.TRIM);
		helper.assertTrue(trim != null && trim.pattern().is(WATCHER), "Result should carry the Watcher trim, got " + trim);
		helper.succeed();
	}

	@GameTest
	public void templateDuplicatesWithDiamondsAndResinBlock(GameTestHelper helper) {
		ItemStack diamond = new ItemStack(Items.DIAMOND);
		CraftingInput input = CraftingInput.of(3, 3, List.of(
				diamond, new ItemStack(ModItems.WATCHER_ARMOR_TRIM_SMITHING_TEMPLATE), diamond,
				diamond, new ItemStack(Items.RESIN_BLOCK), diamond,
				diamond, diamond, diamond));

		var recipe = helper.getLevel().getServer().getRecipeManager()
				.getRecipeFor(RecipeType.CRAFTING, input, helper.getLevel());

		helper.assertTrue(recipe.isPresent(), "No crafting recipe matched the Watcher template duplication");
		ItemStack result = recipe.get().value().assemble(input);
		helper.assertValueEqual(result.getItem(), ModItems.WATCHER_ARMOR_TRIM_SMITHING_TEMPLATE, "crafted item");
		helper.assertValueEqual(result.getCount(), 2, "crafted count");
		helper.succeed();
	}
}
