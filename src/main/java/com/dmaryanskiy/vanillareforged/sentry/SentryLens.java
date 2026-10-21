package com.dmaryanskiy.vanillareforged.sentry;

import com.dmaryanskiy.vanillareforged.ElderwoodGaze;
import com.dmaryanskiy.vanillareforged.elderwood.ElderwoodLens;
import com.dmaryanskiy.vanillareforged.petrification.Petrification;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

/**
 * An Elderwood Lens set in a player's own Sentry: the Ruin Sentries' gaze, turned on whatever it
 * fights. A mob it holds is slowed, and held long enough it's petrified for a while, rooted to the
 * spot; a boss is only slowed. A player it has turned on is petrified as a Ruin Sentry petrifies.
 */
public final class SentryLens {
	public static final int PETRIFY_AFTER_TICKS = 100;
	public static final int PETRIFIED_TICKS = 60;
	// Each level of Slowness takes 15% of a mob's speed, so VII takes all of it.
	public static final int PETRIFIED_AMPLIFIER = 6;
	private static final double LENS_HEIGHT = 1.7;

	private LivingEntity held;
	private int heldTicks;

	void tick(ServerLevel level, HeartwoodSentry sentry) {
		LivingEntity target = sentry.getTarget();
		if (target == null || !sees(sentry, target)) {
			this.held = null;
			this.heldTicks = 0;
			return;
		}
		Vec3 lens = sentry.position().add(0.0, LENS_HEIGHT, 0.0).add(Vec3.directionFromRotation(0.0F, sentry.yBodyRot).scale(0.3));
		if (sentry.tickCount % ElderwoodLens.TRAIL_INTERVAL_TICKS == 0) {
			ElderwoodLens.showTrail(level, lens, target.getEyePosition(), sentry.getRandom());
		}
		if (target instanceof Player player) {
			Petrification.expose(player, RuinSentry.GAZE_RATE, lens);
			return;
		}
		if (target != this.held) {
			this.held = target;
			this.heldTicks = 0;
		}
		ElderwoodGaze.slow(target, sentry);
		// Its next petrification takes another full hold, which starts once this one is over.
		if (ElderwoodGaze.resists(target) || isPetrified(target)) {
			return;
		}
		if (++this.heldTicks >= PETRIFY_AFTER_TICKS) {
			this.heldTicks = 0;
			target.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, PETRIFIED_TICKS, PETRIFIED_AMPLIFIER), sentry);
		}
	}

	public static boolean isPetrified(LivingEntity mob) {
		MobEffectInstance slowness = mob.getEffect(MobEffects.SLOWNESS);
		return slowness != null && slowness.getAmplifier() >= PETRIFIED_AMPLIFIER;
	}

	/** Whether its lens sees the mob: as far and as wide as a Ruin Sentry's gaze reaches, with a clear line. */
	public static boolean sees(AbstractSentry sentry, LivingEntity mob) {
		float yaw = (float) (Mth.atan2(mob.getZ() - sentry.getZ(), mob.getX() - sentry.getX()) * Mth.RAD_TO_DEG) - 90.0F;
		return sentry.distanceToSqr(mob) <= RuinSentry.GAZE_RANGE * RuinSentry.GAZE_RANGE
				&& Math.abs(Mth.degreesDifference(sentry.yHeadRot, yaw)) <= RuinSentry.GAZE_ARC_DEGREES
				&& sentry.getSensing().hasLineOfSight(mob);
	}
}
