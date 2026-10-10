package com.dmaryanskiy.vanillareforged.elderwood;

import com.dmaryanskiy.vanillareforged.heart.HeartHit;

import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/**
 * The hearts on the Elderwood, its only weak points. Each is a box at a fixed spot relative to the
 * tree (blocks to its right, up from the ground, in front of the trunk's center):
 * <ul>
 * <li>the chest hearts, low on the trunk, can always be hit, from the front;</li>
 * <li>a branch ("arm") heart is a band around its main branch, so it moves with the branch: far
 * overhead at rest, lower while the stunned tree droops, and near the ground while the branch is
 * planted after a slam. It can be hit from any side whenever it's open;</li>
 * <li>the core only once every other heart is destroyed, from the front.</li>
 * </ul>
 */
public enum ElderwoodHeart {
	RIGHT_CHEST(3, 0.625, 1.56, 1.5, 0.7),
	LEFT_CHEST(3, -0.625, 1.56, 1.5, 0.7),
	RIGHT_ARM(3, 1.25, 4.5, 0.0, 0.9),
	LEFT_ARM(3, -1.25, 4.5, 0.0, 0.9),
	CORE(5, 0.0, 2.94, 1.28, 0.9);

	public static final int TOTAL_CRACKS = 17;
	private static final int BITS = 3;
	// A branch heart sits this far along its branch from the pivot (the arm hearts' spot above), in
	// the direction (right, up, forward) the right branch points; the left one mirrors it. These
	// follow the poses in the client's ElderwoodModel.
	private static final double ALONG_BRANCH = 4.03;
	private static final Vec3 BRANCH_AT_REST = new Vec3(0.423, 0.906, 0.0);
	private static final Vec3 BRANCH_STUNNED = new Vec3(0.940, 0.342, 0.0);
	private static final Vec3 BRANCH_PLANTED = new Vec3(-0.214, -0.771, 0.600);

	private final int maxCracks;
	private final double right;
	private final double up;
	private final double forward;
	private final double size;

	ElderwoodHeart(int maxCracks, double right, double up, double forward, double size) {
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

	public AABB box(Elderwood elderwood) {
		Vec3 facing = facing(elderwood);
		Vec3 toRight = new Vec3(-facing.z, 0.0, facing.x);
		Vec3 spot = new Vec3(this.right, this.up, this.forward);
		if (this.isArm()) {
			Vec3 branch = elderwood.isStuckArm(this) ? BRANCH_PLANTED : elderwood.isStunned() ? BRANCH_STUNNED : BRANCH_AT_REST;
			spot = spot.add(branch.multiply(Math.signum(this.right), 1.0, 1.0).scale(ALONG_BRANCH));
		}
		Vec3 center = elderwood.position().add(0.0, spot.y, 0.0).add(facing.scale(spot.z)).add(toRight.scale(spot.x));
		return AABB.ofSize(center, this.size, this.size, this.size);
	}

	public boolean isHitBy(Elderwood elderwood, HeartHit hit) {
		return (this.isArm() || hit.comesFromTheFrontOf(facing(elderwood))) && hit.passesThrough(this.box(elderwood));
	}

	/** Cracks of every heart packed into one synced number, {@value #BITS} bits each. */
	public int cracksIn(int packed) {
		return packed >> this.ordinal() * BITS & (1 << BITS) - 1;
	}

	public int withCracks(int packed, int cracks) {
		int shift = this.ordinal() * BITS;
		return packed & ~((1 << BITS) - 1 << shift) | cracks << shift;
	}

	private static Vec3 facing(Elderwood elderwood) {
		return Vec3.directionFromRotation(0.0F, elderwood.yBodyRot);
	}
}
