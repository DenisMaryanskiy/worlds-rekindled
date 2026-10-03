package com.dmaryanskiy.vanillareforged.test;

import com.dmaryanskiy.vanillareforged.ModItems;

import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.sensing.Sensor;
import net.minecraft.world.entity.monster.creaking.Creaking;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameType;
import net.minecraft.world.phys.Vec3;

public class PaleMaskStealthTest {
	@GameTest
	public void creakingIgnoresMaskedPlayer(GameTestHelper helper) {
		Creaking creaking = spawnCreaking(helper);
		Player player = player(helper, true);

		helper.assertFalse(creaking.canAttack(player), "Creaking shouldn't attack a Pale Mask wearer");
		helper.assertFalse(Sensor.isEntityAttackable(helper.getLevel(), creaking, player),
				"Creaking's sensor shouldn't see a Pale Mask wearer as attackable");
		helper.succeed();
	}

	@GameTest
	public void creakingStillHuntsUnmaskedPlayer(GameTestHelper helper) {
		Creaking creaking = spawnCreaking(helper);
		Player player = player(helper, false);

		helper.assertTrue(creaking.canAttack(player), "Creaking should attack a player without the mask");
		helper.assertTrue(Sensor.isEntityAttackable(helper.getLevel(), creaking, player),
				"Creaking's sensor should see a player without the mask as attackable");
		helper.succeed();
	}

	@GameTest
	public void attackingRevealsOnlyTheAttacker(GameTestHelper helper) {
		Creaking creaking = spawnCreaking(helper);
		Player attacker = player(helper, true);
		Player bystander = player(helper, true);

		creaking.hurtServer(helper.getLevel(), helper.getLevel().damageSources().playerAttack(attacker), 1.0F);

		helper.assertTrue(creaking.canAttack(attacker), "Attacking a Creaking should reveal the wearer to it");
		helper.assertFalse(creaking.canAttack(bystander), "Other wearers should stay hidden");
		helper.succeed();
	}

	@GameTest
	public void revealIsPerCreaking(GameTestHelper helper) {
		Creaking hit = spawnCreaking(helper);
		Creaking other = spawnCreaking(helper);
		Player attacker = player(helper, true);

		hit.hurtServer(helper.getLevel(), helper.getLevel().damageSources().playerAttack(attacker), 1.0F);

		helper.assertFalse(other.canAttack(attacker), "Only the Creaking that was hit should see the wearer");
		helper.succeed();
	}

	private static Creaking spawnCreaking(GameTestHelper helper) {
		Creaking creaking = helper.spawn(EntityTypes.CREAKING, new Vec3(5.5, 1.0, 3.5));
		creaking.setNoAi(true);
		return creaking;
	}

	private static Player player(GameTestHelper helper, boolean masked) {
		Player player = helper.makeMockServerPlayer(GameType.SURVIVAL);
		player.setPos(helper.absoluteVec(new Vec3(2.5, 1.0, 3.5)));
		if (masked) {
			player.setItemSlot(EquipmentSlot.HEAD, new ItemStack(ModItems.PALE_MASK));
		}
		return player;
	}
}
