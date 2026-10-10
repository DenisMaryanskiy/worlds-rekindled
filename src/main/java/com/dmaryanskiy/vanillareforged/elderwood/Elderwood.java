package com.dmaryanskiy.vanillareforged.elderwood;

import java.util.Arrays;
import java.util.Optional;

import com.dmaryanskiy.vanillareforged.VanillaReforged;
import com.dmaryanskiy.vanillareforged.heart.HeartHit;
import com.dmaryanskiy.vanillareforged.petrification.Petrification;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerBossEvent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.tags.TagKey;
import net.minecraft.util.Mth;
import net.minecraft.world.BossEvent;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.gamerules.GameRules;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LevelEvent;
import net.minecraft.world.level.block.WeatheringCopper;
import net.minecraft.world.level.material.PushReaction;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/**
 * The oldest pale oak, woken whole by its Creaking Heart and fitted by an ancient civilization with
 * copper bands and a lens that turns the Creakings' freeze outward: whoever it watches is petrified.
 * Grown by placing a Creaking Heart on its stump (see {@link ElderwoodSummon}).
 * <p>
 * It's rooted: it never leaves its spot and only turns, slower than a player can circle it. It can
 * only be hurt through its hearts (see {@link ElderwoodHeart}). Up close it slams a branch down: the
 * hit shatters a petrified player's stone shell, and the branch then stays stuck in the ground for a
 * moment, bringing its heart within reach.
 */
public class Elderwood extends Monster {
	public static final double GAZE_RANGE = 24.0;
	// The lens only sees this far to each side of straight ahead.
	public static final float GAZE_ARC_DEGREES = 60.0F;
	public static final int GAZE_RATE = 2;
	public static final int CORE_GAZE_RATE = 3;
	public static final double SLAM_REACH = 6.0;
	// Closer than this the branch would have to come down through its own trunk.
	public static final double SLAM_MIN_DISTANCE = 2.5;
	public static final float SLAM_DAMAGE = 10.0F;
	public static final int SLAM_WINDUP_TICKS = 15;
	public static final int SLAM_STUCK_TICKS = 30;
	private static final int SLAM_COOLDOWN_TICKS = 40;
	public static final double SLAM_DISTANCE = 3.5;
	public static final float SLAM_ARC_DEGREES = 75.0F;
	private static final double SLAM_RADIUS = 2.2;
	private static final double SMASH_RADIUS = 1.5;
	private static final int SMASH_HEIGHT = 4;
	public static final TagKey<Block> BREAKABLE = TagKey.create(Registries.BLOCK, VanillaReforged.id("elderwood_breakable"));
	private static final int CRACK_COOLDOWN_TICKS = 10;
	private static final double HEART_DEPTH = 0.75;
	public static final double RESIN_MIN_RANGE = 6.0;
	public static final int RESIN_INTERVAL_TICKS = 60;
	private static final int RESIN_FIRST_TICKS = 20;
	public static final double ROOT_RANGE = 12.0;
	public static final int ROOT_INTERVAL_TICKS = 50;
	private static final int ROOT_FIRST_TICKS = 20;
	private static final int ROOT_GROUND_SEARCH = 4;
	public static final int DORMANT_AFTER_TICKS = 600;
	public static final int HEAL_INTERVAL_TICKS = 100;
	public static final int STUN_TICKS = 60;
	public static final int STUN_IMMUNITY_TICKS = 100;
	public static final float TURN_DEGREES_PER_TICK = 1.5F;
	public static final int GROW_STAGES = 3;
	public static final int GROW_STAGE_TICKS = 20;
	public static final int GROW_TICKS = GROW_STAGES * GROW_STAGE_TICKS;
	private static final float GROW_SNAP_TICKS = 3.0F;
	private static final ElderwoodHeart[] BRANCH_HEARTS = {ElderwoodHeart.RIGHT_BRANCH, ElderwoodHeart.LEFT_BRANCH};

