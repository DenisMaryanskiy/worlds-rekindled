package com.dmaryanskiy.vanillareforged.test;

import com.dmaryanskiy.vanillareforged.ModItems;
import com.dmaryanskiy.vanillareforged.WatcherGaze;

import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;

public class WatcherGazeTest {
	@GameTest
	public void hostileMobInCrosshairGetsSlownessTwo(GameTestHelper helper) {
		Mob zombie = spawnPinned(helper, EntityTypes.ZOMBIE);
		Player player = maskedPlayerLookingAt(helper, zombie);

		WatcherGaze.tick(player);

		assertSlowness(helper, zombie, 1);
		helper.succeed();
	}

	@GameTest
	public void passiveMobIsNotSlowed(GameTestHelper helper) {
		Mob cow = spawnPinned(helper, EntityTypes.COW);
		Player player = maskedPlayerLookingAt(helper, cow);

		WatcherGaze.tick(player);

		helper.assertFalse(cow.hasEffect(MobEffects.SLOWNESS), "Passive mobs shouldn't be slowed");
		helper.succeed();
	}

	@GameTest
	public void wallBlocksTheGaze(GameTestHelper helper) {
		Mob zombie = spawnPinned(helper, EntityTypes.ZOMBIE);
		Player player = maskedPlayerLookingAt(helper, zombie);
		for (int y = 1; y <= 3; y++) {
			helper.setBlock(new BlockPos(3, y, 3), Blocks.STONE);
		}

		WatcherGaze.tick(player);

		helper.assertFalse(zombie.hasEffect(MobEffects.SLOWNESS), "A wall should block the gaze");
		helper.succeed();
	}

	@GameTest
	public void noMaskNoGaze(GameTestHelper helper) {
		Mob zombie = spawnPinned(helper, EntityTypes.ZOMBIE);
		Player player = maskedPlayerLookingAt(helper, zombie);
		player.setItemSlot(EquipmentSlot.HEAD, ItemStack.EMPTY);

		WatcherGaze.tick(player);

		helper.assertFalse(zombie.hasEffect(MobEffects.SLOWNESS), "Gaze needs the Pale Mask on the head");
		helper.succeed();
	}

	@GameTest
	public void eachSecondOfGazeCostsOneDurability(GameTestHelper helper) {
		Mob zombie = spawnPinned(helper, EntityTypes.ZOMBIE);
		Player player = maskedPlayerLookingAt(helper, zombie);
		ItemStack mask = player.getItemBySlot(EquipmentSlot.HEAD);

		for (int i = 1; i < WatcherGaze.TICKS_PER_DURABILITY; i++) {
			WatcherGaze.tick(player);
		}
		helper.assertValueEqual(mask.getDamageValue(), 0, "damage just before a full second");
		WatcherGaze.tick(player);
		helper.assertValueEqual(mask.getDamageValue(), 1, "damage after a full second");
		helper.succeed();
	}

	@GameTest
	public void bossGetsWeakerSlowness(GameTestHelper helper) {
		Mob warden = spawnPinned(helper, EntityTypes.WARDEN);
		Player player = maskedPlayerLookingAt(helper, warden);

		WatcherGaze.tick(player);

		assertSlowness(helper, warden, 0);
		helper.succeed();
	}

	@GameTest
	public void witherIsSlowedByDragSinceItIgnoresEffects(GameTestHelper helper) {
		Mob wither = spawnPinned(helper, EntityTypes.WITHER);
		Player player = maskedPlayerLookingAt(helper, wither);
		wither.setDeltaMovement(1.0, 0.5, 1.0);

		WatcherGaze.tick(player);

		helper.assertValueEqual(wither.getDeltaMovement(), new Vec3(WatcherGaze.BOSS_DRAG, 0.5, WatcherGaze.BOSS_DRAG),
				"wither velocity after drag");
		helper.succeed();
	}

	private static Mob spawnPinned(GameTestHelper helper, EntityType<? extends Mob> type) {
		Mob mob = helper.spawn(type, new Vec3(5.5, 1.0, 3.5));
		mob.setNoAi(true);
		return mob;
	}

	private static Player maskedPlayerLookingAt(GameTestHelper helper, Mob target) {
		Player player = helper.makeMockServerPlayer(GameType.SURVIVAL);
		player.setPos(helper.absoluteVec(new Vec3(0.5, 1.0, 3.5)));
		player.setItemSlot(EquipmentSlot.HEAD, new ItemStack(ModItems.PALE_MASK));
		// Rotate directly: ServerPlayer.lookAt sends a packet, and mock players have no connection.
		Vec3 delta = target.getBoundingBox().getCenter().subtract(player.getEyePosition());
		float yaw = (float) Math.toDegrees(Math.atan2(delta.z, delta.x)) - 90.0F;
		player.setYRot(yaw);
		player.setYHeadRot(yaw);
		player.setXRot((float) -Math.toDegrees(Math.atan2(delta.y, delta.horizontalDistance())));
		return player;
	}

	private static void assertSlowness(GameTestHelper helper, Mob mob, int amplifier) {
		MobEffectInstance slowness = mob.getEffect(MobEffects.SLOWNESS);
		helper.assertTrue(slowness != null, mob.getType().getDescription().getString() + " should be slowed");
		helper.assertValueEqual(slowness.getAmplifier(), amplifier, "slowness amplifier");
	}
}
