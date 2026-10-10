package com.dmaryanskiy.vanillareforged.test;

import java.util.List;

import com.dmaryanskiy.vanillareforged.ModEntities;
import com.dmaryanskiy.vanillareforged.ModItems;
import com.dmaryanskiy.vanillareforged.petrification.Petrification;
import com.dmaryanskiy.vanillareforged.watcher.BurningResin;
import com.dmaryanskiy.vanillareforged.watcher.Watcher;
import com.dmaryanskiy.vanillareforged.watcher.WatcherHeart;

import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.gametest.framework.GameTestSequence;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.WeatheringCopper;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

public class WatcherTest {
	private static final String NOON = "vanillareforged-test:noon";
	private static final String MIDNIGHT = "vanillareforged-test:midnight";
	// The wide tree, top row first, in the x/y plane at z = 2; the heart is placed last.
	private static final String[] PATTERN = {"~~W~~", "LLCLL", "R~H~R", "~LLL~"};
	private static final int TOP = 4;
	private static final BlockPos HEART = new BlockPos(2, TOP - 2, 2);
	private static final Vec3 WATCHER = new Vec3(3.5, 1.0, 2.5);
	// It faces south (+z), so its front is on its +z side.
	private static final Vec3 FRONT = new Vec3(0.0, 0.0, 1.0);

	// Sky access removes the test's barrier ceiling, which would cut through the pattern.
	@GameTest(environment = MIDNIGHT, skyAccess = true)
	public void placingTheHeartLastAtNightWakesTheWatcher(GameTestHelper helper) {
		buildBody(helper, Blocks.COPPER_BLOCK.weathering().weathered());
		helper.setBlock(HEART, Blocks.CREAKING_HEART);

		Watcher watcher = findWatcher(helper);
		helper.assertValueEqual(watcher.getCopperStage(), WeatheringCopper.WeatherState.WEATHERED, "copper stage");
		helper.assertBlockPresent(Blocks.AIR, HEART);
		helper.assertBlockPresent(Blocks.AIR, new BlockPos(2, TOP, 2));
		helper.succeed();
	}

	@GameTest(environment = MIDNIGHT, skyAccess = true)
	public void waxedCopperKeepsItsStage(GameTestHelper helper) {
		buildBody(helper, Blocks.COPPER_BLOCK.waxed().oxidized());
		helper.setBlock(HEART, Blocks.CREAKING_HEART);

		helper.assertValueEqual(findWatcher(helper).getCopperStage(), WeatheringCopper.WeatherState.OXIDIZED, "copper stage");
		helper.succeed();
	}

	@GameTest(environment = MIDNIGHT, skyAccess = true)
	public void plantsAndCarpetsInTheEmptyCellsDoNotBlockIt(GameTestHelper helper) {
		for (int x = 0; x < 5; x++) {
			helper.setBlock(new BlockPos(x, 0, 2), Blocks.STONE);
		}
		buildBody(helper, Blocks.COPPER_BLOCK.weathering().unaffected());
		BlockPos carpet = new BlockPos(0, 1, 2);
		helper.setBlock(carpet, Blocks.PALE_MOSS_CARPET);
		helper.setBlock(new BlockPos(4, 1, 2), Blocks.SHORT_GRASS);
		helper.setBlock(HEART, Blocks.CREAKING_HEART);

		helper.assertValueEqual(findWatcher(helper).getCopperStage(), WeatheringCopper.WeatherState.UNAFFECTED, "copper stage");
		helper.assertBlockPresent(Blocks.PALE_MOSS_CARPET, carpet);
		helper.succeed();
	}

	@GameTest(environment = MIDNIGHT, skyAccess = true)
	public void aFullBlockInAnEmptyCellBlocksIt(GameTestHelper helper) {
		buildBody(helper, Blocks.COPPER_BLOCK.weathering().unaffected());
		helper.setBlock(new BlockPos(0, TOP, 2), Blocks.STONE);
		helper.setBlock(HEART, Blocks.CREAKING_HEART);

		helper.assertEntityNotPresent(ModEntities.WATCHER);
		helper.succeed();
	}

	@GameTest(environment = NOON, skyAccess = true)
	public void itDoesNotWakeInDaylight(GameTestHelper helper) {
		buildBody(helper, Blocks.COPPER_BLOCK.weathering().unaffected());
		helper.setBlock(HEART, Blocks.CREAKING_HEART);

		helper.assertEntityNotPresent(ModEntities.WATCHER);
		helper.assertBlockPresent(Blocks.CREAKING_HEART, HEART);
		helper.succeed();
	}

