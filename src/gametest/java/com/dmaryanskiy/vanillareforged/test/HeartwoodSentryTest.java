package com.dmaryanskiy.vanillareforged.test;

import java.util.List;

import com.dmaryanskiy.vanillareforged.ModEntities;
import com.dmaryanskiy.vanillareforged.sentry.HeartwoodSentry;
import com.dmaryanskiy.vanillareforged.sentry.SentryLantern;

import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.animal.cow.Cow;
import net.minecraft.world.entity.monster.zombie.Zombie;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LightBlock;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

public class HeartwoodSentryTest {
	private static final BlockPos PEDESTAL = new BlockPos(1, 1, 1);
	private static final String NOON = "vanillareforged-test:noon";
	private static final String MIDNIGHT = "vanillareforged-test:midnight";

	@GameTest
	public void buildingSpawnsSentryAndKeepsLogAsPedestal(GameTestHelper helper) {
		HeartwoodSentry sentry = build(helper, Blocks.CARVED_PUMPKIN);

		helper.assertBlockPresent(Blocks.PALE_OAK_LOG, PEDESTAL);
		helper.assertBlockPresent(Blocks.AIR, PEDESTAL.above());
		helper.assertBlockPresent(Blocks.AIR, PEDESTAL.above(2));
		helper.assertValueEqual(sentry.getPedestal(), helper.absolutePos(PEDESTAL), "pedestal");
		helper.assertFalse(sentry.isJackOLantern(), "Carved pumpkin head");
		helper.succeed();
	}

	@GameTest
	// The builder credits the players around it in the level, and this is the only mock player that's
	// in it. Vanilla has it marked for removal without a replacement so far.
	@SuppressWarnings("removal")
	public void buildingOneNextToAPlayerGrantsHiredHelp(GameTestHelper helper) {
		ServerPlayer player = helper.makeMockServerPlayerInLevel();
		try {
			player.snapTo(helper.absoluteVec(Vec3.atBottomCenterOf(PEDESTAL).add(2.0, 0.0, 0.0)), 0.0F, 0.0F);

			build(helper, Blocks.CARVED_PUMPKIN);

			helper.assertTrue(AdvancementTest.hasVanilla(helper, player, "adventure/summon_iron_golem"), "Building a Sentry should grant Hired Help");
		} finally {
			helper.getLevel().getServer().getPlayerList().remove(player);
		}
		helper.succeed();
	}

	@GameTest
	public void jackOLanternHeadIsRemembered(GameTestHelper helper) {
		HeartwoodSentry sentry = build(helper, Blocks.JACK_O_LANTERN);

		helper.assertTrue(sentry.isJackOLantern(), "Jack o'lantern head");
		helper.succeed();
	}

	@GameTest
	public void wrongOrderBuildsNothing(GameTestHelper helper) {
		helper.setBlock(PEDESTAL, Blocks.CREAKING_HEART);
		helper.setBlock(PEDESTAL.above(), Blocks.PALE_OAK_LOG);
		helper.setBlock(PEDESTAL.above(2), Blocks.CARVED_PUMPKIN);

		helper.assertEntityNotPresent(ModEntities.HEARTWOOD_SENTRY);
		helper.succeed();
	}

	@GameTest
	public void mobsCannotDamageIt(GameTestHelper helper) {
		HeartwoodSentry sentry = build(helper, Blocks.CARVED_PUMPKIN);
		Zombie zombie = helper.spawn(EntityTypes.ZOMBIE, new Vec3(4.5, 1.0, 4.5));

		boolean hurt = sentry.hurtServer(helper.getLevel(), helper.getLevel().damageSources().mobAttack(zombie), 10.0F);

		helper.assertFalse(hurt, "Mob attacks shouldn't hurt the Sentry");
		helper.assertValueEqual(sentry.getHealth(), sentry.getMaxHealth(), "health");
		helper.succeed();
	}

	@GameTest
	public void attackingPlayerBecomesTarget(GameTestHelper helper) {
		HeartwoodSentry sentry = build(helper, Blocks.CARVED_PUMPKIN);
		Player player = helper.makeMockServerPlayer(GameType.SURVIVAL);

		sentry.hurtServer(helper.getLevel(), helper.getLevel().damageSources().playerAttack(player), 1.0F);

		helper.assertValueEqual(sentry.getTarget(), player, "target after a player hit it");
		helper.succeed();
	}

