package com.dmaryanskiy.vanillareforged.sentry;

import java.util.EnumSet;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.util.DefaultRandomPos;
import net.minecraft.world.phys.Vec3;

/**
 * A Ruin Sentry can't follow its target into sunlight. Rather than stand at the edge of the light
 * to be shot at, it backs away through the shade until its target can't see it any more.
 */
public class RetreatFromSunlitTargetGoal extends Goal {
	private static final int REPATH_TICKS = 20;
	private static final int RETREAT_DISTANCE = 8;

	private final RuinSentry sentry;
	private int repathCooldown;

	public RetreatFromSunlitTargetGoal(RuinSentry sentry) {
		this.sentry = sentry;
		this.setFlags(EnumSet.of(Goal.Flag.MOVE));
	}

	@Override
	public boolean canUse() {
		return this.sentry.isShielding();
	}

	@Override
	public void start() {
		this.repathCooldown = 0;
	}

	@Override
	public void tick() {
		LivingEntity target = this.sentry.getTarget();
		if (target == null || --this.repathCooldown > 0) {
			return;
		}
		this.repathCooldown = REPATH_TICKS;
		if (!this.sentry.getSensing().hasLineOfSight(target)) {
			this.sentry.getNavigation().stop();
			return;
		}
		Vec3 away = DefaultRandomPos.getPosAway(this.sentry, RETREAT_DISTANCE, 4, target.position());
		if (away != null && !this.sentry.isSunlit(BlockPos.containing(away))) {
			this.sentry.getNavigation().moveTo(away.x, away.y, away.z, 1.0);
		}
	}
}
