package com.dmaryanskiy.vanillareforged.test;

import com.dmaryanskiy.vanillareforged.ModEntities;
import com.dmaryanskiy.vanillareforged.ModItems;
import com.dmaryanskiy.vanillareforged.PaleMaskStealth;
import com.dmaryanskiy.vanillareforged.VanillaReforged;
import com.dmaryanskiy.vanillareforged.elderwood.Elderwood;
import com.dmaryanskiy.vanillareforged.elderwood.ElderwoodHeart;
import com.dmaryanskiy.vanillareforged.petrification.Petrification;

import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.advancements.AdvancementHolder;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.gametest.framework.GameTestSequence;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.monster.creaking.Creaking;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;

public class AdvancementTest {
	private static final int TICKS_BETWEEN_HITS = 12;
	private static final Vec3 CREAKING = new Vec3(5.5, 1.0, 3.5);

	@GameTest(environment = ElderwoodTest.MIDNIGHT, skyAccess = true)
	// The summon looks for players in the level, and this is the only mock player that's in it. Vanilla
	// has it marked for removal without a replacement so far.
	@SuppressWarnings("removal")
	public void wakingTheElderwoodGrantsOldGrowth(GameTestHelper helper) {
		ServerPlayer player = helper.makeMockServerPlayerInLevel();
		try {
			player.snapTo(helper.absoluteVec(ElderwoodTest.ELDERWOOD.add(0.0, 0.0, 3.0)), 0.0F, 0.0F);
			ElderwoodTest.buildStump(helper, Blocks.COPPER_BLOCK.weathering().weathered());
			helper.assertFalse(has(helper, player, "old_growth"), "Old Growth shouldn't be granted before the tree wakes");

			helper.setBlock(ElderwoodTest.HEART, Blocks.CREAKING_HEART);

			helper.assertTrue(has(helper, player, "old_growth"), "Waking the Elderwood should grant Old Growth");
		} finally {
			helper.getLevel().getServer().getPlayerList().remove(player);
		}
		helper.succeed();
	}

	@GameTest(maxTicks = Petrification.PETRIFIED_TICKS + 40)
	public void livingThroughPetrificationGrantsSetInStone(GameTestHelper helper) {
		Player player = helper.makeMockServerPlayer(GameType.SURVIVAL);
		Petrification.expose(player, Petrification.FULL);
		// Mock players aren't in the level's player list, which the server tick goes through.
		helper.onEachTick(() -> Petrification.tick(player));

		helper.assertFalse(has(helper, player, "set_in_stone"), "Set in Stone shouldn't be granted while still petrified");
		helper.succeedWhen(() -> helper.assertTrue(has(helper, player, "set_in_stone"), "Cracking free should grant Set in Stone"));
	}

	@GameTest
	public void aGazeThatNeverTurnsThePlayerToStoneGrantsNothing(GameTestHelper helper) {
		Player player = helper.makeMockServerPlayer(GameType.SURVIVAL);
		Petrification.expose(player, Petrification.FULL / 2);

		Petrification.shatter(player);

		helper.assertFalse(has(helper, player, "set_in_stone"), "Set in Stone needs the meter to fill");
		helper.succeed();
	}

	@GameTest(environment = ElderwoodTest.MIDNIGHT, maxTicks = 100)
	public void killingTheElderwoodUnpetrifiedGrantsDeadwoodAndDontBlink(GameTestHelper helper) {
		Elderwood elderwood = ElderwoodTest.stillElderwood(helper);
		Player player = helper.makeMockServerPlayer(GameType.SURVIVAL);

		kill(helper, helper.startSequence(), elderwood, player)
				.thenExecute(() -> {
					helper.assertFalse(elderwood.isAlive(), "The Elderwood should be dead");
					helper.assertTrue(has(helper, player, "deadwood"), "Killing the Elderwood should grant Deadwood");
					helper.assertTrue(has(helper, player, "dont_blink"), "Killing it without being petrified should grant Don't Blink");
					helper.assertTrue(hasVanilla(helper, player, "adventure/kill_a_mob"), "Killing the Elderwood should grant Monster Hunter");
				})
				.thenSucceed();
	}

	@GameTest(environment = ElderwoodTest.MIDNIGHT, maxTicks = 220)
	public void killingTheElderwoodAfterItPetrifiedThePlayerGrantsOnlyDeadwood(GameTestHelper helper) {
		// The test structure floats, and this takes a few seconds: without a floor the tree would fall out of it.
		for (int x = 0; x < 8; x++) {
			for (int z = 0; z < 8; z++) {
				helper.setBlock(new BlockPos(x, 0, z), Blocks.STONE);
			}
		}
		Elderwood elderwood = helper.spawn(ModEntities.ELDERWOOD, new Vec3(1.5, 1.0, 1.5));
		// Out of melee reach: a mock player can't take hits (it has no connection).
		Player player = helper.makeMockServerPlayer(GameType.SURVIVAL);
		player.snapTo(helper.absoluteVec(new Vec3(6.5, 1.0, 6.5)), 0.0F, 0.0F);
		elderwood.setTarget(player);

		GameTestSequence sequence = helper.startSequence()
				.thenWaitUntil(() -> helper.assertTrue(elderwood.hasPetrified(player), "Its gaze should petrify the player"))
				.thenExecute(() -> elderwood.setNoAi(true));
		kill(helper, sequence, elderwood, player)
				.thenExecute(() -> {
					helper.assertFalse(elderwood.isAlive(), "The Elderwood should be dead");
					helper.assertTrue(has(helper, player, "deadwood"), "Killing the Elderwood should grant Deadwood");
					helper.assertFalse(has(helper, player, "dont_blink"), "Don't Blink is lost once the tree has petrified the player");
				})
				.thenSucceed();
	}

