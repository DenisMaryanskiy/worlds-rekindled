package com.dmaryanskiy.vanillareforged.watcher;

import java.util.Arrays;
import java.util.Optional;

import com.dmaryanskiy.vanillareforged.heart.HeartHit;
import com.dmaryanskiy.vanillareforged.petrification.Petrification;

import net.minecraft.core.BlockPos;
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
import net.minecraft.util.Mth;
import net.minecraft.world.BossEvent;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LevelEvent;
import net.minecraft.world.level.block.WeatheringCopper;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/**
 * The oldest Creaking, fitted by an ancient civilization with copper braces and a lens that turns
 * its freeze outward: whoever it watches is petrified. Woken by placing a Creaking Heart into its
 * summon pattern (see {@link WatcherSummon}).
 * <p>
 * It can only be hurt through the hearts on its body (see {@link WatcherHeart}). Up close it slams
 * a branch arm down: the hit shatters a petrified player's stone shell, and the arm then stays stuck
 * in the ground for a moment, exposing its heart.
 */
public class Watcher extends Monster {
	public static final double GAZE_RANGE = 24.0;
	public static final int GAZE_RATE = 2;
	public static final int CORE_GAZE_RATE = 3;
	public static final double SLAM_REACH = 3.5;
	public static final float SLAM_DAMAGE = 10.0F;
	public static final int SLAM_WINDUP_TICKS = 15;
	public static final int SLAM_STUCK_TICKS = 30;
	private static final int SLAM_COOLDOWN_TICKS = 40;
	private static final double SLAM_DISTANCE = 1.8;
	private static final double SLAM_RADIUS = 2.2;
	private static final int CRACK_COOLDOWN_TICKS = 10;
	public static final double RESIN_MIN_RANGE = 6.0;
	public static final int RESIN_INTERVAL_TICKS = 60;
	private static final int RESIN_FIRST_TICKS = 20;
	public static final int STUN_TICKS = 60;
	public static final int STUN_IMMUNITY_TICKS = 100;

	private static final EntityDataAccessor<Integer> COPPER_STAGE = SynchedEntityData.defineId(Watcher.class,
			EntityDataSerializers.INT);
	private static final EntityDataAccessor<Integer> HEART_CRACKS = SynchedEntityData.defineId(Watcher.class,
			EntityDataSerializers.INT);
	private static final EntityDataAccessor<Integer> SLAM_PHASE = SynchedEntityData.defineId(Watcher.class,
			EntityDataSerializers.INT);
	private static final EntityDataAccessor<Boolean> SLAM_RIGHT_ARM = SynchedEntityData.defineId(Watcher.class,
			EntityDataSerializers.BOOLEAN);
	private static final EntityDataAccessor<Boolean> STUNNED = SynchedEntityData.defineId(Watcher.class,
			EntityDataSerializers.BOOLEAN);

	public enum SlamPhase {
		NONE, WINDUP, STUCK
	}

	private int slamTicks;
	private int slamPhaseAge;
	private int slamCooldown;
	private float slamYaw;
	private int crackCooldown;
	private int resinCooldown = RESIN_FIRST_TICKS;
	private int stunTicks;
	private int stunImmunity;
	private final ServerBossEvent bossEvent = new ServerBossEvent(Mth.createInsecureUUID(this.random), this.getDisplayName(),
			BossEvent.BossBarColor.YELLOW, BossEvent.BossBarOverlay.PROGRESS);

	public Watcher(EntityType<? extends Watcher> type, Level level) {
		super(type, level);
		this.xpReward = 50;
	}

	public static AttributeSupplier.Builder createAttributes() {
		return Monster.createMonsterAttributes()
				.add(Attributes.MAX_HEALTH, 100.0)
				.add(Attributes.MOVEMENT_SPEED, 0.25)
				.add(Attributes.KNOCKBACK_RESISTANCE, 1.0)
				.add(Attributes.FOLLOW_RANGE, 32.0)
				.add(Attributes.STEP_HEIGHT, 1.5);
	}

	@Override
	protected void registerGoals() {
		this.goalSelector.addGoal(0, new FloatGoal(this));
		this.goalSelector.addGoal(2, new WatcherSlamGoal(this));
		this.goalSelector.addGoal(6, new WaterAvoidingRandomStrollGoal(this, 0.6));
		this.goalSelector.addGoal(7, new LookAtPlayerGoal(this, Player.class, 16.0F));
		this.goalSelector.addGoal(8, new RandomLookAroundGoal(this));
		this.targetSelector.addGoal(1, new NearestAttackableTargetGoal<>(this, Player.class, true));
	}

