package com.dmaryanskiy.vanillareforged.test;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.dmaryanskiy.vanillareforged.ModEntities;
import com.dmaryanskiy.vanillareforged.ModItems;
import com.dmaryanskiy.vanillareforged.petrification.Petrification;
import com.dmaryanskiy.vanillareforged.elderwood.BurningResin;
import com.dmaryanskiy.vanillareforged.elderwood.Elderwood;
import com.dmaryanskiy.vanillareforged.elderwood.ElderwoodBranchHeart;
import com.dmaryanskiy.vanillareforged.elderwood.ElderwoodHeart;
import com.dmaryanskiy.vanillareforged.elderwood.ElderwoodRoots;
import com.dmaryanskiy.vanillareforged.elderwood.ElderwoodSummon;

import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.gametest.framework.GameTestSequence;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.arrow.Arrow;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.WeatheringCopper;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

public class ElderwoodTest {
	private static final String NOON = "vanillareforged-test:noon";
	private static final String MIDNIGHT = "vanillareforged-test:midnight";
	// The wide tree, top row first, in the x/y plane at z = 2; the heart is placed last.
	private static final BlockPos LOG = new BlockPos(3, 1, 3);
	private static final BlockPos HEART = LOG.above(2);
	private static final Vec3 ELDERWOOD = new Vec3(3.5, 1.0, 2.5);
	// It faces south (+z), so its front is on its +z side.
	private static final Vec3 FRONT = new Vec3(0.0, 0.0, 1.0);

	// Sky access removes the test's barrier ceiling: the tree needs more room than the test is tall.
	@GameTest(environment = MIDNIGHT, skyAccess = true, maxTicks = Elderwood.GROW_TICKS + 40)
	public void placingTheHeartLastAtNightGrowsTheElderwood(GameTestHelper helper) {
		buildStump(helper, Blocks.COPPER_BLOCK.weathering().weathered());
		helper.setBlock(HEART, Blocks.CREAKING_HEART);

		Elderwood elderwood = findElderwood(helper);
		helper.assertValueEqual(elderwood.getCopperStage(), WeatheringCopper.WeatherState.WEATHERED, "copper stage");
		for (BlockPos pos : new BlockPos[] {HEART, HEART.below(), LOG, LOG.north(), LOG.south(), LOG.east(), LOG.west()}) {
			helper.assertBlockPresent(Blocks.AIR, pos);
		}
		helper.assertTrue(elderwood.isGrowing(), "It should start as a sprout");
		helper.assertFalse(hit(helper, elderwood, ElderwoodHeart.TRUNK_FRONT, elderwood.getLookAngle()), "It can't be hurt while growing");
		helper.assertFalse(elderwood.canBeCollidedWith(null), "It isn't solid until it's grown");
		helper.assertValueEqual(elderwood.getGrowStage(), 1, "size right after waking");
		helper.runAfterDelay(Elderwood.GROW_STAGE_TICKS + 2, () -> helper.assertValueEqual(elderwood.getGrowStage(), 2, "size after one burst"));
		helper.runAfterDelay(2 * Elderwood.GROW_STAGE_TICKS + 2,
				() -> helper.assertValueEqual(elderwood.getGrowStage(), 3, "size after two bursts"));
		helper.succeedWhen(() -> {
			helper.assertFalse(elderwood.isGrowing(), "It should finish growing");
			helper.assertTrue(elderwood.canBeCollidedWith(null), "The grown trunk should be solid");
		});
	}

	@GameTest(environment = MIDNIGHT, skyAccess = true)
	public void waxedCopperKeepsItsStage(GameTestHelper helper) {
		buildStump(helper, Blocks.COPPER_BLOCK.waxed().oxidized());
		helper.setBlock(HEART, Blocks.CREAKING_HEART);

		helper.assertValueEqual(findElderwood(helper).getCopperStage(), WeatheringCopper.WeatherState.OXIDIZED, "copper stage");
		helper.succeed();
	}

	@GameTest(environment = MIDNIGHT, skyAccess = true)
	public void plantsAndCarpetsAroundTheStumpDoNotBlockIt(GameTestHelper helper) {
		buildStump(helper, Blocks.COPPER_BLOCK.weathering().unaffected());
		BlockPos carpet = LOG.offset(2, 0, 2);
		helper.setBlock(carpet, Blocks.PALE_MOSS_CARPET);
		helper.setBlock(LOG.offset(-1, 0, -1), Blocks.SHORT_GRASS);
		helper.setBlock(HEART, Blocks.CREAKING_HEART);

		helper.assertValueEqual(findElderwood(helper).getCopperStage(), WeatheringCopper.WeatherState.UNAFFECTED, "copper stage");
		helper.assertBlockPresent(Blocks.PALE_MOSS_CARPET, carpet);
		helper.succeed();
	}