	@GameTest
	public void aCreakingIgnoringAMaskedPlayerNextToItGrantsOneOfThem(GameTestHelper helper) {
		Creaking creaking = spawnCreaking(helper);
		Player player = playerNear(helper, PaleMaskStealth.NEXT_TO_RANGE - 1.0);
		player.setItemSlot(EquipmentSlot.HEAD, new ItemStack(ModItems.PALE_MASK));

		creaking.canAttack(player);

		helper.assertTrue(has(helper, player, "one_of_them"), "Being ignored up close should grant One of Them");
		helper.succeed();
	}

	@GameTest
	public void oneOfThemNeedsTheMaskAndToBeClose(GameTestHelper helper) {
		Creaking creaking = spawnCreaking(helper);
		Player unmasked = playerNear(helper, PaleMaskStealth.NEXT_TO_RANGE - 1.0);
		Player distant = playerNear(helper, PaleMaskStealth.NEXT_TO_RANGE + 1.0);
		distant.setItemSlot(EquipmentSlot.HEAD, new ItemStack(ModItems.PALE_MASK));

		creaking.canAttack(unmasked);
		creaking.canAttack(distant);

		helper.assertFalse(has(helper, unmasked, "one_of_them"), "A player without the mask isn't ignored");
		helper.assertFalse(has(helper, distant, "one_of_them"), "A wearer far from the Creaking isn't next to it");
		helper.succeed();
	}

	@GameTest
	// The builder credits the players around it in the level, and this is the only mock player that's
	// in it. Vanilla has it marked for removal without a replacement so far.
	@SuppressWarnings("removal")
	public void buildingAHeartwoodSentryGrantsHeartwoodGuardian(GameTestHelper helper) {
		ServerPlayer player = helper.makeMockServerPlayerInLevel();
		try {
			player.snapTo(helper.absoluteVec(new Vec3(3.5, 1.0, 1.5)), 0.0F, 0.0F);
			helper.setBlock(new BlockPos(1, 1, 1), Blocks.PALE_OAK_LOG);
			helper.setBlock(new BlockPos(1, 2, 1), Blocks.CREAKING_HEART);
			helper.assertFalse(has(helper, player, "heartwood_guardian"), "Heartwood Guardian shouldn't be granted before the Sentry is built");

			helper.setBlock(new BlockPos(1, 3, 1), Blocks.CARVED_PUMPKIN);

			helper.assertTrue(has(helper, player, "heartwood_guardian"), "Building a Heartwood Sentry should grant Heartwood Guardian");
		} finally {
			helper.getLevel().getServer().getPlayerList().remove(player);
		}
		helper.succeed();
	}

	@GameTest
	public void craftingANewLookCountsTheElderwoodTrim(GameTestHelper helper) {
		AdvancementHolder advancement = helper.getLevel().getServer().getAdvancements()
				.get(Identifier.withDefaultNamespace("adventure/trim_with_any_armor_pattern"));
		helper.assertTrue(advancement.value().criteria()
				.containsKey("armor_trimmed_vanillareforged:elderwood_armor_trim_smithing_template_smithing_trim"),
				"Crafting a New Look should count the Elderwood trim");
		helper.succeed();
	}

	/** Destroys every other heart at once, then the core with as many sword swings as it takes. */
	private static GameTestSequence kill(GameTestHelper helper, GameTestSequence sequence, Elderwood elderwood, Player killer) {
		Runnable hitCore = () -> ElderwoodTest.hitAs(helper, killer, elderwood, ElderwoodHeart.CORE, elderwood.getLookAngle(), 2.0, 1.0F);
		sequence.thenExecute(() -> {
			for (ElderwoodHeart heart : ElderwoodHeart.values()) {
				if (heart != ElderwoodHeart.CORE) {
					elderwood.setCracks(heart, heart.maxCracks());
				}
			}
			hitCore.run();
		});
		for (int i = 1; i < ElderwoodHeart.CORE.maxCracks(); i++) {
			sequence.thenExecuteAfter(TICKS_BETWEEN_HITS, hitCore);
		}
		return sequence;
	}

	private static Creaking spawnCreaking(GameTestHelper helper) {
		Creaking creaking = helper.spawn(EntityTypes.CREAKING, CREAKING);
		creaking.setNoAi(true);
		return creaking;
	}

	private static Player playerNear(GameTestHelper helper, double distance) {
		Player player = helper.makeMockServerPlayer(GameType.SURVIVAL);
		player.setPos(helper.absoluteVec(CREAKING.add(-distance, 0.0, 0.0)));
		return player;
	}

	private static boolean has(GameTestHelper helper, Player player, String name) {
		return isDone(helper, player, VanillaReforged.id("adventure/" + name));
	}

	static boolean hasVanilla(GameTestHelper helper, Player player, String name) {
		return isDone(helper, player, Identifier.withDefaultNamespace(name));
	}

	private static boolean isDone(GameTestHelper helper, Player player, Identifier id) {
		AdvancementHolder advancement = helper.getLevel().getServer().getAdvancements().get(id);
		helper.assertTrue(advancement != null, "Advancement " + id + " should exist");
		return ((ServerPlayer) player).getAdvancements().getOrStartProgress(advancement).isDone();
	}
}
