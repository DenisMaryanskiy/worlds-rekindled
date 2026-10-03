package com.dmaryanskiy.vanillareforged.sentry;

import com.dmaryanskiy.vanillareforged.ModEntities;

import net.minecraft.advancements.triggers.CriteriaTriggers;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

/** Pumpkin on top of a Creaking Heart on top of a pale oak log: the log stays as the pedestal. */
public final class SentryBuilder {
	private SentryBuilder() {
	}

	/** Called when a carved pumpkin or jack o'lantern is placed at {@code headPos}. */
	public static boolean trySpawn(Level level, BlockPos headPos) {
		BlockPos heartPos = headPos.below();
		BlockPos pedestal = heartPos.below();
		BlockState head = level.getBlockState(headPos);
		if (!level.getBlockState(heartPos).is(Blocks.CREAKING_HEART) || !level.getBlockState(pedestal).is(BlockTags.PALE_OAK_LOGS)) {
			return false;
		}

		HeartwoodSentry sentry = ModEntities.HEARTWOOD_SENTRY.create(level, EntitySpawnReason.TRIGGERED);
		if (sentry == null) {
			return false;
		}
		sentry.setPedestal(pedestal);
		sentry.setJackOLantern(head.is(Blocks.JACK_O_LANTERN));
		clear(level, headPos);
		clear(level, heartPos);
		sentry.snapTo(Vec3.atBottomCenterOf(heartPos), 0.0F, 0.0F);
		level.addFreshEntity(sentry);
		for (ServerPlayer player : level.getEntitiesOfClass(ServerPlayer.class, sentry.getBoundingBox().inflate(5.0))) {
			CriteriaTriggers.SUMMONED_ENTITY.trigger(player, sentry);
		}
		level.updateNeighborsAt(headPos, Blocks.AIR);
		level.updateNeighborsAt(heartPos, Blocks.AIR);
		return true;
	}

	private static void clear(Level level, BlockPos pos) {
		BlockState state = level.getBlockState(pos);
		level.setBlock(pos, Blocks.AIR.defaultBlockState(), Block.UPDATE_CLIENTS);
		level.levelEvent(2001, pos, Block.getId(state));
	}
}
