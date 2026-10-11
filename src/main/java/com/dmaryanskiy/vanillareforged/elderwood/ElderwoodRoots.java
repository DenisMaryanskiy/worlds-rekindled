package com.dmaryanskiy.vanillareforged.elderwood;

import org.jspecify.annotations.Nullable;

import com.dmaryanskiy.vanillareforged.ModEntities;
import com.dmaryanskiy.vanillareforged.ModSounds;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityReference;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

/**
 * The Elderwood's roots breaking out of the ground under a player it can't see, like an evoker's
 * fangs: the ground cracks for a moment as a warning, then the roots burst out, hurt whoever still
 * stands there and toss them into the air.
 */
public class ElderwoodRoots extends Entity {
	public static final int WARNING_TICKS = 20;
	public static final int LIFE_TICKS = WARNING_TICKS + 20;
	public static final float DAMAGE = 6.0F;
	private static final float RISE_TICKS = 3.0F;
	private static final float SINK_TICKS = 6.0F;
	// Low enough that landing does not hurt on top of the hit.
	private static final double TOSS = 0.6;

	private @Nullable EntityReference<LivingEntity> owner;

	public ElderwoodRoots(EntityType<? extends ElderwoodRoots> type, Level level) {
		super(type, level);
	}

	public ElderwoodRoots(Level level, LivingEntity owner, double x, double y, double z) {
		this(ModEntities.ELDERWOOD_ROOTS, level);
		this.owner = EntityReference.of(owner);
		this.setPos(x, y, z);
		this.setYRot(this.random.nextFloat() * 360.0F);
	}

	@Override
	protected void defineSynchedData(SynchedEntityData.Builder builder) {
	}

	@Override
	public void tick() {
		super.tick();
		if (this.level() instanceof ServerLevel level) {
			if (this.tickCount == 1) {
				this.playSound(ModSounds.ELDERWOOD_ROOTS_CRACK, 1.5F, 0.6F);
			}
			if (this.tickCount == WARNING_TICKS) {
				this.erupt(level);
			}
			if (this.tickCount >= LIFE_TICKS) {
				this.discard();
			}
		} else if (this.tickCount <= WARNING_TICKS + RISE_TICKS) {
			this.crackTheGround(this.tickCount < WARNING_TICKS ? 2 : 12);
		}
	}

	private void erupt(ServerLevel level) {
		this.playSound(ModSounds.ELDERWOOD_ROOTS_ERUPT, 1.5F, 0.8F);
		this.playSound(ModSounds.ELDERWOOD_SPLINTER, 1.5F, 0.7F);
		LivingEntity owner = EntityReference.getLivingEntity(this.owner, level);
		DamageSource source = owner == null ? this.damageSources().magic() : this.damageSources().indirectMagic(this, owner);
		for (Player player : level.getEntitiesOfClass(Player.class, this.getBoundingBox().inflate(0.2, 0.0, 0.2))) {
			if (player.hurtServer(level, source, DAMAGE)) {
				player.push(0.0, TOSS, 0.0);
			}
		}
	}

	private void crackTheGround(int particles) {
		BlockPos ground = BlockPos.containing(this.getX(), this.getY() - 0.2, this.getZ());
		BlockState state = this.level().getBlockState(ground);
		if (state.isAir()) {
			return;
		}
		for (int i = 0; i < particles; i++) {
			double x = this.getX() + (this.random.nextDouble() - 0.5) * this.getBbWidth();
			double z = this.getZ() + (this.random.nextDouble() - 0.5) * this.getBbWidth();
			this.level().addParticle(new BlockParticleOption(ParticleTypes.BLOCK, state), x, this.getY() + 0.1, z,
					0.0, 0.2 + this.random.nextDouble() * 0.2, 0.0);
		}
	}

	/** How far the roots are out of the ground, from 0 (still below) to 1, for the client. */
	public float getRise(float partialTicks) {
		float age = this.tickCount + partialTicks;
		float up = Mth.clamp((age - WARNING_TICKS) / RISE_TICKS, 0.0F, 1.0F);
		float down = Mth.clamp((LIFE_TICKS - age) / SINK_TICKS, 0.0F, 1.0F);
		return up * down;
	}

	@Override
	public boolean hurtServer(ServerLevel level, DamageSource source, float damage) {
		return false;
	}

	@Override
	protected void readAdditionalSaveData(ValueInput input) {
		this.owner = EntityReference.read(input, "Owner");
	}

	@Override
	protected void addAdditionalSaveData(ValueOutput output) {
		EntityReference.store(this.owner, output, "Owner");
	}
}
