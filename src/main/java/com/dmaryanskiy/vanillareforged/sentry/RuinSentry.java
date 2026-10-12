package com.dmaryanskiy.vanillareforged.sentry;

import java.util.List;

import org.jspecify.annotations.Nullable;

import com.dmaryanskiy.vanillareforged.VanillaReforged;
import com.dmaryanskiy.vanillareforged.petrification.Petrification;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.ai.navigation.GroundPathNavigation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructurePiece;
import net.minecraft.world.level.levelgen.structure.StructureStart;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.Vec3;

/**
 * A guard of the Pale Ruin: the ancient builders' Sentry, with no pumpkin and a shard of the
 * Elderwood's lens for an eye. It hunts players anywhere in its Ruin, and its gaze petrifies them,
 * more slowly than the Elderwood's. It can't be taken apart, only killed through its heart, and
 * only from where it can come for whoever hits it.
 * <p>
 * It fears sunlight, like the undead: in direct sun it's a statue until it's shaded again, its paths
 * stop short of sunlit ground, and it shields its heart from anyone who stands in the sun, where it
 * can't follow.
 */
public class RuinSentry extends AbstractSentry {
	public static final ResourceKey<Structure> RUIN = ResourceKey.create(Registries.STRUCTURE, VanillaReforged.id("pale_ruin"));
	public static final float ATTACK_DAMAGE = 4.0F;
	// What a Sentry that stands in no Ruin guards: this far around its post.
	public static final int LEASH_RADIUS = 16;
	// Its Ruin reaches this far into the rock around every room, so a niche dug into a wall is no way out of it.
	private static final int RUIN_MARGIN = 2;
	public static final double GAZE_RANGE = 12.0;
	// It only sees this far to each side of where its head points.
	public static final float GAZE_ARC_DEGREES = 60.0F;
	public static final int GAZE_RATE = 1;

	private static final EntityDataAccessor<Boolean> STATUE = SynchedEntityData.defineId(RuinSentry.class,
			EntityDataSerializers.BOOLEAN);
	private static final EntityDataAccessor<Boolean> SHIELDING = SynchedEntityData.defineId(RuinSentry.class,
			EntityDataSerializers.BOOLEAN);

	// The rooms of the Ruin its post lies in; empty if it stands in none, null until it has looked.
	private @Nullable List<BoundingBox> ruin;
	// Its own room among them: the pieces its post lies in.
	private List<BoundingBox> room = List.of();

	public RuinSentry(EntityType<? extends RuinSentry> type, Level level) {
		super(type, level);
		this.xpReward = 10;
	}

	public static AttributeSupplier.Builder createAttributes() {
		return AbstractSentry.createAttributes().add(Attributes.ATTACK_DAMAGE, ATTACK_DAMAGE);
	}

	@Override
	protected void defineSynchedData(SynchedEntityData.Builder builder) {
		super.defineSynchedData(builder);
		builder.define(STATUE, false);
		builder.define(SHIELDING, false);
	}

