package com.dmaryanskiy.vanillareforged.test;

import com.dmaryanskiy.vanillareforged.ElderwoodGaze;
import com.dmaryanskiy.vanillareforged.ModEntities;
import com.dmaryanskiy.vanillareforged.ModItems;
import com.dmaryanskiy.vanillareforged.petrification.Petrification;
import com.dmaryanskiy.vanillareforged.sentry.RuinSentry;

import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;

public class RuinSentryTest {
	private static final String NOON = "vanillareforged-test:noon";
	// Barriers let skylight through, so under the test's ceiling it's daylight too: everything that
	// isn't about the sun runs at night.
	private static final String MIDNIGHT = "vanillareforged-test:midnight";
	private static final Vec3 SENTRY = new Vec3(1.5, 1.0, 1.5);
	private static final BlockPos SENTRY_POS = BlockPos.containing(SENTRY);
	private static final int ROOF_Y = 5;
	private static final double HEART_Y = SENTRY.y + 1.53;
	private static final double PLAYER_EYE_HEIGHT = 1.62;
	// The Sentry faces south (+z), so its heart and its eye are on its +z side.
	private static final Vec3 IN_FRONT = new Vec3(1.5, HEART_Y - PLAYER_EYE_HEIGHT, 4.0);
	private static final Vec3 BEHIND = new Vec3(1.5, HEART_Y - PLAYER_EYE_HEIGHT, -1.0);

	@GameTest(environment = MIDNIGHT, maxTicks = 40)
	public void itsGazePetrifiesThePlayerItFaces(GameTestHelper helper) {
		RuinSentry sentry = sentry(helper);
		Player player = player(helper, IN_FRONT, 180.0F);
		sentry.setTarget(player);

		helper.succeedWhen(() -> helper.assertTrue(Petrification.getMeter(player) > 0, "The gaze should petrify the player"));
	}

	@GameTest(environment = MIDNIGHT, maxTicks = 40)
	public void itsGazeMissesAPlayerBehindIt(GameTestHelper helper) {
		RuinSentry sentry = sentry(helper);
		Player player = player(helper, BEHIND, 0.0F);
		sentry.setTarget(player);

		helper.runAfterDelay(10, () -> {
			helper.assertValueEqual(Petrification.getMeter(player), 0, "meter behind the Sentry");
			helper.succeed();
		});
	}

	@GameTest(environment = MIDNIGHT, maxTicks = 40)
	public void aWallBlocksItsGaze(GameTestHelper helper) {
		for (int y = 1; y <= 4; y++) {
			for (int x = 0; x <= 3; x++) {
				helper.setBlock(new BlockPos(x, y, 3), Blocks.STONE);
			}
		}
		RuinSentry sentry = sentry(helper);
		Player player = player(helper, new Vec3(1.5, 1.0, 5.5), 180.0F);
		sentry.setTarget(player);

		helper.runAfterDelay(10, () -> {
			helper.assertValueEqual(Petrification.getMeter(player), 0, "meter behind a wall");
			helper.succeed();
		});
	}

	@GameTest(environment = MIDNIGHT, maxTicks = 40)
	public void itsGazeIsWeakerThanTheElderwoods(GameTestHelper helper) {
		RuinSentry sentry = sentry(helper);
		Player player = player(helper, IN_FRONT, 180.0F);
		sentry.setTarget(player);

		helper.runAfterDelay(20, () -> {
			int meter = Petrification.getMeter(player);
			helper.assertTrue(meter >= 19 * RuinSentry.GAZE_RATE && meter <= 21 * RuinSentry.GAZE_RATE, "meter after a second: " + meter);
			helper.succeed();
		});
	}

	@GameTest(environment = MIDNIGHT, maxTicks = 40)
	public void itLetsGoOfAPlayerBeyondItsLeash(GameTestHelper helper) {
		RuinSentry sentry = sentry(helper);
		Player player = player(helper, SENTRY.add(RuinSentry.LEASH_RADIUS + 4.0, 0.0, 0.0), 0.0F);
		sentry.setTarget(player);

		helper.runAfterDelay(2, () -> {
			helper.assertTrue(sentry.getTarget() == null, "It shouldn't keep a target beyond its leash");
			helper.succeed();
		});
	}

	// Either it comes for whoever hits it or the hit does nothing: from just beyond its leash it
	// used to stand and take it.
	@GameTest(environment = MIDNIGHT)
	public void aHitFromBeyondItsLeashBouncesOff(GameTestHelper helper) {
		RuinSentry sentry = sentry(helper);
		sentry.setPost(helper.absolutePos(SENTRY_POS).north(RuinSentry.LEASH_RADIUS));
		Player player = swordsman(helper, IN_FRONT, 180.0F);

		helper.assertFalse(hit(helper, sentry, player), "A hit from beyond its leash should bounce off");
		helper.assertValueEqual(sentry.getCracks(), 0, "cracks");
		helper.assertTrue(sentry.getTarget() == null, "It shouldn't hunt a player beyond its leash");
		helper.succeed();
	}

