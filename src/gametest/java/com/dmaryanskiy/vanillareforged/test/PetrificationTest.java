package com.dmaryanskiy.vanillareforged.test;

import com.dmaryanskiy.vanillareforged.ModItems;
import com.dmaryanskiy.vanillareforged.ElderwoodGaze;
import com.dmaryanskiy.vanillareforged.petrification.Petrification;

import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.item.Items;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameType;

public class PetrificationTest {
	@GameTest
	public void gazeFillsTheMeterAndSlows(GameTestHelper helper) {
		Player player = helper.makeMockServerPlayer(GameType.SURVIVAL);
		double baseSpeed = player.getAttributeValue(Attributes.MOVEMENT_SPEED);

		Petrification.expose(player, Petrification.FULL / 2);

		helper.assertValueEqual(Petrification.getProgress(player), 0.5F, "progress");
		helper.assertValueEqual(player.getAttributeValue(Attributes.MOVEMENT_SPEED), baseSpeed, "movement speed at half");

		// In the same tick a stronger gaze only adds what it has over the first: gazes don't add up.
		Petrification.expose(player, Petrification.FULL * 3 / 4);

		helper.assertValueEqual(Petrification.getProgress(player), 0.75F, "progress under the stronger of two gazes");
		helper.assertValueEqual(player.getAttributeValue(Attributes.MOVEMENT_SPEED),
				baseSpeed * (1.0 - Petrification.MAX_SLOWDOWN * 0.5F), "movement speed at three quarters");
		helper.succeed();
	}

	@GameTest
	public void fullMeterPetrifies(GameTestHelper helper) {
		Player player = helper.makeMockServerPlayer(GameType.SURVIVAL);

		Petrification.expose(player, Petrification.FULL);

		helper.assertTrue(Petrification.isPetrified(player), "Player should be petrified");
		helper.assertValueEqual(player.getAttributeValue(Attributes.MOVEMENT_SPEED), 0.0, "movement speed");
		helper.assertValueEqual(player.getAttributeValue(Attributes.JUMP_STRENGTH), 0.0, "jump strength");
		helper.succeed();
	}

	@GameTest
	public void petrifiedPlayerBurnsNoWorse(GameTestHelper helper) {
		Player player = helper.makeMockPlayer(GameType.SURVIVAL);
		Petrification.expose(player, Petrification.FULL);

		player.hurtServer(helper.getLevel(), helper.getLevel().damageSources().onFire(), 4.0F);

		helper.assertValueEqual(player.getHealth(), player.getMaxHealth() - 4.0F, "health");
		helper.succeed();
	}

	@GameTest
	public void petrifiedPlayerIsBrittle(GameTestHelper helper) {
		// A plain mock player: a mock ServerPlayer has no connection, which hurtServer needs.
		Player player = helper.makeMockPlayer(GameType.SURVIVAL);
		Petrification.expose(player, Petrification.FULL);

		player.hurtServer(helper.getLevel(), helper.getLevel().damageSources().generic(), 4.0F);

		helper.assertValueEqual(player.getHealth(), player.getMaxHealth() - 4.0F * (1.0F + Petrification.MAX_EXTRA_DAMAGE), "health");
		helper.succeed();
	}

	@GameTest(maxTicks = ElderwoodGaze.TICKS_PER_DURABILITY + 20)
	public void paleMaskBlocksTheGazeButWearsDown(GameTestHelper helper) {
		Player player = helper.makeMockServerPlayer(GameType.SURVIVAL);
		ItemStack mask = new ItemStack(ModItems.PALE_MASK);
		player.setItemSlot(EquipmentSlot.HEAD, mask);
		// Two gazes at once wear it no faster than one.
		helper.onEachTick(() -> {
			Petrification.expose(player, Petrification.FULL / 4);
			Petrification.expose(player, Petrification.FULL / 4);
		});

		helper.runAfterDelay(ElderwoodGaze.TICKS_PER_DURABILITY - 5, () -> {
			helper.assertValueEqual(Petrification.getMeter(player), 0, "meter");
			helper.assertValueEqual(mask.getDamageValue(), 0, "mask damage just short of a second of gaze");
		});
		helper.runAfterDelay(ElderwoodGaze.TICKS_PER_DURABILITY + 5, () -> {
			helper.assertValueEqual(Petrification.getMeter(player), 0, "meter");
			helper.assertValueEqual(mask.getDamageValue(), 1, "mask damage after a second of gaze");
			helper.succeed();
		});
	}

