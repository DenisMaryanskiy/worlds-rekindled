package com.dmaryanskiy.vanillareforged.sentry;

import com.dmaryanskiy.vanillareforged.heart.HeartHit;

import net.minecraft.core.BlockPos;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.AnimationState;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.animal.golem.AbstractGolem;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LevelEvent;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

/**
 * What a player's Heartwood Sentry and the Ruin's guards share: a Creaking's body around a Creaking
 * Heart, which is the only part of it that can be hurt (see {@link SentryHeart}), and its long-armed
 * melee.
 */
public abstract class AbstractSentry extends AbstractGolem {
	public static final float ATTACK_DAMAGE = 6.0F;
	public static final int HEAL_INTERVAL_TICKS = 1200;
	private static final double HEAL_PLAYER_RANGE = 16.0;
	private static final int CRACK_COOLDOWN_TICKS = 10;
	private static final int ATTACK_ANIMATION_TICKS = 15;
	private static final byte ATTACK_EVENT = 4;

	private static final EntityDataAccessor<Integer> CRACKS = SynchedEntityData.defineId(AbstractSentry.class,
			EntityDataSerializers.INT);

	public final AnimationState attackAnimationState = new AnimationState();
	private int attackAnimationRemainingTicks;
	private int crackCooldown;
	private int healTicks;

	protected AbstractSentry(EntityType<? extends AbstractSentry> type, Level level) {
		super(type, level);
	}

	public static AttributeSupplier.Builder createAttributes() {
		return Mob.createMobAttributes()
				.add(Attributes.MAX_HEALTH, 40.0)
				.add(Attributes.MOVEMENT_SPEED, 0.3)
				.add(Attributes.KNOCKBACK_RESISTANCE, 1.0)
				.add(Attributes.ATTACK_DAMAGE, ATTACK_DAMAGE)
				.add(Attributes.FOLLOW_RANGE, 24.0)
				.add(Attributes.STEP_HEIGHT, 1.0);
	}

	@Override
	protected void defineSynchedData(SynchedEntityData.Builder builder) {
		super.defineSynchedData(builder);
		builder.define(CRACKS, 0);
	}

	/** How many cracks destroy the heart. */
	public abstract int getMaxCracks();

	/** A statue is frozen: it does nothing and nothing hurts it. */
	public abstract boolean isStatue();

	/** Whether it's out to get the player right now, which is when the Pale Mask's gaze slows it. */
	public abstract boolean isHostileTo(Player player);

	public int getCracks() {
		return this.entityData.get(CRACKS);
	}

	public void setCracks(int cracks) {
		this.entityData.set(CRACKS, cracks);
	}

	@Override
	public void tick() {
		super.tick();
		if (this.level() instanceof ServerLevel level) {
			this.healCracks(level);
		} else {
			this.attackAnimationState.animateWhen(this.attackAnimationRemainingTicks > 0, this.tickCount);
		}
		if (this.attackAnimationRemainingTicks > 0) {
			this.attackAnimationRemainingTicks--;
		}
	}

	/** Out of combat (no target, no player around), the heart mends one crack a minute. */
	private void healCracks(ServerLevel level) {
		if (this.crackCooldown > 0) {
			this.crackCooldown--;
		}
		if (this.getCracks() == 0 || this.getTarget() != null || level.getNearestPlayer(this, HEAL_PLAYER_RANGE) != null) {
			this.healTicks = 0;
		} else if (++this.healTicks >= HEAL_INTERVAL_TICKS) {
			this.healTicks = 0;
			this.setCracks(this.getCracks() - 1);
		}
	}

	/**
	 * Only through the heart: player hits and mob projectiles; mob melee and explosions do nothing.
	 * The last crack destroys the heart and kills the Sentry.
	 */
	protected boolean hurtHeart(ServerLevel level, DamageSource source) {
		if (this.crackCooldown > 0 || !SentryHeart.isCrackedBy(this, source)) {
			return false;
		}
		this.crackCooldown = CRACK_COOLDOWN_TICKS;
		this.healTicks = 0;
		// A critical hit cracks it twice: its one crack per hit otherwise leaves a well-timed jump no better than a tap.
		this.setCracks(Math.min(this.getCracks() + (HeartHit.isCritical(source) ? 2 : 1), this.getMaxCracks()));
		if (this.getCracks() < this.getMaxCracks()) {
			level.broadcastDamageEvent(this, source);
			return true;
		}
		level.levelEvent(LevelEvent.PARTICLES_AND_SOUND_DESTROY_BLOCK, BlockPos.containing(SentryHeart.box(this).getCenter()),
				Block.getId(Blocks.CREAKING_HEART.defaultBlockState()));
		return super.hurtServer(level, source, Float.MAX_VALUE);
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
	public boolean isPushable() {
		return !this.isStatue() && super.isPushable();
	}

	@Override
	public boolean doHurtTarget(ServerLevel level, Entity target) {
		this.attackAnimationRemainingTicks = ATTACK_ANIMATION_TICKS;
		level.broadcastEntityEvent(this, ATTACK_EVENT);
		DamageSource damageSource = this.damageSources().mobAttack(this);
		boolean hurt = target.hurtServer(level, damageSource, (float) this.getAttributeValue(Attributes.ATTACK_DAMAGE));
		if (hurt && this.tossesItsTarget()) {
			double resistance = target instanceof LivingEntity living ? living.getAttributeValue(Attributes.KNOCKBACK_RESISTANCE) : 0.0;
			target.setDeltaMovement(target.getDeltaMovement().add(0.0, 0.4 * Math.max(0.0, 1.0 - resistance), 0.0));
		}
		this.playSound(SoundEvents.CREAKING_ATTACK, 1.0F, 1.0F);
		return hurt;
	}

	/** Whether its blow throws its target into the air, as an iron golem's does. */
	protected boolean tossesItsTarget() {
		return true;
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
	protected void addAdditionalSaveData(ValueOutput output) {
		super.addAdditionalSaveData(output);
		output.putInt("cracks", this.getCracks());
	}

	@Override
	protected void readAdditionalSaveData(ValueInput input) {
		super.readAdditionalSaveData(input);
		this.setCracks(input.getIntOr("cracks", 0));
	}
}
