package com.dmaryanskiy.vanillareforged.client.watcher;

import net.minecraft.client.renderer.entity.state.CreakingRenderState;
import net.minecraft.world.level.block.WeatheringCopper;

public class WatcherRenderState extends CreakingRenderState {
	public WeatheringCopper.WeatherState copperStage = WeatheringCopper.WeatherState.WEATHERED;
}
