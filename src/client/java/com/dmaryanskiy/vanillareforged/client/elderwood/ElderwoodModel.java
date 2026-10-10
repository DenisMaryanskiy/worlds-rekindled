package com.dmaryanskiy.vanillareforged.client.elderwood;

import com.dmaryanskiy.vanillareforged.elderwood.Elderwood;

import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.util.Mth;

/**
 * An old pale oak: a thick trunk on spreading roots, a crown of bare branches, and the builders'
 * copper. Only its two main branches move. Their poses are mirrored on the server by the branch
 * hearts' spots in {@code ElderwoodHeart}, so the two must change together.
 */
public class ElderwoodModel extends EntityModel<ElderwoodRenderState> {
	// Angles from straight up: out to the side (z) and over the top towards the front (x).
	private static final float REST_SPREAD = 0.436F;
	private static final float STUNNED_SPREAD = 1.222F;
	private static final float RAISED_SPREAD = 0.15F;
	private static final float RAISED_BACK = -0.5F;
	// A main branch's pivot and length in model pixels (ElderwoodGeometry), to aim its tip at the slam.
	private static final float PIVOT_SIDE = 20.0F;
	private static final float PIVOT_HEIGHT = 72.0F;
	private static final float BRANCH_LENGTH = 92.0F;
	private static final float SLAM_TICKS = 3.0F;
	private static final float RECOVER_TICKS = 6.0F;

	private final ModelPart rightBranch;
	private final ModelPart leftBranch;

	public ElderwoodModel(ModelPart root) {
		super(root);
		ModelPart tree = root.getChild("tree");
		this.rightBranch = tree.getChild("right_branch");
		this.leftBranch = tree.getChild("left_branch");
	}

	public static LayerDefinition createBodyLayer() {
		return ElderwoodGeometry.create();
	}

	@Override
	public void setupAnim(ElderwoodRenderState state) {
		super.setupAnim(state);
		float sway = Mth.sin(state.ageInTicks * 0.04F) * 0.02F;
		this.pose(this.rightBranch, -1.0F, state, state.slamRightBranch, sway);
		this.pose(this.leftBranch, 1.0F, state, !state.slamRightBranch, sway);
	}

	private void pose(ModelPart branch, float side, ElderwoodRenderState state, boolean slamming, float sway) {
		float forward = 0.0F;
		float spread = REST_SPREAD;
		float stretch = 1.0F;
		if (state.stunned) {
			spread = STUNNED_SPREAD;
		} else if (slamming && state.slamPhase == Elderwood.SlamPhase.WINDUP) {
			float raise = Mth.clamp(state.slamPhaseAge / Elderwood.SLAM_WINDUP_TICKS, 0.0F, 1.0F);
			forward = RAISED_BACK * raise;
			spread = Mth.lerp(raise, REST_SPREAD, RAISED_SPREAD);
		} else if (slamming && state.slamPhase == Elderwood.SlamPhase.STUCK) {
			// The branch comes down fast, stays planted, and lifts back just before it lets go.
			float down = Mth.clamp(state.slamPhaseAge / SLAM_TICKS, 0.0F, 1.0F);
			float held = Mth.clamp((Elderwood.SLAM_STUCK_TICKS - state.slamPhaseAge) / RECOVER_TICKS, 0.0F, 1.0F);
			// From the pivot to where the slam landed, in model space: x to its left, y down, z to its back.
			float x = -state.slamRight * 16.0F - side * PIVOT_SIDE;
			float z = -state.slamForward * 16.0F;
			float length = Mth.sqrt(x * x + PIVOT_HEIGHT * PIVOT_HEIGHT + z * z);
			float plantedForward = Mth.PI - (float) Math.asin(-z / length);
			float plantedSpread = side * (float) Mth.atan2(-x, PIVOT_HEIGHT);
			forward = Mth.lerp(held, 0.0F, Mth.lerp(down, RAISED_BACK, plantedForward));
			spread = Mth.lerp(held, REST_SPREAD, Mth.lerp(down, RAISED_SPREAD, plantedSpread));
			// The branch reaches for the spot: it's drawn longer or shorter so its tip is in the ground there.
			stretch = Mth.lerp(held * down, 1.0F, length / BRANCH_LENGTH);
		}
		branch.xRot = forward;
		branch.zRot = side * spread + sway;
		branch.yScale = stretch;
	}
}
