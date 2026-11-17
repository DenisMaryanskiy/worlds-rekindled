package com.dmaryanskiy.vanillareforged.elderwood;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

import com.dmaryanskiy.vanillareforged.ModCriteria;
import com.dmaryanskiy.vanillareforged.ModItems;
import com.dmaryanskiy.vanillareforged.ModSounds;
import com.dmaryanskiy.vanillareforged.heart.HeartHit;

import net.minecraft.core.BlockPos;
import net.minecraft.core.UUIDUtil;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerBossEvent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.tags.DamageTypeTags;
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
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.WeatheringCopper;
import net.minecraft.world.level.material.PushReaction;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.Vec3;

/**
 * The oldest pale oak, woken whole by its Creaking Heart and fitted by an ancient civilization with
 * copper bands and a lens that turns the Creakings' freeze outward: whoever it watches is petrified.
 * Grown by placing a Creaking Heart on its stump (see {@link ElderwoodSummon}).
 * <p>
 * It's rooted: it never leaves its spot and only turns, slower than a player can circle it. It can
 * only be hurt through its hearts (see {@link ElderwoodHeart}).
 * <p>
 * This class holds its state and its life from growing to dying. What it does to players lives in
 * its parts: the gaze and the resin in {@link ElderwoodLens}, the branch slam in
 * {@link ElderwoodSlam}, the roots in {@link ElderwoodRoots}, and its death in {@link ElderwoodDecay}.
 */
public class Elderwood extends Monster {
	// These four are its oxidized copper tier's; see ElderwoodTier for the others.
	public static final float TURN_DEGREES_PER_TICK = 1.5F;
	public static final int GROW_STAGES = 3;
	public static final int GROW_STAGE_TICKS = 20;
	public static final int GROW_TICKS = GROW_STAGES * GROW_STAGE_TICKS;
	private static final float GROW_SNAP_TICKS = 3.0F;
	public static final int STUN_TICKS = 60;
	public static final int STUN_IMMUNITY_TICKS = 100;
	// As far as the lens sees, so no spot is out of reach of both. At 12 the hall's corners, about 15
	// from the trunk, got roots no more and the lens only when nothing stood in its line.
	public static final double ROOT_RANGE = ElderwoodLens.RANGE;
	public static final int ROOT_INTERVAL_TICKS = 80;
	private static final int ROOT_FIRST_TICKS = 20;
	private static final int ROOT_GROUND_SEARCH = 4;
	// Long enough to die, come back from a respawn point at the Ruin's entrance and find it as it was left.
	public static final int DORMANT_AFTER_TICKS = 1800;
	public static final int HEAL_INTERVAL_TICKS = 100;
	// Players this close when it wakes make it a group's fight, up to this many.
	public static final double PLAYER_RANGE = 24.0;
	public static final int MAX_PLAYERS = 4;
	private static final int CRACK_COOLDOWN_TICKS = 10;
	private static final double HEART_DEPTH = 0.75;
	private static final ElderwoodHeart[] BRANCH_HEARTS = {ElderwoodHeart.RIGHT_BRANCH, ElderwoodHeart.LEFT_BRANCH};
	private static final SlamPhase[] SLAM_PHASES = SlamPhase.values();
	private static final WeatheringCopper.WeatherState[] COPPER_STAGES = WeatheringCopper.WeatherState.values();
	static final BlockParticleOption SPLINTERS = new BlockParticleOption(ParticleTypes.BLOCK, Blocks.PALE_OAK_LOG.defaultBlockState());
	private static final BlockParticleOption RESIN_CHIPS = new BlockParticleOption(ParticleTypes.BLOCK, Blocks.RESIN_BLOCK.defaultBlockState());
	private static final BlockParticleOption HEART_SHARDS = new BlockParticleOption(ParticleTypes.BLOCK,
			Blocks.CREAKING_HEART.defaultBlockState());

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
	private static final EntityDataAccessor<Boolean> DORMANT = SynchedEntityData.defineId(Elderwood.class,
			EntityDataSerializers.BOOLEAN);
	private static final EntityDataAccessor<Integer> PLAYERS = SynchedEntityData.defineId(Elderwood.class,
			EntityDataSerializers.INT);