	@GameTest(environment = MIDNIGHT, skyAccess = true)
	public void aFullBlockInItsWayLeavesTheStumpAsItIs(GameTestHelper helper) {
		buildStump(helper, Blocks.COPPER_BLOCK.weathering().unaffected());
		helper.setBlock(LOG.offset(2, ElderwoodSummon.ROOM_HEIGHT - 1, -2), Blocks.STONE);
		helper.setBlock(HEART, Blocks.CREAKING_HEART);

		helper.assertEntityNotPresent(ModEntities.ELDERWOOD);
		helper.assertBlockPresent(Blocks.CREAKING_HEART, HEART);
		helper.assertBlockPresent(Blocks.RESIN_BLOCK, LOG.north());
		helper.succeed();
	}

	@GameTest(environment = NOON, skyAccess = true)
	public void itDoesNotWakeInDaylight(GameTestHelper helper) {
		buildStump(helper, Blocks.COPPER_BLOCK.weathering().unaffected());
		helper.setBlock(HEART, Blocks.CREAKING_HEART);

		helper.assertEntityNotPresent(ModEntities.ELDERWOOD);
		helper.assertBlockPresent(Blocks.CREAKING_HEART, HEART);
		helper.succeed();
	}

	@GameTest(environment = MIDNIGHT, skyAccess = true)
	public void aStumpMissingARootStaysAsleep(GameTestHelper helper) {
		buildStump(helper, Blocks.COPPER_BLOCK.weathering().unaffected());
		helper.setBlock(LOG.east(), Blocks.AIR);
		helper.setBlock(HEART, Blocks.CREAKING_HEART);

		helper.assertEntityNotPresent(ModEntities.ELDERWOOD);
		helper.succeed();
	}

	@GameTest(environment = MIDNIGHT)
	public void nothingMovesItFromItsSpot(GameTestHelper helper) {
		Elderwood elderwood = stillElderwood(helper);
		Vec3 spot = elderwood.position();

		elderwood.move(MoverType.SELF, new Vec3(1.0, 0.0, 1.0));
		elderwood.move(MoverType.PISTON, new Vec3(1.0, 0.0, 0.0));

		helper.assertValueEqual(elderwood.getX(), spot.x, "x after being moved");
		helper.assertValueEqual(elderwood.getZ(), spot.z, "z after being moved");
		helper.assertFalse(elderwood.isPushable(), "It shouldn't be pushable");
		helper.succeed();
	}

	@GameTest(environment = MIDNIGHT, maxTicks = 40)
	public void itTurnsSlowlyTowardsItsTarget(GameTestHelper helper) {
		Elderwood elderwood = facingSouth(helper.spawn(ModEntities.ELDERWOOD, ELDERWOOD));
		// Straight behind it, out of slam reach.
		Player player = helper.makeMockServerPlayer(GameType.SURVIVAL);
		player.snapTo(helper.absoluteVec(ELDERWOOD.add(0.5, 0.0, -7.0)), 0.0F, 0.0F);
		elderwood.setTarget(player);

		helper.runAfterDelay(20, () -> {
			float turned = Math.abs(Mth.degreesDifference(0.0F, elderwood.yBodyRot));
			helper.assertTrue(turned > 10.0F, "It should be turning towards the player, turned " + turned);
			helper.assertTrue(turned <= 20 * Elderwood.TURN_DEGREES_PER_TICK + 0.5F, "It should turn slowly, turned " + turned);
			helper.succeed();
		});
	}

	@GameTest(environment = MIDNIGHT, maxTicks = 40)
	public void itsGazePetrifiesThePlayerItWatches(GameTestHelper helper) {
		Elderwood elderwood = helper.spawn(ModEntities.ELDERWOOD, new Vec3(1.5, 1.0, 1.5));
		// Out of melee reach: a mock player can't take hits (it has no connection).
		Player player = helper.makeMockServerPlayer(GameType.SURVIVAL);
		player.snapTo(helper.absoluteVec(new Vec3(6.5, 1.0, 6.5)), 0.0F, 0.0F);
		elderwood.setTarget(player);

		helper.succeedWhen(() -> helper.assertTrue(Petrification.getMeter(player) > 0, "The gaze should petrify the player"));
	}

	@GameTest(environment = MIDNIGHT, maxTicks = 40)
	public void aWallBlocksItsGaze(GameTestHelper helper) {
		Elderwood elderwood = helper.spawn(ModEntities.ELDERWOOD, new Vec3(1.5, 1.0, 1.5));
		Player player = helper.makeMockServerPlayer(GameType.SURVIVAL);
		player.snapTo(helper.absoluteVec(new Vec3(1.5, 1.0, 6.5)), 0.0F, 0.0F);
		for (int x = 0; x < 8; x++) {
			for (int y = 0; y < 8; y++) {
				helper.setBlock(new BlockPos(x, y, 4), Blocks.STONE);
			}
		}
		elderwood.setTarget(player);

		helper.runAfterDelay(20, () -> {
			helper.assertValueEqual(Petrification.getMeter(player), 0, "meter behind a wall");
			helper.succeed();
		});
	}

