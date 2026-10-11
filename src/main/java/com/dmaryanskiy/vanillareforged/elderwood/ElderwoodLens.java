package com.dmaryanskiy.vanillareforged.elderwood;

import com.dmaryanskiy.vanillareforged.ModSounds;
import com.dmaryanskiy.vanillareforged.petrification.Petrification;

import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

/**
 * The lens in the Elderwood's trunk and what it does to a player it sees: its gaze petrifies them,
 * and from a distance it throws burning resin at them. It only sees in a cone ahead of the trunk
 * and needs a clear line to the player.
 */
public final class ElderwoodLens {
	public static final double RANGE = 24.0;
	// It only sees this far to each side of straight ahead.
	public static final float ARC_DEGREES = 60.0F;
	public static final int GAZE_RATE = 2;
	public static final int CORE_GAZE_RATE = 3;
	public static final double RESIN_MIN_RANGE = 6.0;
	public static final int RESIN_INTERVAL_TICKS = 60;
	private static final int RESIN_FIRST_TICKS = 20;
	private static final int TRAIL_INTERVAL_TICKS = 4;
	private static final double TRAIL_SPACING = 1.5;
	private static final DustParticleOptions TRAIL = new DustParticleOptions(0xFAB326, 0.7F);

	private final Elderwood tree;
	private int resinCooldown = RESIN_FIRST_TICKS;

	ElderwoodLens(Elderwood tree) {
		this.tree = tree;
	}

	boolean sees(Player player) {
		return this.tree.distanceToSqr(player) <= RANGE * RANGE
				&& Math.abs(Mth.degreesDifference(this.tree.yBodyRot, this.tree.yawTo(player))) <= ARC_DEGREES
				&& this.tree.getSensing().hasLineOfSight(player);
	}

	/** One tick of holding a player it sees. */
	void watch(ServerLevel level, Player player) {
		Petrification.expose(player, this.tree.isCoreOpen() ? CORE_GAZE_RATE : GAZE_RATE, this.tree.getEyePosition());
		this.showGaze(level, player);
		this.throwResin(level, player);
	}

	// Nothing else shows where the lens looks before the stone starts to creep in: a thin trail of
	// motes from the lens towards whoever it holds, stopping short of their face.
	private void showGaze(ServerLevel level, Player player) {
		if (this.tree.tickCount % TRAIL_INTERVAL_TICKS != 0) {
			return;
		}
		Vec3 from = this.tree.getEyePosition().add(this.tree.facing().scale(1.6));
		Vec3 line = player.getEyePosition().subtract(from);
		Vec3 step = line.normalize();
		double end = line.length() - TRAIL_SPACING;
		for (double along = this.tree.getRandom().nextDouble() * TRAIL_SPACING; along < end; along += TRAIL_SPACING) {
			Vec3 mote = from.add(step.scale(along));
			level.sendParticles(TRAIL, mote.x, mote.y, mote.z, 1, 0.05, 0.05, 0.05, 0.0);
		}
	}

	// At range it throws burning resin every few seconds; up close it slams instead.
	private void throwResin(ServerLevel level, Player target) {
		if (this.resinCooldown > 0) {
			this.resinCooldown--;
		}
		if (this.resinCooldown > 0 || this.tree.getSlamPhase() != Elderwood.SlamPhase.NONE
				|| this.tree.distanceToSqr(target) < RESIN_MIN_RANGE * RESIN_MIN_RANGE) {
			return;
		}
		this.resinCooldown = RESIN_INTERVAL_TICKS;
		Vec3 from = this.tree.getEyePosition();
		Vec3 direction = target.getEyePosition().subtract(from).normalize();
		BurningResin resin = new BurningResin(level, this.tree, direction);
		resin.setPos(from.add(direction.scale(2.0)));
		level.addFreshEntity(resin);
		this.tree.playSound(ModSounds.ELDERWOOD_THROW, 1.5F, 0.6F);
	}
}
