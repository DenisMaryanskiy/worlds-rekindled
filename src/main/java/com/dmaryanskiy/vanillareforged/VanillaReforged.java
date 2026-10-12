package com.dmaryanskiy.vanillareforged;

import com.dmaryanskiy.vanillareforged.petrification.Petrification;
import com.dmaryanskiy.vanillareforged.ruin.ModStructureTypes;

import net.fabricmc.api.ModInitializer;

import net.minecraft.resources.Identifier;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class VanillaReforged implements ModInitializer {
	public static final String MOD_ID = "vanillareforged";

	// This logger is used to write text to the console and the log file.
	// It is considered best practice to use your mod id as the logger's name.
	// That way, it's clear which mod wrote info, warnings, and errors.
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	@Override
	public void onInitialize() {
		// This code runs as soon as Minecraft is in a mod-load-ready state.
		// However, some things (like resources) may still be uninitialized.
		// Proceed with mild caution.

		ModSounds.initialize();
		ModEntities.initialize();
		ModItems.initialize();
		ModCriteria.initialize();
		ModStructureTypes.initialize();
		ElderwoodGaze.initialize();
		Petrification.initialize();

		LOGGER.info("Vanilla Reforged loaded");
	}

	public static Identifier id(String path) {
		return Identifier.fromNamespaceAndPath(MOD_ID, path);
	}
}
