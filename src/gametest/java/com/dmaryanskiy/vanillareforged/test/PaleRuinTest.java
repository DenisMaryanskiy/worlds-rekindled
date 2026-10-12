package com.dmaryanskiy.vanillareforged.test;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import com.dmaryanskiy.vanillareforged.ModEntities;
import com.dmaryanskiy.vanillareforged.VanillaReforged;
import com.dmaryanskiy.vanillareforged.sentry.RuinSentry;

import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerChunkCache;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CreakingHeartBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BrushableBlockEntity;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.level.block.entity.DecoratedPotBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.CreakingHeartState;
import net.minecraft.world.level.levelgen.densityfunction.SamplerContext;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.PoolElementStructurePiece;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructurePiece;
import net.minecraft.world.level.levelgen.structure.StructureStart;
import net.minecraft.world.level.levelgen.structure.pools.SinglePoolElement;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;

public class PaleRuinTest {
	private static final int LAYOUTS = 300;
	// Every way four rooms can turn without the line curling back on itself.
	private static final int WINDINGS = 41;
	private static final int ROOMS = 4;

	// Underground the Ruin cuts through water pockets. Vanilla waterlogs what a structure places in
	// water, and a waterlogged lantern then floods the room around it.
	@GameTest
	public void itsLanternsStayDryWhenItIsBuiltIntoWater(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		int lanterns = 0;
		for (BlockPos pos : buildTheHall(level, new ChunkPos(3000, 3000), true)) {
			BlockState state = level.getBlockState(pos);
			if (state.hasProperty(BlockStateProperties.WATERLOGGED)) {
				lanterns += BuiltInRegistries.BLOCK.getKey(state.getBlock()).getPath().endsWith("lantern") ? 1 : 0;
				helper.assertFalse(state.getValue(BlockStateProperties.WATERLOGGED), state.getBlock().getName().getString() + " is waterlogged");
			}
		}
		helper.assertTrue(lanterns > 0, "The hall should have been built");
		helper.succeed();
	}

	@GameTest
	public void theHallsChestPotsAndHeapsAreFilledFromItsLootTables(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		int chests = 0;
		int pots = 0;
		int heaps = 0;
		for (BlockPos pos : buildTheHall(level, new ChunkPos(3100, 3100), false)) {
			BlockEntity blockEntity = level.getBlockEntity(pos);
			if (blockEntity instanceof ChestBlockEntity chest) {
				chests++;
				helper.assertValueEqual(chest.getLootTable().identifier(), VanillaReforged.id("chests/pale_ruin/hall"), "the chest's loot table");
			} else if (blockEntity instanceof DecoratedPotBlockEntity pot) {
				pots++;
				helper.assertValueEqual(pot.getLootTable().identifier(), VanillaReforged.id("pots/pale_ruin"), "a pot's loot table");
			} else if (blockEntity instanceof BrushableBlockEntity heap) {
				heaps++;
				helper.assertValueEqual(heap.saveWithoutMetadata(level.registryAccess()).getStringOr("LootTable", ""),
						"vanillareforged:archaeology/pale_ruin_hall", "a heap's loot table");
			}
		}
		helper.assertValueEqual(chests, 1, "chests in the hall");
		helper.assertValueEqual(pots, 2, "pots in the hall");
		helper.assertValueEqual(heaps, 4, "heaps to brush in the hall");
		helper.succeed();
	}

	/**
	 * Builds a Ruin far from the tests (its hall is bigger than a test's plot), into water if asked,
	 * and returns every position of the hall.
	 */
	private static Iterable<BlockPos> buildTheHall(ServerLevel level, ChunkPos where, boolean intoWater) {
		ServerChunkCache chunks = level.getChunkSource();
		Holder<Structure> ruin = ruin(level);
		StructureStart start = ruin.value().generate(ruin, level.dimension(), level.registryAccess(), chunks.getGenerator(),
				chunks.getGenerator().getBiomeSource(), chunks.randomState().createClimateSampler(SamplerContext.EMPTY_UNCACHED), chunks.randomState(),
				level.getStructureTemplateManager(), 1L, where, 0, level, biome -> true);
		BoundingBox hall = start.getPieces().stream().filter(piece -> template(piece).equals("pale_ruin/hall")).findFirst().orElseThrow()
				.getBoundingBox();
		Iterable<BlockPos> inTheHall = BlockPos.betweenClosed(hall.minX(), hall.minY(), hall.minZ(), hall.maxX(), hall.maxY(), hall.maxZ());
		if (intoWater) {
			for (BlockPos pos : inTheHall) {
				level.setBlock(pos, Blocks.WATER.defaultBlockState(), Block.UPDATE_CLIENTS);
			}
		}
		ChunkPos.rangeClosed(new ChunkPos(hall.minX() >> 4, hall.minZ() >> 4), new ChunkPos(hall.maxX() >> 4, hall.maxZ() >> 4))
				.forEach(chunk -> start.placeInChunk(level, level.structureManager(), chunks.getGenerator(), level.getRandom(),
						new BoundingBox(chunk.getMinBlockX(), level.getMinY(), chunk.getMinBlockZ(), chunk.getMaxBlockX(), level.getMaxY(),
								chunk.getMaxBlockZ()), chunk));
		return inTheHall;
	}