	@GameTest(environment = MIDNIGHT)
	public void swordOnATrunkHeartCracksIt(GameTestHelper helper) {
		Elderwood elderwood = stillElderwood(helper);

		boolean hurt = hit(helper, elderwood, ElderwoodHeart.TRUNK_FRONT, FRONT);

		helper.assertTrue(hurt, "A sword hit on a trunk heart should land");
		helper.assertValueEqual(elderwood.getCracks(ElderwoodHeart.TRUNK_FRONT), 1, "front heart cracks");
		helper.assertValueEqual(elderwood.getCracks(ElderwoodHeart.TRUNK_RIGHT), 0, "right heart cracks");
		helper.succeed();
	}

	// Its box doesn't turn with it. Turned 45 degrees, the front heart sits inside the box's corner: the
	// corner is within the player's reach here, the heart itself just beyond it.
	@GameTest(environment = MIDNIGHT)
	public void aSwingAtFullReachCracksAHeartOnATurnedTrunk(GameTestHelper helper) {
		Elderwood elderwood = stillElderwood(helper);
		elderwood.setYRot(45.0F);
		elderwood.setYHeadRot(45.0F);
		elderwood.setYBodyRot(45.0F);
		Vec3 outwards = Vec3.directionFromRotation(0.0F, 45.0F);

		helper.assertTrue(hit(helper, elderwood, ElderwoodHeart.TRUNK_FRONT, outwards, 3.6, 1.0F),
				"A swing that reaches the trunk's box should reach the heart in it");
		helper.succeed();
	}

	@GameTest(environment = MIDNIGHT)
	public void aWeakSwingDoesNotCrackAHeart(GameTestHelper helper) {
		Elderwood elderwood = stillElderwood(helper);

		helper.assertFalse(hit(helper, elderwood, ElderwoodHeart.TRUNK_FRONT, FRONT, 2.0, 0.6F), "A half-charged swing shouldn't crack a heart");
		helper.assertValueEqual(elderwood.getCracks(ElderwoodHeart.TRUNK_FRONT), 0, "front heart cracks");
		helper.succeed();
	}

	// Facing south, its right is to the west.
	@GameTest(environment = MIDNIGHT, maxTicks = 80)
	public void eachTrunkHeartOpensOnlyFromItsOwnSide(GameTestHelper helper) {
		Elderwood elderwood = stillElderwood(helper);
		Map<ElderwoodHeart, Vec3> sides = new LinkedHashMap<>();
		sides.put(ElderwoodHeart.TRUNK_FRONT, FRONT);
		sides.put(ElderwoodHeart.TRUNK_BACK, FRONT.scale(-1.0));
		sides.put(ElderwoodHeart.TRUNK_RIGHT, new Vec3(-1.0, 0.0, 0.0));
		sides.put(ElderwoodHeart.TRUNK_LEFT, new Vec3(1.0, 0.0, 0.0));

		GameTestSequence sequence = helper.startSequence();
		sides.forEach((heart, side) -> sequence.thenExecuteAfter(12, () -> {
			helper.assertFalse(hit(helper, elderwood, heart, side.scale(-1.0)), heart + " shouldn't open through the trunk");
			helper.assertTrue(hit(helper, elderwood, heart, side), heart + " should open from its own side");
			helper.assertValueEqual(elderwood.getCracks(heart), 1, heart + " cracks");
		}));
		sequence.thenSucceed();
	}

	@GameTest(environment = MIDNIGHT, maxTicks = 40)
	public void itsGazeMissesAPlayerBehindIt(GameTestHelper helper) {
		Elderwood elderwood = facingSouth(helper.spawn(ModEntities.ELDERWOOD, ELDERWOOD));
		Player player = helper.makeMockServerPlayer(GameType.SURVIVAL);
		player.snapTo(helper.absoluteVec(ELDERWOOD.add(0.5, 0.0, -7.0)), 0.0F, 0.0F);
		elderwood.setTarget(player);

		helper.runAfterDelay(20, () -> {
			helper.assertFalse(elderwood.isWatching(player), "The lens shouldn't see behind the trunk");
			helper.assertValueEqual(Petrification.getMeter(player), 0, "meter behind it");
			helper.succeed();
		});
	}

	@GameTest(environment = MIDNIGHT)
	public void theLensSeesSixtyDegreesToEachSide(GameTestHelper helper) {
		Elderwood elderwood = stillElderwood(helper);
		Player player = helper.makeMockServerPlayer(GameType.SURVIVAL);

		// 45 degrees off straight ahead, then 72.
		player.snapTo(helper.absoluteVec(ELDERWOOD.add(3.0, 0.0, 3.0)), 0.0F, 0.0F);
		helper.assertTrue(elderwood.isWatching(player), "It should see a player 45 degrees to the side");
		player.snapTo(helper.absoluteVec(ELDERWOOD.add(3.0, 0.0, 1.0)), 0.0F, 0.0F);
		helper.assertFalse(elderwood.isWatching(player), "It shouldn't see a player 72 degrees to the side");
		helper.succeed();
	}

