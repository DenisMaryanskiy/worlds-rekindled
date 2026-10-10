package com.dmaryanskiy.vanillareforged.watcher;

import com.dmaryanskiy.vanillareforged.petrification.Petrification;

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
import net.minecraft.world.entity.AnimationState;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.WeatheringCopper;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

/**
 * The oldest Creaking, fitted by an ancient civilization with copper braces and a lens that turns
 * its freeze outward: whoever it watches is petrified. Woken by placing a Creaking Heart into its
 * summon pattern (see {@link WatcherSummon}).
 */
public class Watcher extends Monster {
	public static final double GAZE_RANGE = 24.0;
	public static final int GAZE_RATE = 2;
	private static final int ATTACK_ANIMATION_TICKS = 15;
	private static final byte ATTACK_EVENT = 4;

	private static final EntityDataAccessor<Integer> COPPER_STAGE = SynchedEntityData.defineId(Watcher.class,
			EntityDataSerializers.INT);

	public final AnimationState attackAnimationState = new AnimationState();
	private int attackAnimationRemainingTicks;
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
				.add(Attributes.ATTACK_DAMAGE, 10.0)
				.add(Attributes.ATTACK_KNOCKBACK, 1.5)
				.add(Attributes.KNOCKBACK_RESISTANCE, 1.0)
				.add(Attributes.FOLLOW_RANGE, 32.0)
				.add(Attributes.STEP_HEIGHT, 1.5);
	}

	@Override
	protected void registerGoals() {
		this.goalSelector.addGoal(0, new FloatGoal(this));
		this.goalSelector.addGoal(2, new MeleeAttackGoal(this, 1.0, true));
		this.goalSelector.addGoal(6, new WaterAvoidingRandomStrollGoal(this, 0.6));
		this.goalSelector.addGoal(7, new LookAtPlayerGoal(this, Player.class, 16.0F));
		this.goalSelector.addGoal(8, new RandomLookAroundGoal(this));
		this.targetSelector.addGoal(1, new NearestAttackableTargetGoal<>(this, Player.class, true));
	}

	@Override
	protected void defineSynchedData(SynchedEntityData.Builder builder) {
		super.defineSynchedData(builder);
		builder.define(COPPER_STAGE, WeatheringCopper.WeatherState.WEATHERED.ordinal());
	}

	/** The collar's oxidation stage, which its copper plates show. */
	public WeatheringCopper.WeatherState getCopperStage() {
		return WeatheringCopper.WeatherState.values()[this.entityData.get(COPPER_STAGE)];
	}

	public void setCopperStage(WeatheringCopper.WeatherState stage) {
		this.entityData.set(COPPER_STAGE, stage.ordinal());
	}

	@Override
	protected void customServerAiStep(ServerLevel level) {
		super.customServerAiStep(level);
		if (this.getTarget() instanceof Player player && this.isWatching(player)) {
			Petrification.expose(player, GAZE_RATE);
		}
		this.bossEvent.setProgress(this.getHealth() / this.getMaxHealth());
	}

	public boolean isWatching(Player player) {
		return this.distanceToSqr(player) <= GAZE_RANGE * GAZE_RANGE && this.getSensing().hasLineOfSight(player);
	}

	@Override
	public void tick() {
		super.tick();
		if (this.level().isClientSide()) {
			this.attackAnimationState.animateWhen(this.attackAnimationRemainingTicks > 0, this.tickCount);
		}
		if (this.attackAnimationRemainingTicks > 0) {
			this.attackAnimationRemainingTicks--;
		}
	}

	@Override
	public boolean hurtServer(ServerLevel level, DamageSource source, float damage) {
		// Only commands like /kill for now: the hearts on its body, its weak points, come next.
		return source.is(DamageTypeTags.BYPASSES_INVULNERABILITY) && super.hurtServer(level, source, damage);
	}

	@Override
	public boolean doHurtTarget(ServerLevel level, Entity target) {
		this.attackAnimationRemainingTicks = ATTACK_ANIMATION_TICKS;
		level.broadcastEntityEvent(this, ATTACK_EVENT);
		this.playSound(SoundEvents.CREAKING_ATTACK, 1.5F, 0.6F);
		return super.doHurtTarget(level, target);
	}

	@Override
	public void handleEntityEvent(byte id) {
		if (id == ATTACK_EVENT) {
			this.attackAnimationRemainingTicks = ATTACK_ANIMATION_TICKS;
		} else {
			super.handleEntityEvent(id);
		}
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
		return SoundEvents.CREAKING_SWAY;
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
	}

	@Override
	protected void readAdditionalSaveData(ValueInput input) {
		super.readAdditionalSaveData(input);
		int stage = input.getIntOr("copper_stage", WeatheringCopper.WeatherState.WEATHERED.ordinal());
		this.setCopperStage(WeatheringCopper.WeatherState.values()[Mth.clamp(stage, 0, 3)]);
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