	@GameTest(environment = MIDNIGHT, skyAccess = true)
	public void anIncompleteBodyStaysAsleep(GameTestHelper helper) {
		buildBody(helper, Blocks.COPPER_BLOCK.weathering().unaffected());
		helper.setBlock(new BlockPos(0, TOP - 2, 2), Blocks.AIR);
		helper.setBlock(HEART, Blocks.CREAKING_HEART);

		helper.assertEntityNotPresent(ModEntities.WATCHER);
		helper.succeed();
	}

	@GameTest(environment = MIDNIGHT, maxTicks = 40)
	public void itsGazePetrifiesThePlayerItWatches(GameTestHelper helper) {
		Watcher watcher = helper.spawn(ModEntities.WATCHER, new Vec3(1.5, 1.0, 1.5));
		// Out of melee reach: a mock player can't take hits (it has no connection).
		Player player = helper.makeMockServerPlayer(GameType.SURVIVAL);
		player.snapTo(helper.absoluteVec(new Vec3(6.5, 1.0, 6.5)), 0.0F, 0.0F);
		watcher.setTarget(player);

		helper.succeedWhen(() -> helper.assertTrue(Petrification.getMeter(player) > 0, "The gaze should petrify the player"));
	}

	@GameTest(environment = MIDNIGHT, maxTicks = 40)
	public void aWallBlocksItsGaze(GameTestHelper helper) {
		Watcher watcher = helper.spawn(ModEntities.WATCHER, new Vec3(1.5, 1.0, 1.5));
		Player player = helper.makeMockServerPlayer(GameType.SURVIVAL);
		player.snapTo(helper.absoluteVec(new Vec3(1.5, 1.0, 6.5)), 0.0F, 0.0F);
		for (int x = 0; x < 8; x++) {
			for (int y = 0; y < 8; y++) {
				helper.setBlock(new BlockPos(x, y, 4), Blocks.STONE);
			}
		}
		watcher.setTarget(player);

		helper.runAfterDelay(20, () -> {
			helper.assertValueEqual(Petrification.getMeter(player), 0, "meter behind a wall");
			helper.succeed();
		});
	}

	@GameTest(environment = MIDNIGHT)
	public void swordOnAChestHeartCracksIt(GameTestHelper helper) {
		Watcher watcher = stillWatcher(helper);

		boolean hurt = hit(helper, watcher, WatcherHeart.RIGHT_CHEST, FRONT);

		helper.assertTrue(hurt, "A sword hit on a chest heart should land");
		helper.assertValueEqual(watcher.getCracks(WatcherHeart.RIGHT_CHEST), 1, "right chest cracks");
		helper.assertValueEqual(watcher.getCracks(WatcherHeart.LEFT_CHEST), 0, "left chest cracks");
		helper.succeed();
	}

	@GameTest(environment = MIDNIGHT)
	public void chestHeartsCannotBeHitFromBehind(GameTestHelper helper) {
		Watcher watcher = stillWatcher(helper);

		helper.assertFalse(hit(helper, watcher, WatcherHeart.RIGHT_CHEST, FRONT.scale(-1.0)), "The body shields its hearts from behind");
		helper.succeed();
	}

	@GameTest(environment = MIDNIGHT)
	public void theCoreIsShutWhileOtherHeartsLive(GameTestHelper helper) {
		Watcher watcher = stillWatcher(helper);

		helper.assertFalse(hit(helper, watcher, WatcherHeart.CORE, FRONT), "The core should be closed");
		helper.assertValueEqual(watcher.getCracks(WatcherHeart.CORE), 0, "core cracks");
		helper.succeed();
	}

	@GameTest(environment = MIDNIGHT)
	public void anArmHeartIsOutOfReachUntilTheArmIsStuck(GameTestHelper helper) {
		Watcher watcher = stillWatcher(helper);

		helper.assertFalse(hit(helper, watcher, WatcherHeart.RIGHT_ARM, FRONT), "A hanging arm's heart can't be hit");
		helper.succeed();
	}

