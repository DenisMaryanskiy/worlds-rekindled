package com.dmaryanskiy.vanillareforged;

import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.sounds.SoundEvent;

/**
 * The mod's own sound events. For now each plays a vanilla sound (see {@code sounds.json}): what
 * they add is their own subtitles, and a place to put real audio later.
 */
public final class ModSounds {
	public static final SoundEvent ELDERWOOD_AMBIENT = register("entity.elderwood.ambient");
	public static final SoundEvent ELDERWOOD_HEART_CRACK = register("entity.elderwood.heart_crack");
	public static final SoundEvent ELDERWOOD_HEART_BREAK = register("entity.elderwood.heart_break");
	public static final SoundEvent ELDERWOOD_DEATH = register("entity.elderwood.death");
	public static final SoundEvent ELDERWOOD_GROW = register("entity.elderwood.grow");
	public static final SoundEvent ELDERWOOD_SPLINTER = register("entity.elderwood.splinter");
	public static final SoundEvent ELDERWOOD_RAISE_BRANCH = register("entity.elderwood.raise_branch");
	public static final SoundEvent ELDERWOOD_SLAM = register("entity.elderwood.slam");
	public static final SoundEvent ELDERWOOD_STUN = register("entity.elderwood.stun");
	public static final SoundEvent ELDERWOOD_THROW = register("entity.elderwood.throw");
	public static final SoundEvent ELDERWOOD_MEND = register("entity.elderwood.mend");
	public static final SoundEvent ELDERWOOD_ROOTS_CRACK = register("entity.elderwood.roots_crack");
	public static final SoundEvent ELDERWOOD_ROOTS_ERUPT = register("entity.elderwood.roots_erupt");
	public static final SoundEvent ELDERWOOD_BREAK_APART = register("entity.elderwood.break_apart");
	public static final SoundEvent ELDERWOOD_BURST = register("entity.elderwood.burst");

	private ModSounds() {
	}

	public static void initialize() {
	}

	private static SoundEvent register(String name) {
		Identifier id = VanillaReforged.id(name);
		return Registry.register(BuiltInRegistries.SOUND_EVENT, id, SoundEvent.createVariableRangeEvent(id));
	}
}
