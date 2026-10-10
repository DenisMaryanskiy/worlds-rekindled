package com.dmaryanskiy.vanillareforged.heart;

import java.util.Optional;

import com.dmaryanskiy.vanillareforged.VanillaReforged;

import net.minecraft.core.registries.Registries;
import net.minecraft.tags.TagKey;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.arrow.AbstractArrow;
import net.minecraft.world.entity.projectile.arrow.ThrownTrident;
import net.minecraft.world.item.Item;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/**
 * A hit that can crack a heart, as the line it travels along. Hearts are small boxes on a
 * mob's body; instead of separate part entities (vanilla only supports those for the Ender Dragon),
 * hits on the mob are traced through them.
 */
public record HeartHit(Vec3 from, Vec3 to) {
	public static final TagKey<Item> CRACKING_WEAPONS = TagKey.create(Registries.ITEM, VanillaReforged.id("cracks_sentry_hearts"));

	/**
	 * A player's sword swing, however charged (how often a heart can crack is up to its mob), or their
	 * charged arrow or thrown trident. Mobs can't charge a shot, so where mob projectiles count, any
	 * arrow or trident of theirs does.
	 */
	public static Optional<HeartHit> of(DamageSource source, boolean mobProjectilesCount) {
		Entity direct = source.getDirectEntity();
		Entity attacker = source.getEntity();
		if (direct instanceof Player player && attacker == player) {
			return player.getMainHandItem().is(CRACKING_WEAPONS) ? Optional.of(swingOf(player)) : Optional.empty();
		}
		if (direct instanceof AbstractArrow arrow) {
			boolean cracks = attacker instanceof Player
					? arrow instanceof ThrownTrident || arrow.isCritArrow()
					: mobProjectilesCount && attacker instanceof Mob;
			if (cracks) {
				// By now the projectile sits where it touched the mob's bounding box.
				Vec3 motion = arrow.getDeltaMovement();
				return Optional.of(new HeartHit(arrow.position().subtract(motion), arrow.position().add(motion)));
			}
		}
		return Optional.empty();
	}

	private static HeartHit swingOf(Player player) {
		Vec3 eyes = player.getEyePosition();
		return new HeartHit(eyes, eyes.add(player.getViewVector(1.0F).scale(player.entityInteractionRange())));
	}

	/** The same hit carried on for {@code blocks} more, for a heart that sits inside its mob's box. */
	public HeartHit carriedOn(double blocks) {
		return new HeartHit(this.from, this.to.add(this.to.subtract(this.from).normalize().scale(blocks)));
	}

	public boolean passesThrough(AABB box) {
		return box.clip(this.from, this.to).isPresent();
	}

	/** Whether the hit travels against {@code facing}, i.e. lands on the side that faces that way. */
	public boolean comesFromTheFrontOf(Vec3 facing) {
		return this.to.subtract(this.from).dot(facing) < 0.0;
	}
}