	// The Watcher needs its AI for the slam to run, so these start it right after turning it south.
	@GameTest(environment = MIDNIGHT, maxTicks = 60)
	public void theStuckArmExposesItsHeartAndHidesTheChest(GameTestHelper helper) {
		Watcher watcher = facingSouth(helper.spawn(ModEntities.WATCHER, WATCHER));
		boolean rightArm = watcher.isSlammingRightArm();
		WatcherHeart arm = rightArm ? WatcherHeart.RIGHT_ARM : WatcherHeart.LEFT_ARM;
		watcher.startSlam();

		helper.startSequence()
				.thenWaitUntil(() -> helper.assertTrue(watcher.getSlamPhase() == Watcher.SlamPhase.STUCK, "arm stuck"))
				.thenExecute(() -> {
					helper.assertFalse(hit(helper, watcher, WatcherHeart.RIGHT_CHEST, FRONT), "Bent over, the chest can't be hit");
					helper.assertTrue(hit(helper, watcher, arm, new Vec3(rightArm ? -1.0 : 1.0, 0.0, 0.0)),
							"The stuck arm's heart should be open, from the side too");
					helper.assertValueEqual(watcher.getCracks(arm), 1, "arm cracks");
				})
				.thenSucceed();
	}

	@GameTest(environment = MIDNIGHT, maxTicks = 100)
	public void destroyingTheCoreLastKillsIt(GameTestHelper helper) {
		Watcher watcher = stillWatcher(helper);
		for (WatcherHeart heart : WatcherHeart.values()) {
			if (heart != WatcherHeart.CORE) {
				watcher.setCracks(heart, heart.maxCracks());
			}
		}
		Runnable hitCore = () -> hit(helper, watcher, WatcherHeart.CORE, FRONT);

		GameTestSequence sequence = helper.startSequence().thenExecute(hitCore);
		for (int i = 1; i < WatcherHeart.CORE.maxCracks(); i++) {
			sequence.thenExecute(() -> helper.assertTrue(watcher.isAlive(), "It should survive until the core's last crack"))
					.thenExecuteAfter(12, hitCore);
		}
		sequence.thenExecute(() -> {
			helper.assertFalse(watcher.isAlive(), "Destroying the core should kill it");
			helper.assertItemEntityPresent(ModItems.WATCHERS_LENS);
		}).thenSucceed();
	}

	@GameTest(environment = MIDNIGHT, maxTicks = 60)
	public void theSlamHitsHarderOnAPetrifiedPlayerAndShattersTheStone(GameTestHelper helper) {
		Watcher watcher = facingSouth(helper.spawn(ModEntities.WATCHER, WATCHER));
		// A plain mock player: a mock ServerPlayer has no connection, which taking damage needs.
		Player player = helper.makeMockPlayer(GameType.SURVIVAL);
		player.snapTo(helper.absoluteVec(WATCHER.add(0.0, 0.0, 1.8)), 0.0F, 0.0F);
		helper.getLevel().addFreshEntity(player);
		Petrification.expose(player, Petrification.FULL);
		watcher.startSlam();

		helper.startSequence()
				.thenWaitUntil(() -> helper.assertTrue(watcher.getSlamPhase() == Watcher.SlamPhase.STUCK, "arm stuck"))
				.thenExecute(() -> {
					helper.assertTrue(player.getHealth() < player.getMaxHealth() - Watcher.SLAM_DAMAGE,
							"A petrified player should take more than the slam's base damage");
					helper.assertValueEqual(Petrification.getMeter(player), 0, "meter after the slam");
				})
				.thenSucceed();
	}

	@GameTest(environment = MIDNIGHT, maxTicks = 60)
	public void steppingOutOfTheSlamDodgesIt(GameTestHelper helper) {
		Watcher watcher = facingSouth(helper.spawn(ModEntities.WATCHER, WATCHER));
		Player player = helper.makeMockPlayer(GameType.SURVIVAL);
		player.snapTo(helper.absoluteVec(WATCHER.add(-3.0, 0.0, 1.8)), 0.0F, 0.0F);
		helper.getLevel().addFreshEntity(player);
		watcher.startSlam();

		helper.startSequence()
				.thenWaitUntil(() -> helper.assertTrue(watcher.getSlamPhase() == Watcher.SlamPhase.STUCK, "arm stuck"))
				.thenExecute(() -> helper.assertValueEqual(player.getHealth(), player.getMaxHealth(), "health beside the slam"))
				.thenSucceed();
	}

	@GameTest(environment = MIDNIGHT)
	public void itDropsItsLens(GameTestHelper helper) {
		Watcher watcher = helper.spawn(ModEntities.WATCHER, new Vec3(1.5, 1.0, 1.5));

		watcher.kill(helper.getLevel());

		helper.assertItemEntityPresent(ModItems.WATCHERS_LENS);
		helper.succeed();
	}

