package com.dmaryanskiy.vanillareforged.watcher;

import com.dmaryanskiy.vanillareforged.heart.HeartHit;

import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/**
 * The hearts on the Watcher's body, its only weak points. Each is a box at a fixed spot relative to
 * the body (blocks to its right, up from its feet, in front of its center):
 * <ul>
 * <li>the chest hearts can always be hit, from the front;</li>
 * <li>an arm heart only while that arm is stuck in the ground after a slam, from any side: the
 * box sits where the planted forearm is;</li>
 * <li>the core only once every other heart is destroyed, from the front.</li>
 * </ul>
 */
public enum WatcherHeart {
	RIGHT_CHEST(3, 0.35, 3.06, 0.39, 0.5),
	LEFT_CHEST(3, -0.35, 3.06, 0.39, 0.5),
	RIGHT_ARM(3, 0.92, 0.85, 1.64, 0.7),
	LEFT_ARM(3, -0.92, 0.85, 1.64, 0.7),
	CORE(5, 0.0, 2.55, 0.39, 0.5);

	public static final int TOTAL_CRACKS = 17;
	private static final int BITS = 3;

	private final int maxCracks;
	private final double right;
	private final double up;
	private final double forward;
	private final double size;

	WatcherHeart(int maxCracks, double right, double up, double forward, double size) {
		this.maxCracks = maxCracks;
		this.right = right;
		this.up = up;
		this.forward = forward;
		this.size = size;
	}

	public int maxCracks() {
		return this.maxCracks;
	}

	public boolean isArm() {
		return this == RIGHT_ARM || this == LEFT_ARM;
	}

	public AABB box(Watcher watcher) {
		Vec3 facing = facing(watcher);
		Vec3 toRight = new Vec3(-facing.z, 0.0, facing.x);
		Vec3 center = watcher.position().add(0.0, this.up, 0.0).add(facing.scale(this.forward)).add(toRight.scale(this.right));
		return AABB.ofSize(center, this.size, this.size, this.size);
	}

	public boolean isHitBy(Watcher watcher, HeartHit hit) {
		return (this.isArm() || hit.comesFromTheFrontOf(facing(watcher))) && hit.passesThrough(this.box(watcher));
	}

	/** Cracks of every heart packed into one synced number, {@value #BITS} bits each. */
	public int cracksIn(int packed) {
		return packed >> this.ordinal() * BITS & (1 << BITS) - 1;
	}

	public int withCracks(int packed, int cracks) {
		int shift = this.ordinal() * BITS;
		return packed & ~((1 << BITS) - 1 << shift) | cracks << shift;
	}

	private static Vec3 facing(Watcher watcher) {
		return Vec3.directionFromRotation(0.0F, watcher.yBodyRot);
	}
}
