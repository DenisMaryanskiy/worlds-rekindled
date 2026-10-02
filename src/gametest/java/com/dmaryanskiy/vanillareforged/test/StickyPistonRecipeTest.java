package com.dmaryanskiy.vanillareforged.test;

import java.util.List;
import java.util.Optional;

import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeType;

public class StickyPistonRecipeTest {
	private static final ResourceKey<Recipe<?>> VANILLA_RECIPE = ResourceKey.create(Registries.RECIPE,
			Identifier.withDefaultNamespace("sticky_piston"));

	@GameTest
	public void slimeBallAbovePistonCraftsStickyPiston(GameTestHelper helper) {
		assertCraftsStickyPiston(helper, Items.SLIME_BALL);
		helper.succeed();
	}

	@GameTest
	public void resinClumpAbovePistonCraftsStickyPiston(GameTestHelper helper) {
		assertCraftsStickyPiston(helper, Items.RESIN_CLUMP);
		helper.succeed();
	}

	@GameTest
	public void resinClumpBelowPistonCraftsNothing(GameTestHelper helper) {
		helper.assertFalse(findRecipe(helper, craftingColumn(Items.PISTON, Items.RESIN_CLUMP)).isPresent(),
				"Resin clump must go above the piston, like the slimeball");
		helper.succeed();
	}

	private static void assertCraftsStickyPiston(GameTestHelper helper, Item adhesive) {
		CraftingInput input = craftingColumn(adhesive, Items.PISTON);
		Optional<RecipeHolder<CraftingRecipe>> recipe = findRecipe(helper, input);

		helper.assertTrue(recipe.isPresent(), "No crafting recipe matched " + adhesive + " above piston");
		helper.assertValueEqual(recipe.get().id(), VANILLA_RECIPE, "matched recipe");
		helper.assertValueEqual(recipe.get().value().assemble(input).getItem(), Items.STICKY_PISTON, "crafted item");
	}

	private static CraftingInput craftingColumn(Item top, Item bottom) {
		return CraftingInput.of(1, 2, List.of(new ItemStack(top), new ItemStack(bottom)));
	}

	private static Optional<RecipeHolder<CraftingRecipe>> findRecipe(GameTestHelper helper, CraftingInput input) {
		return helper.getLevel().getServer().getRecipeManager().getRecipeFor(RecipeType.CRAFTING, input, helper.getLevel());
	}
}
