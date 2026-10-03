package com.dmaryanskiy.vanillareforged;

import java.util.Set;
import java.util.UUID;

import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;

/**
 * Pale Mask passive: Creakings treat the wearer as one of them and ignore them, until the wearer
 * attacks that Creaking (like piglins and gold armor). Each Creaking keeps its own revealed set.
 */
public final class PaleMaskStealth {
	private PaleMaskStealth() {
	}

	public static boolean isHiddenFrom(LivingEntity target, Set<UUID> revealedTo) {
		return wearsMask(target) && !revealedTo.contains(target.getUUID());
	}

	public static void onCreakingHurt(DamageSource source, Set<UUID> revealedTo) {
		if (source.getEntity() instanceof LivingEntity attacker && wearsMask(attacker)) {
			revealedTo.add(attacker.getUUID());
		}
	}

	private static boolean wearsMask(LivingEntity entity) {
		return entity.getItemBySlot(EquipmentSlot.HEAD).is(ModItems.PALE_MASK);
	}
}