	@GameTest(environment = MIDNIGHT)
	public void theCoreIsShutWhileOtherHeartsLive(GameTestHelper helper) {
		Elderwood elderwood = stillElderwood(helper);

		helper.assertFalse(hit(helper, elderwood, ElderwoodHeart.CORE, FRONT), "The core should be closed");
		helper.assertValueEqual(elderwood.getCracks(ElderwoodHeart.CORE), 0, "core cracks");
		helper.succeed();
	}

	@GameTest(environment = MIDNIGHT)
	public void aBranchHeartIsOutOfReachUntilTheBranchIsPlanted(GameTestHelper helper) {
		Elderwood elderwood = stillElderwood(helper);

		helper.assertFalse(hit(helper, elderwood, ElderwoodHeart.RIGHT_BRANCH, FRONT), "A branch heart overhead can't be hit");
		helper.succeed();
	}

	// The Elderwood needs its AI for the slam to run, so these start it right after turning it south.
	@GameTest(environment = MIDNIGHT, maxTicks = 60)
	public void thePlantedBranchBringsItsHeartWithinReach(GameTestHelper helper) {
		Elderwood elderwood = facingSouth(helper.spawn(ModEntities.ELDERWOOD, ELDERWOOD));
		boolean right = elderwood.isSlammingRightBranch();
		ElderwoodHeart branch = right ? ElderwoodHeart.RIGHT_BRANCH : ElderwoodHeart.LEFT_BRANCH;
		elderwood.startSlam();

		helper.startSequence()
				.thenWaitUntil(() -> helper.assertTrue(elderwood.getSlamPhase() == Elderwood.SlamPhase.STUCK, "branch planted"))
				.thenExecute(() -> {
					helper.assertTrue(branch.box(elderwood).getCenter().y - elderwood.getY() < 2.0, "The planted branch's heart should be low");
					helper.assertTrue(hit(helper, elderwood, branch, new Vec3(right ? -1.0 : 1.0, 0.0, 0.0)),
							"The planted branch's heart should be open, from the side too");
					helper.assertValueEqual(elderwood.getCracks(branch), 1, "branch cracks");
				})
				.thenSucceed();
	}

	@GameTest(environment = MIDNIGHT, maxTicks = 100)
	public void destroyingTheCoreLastKillsIt(GameTestHelper helper) {
		Elderwood elderwood = stillElderwood(helper);
		for (ElderwoodHeart heart : ElderwoodHeart.values()) {
			if (heart != ElderwoodHeart.CORE) {
				elderwood.setCracks(heart, heart.maxCracks());
			}
		}
		Runnable hitCore = () -> hit(helper, elderwood, ElderwoodHeart.CORE, FRONT);

		GameTestSequence sequence = helper.startSequence().thenExecute(hitCore);
		for (int i = 1; i < ElderwoodHeart.CORE.maxCracks(); i++) {
			sequence.thenExecute(() -> helper.assertTrue(elderwood.isAlive(), "It should survive until the core's last crack"))
					.thenExecuteAfter(12, hitCore);
		}
		sequence.thenExecute(() -> {
			helper.assertFalse(elderwood.isAlive(), "Destroying the core should kill it");
			helper.assertItemEntityPresent(ModItems.ELDERWOOD_LENS);
		}).thenSucceed();
	}

	@GameTest(environment = MIDNIGHT, maxTicks = 60)
	public void theSlamHitsHarderOnAPetrifiedPlayerAndShattersTheStone(GameTestHelper helper) {
		Elderwood elderwood = facingSouth(helper.spawn(ModEntities.ELDERWOOD, ELDERWOOD));
		// A plain mock player: a mock ServerPlayer has no connection, which taking damage needs.
		Player player = helper.makeMockPlayer(GameType.SURVIVAL);
		player.snapTo(helper.absoluteVec(ELDERWOOD.add(0.0, 0.0, Elderwood.SLAM_DISTANCE)), 0.0F, 0.0F);
		helper.getLevel().addFreshEntity(player);
		Petrification.expose(player, Petrification.FULL);
		elderwood.startSlam();

		helper.startSequence()
				.thenWaitUntil(() -> helper.assertTrue(elderwood.getSlamPhase() == Elderwood.SlamPhase.STUCK, "branch planted"))
				.thenExecute(() -> {
					helper.assertTrue(player.getHealth() < player.getMaxHealth() - Elderwood.SLAM_DAMAGE,
							"A petrified player should take more than the slam's base damage");
					helper.assertValueEqual(Petrification.getMeter(player), 0, "meter after the slam");
				})
				.thenSucceed();
	}

