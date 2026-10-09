package com.dmaryanskiy.vanillareforged.watcher;


import com.dmaryanskiy.vanillareforged.ModEntities;
import com.dmaryanskiy.vanillareforged.VanillaReforged;

import net.minecraft.advancements.triggers.CriteriaTriggers;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.item.HoneycombItem;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LevelEvent;
import net.minecraft.world.level.block.WeatheringCopper;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.pattern.BlockInWorld;
import net.minecraft.world.level.block.state.pattern.BlockPattern;
import net.minecraft.world.level.block.state.pattern.BlockPatternBuilder;
import net.minecraft.world.level.block.state.predicate.BlockStatePredicate;

/**
 * The Watcher's dormant body, the "wide tree", woken by placing its Creaking Heart last, at night or
 * in darkness:
 *
 * <pre>
 * .  .  W  .  .     W, L  any pale oak log or wood (head, branches)
 * L  L  C  L  L     C     any copper block: its oxidation stage carries over to the Watcher
 * R  .  H  .  R     R     resin block, H  Creaking Heart
 * .  L  L  L  .     roots
 * </pre>
 */
public final class WatcherSummon {
	public static final TagKey<Block> COPPER = TagKey.create(Registries.BLOCK, VanillaReforged.id("watcher_copper"));

	private static final BlockPattern PATTERN = BlockPatternBuilder.start()
			.aisle("~~W~~", "LLCLL", "R~H~R", "~LLL~")
			.where('~', BlockInWorld.hasState(BlockBehaviour.BlockStateBase::isAir))
			.where('W', BlockInWorld.hasState(state -> state.is(BlockTags.PALE_OAK_LOGS)))
			.where('L', BlockInWorld.hasState(state -> state.is(BlockTags.PALE_OAK_LOGS)))
			.where('C', BlockInWorld.hasState(state -> state.is(COPPER)))
			.where('R', BlockInWorld.hasState(BlockStatePredicate.forBlock(Blocks.RESIN_BLOCK)))
			.where('H', BlockInWorld.hasState(BlockStatePredicate.forBlock(Blocks.CREAKING_HEART)))
			.build();

	private WatcherSummon() {
	}

	/** Wakes the Watcher if the heart just placed at {@code heartPos} completes its body. */
	public static boolean trySummon(ServerLevel level, BlockPos heartPos) {
		BlockPattern.BlockPatternMatch match = PATTERN.find(level, heartPos);
		if (match == null || isDaylight(level, match.getBlock(2, 0, 0).getPos().above())) {
			return false;
		}
		Watcher watcher = ModEntities.WATCHER.create(level, EntitySpawnReason.TRIGGERED);
		if (watcher == null) {
			return false;
		}
		watcher.setCopperStage(stageOf(match.getBlock(2, 1, 0).getState()));
		watcher.setPersistenceRequired();

		BlockPos feet = match.getBlock(2, 3, 0).getPos();
		for (int x = 0; x < match.getWidth(); x++) {
			for (int y = 0; y < match.getHeight(); y++) {
				BlockInWorld block = match.getBlock(x, y, 0);
				level.setBlock(block.getPos(), Blocks.AIR.defaultBlockState(), Block.UPDATE_CLIENTS);
				level.levelEvent(LevelEvent.PARTICLES_AND_SOUND_DESTROY_BLOCK, block.getPos(), Block.getId(block.getState()));
			}
		}
		watcher.snapTo(feet.getX() + 0.5, feet.getY() + 0.05, feet.getZ() + 0.5, 0.0F, 0.0F);
		level.addFreshEntity(watcher);
		for (ServerPlayer player : level.getEntitiesOfClass(ServerPlayer.class, watcher.getBoundingBox().inflate(8.0))) {
			CriteriaTriggers.SUMMONED_ENTITY.trigger(player, watcher);
		}
		for (int x = 0; x < match.getWidth(); x++) {
			for (int y = 0; y < match.getHeight(); y++) {
				level.updateNeighborsAt(match.getBlock(x, y, 0).getPos(), Blocks.AIR);
			}
		}
		return true;
	}

	private static boolean isDaylight(ServerLevel level, BlockPos above) {
		return level.isBrightOutside() && level.canSeeSky(above);
	}

	// Waxed copper keeps its stage: unwax it first, like vanilla does for the copper golem.
	private static WeatheringCopper.WeatherState stageOf(BlockState state) {
		Block unwaxed = HoneycombItem.WAX_OFF_BY_BLOCK.get().getOrDefault(state.getBlock(), state.getBlock());
		return unwaxed instanceof WeatheringCopper copper ? copper.getAge() : WeatheringCopper.WeatherState.UNAFFECTED;
	}
}
