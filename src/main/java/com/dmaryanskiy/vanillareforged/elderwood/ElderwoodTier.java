package com.dmaryanskiy.vanillareforged.elderwood;

import java.util.Locale;

import com.dmaryanskiy.vanillareforged.VanillaReforged;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.block.WeatheringCopper;
import net.minecraft.world.level.storage.loot.LootTable;

/**
 * How hard the Elderwood fights and what it leaves, set by the copper collar it was woken with.
 * Plain copper is the fight as playtested; every stage of oxidation makes it harder and pays more,
 * as Ominous trials are to plain ones. Waxed copper counts as its stage.
 */
public enum ElderwoodTier {
	COPPER(Elderwood.ROOT_INTERVAL_TICKS, ElderwoodLens.RESIN_INTERVAL_TICKS, Elderwood.TURN_DEGREES_PER_TICK, 0,
			ElderwoodLens.GAZE_RATE, ElderwoodLens.CORE_GAZE_RATE),
	EXPOSED(70, ElderwoodLens.RESIN_INTERVAL_TICKS, Elderwood.TURN_DEGREES_PER_TICK * 1.15F, 0,
			ElderwoodLens.GAZE_RATE, ElderwoodLens.CORE_GAZE_RATE),
	WEATHERED(60, 50, Elderwood.TURN_DEGREES_PER_TICK * 1.3F, 1, ElderwoodLens.GAZE_RATE, ElderwoodLens.CORE_GAZE_RATE),
	// Its gaze fills half as fast again, as near as whole meter points get.
	OXIDIZED(60, 50, Elderwood.TURN_DEGREES_PER_TICK * 1.3F, 1, 3, 4);

	private final int rootIntervalTicks;
	private final int resinIntervalTicks;
	private final float turnDegreesPerTick;
	private final int extraCracks;
	private final int gazeRate;
	private final int coreGazeRate;

	ElderwoodTier(int rootIntervalTicks, int resinIntervalTicks, float turnDegreesPerTick, int extraCracks, int gazeRate,
			int coreGazeRate) {
		this.rootIntervalTicks = rootIntervalTicks;
		this.resinIntervalTicks = resinIntervalTicks;
		this.turnDegreesPerTick = turnDegreesPerTick;
		this.extraCracks = extraCracks;
		this.gazeRate = gazeRate;
		this.coreGazeRate = coreGazeRate;
	}

	public static ElderwoodTier of(WeatheringCopper.WeatherState stage) {
		return switch (stage) {
			case UNAFFECTED -> COPPER;
			case EXPOSED -> EXPOSED;
			case WEATHERED -> WEATHERED;
			case OXIDIZED -> OXIDIZED;
		};
	}

	public int rootIntervalTicks() {
		return this.rootIntervalTicks;
	}

	public int resinIntervalTicks() {
		return this.resinIntervalTicks;
	}

	public float turnDegreesPerTick() {
		return this.turnDegreesPerTick;
	}

	public int maxCracks(ElderwoodHeart heart) {
		return heart.maxCracks() + this.extraCracks;
	}

	public int totalCracks() {
		return ElderwoodHeart.ALL.stream().mapToInt(this::maxCracks).sum();
	}

	public int gazeRate(boolean coreOpen) {
		return coreOpen ? this.coreGazeRate : this.gazeRate;
	}

	/** What it drops: its lens or lenses, shards on the exposed and oxidized tiers, resin, and its Core. */
	public ResourceKey<LootTable> lootTable() {
		String suffix = this == COPPER ? "" : "_" + this.name().toLowerCase(Locale.ROOT);
		return ResourceKey.create(Registries.LOOT_TABLE, VanillaReforged.id("entities/elderwood" + suffix));
	}
}
