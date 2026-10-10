package com.dmaryanskiy.vanillareforged.test;

import com.dmaryanskiy.vanillareforged.ModItems;
import com.dmaryanskiy.vanillareforged.ElderwoodGaze;
import com.dmaryanskiy.vanillareforged.petrification.Petrification;

import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameType;

public class PetrificationTest {
	@GameTest
	public void gazeFillsTheMeterAndSlows(GameTestHelper helper) {
		Player player = helper.makeMockServerPlayer(GameType.SURVIVAL);
		double baseSpeed = player.getAttributeValue(Attributes.MOVEMENT_SPEED);

		Petrification.expose(player, Petrification.FULL / 2);

		helper.assertValueEqual(Petrification.getProgress(player), 0.5F, "progress");
		helper.assertValueEqual(player.getAttributeValue(Attributes.MOVEMENT_SPEED),
				baseSpeed * (1.0 - Petrification.MAX_SLOWDOWN * 0.5F), "movement speed");
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
	public void petrifiedPlayerIsBrittle(GameTestHelper helper) {
		// A plain mock player: a mock ServerPlayer has no connection, which hurtServer needs.
		Player player = helper.makeMockPlayer(GameType.SURVIVAL);
		Petrification.expose(player, Petrification.FULL);

		player.hurtServer(helper.getLevel(), helper.getLevel().damageSources().generic(), 4.0F);

		helper.assertValueEqual(player.getHealth(), player.getMaxHealth() - 4.0F * (1.0F + Petrification.MAX_EXTRA_DAMAGE), "health");
		helper.succeed();
	}

	@GameTest
	public void paleMaskBlocksTheGazeButWearsDown(GameTestHelper helper) {
		Player player = helper.makeMockServerPlayer(GameType.SURVIVAL);
		ItemStack mask = new ItemStack(ModItems.PALE_MASK);
		player.setItemSlot(EquipmentSlot.HEAD, mask);

		for (int i = 0; i < ElderwoodGaze.TICKS_PER_DURABILITY; i++) {
			Petrification.expose(player, Petrification.FULL / 4);
		}

		helper.assertValueEqual(Petrification.getMeter(player), 0, "meter");
		helper.assertValueEqual(mask.getDamageValue(), 1, "mask damage after a second of gaze");
		helper.succeed();
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

	@GameTest(maxTicks = Petrification.PETRIFIED_TICKS + 40)
	public void petrifiedPlayerCracksFreeAtHalf(GameTestHelper helper) {
		Player player = helper.makeMockServerPlayer(GameType.SURVIVAL);
		Petrification.expose(player, Petrification.FULL);
		tickEachTick(helper, player);

		helper.startSequence()
				.thenExecuteAfter(Petrification.PETRIFIED_TICKS - 5, () ->
						helper.assertTrue(Petrification.isPetrified(player), "still petrified even without a gaze"))
				.thenWaitUntil(() -> helper.assertFalse(Petrification.isPetrified(player), "Player should crack free"))
				.thenExecute(() -> helper.assertTrue(Petrification.getMeter(player) <= Petrification.FULL / 2,
						"Cracking free should leave the meter at most half full"))
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