	@Override
	protected void defineSynchedData(SynchedEntityData.Builder builder) {
		super.defineSynchedData(builder);
		builder.define(COPPER_STAGE, WeatheringCopper.WeatherState.WEATHERED.ordinal());
		builder.define(HEART_CRACKS, 0);
		builder.define(SLAM_PHASE, SlamPhase.NONE.ordinal());
		builder.define(SLAM_RIGHT_ARM, true);
		builder.define(STUNNED, false);
	}

	/** The collar's oxidation stage, which its copper plates show. */
	public WeatheringCopper.WeatherState getCopperStage() {
		return WeatheringCopper.WeatherState.values()[this.entityData.get(COPPER_STAGE)];
	}

	public void setCopperStage(WeatheringCopper.WeatherState stage) {
		this.entityData.set(COPPER_STAGE, stage.ordinal());
	}

	public int getCracks(WatcherHeart heart) {
		return heart.cracksIn(this.entityData.get(HEART_CRACKS));
	}

	public void setCracks(WatcherHeart heart, int cracks) {
		this.entityData.set(HEART_CRACKS, heart.withCracks(this.entityData.get(HEART_CRACKS), cracks));
	}

	public boolean isDestroyed(WatcherHeart heart) {
		return this.getCracks(heart) >= heart.maxCracks();
	}

	/** The core only opens once every other heart is destroyed. */
	public boolean isCoreOpen() {
		return Arrays.stream(WatcherHeart.values()).allMatch(heart -> heart == WatcherHeart.CORE || this.isDestroyed(heart));
	}

	/** Whether the heart can be hit right now; these are the hearts that glow. */
	public boolean isVulnerable(WatcherHeart heart) {
		if (this.isDestroyed(heart)) {
			return false;
		}
		// While it's bent over its stuck arm, only that arm's heart is in reach.
		if (this.getSlamPhase() == SlamPhase.STUCK) {
			return this.isStuckArm(heart);
		}
		// Its arms only hang still enough to hit while it's stunned.
		if (heart.isArm()) {
			return this.isStunned();
		}
		return heart != WatcherHeart.CORE || this.isCoreOpen();
	}

	/** Whether this is the heart of the arm that's planted in the ground right now. */
	public boolean isStuckArm(WatcherHeart heart) {
		return heart.isArm() && this.getSlamPhase() == SlamPhase.STUCK
				&& (heart == WatcherHeart.RIGHT_ARM) == this.isSlammingRightArm();
	}

	public boolean isStunned() {
		return this.entityData.get(STUNNED);
	}

	/**
	 * Its own resin, hit back at it, stuns it: it stops gazing, turning and attacking, and its arms
	 * hang with their hearts open. It can't be stunned again right away.
	 */
	public void stun() {
		if (this.isStunned() || this.stunImmunity > 0) {
			return;
		}
		this.entityData.set(STUNNED, true);
		this.entityData.set(SLAM_PHASE, SlamPhase.NONE.ordinal());
		this.stunTicks = STUN_TICKS;
		this.slamYaw = this.yBodyRot;
		this.getNavigation().stop();
		this.playSound(SoundEvents.CREAKING_FREEZE, 1.5F, 0.6F);
	}

	public SlamPhase getSlamPhase() {
		return SlamPhase.values()[this.entityData.get(SLAM_PHASE)];
	}

	public boolean isSlammingRightArm() {
		return this.entityData.get(SLAM_RIGHT_ARM);
	}

	public boolean canStartSlam() {
		return !this.isStunned() && this.getSlamPhase() == SlamPhase.NONE && this.slamCooldown == 0;
	}

	/** Raises an arm; after the wind-up it strikes the ground in front, where the Watcher faces now. */
	public void startSlam() {
		if (!this.canStartSlam()) {
			return;
		}
		this.getNavigation().stop();
		this.slamYaw = this.yBodyRot;
		this.slamTicks = 0;
		this.entityData.set(SLAM_PHASE, SlamPhase.WINDUP.ordinal());
		this.playSound(SoundEvents.CREAKING_ATTACK, 1.5F, 0.6F);
	}