	private static final EntityDataAccessor<Integer> COPPER_STAGE = SynchedEntityData.defineId(Elderwood.class,
			EntityDataSerializers.INT);
	private static final EntityDataAccessor<Integer> HEART_CRACKS = SynchedEntityData.defineId(Elderwood.class,
			EntityDataSerializers.INT);
	private static final EntityDataAccessor<Integer> SLAM_PHASE = SynchedEntityData.defineId(Elderwood.class,
			EntityDataSerializers.INT);
	private static final EntityDataAccessor<Boolean> SLAM_RIGHT_BRANCH = SynchedEntityData.defineId(Elderwood.class,
			EntityDataSerializers.BOOLEAN);
	// Where the slam lands, in blocks to its right and in front of the trunk's center.
	private static final EntityDataAccessor<Float> SLAM_RIGHT = SynchedEntityData.defineId(Elderwood.class,
			EntityDataSerializers.FLOAT);
	private static final EntityDataAccessor<Float> SLAM_FORWARD = SynchedEntityData.defineId(Elderwood.class,
			EntityDataSerializers.FLOAT);
	private static final EntityDataAccessor<Boolean> STUNNED = SynchedEntityData.defineId(Elderwood.class,
			EntityDataSerializers.BOOLEAN);
	private static final EntityDataAccessor<Boolean> GROWING = SynchedEntityData.defineId(Elderwood.class,
			EntityDataSerializers.BOOLEAN);

	public enum SlamPhase {
		NONE, WINDUP, STUCK
	}

	private int slamTicks;
	private int slamPhaseAge;
	private int slamCooldown;
	private double slamHeight;
	private int crackCooldown;
	private int resinCooldown = RESIN_FIRST_TICKS;
	private int rootCooldown = ROOT_FIRST_TICKS;
	private int idleTicks;
	private final ElderwoodBranchHeart[] branchHearts = new ElderwoodBranchHeart[BRANCH_HEARTS.length];
	private int stunTicks;
	private int stunImmunity;
	private int growAge;
	private final ServerBossEvent bossEvent = new ServerBossEvent(Mth.createInsecureUUID(this.random), this.getDisplayName(),
			BossEvent.BossBarColor.YELLOW, BossEvent.BossBarOverlay.PROGRESS);

	public Elderwood(EntityType<? extends Elderwood> type, Level level) {
		super(type, level);
		this.xpReward = 50;
	}

	public static AttributeSupplier.Builder createAttributes() {
		return Monster.createMonsterAttributes()
				.add(Attributes.MAX_HEALTH, 100.0)
				.add(Attributes.MOVEMENT_SPEED, 0.0)
				.add(Attributes.KNOCKBACK_RESISTANCE, 1.0)
				.add(Attributes.FOLLOW_RANGE, 32.0);
	}

	@Override
	protected void registerGoals() {
		// It feels players through its roots, so cover doesn't hide them from it, only from its lens.
		this.targetSelector.addGoal(1, new NearestAttackableTargetGoal<>(this, Player.class, false));
	}

	@Override
	protected void defineSynchedData(SynchedEntityData.Builder builder) {
		super.defineSynchedData(builder);
		builder.define(COPPER_STAGE, WeatheringCopper.WeatherState.WEATHERED.ordinal());
		builder.define(HEART_CRACKS, 0);
		builder.define(SLAM_PHASE, SlamPhase.NONE.ordinal());
		builder.define(SLAM_RIGHT_BRANCH, true);
		builder.define(SLAM_RIGHT, 0.0F);
		builder.define(SLAM_FORWARD, (float) SLAM_DISTANCE);
		builder.define(STUNNED, false);
		builder.define(GROWING, false);
	}

	/** The collar's oxidation stage, which its copper plates show. */
	public WeatheringCopper.WeatherState getCopperStage() {
		return WeatheringCopper.WeatherState.values()[this.entityData.get(COPPER_STAGE)];
	}

	public void setCopperStage(WeatheringCopper.WeatherState stage) {
		this.entityData.set(COPPER_STAGE, stage.ordinal());
	}

	public int getCracks(ElderwoodHeart heart) {
		return heart.cracksIn(this.entityData.get(HEART_CRACKS));
	}

	public void setCracks(ElderwoodHeart heart, int cracks) {
		this.entityData.set(HEART_CRACKS, heart.withCracks(this.entityData.get(HEART_CRACKS), cracks));
	}

	public boolean isDestroyed(ElderwoodHeart heart) {
		return this.getCracks(heart) >= heart.maxCracks();
	}

	/** The core only opens once every other heart is destroyed. */
	public boolean isCoreOpen() {
		return Arrays.stream(ElderwoodHeart.values()).allMatch(heart -> heart == ElderwoodHeart.CORE || this.isDestroyed(heart));
	}

