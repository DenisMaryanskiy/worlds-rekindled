package com.dmaryanskiy.vanillareforged;

import java.util.Map;
import java.util.Optional;
import java.util.WeakHashMap;

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.core.registries.Registries;
import net.minecraft.tags.TagKey;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.Vec3;

/** Pale Mask ability: the hostile mob in the wearer's crosshair is slowed while the gaze is held. */
public final class WatcherGaze {
	public static final TagKey<EntityType<?>> GAZE_RESISTANT = TagKey.create(Registries.ENTITY_TYPE,
			VanillaReforged.id("gaze_resistant"));
	public static final double RANGE = 16.0;
	public static final int TICKS_PER_DURABILITY = 20;
	public static final double BOSS_DRAG = 0.85;
	private static final int SLOWNESS_TICKS = 10;

	private static final Map<Player, Integer> GAZE_TICKS = new WeakHashMap<>();

	private WatcherGaze() {
	}

	public static void initialize() {
		ServerTickEvents.END_LEVEL_TICK.register(level -> level.players().forEach(WatcherGaze::tick));
	}

	public static void tick(Player player) {
		ItemStack mask = player.getItemBySlot(EquipmentSlot.HEAD);
		Optional<LivingEntity> target = mask.is(ModItems.PALE_MASK) && !player.isSpectator()
				? findTarget(player)
				: Optional.empty();
		if (target.isEmpty()) {
			GAZE_TICKS.remove(player);
			return;
		}

		LivingEntity mob = target.get();
		boolean boss = mob.getType().builtInRegistryHolder().is(GAZE_RESISTANT);
		mob.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, SLOWNESS_TICKS, boss ? 0 : 1), player);
		if (boss) {
			// The Wither ignores all effects and chases with hard-coded acceleration, so bosses also get drag.
			mob.setDeltaMovement(mob.getDeltaMovement().multiply(BOSS_DRAG, 1.0, BOSS_DRAG));
		}

		int ticks = GAZE_TICKS.merge(player, 1, Integer::sum);
		if (ticks >= TICKS_PER_DURABILITY) {
			GAZE_TICKS.put(player, 0);
			mask.hurtAndBreak(1, player, EquipmentSlot.HEAD);
		}
	}

	public static Optional<LivingEntity> findTarget(Player player) {
		Vec3 eye = player.getEyePosition();
		Vec3 end = eye.add(player.getViewVector(1.0F).scale(RANGE));
		Vec3 blockHit = player.level().clip(new ClipContext(eye, end, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, player))
				.getLocation();

		EntityHitResult hit = ProjectileUtil.getEntityHitResult(player, eye, blockHit,
				player.getBoundingBox().expandTowards(blockHit.subtract(eye)).inflate(1.0),
				entity -> !entity.isSpectator() && entity.isPickable(), eye.distanceToSqr(blockHit));
		if (hit == null) {
			return Optional.empty();
		}
		// Ender Dragon parts aren't LivingEntities, so the dragon (immune to effects) never costs durability.
		return hit.getEntity() instanceof LivingEntity living && living instanceof Enemy ? Optional.of(living) : Optional.empty();
	}
}
