package com.dmaryanskiy.vanillareforged.sentry;

import java.util.Optional;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LightBlock;
import net.minecraft.world.level.block.state.BlockState;

/**
 * A jack o'lantern head lights up its surroundings. Vanilla has no light from entities, so an
 * invisible light block follows the Sentry's head through the air.
 */
public class SentryLantern {
	public static final int LIGHT_LEVEL = 10;
	private static final BlockState LIGHT = Blocks.LIGHT.defaultBlockState().setValue(LightBlock.LEVEL, LIGHT_LEVEL);

	private Optional<BlockPos> lightPos = Optional.empty();

	public Optional<BlockPos> getLightPos() {
		return this.lightPos;
	}

	/** Restores where the light was when the Sentry was saved, so a reload can clean it up. */
	public void setLightPos(Optional<BlockPos> lightPos) {
		this.lightPos = lightPos;
	}

	public void tick(ServerLevel level, HeartwoodSentry sentry) {
		Optional<BlockPos> wanted = sentry.isJackOLantern() && sentry.isAlive()
				? Optional.of(BlockPos.containing(sentry.getEyePosition()))
				: Optional.empty();
		if (wanted.equals(this.lightPos)) {
			return;
		}
		this.remove(level);
		wanted.filter(pos -> level.getBlockState(pos).isAir()).ifPresent(pos -> {
			level.setBlock(pos, LIGHT, Block.UPDATE_CLIENTS);
			this.lightPos = Optional.of(pos);
		});
	}

	public void remove(ServerLevel level) {
		// Only our own light: the player may have built over it since.
		this.lightPos.filter(pos -> level.getBlockState(pos) == LIGHT)
				.ifPresent(pos -> level.setBlock(pos, Blocks.AIR.defaultBlockState(), Block.UPDATE_CLIENTS));
		this.lightPos = Optional.empty();
	}
}
