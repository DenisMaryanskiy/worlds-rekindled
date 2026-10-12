package com.dmaryanskiy.vanillareforged.ruin;

import com.dmaryanskiy.vanillareforged.VanillaReforged;

import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.level.levelgen.structure.StructureType;

public final class ModStructureTypes {
	public static final StructureType<PaleRuinStructure> PALE_RUIN = Registry.register(BuiltInRegistries.STRUCTURE_TYPE,
			VanillaReforged.id("pale_ruin"), () -> PaleRuinStructure.CODEC);

	private ModStructureTypes() {
	}

	public static void initialize() {
	}
}