	@GameTest(maxTicks = 100)
	public void meterDrainsOnceTheGazeBreaks(GameTestHelper helper) {
		Player player = helper.makeMockServerPlayer(GameType.SURVIVAL);
		double baseSpeed = player.getAttributeValue(Attributes.MOVEMENT_SPEED);
		Petrification.expose(player, Petrification.FULL / 2);
		tickEachTick(helper, player);

		helper.succeedWhen(() -> {
			helper.assertValueEqual(Petrification.getMeter(player), 0, "meter");
			helper.assertValueEqual(player.getAttributeValue(Attributes.MOVEMENT_SPEED), baseSpeed, "movement speed");
		});
	}

	// A plain mock player in the level ticks, which raising a shield needs.
	@GameTest(maxTicks = 40)
	public void aShieldRaisedTowardsTheGazeBlocksIt(GameTestHelper helper) {
		Player player = helper.makeMockPlayer(GameType.SURVIVAL);
		player.snapTo(helper.absoluteVec(new Vec3(3.5, 1.0, 3.5)), 0.0F, 0.0F);
		helper.getLevel().addFreshEntity(player);
		player.setItemInHand(InteractionHand.OFF_HAND, new ItemStack(Items.SHIELD));
		player.startUsingItem(InteractionHand.OFF_HAND);
		// It looks south, so a gaze from the south meets the shield and one from the north doesn't.
		Vec3 inFront = player.getEyePosition().add(0.0, 2.0, 6.0);
		Vec3 behind = player.getEyePosition().add(0.0, 2.0, -6.0);

		helper.runAfterDelay(10, () -> {
			helper.assertTrue(player.isBlocking(), "The shield should be up");
			Petrification.expose(player, 10, inFront);
			helper.assertValueEqual(Petrification.getMeter(player), 0, "meter behind the shield");
			Petrification.expose(player, 10, behind);
			helper.assertValueEqual(Petrification.getMeter(player), 10, "meter with the gaze at its back");
			helper.succeed();
		});
	}

	@GameTest(maxTicks = Petrification.PETRIFIED_TICKS + Petrification.IMMUNE_TICKS + 40)
	public void petrifiedPlayerCracksFreeAndGetsABreather(GameTestHelper helper) {
		Player player = helper.makeMockServerPlayer(GameType.SURVIVAL);
		Petrification.expose(player, Petrification.FULL);
		tickEachTick(helper, player);

		helper.startSequence()
				.thenExecuteAfter(Petrification.PETRIFIED_TICKS - 5, () ->
						helper.assertTrue(Petrification.isPetrified(player), "still petrified even without a gaze"))
				.thenWaitUntil(() -> helper.assertFalse(Petrification.isPetrified(player), "Player should crack free"))
				.thenExecute(() -> {
					helper.assertValueEqual(Petrification.getMeter(player), 0, "meter after cracking free");
					Petrification.expose(player, Petrification.FULL / 2);
					helper.assertValueEqual(Petrification.getMeter(player), 0, "meter under a gaze right after cracking free");
				})
				.thenExecuteAfter(Petrification.IMMUNE_TICKS, () -> {
					Petrification.expose(player, Petrification.FULL / 2);
					helper.assertValueEqual(Petrification.getMeter(player), Petrification.FULL / 2, "meter once the breather is over");
				})
				.thenSucceed();
	}

	// Mock players aren't in the level's player list, which the server tick goes through.
	private static void tickEachTick(GameTestHelper helper, Player player) {
		helper.onEachTick(() -> Petrification.tick(player));
	}

	@GameTest
	public void creativePlayersAreNotPetrified(GameTestHelper helper) {
		Player player = helper.makeMockServerPlayer(GameType.CREATIVE);

		Petrification.expose(player, Petrification.FULL);

		helper.assertValueEqual(Petrification.getMeter(player), 0, "meter");
		helper.succeed();
	}
}
