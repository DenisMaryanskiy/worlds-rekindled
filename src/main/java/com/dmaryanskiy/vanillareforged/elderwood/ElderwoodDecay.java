package com.dmaryanskiy.vanillareforged.elderwood;

import com.dmaryanskiy.vanillareforged.ModSounds;

import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Blocks;

/**
 * How a dying Elderwood falls apart. It doesn't tip over like other mobs: it shakes itself apart in
 * three breaks, the crown and branches first, then the upper trunk, and then the stump bursts.
 */
public final class ElderwoodDecay {
	public static final int STAGES = 3;
	public static final int STAGE_TICKS = 25;
	public static final int TICKS = STAGES * STAGE_TICKS;
	// Where the model's layers meet (the client's geometry): the crown sits on the upper trunk, which
	// sits on the stump.
	private static final double CROWN_HEIGHT = 8.4;
	private static final double UPPER_HEIGHT = 3.5;
	private static final BlockParticleOption BARK_DUST = new BlockParticleOption(ParticleTypes.FALLING_DUST,
			Blocks.PALE_OAK_LOG.defaultBlockState());

	private ElderwoodDecay() {
	}

	/** How far it has fallen apart: 0 whole, 1 without its crown and branches, 2 only a stump, 3 gone. */
	public static int stage(int deathTime) {
		return Math.min(deathTime / STAGE_TICKS, STAGES);
	}

	/** One tick of dying; returns whether the last piece has just burst and the tree is gone. */
	static boolean tick(Elderwood tree, ServerLevel level) {
		int stage = stage(tree.deathTime);
		double height = heightLeft(tree, stage);
		level.sendParticles(BARK_DUST, tree.getX(), tree.getY() + height / 2.0, tree.getZ(), 3, 1.5, height / 3.0, 1.5, 0.0);
		if (tree.deathTime % STAGE_TICKS != 0) {
			return false;
		}
		switch (stage) {
			case 1 -> breakOff(tree, level, CROWN_HEIGHT, heightLeft(tree, 0), 3.5, 250);
			case 2 -> breakOff(tree, level, UPPER_HEIGHT, CROWN_HEIGHT, 1.4, 200);
			default -> {
				breakOff(tree, level, 0.0, UPPER_HEIGHT, 1.6, 300);
				level.sendParticles(ParticleTypes.EXPLOSION_EMITTER, tree.getX(), tree.getY() + 1.5, tree.getZ(), 1, 0.0, 0.0, 0.0, 0.0);
				tree.playSound(ModSounds.ELDERWOOD_BURST, 4.0F, 0.7F);
			}
		}
		return stage >= STAGES;
	}

	// Splinters fill the space the broken piece took, with a loud crack.
	private static void breakOff(Elderwood tree, ServerLevel level, double from, double to, double spread, int splinters) {
		level.sendParticles(Elderwood.SPLINTERS, tree.getX(), tree.getY() + (from + to) / 2.0, tree.getZ(), splinters,
				spread, (to - from) / 3.0, spread, 0.15);
		tree.playSound(ModSounds.ELDERWOOD_BREAK_APART, 4.0F, 0.5F);
	}

	private static double heightLeft(Elderwood tree, int stage) {
		return stage <= 0 ? tree.getBbHeight() : stage == 1 ? CROWN_HEIGHT : UPPER_HEIGHT;
	}
}