	@Override
	protected void customServerAiStep(ServerLevel level) {
		super.customServerAiStep(level);
		if (this.stunImmunity > 0) {
			this.stunImmunity--;
		}
		if (this.isStunned()) {
			this.holdStill();
			if (--this.stunTicks <= 0) {
				this.entityData.set(STUNNED, false);
				this.stunImmunity = STUN_IMMUNITY_TICKS;
			}
		} else {
			this.tickSlam(level);
			// Bent over its stuck arm, the lens faces the ground.
			if (this.getSlamPhase() != SlamPhase.STUCK && this.getTarget() instanceof Player player && this.isWatching(player)) {
				Petrification.expose(player, this.isCoreOpen() ? CORE_GAZE_RATE : GAZE_RATE);
				this.tickResin(level, player);
			}
		}
		int cracks = Arrays.stream(WatcherHeart.values()).mapToInt(this::getCracks).sum();
		this.bossEvent.setProgress(1.0F - cracks / (float) WatcherHeart.TOTAL_CRACKS);
	}

	private void tickSlam(ServerLevel level) {
		if (this.slamCooldown > 0) {
			this.slamCooldown--;
		}
		SlamPhase phase = this.getSlamPhase();
		if (phase == SlamPhase.NONE) {
			return;
		}
		// The strike lands where it aimed when it raised the arm, so stepping aside dodges it.
		this.holdStill();
		this.slamTicks++;
		if (phase == SlamPhase.WINDUP && this.slamTicks >= SLAM_WINDUP_TICKS) {
			this.strike(level);
			this.slamTicks = 0;
			this.entityData.set(SLAM_PHASE, SlamPhase.STUCK.ordinal());
		} else if (phase == SlamPhase.STUCK && this.slamTicks >= SLAM_STUCK_TICKS) {
			this.entityData.set(SLAM_PHASE, SlamPhase.NONE.ordinal());
			this.entityData.set(SLAM_RIGHT_ARM, !this.isSlammingRightArm());
			this.slamCooldown = SLAM_COOLDOWN_TICKS;
		}
	}

	private void holdStill() {
		this.getNavigation().stop();
		this.setYRot(this.slamYaw);
		this.yBodyRot = this.slamYaw;
		this.yHeadRot = this.slamYaw;
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
		resin.setPos(from.add(direction.scale(1.5)));
		level.addFreshEntity(resin);
		this.playSound(SoundEvents.BLAZE_SHOOT, 1.5F, 0.6F);
	}

	private void strike(ServerLevel level) {
		Vec3 point = this.position().add(Vec3.directionFromRotation(0.0F, this.slamYaw).scale(SLAM_DISTANCE));
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
		if (this.crackCooldown > 0) {
			this.crackCooldown--;
		}
	}

	/** Ticks since the slam phase last changed, for the client's pose. */
	public int getSlamPhaseAge() {
		return this.slamPhaseAge;
	}

	public boolean isWatching(Player player) {
		return this.distanceToSqr(player) <= GAZE_RANGE * GAZE_RANGE && this.getSensing().hasLineOfSight(player);
	}

	@Override
	public boolean hurtServer(ServerLevel level, DamageSource source, float damage) {
		if (source.is(DamageTypeTags.BYPASSES_INVULNERABILITY)) {
			return super.hurtServer(level, source, damage);
		}
		// Only players can hurt it, and only through a heart that's open to them.
		Optional<HeartHit> hit = HeartHit.of(source, damage, false);
		if (hit.isEmpty() || this.crackCooldown > 0) {
			return false;
		}
		for (WatcherHeart heart : WatcherHeart.values()) {
			if (this.isVulnerable(heart) && heart.isHitBy(this, hit.get())) {
				return this.crack(level, source, heart);
			}
		}
		return false;
	}

	private boolean crack(ServerLevel level, DamageSource source, WatcherHeart heart) {
		this.crackCooldown = CRACK_COOLDOWN_TICKS;
		this.setCracks(heart, this.getCracks(heart) + 1);
		if (this.isDestroyed(heart)) {
			level.levelEvent(LevelEvent.PARTICLES_AND_SOUND_DESTROY_BLOCK, BlockPos.containing(heart.box(this).getCenter()),
					Block.getId(Blocks.CREAKING_HEART.defaultBlockState()));
			if (heart == WatcherHeart.CORE) {
				return super.hurtServer(level, source, Float.MAX_VALUE);
			}
		}
		level.broadcastDamageEvent(this, source);
		return true;
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