	/** Whether the heart can be hit right now; these are the hearts that glow. */
	public boolean isVulnerable(ElderwoodHeart heart) {
		if (this.isDestroyed(heart)) {
			return false;
		}
		if (this.isGrowing()) {
			return false;
		}
		// A branch heart is out of reach until its branch is planted, or the stun makes both droop.
		if (heart.isBranch()) {
			return this.isStunned() || this.isPlanted(heart);
		}
		return heart != ElderwoodHeart.CORE || this.isCoreOpen();
	}

	/** Whether this is the heart of the branch that's planted in the ground right now. */
	public boolean isPlanted(ElderwoodHeart heart) {
		return heart.isBranch() && this.getSlamPhase() == SlamPhase.STUCK
				&& (heart == ElderwoodHeart.RIGHT_BRANCH) == this.isSlammingRightBranch();
	}

	public boolean isStunned() {
		return this.entityData.get(STUNNED);
	}

	public boolean isGrowing() {
		return this.entityData.get(GROWING);
	}

	/** Makes it rise out of its stump: until it's grown it can't be hurt and does nothing. */
	public void startGrowing() {
		this.entityData.set(GROWING, true);
		this.playSound(SoundEvents.CREAKING_SPAWN, 2.0F, 0.5F);
	}

	/** Which of its {@value #GROW_STAGES} sizes it has reached while growing, from 1. */
	public int getGrowStage() {
		return Math.min(this.growAge / GROW_STAGE_TICKS, GROW_STAGES - 1) + 1;
	}

	/** How much of its full size it has reached, for the client. */
	public float getGrowth(float partialTicks) {
		if (!this.isGrowing()) {
			return 1.0F;
		}
		// It doesn't grow smoothly: it bursts to each next size in a few ticks, then holds it.
		int stage = this.getGrowStage();
		float sinceBurst = this.growAge + partialTicks - (stage - 1) * GROW_STAGE_TICKS;
		float snap = Mth.clamp(sinceBurst / GROW_SNAP_TICKS, 0.0F, 1.0F);
		return Mth.lerp(snap, stage - 1.0F, stage) / GROW_STAGES;
	}

	/**
	 * Its own resin, hit back at it, stuns it: it stops gazing, turning and attacking, and its
	 * branches droop with their hearts open. It can't be stunned again right away.
	 */
	public void stun() {
		if (this.isStunned() || this.isGrowing() || this.stunImmunity > 0) {
			return;
		}
		this.entityData.set(STUNNED, true);
		this.entityData.set(SLAM_PHASE, SlamPhase.NONE.ordinal());
		this.stunTicks = STUN_TICKS;
		this.playSound(SoundEvents.CREAKING_FREEZE, 1.5F, 0.6F);
	}

	public SlamPhase getSlamPhase() {
		return SlamPhase.values()[this.entityData.get(SLAM_PHASE)];
	}

	public boolean isSlammingRightBranch() {
		return this.entityData.get(SLAM_RIGHT_BRANCH);
	}

	public boolean canStartSlam() {
		return !this.isStunned() && !this.isGrowing() && this.getSlamPhase() == SlamPhase.NONE && this.slamCooldown == 0;
	}

	public float getSlamRight() {
		return this.entityData.get(SLAM_RIGHT);
	}

	public float getSlamForward() {
		return this.entityData.get(SLAM_FORWARD);
	}

	/** Slams straight ahead. */
	public void startSlam() {
		this.startSlam(this.position().add(Vec3.directionFromRotation(0.0F, this.yBodyRot).scale(SLAM_DISTANCE)));
	}

	/**
	 * Raises a branch; after the wind-up it comes down on {@code point}, wherever its target has gone
	 * by then, so stepping aside dodges it.
	 */
	public void startSlam(Vec3 point) {
		if (!this.canStartSlam()) {
			return;
		}
		Vec3 facing = Vec3.directionFromRotation(0.0F, this.yBodyRot);
		Vec3 offset = point.subtract(this.position());
		Vec3 flat = new Vec3(offset.x, 0.0, offset.z);
		flat = flat.normalize().scale(Mth.clamp(flat.length(), SLAM_MIN_DISTANCE, SLAM_REACH));
		this.entityData.set(SLAM_RIGHT, (float) (-facing.z * flat.x + facing.x * flat.z));
		this.entityData.set(SLAM_FORWARD, (float) flat.dot(facing));
		this.slamHeight = Mth.clamp(offset.y, -1.0, 3.0);
		this.slamTicks = 0;
		this.entityData.set(SLAM_PHASE, SlamPhase.WINDUP.ordinal());
		this.playSound(SoundEvents.CREAKING_ATTACK, 1.5F, 0.6F);
	}

