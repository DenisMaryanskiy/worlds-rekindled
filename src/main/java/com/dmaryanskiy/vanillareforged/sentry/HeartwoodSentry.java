package com.dmaryanskiy.vanillareforged.sentry;

import net.minecraft.core.BlockPos;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.AnimationState;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.MoveTowardsRestrictionGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.animal.golem.AbstractGolem;
import net.minecraft.world.entity.monster.Creeper;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LevelEvent;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.Vec3;

/**
 * Night guard built from a pale oak log, a Creaking Heart and a carved pumpkin. Active while its
 * pedestal gets no daylight; in daylight it walks back onto the pedestal and freezes as a statue.
 */
public class HeartwoodSentry extends AbstractGolem {
	public static final int HOME_RADIUS = 20;
	public static final float ATTACK_DAMAGE = 6.0F;
	public static final int MAX_CRACKS = 5;
	public static final int HEAL_INTERVAL_TICKS = 1200;
	private static final double HEAL_PLAYER_RANGE = 16.0;
	private static final int CRACK_COOLDOWN_TICKS = 10;
	private static final int RETURN_TIMEOUT_TICKS = 600;
	private static final int ATTACK_ANIMATION_TICKS = 15;
	private static final byte ATTACK_EVENT = 4;

	private static final EntityDataAccessor<Boolean> STATUE = SynchedEntityData.defineId(HeartwoodSentry.class,
			EntityDataSerializers.BOOLEAN);
	private static final EntityDataAccessor<Boolean> JACK_O_LANTERN = SynchedEntityData.defineId(HeartwoodSentry.class,
			EntityDataSerializers.BOOLEAN);
	private static final EntityDataAccessor<Integer> CRACKS = SynchedEntityData.defineId(HeartwoodSentry.class,
			EntityDataSerializers.INT);

	public final AnimationState attackAnimationState = new AnimationState();
	private int attackAnimationRemainingTicks;
	private int returnTicks;
	private int crackCooldown;
	private int healTicks;
	private final SentryLantern lantern = new SentryLantern();