	// Facing south, its right is to the west, so a player to the east is on its left.
	@GameTest(environment = MIDNIGHT, maxTicks = 60)
	public void theSlamLandsWhereItsTargetStands(GameTestHelper helper) {
		Elderwood elderwood = facingSouth(helper.spawn(ModEntities.ELDERWOOD, ELDERWOOD));
		Player player = plainPlayer(helper, ELDERWOOD.add(2.5, 0.0, 3.5));
		elderwood.setTarget(player);

		helper.startSequence()
				.thenWaitUntil(() -> helper.assertTrue(elderwood.getSlamPhase() == Elderwood.SlamPhase.STUCK, "branch planted"))
				.thenExecute(() -> {
					helper.assertTrue(player.getHealth() < player.getMaxHealth(), "The slam should hit a player off to the side");
					helper.assertTrue(Math.abs(elderwood.getSlamRight() + 2.5F) < 0.5F, "It should aim to its left, was " + elderwood.getSlamRight());
					ElderwoodHeart branch = elderwood.isSlammingRightBranch() ? ElderwoodHeart.RIGHT_BRANCH : ElderwoodHeart.LEFT_BRANCH;
					Vec3 heart = branch.box(elderwood).getCenter();
					helper.assertTrue(heart.y - elderwood.getY() < 2.0, "The planted branch's heart should be low");
					helper.assertTrue(heart.z - elderwood.getZ() > 2.0, "The heart should be out where the branch landed");
				})
				.thenSucceed();
	}

	@GameTest(environment = MIDNIGHT, maxTicks = 60)
	public void theSlamSmashesWoodButNotStone(GameTestHelper helper) {
		Elderwood elderwood = facingSouth(helper.spawn(ModEntities.ELDERWOOD, ELDERWOOD));
		// It lands at z 6.0, between these two rows.
		BlockPos planks = new BlockPos(3, 1, 5);
		BlockPos roof = new BlockPos(3, 3, 5);
		BlockPos stone = new BlockPos(3, 1, 6);
		helper.setBlock(planks, Blocks.OAK_PLANKS);
		helper.setBlock(roof, Blocks.PALE_OAK_LEAVES);
		helper.setBlock(stone, Blocks.STONE);
		elderwood.startSlam();

		helper.startSequence()
				.thenWaitUntil(() -> helper.assertTrue(elderwood.getSlamPhase() == Elderwood.SlamPhase.STUCK, "branch planted"))
				.thenExecute(() -> {
					helper.assertBlockPresent(Blocks.AIR, planks);
					helper.assertBlockPresent(Blocks.AIR, roof);
					helper.assertBlockPresent(Blocks.STONE, stone);
					helper.assertItemEntityPresent(Items.OAK_PLANKS);
				})
				.thenSucceed();
	}

	@GameTest(environment = MIDNIGHT, maxTicks = ElderwoodRoots.LIFE_TICKS + 20)
	public void rootsHurtWhoeverStillStandsOnTheCrackedGround(GameTestHelper helper) {
		Elderwood elderwood = stillElderwood(helper);
		Player caught = plainPlayer(helper, ELDERWOOD.add(-2.5, 0.0, 1.0));
		Player away = plainPlayer(helper, ELDERWOOD.add(-2.5, 0.0, 4.5));
		elderwood.raiseRoots(helper.getLevel(), caught.position());

		helper.runAfterDelay(ElderwoodRoots.WARNING_TICKS - 2,
				() -> helper.assertValueEqual(caught.getHealth(), caught.getMaxHealth(), "health while the ground only cracks"));
		helper.startSequence()
				.thenWaitUntil(() -> helper.assertEntityNotPresent(ModEntities.ELDERWOOD_ROOTS))
				.thenExecute(() -> {
					helper.assertValueEqual(caught.getHealth(), caught.getMaxHealth() - ElderwoodRoots.DAMAGE, "health after the roots");
					helper.assertValueEqual(away.getHealth(), away.getMaxHealth(), "health away from the roots");
				})
				.thenSucceed();
	}

	// Facing south, a player to its north-west is well outside what the lens sees.
	@GameTest(environment = MIDNIGHT, maxTicks = 40)
	public void itRaisesRootsUnderAPlayerItsLensCannotSee(GameTestHelper helper) {
		Elderwood elderwood = facingSouth(helper.spawn(ModEntities.ELDERWOOD, ELDERWOOD));
		Player player = helper.makeMockServerPlayer(GameType.SURVIVAL);
		player.snapTo(helper.absoluteVec(ELDERWOOD.add(-3.0, 0.0, -1.5)), 0.0F, 0.0F);
		elderwood.setTarget(player);

		helper.succeedWhen(() -> helper.assertEntityPresent(ModEntities.ELDERWOOD_ROOTS));
	}

	@GameTest(environment = MIDNIGHT, maxTicks = 60)
	public void itRaisesNoRootsUnderAPlayerItWatches(GameTestHelper helper) {
		Elderwood elderwood = facingSouth(helper.spawn(ModEntities.ELDERWOOD, ELDERWOOD));
		Player player = helper.makeMockServerPlayer(GameType.SURVIVAL);
		player.snapTo(helper.absoluteVec(ELDERWOOD.add(0.0, 0.0, 5.0)), 0.0F, 0.0F);
		elderwood.setTarget(player);

		helper.runAfterDelay(50, () -> {
			helper.assertEntityNotPresent(ModEntities.ELDERWOOD_ROOTS);
			helper.succeed();
		});
	}

