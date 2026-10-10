package com.dmaryanskiy.vanillareforged.client.watcher;

import com.dmaryanskiy.vanillareforged.watcher.Watcher;
import com.dmaryanskiy.vanillareforged.watcher.WatcherHeart;

import net.minecraft.client.renderer.entity.state.CreakingRenderState;
import net.minecraft.world.level.block.WeatheringCopper;

public class WatcherRenderState extends CreakingRenderState {
	public WeatheringCopper.WeatherState copperStage = WeatheringCopper.WeatherState.WEATHERED;
	public final int[] heartCracks = new int[WatcherHeart.values().length];
	public final boolean[] heartVulnerable = new boolean[WatcherHeart.values().length];
	public boolean coreOpen;
	public Watcher.SlamPhase slamPhase = Watcher.SlamPhase.NONE;
	public boolean slamRightArm;
	public float slamPhaseAge;
}