	// Generous timeout: on slow CI machines the night sky can lag behind the batch's clock change.
	@GameTest(environment = MIDNIGHT, skyAccess = true, maxTicks = 300)
	public void targetsNearbyHostileMobAtNight(GameTestHelper helper) {
		floor(helper);
		HeartwoodSentry sentry = build(helper, Blocks.CARVED_PUMPKIN);
		Zombie zombie = helper.spawn(EntityTypes.ZOMBIE, new Vec3(5.5, 1.0, 5.5));
		zombie.setNoAi(true);

		helper.startSequence()
				.thenWaitUntil(() -> helper.assertFalse(sentry.isStatue(), "Sentry should be awake at night"))
				.thenWaitUntil(() -> helper.assertTrue(sentry.getTarget() == zombie, "Sentry should target the zombie (statue="
						+ sentry.isStatue() + ", daylight=" + sentry.isDaylightAtPedestal() + ", distance=" + sentry.distanceTo(zombie) + ")"))
				.thenSucceed();
	}

	@GameTest
	public void attackDealsFlatDamageAndLaunches(GameTestHelper helper) {
		HeartwoodSentry sentry = build(helper, Blocks.CARVED_PUMPKIN);
		// A cow has no armor, so the full damage lands.
		Cow cow = helper.spawn(EntityTypes.COW, new Vec3(3.5, 1.0, 1.5));
		cow.setNoAi(true);
		float health = cow.getHealth();

		sentry.doHurtTarget(helper.getLevel(), cow);

		helper.assertValueEqual(cow.getHealth(), health - HeartwoodSentry.ATTACK_DAMAGE, "cow health");
		helper.assertTrue(cow.getDeltaMovement().y > 0.3, "Target should be launched upward");
		helper.succeed();
	}

	@GameTest(environment = NOON, skyAccess = true, maxTicks = 40)
	public void turnsIntoStatueInDaylight(GameTestHelper helper) {
		HeartwoodSentry sentry = build(helper, Blocks.CARVED_PUMPKIN);

		helper.succeedWhen(() -> {
			helper.assertTrue(sentry.isStatue(), "Sentry should be a statue in daylight");
			helper.assertTrue(sentry.isNoAi(), "Statue shouldn't run AI");
		});
	}

	// Sky access is read from skylight, which the light engine updates lazily, so allow it time.
	@GameTest(environment = NOON, skyAccess = true, maxTicks = 300)
	public void wakesUpWhenThePedestalLosesDaylight(GameTestHelper helper) {
		HeartwoodSentry sentry = build(helper, Blocks.CARVED_PUMPKIN);

		helper.startSequence()
				.thenWaitUntil(() -> helper.assertTrue(sentry.isStatue(), "statue first"))
				.thenExecute(() -> helper.setBlock(PEDESTAL.above(4), Blocks.STONE))
				.thenWaitUntil(() -> helper.assertFalse(sentry.isStatue(), "Sentry should wake without daylight"))
				.thenSucceed();
	}

	@GameTest(environment = NOON, skyAccess = true, maxTicks = 200)
	public void walksBackToPedestalAtDawn(GameTestHelper helper) {
		floor(helper);
		helper.setBlock(PEDESTAL, Blocks.PALE_OAK_LOG);
		HeartwoodSentry sentry = helper.spawn(ModEntities.HEARTWOOD_SENTRY, new Vec3(5.5, 1.0, 5.5));
		sentry.setPedestal(helper.absolutePos(PEDESTAL));

		helper.succeedWhen(() -> {
			helper.assertTrue(sentry.isStatue(), "Sentry should be a statue");
			helper.assertTrue(sentry.blockPosition().equals(helper.absolutePos(PEDESTAL.above())), "Statue should stand on its pedestal");
		});
	}

	@GameTest
	public void sneakingWithAnAxeTakesTheSentryApart(GameTestHelper helper) {
		HeartwoodSentry sentry = build(helper, Blocks.JACK_O_LANTERN);
		Player player = axePlayer(helper, true);

		sentry.interact(player, InteractionHand.MAIN_HAND, sentry.position());

		helper.assertTrue(sentry.isRemoved(), "Sentry should be taken apart");
		helper.assertItemEntityPresent(Items.CREAKING_HEART);
		helper.assertItemEntityPresent(Items.JACK_O_LANTERN);
		helper.assertBlockPresent(Blocks.PALE_OAK_LOG, PEDESTAL);
		helper.assertValueEqual(player.getMainHandItem().getDamageValue(), 1, "axe damage");
		helper.succeed();
	}

