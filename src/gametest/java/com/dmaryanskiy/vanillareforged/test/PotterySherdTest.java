package com.dmaryanskiy.vanillareforged.test;

import java.util.List;
import java.util.Optional;

import com.dmaryanskiy.vanillareforged.ModItems;
import com.dmaryanskiy.vanillareforged.ModPotPatterns;

import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.block.entity.DecoratedPotPattern;
import net.minecraft.world.level.block.entity.PotDecorations;

public class PotterySherdTest {
	@GameTest
	public void gazeSherdProvidesGazePattern(GameTestHelper helper) {
		assertProvidesPattern(helper, ModItems.GAZE_POTTERY_SHERD, ModPotPatterns.GAZE);
		helper.succeed();
	}

	@GameTest
	public void creakingSherdProvidesCreakingPattern(GameTestHelper helper) {
		assertProvidesPattern(helper, ModItems.CREAKING_POTTERY_SHERD, ModPotPatterns.CREAKING);
		helper.succeed();
	}

	@GameTest
	public void sherdDecoratesCraftedPot(GameTestHelper helper) {
		ItemStack brick = new ItemStack(Items.BRICK);
		CraftingInput input = CraftingInput.of(3, 3, List.of(
				ItemStack.EMPTY, brick, ItemStack.EMPTY,
				brick, ItemStack.EMPTY, brick,
				ItemStack.EMPTY, new ItemStack(ModItems.GAZE_POTTERY_SHERD), ItemStack.EMPTY));

		var recipe = helper.getLevel().getServer().getRecipeManager()
				.getRecipeFor(RecipeType.CRAFTING, input, helper.getLevel());

		helper.assertTrue(recipe.isPresent(), "Gaze sherd should be accepted by the decorated pot recipe");
		PotDecorations decorations = recipe.get().value().assemble(input).get(DataComponents.POT_DECORATIONS);
		helper.assertTrue(decorations != null, "Crafted pot has no decorations");
		helper.assertValueEqual(decorations.front().map(front -> front.item().value()),
				Optional.of(ModItems.GAZE_POTTERY_SHERD), "front decoration");
		helper.succeed();
	}

	private static void assertProvidesPattern(GameTestHelper helper, Item sherd, ResourceKey<DecoratedPotPattern> pattern) {
		Holder<DecoratedPotPattern> provided = new ItemStack(sherd).get(DataComponents.PROVIDES_POTTERY_PATTERN);
		helper.assertTrue(provided != null && provided.is(pattern), sherd + " should provide " + pattern.identifier());
	}
}
