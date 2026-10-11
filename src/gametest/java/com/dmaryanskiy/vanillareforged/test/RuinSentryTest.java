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
	private static final Vec3 SENTRY = new Vec3(1.5, 1.0, 1.5);
	private static final double HEART_Y = SENTRY.y + 1.53;
	private static final double PLAYER_EYE_HEIGHT = 1.62;
	// The Sentry faces south (+z), so its heart and its eye are on its +z side.
	private static final Vec3 IN_FRONT = new Vec3(1.5, HEART_Y - PLAYER_EYE_HEIGHT, 4.0);
	private static final Vec3 BEHIND = new Vec3(1.5, HEART_Y - PLAYER_EYE_HEIGHT, -1.0);

	@GameTest(maxTicks = 40)
	public void itsGazePetrifiesThePlayerItFaces(GameTestHelper helper) {
		RuinSentry sentry = sentry(helper);
		Player player = player(helper, IN_FRONT, 180.0F);
		sentry.setTarget(player);

		helper.succeedWhen(() -> helper.assertTrue(Petrification.getMeter(player) > 0, "The gaze should petrify the player"));
	}

	@GameTest(maxTicks = 40)
	public void itsGazeMissesAPlayerBehindIt(GameTestHelper helper) {
		RuinSentry sentry = sentry(helper);
		Player player = player(helper, BEHIND, 0.0F);
		sentry.setTarget(player);

		helper.runAfterDelay(10, () -> {
			helper.assertValueEqual(Petrification.getMeter(player), 0, "meter behind the Sentry");
			helper.succeed();
		});
	}

	@GameTest(maxTicks = 40)
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

	@GameTest(maxTicks = 40)
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

	@GameTest(maxTicks = 40)
	public void itLetsGoOfAPlayerBeyondItsLeash(GameTestHelper helper) {
		RuinSentry sentry = sentry(helper);
		Player player = player(helper, SENTRY.add(RuinSentry.LEASH_RADIUS + 4.0, 0.0, 0.0), 0.0F);
		sentry.setTarget(player);

		helper.runAfterDelay(2, () -> {
			helper.assertTrue(sentry.getTarget() == null, "It shouldn't keep a target beyond its leash");
			helper.succeed();
		});
	}

	@GameTest
	public void itTakesItsPostWhereItFirstStands(GameTestHelper helper) {
		RuinSentry sentry = helper.spawn(ModEntities.RUIN_SENTRY, SENTRY);
		sentry.setNoAi(true);

		helper.runAfterDelay(2, () -> {
			helper.assertValueEqual(sentry.getPost(), helper.absolutePos(BlockPos.containing(SENTRY)), "post");
			helper.succeed();
		});
	}

	@GameTest
	public void aSwordOnItsHeartCracksItAndTurnsItOnThePlayer(GameTestHelper helper) {
		RuinSentry sentry = sentry(helper);
		Player player = swordsman(helper, IN_FRONT, 180.0F);

		boolean hurt = hit(helper, sentry, player);

		helper.assertTrue(hurt, "A sword hit on the heart should land");
		helper.assertValueEqual(sentry.getCracks(), 1, "cracks");
		helper.assertTrue(sentry.getTarget() == player, "It should turn on its attacker");
		helper.succeed();
	}

	@GameTest
	public void aHitFromBehindMissesItsHeart(GameTestHelper helper) {
		RuinSentry sentry = sentry(helper);

		hit(helper, sentry, swordsman(helper, BEHIND, 0.0F));

		helper.assertValueEqual(sentry.getCracks(), 0, "cracks");
		helper.succeed();
	}

	@GameTest
	public void theLastCrackKillsItAndItDropsLensShards(GameTestHelper helper) {
		RuinSentry sentry = sentry(helper);
		sentry.setCracks(sentry.getMaxCracks() - 1);

		hit(helper, sentry, swordsman(helper, IN_FRONT, 180.0F));

		helper.assertFalse(sentry.isAlive(), "The last crack should kill it");
		helper.assertItemEntityPresent(ModItems.ELDERWOOD_LENS_SHARD);
		helper.assertItemEntityPresent(Items.RESIN_CLUMP);
		helper.succeed();
	}

	@GameTest
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

	@GameTest
	public void thePaleMasksGazeFindsItWithoutBeingHunted(GameTestHelper helper) {
		RuinSentry sentry = sentry(helper);
		Player player = player(helper, new Vec3(1.5, 1.0, 5.5), 180.0F);
		player.setItemSlot(EquipmentSlot.HEAD, new ItemStack(ModItems.PALE_MASK));

		helper.assertTrue(ElderwoodGaze.findTarget(player).filter(target -> target == sentry).isPresent(),
				"A Ruin Sentry is always hostile, so the mask's gaze should take hold of it");
		helper.succeed();
	}

	private static RuinSentry sentry(GameTestHelper helper) {
		RuinSentry sentry = helper.spawn(ModEntities.RUIN_SENTRY, SENTRY);
		sentry.setPost(helper.absolutePos(BlockPos.containing(SENTRY)));
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
