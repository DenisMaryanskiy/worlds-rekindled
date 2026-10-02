package com.dmaryanskiy.vanillareforged.test;

import java.util.List;

import com.dmaryanskiy.vanillareforged.ModArmorMaterials;
import com.dmaryanskiy.vanillareforged.ModItems;

import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.component.DataComponents;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.enchantment.Repairable;
import net.minecraft.world.item.equipment.Equippable;

public class PaleMaskTest {
	@GameTest
	public void creakingEyeAndPaleLogsCraftMask(GameTestHelper helper) {
		assertCraftsMask(helper, Items.PALE_OAK_LOG, Items.PALE_OAK_LOG, Items.PALE_OAK_LOG, Items.PALE_OAK_LOG);
		helper.succeed();
	}

	@GameTest
	public void anyPaleOakLogOrWoodVariantCraftsMask(GameTestHelper helper) {
		assertCraftsMask(helper, Items.PALE_OAK_WOOD, Items.STRIPPED_PALE_OAK_LOG, Items.PALE_OAK_LOG,
				Items.STRIPPED_PALE_OAK_WOOD);
		helper.succeed();
	}

	@GameTest
	public void maskIsHeadArmorWithTurtleShellStats(GameTestHelper helper) {
		ItemStack mask = new ItemStack(ModItems.PALE_MASK);
		Equippable equippable = mask.get(DataComponents.EQUIPPABLE);

		helper.assertTrue(equippable != null && equippable.slot() == EquipmentSlot.HEAD, "Mask should equip to the head");
		helper.assertValueEqual(equippable.assetId().orElse(null), ModArmorMaterials.PALE_MASK_ASSET, "equipment asset");
		helper.assertValueEqual(mask.get(DataComponents.ATTRIBUTE_MODIFIERS).compute(Attributes.ARMOR, 0, EquipmentSlot.HEAD),
				2.0, "armor");
		helper.assertValueEqual(mask.getMaxDamage(), 275, "durability");
		helper.assertTrue(mask.is(ItemTags.HEAD_ARMOR), "Mask should be in #head_armor (enchantable, trimmable)");
		helper.succeed();
	}

	@GameTest
	public void resinClumpRepairsMask(GameTestHelper helper) {
		Repairable repairable = new ItemStack(ModItems.PALE_MASK).get(DataComponents.REPAIRABLE);

		helper.assertTrue(repairable != null && repairable.isValidRepairItem(new ItemStack(Items.RESIN_CLUMP)),
				"Resin clump should repair the mask");
		helper.succeed();
	}

	private static void assertCraftsMask(GameTestHelper helper, Item topLeft, Item topRight, Item bottomLeft, Item bottomRight) {
		CraftingInput input = CraftingInput.of(3, 2, List.of(
				new ItemStack(topLeft), new ItemStack(ModItems.CREAKING_EYE), new ItemStack(topRight),
				new ItemStack(bottomLeft), ItemStack.EMPTY, new ItemStack(bottomRight)));

		var recipe = helper.getLevel().getServer().getRecipeManager()
				.getRecipeFor(RecipeType.CRAFTING, input, helper.getLevel());

		helper.assertTrue(recipe.isPresent(), "No crafting recipe matched the Pale Mask layout");
		helper.assertValueEqual(recipe.get().value().assemble(input).getItem(), ModItems.PALE_MASK, "crafted item");
	}
}