	@Override
	protected void registerGoals() {
		this.goalSelector.addGoal(0, new RetreatFromSunlitTargetGoal(this));
		this.goalSelector.addGoal(1, new MeleeAttackGoal(this, 1.0, false));
		this.goalSelector.addGoal(2, new ReturnToPostGoal(this));
		this.goalSelector.addGoal(5, new WaterAvoidingRandomStrollGoal(this, 0.6));
		this.goalSelector.addGoal(7, new LookAtPlayerGoal(this, Player.class, 8.0F));
		this.goalSelector.addGoal(8, new RandomLookAroundGoal(this));
		this.targetSelector.addGoal(1, new HurtByTargetGoal(this));
		this.targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, 5, true, false,
				(target, level) -> this.notices(target.blockPosition())));
	}

	/** Where it stands guard, and where it walks back to once there's nobody left to hunt. */
	public BlockPos getPost() {
		return this.getHomePosition();
	}

	public void setPost(BlockPos post) {
		this.setHomeTo(post, LEASH_RADIUS);
		this.ruin = null;
	}

	/**
	 * Whether it comes for a player who stands at {@code pos}: anywhere in the Ruin its post lies in,
	 * the stairway up included, or within its leash of its post if it stands in no Ruin. Beyond that
	 * it neither hunts nor can be hurt.
	 */
	public boolean guards(BlockPos pos) {
		if (!this.standsInARuin()) {
			return super.isWithinHome(pos);
		}
		return isIn(this.ruin, pos);
	}

	/**
	 * Whether a player at {@code pos} draws it out by being there: only in its own room. It still
	 * follows a target, or comes for whoever hits it, anywhere it guards. Otherwise every Sentry
	 * within sight would join a hunt, and the Ruin's seven would be fought as one pack.
	 */
	public boolean notices(BlockPos pos) {
		return this.standsInARuin() ? isIn(this.room, pos) : this.guards(pos);
	}

	private boolean standsInARuin() {
		if (!(this.level() instanceof ServerLevel level) || !this.hasHome()) {
			return false;
		}
		if (this.ruin == null) {
			BlockPos post = this.getPost();
			StructureStart start = level.structureManager().getStructureWithPieceAt(post, structure -> structure.is(RUIN));
			List<BoundingBox> pieces = start.isValid() ? start.getPieces().stream().map(StructurePiece::getBoundingBox).toList() : List.of();
			this.ruin = pieces.stream().map(piece -> piece.inflatedBy(RUIN_MARGIN)).toList();
			this.room = pieces.stream().filter(piece -> piece.isInside(post)).toList();
		}
		return !this.ruin.isEmpty();
	}

	private static boolean isIn(List<BoundingBox> boxes, BlockPos pos) {
		for (BoundingBox box : boxes) {
			if (box.isInside(pos)) {
				return true;
			}
		}
		return false;
	}

	// Its blow would throw a player up into the next one, from it or from the Sentry beside it.
	@Override
	protected boolean tossesItsTarget() {
		return false;
	}

	// Vanilla's goals ask this before they pick, keep or chase a target.
	@Override
	public boolean isWithinHome(BlockPos pos) {
		return this.guards(pos);
	}

	@Override
	public boolean isWithinHome(Vec3 pos) {
		return this.guards(BlockPos.containing(pos));
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
		return this.entityData.get(STATUE);
	}

	/** Whether it holds its arms over its heart, because its target stands in sunlight. */
	public boolean isShielding() {
		return this.entityData.get(SHIELDING);
	}

	@Override
	public boolean isHostileTo(Player player) {
		return !this.isStatue();
	}

	/** Direct sunlight: daytime with open sky above. Torches and other light don't count. */
	public boolean isSunlit(BlockPos pos) {
		return this.level().isBrightOutside() && this.level().canSeeSky(pos);
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
		this.updateStatue();
		if (this.isStatue()) {
			return;
		}
		// Vanilla's sun-avoiding paths don't ask what time it is.
		if (this.getNavigation() instanceof GroundPathNavigation navigation) {
			navigation.setAvoidSun(this.level().isBrightOutside());
		}
		LivingEntity target = this.getTarget();
		if (target != null && !this.guards(target.blockPosition())) {
			this.setTarget(null);
			target = null;
		}
		this.entityData.set(SHIELDING, target != null && this.isSunlit(target.blockPosition()));
		if (target instanceof Player player && this.gazesAt(player)) {
			Petrification.expose(player, GAZE_RATE, this.getEyePosition());
		}
	}

	// Where the sun finds it, it stands as a statue. That clears its cracks, so luring it into the
	// sun makes it harmless for a while but never easier to kill.
	private void updateStatue() {
		boolean sunlit = this.isSunlit(this.blockPosition());
		if (sunlit == this.isStatue()) {
			return;
		}
		if (sunlit) {
			this.getNavigation().stop();
			this.setDeltaMovement(Vec3.ZERO);
			this.setTarget(null);
			this.setCracks(0);
			this.entityData.set(SHIELDING, false);
			this.playSound(SoundEvents.CREAKING_FREEZE, 1.0F, 1.0F);
		} else {
			this.playSound(SoundEvents.CREAKING_UNFREEZE, 1.0F, 1.0F);
		}
		this.entityData.set(STATUE, sunlit);
		this.setNoAi(sunlit);
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
		if (this.isStatue()) {
			return false;
		}
		if (source.getEntity() instanceof Player player && !player.isCreative() && !player.isSpectator()
				&& this.guards(player.blockPosition())) {
			this.setTarget(player);
		}
		// Nobody cracks its heart from where it can't reach them, in the sun or beyond what it guards:
		// its arms are in the way.
		Entity attacker = source.getEntity();
		if (attacker != null && (this.isSunlit(attacker.blockPosition()) || !this.guards(attacker.blockPosition()))) {
			if (SentryHeart.isCrackedBy(this, source)) {
				this.playSound(SoundEvents.WOOD_HIT, 1.0F, 0.6F);
			}
			return false;
		}
		return this.hurtHeart(level, source);
	}

	// Its blow breaks the stone it turned a player into: being petrified costs them one hit, not as
	// many as it lands before they crack free.
	@Override
	public boolean doHurtTarget(ServerLevel level, Entity target) {
		boolean petrified = target instanceof Player player && Petrification.isPetrified(player);
		boolean hurt = super.doHurtTarget(level, target);
		if (hurt && petrified) {
			Petrification.shatter((Player) target);
		}
		return hurt;
	}

	@Override
	protected void addAdditionalSaveData(ValueOutput output) {
		super.addAdditionalSaveData(output);
		if (this.hasHome()) {
			output.store("post", BlockPos.CODEC, this.getPost());
		}
		output.putBoolean("statue", this.isStatue());
	}

	@Override
	protected void readAdditionalSaveData(ValueInput input) {
		super.readAdditionalSaveData(input);
		input.read("post", BlockPos.CODEC).ifPresent(this::setPost);
		this.entityData.set(STATUE, input.getBooleanOr("statue", false));
	}
}