	public HeartwoodSentry(EntityType<? extends HeartwoodSentry> type, Level level) {
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
	protected void registerGoals() {
		this.goalSelector.addGoal(0, new ReturnToPedestalGoal(this));
		this.goalSelector.addGoal(1, new MeleeAttackGoal(this, 1.0, false));
		this.goalSelector.addGoal(2, new MoveTowardsRestrictionGoal(this, 1.0));
		this.goalSelector.addGoal(5, new WaterAvoidingRandomStrollGoal(this, 0.6));
		this.goalSelector.addGoal(7, new LookAtPlayerGoal(this, Player.class, 6.0F));
		this.goalSelector.addGoal(8, new RandomLookAroundGoal(this));
		this.targetSelector.addGoal(1, new HurtByTargetGoal(this));
		this.targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Mob.class, 5, false, false,
				(target, level) -> target instanceof Enemy && !(target instanceof Creeper)
						&& this.isWithinHome(target.blockPosition()) && !this.isDaylightAtPedestal()));
	}

	@Override
	protected void defineSynchedData(SynchedEntityData.Builder builder) {
		super.defineSynchedData(builder);
		builder.define(STATUE, false);
		builder.define(JACK_O_LANTERN, false);
		builder.define(CRACKS, 0);
	}

	public BlockPos getPedestal() {
		return this.getHomePosition();
	}

	public void setPedestal(BlockPos pedestal) {
		this.setHomeTo(pedestal, HOME_RADIUS);
	}

	/** The spot on top of the pedestal where the statue stands. */
	public BlockPos getStandingPos() {
		return this.getPedestal().above();
	}

	public boolean isStatue() {
		return this.entityData.get(STATUE);
	}

	public boolean isJackOLantern() {
		return this.entityData.get(JACK_O_LANTERN);
	}

	public void setJackOLantern(boolean jackOLantern) {
		this.entityData.set(JACK_O_LANTERN, jackOLantern);
	}

	public int getCracks() {
		return this.entityData.get(CRACKS);
	}

	public void setCracks(int cracks) {
		this.entityData.set(CRACKS, cracks);
	}

	public boolean isDaylightAtPedestal() {
		return this.level().isBrightOutside() && this.level().canSeeSky(this.getStandingPos());
	}

	@Override
	public SpawnGroupData finalizeSpawn(ServerLevelAccessor level, DifficultyInstance difficulty, EntitySpawnReason reason,
			SpawnGroupData groupData) {
		if (!this.hasHome()) {
			this.setPedestal(this.blockPosition().below());
		}
		return super.finalizeSpawn(level, difficulty, reason, groupData);
	}

	@Override
	public void tick() {
		super.tick();
		if (this.level() instanceof ServerLevel level) {
			this.updateStatue();
			this.healCracks(level);
			this.lantern.tick(level, this);
		} else {
			this.attackAnimationState.animateWhen(this.attackAnimationRemainingTicks > 0, this.tickCount);
		}
		if (this.attackAnimationRemainingTicks > 0) {
			this.attackAnimationRemainingTicks--;
		}
	}

	private void updateStatue() {
		boolean daylight = this.isDaylightAtPedestal();
		if (this.isStatue()) {
			if (!daylight) {
				this.entityData.set(STATUE, false);
				this.setNoAi(false);
			}
			return;
		}
		if (!daylight) {
			this.returnTicks = 0;
			return;
		}
		this.setTarget(null);
		boolean home = this.position().distanceToSqr(Vec3.atBottomCenterOf(this.getStandingPos())) < 2.25;
		if (home || ++this.returnTicks > RETURN_TIMEOUT_TICKS) {
			this.freezeOnPedestal();
		}
	}

	private void freezeOnPedestal() {
		this.getNavigation().stop();
		this.snapTo(Vec3.atBottomCenterOf(this.getStandingPos()), this.getYRot(), 0.0F);
		this.setDeltaMovement(Vec3.ZERO);
		this.returnTicks = 0;
		this.setCracks(0);
		this.entityData.set(STATUE, true);
		this.setNoAi(true);
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

	@Override
	public boolean hurtServer(ServerLevel level, DamageSource source, float damage) {
		if (source.is(DamageTypeTags.BYPASSES_INVULNERABILITY)) {
			return super.hurtServer(level, source, damage);
		}
		Entity attacker = source.getEntity();
		if (this.isStatue()) {
			if (attacker instanceof Player player && source.getDirectEntity() == player && player.getMainHandItem().is(ItemTags.AXES)) {
				this.dismantle(level, player, InteractionHand.MAIN_HAND);
			}
			return false;
		}
		if (attacker instanceof Player player) {
			if (!player.isCreative() && !player.isSpectator()) {
				this.setTarget(player);
			}
		} else if (attacker instanceof Enemy && attacker instanceof LivingEntity shooter && source.getDirectEntity() != attacker
				&& this.getTarget() == null) {
			this.setTarget(shooter);
		}
		// Only through the heart: player hits and mob projectiles; mob melee and explosions do nothing.
		if (this.crackCooldown > 0 || !SentryHeart.isCrackedBy(this, source)) {
			return false;
		}
		this.crackCooldown = CRACK_COOLDOWN_TICKS;
		this.healTicks = 0;
		this.setCracks(this.getCracks() + 1);
		if (this.getCracks() < MAX_CRACKS) {
			level.broadcastDamageEvent(this, source);
			return true;
		}
		level.levelEvent(LevelEvent.PARTICLES_AND_SOUND_DESTROY_BLOCK, BlockPos.containing(SentryHeart.box(this).getCenter()),
				Block.getId(Blocks.CREAKING_HEART.defaultBlockState()));
		return super.hurtServer(level, source, Float.MAX_VALUE);
	}

	/** Sneak + axe takes apart a Sentry that isn't hunting the player; a statue can also just be chopped. */
	@Override
	protected InteractionResult mobInteract(Player player, InteractionHand hand) {
		if (!player.isSecondaryUseActive() || !player.getItemInHand(hand).is(ItemTags.AXES)) {
			return super.mobInteract(player, hand);
		}
		if (this.level() instanceof ServerLevel level) {
			if (this.getTarget() == player) {
				return InteractionResult.FAIL;
			}
			this.dismantle(level, player, hand);
		}
		return InteractionResult.SUCCESS;
	}

	/** Returns the heart and the head; the pedestal log stays where it is. */
	private void dismantle(ServerLevel level, Player player, InteractionHand hand) {
		this.spawnAtLocation(level, new ItemStack(Items.CREAKING_HEART), 1.5F);
		this.spawnAtLocation(level, new ItemStack(this.isJackOLantern() ? Items.JACK_O_LANTERN : Items.CARVED_PUMPKIN), 2.3F);
		level.levelEvent(LevelEvent.PARTICLES_AND_SOUND_DESTROY_BLOCK, this.blockPosition().above(),
				Block.getId(Blocks.PALE_OAK_WOOD.defaultBlockState()));
		player.getItemInHand(hand).hurtAndBreak(1, player, hand == InteractionHand.MAIN_HAND ? EquipmentSlot.MAINHAND : EquipmentSlot.OFFHAND);
		this.discard();
	}

	@Override
	public void onRemoval(RemovalReason reason) {
		super.onRemoval(reason);
		// When only unloaded, the saved light position lets the next load move or clear the light.
		if ((reason.shouldDestroy() || reason == RemovalReason.CHANGED_DIMENSION) && this.level() instanceof ServerLevel level) {
			this.lantern.remove(level);
		}
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
		if (hurt) {
			double resistance = target instanceof LivingEntity living ? living.getAttributeValue(Attributes.KNOCKBACK_RESISTANCE) : 0.0;
			target.setDeltaMovement(target.getDeltaMovement().add(0.0, 0.4 * Math.max(0.0, 1.0 - resistance), 0.0));
		}
		this.playSound(SoundEvents.CREAKING_ATTACK, 1.0F, 1.0F);
		return hurt;
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
		output.store("pedestal", BlockPos.CODEC, this.getPedestal());
		output.putBoolean("statue", this.isStatue());
		output.putBoolean("jack_o_lantern", this.isJackOLantern());
		output.putInt("cracks", this.getCracks());
		this.lantern.getLightPos().ifPresent(pos -> output.store("light", BlockPos.CODEC, pos));
	}

	@Override
	protected void readAdditionalSaveData(ValueInput input) {
		super.readAdditionalSaveData(input);
		input.read("pedestal", BlockPos.CODEC).ifPresent(this::setPedestal);
		this.entityData.set(STATUE, input.getBooleanOr("statue", false));
		this.setJackOLantern(input.getBooleanOr("jack_o_lantern", false));
		this.setCracks(input.getIntOr("cracks", 0));
		this.lantern.setLightPos(input.read("light", BlockPos.CODEC));
	}
}