	@Override
	protected void customServerAiStep(ServerLevel level) {
		super.customServerAiStep(level);
		float yaw = this.yBodyRot;
		if (this.isGrowing()) {
			this.pushOutOfTheTrunk(level);
			if (this.growAge >= GROW_TICKS) {
				this.entityData.set(GROWING, false);
			} else if (this.growAge > 0 && this.growAge % GROW_STAGE_TICKS == 0) {
				this.burstToNextSize(level);
			}
			this.face(yaw);
			return;
		}
		if (this.stunImmunity > 0) {
			this.stunImmunity--;
		}
		// It turns only while it's free: a planted branch or a stun holds it where it faced.
		if (!this.isStunned() && this.getSlamPhase() == SlamPhase.NONE && this.getTarget() != null) {
			yaw = Mth.approachDegrees(yaw, this.yawTo(this.getTarget()), TURN_DEGREES_PER_TICK);
		}
		this.face(yaw);
		if (this.isStunned()) {
			if (--this.stunTicks <= 0) {
				this.entityData.set(STUNNED, false);
				this.stunImmunity = STUN_IMMUNITY_TICKS;
			}
		} else {
			this.tickSlam(level);
			if (this.getTarget() != null && this.isInSlamArc(this.getTarget())) {
				this.startSlam(this.getTarget().position());
			}
			// While a branch is planted it strains at it and does not gaze.
			if (this.getSlamPhase() != SlamPhase.STUCK && this.getTarget() instanceof Player player && this.isWatching(player)) {
				Petrification.expose(player, this.isCoreOpen() ? CORE_GAZE_RATE : GAZE_RATE, this.getEyePosition());
				this.tickResin(level, player);
			} else if (this.getTarget() instanceof Player player) {
				this.tickRoots(level, player);
			}
		}
		this.tickDormancy();
		int cracks = Arrays.stream(ElderwoodHeart.values()).mapToInt(this::getCracks).sum();
		this.bossEvent.setProgress(1.0F - cracks / (float) ElderwoodHeart.TOTAL_CRACKS);
	}

	private void tickSlam(ServerLevel level) {
		if (this.slamCooldown > 0) {
			this.slamCooldown--;
		}
		SlamPhase phase = this.getSlamPhase();
		if (phase == SlamPhase.NONE) {
			return;
		}
		this.slamTicks++;
		if (phase == SlamPhase.WINDUP && this.slamTicks >= SLAM_WINDUP_TICKS) {
			this.strike(level);
			this.slamTicks = 0;
			this.entityData.set(SLAM_PHASE, SlamPhase.STUCK.ordinal());
		} else if (phase == SlamPhase.STUCK && this.slamTicks >= SLAM_STUCK_TICKS) {
			this.entityData.set(SLAM_PHASE, SlamPhase.NONE.ordinal());
			this.entityData.set(SLAM_RIGHT_BRANCH, !this.isSlammingRightBranch());
			this.slamCooldown = SLAM_COOLDOWN_TICKS;
		}
	}

	// The whole trunk turns as one: there's no head to look around with.
	private void face(float yaw) {
		this.setYRot(yaw);
		this.yBodyRot = yaw;
		this.yHeadRot = yaw;
	}

	private float yawTo(Entity target) {
		return (float) (Mth.atan2(target.getZ() - this.getZ(), target.getX() - this.getX()) * Mth.RAD_TO_DEG) - 90.0F;
	}

	// Its main branches only swing forwards, so it can't slam at a player beside or behind it.
	private boolean isInSlamArc(Entity target) {
		return this.distanceToSqr(target) <= SLAM_REACH * SLAM_REACH
				&& Math.abs(Mth.degreesDifference(this.yBodyRot, this.yawTo(target))) <= SLAM_ARC_DEGREES;
	}

