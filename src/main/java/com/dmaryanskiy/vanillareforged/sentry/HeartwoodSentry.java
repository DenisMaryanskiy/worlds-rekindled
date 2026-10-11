package com.dmaryanskiy.vanillareforged.sentry;

import net.minecraft.core.BlockPos;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.MoveTowardsRestrictionGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
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
public class HeartwoodSentry extends AbstractSentry {
	public static final int HOME_RADIUS = 20;
	public static final int MAX_CRACKS = 5;
	private static final int RETURN_TIMEOUT_TICKS = 600;

	private static final EntityDataAccessor<Boolean> STATUE = SynchedEntityData.defineId(HeartwoodSentry.class,
			EntityDataSerializers.BOOLEAN);
	private static final EntityDataAccessor<Boolean> JACK_O_LANTERN = SynchedEntityData.defineId(HeartwoodSentry.class,
			EntityDataSerializers.BOOLEAN);

	private int returnTicks;
	private final SentryLantern lantern = new SentryLantern();

	public HeartwoodSentry(EntityType<? extends HeartwoodSentry> type, Level level) {
		super(type, level);
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

	@Override
	public boolean isStatue() {
		return this.entityData.get(STATUE);
	}

	@Override
	public int getMaxCracks() {
		return MAX_CRACKS;
	}

	/** It isn't a monster: looking at your own guard is free. */
	@Override
	public boolean isHostileTo(Player player) {
		return !this.isStatue() && this.getTarget() == player;
	}

	public boolean isJackOLantern() {
		return this.entityData.get(JACK_O_LANTERN);
	}

	public void setJackOLantern(boolean jackOLantern) {
		this.entityData.set(JACK_O_LANTERN, jackOLantern);
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
			this.lantern.tick(level, this);
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
		return this.hurtHeart(level, source);
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
	protected void addAdditionalSaveData(ValueOutput output) {
		super.addAdditionalSaveData(output);
		output.store("pedestal", BlockPos.CODEC, this.getPedestal());
		output.putBoolean("statue", this.isStatue());
		output.putBoolean("jack_o_lantern", this.isJackOLantern());
		this.lantern.getLightPos().ifPresent(pos -> output.store("light", BlockPos.CODEC, pos));
	}

	@Override
	protected void readAdditionalSaveData(ValueInput input) {
		super.readAdditionalSaveData(input);
		input.read("pedestal", BlockPos.CODEC).ifPresent(this::setPedestal);
		this.entityData.set(STATUE, input.getBooleanOr("statue", false));
		this.setJackOLantern(input.getBooleanOr("jack_o_lantern", false));
		this.lantern.setLightPos(input.read("light", BlockPos.CODEC));
	}
}
