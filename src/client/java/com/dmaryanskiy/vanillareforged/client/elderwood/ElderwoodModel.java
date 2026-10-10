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
	// Swung this far, the branch's tip is in the ground in front of the trunk.
	private static final float PLANTED_FORWARD = 2.498F;
	private static final float PLANTED_SPREAD = 0.271F;
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
		this.pose(this.rightBranch, -1.0F, state, state.slamRightArm, sway);
		this.pose(this.leftBranch, 1.0F, state, !state.slamRightArm, sway);
	}

	private void pose(ModelPart branch, float side, ElderwoodRenderState state, boolean slamming, float sway) {
		float forward = 0.0F;
		float spread = REST_SPREAD;
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
			forward = Mth.lerp(held, 0.0F, Mth.lerp(down, RAISED_BACK, PLANTED_FORWARD));
			spread = Mth.lerp(held, REST_SPREAD, Mth.lerp(down, RAISED_SPREAD, PLANTED_SPREAD));
		}
		branch.xRot = forward;
		branch.zRot = side * spread + sway;
	}
}
