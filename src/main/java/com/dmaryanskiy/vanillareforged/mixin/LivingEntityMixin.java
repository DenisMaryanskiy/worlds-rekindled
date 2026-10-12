package com.dmaryanskiy.vanillareforged.mixin;

import com.dmaryanskiy.vanillareforged.petrification.Petrification;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;

/** Petrified players are brittle. Fabric has no event that changes damage, hence the Mixin. */
@Mixin(LivingEntity.class)
public abstract class LivingEntityMixin {
	// Player.hurtServer scales by difficulty first and then calls this, so the bonus applies on top.
	@ModifyVariable(method = "hurtServer", at = @At("HEAD"), argsOnly = true)
	private float vanillareforged$brittleWhenPetrified(float damage, ServerLevel level, DamageSource source) {
		// Stone doesn't burn any better: fire already goes through armor, and a petrified player can't put it out.
		return (Object) this instanceof Player player && !source.is(DamageTypeTags.IS_FIRE)
				? damage * Petrification.damageMultiplier(player)
				: damage;
	}
}
