package com.dmaryanskiy.vanillareforged.test;

import java.util.List;

import com.dmaryanskiy.vanillareforged.VanillaReforged;

import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerChunkCache;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.levelgen.densityfunction.SamplerContext;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.PoolElementStructurePiece;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructureStart;
import net.minecraft.world.level.levelgen.structure.pools.SinglePoolElement;

public class PaleRuinTest {
	private static final int LAYOUTS = 300;
	private static final int ROOMS = 4;

	// Underground the Ruin cuts through water pockets. Vanilla waterlogs what a structure places in
	// water, and a waterlogged lantern then floods the room around it.
	@GameTest
	public void itsLanternsStayDryWhenItIsBuiltIntoWater(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		ServerChunkCache chunks = level.getChunkSource();
		Holder<Structure> ruin = ruin(level);
		// Far from the tests: the hall is bigger than a test's plot.
		StructureStart start = ruin.value().generate(ruin, level.dimension(), level.registryAccess(), chunks.getGenerator(),
				chunks.getGenerator().getBiomeSource(), chunks.randomState().createClimateSampler(SamplerContext.EMPTY_UNCACHED), chunks.randomState(),
				level.getStructureTemplateManager(), 1L, new ChunkPos(3000, 3000), 0, level, biome -> true);
		BoundingBox hall = start.getPieces().getLast().getBoundingBox();
		Iterable<BlockPos> inTheHall = BlockPos.betweenClosed(hall.minX(), hall.minY(), hall.minZ(), hall.maxX(), hall.maxY(), hall.maxZ());
		for (BlockPos pos : inTheHall) {
			level.setBlock(pos, Blocks.WATER.defaultBlockState(), Block.UPDATE_CLIENTS);
		}

		ChunkPos.rangeClosed(new ChunkPos(hall.minX() >> 4, hall.minZ() >> 4), new ChunkPos(hall.maxX() >> 4, hall.maxZ() >> 4))
				.forEach(chunk -> start.placeInChunk(level, level.structureManager(), chunks.getGenerator(), level.getRandom(),
						new BoundingBox(chunk.getMinBlockX(), level.getMinY(), chunk.getMinBlockZ(), chunk.getMaxBlockX(), level.getMaxY(),
								chunk.getMaxBlockZ()), chunk));

		int lanterns = 0;
		for (BlockPos pos : inTheHall) {
			BlockState state = level.getBlockState(pos);
			if (state.hasProperty(BlockStateProperties.WATERLOGGED)) {
				lanterns += BuiltInRegistries.BLOCK.getKey(state.getBlock()).getPath().endsWith("lantern") ? 1 : 0;
				helper.assertFalse(state.getValue(BlockStateProperties.WATERLOGGED), state.getBlock().getName().getString() + " is waterlogged");
			}
		}
		helper.assertTrue(lanterns > 0, "The hall should have been built");
		helper.succeed();
	}

	// Lays the Ruin out many times without building it: wherever its rooms turn, none may be squeezed out.
	@GameTest
	public void everyLayoutHasAllItsRoomsAndEndsInTheHall(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		ServerChunkCache chunks = level.getChunkSource();
		Holder<Structure> ruin = ruin(level);

		boolean sawTheLongest = false;
		for (int layout = 0; layout < LAYOUTS; layout++) {
			StructureStart start = ruin.value().generate(ruin, level.dimension(), level.registryAccess(), chunks.getGenerator(),
					chunks.getGenerator().getBiomeSource(), chunks.randomState().createClimateSampler(SamplerContext.EMPTY_UNCACHED), chunks.randomState(),
					level.getStructureTemplateManager(), layout * 7919L, new ChunkPos(layout * 40, 0), 0, level, biome -> true);
			List<String> pieces = start.getPieces().stream()
					.map(piece -> ((SinglePoolElement) ((PoolElementStructurePiece) piece).getElement()).getTemplateLocation().getPath())
					.toList();

			helper.assertValueEqual(pieces.size(), ROOMS + 3, "pieces in layout " + layout + " " + pieces);
			helper.assertValueEqual(pieces.getFirst(), "pale_ruin/entrance", "first piece of layout " + layout);
			helper.assertValueEqual(pieces.get(ROOMS + 1), "pale_ruin/descent", "the way down in layout " + layout);
			helper.assertValueEqual(pieces.getLast(), "pale_ruin/hall", "last piece of layout " + layout);
			for (int room = 1; room <= ROOMS; room++) {
				helper.assertTrue(pieces.get(room).startsWith("pale_ruin/room_" + room + "_"), "room " + room + " of layout " + layout + " " + pieces);
			}
			sawTheLongest |= pieces.subList(1, ROOMS + 1).stream().allMatch(room -> room.endsWith("_straight"));
		}
		// With every exit straight the line is at its longest, which is the one that could outgrow jigsaw's reach.
		helper.assertTrue(sawTheLongest, "The layouts tried should include the straight line");
		helper.succeed();
	}

	private static Holder<Structure> ruin(ServerLevel level) {
		return level.registryAccess().lookupOrThrow(Registries.STRUCTURE)
				.getOrThrow(ResourceKey.create(Registries.STRUCTURE, VanillaReforged.id("pale_ruin")));
	}
}
