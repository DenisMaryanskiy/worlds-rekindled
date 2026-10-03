package com.dmaryanskiy.vanillareforged;

import java.util.Map;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.equipment.ArmorMaterial;
import net.minecraft.world.item.equipment.ArmorType;
import net.minecraft.world.item.equipment.EquipmentAsset;
import net.minecraft.world.item.equipment.EquipmentAssets;

public final class ModArmorMaterials {
	public static final TagKey<Item> REPAIRS_PALE_MASK = TagKey.create(Registries.ITEM, VanillaReforged.id("repairs_pale_mask"));
	public static final ResourceKey<EquipmentAsset> PALE_MASK_ASSET = ResourceKey.create(EquipmentAssets.ROOT_ID,
			VanillaReforged.id("pale_mask"));

	// Same stats as the turtle shell: 2 armor, 275 durability as a helmet.
	public static final ArmorMaterial PALE_MASK = new ArmorMaterial(25, Map.of(ArmorType.HELMET, 2), 9,
			SoundEvents.ARMOR_EQUIP_TURTLE, 0.0F, 0.0F, REPAIRS_PALE_MASK, PALE_MASK_ASSET);

	private ModArmorMaterials() {
	}
}
