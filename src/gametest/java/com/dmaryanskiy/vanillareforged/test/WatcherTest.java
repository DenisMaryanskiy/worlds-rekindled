package com.dmaryanskiy.vanillareforged.test;

import java.util.List;

import com.dmaryanskiy.vanillareforged.ModEntities;
import com.dmaryanskiy.vanillareforged.ModItems;
import com.dmaryanskiy.vanillareforged.petrification.Petrification;
import com.dmaryanskiy.vanillareforged.watcher.Watcher;

import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.entity.player.Player;
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
	public void playersCannotHurtItYet(GameTestHelper helper) {
		Watcher watcher = helper.spawn(ModEntities.WATCHER, new Vec3(1.5, 1.0, 1.5));
		Player player = helper.makeMockServerPlayer(GameType.SURVIVAL);

		boolean hurt = watcher.hurtServer(helper.getLevel(), helper.getLevel().damageSources().playerAttack(player), 20.0F);

		helper.assertFalse(hurt, "Only its hearts will be able to hurt it");
		helper.succeed();
	}

	@GameTest(environment = MIDNIGHT)
	public void itDropsItsLens(GameTestHelper helper) {
		Watcher watcher = helper.spawn(ModEntities.WATCHER, new Vec3(1.5, 1.0, 1.5));

		watcher.kill(helper.getLevel());

		helper.assertItemEntityPresent(ModItems.WATCHERS_LENS);
		helper.succeed();
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
