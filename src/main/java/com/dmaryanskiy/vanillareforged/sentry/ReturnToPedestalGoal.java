package com.dmaryanskiy.vanillareforged.sentry;

import java.util.EnumSet;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.ai.goal.Goal;

/** In daylight the Sentry drops everything and walks back onto its pedestal to become a statue. */
public class ReturnToPedestalGoal extends Goal {
	private static final int REPATH_TICKS = 20;

	private final HeartwoodSentry sentry;
	private int repathCooldown;

	public ReturnToPedestalGoal(HeartwoodSentry sentry) {
		this.sentry = sentry;
		this.setFlags(EnumSet.of(Goal.Flag.MOVE, Goal.Flag.LOOK, Goal.Flag.TARGET));
	}

	@Override
	public boolean canUse() {
		return !this.sentry.isStatue() && this.sentry.isDaylightAtPedestal();
	}

	@Override
	public void start() {
		this.repathCooldown = 0;
	}

	@Override
	public void tick() {
		if (--this.repathCooldown <= 0) {
			this.repathCooldown = REPATH_TICKS;
			BlockPos target = this.sentry.getStandingPos();
			this.sentry.getNavigation().moveTo(target.getX() + 0.5, target.getY(), target.getZ() + 0.5, 1.0);
		}
	}
}