	// Vanilla never ticks an "uprooted" heart, and a structure's heart isn't checked when it's placed:
	// written as uprooted it would stay so for good, and no Creaking would ever walk the Ruin.
	@GameTest
	public void theRoomsHeartsArePlacedInAStateThatTicks(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		// Far from the tests: a room's furnishing is bigger than a test's plot.
		BlockPos first = new BlockPos(3200 * 16, 100, 3200 * 16);
		int hearts = 0;
		List<String> interiors = List.of("guard_tools", "overgrown_heart", "cells_heart");
		for (String interior : interiors) {
			BlockPos origin = first.offset(32 * interiors.indexOf(interior), 0, 0);
			StructureTemplate template = level.getStructureTemplateManager().get(VanillaReforged.id("pale_ruin/interior/" + interior)).orElseThrow();
			// The test world is kept between runs: start from nothing.
			for (BlockPos pos : BlockPos.betweenClosed(origin, origin.offset(template.getSize()))) {
				level.setBlock(pos, Blocks.AIR.defaultBlockState(), Block.UPDATE_CLIENTS);
			}
			template.placeInWorld(level, origin, origin, new StructurePlaceSettings(), level.getRandom(), Block.UPDATE_CLIENTS);
			for (BlockPos pos : BlockPos.betweenClosed(origin, origin.offset(template.getSize()))) {
				BlockState state = level.getBlockState(pos);
				if (state.is(Blocks.CREAKING_HEART)) {
					hearts++;
					helper.assertTrue(state.getValue(BlockStateProperties.CREAKING_HEART_STATE) != CreakingHeartState.UPROOTED,
							"The heart of " + interior + " is uprooted");
					helper.assertTrue(CreakingHeartBlock.hasRequiredLogs(state, level, pos), "The heart of " + interior + " needs a log on each side");
					helper.assertTrue(state.getValue(BlockStateProperties.NATURAL), "The heart of " + interior + " should be natural, so its Creaking gives resin");
				}
			}
		}
		helper.assertValueEqual(hearts, 3, "hearts found");
		helper.succeed();
	}

	// A Sentry is at home in its whole Ruin, however far from its post: it used to let go of a player
	// 16 blocks from where it stood, a few steps into the corridor of its own room.
	@GameTest
	public void aSentryGuardsItsWholeRuinAndNothingBeyond(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		ServerChunkCache chunks = level.getChunkSource();
		Holder<Structure> ruin = ruin(level);
		ChunkPos where = new ChunkPos(3300, 3300);
		StructureStart start = ruin.value().generate(ruin, level.dimension(), level.registryAccess(), chunks.getGenerator(),
				chunks.getGenerator().getBiomeSource(), chunks.randomState().createClimateSampler(SamplerContext.EMPTY_UNCACHED), chunks.randomState(),
				level.getStructureTemplateManager(), 1L, where, 0, level, biome -> true);
		BlockPos post = box(start, "pale_ruin/room_1").getCenter();
		BlockPos inTheHall = box(start, "pale_ruin/hall").getCenter();
		BlockPos atTheEntrance = box(start, "pale_ruin/entrance").getCenter();
		// The Ruin as the world would know it, without building it.
		level.getChunk(where.x(), where.z()).setStartForStructure(ruin.value(), start);
		level.getChunk(post).addReferenceForStructure(ruin.value(), where.pack());

		RuinSentry sentry = ModEntities.RUIN_SENTRY.create(level, EntitySpawnReason.COMMAND);
		sentry.setPost(post);

		helper.assertFalse(post.closerThan(inTheHall, RuinSentry.LEASH_RADIUS), "The hall should be farther than its old leash");
		helper.assertTrue(sentry.guards(inTheHall), "It should guard the hall");
		helper.assertTrue(sentry.notices(post.north(3)), "It should notice a player in its own room");
		helper.assertFalse(sentry.notices(inTheHall), "A player in the hall shouldn't draw it out of its room");
		helper.assertFalse(sentry.notices(box(start, "pale_ruin/room_2").getCenter()), "Nor should a player in the next room");
		helper.assertTrue(sentry.guards(atTheEntrance), "It should guard the entrance");
		helper.assertFalse(sentry.guards(post.above(100)), "It shouldn't guard the sky over its room");
		helper.assertFalse(sentry.guards(inTheHall.east(200)), "It shouldn't guard the rock far from the Ruin");
		helper.succeed();
	}

