package com.dmaryanskiy.vanillareforged.watcher;

import com.dmaryanskiy.vanillareforged.ModEntities;

import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.hurtingprojectile.Fireball;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

/**
 * A glob of burning resin the Watcher throws at range. It sets whoever it hits on fire but, unlike a
 * blaze's fireball, never blocks, so nothing burns down around the fight. Like a ghast's fireball it
 * can be hit back (the {@code redirectable_projectile} tag); hit back into the Watcher it does no
 * damage, since only hearts take damage, but stuns it.
 */
public class BurningResin extends Fireball {
	public static final float DAMAGE = 5.0F;
	public static final float BURN_SECONDS = 5.0F;
	private static final double ACCELERATION = 0.06;

	public BurningResin(EntityType<? extends BurningResin> type, Level level) {
		super(type, level);
		this.accelerationPower = ACCELERATION;
	}

	public BurningResin(Level level, LivingEntity owner, Vec3 direction) {
		super(ModEntities.BURNING_RESIN, owner, direction, level);
		this.accelerationPower = ACCELERATION;
		this.setItem(new ItemStack(Items.RESIN_CLUMP));
	}

	@Override
	protected void onHitEntity(EntityHitResult hitResult) {
		super.onHitEntity(hitResult);
		if (!(this.level() instanceof ServerLevel level)) {
			return;
		}
		Entity target = hitResult.getEntity();
		Entity owner = this.getOwner();
		if (target instanceof Watcher watcher) {
			if (owner instanceof Player) {
				watcher.stun();
			}
			return;
		}
		int fireTicks = target.getRemainingFireTicks();
		target.igniteForSeconds(BURN_SECONDS);
		DamageSource source = this.damageSources().fireball(this, owner);
		if (!target.hurtServer(level, source, DAMAGE)) {
			target.setRemainingFireTicks(fireTicks);
		}
	}

	@Override
	protected void onHit(HitResult hitResult) {
		super.onHit(hitResult);
		if (!this.level().isClientSide()) {
			this.discard();
		}
	}

	@Override
	protected ParticleOptions getTrailParticle() {
		return ParticleTypes.SMALL_FLAME;
	}
}