	@GameTest(environment = MIDNIGHT, maxTicks = Elderwood.DORMANT_AFTER_TICKS + 2 * Elderwood.HEAL_INTERVAL_TICKS + 40)
	public void leftAloneItFallsDormantAndItsHeartsMend(GameTestHelper helper) {
		Elderwood elderwood = facingSouth(helper.spawn(ModEntities.ELDERWOOD, ELDERWOOD));
		elderwood.setCracks(ElderwoodHeart.TRUNK_BACK, ElderwoodHeart.TRUNK_BACK.maxCracks());

		helper.runAfterDelay(Elderwood.DORMANT_AFTER_TICKS - 20, () -> {
			helper.assertFalse(elderwood.isDormant(), "It shouldn't fall dormant so soon");
			helper.assertTrue(elderwood.isDestroyed(ElderwoodHeart.TRUNK_BACK), "Its hearts shouldn't mend before it's dormant");
		});
		helper.runAfterDelay(Elderwood.DORMANT_AFTER_TICKS + Elderwood.HEAL_INTERVAL_TICKS + 20, () -> {
			helper.assertTrue(elderwood.isDormant(), "It should be dormant by now");
			helper.assertValueEqual(elderwood.getCracks(ElderwoodHeart.TRUNK_BACK), ElderwoodHeart.TRUNK_BACK.maxCracks() - 2,
					"cracks after two mends");
			helper.succeed();
		});
	}

	@GameTest(environment = MIDNIGHT, maxTicks = 60)
	public void steppingOutOfTheSlamDodgesIt(GameTestHelper helper) {
		Elderwood elderwood = facingSouth(helper.spawn(ModEntities.ELDERWOOD, ELDERWOOD));
		Player player = helper.makeMockPlayer(GameType.SURVIVAL);
		player.snapTo(helper.absoluteVec(ELDERWOOD.add(-3.0, 0.0, Elderwood.SLAM_DISTANCE)), 0.0F, 0.0F);
		helper.getLevel().addFreshEntity(player);
		elderwood.startSlam();

		helper.startSequence()
				.thenWaitUntil(() -> helper.assertTrue(elderwood.getSlamPhase() == Elderwood.SlamPhase.STUCK, "branch planted"))
				.thenExecute(() -> helper.assertValueEqual(player.getHealth(), player.getMaxHealth(), "health beside the slam"))
				.thenSucceed();
	}

	@GameTest(environment = MIDNIGHT)
	public void itDropsItsLens(GameTestHelper helper) {
		Elderwood elderwood = helper.spawn(ModEntities.ELDERWOOD, new Vec3(1.5, 1.0, 1.5));

		elderwood.kill(helper.getLevel());

		helper.assertItemEntityPresent(ModItems.ELDERWOOD_LENS);
		helper.succeed();
	}

	@GameTest(environment = MIDNIGHT, maxTicks = 60)
	public void burningResinSetsAPlayerOnFire(GameTestHelper helper) {
		Elderwood elderwood = stillElderwood(helper);
		Player player = plainPlayer(helper, ELDERWOOD.add(0.0, 0.0, 4.5));
		throwResin(helper, elderwood, ELDERWOOD.add(0.0, 1.0, 2.0), new Vec3(0.0, 0.0, 1.0));

		helper.succeedWhen(() -> {
			helper.assertTrue(player.getRemainingFireTicks() > 0, "Burning resin should set the player on fire");
			helper.assertTrue(player.getHealth() < player.getMaxHealth(), "Burning resin should hurt");
		});
	}

	@GameTest(environment = MIDNIGHT, maxTicks = 60)
	public void burningResinDoesNotSetBlocksOnFire(GameTestHelper helper) {
		Elderwood elderwood = stillElderwood(helper);
		for (int x = 2; x <= 4; x++) {
			for (int y = 1; y <= 3; y++) {
				helper.setBlock(new BlockPos(x, y, 6), Blocks.OAK_PLANKS);
			}
		}
		BurningResin resin = throwResin(helper, elderwood, ELDERWOOD.add(0.0, 1.0, 2.0), new Vec3(0.0, 0.0, 1.0));

		helper.startSequence()
				.thenWaitUntil(() -> helper.assertTrue(resin.isRemoved(), "The resin should burst on the planks"))
				.thenExecute(() -> helper.assertBlockNotPresent(Blocks.FIRE, new BlockPos(3, 2, 5)))
				.thenSucceed();
	}

	@GameTest(environment = MIDNIGHT, maxTicks = 60)
	public void resinHitBackByAPlayerStunsIt(GameTestHelper helper) {
		Elderwood elderwood = stillElderwood(helper);
		Player player = helper.makeMockServerPlayer(GameType.SURVIVAL);
		throwResin(helper, player, ELDERWOOD.add(0.0, 2.0, 4.5), new Vec3(0.0, 0.0, -1.0));

		helper.succeedWhen(() -> helper.assertTrue(elderwood.isStunned(), "Its own resin, hit back, should stun it"));
	}

