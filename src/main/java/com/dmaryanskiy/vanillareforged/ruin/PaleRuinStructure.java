package com.dmaryanskiy.vanillareforged.ruin;

import java.util.List;
import java.util.Optional;
import java.util.function.IntBinaryOperator;

import com.dmaryanskiy.vanillareforged.VanillaReforged;
import com.mojang.datafixers.util.Either;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.minecraft.resources.Identifier;
import net.minecraft.util.ExtraCodecs;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.PoolElementStructurePiece;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructurePiece;
import net.minecraft.world.level.levelgen.structure.StructureType;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePiecesBuilder;
import net.minecraft.world.level.levelgen.structure.pools.SinglePoolElement;
import net.minecraft.world.level.levelgen.structure.structures.JigsawStructure;

/**
 * The Pale Ruin: a jigsaw structure that only takes a site where it stays underground.
 * <p>
 * Jigsaw sets the whole Ruin's height by the ground at its entrance and never looks at the ground
 * over its rooms, which lie up to a hundred blocks away at a fixed depth below that entrance. Around
 * a Pale Garden the ground commonly falls further than that depth, and the rooms came out of the
 * hillside, roofs and all.
 * <p>
 * So the Ruin comes at several depths, each a jigsaw structure of its own with a longer stairway
 * down, from the shallowest. A site gets the first one that has ground over every piece but its
 * entrance; a site where not even the deepest does is left alone.
 */
public class PaleRuinStructure extends Structure {
	public static final MapCodec<PaleRuinStructure> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
			ExtraCodecs.nonEmptyList(Codec.list(JigsawStructure.CODEC.codec())).fieldOf("depths").forGetter(PaleRuinStructure::depths))
			.apply(instance, PaleRuinStructure::new));
	// How much ground has to lie over a piece's roof, at its corners and its middle.
	public static final int COVER = 3;
	private static final Identifier HALL = VanillaReforged.id("pale_ruin/hall");

	private final List<JigsawStructure> depths;

	public PaleRuinStructure(List<JigsawStructure> depths) {
		super(new StructureSettings(depths.getFirst().biomes(), depths.getFirst().spawnOverrides(), depths.getFirst().step(),
				depths.getFirst().terrainAdaptation()));
		this.depths = depths;
	}

	/** The Ruin at each depth it comes in, from the shallowest. */
	public List<JigsawStructure> depths() {
		return this.depths;
	}

	@Override
	protected Optional<GenerationStub> findGenerationPoint(GenerationContext context) {
		// The sea floor, not the surface: water over a room is no roof.
		IntBinaryOperator ground = (x, z) -> context.chunkGenerator().getFirstOccupiedHeight(x, z, Heightmap.Types.OCEAN_FLOOR_WG,
				context.heightAccessor(), context.randomState());
		for (JigsawStructure ruin : this.depths) {
			// This asks for the biome before anything is laid out, which is the costly part. A site
			// in the wrong biome is wrong at every depth.
			Optional<GenerationStub> site = ruin.findValidGenerationPoint(context);
			if (site.isEmpty()) {
				return Optional.empty();
			}
			StructurePiecesBuilder pieces = site.get().getPiecesBuilder();
			List<StructurePiece> laidOut = pieces.build().pieces();
			if (endsInTheHall(laidOut) && staysBuried(underground(laidOut), ground)) {
				return Optional.of(new GenerationStub(site.get().position(), Either.right(pieces)));
			}
		}
		return Optional.empty();
	}

	/**
	 * Whether the Ruin came out whole. Jigsaw leaves out a piece that doesn't fit, with all that
	 * follows it: near the bottom of the world a deep Ruin would end at its last room.
	 */
	public static boolean endsInTheHall(List<StructurePiece> pieces) {
		return pieces.stream().anyMatch(piece -> piece instanceof PoolElementStructurePiece placed
				&& placed.getElement() instanceof SinglePoolElement element && element.getTemplateLocation().equals(HALL));
	}

	/** Every piece but the first, which is the entrance and stands in the open. */
	public static List<BoundingBox> underground(List<StructurePiece> pieces) {
		return pieces.stream().skip(1).map(StructurePiece::getBoundingBox).toList();
	}

	/** Whether the ground, by its height at each (x, z), lies over every one of the pieces. */
	public static boolean staysBuried(List<BoundingBox> pieces, IntBinaryOperator ground) {
		for (BoundingBox piece : pieces) {
			int needed = piece.maxY() + COVER;
			int midX = (piece.minX() + piece.maxX()) / 2;
			int midZ = (piece.minZ() + piece.maxZ()) / 2;
			if (ground.applyAsInt(piece.minX(), piece.minZ()) < needed || ground.applyAsInt(piece.minX(), piece.maxZ()) < needed
					|| ground.applyAsInt(piece.maxX(), piece.minZ()) < needed || ground.applyAsInt(piece.maxX(), piece.maxZ()) < needed
					|| ground.applyAsInt(midX, midZ) < needed) {
				return false;
			}
		}
		return true;
	}

	@Override
	public StructureType<?> type() {
		return ModStructureTypes.PALE_RUIN;
	}
}
