package com.dmaryanskiy.vanillareforged.petrification;

import java.util.Map;
import java.util.WeakHashMap;

import com.dmaryanskiy.vanillareforged.ModItems;
import com.dmaryanskiy.vanillareforged.VanillaReforged;
import com.dmaryanskiy.vanillareforged.ElderwoodGaze;

import net.fabricmc.fabric.api.attachment.v1.AttachmentRegistry;
import net.fabricmc.fabric.api.attachment.v1.AttachmentSyncPredicate;
import net.fabricmc.fabric.api.attachment.v1.AttachmentType;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

/**
 * The Elderwood's reversed gaze: a meter, like freezing in powder snow, that fills while a petrifying
 * gaze holds the player and drains once it's broken. As it fills the player slows down and turns
 * brittle; when it's full they're petrified (stuck) for a while, then crack free at half.
 * <p>
 * The meter is one synced number: {@code 0..FULL} fills, and above {@code FULL} the player is petrified
 * with {@code meter - FULL} ticks left.
 */
public final class Petrification {
	public static final int FULL = 100;
	public static final int PETRIFIED_TICKS = 60;
	public static final int DRAIN_PER_TICK = 2;
	public static final float MAX_SLOWDOWN = 0.6F;
	public static final float MAX_EXTRA_DAMAGE = 0.5F;

	public static final AttachmentType<Integer> METER = AttachmentRegistry.create(VanillaReforged.id("petrification"),
			builder -> builder.initializer(() -> 0).syncWith(ByteBufCodecs.VAR_INT, AttachmentSyncPredicate.targetOnly()));

	private static final Identifier MODIFIER_ID = VanillaReforged.id("petrification");
	private static final Map<Player, Long> LAST_EXPOSED = new WeakHashMap<>();
	private static final Map<Player, Integer> MASK_TICKS = new WeakHashMap<>();

	private Petrification() {
	}

	public static void initialize() {
		ServerTickEvents.END_LEVEL_TICK.register(level -> level.players().forEach(Petrification::tick));
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

	/**
	 * Called every tick a petrifying gaze holds the player. The Pale Mask's wearer looks through the
	 * same lens, so the gaze doesn't take hold, but blocking it wears the mask down like its own gaze.
	 */
	public static void expose(Player player, int amount) {
		if (player.isCreative() || player.isSpectator()) {
			return;
		}
		LAST_EXPOSED.put(player, player.level().getGameTime());
		ItemStack mask = player.getItemBySlot(EquipmentSlot.HEAD);
		if (mask.is(ModItems.PALE_MASK)) {
			if (MASK_TICKS.merge(player, 1, Integer::sum) >= ElderwoodGaze.TICKS_PER_DURABILITY) {
				MASK_TICKS.put(player, 0);
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
			setMeter(player, 0);
		}
	}

	public static void tick(Player player) {
		int meter = getMeter(player);
		if (meter > FULL) {
			setMeter(player, meter - 1 == FULL ? FULL / 2 : meter - 1);
		} else if (meter > 0 && LAST_EXPOSED.getOrDefault(player, -1L) != player.level().getGameTime()) {
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
		double slowdown = petrified ? 1.0 : MAX_SLOWDOWN * getProgress(player);
		setModifier(player.getAttribute(Attributes.MOVEMENT_SPEED), -slowdown);
		setModifier(player.getAttribute(Attributes.JUMP_STRENGTH), petrified ? -1.0 : 0.0);
	}

	private static void setModifier(AttributeInstance attribute, double amount) {
		if (attribute == null) {
			return;
		}
		attribute.removeModifier(MODIFIER_ID);
		if (amount != 0.0) {
			attribute.addTransientModifier(new AttributeModifier(MODIFIER_ID, amount, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL));
		}
	}
}