	@GameTest(environment = MIDNIGHT, maxTicks = 200)
	public void itWalksBackToItsPostWithNobodyToHunt(GameTestHelper helper) {
		floor(helper);
		BlockPos post = helper.absolutePos(SENTRY_POS);
		RuinSentry sentry = helper.spawn(ModEntities.RUIN_SENTRY, new Vec3(1.5, 1.0, 7.5));
		sentry.setPost(post);

		helper.succeedWhen(() -> helper.assertTrue(post.closerThan(sentry.blockPosition(), 3.0), "It should be back at its post"));
	}

	@GameTest(environment = MIDNIGHT, maxTicks = 40)
	public void twoGazesPetrifyNoFasterThanOne(GameTestHelper helper) {
		RuinSentry sentry = sentry(helper);
		RuinSentry other = sentry(helper);
		Player player = player(helper, IN_FRONT, 180.0F);
		sentry.setTarget(player);
		other.setTarget(player);

		helper.runAfterDelay(20, () -> {
			int meter = Petrification.getMeter(player);
			helper.assertTrue(meter >= 19 * RuinSentry.GAZE_RATE && meter <= 21 * RuinSentry.GAZE_RATE, "meter after a second: " + meter);
			helper.succeed();
		});
	}

	@GameTest(environment = MIDNIGHT)
	public void itsBlowBreaksTheStoneItTurnedAPlayerInto(GameTestHelper helper) {
		RuinSentry sentry = sentry(helper);
		// A plain mock player: a mock ServerPlayer has no connection to take a hit with.
		Player player = helper.makeMockPlayer(GameType.SURVIVAL);
		player.snapTo(helper.absoluteVec(IN_FRONT), 180.0F, 0.0F);
		player.setAttached(Petrification.METER, Petrification.FULL + Petrification.PETRIFIED_TICKS);
		helper.assertTrue(Petrification.isPetrified(player), "The player should be petrified");
		float health = player.getHealth();

		helper.assertTrue(sentry.doHurtTarget(helper.getLevel(), player), "The blow should land");
		helper.assertTrue(player.getHealth() < health, "The blow should hurt");
		helper.assertTrue(player.getDeltaMovement().y <= 0.0, "The blow shouldn't throw the player up: " + player.getDeltaMovement().y);
		helper.assertValueEqual(Petrification.getMeter(player), 0, "meter");
		helper.assertTrue(Petrification.isImmune(player), "Cracked free, no gaze should take hold for a moment");
		helper.succeed();
	}

	@GameTest(environment = MIDNIGHT)
	public void itTakesItsPostWhereItFirstStands(GameTestHelper helper) {
		RuinSentry sentry = helper.spawn(ModEntities.RUIN_SENTRY, SENTRY);
		sentry.setNoAi(true);

		helper.runAfterDelay(2, () -> {
			helper.assertValueEqual(sentry.getPost(), helper.absolutePos(BlockPos.containing(SENTRY)), "post");
			helper.succeed();
		});
	}

	@GameTest(environment = MIDNIGHT)
	public void aSwordOnItsHeartCracksItAndTurnsItOnThePlayer(GameTestHelper helper) {
		RuinSentry sentry = sentry(helper);
		Player player = swordsman(helper, IN_FRONT, 180.0F);

		boolean hurt = hit(helper, sentry, player);

		helper.assertTrue(hurt, "A sword hit on the heart should land");
		helper.assertValueEqual(sentry.getCracks(), 1, "cracks");
		helper.assertTrue(sentry.getTarget() == player, "It should turn on its attacker");
		helper.succeed();
	}

	@GameTest(environment = MIDNIGHT)
	public void aCriticalHitCracksItsHeartTwice(GameTestHelper helper) {
		RuinSentry sentry = sentry(helper);
		Player player = swordsman(helper, IN_FRONT, 180.0F);
		player.setOnGround(false);
		player.fallDistance = 1.0;

		helper.assertTrue(hit(helper, sentry, player), "A falling sword hit on the heart should land");
		helper.assertValueEqual(sentry.getCracks(), 2, "cracks");
		helper.succeed();
	}

