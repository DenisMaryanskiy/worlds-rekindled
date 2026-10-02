package com.dmaryanskiy.vanillareforged;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.block.entity.DecoratedPotPattern;

public final class ModPotPatterns {
	public static final ResourceKey<DecoratedPotPattern> GAZE = create("gaze");
	public static final ResourceKey<DecoratedPotPattern> CREAKING = create("creaking");

	private ModPotPatterns() {
	}

	private static ResourceKey<DecoratedPotPattern> create(String name) {
		return ResourceKey.create(Registries.DECORATED_POT_PATTERN, VanillaReforged.id(name));
	}
}