	@GameTest(environment = MIDNIGHT, maxTicks = 60)
	public void burningResinSetsAPlayerOnFire(GameTestHelper helper) {
		Watcher watcher = stillWatcher(helper);
		Player player = plainPlayer(helper, WATCHER.add(0.0, 0.0, 4.5));
		throwResin(helper, watcher, WATCHER.add(0.0, 1.0, 2.0), new Vec3(0.0, 0.0, 1.0));

		helper.succeedWhen(() -> {
			helper.assertTrue(player.getRemainingFireTicks() > 0, "Burning resin should set the player on fire");
			helper.assertTrue(player.getHealth() < player.getMaxHealth(), "Burning resin should hurt");
		});
	}

	@GameTest(environment = MIDNIGHT, maxTicks = 60)
	public void burningResinDoesNotSetBlocksOnFire(GameTestHelper helper) {
		Watcher watcher = stillWatcher(helper);
		for (int x = 2; x <= 4; x++) {
			for (int y = 1; y <= 3; y++) {
				helper.setBlock(new BlockPos(x, y, 6), Blocks.OAK_PLANKS);
			}
		}
		BurningResin resin = throwResin(helper, watcher, WATCHER.add(0.0, 1.0, 2.0), new Vec3(0.0, 0.0, 1.0));

		helper.startSequence()
				.thenWaitUntil(() -> helper.assertTrue(resin.isRemoved(), "The resin should burst on the planks"))
				.thenExecute(() -> helper.assertBlockNotPresent(Blocks.FIRE, new BlockPos(3, 2, 5)))
				.thenSucceed();
	}

	@GameTest(environment = MIDNIGHT, maxTicks = 60)
	public void resinHitBackByAPlayerStunsIt(GameTestHelper helper) {
		Watcher watcher = stillWatcher(helper);
		Player player = helper.makeMockServerPlayer(GameType.SURVIVAL);
		throwResin(helper, player, WATCHER.add(0.0, 2.0, 4.5), new Vec3(0.0, 0.0, -1.0));

		helper.succeedWhen(() -> helper.assertTrue(watcher.isStunned(), "Its own resin, hit back, should stun it"));
	}

	@GameTest(environment = MIDNIGHT, maxTicks = 60)
	public void resinFromAnotherWatcherDoesNotStunIt(GameTestHelper helper) {
		Watcher watcher = stillWatcher(helper);
		Watcher other = helper.spawn(ModEntities.WATCHER, WATCHER.add(0.0, 0.0, 5.0));
		other.setNoAi(true);
		BurningResin resin = throwResin(helper, other, WATCHER.add(0.0, 2.0, 3.0), new Vec3(0.0, 0.0, -1.0));

		helper.startSequence()
				.thenWaitUntil(() -> helper.assertTrue(resin.isRemoved(), "The resin should hit the Watcher"))
				.thenExecute(() -> helper.assertFalse(watcher.isStunned(), "Only resin a player hit back stuns it"))
				.thenSucceed();
	}

	@GameTest(environment = MIDNIGHT)
	public void aPlayersHitSendsTheResinBack(GameTestHelper helper) {
		Watcher watcher = stillWatcher(helper);
		Player player = helper.makeMockServerPlayer(GameType.SURVIVAL);
		player.snapTo(helper.absoluteVec(WATCHER.add(0.0, 0.0, 5.0)), 180.0F, 0.0F);
		BurningResin resin = throwResin(helper, watcher, WATCHER.add(0.0, 1.5, 4.0), new Vec3(0.0, 0.0, 1.0));

		player.attack(resin);

		helper.assertTrue(resin.getOwner() == player, "Hitting the resin should make it the player's");
		helper.assertTrue(resin.getDeltaMovement().z < 0.0, "It should fly back the way the player looks");
		helper.succeed();
	}

	@GameTest(environment = MIDNIGHT, maxTicks = 60)
	public void stunnedItStopsGazingAndItsArmHeartsOpen(GameTestHelper helper) {
		Watcher watcher = facingSouth(helper.spawn(ModEntities.WATCHER, WATCHER));
		// Close enough not to draw resin, far enough not to draw a slam.
		Player player = helper.makeMockServerPlayer(GameType.SURVIVAL);
		player.snapTo(helper.absoluteVec(WATCHER.add(0.0, 0.0, 4.9)), 0.0F, 0.0F);
		watcher.setTarget(player);
		watcher.stun();

		helper.runAfterDelay(20, () -> {
			helper.assertTrue(watcher.isStunned(), "still stunned");
			helper.assertValueEqual(Petrification.getMeter(player), 0, "meter while it's stunned");
			helper.assertTrue(hit(helper, watcher, WatcherHeart.LEFT_ARM, FRONT), "A stunned Watcher's arm hearts should be open");
			helper.assertValueEqual(watcher.getCracks(WatcherHeart.LEFT_ARM), 1, "left arm cracks");
			helper.succeed();
		});
	}

