package com.dmaryanskiy.vanillareforged;

import net.minecraft.advancements.triggers.PlayerTrigger;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;

/**
 * Advancement triggers for what vanilla's own can't tell. Each is a plain {@link PlayerTrigger},
 * like vanilla's {@code avoid_vibration}: the code decides when it fires, and an advancement can
 * only narrow it down by the player.
 */
public final class ModCriteria {
	public static final PlayerTrigger SURVIVED_PETRIFICATION = register("survived_petrification");
	public static final PlayerTrigger IGNORED_BY_CREAKING = register("ignored_by_creaking");
	public static final PlayerTrigger KILLED_ELDERWOOD_UNPETRIFIED = register("killed_elderwood_unpetrified");
	public static final PlayerTrigger KILLED_OXIDIZED_ELDERWOOD = register("killed_oxidized_elderwood");

	private ModCriteria() {
	}

	public static void initialize() {
	}

	private static PlayerTrigger register(String name) {
		return Registry.register(BuiltInRegistries.TRIGGER_TYPES, VanillaReforged.id(name), new PlayerTrigger());
	}
}