	private static BoundingBox box(StructureStart start, String template) {
		return start.getPieces().stream().filter(piece -> template(piece).startsWith(template)).findFirst().orElseThrow().getBoundingBox();
	}

	// Lays the Ruin out many times without building it: wherever its rooms turn, none may be squeezed out.
	@GameTest
	public void everyLayoutHasAllItsRoomsAndEndsInTheHall(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		ServerChunkCache chunks = level.getChunkSource();
		Holder<Structure> ruin = ruin(level);

		boolean sawTheLongest = false;
		Set<String> windings = new HashSet<>();
		for (int layout = 0; layout < LAYOUTS; layout++) {
			StructureStart start = ruin.value().generate(ruin, level.dimension(), level.registryAccess(), chunks.getGenerator(),
					chunks.getGenerator().getBiomeSource(), chunks.randomState().createClimateSampler(SamplerContext.EMPTY_UNCACHED), chunks.randomState(),
					level.getStructureTemplateManager(), layout * 7919L, new ChunkPos(layout * 40, 0), 0, level, biome -> true);
			List<String> pieces = start.getPieces().stream().map(PaleRuinTest::template).toList();
			List<StructurePiece> rooms = start.getPieces().stream().filter(piece -> template(piece).startsWith("pale_ruin/room_")).toList();
			List<StructurePiece> interiors = start.getPieces().stream().filter(piece -> template(piece).startsWith("pale_ruin/interior/"))
					.toList();

			List<StructurePiece> chambers = start.getPieces().stream().filter(piece -> template(piece).startsWith("pale_ruin/chamber/"))
					.toList();

			helper.assertValueEqual(pieces.size(), 3 * ROOMS + 3, "pieces in layout " + layout + " " + pieces);
			helper.assertValueEqual(pieces.getFirst(), "pale_ruin/entrance", "first piece of layout " + layout);
			helper.assertTrue(pieces.contains("pale_ruin/descent"), "Layout " + layout + " should have its way down " + pieces);
			helper.assertTrue(pieces.contains("pale_ruin/hall"), "Layout " + layout + " should end in the hall " + pieces);
			helper.assertValueEqual(rooms.size(), ROOMS, "rooms in layout " + layout + " " + pieces);
			for (int room = 1; room <= ROOMS; room++) {
				helper.assertTrue(template(rooms.get(room - 1)).startsWith("pale_ruin/room_" + room + "_"),
						"room " + room + " of layout " + layout + " " + pieces);
			}
			// Every room is furnished, from inside its own walls.
			helper.assertValueEqual(interiors.size(), ROOMS, "furnished rooms in layout " + layout + " " + pieces);
			for (StructurePiece interior : interiors) {
				helper.assertTrue(rooms.stream().anyMatch(room -> contains(room.getBoundingBox(), interior.getBoundingBox())),
						template(interior) + " should lie inside a room in layout " + layout);
			}
			helper.assertValueEqual(interiors.stream().filter(interior -> template(interior).endsWith("_tools")).count(), 1L,
					"rooms with the tool chest in layout " + layout);
			// A side chamber hangs off every room: a door that opened on bare rock would be worse than none.
			helper.assertValueEqual(chambers.size(), ROOMS, "side chambers in layout " + layout + " " + pieces);
			sawTheLongest |= rooms.stream().allMatch(room -> template(room).endsWith("_straight"));
			windings.add(rooms.stream().map(PaleRuinTest::template).collect(Collectors.joining(" ")));
		}
		// A chamber is placed before the rooms two on and must never be in their way, however the line winds.
		helper.assertValueEqual(windings.size(), WINDINGS, "ways the line wound in the layouts tried");
		// With every exit straight the line is at its longest, which is the one that could outgrow jigsaw's reach.
		helper.assertTrue(sawTheLongest, "The layouts tried should include the straight line");
		helper.succeed();
	}

	private static Holder<Structure> ruin(ServerLevel level) {
		return level.registryAccess().lookupOrThrow(Registries.STRUCTURE)
				.getOrThrow(ResourceKey.create(Registries.STRUCTURE, VanillaReforged.id("pale_ruin")));
	}

	private static String template(StructurePiece piece) {
		return ((SinglePoolElement) ((PoolElementStructurePiece) piece).getElement()).getTemplateLocation().getPath();
	}

	private static boolean contains(BoundingBox outer, BoundingBox inner) {
		return outer.minX() <= inner.minX() && outer.minY() <= inner.minY() && outer.minZ() <= inner.minZ()
				&& outer.maxX() >= inner.maxX() && outer.maxY() >= inner.maxY() && outer.maxZ() >= inner.maxZ();
	}
}
