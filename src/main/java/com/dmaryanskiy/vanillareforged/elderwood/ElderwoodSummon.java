package com.dmaryanskiy.vanillareforged.elderwood;

import java.util.ArrayList;
import java.util.List;

import com.dmaryanskiy.vanillareforged.ModEntities;
import com.dmaryanskiy.vanillareforged.VanillaReforged;

import net.minecraft.advancements.triggers.CriteriaTriggers;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.TagKey;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.Difficulty;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.HoneycombItem;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LevelEvent;
import net.minecraft.world.level.block.WeatheringCopper;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/**
 * The Elderwood's stump, which it grows out of when its Creaking Heart is placed last, on top, at
 * night or in darkness:
 *
 * <pre>
 * top view, ground level     side view
 * .  R  .                    .  H  .     H  Creaking Heart
 * R  L  R                    .  C  .     C  any copper block: its oxidation stage carries over
 * .  R  .                    R  L  R     L  any pale oak log or wood, R  resin blocks (the roots)
 * </pre>
 *
 * It needs room to grow: no full blocks in the {@value #ROOM_WIDTH} x {@value #ROOM_WIDTH} column
 * around the stump, {@value #ROOM_HEIGHT} blocks up from the ground.
 */
public final class ElderwoodSummon {
	public static final TagKey<Block> COPPER = TagKey.create(Registries.BLOCK, VanillaReforged.id("elderwood_copper"));
	public static final String ASLEEP_MESSAGE = "block.vanillareforged.creaking_heart.elderwood_asleep";
	public static final String NO_ROOM_MESSAGE = "block.vanillareforged.creaking_heart.elderwood_no_room";
	public static final String PEACEFUL_MESSAGE = "block.vanillareforged.creaking_heart.elderwood_peaceful";
	public static final int ROOM_WIDTH = 5;
	public static final int ROOM_HEIGHT = 10;
	private static final double MESSAGE_RANGE = 8.0;

	private ElderwoodSummon() {
	}

	/** Grows the Elderwood if the heart just placed at {@code heartPos} completes its stump. */
	public static boolean trySummon(ServerLevel level, BlockPos heartPos) {
		BlockPos copperPos = heartPos.below();
		BlockPos logPos = copperPos.below();
		BlockState copper = level.getBlockState(copperPos);
		if (!copper.is(COPPER) || !level.getBlockState(logPos).is(BlockTags.PALE_OAK_LOGS)) {
			return false;
		}
		List<BlockPos> stump = new ArrayList<>(List.of(heartPos, copperPos, logPos));
		for (Direction side : Direction.Plane.HORIZONTAL) {
			BlockPos root = logPos.relative(side);
			if (!level.getBlockState(root).is(Blocks.RESIN_BLOCK)) {
				return false;
			}
			stump.add(root);
		}
		// Like a bed in daytime: tell the builder why nothing happened.
		if (level.getDifficulty() == Difficulty.PEACEFUL) {
			tell(level, heartPos, PEACEFUL_MESSAGE);
			return false;
		}
		if (level.isBrightOutside() && level.canSeeSky(heartPos.above())) {
			tell(level, heartPos, ASLEEP_MESSAGE);
			return false;
		}
		if (!hasRoom(level, logPos, stump)) {
			tell(level, heartPos, NO_ROOM_MESSAGE);
			return false;
		}
		Elderwood elderwood = ModEntities.ELDERWOOD.create(level, EntitySpawnReason.TRIGGERED);
		if (elderwood == null) {
			return false;
		}
		elderwood.setCopperStage(stageOf(copper));
		Vec3 stumpCenter = Vec3.atCenterOf(logPos);
		elderwood.setPlayers(level.getEntitiesOfClass(Player.class, new AABB(logPos).inflate(Elderwood.PLAYER_RANGE),
				player -> !player.isSpectator() && !player.isCreative()
						&& player.distanceToSqr(stumpCenter) <= Elderwood.PLAYER_RANGE * Elderwood.PLAYER_RANGE).size());
		elderwood.setPersistenceRequired();
		for (BlockPos pos : stump) {
			BlockState state = level.getBlockState(pos);
			level.setBlock(pos, Blocks.AIR.defaultBlockState(), Block.UPDATE_CLIENTS);
			level.levelEvent(LevelEvent.PARTICLES_AND_SOUND_DESTROY_BLOCK, pos, Block.getId(state));
		}
		double x = logPos.getX() + 0.5;
		double z = logPos.getZ() + 0.5;
		// It grows facing whoever woke it.
		Player waker = level.getNearestPlayer(x, logPos.getY(), z, MESSAGE_RANGE, false);
		float yaw = waker == null ? 0.0F : (float) (Mth.atan2(waker.getZ() - z, waker.getX() - x) * Mth.RAD_TO_DEG) - 90.0F;
		elderwood.snapTo(x, logPos.getY(), z, yaw, 0.0F);
		elderwood.yBodyRot = yaw;
		elderwood.yHeadRot = yaw;
		elderwood.startGrowing();
		level.addFreshEntity(elderwood);
		for (ServerPlayer player : level.getEntitiesOfClass(ServerPlayer.class, elderwood.getBoundingBox().inflate(MESSAGE_RANGE))) {
			CriteriaTriggers.SUMMONED_ENTITY.trigger(player, elderwood);
		}
		for (BlockPos pos : stump) {
			level.updateNeighborsAt(pos, Blocks.AIR);
		}
		return true;
	}

	// In a Pale Garden moss carpets, eyeblossoms and grass are everywhere, so anything that isn't a
	// full block may stay, and is left alone.
	private static boolean hasRoom(ServerLevel level, BlockPos logPos, List<BlockPos> stump) {
		int reach = ROOM_WIDTH / 2;
		for (BlockPos pos : BlockPos.betweenClosed(logPos.offset(-reach, 0, -reach), logPos.offset(reach, ROOM_HEIGHT - 1, reach))) {
			if (!stump.contains(pos) && level.getBlockState(pos).isCollisionShapeFullBlock(level, pos)) {
				return false;
			}
		}
		return true;
	}

	private static void tell(ServerLevel level, BlockPos heartPos, String message) {
		for (ServerPlayer player : level.getEntitiesOfClass(ServerPlayer.class, new AABB(heartPos).inflate(MESSAGE_RANGE))) {
			player.sendOverlayMessage(Component.translatable(message));
		}
	}

	// Waxed copper keeps its stage: unwax it first, like vanilla does for the copper golem.
	private static WeatheringCopper.WeatherState stageOf(BlockState state) {
		Block unwaxed = HoneycombItem.WAX_OFF_BY_BLOCK.get().getOrDefault(state.getBlock(), state.getBlock());
		return unwaxed instanceof WeatheringCopper copper ? copper.getAge() : WeatheringCopper.WeatherState.UNAFFECTED;
	}
}