	@GameTest(environment = MIDNIGHT, maxTicks = 60)
	public void resinFromAnotherElderwoodDoesNotStunIt(GameTestHelper helper) {
		Elderwood elderwood = stillElderwood(helper);
		Elderwood other = helper.spawn(ModEntities.ELDERWOOD, ELDERWOOD.add(0.0, 0.0, 5.0));
		other.setNoAi(true);
		BurningResin resin = throwResin(helper, other, ELDERWOOD.add(0.0, 2.0, 3.0), new Vec3(0.0, 0.0, -1.0));

		helper.startSequence()
				.thenWaitUntil(() -> helper.assertTrue(resin.isRemoved(), "The resin should hit the Elderwood"))
				.thenExecute(() -> helper.assertFalse(elderwood.isStunned(), "Only resin a player hit back stuns it"))
				.thenSucceed();
	}

	@GameTest(environment = MIDNIGHT)
	public void aPlayersHitSendsTheResinBack(GameTestHelper helper) {
		Elderwood elderwood = stillElderwood(helper);
		Player player = helper.makeMockServerPlayer(GameType.SURVIVAL);
		player.snapTo(helper.absoluteVec(ELDERWOOD.add(0.0, 0.0, 5.0)), 180.0F, 0.0F);
		BurningResin resin = throwResin(helper, elderwood, ELDERWOOD.add(0.0, 1.5, 4.0), new Vec3(0.0, 0.0, 1.0));

		player.attack(resin);

		helper.assertTrue(resin.getOwner() == player, "Hitting the resin should make it the player's");
		helper.assertTrue(resin.getDeltaMovement().z < 0.0, "It should fly back the way the player looks");
		helper.succeed();
	}

	@GameTest(environment = MIDNIGHT, maxTicks = 60)
	public void stunnedItStopsGazingAndItsBranchHeartsOpen(GameTestHelper helper) {
		Elderwood elderwood = facingSouth(helper.spawn(ModEntities.ELDERWOOD, ELDERWOOD));
		// Close enough not to draw resin, far enough not to draw a slam.
		Player player = helper.makeMockServerPlayer(GameType.SURVIVAL);
		player.snapTo(helper.absoluteVec(ELDERWOOD.add(0.0, 0.0, 4.9)), 0.0F, 0.0F);
		elderwood.setTarget(player);
		elderwood.stun();

		helper.runAfterDelay(20, () -> {
			helper.assertTrue(elderwood.isStunned(), "still stunned");
			helper.assertValueEqual(Petrification.getMeter(player), 0, "meter while it's stunned");
			helper.assertTrue(hit(helper, elderwood, ElderwoodHeart.LEFT_BRANCH, FRONT), "A stunned Elderwood's branch hearts should be open");
			helper.assertValueEqual(elderwood.getCracks(ElderwoodHeart.LEFT_BRANCH), 1, "left branch cracks");
			helper.succeed();
		});
	}

	// A stunned tree's branch hearts droop far outside its own box, where an arrow would fly past it.
	@GameTest(environment = MIDNIGHT, skyAccess = true, maxTicks = 60)
	public void anArrowCracksAStunnedBranchHeartWhereItHangs(GameTestHelper helper) {
		Elderwood elderwood = facingSouth(helper.spawn(ModEntities.ELDERWOOD, ELDERWOOD));
		Player player = helper.makeMockServerPlayer(GameType.SURVIVAL);
		helper.runAfterDelay(2, () -> {
			helper.assertValueEqual(branchHeartParts(helper, elderwood).stream().filter(Entity::isPickable).count(), 0L,
					"branch heart parts open at rest");
			elderwood.stun();
		});
		helper.runAfterDelay(5, () -> {
			Vec3 heart = ElderwoodHeart.RIGHT_BRANCH.box(elderwood).getCenter();
			helper.assertTrue(elderwood.getBoundingBox().distanceToSqr(heart) > 4.0, "The drooping heart should hang well outside the trunk");
			helper.assertValueEqual(branchHeartParts(helper, elderwood).stream().filter(Entity::isPickable).count(), 2L,
					"branch heart parts open while stunned");
			Arrow arrow = new Arrow(helper.getLevel(), heart.x, heart.y + 12.0, heart.z, new ItemStack(Items.ARROW), null);
			arrow.setOwner(player);
			arrow.setCritArrow(true);
			arrow.setDeltaMovement(0.0, -3.0, 0.0);
			helper.getLevel().addFreshEntity(arrow);
		});
		helper.succeedWhen(() -> helper.assertValueEqual(elderwood.getCracks(ElderwoodHeart.RIGHT_BRANCH), 1, "right branch cracks"));
	}

	private static List<ElderwoodBranchHeart> branchHeartParts(GameTestHelper helper, Elderwood elderwood) {
		// Only where this tree's own branch hearts are: other tests' trees stand close by.
		AABB right = ElderwoodHeart.RIGHT_BRANCH.box(elderwood);
		AABB left = ElderwoodHeart.LEFT_BRANCH.box(elderwood);
		return helper.getLevel().getEntities(ModEntities.ELDERWOOD_BRANCH_HEART, right.minmax(left),
				part -> part.getBoundingBox().intersects(right) || part.getBoundingBox().intersects(left));
	}

