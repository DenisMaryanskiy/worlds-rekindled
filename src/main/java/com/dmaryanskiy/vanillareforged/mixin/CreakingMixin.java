package com.dmaryanskiy.vanillareforged.mixin;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

import com.dmaryanskiy.vanillareforged.PaleMaskStealth;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.monster.creaking.Creaking;
import net.minecraft.world.level.Level;

@Mixin(Creaking.class)
public abstract class CreakingMixin extends Monster {
	// Not saved: a reloaded Creaking forgets who attacked it.
	@Unique
	private final Set<UUID> vanillareforged$revealedTo = new HashSet<>();

	protected CreakingMixin(EntityType<? extends Monster> type, Level level) {
		super(type, level);
	}

	// Vanilla routes both targeting and the "frozen while watched" check through canAttack.
	@Override
	public boolean canAttack(LivingEntity target) {
		if (!super.canAttack(target)) {
			return false;
		}
		if (!PaleMaskStealth.isHiddenFrom(target, vanillareforged$revealedTo)) {
			return true;
		}
		PaleMaskStealth.onIgnored(this, target);
		return false;
	}

	@Inject(method = "hurtServer", at = @At("HEAD"))
	private void vanillareforged$revealAttacker(ServerLevel level, DamageSource source, float damage,
			CallbackInfoReturnable<Boolean> cir) {
		PaleMaskStealth.onCreakingHurt(source, vanillareforged$revealedTo);
	}
}
