package com.dmaryanskiy.vanillareforged.sentry;

import com.dmaryanskiy.vanillareforged.heart.HeartHit;

import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/** The Sentry's weak point: a small box in the chest, hit from the front. */
public final class SentryHeart {
	private static final double HEIGHT = 1.53;
	private static final double FORWARD = 0.125;
	private static final double SIZE = 0.5;

	private SentryHeart() {
	}

	public static AABB box(AbstractSentry sentry) {
		Vec3 center = sentry.position().add(0.0, HEIGHT, 0.0).add(forward(sentry).scale(FORWARD));
		return AABB.ofSize(center, SIZE, SIZE, SIZE);
	}

	/** Whether a hit cracks the heart: a player's cracking hit, or any mob's arrow or trident, on the heart. */
	public static boolean isCrackedBy(AbstractSentry sentry, DamageSource source) {
		return HeartHit.of(source, true).filter(hit -> isHitBy(sentry, hit)).isPresent();
	}

	public static boolean isHitBy(AbstractSentry sentry, HeartHit hit) {
		return hit.comesFromTheFrontOf(forward(sentry)) && hit.passesThrough(box(sentry));
	}

	private static Vec3 forward(AbstractSentry sentry) {
		return Vec3.directionFromRotation(0.0F, sentry.yBodyRot);
	}
}
