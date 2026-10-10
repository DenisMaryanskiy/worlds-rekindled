package com.dmaryanskiy.vanillareforged.heart;

import java.util.Optional;

import com.dmaryanskiy.vanillareforged.VanillaReforged;

import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
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
 * A hit strong enough to crack a heart, as the line it travels along. Hearts are small boxes on a
 * mob's body; instead of separate part entities (vanilla only supports those for the Ender Dragon),
 * hits on the mob are traced through them.
 */
public record HeartHit(Vec3 from, Vec3 to) {
	public static final TagKey<Item> CRACKING_WEAPONS = TagKey.create(Registries.ITEM, VanillaReforged.id("cracks_sentry_hearts"));

	// Player.attack resets the attack strength before the hit lands, so a full-strength swing is told
	// apart by its damage: below 90% charge it deals at most ~85% of the attack damage attribute.
	private static final double FULL_STRENGTH_DAMAGE = 0.84;
	public static final String WEAK_SWING_MESSAGE = "entity.vanillareforged.heart.weak_swing";

	/**
	 * A player's full-strength sword swing, or their charged arrow or thrown trident. Mobs can't charge
	 * a shot, so where mob projectiles count, any arrow or trident of theirs does.
	 */
	public static Optional<HeartHit> of(DamageSource source, float damage, boolean mobProjectilesCount) {
		Entity direct = source.getDirectEntity();
		Entity attacker = source.getEntity();
		if (direct instanceof Player player && attacker == player) {
			return player.getMainHandItem().is(CRACKING_WEAPONS) && isFullStrength(player, damage)
					? Optional.of(swingOf(player))
					: Optional.empty();
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

	/**
	 * A player's swing of a cracking weapon that was too weak to crack anything: the mob can then tell
	 * them so (see {@link #warnWeakSwing}), since otherwise it looks like a miss.
	 */
	public static Optional<HeartHit> ofWeakSwing(DamageSource source, float damage) {
		return source.getDirectEntity() instanceof Player player && source.getEntity() == player
				&& player.getMainHandItem().is(CRACKING_WEAPONS) && !isFullStrength(player, damage)
						? Optional.of(swingOf(player))
						: Optional.empty();
	}

	/** A dull knock and a line above the hotbar for a swing that landed on a heart but too weakly. */
	public static void warnWeakSwing(Entity heartOwner, DamageSource source) {
		heartOwner.playSound(SoundEvents.WOOD_HIT, 1.0F, 0.5F);
		if (source.getEntity() instanceof ServerPlayer player && player.connection != null) {
			player.sendOverlayMessage(Component.translatable(WEAK_SWING_MESSAGE));
		}
	}

	private static boolean isFullStrength(Player player, float damage) {
		return damage >= FULL_STRENGTH_DAMAGE * player.getAttributeValue(Attributes.ATTACK_DAMAGE);
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
