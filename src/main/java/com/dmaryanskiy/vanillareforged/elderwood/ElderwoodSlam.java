package com.dmaryanskiy.vanillareforged.elderwood;

import com.dmaryanskiy.vanillareforged.ModSounds;
import com.dmaryanskiy.vanillareforged.VanillaReforged;
import com.dmaryanskiy.vanillareforged.petrification.Petrification;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.TagKey;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.LevelEvent;
import net.minecraft.world.level.gamerules.GameRules;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/**
 * The Elderwood's branch slam. It raises a main branch, and after the wind-up brings it down where
 * its target stood when it started, so stepping aside dodges it. The hit shatters a petrified
 * player's stone shell and smashes soft cover; the branch then stays planted for a moment, which
 * brings its heart within reach. The slam's phase and landing spot are the tree's synced state.
 */
public final class ElderwoodSlam {
	public static final double REACH = 6.0;
	// Closer than this the branch would have to come down through its own trunk.
	public static final double MIN_DISTANCE = 2.5;
	/** How far ahead a slam with no target lands. */
	public static final double DISTANCE = 3.5;
	// Its main branches only swing forwards, so it can't slam at a player beside or behind it.
	public static final float ARC_DEGREES = 75.0F;
	public static final float DAMAGE = 10.0F;
	public static final int WINDUP_TICKS = 15;
	public static final int STUCK_TICKS = 30;
	public static final TagKey<Block> BREAKABLE = TagKey.create(Registries.BLOCK, VanillaReforged.id("elderwood_breakable"));
	public static final int COOLDOWN_TICKS = 40;
	private static final double HIT_RADIUS = 2.2;
	private static final double SMASH_RADIUS = 1.5;
	private static final int SMASH_HEIGHT = 4;

	private final Elderwood tree;
	private int ticks;
	private int cooldown;
	private double height;

	ElderwoodSlam(Elderwood tree) {
		this.tree = tree;
	}

	boolean canStart() {
		return !this.tree.isStunned() && !this.tree.isGrowing() && this.tree.getSlamPhase() == Elderwood.SlamPhase.NONE
				&& this.cooldown == 0;
	}

	boolean reaches(Entity target) {
		return this.tree.distanceToSqr(target) <= REACH * REACH
				&& Math.abs(Mth.degreesDifference(this.tree.yBodyRot, this.tree.yawTo(target))) <= ARC_DEGREES;
	}

	void start(Vec3 point) {
		if (!this.canStart()) {
			return;
		}
		Vec3 facing = this.tree.facing();
		Vec3 offset = point.subtract(this.tree.position());
		Vec3 flat = new Vec3(offset.x, 0.0, offset.z);
		flat = flat.normalize().scale(Mth.clamp(flat.length(), MIN_DISTANCE, REACH));
		this.tree.aimSlam((float) (-facing.z * flat.x + facing.x * flat.z), (float) flat.dot(facing));
		this.height = Mth.clamp(offset.y, -1.0, 3.0);
		this.ticks = 0;
		this.tree.setSlamPhase(Elderwood.SlamPhase.WINDUP);
		this.tree.playSound(ModSounds.ELDERWOOD_RAISE_BRANCH, 1.5F, 0.6F);
	}

	void tick(ServerLevel level) {
		if (this.cooldown > 0) {
			this.cooldown--;
		}
		Elderwood.SlamPhase phase = this.tree.getSlamPhase();
		if (phase == Elderwood.SlamPhase.NONE) {
			return;
		}
		this.ticks++;
		if (phase == Elderwood.SlamPhase.WINDUP && this.ticks >= WINDUP_TICKS) {
			this.strike(level);
			this.ticks = 0;
			this.tree.setSlamPhase(Elderwood.SlamPhase.STUCK);
		} else if (phase == Elderwood.SlamPhase.STUCK && this.ticks >= STUCK_TICKS) {
			this.tree.setSlamPhase(Elderwood.SlamPhase.NONE);
			this.tree.switchSlamBranch();
			this.cooldown = this.tree.getTier().slamCooldownTicks();
		}
	}

	private void strike(ServerLevel level) {
		Vec3 facing = this.tree.facing();
		Vec3 point = this.tree.position().add(0.0, this.height, 0.0).add(facing.scale(this.tree.getSlamForward()))
				.add(new Vec3(-facing.z, 0.0, facing.x).scale(this.tree.getSlamRight()));
		this.smashSoftCover(level, point);
		this.tree.playSound(ModSounds.ELDERWOOD_SLAM, 2.0F, 0.7F);
		BlockPos ground = BlockPos.containing(point).below();
		level.levelEvent(LevelEvent.PARTICLES_AND_SOUND_DESTROY_BLOCK, ground, Block.getId(level.getBlockState(ground)));
		AABB area = AABB.ofSize(point.add(0.0, 1.0, 0.0), HIT_RADIUS * 2.0, 3.0, HIT_RADIUS * 2.0);
		DamageSource source = this.tree.damageSources().mobAttack(this.tree);
		for (Player player : level.getEntitiesOfClass(Player.class, area, p -> !p.isCreative() && !p.isSpectator())) {
			// Petrified players are brittle, so the hit lands harder; then the stone shell breaks.
			if (player.hurtServer(level, source, DAMAGE)) {
				Petrification.shatter(player);
				player.knockback(1.5, this.tree.getX() - player.getX(), this.tree.getZ() - player.getZ(), source, DAMAGE);
			}
		}
	}

	// Wood, leaves and dirt don't stop a branch that size; stone does. The ground it lands on stays.
	private void smashSoftCover(ServerLevel level, Vec3 point) {
		if (!level.getGameRules().get(GameRules.MOB_GRIEFING)) {
			return;
		}
		BlockPos center = BlockPos.containing(point);
		int reach = Mth.ceil(SMASH_RADIUS);
		for (BlockPos pos : BlockPos.betweenClosed(center.offset(-reach, 0, -reach), center.offset(reach, SMASH_HEIGHT - 1, reach))) {
			double dx = pos.getX() + 0.5 - point.x;
			double dz = pos.getZ() + 0.5 - point.z;
			if (dx * dx + dz * dz <= SMASH_RADIUS * SMASH_RADIUS && level.getBlockState(pos).is(BREAKABLE)) {
				level.destroyBlock(pos, true, this.tree);
			}
		}
	}
}