	@GameTest(environment = MIDNIGHT, maxTicks = Elderwood.STUN_TICKS + 40)
	public void theStunWearsOffAndCannotBeRepeatedAtOnce(GameTestHelper helper) {
		Elderwood elderwood = facingSouth(helper.spawn(ModEntities.ELDERWOOD, ELDERWOOD));
		elderwood.stun();

		helper.startSequence()
				.thenWaitUntil(() -> helper.assertFalse(elderwood.isStunned(), "The stun should wear off"))
				.thenExecute(() -> {
					elderwood.stun();
					helper.assertFalse(elderwood.isStunned(), "It shouldn't be stunned again right away");
				})
				.thenSucceed();
	}

	@GameTest(environment = MIDNIGHT, maxTicks = Elderwood.RESIN_INTERVAL_TICKS + 60)
	public void itThrowsBurningResinAtADistantPlayer(GameTestHelper helper) {
		// The test structure floats, and this takes a few seconds: without a floor both would fall out of it.
		for (int x = 0; x < 8; x++) {
			for (int z = 0; z < 8; z++) {
				helper.setBlock(new BlockPos(x, 0, z), Blocks.STONE);
			}
		}
		Elderwood elderwood = helper.spawn(ModEntities.ELDERWOOD, new Vec3(1.5, 1.0, 1.5));
		// Rooted, so it can't close in and switch to its slam before it throws.
		elderwood.getAttribute(Attributes.MOVEMENT_SPEED).setBaseValue(0.0);
		elderwood.setTarget(plainPlayer(helper, new Vec3(6.5, 1.0, 6.5)));

		helper.succeedWhen(() -> helper.assertEntityPresent(ModEntities.BURNING_RESIN));
	}

	// A plain mock player in the level: unlike a mock ServerPlayer it can take damage (no connection needed).
	private static Player plainPlayer(GameTestHelper helper, Vec3 pos) {
		Player player = helper.makeMockPlayer(GameType.SURVIVAL);
		player.snapTo(helper.absoluteVec(pos), 0.0F, 0.0F);
		helper.getLevel().addFreshEntity(player);
		return player;
	}

	private static BurningResin throwResin(GameTestHelper helper, LivingEntity owner, Vec3 from, Vec3 direction) {
		BurningResin resin = new BurningResin(helper.getLevel(), owner, direction);
		resin.setPos(helper.absoluteVec(from));
		helper.getLevel().addFreshEntity(resin);
		return resin;
	}

	private static Elderwood stillElderwood(GameTestHelper helper) {
		Elderwood elderwood = facingSouth(helper.spawn(ModEntities.ELDERWOOD, ELDERWOOD));
		elderwood.setNoAi(true);
		return elderwood;
	}

	private static Elderwood facingSouth(Elderwood elderwood) {
		elderwood.setYRot(0.0F);
		elderwood.setYHeadRot(0.0F);
		elderwood.setYBodyRot(0.0F);
		return elderwood;
	}

	/** A full-strength sword swing aimed at the heart by a player standing two blocks from it on the given side. */
	private static boolean hit(GameTestHelper helper, Elderwood elderwood, ElderwoodHeart heart, Vec3 side) {
		return hit(helper, elderwood, heart, side, 2.0, 1.0F);
	}

	private static boolean hit(GameTestHelper helper, Elderwood elderwood, ElderwoodHeart heart, Vec3 side, double distance, float strength) {
		Vec3 target = heart.box(elderwood).getCenter();
		Vec3 eyes = target.add(side.normalize().scale(distance));
		Player player = helper.makeMockServerPlayer(GameType.SURVIVAL);
		player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.IRON_SWORD));
		float yaw = (float) Math.toDegrees(Math.atan2(target.z - eyes.z, target.x - eyes.x)) - 90.0F;
		player.snapTo(eyes.subtract(0.0, player.getEyeHeight(), 0.0), yaw, 0.0F);
		player.setYHeadRot(yaw);
		float damage = strength * (float) player.getAttributeValue(Attributes.ATTACK_DAMAGE);
		return elderwood.hurtServer(helper.getLevel(), helper.getLevel().damageSources().playerAttack(player), damage);
	}

	/** The stump without its heart, on a stone floor for the tree to stand on. */
	private static void buildStump(GameTestHelper helper, Block copper) {
		for (int x = -2; x <= 2; x++) {
			for (int z = -2; z <= 2; z++) {
				helper.setBlock(LOG.offset(x, -1, z), Blocks.STONE);
			}
		}
		helper.setBlock(LOG, Blocks.PALE_OAK_LOG);
		for (Direction side : Direction.Plane.HORIZONTAL) {
			helper.setBlock(LOG.relative(side), Blocks.RESIN_BLOCK);
		}
		helper.setBlock(LOG.above(), copper);
	}

	private static Elderwood findElderwood(GameTestHelper helper) {
		List<Elderwood> elderwoods = helper.getLevel().getEntities(ModEntities.ELDERWOOD,
				new AABB(helper.absolutePos(HEART)).inflate(4.0), elderwood -> true);
		helper.assertValueEqual(elderwoods.size(), 1, "elderwoods woken");
		return elderwoods.getFirst();
	}
}