	@GameTest(environment = MIDNIGHT)
	public void aCriticalHitOnItsLastCrackKillsIt(GameTestHelper helper) {
		RuinSentry sentry = sentry(helper);
		sentry.setCracks(sentry.getMaxCracks() - 1);
		Player player = swordsman(helper, IN_FRONT, 180.0F);
		player.setOnGround(false);
		player.fallDistance = 1.0;

		hit(helper, sentry, player);

		helper.assertFalse(sentry.isAlive(), "It should be dead");
		helper.succeed();
	}

	@GameTest(environment = MIDNIGHT)
	public void aHitFromBehindMissesItsHeart(GameTestHelper helper) {
		RuinSentry sentry = sentry(helper);

		hit(helper, sentry, swordsman(helper, BEHIND, 0.0F));

		helper.assertValueEqual(sentry.getCracks(), 0, "cracks");
		helper.succeed();
	}

	@GameTest(environment = MIDNIGHT)
	public void theLastCrackKillsItAndItDropsLensShards(GameTestHelper helper) {
		RuinSentry sentry = sentry(helper);
		sentry.setCracks(sentry.getMaxCracks() - 1);

		hit(helper, sentry, swordsman(helper, IN_FRONT, 180.0F));

		helper.assertFalse(sentry.isAlive(), "The last crack should kill it");
		helper.assertItemEntityPresent(ModItems.ELDERWOOD_LENS_SHARD);
		helper.assertItemEntityPresent(Items.RESIN_CLUMP);
		helper.succeed();
	}