	public enum SlamPhase {
		NONE, WINDUP, STUCK
	}

	private final ElderwoodLens lens = new ElderwoodLens(this);
	private final ElderwoodSlam slam = new ElderwoodSlam(this);
	private final ElderwoodBranchHeart[] branchHearts = new ElderwoodBranchHeart[BRANCH_HEARTS.length];
	private final ServerBossEvent bossEvent = new ServerBossEvent(Mth.createInsecureUUID(this.random), this.getDisplayName(),
			BossEvent.BossBarColor.YELLOW, BossEvent.BossBarOverlay.PROGRESS);
	private int growAge;
	private int slamPhaseAge;
	private int stunTicks;
	private int stunImmunity;
	private int rootCooldown = ROOT_FIRST_TICKS;
	private int crackCooldown;
	private int idleTicks;
	private DamageSource killedBy;
	private final Set<UUID> petrifiedThisFight = new HashSet<>();
	private final Set<UUID> crackedThisFight = new HashSet<>();

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
		builder.define(COPPER_STAGE, WeatheringCopper.WeatherState.UNAFFECTED.ordinal());
		builder.define(HEART_CRACKS, 0);
		builder.define(SLAM_PHASE, SlamPhase.NONE.ordinal());
		builder.define(SLAM_RIGHT_BRANCH, true);
		builder.define(SLAM_RIGHT, 0.0F);
		builder.define(SLAM_FORWARD, (float) ElderwoodSlam.DISTANCE);
		builder.define(STUNNED, false);
		builder.define(GROWING, false);
		builder.define(DORMANT, false);
		builder.define(PLAYERS, 1);
	}

	/** The collar's oxidation stage, which its copper plates show. */
	public WeatheringCopper.WeatherState getCopperStage() {
		return COPPER_STAGES[this.entityData.get(COPPER_STAGE)];
	}

	public void setCopperStage(WeatheringCopper.WeatherState stage) {
		this.entityData.set(COPPER_STAGE, stage.ordinal());
		this.showCracksOnTheBossBar();
	}

	/** How many players it woke among: every heart takes a crack more for each past the first. */
	public int getPlayers() {
		return this.entityData.get(PLAYERS);
	}

	public void setPlayers(int players) {
		this.entityData.set(PLAYERS, Mth.clamp(players, 1, MAX_PLAYERS));
		this.showCracksOnTheBossBar();
	}

	/** How many cracks destroy the heart: its tier's, and one more for each player past the first. */
	public int maxCracks(ElderwoodHeart heart) {
		return this.getTier().maxCracks(heart) + this.getPlayers() - 1;
	}

	/** How hard it fights and what it leaves, which its collar sets. */
	public ElderwoodTier getTier() {
		return ElderwoodTier.of(this.getCopperStage());
	}

	Vec3 facing() {
		return Vec3.directionFromRotation(0.0F, this.yBodyRot);
	}

	float yawTo(Entity target) {
		return (float) (Mth.atan2(target.getZ() - this.getZ(), target.getX() - this.getX()) * Mth.RAD_TO_DEG) - 90.0F;
	}

	// The whole trunk turns as one: there's no head to look around with.
	private void face(float yaw) {
		this.setYRot(yaw);
		this.yBodyRot = yaw;
		this.yHeadRot = yaw;
	}

	// Vanilla lets a standing mob's body trail its head by up to a quarter turn before it follows. On
	// the client that drew the trunk, with its hearts and lens, far from where the server had them.
	@Override
	protected void tickHeadTurn(float yBodyRotT) {
		this.yBodyRot = this.getYRot();
	}

	public int getCracks(ElderwoodHeart heart) {
		return heart.cracksIn(this.entityData.get(HEART_CRACKS));
	}

	public void setCracks(ElderwoodHeart heart, int cracks) {
		this.entityData.set(HEART_CRACKS, heart.withCracks(this.entityData.get(HEART_CRACKS), cracks));
		this.showCracksOnTheBossBar();
	}

	public boolean isDestroyed(ElderwoodHeart heart) {
		return this.getCracks(heart) >= this.maxCracks(heart);
	}

	/** The core only opens once every other heart is destroyed. */
	public boolean isCoreOpen() {
		for (ElderwoodHeart heart : ElderwoodHeart.ALL) {
			if (heart != ElderwoodHeart.CORE && !this.isDestroyed(heart)) {
				return false;
			}
		}
		return true;
	}

	/** Whether the heart can be hit right now; these are the hearts that glow. */
	public boolean isVulnerable(ElderwoodHeart heart) {
		if (this.isDestroyed(heart) || this.isGrowing() || !this.isAlive()) {
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

	private void showCracksOnTheBossBar() {
		int cracks = 0;
		int total = 0;
		for (ElderwoodHeart heart : ElderwoodHeart.ALL) {
			cracks += this.getCracks(heart);
			total += this.maxCracks(heart);
		}
		this.bossEvent.setProgress(1.0F - cracks / (float) total);
	}

	@Override
	public boolean hurtServer(ServerLevel level, DamageSource source, float damage) {
		if (source.is(DamageTypeTags.BYPASSES_INVULNERABILITY)) {
			return super.hurtServer(level, source, damage);
		}
		// Only players can hurt it, and only through a heart that's open to them.
		ElderwoodHeart heart = HeartHit.of(source, false).map(this::intoTheTrunk).map(this::openHeartOn).orElse(null);
		return heart != null && this.crackCooldown == 0 && this.crack(level, source, heart);
	}

	/** A hit that landed on one heart for certain, passed on by a branch heart's own entity. */
	public boolean hurtHeart(ServerLevel level, ElderwoodHeart heart, DamageSource source) {
		return this.isVulnerable(heart) && HeartHit.of(source, false).isPresent() && this.crackCooldown == 0
				&& this.crack(level, source, heart);
	}

	// Its box doesn't turn with it: turned, a trunk heart sits up to this deep inside the box's corner,
	// and a swing at the very end of a player's reach would stop at the box's face, short of it.
	private HeartHit intoTheTrunk(HeartHit hit) {
		return hit.carriedOn(HEART_DEPTH);
	}

	private ElderwoodHeart openHeartOn(HeartHit hit) {
		for (ElderwoodHeart heart : ElderwoodHeart.ALL) {
			if (this.isVulnerable(heart) && heart.isHitBy(this, hit)) {
				return heart;
			}
		}
		return null;
	}

	private boolean crack(ServerLevel level, DamageSource source, ElderwoodHeart heart) {
		this.crackCooldown = CRACK_COOLDOWN_TICKS;
		if (source.getEntity() instanceof Player player) {
			this.crackedThisFight.add(player.getUUID());
		}
		this.setCracks(heart, this.getCracks(heart) + 1);
		// Every crack shows on the heart that took it: resin chips, and the heart's own shards when it breaks.
		Vec3 at = heart.box(this).getCenter();
		boolean destroyed = this.isDestroyed(heart);
		level.sendParticles(destroyed ? HEART_SHARDS : RESIN_CHIPS, at.x, at.y, at.z, destroyed ? 40 : 12, 0.2, 0.2, 0.2, 0.1);
		if (destroyed) {
			this.playSound(ModSounds.ELDERWOOD_HEART_BREAK, 2.0F, 0.8F);
			if (heart == ElderwoodHeart.CORE) {
				return super.hurtServer(level, source, Float.MAX_VALUE);
			}
		}
		level.broadcastDamageEvent(this, source);
		return true;
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

	public boolean isGrowing() {
		return this.entityData.get(GROWING);
	}

	/** Makes it rise out of its stump: until it's grown it can't be hurt and does nothing. */
	public void startGrowing() {
		this.entityData.set(GROWING, true);
		this.playSound(ModSounds.ELDERWOOD_GROW, 2.0F, 0.5F);
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

	private void tickGrowth(ServerLevel level) {
		this.pushOutOfTheTrunk(level);
		if (this.growAge >= GROW_TICKS) {
			this.entityData.set(GROWING, false);
		} else if (this.growAge > 0 && this.growAge % GROW_STAGE_TICKS == 0) {
			this.burstToNextSize(level);
		}
		this.face(this.yBodyRot);
	}

	private void burstToNextSize(ServerLevel level) {
		float size = this.getGrowStage() / (float) GROW_STAGES;
		double height = this.getBbHeight() * size;
		double width = this.getBbWidth() * size;
		level.sendParticles(SPLINTERS, this.getX(), this.getY() + height / 2.0, this.getZ(), (int) (120 * size),
				width / 2.0, height / 3.0, width / 2.0, 0.1);
		this.playSound(ModSounds.ELDERWOOD_SPLINTER, 2.0F, 0.6F);
		this.playSound(ModSounds.ELDERWOOD_GROW, 2.0F, 0.4F + 0.15F * this.getGrowStage());
	}

	// The trunk becomes solid once it's grown; nobody should end up inside it.
	private void pushOutOfTheTrunk(ServerLevel level) {
		for (Entity entity : level.getEntities(this, this.getBoundingBox(), e -> !e.isSpectator() && e.isPushable())) {
			Vec3 away = new Vec3(entity.getX() - this.getX(), 0.0, entity.getZ() - this.getZ());
			Vec3 push = (away.lengthSqr() < 1.0E-4 ? this.facing() : away.normalize()).scale(0.2);
			entity.push(push.x, 0.0, push.z);
		}
	}

	@Override
	protected void customServerAiStep(ServerLevel level) {
		super.customServerAiStep(level);
		if (this.isGrowing()) {
			this.tickGrowth(level);
			return;
		}
		if (this.stunImmunity > 0) {
			this.stunImmunity--;
		}
		LivingEntity target = this.getTarget();
		// It turns only while it's free: a planted branch or a stun holds it where it faced.
		boolean free = !this.isStunned() && this.getSlamPhase() == SlamPhase.NONE;
		this.face(free && target != null ? Mth.approachDegrees(this.yBodyRot, this.yawTo(target), this.getTier().turnDegreesPerTick()) : this.yBodyRot);
		if (this.isStunned()) {
			if (--this.stunTicks <= 0) {
				this.entityData.set(STUNNED, false);
				this.stunImmunity = STUN_IMMUNITY_TICKS;
				this.lens.restAfterStun();
			}
		} else {
			this.slam.tick(level);
			if (target != null) {
				this.attack(level, target);
			}
		}
		this.tickDormancy();
	}

	private void attack(ServerLevel level, LivingEntity target) {
		if (this.slam.reaches(target)) {
			this.slam.start(target.position());
		}
		if (!(target instanceof Player player)) {
			return;
		}
		// While a branch is planted it strains at it and does not gaze.
		if (this.getSlamPhase() != SlamPhase.STUCK && this.lens.sees(player)) {
			this.lens.watch(level, player);
		}
		this.tickRoots(level, player);
	}

	public boolean isWatching(Player player) {
		return this.lens.sees(player);
	}

	void rememberPetrifying(Player player) {
		this.petrifiedThisFight.add(player.getUUID());
	}

	/** Whether its gaze has turned the player to stone since it was last whole. */
	public boolean hasPetrified(Player player) {
		return this.petrifiedThisFight.contains(player.getUUID());
	}

	public SlamPhase getSlamPhase() {
		return SLAM_PHASES[this.entityData.get(SLAM_PHASE)];
	}

	void setSlamPhase(SlamPhase phase) {
		this.entityData.set(SLAM_PHASE, phase.ordinal());
	}

	public boolean isSlammingRightBranch() {
		return this.entityData.get(SLAM_RIGHT_BRANCH);
	}

	void switchSlamBranch() {
		this.entityData.set(SLAM_RIGHT_BRANCH, !this.isSlammingRightBranch());
	}

	public float getSlamRight() {
		return this.entityData.get(SLAM_RIGHT);
	}

	public float getSlamForward() {
		return this.entityData.get(SLAM_FORWARD);
	}

	void aimSlam(float right, float forward) {
		this.entityData.set(SLAM_RIGHT, right);
		this.entityData.set(SLAM_FORWARD, forward);
	}

	/** Ticks since the slam phase last changed, for the client's pose. */
	public int getSlamPhaseAge() {
		return this.slamPhaseAge;
	}

	/** Slams straight ahead. */
	public void startSlam() {
		this.startSlam(this.position().add(this.facing().scale(ElderwoodSlam.DISTANCE)));
	}

	/** Slams at {@code point}, or as near to it as its branches reach. */
	public void startSlam(Vec3 point) {
		this.slam.start(point);
	}

	public boolean isStunned() {
		return this.entityData.get(STUNNED);
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
		this.setSlamPhase(SlamPhase.NONE);
		this.stunTicks = STUN_TICKS;
		this.playSound(ModSounds.ELDERWOOD_STUN, 1.5F, 0.6F);
	}

	/**
	 * Roots from below for a player its branches don't deal with: one its lens can't see (beside it,
	 * behind it or behind cover), and one it sees beyond their reach. Seen at range, a player had
	 * only the gaze and the resin to face, and could stand petrified hitting the resin back.
	 */
	private void tickRoots(ServerLevel level, Player target) {
		double distance = this.distanceToSqr(target);
		if (distance > ROOT_RANGE * ROOT_RANGE || this.lens.sees(target) && distance <= ElderwoodSlam.REACH * ElderwoodSlam.REACH) {
			return;
		}
		if (--this.rootCooldown <= 0) {
			this.rootCooldown = this.getTier().rootIntervalTicks();
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

	public boolean isDormant() {
		return this.entityData.get(DORMANT);
	}

	// With nobody near for a while it settles and mends, so walking away resets the fight.
	private void tickDormancy() {
		this.idleTicks = this.getTarget() != null ? 0 : this.idleTicks + 1;
		this.entityData.set(DORMANT, this.idleTicks >= DORMANT_AFTER_TICKS);
		if (!this.isDormant() || (this.idleTicks - DORMANT_AFTER_TICKS) % HEAL_INTERVAL_TICKS != 0) {
			return;
		}
		for (ElderwoodHeart heart : ElderwoodHeart.ALL) {
			if (this.getCracks(heart) > 0) {
				this.setCracks(heart, this.getCracks(heart) - 1);
				this.playSound(ModSounds.ELDERWOOD_MEND, 1.5F, 0.6F);
				return;
			}
		}
		// Whole again: whoever comes next starts a new fight.
		this.petrifiedThisFight.clear();
		this.crackedThisFight.clear();
	}

	/** How far it has fallen apart while dying (see {@link ElderwoodDecay#stage}). */
	public int getDecayStage() {
		return ElderwoodDecay.stage(this.deathTime);
	}

	@Override
	public void die(DamageSource source) {
		boolean wasAlive = !this.dead;
		super.die(source);
		if (wasAlive && source.getEntity() instanceof ServerPlayer killer) {
			if (!this.hasPetrified(killer)) {
				ModCriteria.KILLED_ELDERWOOD_UNPETRIFIED.trigger(killer);
			}
			if (this.getTier() == ElderwoodTier.OXIDIZED) {
				// Keep the released trigger working for datapacks that still use it.
				ModCriteria.KILLED_OXIDIZED_ELDERWOOD.trigger(killer);
			} else if (this.getTier() == ElderwoodTier.COPPER) {
				ModCriteria.KILLED_FRESH_ELDERWOOD.trigger(killer);
			}
		}
	}

	@Override
	protected void tickDeath() {
		this.deathTime++;
		if (this.level() instanceof ServerLevel level && !this.isRemoved() && ElderwoodDecay.tick(this, level)) {
			// What it drops is left where the trunk stood, once the last of it is gone.
			super.dropAllDeathLoot(level, this.killedBy != null ? this.killedBy : this.damageSources().generic());
			this.remove(RemovalReason.KILLED);
		}
	}

	// Each tier has a loot table of its own; the entity type's is the plain copper one.
	@Override
	protected void dropFromLootTable(ServerLevel level, DamageSource source, boolean killedByPlayer) {
		this.dropFromLootTable(level, source, killedByPlayer, this.getTier().lootTable());
		// In a group, everyone who cracked a heart takes a lens home; the rest is one set.
		int extraLenses = this.crackedThisFight.size() - 1;
		if (extraLenses > 0) {
			this.spawnAtLocation(level, new ItemStack(ModItems.ELDERWOOD_LENS, extraLenses));
		}
	}

	@Override
	protected void dropAllDeathLoot(ServerLevel level, DamageSource source) {
		this.killedBy = source;
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

	@Override
	public void onSyncedDataUpdated(EntityDataAccessor<?> accessor) {
		super.onSyncedDataUpdated(accessor);
		if (SLAM_PHASE.equals(accessor)) {
			this.slamPhaseAge = 0;
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
	public boolean canAttack(LivingEntity target) {
		return target instanceof Player && super.canAttack(target);
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
	public void setCustomName(Component name) {
		super.setCustomName(name);
		this.bossEvent.setName(this.getDisplayName());
	}

	@Override
	protected SoundEvent getAmbientSound() {
		return ModSounds.ELDERWOOD_AMBIENT;
	}

	@Override
	protected SoundEvent getHurtSound(DamageSource source) {
		return ModSounds.ELDERWOOD_HEART_CRACK;
	}

	@Override
	protected SoundEvent getDeathSound() {
		return ModSounds.ELDERWOOD_DEATH;
	}

	@Override
	public float getVoicePitch() {
		return 0.6F;
	}

	@Override
	protected void addAdditionalSaveData(ValueOutput output) {
		super.addAdditionalSaveData(output);
		output.putInt("copper_stage", this.getCopperStage().ordinal());
		output.putInt("hearts", this.entityData.get(HEART_CRACKS));
		output.putInt("players", this.getPlayers());
		output.store("petrified_this_fight", UUIDUtil.CODEC_SET, this.petrifiedThisFight);
		output.store("cracked_this_fight", UUIDUtil.CODEC_SET, this.crackedThisFight);
	}

	@Override
	protected void readAdditionalSaveData(ValueInput input) {
		super.readAdditionalSaveData(input);
		int stage = input.getIntOr("copper_stage", WeatheringCopper.WeatherState.UNAFFECTED.ordinal());
		this.setCopperStage(COPPER_STAGES[Mth.clamp(stage, 0, COPPER_STAGES.length - 1)]);
		this.entityData.set(PLAYERS, Mth.clamp(input.getIntOr("players", 1), 1, MAX_PLAYERS));
		this.entityData.set(HEART_CRACKS, input.getIntOr("hearts", 0));
		int old = input.getIntOr("heart_cracks", 0);
		for (ElderwoodHeart heart : ElderwoodHeart.ALL) {
			if (old != 0) {
				this.setCracks(heart, heart.cracksInOld(old));
			}
		}
		this.crackedThisFight.clear();
		input.read("cracked_this_fight", UUIDUtil.CODEC_SET).ifPresent(this.crackedThisFight::addAll);
		this.petrifiedThisFight.clear();
		input.read("petrified_this_fight", UUIDUtil.CODEC_SET).ifPresent(this.petrifiedThisFight::addAll);
		this.showCracksOnTheBossBar();
		if (this.hasCustomName()) {
			this.bossEvent.setName(this.getDisplayName());
		}
	}
}