	@GameTest
	public void sentryHuntingThePlayerCannotBeTakenApart(GameTestHelper helper) {
		HeartwoodSentry sentry = build(helper, Blocks.CARVED_PUMPKIN);
		Player player = axePlayer(helper, true);
		sentry.setTarget(player);

		sentry.interact(player, InteractionHand.MAIN_HAND, sentry.position());

		helper.assertFalse(sentry.isRemoved(), "An angry Sentry has to be fought");
		helper.succeed();
	}

	@GameTest
	public void takingApartNeedsSneaking(GameTestHelper helper) {
		HeartwoodSentry sentry = build(helper, Blocks.CARVED_PUMPKIN);
		Player player = axePlayer(helper, false);

		sentry.interact(player, InteractionHand.MAIN_HAND, sentry.position());

		helper.assertFalse(sentry.isRemoved(), "Using an axe without sneaking shouldn't take it apart");
		helper.succeed();
	}

	@GameTest(environment = NOON, skyAccess = true, maxTicks = 40)
	public void choppingAStatueTakesItApart(GameTestHelper helper) {
		HeartwoodSentry sentry = build(helper, Blocks.CARVED_PUMPKIN);
		Player player = axePlayer(helper, false);

		helper.startSequence()
				.thenWaitUntil(() -> helper.assertTrue(sentry.isStatue(), "statue first"))
				.thenExecute(() -> {
					sentry.hurtServer(helper.getLevel(), helper.getLevel().damageSources().playerAttack(player), 9.0F);
					helper.assertTrue(sentry.isRemoved(), "An axe should take the statue apart");
					helper.assertItemEntityPresent(Items.CREAKING_HEART);
					helper.assertItemEntityPresent(Items.CARVED_PUMPKIN);
				})
				.thenSucceed();
	}

	// Sky access removes the test's barrier ceiling, which sits at head height.
	@GameTest(environment = MIDNIGHT, skyAccess = true, maxTicks = 40)
	public void jackOLanternHeadLightsUpAndCleansUp(GameTestHelper helper) {
		HeartwoodSentry sentry = build(helper, Blocks.JACK_O_LANTERN);
		sentry.setNoAi(true);
		BlockPos head = PEDESTAL.above(3);

		helper.startSequence()
				.thenWaitUntil(() -> {
					helper.assertBlockPresent(Blocks.LIGHT, head);
					helper.assertBlockProperty(head, LightBlock.LEVEL, SentryLantern.LIGHT_LEVEL);
				})
				.thenExecute(sentry::discard)
				.thenExecute(() -> helper.assertBlockPresent(Blocks.AIR, head))
				.thenSucceed();
	}

	@GameTest(environment = MIDNIGHT, skyAccess = true)
	public void carvedPumpkinHeadGivesNoLight(GameTestHelper helper) {
		HeartwoodSentry sentry = build(helper, Blocks.CARVED_PUMPKIN);
		sentry.setNoAi(true);
		BlockPos head = PEDESTAL.above(3);

		helper.runAfterDelay(5, () -> {
			helper.assertBlockPresent(Blocks.AIR, head);
			helper.succeed();
		});
	}

	private static Player axePlayer(GameTestHelper helper, boolean sneaking) {
		Player player = helper.makeMockServerPlayer(GameType.SURVIVAL);
		player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.IRON_AXE));
		player.setShiftKeyDown(sneaking);
		return player;
	}

	// The default test structure is empty and floats above the ground, so walking Sentries need a floor.
	private static void floor(GameTestHelper helper) {
		for (int x = 0; x < 8; x++) {
			for (int z = 0; z < 8; z++) {
				helper.setBlock(new BlockPos(x, 0, z), Blocks.STONE);
			}
		}
	}

	private static HeartwoodSentry build(GameTestHelper helper, Block head) {
		helper.setBlock(PEDESTAL, Blocks.PALE_OAK_LOG);
		helper.setBlock(PEDESTAL.above(), Blocks.CREAKING_HEART);
		helper.setBlock(PEDESTAL.above(2), head);
		List<HeartwoodSentry> sentries = helper.getLevel().getEntities(ModEntities.HEARTWOOD_SENTRY,
				new AABB(helper.absolutePos(PEDESTAL)).inflate(3.0), sentry -> true);
		helper.assertValueEqual(sentries.size(), 1, "sentries built");
		return sentries.getFirst();
	}
}