	@GameTest(environment = MIDNIGHT)
	public void sneakingWithAnAxeDoesNotTakeItApart(GameTestHelper helper) {
		RuinSentry sentry = sentry(helper);
		Player player = player(helper, IN_FRONT, 180.0F);
		player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.IRON_AXE));
		player.setShiftKeyDown(true);

		sentry.interact(player, InteractionHand.MAIN_HAND, sentry.position());

		helper.assertFalse(sentry.isRemoved(), "A Ruin Sentry can't be taken apart");
		helper.assertItemEntityNotPresent(Items.CREAKING_HEART);
		helper.succeed();
	}

	@GameTest(environment = MIDNIGHT)
	public void thePaleMasksGazeFindsItWithoutBeingHunted(GameTestHelper helper) {
		RuinSentry sentry = sentry(helper);
		Player player = player(helper, new Vec3(1.5, 1.0, 5.5), 180.0F);
		player.setItemSlot(EquipmentSlot.HEAD, new ItemStack(ModItems.PALE_MASK));

		helper.assertTrue(ElderwoodGaze.findTarget(player).filter(target -> target == sentry).isPresent(),
				"A Ruin Sentry is always hostile, so the mask's gaze should take hold of it");
		helper.succeed();
	}

	@GameTest(environment = NOON, skyAccess = true, maxTicks = 40)
	public void directSunlightTurnsItIntoAStatueAndClearsItsCracks(GameTestHelper helper) {
		RuinSentry sentry = sentry(helper);
		sentry.setCracks(2);

		helper.succeedWhen(() -> {
			helper.assertTrue(sentry.isStatue(), "It should be a statue in direct sunlight");
			helper.assertTrue(sentry.isNoAi(), "A statue shouldn't run AI");
			helper.assertValueEqual(sentry.getCracks(), 0, "cracks");
			helper.assertFalse(hit(helper, sentry, swordsman(helper, IN_FRONT, 180.0F)), "A statue can't be hurt");
			helper.assertValueEqual(sentry.getCracks(), 0, "cracks after a hit");
		});
	}

	// Sky access is read from skylight, which the light engine updates lazily, so allow it time.
	@GameTest(environment = NOON, skyAccess = true, maxTicks = 300)
	public void itWakesOnceItIsShaded(GameTestHelper helper) {
		RuinSentry sentry = sentry(helper);

		helper.startSequence()
				.thenWaitUntil(() -> helper.assertTrue(sentry.isStatue(), "statue first"))
				.thenExecute(() -> helper.setBlock(SENTRY_POS.atY(ROOF_Y), Blocks.STONE))
				.thenWaitUntil(() -> helper.assertFalse(sentry.isStatue(), "It should wake in the shade"))
				.thenExecute(() -> helper.assertFalse(sentry.isNoAi(), "Awake, it runs its AI again"))
				.thenSucceed();
	}

	@GameTest(environment = MIDNIGHT, skyAccess = true, maxTicks = 40)
	public void anOpenSkyAtNightDoesNothingToIt(GameTestHelper helper) {
		RuinSentry sentry = sentry(helper);

		helper.runAfterDelay(20, () -> {
			helper.assertFalse(sentry.isStatue(), "Only sunlight turns it into a statue");
			helper.succeed();
		});
	}

	@GameTest(environment = NOON, skyAccess = true, maxTicks = 300)
	public void aHitFromASunlitSpotBouncesOffItsShieldedHeart(GameTestHelper helper) {
		roof(helper, 2);
		RuinSentry sentry = sentry(helper);
		Player player = swordsman(helper, IN_FRONT, 180.0F);

		helper.startSequence()
				.thenWaitUntil(() -> {
					helper.assertTrue(sentry.isSunlit(player.blockPosition()), "The player should stand in the sun");
					helper.assertFalse(sentry.isSunlit(sentry.blockPosition()), "The Sentry should be in the shade");
					helper.assertFalse(sentry.isStatue(), "In the shade it's awake");
				})
				.thenExecute(() -> {
					// Waking gave it its AI back; pin it again so it keeps facing the player.
					sentry.setNoAi(true);
					helper.assertFalse(hit(helper, sentry, player), "A hit from the sun shouldn't land");
					helper.assertValueEqual(sentry.getCracks(), 0, "cracks");
				})
				.thenWaitUntil(() -> helper.assertTrue(sentry.isShielding(), "It should shield its heart from a target in the sun"))
				.thenSucceed();
	}

	@GameTest(environment = NOON, skyAccess = true, maxTicks = 300)
	public void itDoesNotFollowItsTargetIntoTheSun(GameTestHelper helper) {
		floor(helper);
		roof(helper, 3);
		RuinSentry sentry = helper.spawn(ModEntities.RUIN_SENTRY, new Vec3(3.5, 1.0, 1.5));
		sentry.setPost(helper.absolutePos(SENTRY_POS));
		// A plain mock player: if the Sentry did reach it, a hit on a mock ServerPlayer (which has no
		// connection) would bring the whole test server down instead of failing this test.
		Player player = helper.makeMockPlayer(GameType.SURVIVAL);
		player.snapTo(helper.absoluteVec(new Vec3(3.5, 1.0, 6.5)), 180.0F, 0.0F);

		helper.startSequence()
				.thenWaitUntil(() -> {
					helper.assertTrue(sentry.isSunlit(player.blockPosition()), "The player should stand in the sun");
					helper.assertFalse(sentry.isSunlit(sentry.blockPosition()), "The Sentry should be in the shade");
					helper.assertFalse(sentry.isStatue(), "In the shade it's awake");
				})
				.thenExecute(() -> sentry.setTarget(player))
				.thenExecuteAfter(100, () -> {
					helper.assertFalse(sentry.isStatue(), "It shouldn't have walked into the sun");
					helper.assertFalse(sentry.isSunlit(sentry.blockPosition()), "It should still be in the shade");
					helper.assertTrue(sentry.isShielding(), "It should shield its heart from a target in the sun");
				})
				.thenSucceed();
	}

	// The default test structure is empty and floats above the ground, so a walking Sentry needs a floor.
	private static void floor(GameTestHelper helper) {
		for (int x = 0; x < 8; x++) {
			for (int z = 0; z < 8; z++) {
				helper.setBlock(new BlockPos(x, 0, z), Blocks.STONE);
			}
		}
	}

	/** A roof over the test's north side, up to and including row {@code lastZ}; the rest stays open to the sky. */
	private static void roof(GameTestHelper helper, int lastZ) {
		for (int x = 0; x < 8; x++) {
			for (int z = 0; z <= lastZ; z++) {
				helper.setBlock(new BlockPos(x, ROOF_Y, z), Blocks.STONE);
			}
		}
	}

	private static RuinSentry sentry(GameTestHelper helper) {
		RuinSentry sentry = helper.spawn(ModEntities.RUIN_SENTRY, SENTRY);
		sentry.setPost(helper.absolutePos(SENTRY_POS));
		sentry.setNoAi(true);
		sentry.setYRot(0.0F);
		sentry.setYHeadRot(0.0F);
		sentry.setYBodyRot(0.0F);
		return sentry;
	}

	private static Player player(GameTestHelper helper, Vec3 pos, float yRot) {
		Player player = helper.makeMockServerPlayer(GameType.SURVIVAL);
		player.snapTo(helper.absoluteVec(pos), yRot, 0.0F);
		player.setYHeadRot(yRot);
		return player;
	}

	private static Player swordsman(GameTestHelper helper, Vec3 pos, float yRot) {
		Player player = player(helper, pos, yRot);
		player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.IRON_SWORD));
		return player;
	}

	private static boolean hit(GameTestHelper helper, RuinSentry sentry, Player player) {
		return sentry.hurtServer(helper.getLevel(), helper.getLevel().damageSources().playerAttack(player),
				(float) player.getAttributeValue(Attributes.ATTACK_DAMAGE));
	}
}
