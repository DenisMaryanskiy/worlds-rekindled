package com.dmaryanskiy.vanillareforged.mixin;

import com.dmaryanskiy.vanillareforged.watcher.WatcherSummon;

import org.spongepowered.asm.mixin.Mixin;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.CreakingHeartBlock;
import net.minecraft.world.level.block.state.BlockState;

/** Placing a Creaking Heart last completes the Watcher's body. The heart block has no placement hook of its own. */
@Mixin(CreakingHeartBlock.class)
public abstract class CreakingHeartBlockMixin extends BaseEntityBlock {
	protected CreakingHeartBlockMixin(Properties properties) {
		super(properties);
	}

	@Override
	protected void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean movedByPiston) {
		super.onPlace(state, level, pos, oldState, movedByPiston);
		if (!oldState.is(state.getBlock()) && level instanceof ServerLevel serverLevel) {
			WatcherSummon.trySummon(serverLevel, pos);
		}
	}
}