	@GameTest(environment = MIDNIGHT, maxTicks = Watcher.STUN_TICKS + 40)
	public void theStunWearsOffAndCannotBeRepeatedAtOnce(GameTestHelper helper) {
		Watcher watcher = facingSouth(helper.spawn(ModEntities.WATCHER, WATCHER));
		watcher.stun();

		helper.startSequence()
				.thenWaitUntil(() -> helper.assertFalse(watcher.isStunned(), "The stun should wear off"))
				.thenExecute(() -> {
					watcher.stun();
					helper.assertFalse(watcher.isStunned(), "It shouldn't be stunned again right away");
				})
				.thenSucceed();
	}

	@GameTest(environment = MIDNIGHT, maxTicks = Watcher.RESIN_INTERVAL_TICKS + 60)
	public void itThrowsBurningResinAtADistantPlayer(GameTestHelper helper) {
		// The test structure floats, and this takes a few seconds: without a floor both would fall out of it.
		for (int x = 0; x < 8; x++) {
			for (int z = 0; z < 8; z++) {
				helper.setBlock(new BlockPos(x, 0, z), Blocks.STONE);
			}
		}
		Watcher watcher = helper.spawn(ModEntities.WATCHER, new Vec3(1.5, 1.0, 1.5));
		// Rooted, so it can't close in and switch to its slam before it throws.
		watcher.getAttribute(Attributes.MOVEMENT_SPEED).setBaseValue(0.0);
		watcher.setTarget(plainPlayer(helper, new Vec3(6.5, 1.0, 6.5)));

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

	private static Watcher stillWatcher(GameTestHelper helper) {
		Watcher watcher = facingSouth(helper.spawn(ModEntities.WATCHER, WATCHER));
		watcher.setNoAi(true);
		return watcher;
	}

	private static Watcher facingSouth(Watcher watcher) {
		watcher.setYRot(0.0F);
		watcher.setYHeadRot(0.0F);
		watcher.setYBodyRot(0.0F);
		return watcher;
	}

	/** A full-strength sword swing aimed at the heart by a player standing two blocks from it on the given side. */
	private static boolean hit(GameTestHelper helper, Watcher watcher, WatcherHeart heart, Vec3 side) {
		Vec3 target = heart.box(watcher).getCenter();
		Vec3 eyes = target.add(side.normalize().scale(2.0));
		Player player = helper.makeMockServerPlayer(GameType.SURVIVAL);
		player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.IRON_SWORD));
		float yaw = (float) Math.toDegrees(Math.atan2(target.z - eyes.z, target.x - eyes.x)) - 90.0F;
		player.snapTo(eyes.subtract(0.0, player.getEyeHeight(), 0.0), yaw, 0.0F);
		player.setYHeadRot(yaw);
		float damage = (float) player.getAttributeValue(Attributes.ATTACK_DAMAGE);
		return watcher.hurtServer(helper.getLevel(), helper.getLevel().damageSources().playerAttack(player), damage);
	}

	private static void buildBody(GameTestHelper helper, Block copper) {
		for (int row = 0; row < PATTERN.length; row++) {
			for (int x = 0; x < 5; x++) {
				Block block = switch (PATTERN[row].charAt(x)) {
					case 'W', 'L' -> Blocks.PALE_OAK_LOG;
					case 'C' -> copper;
					case 'R' -> Blocks.RESIN_BLOCK;
					default -> null;
				};
				if (block != null) {
					helper.setBlock(new BlockPos(x, TOP - row, 2), block);
				}
			}
		}
	}

	private static Watcher findWatcher(GameTestHelper helper) {
		List<Watcher> watchers = helper.getLevel().getEntities(ModEntities.WATCHER,
				new AABB(helper.absolutePos(HEART)).inflate(4.0), watcher -> true);
		helper.assertValueEqual(watchers.size(), 1, "watchers woken");
		return watchers.getFirst();
	}
}
