package com.dmaryanskiy.vanillareforged.elderwood;

import java.util.EnumSet;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.Goal;

/** Walks up to the target and slams an arm down once it's in reach; stands still while slamming. */
public class ElderwoodSlamGoal extends Goal {
	private static final int REPATH_TICKS = 10;

	private final Elderwood elderwood;
	private int repathTicks;

	public ElderwoodSlamGoal(Elderwood elderwood) {
		this.elderwood = elderwood;
		this.setFlags(EnumSet.of(Goal.Flag.MOVE, Goal.Flag.LOOK));
	}

	@Override
	public boolean canUse() {
		LivingEntity target = this.elderwood.getTarget();
		return target != null && target.isAlive();
	}

	@Override
	public void stop() {
		this.elderwood.getNavigation().stop();
	}

	@Override
	public boolean requiresUpdateEveryTick() {
		return true;
	}

	@Override
	public void tick() {
		LivingEntity target = this.elderwood.getTarget();
		if (target == null || this.elderwood.getSlamPhase() != Elderwood.SlamPhase.NONE) {
			return;
		}
		this.elderwood.getLookControl().setLookAt(target, 30.0F, 30.0F);
		if (this.elderwood.distanceToSqr(target) <= Elderwood.SLAM_REACH * Elderwood.SLAM_REACH) {
			this.elderwood.getNavigation().stop();
			this.elderwood.startSlam();
		} else if (--this.repathTicks <= 0) {
			this.repathTicks = REPATH_TICKS;
			this.elderwood.getNavigation().moveTo(target, 1.0);
		}
	}
}
