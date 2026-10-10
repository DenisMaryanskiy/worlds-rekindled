package com.dmaryanskiy.vanillareforged.client.elderwood;

import com.dmaryanskiy.vanillareforged.elderwood.Elderwood;
import com.dmaryanskiy.vanillareforged.elderwood.ElderwoodHeart;

import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.world.level.block.WeatheringCopper;

public class ElderwoodRenderState extends LivingEntityRenderState {
	public WeatheringCopper.WeatherState copperStage = WeatheringCopper.WeatherState.WEATHERED;
	public final int[] heartCracks = new int[ElderwoodHeart.values().length];
	public final boolean[] heartVulnerable = new boolean[ElderwoodHeart.values().length];
	public boolean coreOpen;
	public Elderwood.SlamPhase slamPhase = Elderwood.SlamPhase.NONE;
	public boolean slamRightArm;
	public float slamPhaseAge;
	public boolean stunned;
	public float growth = 1.0F;
}
