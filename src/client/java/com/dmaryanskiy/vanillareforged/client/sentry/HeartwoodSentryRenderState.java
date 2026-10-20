package com.dmaryanskiy.vanillareforged.client.sentry;

import net.minecraft.client.renderer.entity.state.CreakingRenderState;
import net.minecraft.client.renderer.item.ItemStackRenderState;

public class HeartwoodSentryRenderState extends CreakingRenderState {
	public boolean statue;
	public boolean glowingFace;
	public boolean shielding;
	public int cracks;
	public final ItemStackRenderState lens = new ItemStackRenderState();
}
