package com.dmaryanskiy.vanillareforged.watcher;

import java.util.EnumSet;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.Goal;

/** Walks up to the target and slams an arm down once it's in reach; stands still while slamming. */
public class WatcherSlamGoal extends Goal {
	private static final int REPATH_TICKS = 10;

	private final Watcher watcher;
	private int repathTicks;

	public WatcherSlamGoal(Watcher watcher) {
		this.watcher = watcher;
		this.setFlags(EnumSet.of(Goal.Flag.MOVE, Goal.Flag.LOOK));
	}

	@Override
	public boolean canUse() {
		LivingEntity target = this.watcher.getTarget();
		return target != null && target.isAlive();
	}

	@Override
	public void stop() {
		this.watcher.getNavigation().stop();
	}

	@Override
	public boolean requiresUpdateEveryTick() {
		return true;
	}

	@Override
	public void tick() {
		LivingEntity target = this.watcher.getTarget();
		if (target == null || this.watcher.getSlamPhase() != Watcher.SlamPhase.NONE) {
			return;
		}
		this.watcher.getLookControl().setLookAt(target, 30.0F, 30.0F);
		if (this.watcher.distanceToSqr(target) <= Watcher.SLAM_REACH * Watcher.SLAM_REACH) {
			this.watcher.getNavigation().stop();
			this.watcher.startSlam();
		} else if (--this.repathTicks <= 0) {
			this.repathTicks = REPATH_TICKS;
			this.watcher.getNavigation().moveTo(target, 1.0);
		}
	}
}