	private void burstToNextSize(ServerLevel level) {
		float size = this.getGrowStage() / (float) GROW_STAGES;
		double height = this.getBbHeight() * size;
		double width = this.getBbWidth() * size;
		level.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, Blocks.PALE_OAK_LOG.defaultBlockState()),
				this.getX(), this.getY() + height / 2.0, this.getZ(), (int) (120 * size), width / 2.0, height / 3.0, width / 2.0, 0.1);
		this.playSound(SoundEvents.WOOD_BREAK, 2.0F, 0.6F);
		this.playSound(SoundEvents.CREAKING_TWITCH, 2.0F, 0.4F + 0.15F * this.getGrowStage());
	}

	// The trunk becomes solid once it's grown; nobody should end up inside it.
	private void pushOutOfTheTrunk(ServerLevel level) {
		for (Entity entity : level.getEntities(this, this.getBoundingBox(), e -> !e.isSpectator() && e.isPushable())) {
			Vec3 away = new Vec3(entity.getX() - this.getX(), 0.0, entity.getZ() - this.getZ());
			Vec3 push = (away.lengthSqr() < 1.0E-4 ? Vec3.directionFromRotation(0.0F, this.yBodyRot) : away.normalize()).scale(0.2);
			entity.push(push.x, 0.0, push.z);
		}
	}

	/** A player its lens can't see, beside it, behind it or behind cover, gets roots from below. */
	private void tickRoots(ServerLevel level, Player target) {
		if (this.isWatching(target) || this.distanceToSqr(target) > ROOT_RANGE * ROOT_RANGE) {
			return;
		}
		if (--this.rootCooldown <= 0) {
			this.rootCooldown = ROOT_INTERVAL_TICKS;
			this.raiseRoots(level, target.position());
		}
	}

	/** Roots crack the ground at {@code spot}, or just below it, and burst out a moment later. */
	public void raiseRoots(ServerLevel level, Vec3 spot) {
		BlockPos.MutableBlockPos pos = BlockPos.containing(spot).mutable();
		for (int i = 0; i < ROOT_GROUND_SEARCH && level.getBlockState(pos.below()).getCollisionShape(level, pos.below()).isEmpty(); i++) {
			pos.move(0, -1, 0);
		}
		level.addFreshEntity(new ElderwoodRoots(level, this, spot.x, pos.getY(), spot.z));
	}

	// With nobody near for a while it settles and mends, so walking away resets the fight.
	private void tickDormancy() {
		if (this.getTarget() != null) {
			this.idleTicks = 0;
			return;
		}
		this.idleTicks++;
		if (this.isDormant() && (this.idleTicks - DORMANT_AFTER_TICKS) % HEAL_INTERVAL_TICKS == 0) {
			for (ElderwoodHeart heart : ElderwoodHeart.values()) {
				if (this.getCracks(heart) > 0) {
					this.setCracks(heart, this.getCracks(heart) - 1);
					this.playSound(SoundEvents.CREAKING_HEART_IDLE, 1.5F, 0.6F);
					break;
				}
			}
		}
	}

	public boolean isDormant() {
		return this.idleTicks >= DORMANT_AFTER_TICKS;
	}

	/** At range, with the player in sight, it throws burning resin every few seconds. */
	private void tickResin(ServerLevel level, Player target) {
		if (this.resinCooldown > 0) {
			this.resinCooldown--;
		}
		if (this.resinCooldown > 0 || this.getSlamPhase() != SlamPhase.NONE
				|| this.distanceToSqr(target) < RESIN_MIN_RANGE * RESIN_MIN_RANGE) {
			return;
		}
		this.resinCooldown = RESIN_INTERVAL_TICKS;
		Vec3 from = this.getEyePosition();
		Vec3 direction = target.getEyePosition().subtract(from).normalize();
		BurningResin resin = new BurningResin(level, this, direction);
		resin.setPos(from.add(direction.scale(2.0)));
		level.addFreshEntity(resin);
		this.playSound(SoundEvents.BLAZE_SHOOT, 1.5F, 0.6F);
	}

	private void strike(ServerLevel level) {
		Vec3 facing = Vec3.directionFromRotation(0.0F, this.yBodyRot);
		Vec3 point = this.position().add(0.0, this.slamHeight, 0.0).add(facing.scale(this.getSlamForward()))
				.add(new Vec3(-facing.z, 0.0, facing.x).scale(this.getSlamRight()));
		this.smashSoftCover(level, point);
		BlockPos ground = BlockPos.containing(point).below();
		level.levelEvent(LevelEvent.PARTICLES_AND_SOUND_DESTROY_BLOCK, ground, Block.getId(level.getBlockState(ground)));
		AABB area = AABB.ofSize(point.add(0.0, 1.0, 0.0), SLAM_RADIUS * 2.0, 3.0, SLAM_RADIUS * 2.0);
		for (Player player : level.getEntitiesOfClass(Player.class, area, p -> !p.isCreative() && !p.isSpectator())) {
			// Petrified players are brittle, so the hit lands harder; then the stone shell breaks.
			DamageSource source = this.damageSources().mobAttack(this);
			if (player.hurtServer(level, source, SLAM_DAMAGE)) {
				Petrification.shatter(player);
				player.knockback(1.5, this.getX() - player.getX(), this.getZ() - player.getZ(), source, SLAM_DAMAGE);
			}
		}
	}

	// Wood, leaves and dirt don't stop a branch that size; stone does. The ground it lands on stays.
	private void smashSoftCover(ServerLevel level, Vec3 point) {
		if (!level.getGameRules().get(GameRules.MOB_GRIEFING)) {
			return;
		}
		BlockPos center = BlockPos.containing(point);
		int reach = Mth.ceil(SMASH_RADIUS);
		for (BlockPos pos : BlockPos.betweenClosed(center.offset(-reach, 0, -reach), center.offset(reach, SMASH_HEIGHT - 1, reach))) {
			double dx = pos.getX() + 0.5 - point.x;
			double dz = pos.getZ() + 0.5 - point.z;
			if (dx * dx + dz * dz <= SMASH_RADIUS * SMASH_RADIUS && level.getBlockState(pos).is(BREAKABLE)) {
				level.destroyBlock(pos, true, this);
			}
		}
	}

	@Override
	public void onSyncedDataUpdated(EntityDataAccessor<?> accessor) {
		super.onSyncedDataUpdated(accessor);
		if (SLAM_PHASE.equals(accessor)) {
			this.slamPhaseAge = 0;
		}
	}

	@Override
	public void tick() {
		super.tick();
		this.slamPhaseAge++;
		if (this.isGrowing()) {
			this.growAge++;
		}
		if (this.crackCooldown > 0) {
			this.crackCooldown--;
		}
		if (this.level() instanceof ServerLevel level) {
			this.carryBranchHearts(level);
		}
	}

	// The branch hearts hang outside its own box, so each has an entity of its own to be hit through.
	private void carryBranchHearts(ServerLevel level) {
		for (int i = 0; i < BRANCH_HEARTS.length; i++) {
			ElderwoodBranchHeart part = this.branchHearts[i];
			boolean missing = part == null || part.isRemoved();
			if (missing) {
				part = new ElderwoodBranchHeart(level, this, BRANCH_HEARTS[i]);
				this.branchHearts[i] = part;
			}
			Vec3 spot = BRANCH_HEARTS[i].box(this).getCenter();
			part.setPos(spot.x, spot.y - part.getBbHeight() / 2.0, spot.z);
			if (missing) {
				level.addFreshEntity(part);
			}
		}
	}

	@Override
	public void remove(RemovalReason reason) {
		super.remove(reason);
		for (ElderwoodBranchHeart part : this.branchHearts) {
			if (part != null) {
				part.discard();
			}
		}
	}

	/** A hit that landed on one heart for certain, passed on by a branch heart's own entity. */
	public boolean hurtHeart(ServerLevel level, ElderwoodHeart heart, DamageSource source) {
		return this.isVulnerable(heart) && HeartHit.of(source, false).isPresent() && this.crackCooldown == 0
				&& this.crack(level, source, heart);
	}

	/** Ticks since the slam phase last changed, for the client's pose. */
	public int getSlamPhaseAge() {
		return this.slamPhaseAge;
	}

	public boolean isWatching(Player player) {
		return this.distanceToSqr(player) <= GAZE_RANGE * GAZE_RANGE
				&& Math.abs(Mth.degreesDifference(this.yBodyRot, this.yawTo(player))) <= GAZE_ARC_DEGREES
				&& this.getSensing().hasLineOfSight(player);
	}

	@Override
	public boolean hurtServer(ServerLevel level, DamageSource source, float damage) {
		if (source.is(DamageTypeTags.BYPASSES_INVULNERABILITY)) {
			return super.hurtServer(level, source, damage);
		}
		if (this.isGrowing()) {
			return false;
		}
		// Only players can hurt it, and only through a heart that's open to them.
		ElderwoodHeart heart = HeartHit.of(source, false).map(this::intoTheTrunk).map(this::openHeartOn).orElse(null);
		return heart != null && this.crackCooldown == 0 && this.crack(level, source, heart);
	}

	// Its box doesn't turn with it: turned, a trunk heart sits up to this deep inside the box's corner,
	// and a swing at the very end of a player's reach would stop at the box's face, short of it.
	private HeartHit intoTheTrunk(HeartHit hit) {
		return hit.carriedOn(HEART_DEPTH);
	}

	private ElderwoodHeart openHeartOn(HeartHit hit) {
		for (ElderwoodHeart heart : ElderwoodHeart.values()) {
			if (this.isVulnerable(heart) && heart.isHitBy(this, hit)) {
				return heart;
			}
		}
		return null;
	}

	private boolean crack(ServerLevel level, DamageSource source, ElderwoodHeart heart) {
		this.crackCooldown = CRACK_COOLDOWN_TICKS;
		this.setCracks(heart, this.getCracks(heart) + 1);
		if (this.isDestroyed(heart)) {
			level.levelEvent(LevelEvent.PARTICLES_AND_SOUND_DESTROY_BLOCK, BlockPos.containing(heart.box(this).getCenter()),
					Block.getId(Blocks.CREAKING_HEART.defaultBlockState()));
			if (heart == ElderwoodHeart.CORE) {
				return super.hurtServer(level, source, Float.MAX_VALUE);
			}
		}
		level.broadcastDamageEvent(this, source);
		return true;
	}

	// Rooted: nothing moves it sideways, not even a piston. It still settles onto the ground.
	@Override
	public void move(MoverType moverType, Vec3 delta) {
		if (moverType != MoverType.PISTON) {
			super.move(moverType, new Vec3(0.0, delta.y, 0.0));
		}
	}

	@Override
	public boolean canBeCollidedWith(Entity other) {
		return this.isAlive() && !this.isGrowing();
	}

	@Override
	public boolean isPushable() {
		return false;
	}

	@Override
	protected void doPush(Entity entity) {
	}

	@Override
	public boolean isPushedByFluid() {
		return false;
	}

	@Override
	public PushReaction getPistonPushReaction() {
		return PushReaction.IGNORE_ENTITY;
	}

	@Override
	protected boolean canRide(Entity vehicle) {
		return false;
	}

	@Override
	public boolean canBeLeashed() {
		return false;
	}

	@Override
	public boolean removeWhenFarAway(double distanceSqr) {
		return false;
	}

	@Override
	public void startSeenByPlayer(ServerPlayer player) {
		super.startSeenByPlayer(player);
		this.bossEvent.addPlayer(player);
	}

	@Override
	public void stopSeenByPlayer(ServerPlayer player) {
		super.stopSeenByPlayer(player);
		this.bossEvent.removePlayer(player);
	}

	@Override
	protected SoundEvent getAmbientSound() {
		return SoundEvents.CREAKING_AMBIENT;
	}

	@Override
	protected SoundEvent getHurtSound(DamageSource source) {
		return SoundEvents.CREAKING_HEART_HURT;
	}

	@Override
	protected SoundEvent getDeathSound() {
		return SoundEvents.CREAKING_DEATH;
	}

	@Override
	public float getVoicePitch() {
		return 0.6F;
	}

	@Override
	protected void addAdditionalSaveData(ValueOutput output) {
		super.addAdditionalSaveData(output);
		output.putInt("copper_stage", this.getCopperStage().ordinal());
		output.putInt("heart_cracks", this.entityData.get(HEART_CRACKS));
	}

	@Override
	protected void readAdditionalSaveData(ValueInput input) {
		super.readAdditionalSaveData(input);
		int stage = input.getIntOr("copper_stage", WeatheringCopper.WeatherState.WEATHERED.ordinal());
		this.setCopperStage(WeatheringCopper.WeatherState.values()[Mth.clamp(stage, 0, 3)]);
		this.entityData.set(HEART_CRACKS, input.getIntOr("heart_cracks", 0));
		if (this.hasCustomName()) {
			this.bossEvent.setName(this.getDisplayName());
		}
	}

	@Override
	public void setCustomName(Component name) {
		super.setCustomName(name);
		this.bossEvent.setName(this.getDisplayName());
	}

	@Override
	public boolean canAttack(LivingEntity target) {
		return target instanceof Player && super.canAttack(target);
	}
}
