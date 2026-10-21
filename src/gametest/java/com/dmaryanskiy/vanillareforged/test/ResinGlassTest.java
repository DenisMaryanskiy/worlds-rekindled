package com.dmaryanskiy.vanillareforged.test;

import java.util.Collections;
import java.util.List;

import com.dmaryanskiy.vanillareforged.ModBlocks;

import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.RecipeType;

public class ResinGlassTest {
	@GameTest
	public void eightGlassAroundABlockOfResinMakeEightResinGlass(GameTestHelper helper) {
		ItemStack glass = new ItemStack(Items.GLASS);
		assertCrafts(helper, CraftingInput.of(3, 3, List.of(glass, glass, glass, glass, new ItemStack(Items.RESIN_BLOCK), glass, glass, glass, glass)),
				ModBlocks.RESIN_GLASS.asItem(), 8);
		helper.succeed();
	}

	@GameTest
	public void resinGlassPanesComeFromResinGlassOrFromPanes(GameTestHelper helper) {
		ItemStack resinGlass = new ItemStack(ModBlocks.RESIN_GLASS);
		assertCrafts(helper, CraftingInput.of(3, 2, Collections.nCopies(6, resinGlass)), ModBlocks.RESIN_GLASS_PANE.asItem(), 16);
		ItemStack pane = new ItemStack(Items.GLASS_PANE);
		assertCrafts(helper, CraftingInput.of(3, 3, List.of(pane, pane, pane, pane, new ItemStack(Items.RESIN_BLOCK), pane, pane, pane, pane)),
				ModBlocks.RESIN_GLASS_PANE.asItem(), 8);
		helper.succeed();
	}

	@GameTest
	public void resinGlassBreaksLikeGlass(GameTestHelper helper) {
		BlockPos pos = new BlockPos(1, 1, 1);
		helper.setBlock(pos, ModBlocks.RESIN_GLASS);

		helper.getLevel().destroyBlock(helper.absolutePos(pos), true);

		helper.assertItemEntityNotPresent(ModBlocks.RESIN_GLASS.asItem());
		helper.succeed();
	}

	private static void assertCrafts(GameTestHelper helper, CraftingInput input, Item item, int count) {
		var recipe = helper.getLevel().getServer().getRecipeManager().getRecipeFor(RecipeType.CRAFTING, input, helper.getLevel());
		helper.assertTrue(recipe.isPresent(), "No recipe for " + item);
		ItemStack result = recipe.get().value().assemble(input);
		helper.assertValueEqual(result.getItem(), item, "crafted item");
		helper.assertValueEqual(result.getCount(), count, "crafted count");
	}
}
