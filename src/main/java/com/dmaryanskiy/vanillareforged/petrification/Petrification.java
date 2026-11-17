package com.dmaryanskiy.vanillareforged.petrification;

import java.util.Map;
import java.util.WeakHashMap;

import com.mojang.serialization.Codec;

import com.dmaryanskiy.vanillareforged.ModCriteria;
import com.dmaryanskiy.vanillareforged.ModItems;
import com.dmaryanskiy.vanillareforged.VanillaReforged;
import com.dmaryanskiy.vanillareforged.ElderwoodGaze;

import net.fabricmc.fabric.api.attachment.v1.AttachmentRegistry;
import net.fabricmc.fabric.api.attachment.v1.AttachmentSyncPredicate;
import net.fabricmc.fabric.api.attachment.v1.AttachmentType;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;

/**
 * The Elderwood's reversed gaze: a meter, like freezing in powder snow, that fills while a petrifying
 * gaze holds the player and drains once it's broken. As it fills the player slows down and turns
 * brittle; when it's full they're petrified (stuck) for a while, then crack free with the meter
 * empty and a short while in which no gaze takes hold, so one mistake isn't an endless loop.
 * <p>
 * Without the Pale Mask, a raised shield held towards the gaze blocks it too.
 * <p>
 * The meter is one synced number: {@code 0..FULL} fills, and above {@code FULL} the player is petrified
 * with {@code meter - FULL} ticks left.
 */
public final class Petrification {
	public static final int FULL = 100;
	public static final int PETRIFIED_TICKS = 60;
	public static final int DRAIN_PER_TICK = 2;
	public static final float MAX_SLOWDOWN = 0.6F;
	// The player only starts to slow once the meter is this full, so there's time to get out of the gaze.
	public static final float SLOWDOWN_FROM = 0.5F;
	public static final int IMMUNE_TICKS = 60;
	public static final int SHIELD_TICKS_PER_DURABILITY = 20;
	public static final float MAX_EXTRA_DAMAGE = 0.5F;

	public static final AttachmentType<Integer> METER = AttachmentRegistry.create(VanillaReforged.id("petrification"),
			builder -> builder.initializer(() -> 0).persistent(Codec.intRange(0, FULL + PETRIFIED_TICKS))
					.syncWith(ByteBufCodecs.VAR_INT, AttachmentSyncPredicate.targetOnly()));
	private static final AttachmentType<Long> IMMUNE_UNTIL = AttachmentRegistry.create(VanillaReforged.id("petrification_immunity"),
			builder -> builder.persistent(Codec.LONG));

	private static final Identifier MODIFIER_ID = VanillaReforged.id("petrification");
	private static final Map<Player, ExposureState> EXPOSURES = new WeakHashMap<>();

	private static final class ExposureState {
		long lastExposed = -1;
		int strongestGaze;
		int maskTicks;
		long lastShieldBlock = Long.MIN_VALUE;
		int shieldTicks;
	}

	private Petrification() {
	}

