package com.dmaryanskiy.vanillareforged.mixin;

import com.dmaryanskiy.vanillareforged.sentry.SentryBuilder;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.CarvedPumpkinBlock;

@Mixin(CarvedPumpkinBlock.class)
public abstract class CarvedPumpkinBlockMixin {
	// Vanilla checks its golem patterns here when a carved pumpkin or jack o'lantern is placed.
	@Inject(method = "trySpawnGolem", at = @At("HEAD"), cancellable = true)
	private void vanillareforged$trySpawnSentry(Level level, BlockPos topPos, CallbackInfo ci) {
		if (SentryBuilder.trySpawn(level, topPos)) {
			ci.cancel();
		}
	}
}
