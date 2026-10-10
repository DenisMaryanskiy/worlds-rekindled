package com.dmaryanskiy.vanillareforged.client.watcher;

import com.dmaryanskiy.vanillareforged.watcher.Watcher;

import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.client.model.monster.creaking.CreakingModel;
import net.minecraft.client.renderer.entity.state.CreakingRenderState;
import net.minecraft.util.Mth;

/**
 * A bulky, symmetric relative of the Creaking: a broad chest with room for its hearts, thick limbs,
 * branches on the head and the builders' copper (a collar, pauldrons, the rim of its single lens).
 * Part names match {@link CreakingModel} so its animations apply; the geometry and the UV layout
 * (128x128) are our own, and the renderer scales it up.
 */
public class WatcherModel extends CreakingModel {
	private static final float RAISED_ARM = -2.8F;
	// Bent this far forward, an arm hanging straight down reaches the ground in front of it.
	private static final float BENT_OVER = 1.2F;
	private static final float SLAM_TICKS = 3.0F;
	private static final float RECOVER_TICKS = 6.0F;

	private final ModelPart upperBody;
	private final ModelPart head;
	private final ModelPart rightArm;
	private final ModelPart leftArm;

	public WatcherModel(ModelPart root) {
		super(root);
		this.upperBody = root.getChild("root").getChild("upper_body");
		this.head = this.upperBody.getChild("head");
		this.rightArm = this.upperBody.getChild("right_arm");
		this.leftArm = this.upperBody.getChild("left_arm");
	}

	public static LayerDefinition createBodyLayer() {
		MeshDefinition mesh = new MeshDefinition();
		PartDefinition root = mesh.getRoot().addOrReplaceChild("root", CubeListBuilder.create(), PartPose.offset(0.0F, 24.0F, 0.0F));
		PartDefinition upperBody = root.addOrReplaceChild("upper_body", CubeListBuilder.create(), PartPose.offset(0.0F, -18.0F, 0.0F));
		upperBody.addOrReplaceChild("head", CubeListBuilder.create()
				.texOffs(0, 0).addBox(-4.0F, -10.0F, -4.0F, 8.0F, 10.0F, 8.0F)
				.texOffs(80, 24).addBox(-4.0F, -13.0F, -4.0F, 8.0F, 3.0F, 8.0F)
				.texOffs(0, 68).addBox(4.0F, -14.0F, 0.0F, 10.0F, 15.0F, 0.0F)
				.texOffs(20, 68).addBox(-14.0F, -15.0F, 0.0F, 10.0F, 15.0F, 0.0F)
				.texOffs(114, 0).addBox(-3.0F, -9.0F, -5.0F, 6.0F, 6.0F, 1.0F), PartPose.offset(0.0F, -22.0F, 0.0F));
		upperBody.addOrReplaceChild("body", CubeListBuilder.create()
				.texOffs(32, 0).addBox(-8.0F, -22.0F, -4.5F, 16.0F, 14.0F, 9.0F)
				.texOffs(82, 0).addBox(-5.0F, -8.0F, -3.0F, 10.0F, 8.0F, 6.0F)
				.texOffs(0, 54).addBox(-8.5F, -23.0F, -5.0F, 17.0F, 3.0F, 10.0F), PartPose.ZERO);
		upperBody.addOrReplaceChild("right_arm", CubeListBuilder.create()
				.texOffs(0, 24).addBox(-2.5F, -2.0F, -2.5F, 5.0F, 24.0F, 5.0F)
				.texOffs(54, 54).addBox(-3.5F, -3.5F, -3.5F, 7.0F, 3.0F, 7.0F), PartPose.offset(-10.5F, -20.0F, 0.0F));
		upperBody.addOrReplaceChild("left_arm", CubeListBuilder.create()
				.texOffs(20, 24).addBox(-2.5F, -2.0F, -2.5F, 5.0F, 24.0F, 5.0F)
				.texOffs(82, 54).addBox(-3.5F, -3.5F, -3.5F, 7.0F, 3.0F, 7.0F), PartPose.offset(10.5F, -20.0F, 0.0F));
		root.addOrReplaceChild("left_leg", CubeListBuilder.create()
				.texOffs(60, 24).addBox(-2.5F, 0.0F, -2.5F, 5.0F, 18.0F, 5.0F), PartPose.offset(3.5F, -18.0F, 0.0F));
		root.addOrReplaceChild("right_leg", CubeListBuilder.create()
				.texOffs(40, 24).addBox(-2.5F, 0.0F, -2.5F, 5.0F, 18.0F, 5.0F), PartPose.offset(-3.5F, -18.0F, 0.0F));
		return LayerDefinition.create(mesh, 128, 128);
	}

	@Override
	public void setupAnim(CreakingRenderState state) {
		super.setupAnim(state);
		if (!(state instanceof WatcherRenderState watcher)) {
			return;
		}
		if (watcher.stunned) {
			// Head down, arms hanging straight: the body stays upright so the hearts are where they're hit.
			this.head.xRot = 0.6F;
			this.head.yRot = 0.0F;
			this.rightArm.xRot = 0.0F;
			this.leftArm.xRot = 0.0F;
			return;
		}
		if (watcher.slamPhase == Watcher.SlamPhase.NONE) {
			return;
		}
		ModelPart arm = watcher.slamRightArm ? this.rightArm : this.leftArm;
		if (watcher.slamPhase == Watcher.SlamPhase.WINDUP) {
			float raise = Mth.clamp(watcher.slamPhaseAge / Watcher.SLAM_WINDUP_TICKS, 0.0F, 1.0F);
			arm.xRot += RAISED_ARM * raise;
			this.upperBody.xRot -= 0.15F * raise;
			return;
		}
		// The arm comes down fast, stays planted, and the body straightens up just before it lets go.
		float down = Mth.clamp(watcher.slamPhaseAge / SLAM_TICKS, 0.0F, 1.0F);
		float held = Mth.clamp((Watcher.SLAM_STUCK_TICKS - watcher.slamPhaseAge) / RECOVER_TICKS, 0.0F, 1.0F);
		this.upperBody.xRot += Mth.lerp(down, -0.15F, BENT_OVER) * held;
		arm.xRot += Mth.lerp(down, RAISED_ARM, -BENT_OVER) * held;
		this.head.xRot += 0.3F * down * held;
	}
}