	public static void initialize() {
		ServerTickEvents.END_LEVEL_TICK.register(level -> level.players().forEach(Petrification::tick));
		ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> updateModifiers(handler.player));
	}

	public static int getMeter(Player player) {
		return player.getAttachedOrElse(METER, 0);
	}

	/** How far petrification has taken hold, from 0 to 1 (1 while petrified). */
	public static float getProgress(Player player) {
		return Math.min(getMeter(player), FULL) / (float) FULL;
	}

	public static boolean isPetrified(Player player) {
		return getMeter(player) > FULL;
	}

	public static float damageMultiplier(Player player) {
		return 1.0F + MAX_EXTRA_DAMAGE * getProgress(player);
	}

	/** Whether the player has just broken free and no gaze takes hold of them yet. */
	public static boolean isImmune(Player player) {
		return player.getAttachedOrElse(IMMUNE_UNTIL, 0L) > player.level().getGameTime();
	}

	public static boolean blocksGaze(Player player, Vec3 eye) {
		Vec3 toEye = eye.subtract(player.getEyePosition()).multiply(1.0, 0.0, 1.0);
		return player.getItemBlockingWith() != null && !player.getItemBySlot(EquipmentSlot.HEAD).is(ModItems.PALE_MASK)
				&& toEye.dot(player.getViewVector(1.0F)) > 0.0;
	}

	/** End the gaze at the raised shield, instead of drawing it through the player's face. */
	public static Vec3 gazeTarget(Player player, Vec3 eye) {
		return blocksGaze(player, eye) ? shieldPosition(player) : player.getEyePosition();
	}

	private static Vec3 shieldPosition(Player player) {
		return player.getEyePosition().add(player.getViewVector(1.0F).scale(0.6)).add(0.0, -0.2, 0.0);
	}

	/**
	 * Called every tick a petrifying gaze from {@code eye} holds the player. A shield raised towards
	 * it blocks the gaze, which wears the shield down, and the meter drains behind it.
	 */
	public static void expose(Player player, int amount, Vec3 eye) {
		if (blocksGaze(player, eye)) {
			blockWithShield(player);
			return;
		}
		expose(player, amount);
	}

	private static void blockWithShield(Player player) {
		long now = player.level().getGameTime();
		ExposureState state = EXPOSURES.computeIfAbsent(player, ignored -> new ExposureState());
		if (state.lastShieldBlock == now) {
			return;
		}
		if (state.lastShieldBlock < now - 1) {
			player.level().playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.SHIELD_BLOCK.value(),
					player.getSoundSource(), 0.6F, 1.0F);
			if (player instanceof ServerPlayer serverPlayer) {
				ModCriteria.BLOCKED_PETRIFYING_GAZE.trigger(serverPlayer);
			}
		}
		state.lastShieldBlock = now;
		if (now % 4 == 0 && player.level() instanceof ServerLevel level) {
			Vec3 shield = shieldPosition(player);
			level.sendParticles(ParticleTypes.CRIT, shield.x, shield.y, shield.z, 2, 0.1, 0.1, 0.1, 0.0);
		}
		if (++state.shieldTicks >= SHIELD_TICKS_PER_DURABILITY) {
			state.shieldTicks = 0;
			ItemStack shield = player.getItemBlockingWith();
			if (shield != null) {
				shield.hurtAndBreak(1, player, player.getUsedItemHand());
			}
		}
	}

	/**
	 * Called every tick a petrifying gaze holds the player. The Pale Mask's wearer looks through the
	 * same lens, so the gaze doesn't take hold, but blocking it wears the mask down like its own gaze.
	 * <p>
	 * Gazes don't add up: of several that hold a player in one tick, only the strongest counts.
	 */
	public static void expose(Player player, int amount) {
		if (player.isCreative() || player.isSpectator() || isImmune(player)) {
			return;
		}
		long now = player.level().getGameTime();
		ExposureState state = EXPOSURES.computeIfAbsent(player, ignored -> new ExposureState());
		int already = state.lastExposed == now ? state.strongestGaze : 0;
		if (amount <= already) {
			return;
		}
		state.lastExposed = now;
		state.strongestGaze = amount;
		amount -= already;
		ItemStack mask = player.getItemBySlot(EquipmentSlot.HEAD);
		if (mask.is(ModItems.PALE_MASK)) {
			if (already == 0 && ++state.maskTicks >= ElderwoodGaze.TICKS_PER_DURABILITY) {
				state.maskTicks = 0;
				mask.hurtAndBreak(1, player, EquipmentSlot.HEAD);
			}
			return;
		}
		int meter = getMeter(player);
		if (meter <= FULL) {
			setMeter(player, meter + amount >= FULL ? FULL + PETRIFIED_TICKS : meter + amount);
		}
	}

	/** A hard hit breaks the stone shell: the petrification ends at once. */
	public static void shatter(Player player) {
		if (getMeter(player) > 0) {
			breakFree(player);
		}
	}

	private static void breakFree(Player player) {
		boolean wasPetrified = isPetrified(player);
		setMeter(player, 0);
		player.setAttached(IMMUNE_UNTIL, player.level().getGameTime() + IMMUNE_TICKS);
		// The hit that shatters the stone lands first, and may have killed them.
		if (wasPetrified && player.isAlive() && player instanceof ServerPlayer serverPlayer) {
			ModCriteria.SURVIVED_PETRIFICATION.trigger(serverPlayer);
		}
	}

	public static void tick(Player player) {
		if (player.hasAttached(IMMUNE_UNTIL) && !isImmune(player)) {
			player.removeAttached(IMMUNE_UNTIL);
		}
		int meter = getMeter(player);
		ExposureState state = EXPOSURES.get(player);
		if (meter - 1 == FULL) {
			breakFree(player);
		} else if (meter > FULL) {
			setMeter(player, meter - 1);
		} else if (meter > 0 && (state == null || state.lastExposed != player.level().getGameTime())) {
			setMeter(player, Math.max(0, meter - DRAIN_PER_TICK));
		}
	}

	private static void setMeter(Player player, int meter) {
		if (meter == 0) {
			player.removeAttached(METER);
		} else {
			player.setAttached(METER, meter);
		}
		updateModifiers(player);
	}

	private static void updateModifiers(Player player) {
		boolean petrified = isPetrified(player);
		double slowdown = petrified ? 1.0
				: MAX_SLOWDOWN * Math.max(0.0F, getProgress(player) - SLOWDOWN_FROM) / (1.0F - SLOWDOWN_FROM);
		setModifier(player.getAttribute(Attributes.MOVEMENT_SPEED), -slowdown);
		setModifier(player.getAttribute(Attributes.JUMP_STRENGTH), petrified ? -1.0 : 0.0);
	}

	private static void setModifier(AttributeInstance attribute, double amount) {
		if (attribute == null) {
			return;
		}
		AttributeModifier current = attribute.getModifier(MODIFIER_ID);
		if (current != null && current.amount() == amount) {
			return;
		}
		if (current != null) {
			attribute.removeModifier(MODIFIER_ID);
		}
		if (amount != 0.0) {
			attribute.addTransientModifier(new AttributeModifier(MODIFIER_ID, amount, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL));
		}
	}
}
