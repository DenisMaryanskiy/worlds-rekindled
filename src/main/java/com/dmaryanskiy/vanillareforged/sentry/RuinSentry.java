package com.dmaryanskiy.vanillareforged.sentry;

import com.dmaryanskiy.vanillareforged.petrification.Petrification;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.MoveTowardsRestrictionGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

/**
 * A guard of the Pale Ruin: the ancient builders' Sentry, with no pumpkin and a shard of the
 * Elderwood's lens for an eye. It hunts players who come near its post, and its gaze petrifies
 * them, more slowly than the Elderwood's. It can't be taken apart, only killed through its heart.
 */
public class RuinSentry extends AbstractSentry {
	public static final int LEASH_RADIUS = 16;
	public static final double GAZE_RANGE = 12.0;
	// It only sees this far to each side of where its head points.
	public static final float GAZE_ARC_DEGREES = 60.0F;
	public static final int GAZE_RATE = 1;

	public RuinSentry(EntityType<? extends RuinSentry> type, Level level) {
		super(type, level);
		this.xpReward = 10;
	}

	@Override
	protected void registerGoals() {
		this.goalSelector.addGoal(1, new MeleeAttackGoal(this, 1.0, false));
		this.goalSelector.addGoal(2, new MoveTowardsRestrictionGoal(this, 1.0));
		this.goalSelector.addGoal(5, new WaterAvoidingRandomStrollGoal(this, 0.6));
		this.goalSelector.addGoal(7, new LookAtPlayerGoal(this, Player.class, 8.0F));
		this.goalSelector.addGoal(8, new RandomLookAroundGoal(this));
		this.targetSelector.addGoal(1, new HurtByTargetGoal(this));
		this.targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, 5, true, false,
				(target, level) -> this.isWithinHome(target.blockPosition())));
	}

	/** Where it stands guard: it never goes, or follows a player, farther than its leash from here. */
	public BlockPos getPost() {
		return this.getHomePosition();
	}

	public void setPost(BlockPos post) {
		this.setHomeTo(post, LEASH_RADIUS);
	}

	@Override
	public int getMaxCracks() {
		return switch (this.level().getDifficulty()) {
			case PEACEFUL, EASY -> 4;
			case NORMAL -> 5;
			case HARD -> 6;
		};
	}

	@Override
	public boolean isStatue() {
		return false;
	}

	@Override
	public boolean isHostileTo(Player player) {
		return true;
	}

	@Override
	public void tick() {
		super.tick();
		if (!(this.level() instanceof ServerLevel)) {
			return;
		}
		// Its post is wherever it first stands, however it got there (structure, egg or command).
		if (!this.hasHome()) {
			this.setPost(this.blockPosition());
		}
		LivingEntity target = this.getTarget();
		if (target != null && !this.isWithinHome(target.blockPosition())) {
			this.setTarget(null);
		} else if (target instanceof Player player && this.gazesAt(player)) {
			Petrification.expose(player, GAZE_RATE, this.getEyePosition());
		}
	}

	public boolean gazesAt(Player player) {
		return this.distanceToSqr(player) <= GAZE_RANGE * GAZE_RANGE
				&& Math.abs(Mth.degreesDifference(this.yHeadRot, this.yawTo(player))) <= GAZE_ARC_DEGREES
				&& this.getSensing().hasLineOfSight(player);
	}

	private float yawTo(Player player) {
		return (float) (Mth.atan2(player.getZ() - this.getZ(), player.getX() - this.getX()) * Mth.RAD_TO_DEG) - 90.0F;
	}

	@Override
	public boolean hurtServer(ServerLevel level, DamageSource source, float damage) {
		if (source.is(DamageTypeTags.BYPASSES_INVULNERABILITY)) {
			return super.hurtServer(level, source, damage);
		}
		if (source.getEntity() instanceof Player player && !player.isCreative() && !player.isSpectator()
				&& this.isWithinHome(player.blockPosition())) {
			this.setTarget(player);
		}
		return this.hurtHeart(level, source);
	}

	@Override
	protected void addAdditionalSaveData(ValueOutput output) {
		super.addAdditionalSaveData(output);
		if (this.hasHome()) {
			output.store("post", BlockPos.CODEC, this.getPost());
		}
	}

	@Override
	protected void readAdditionalSaveData(ValueInput input) {
		super.readAdditionalSaveData(input);
		input.read("post", BlockPos.CODEC).ifPresent(this::setPost);
	}
}
