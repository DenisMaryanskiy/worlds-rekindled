package com.dmaryanskiy.vanillareforged.sentry;

import com.dmaryanskiy.vanillareforged.VanillaReforged;

import net.minecraft.core.registries.Registries;
import net.minecraft.tags.TagKey;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.arrow.AbstractArrow;
import net.minecraft.world.entity.projectile.arrow.ThrownTrident;
import net.minecraft.world.item.Item;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/**
 * The Sentry's weak point: a small box in the chest, hit from the front. Instead of a separate part
 * entity (vanilla only supports those for the Ender Dragon), hits on the Sentry are traced through
 * this box.
 */
public final class SentryHeart {
	public static final TagKey<Item> CRACKING_WEAPONS = TagKey.create(Registries.ITEM, VanillaReforged.id("cracks_sentry_hearts"));

	private static final double HEIGHT = 1.53;
	private static final double FORWARD = 0.125;
	private static final double SIZE = 0.5;

	// Player.attack resets the attack strength before the hit lands, so a full-strength swing is told
	// apart by its damage: below 90% charge it deals at most ~85% of the attack damage attribute.
	private static final double FULL_STRENGTH_DAMAGE = 0.84;

	private SentryHeart() {
	}

	public static AABB box(HeartwoodSentry sentry) {
		Vec3 center = sentry.position().add(0.0, HEIGHT, 0.0).add(forward(sentry).scale(FORWARD));
		return AABB.ofSize(center, SIZE, SIZE, SIZE);
	}

	/**
	 * Whether a hit cracks the heart: a player's full-strength sword swing or charged arrow or trident on
	 * the heart, or any mob's arrow or trident on the heart.
	 */
	public static boolean isCrackedBy(HeartwoodSentry sentry, DamageSource source, float damage) {
		Entity direct = source.getDirectEntity();
		if (direct instanceof Player player && source.getEntity() == player) {
			Vec3 eyes = player.getEyePosition();
			return player.getMainHandItem().is(CRACKING_WEAPONS)
					&& damage >= FULL_STRENGTH_DAMAGE * player.getAttributeValue(Attributes.ATTACK_DAMAGE)
					&& isHit(sentry, eyes, eyes.add(player.getViewVector(1.0F).scale(player.entityInteractionRange())));
		}
		if (direct instanceof AbstractArrow arrow && isCrackingProjectile(arrow, source.getEntity())) {
			// By now the projectile sits where it touched the Sentry's bounding box.
			Vec3 motion = direct.getDeltaMovement();
			return isHit(sentry, direct.position().subtract(motion), direct.position().add(motion));
		}
		return false;
	}

	// Mobs can't charge a shot, so any arrow of theirs counts; a player's must be fully charged.
	private static boolean isCrackingProjectile(AbstractArrow arrow, Entity shooter) {
		if (shooter instanceof Player) {
			return arrow instanceof ThrownTrident || arrow.isCritArrow();
		}
		return shooter instanceof Mob;
	}

	private static boolean isHit(HeartwoodSentry sentry, Vec3 from, Vec3 to) {
		return to.subtract(from).dot(forward(sentry)) < 0.0 && box(sentry).clip(from, to).isPresent();
	}

	private static Vec3 forward(HeartwoodSentry sentry) {
		return Vec3.directionFromRotation(0.0F, sentry.yBodyRot);
	}
}
