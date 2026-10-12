package com.dmaryanskiy.vanillareforged.sentry;

import java.util.EnumSet;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.ai.goal.Goal;

/**
 * With nobody left to hunt, a Ruin Sentry walks back to its post: a hunt can take it anywhere in its
 * Ruin, and its idle strolls would otherwise carry it off for good.
 */
public class ReturnToPostGoal extends Goal {
	private static final int NEAR_POST = 6;

	private final RuinSentry sentry;

	public ReturnToPostGoal(RuinSentry sentry) {
		this.sentry = sentry;
		this.setFlags(EnumSet.of(Goal.Flag.MOVE));
	}

	@Override
	public boolean canUse() {
		return this.sentry.hasHome() && this.sentry.getTarget() == null
				&& !this.sentry.getPost().closerThan(this.sentry.blockPosition(), NEAR_POST);
	}

	@Override
	public boolean canContinueToUse() {
		return this.sentry.getTarget() == null && !this.sentry.getNavigation().isDone();
	}

	@Override
	public void start() {
		BlockPos post = this.sentry.getPost();
		this.sentry.getNavigation().moveTo(post.getX() + 0.5, post.getY(), post.getZ() + 0.5, 1.0);
	}
}
