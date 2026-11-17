package com.dmaryanskiy.vanillareforged.test;

import java.util.List;

import com.dmaryanskiy.vanillareforged.ModEntities;
import com.dmaryanskiy.vanillareforged.elderwood.Elderwood;
import com.dmaryanskiy.vanillareforged.elderwood.ElderwoodHeart;
import com.dmaryanskiy.vanillareforged.sentry.AbstractSentry;
import com.dmaryanskiy.vanillareforged.sentry.HeartwoodSentry;
import com.dmaryanskiy.vanillareforged.sentry.RuinSentry;
import com.dmaryanskiy.vanillareforged.sentry.SentryHeart;

import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.gametest.framework.GameTestSequence;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.skeleton.Skeleton;
import net.minecraft.world.entity.monster.zombie.Zombie;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.arrow.Arrow;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.phys.Vec3;

public class SentryHeartTest {
	private static final String NOON = "vanillareforged-test:noon";
	private static final String MIDNIGHT = "vanillareforged-test:midnight";
	private static final Vec3 SENTRY = new Vec3(1.5, 1.0, 1.5);
	private static final double HEART_Y = SENTRY.y + 1.53;
	private static final double PLAYER_EYE_HEIGHT = 1.62;
	// The Sentry faces south (+z), so the heart is on its +z side.
	private static final Vec3 IN_FRONT = new Vec3(1.5, HEART_Y - PLAYER_EYE_HEIGHT, 4.0);
	private static final Vec3 BEHIND = new Vec3(1.5, HEART_Y - PLAYER_EYE_HEIGHT, -1.0);

	@GameTest(environment = MIDNIGHT)
	public void fullSwordSwingOnTheHeartCracksIt(GameTestHelper helper) {
		HeartwoodSentry sentry = sentry(helper);
		Player player = swordsman(helper, IN_FRONT, 180.0F);

		boolean hurt = sentry.hurtServer(helper.getLevel(), helper.getLevel().damageSources().playerAttack(player), fullDamage(player));

		helper.assertTrue(hurt, "A sword hit on the heart should land");
		helper.assertValueEqual(sentry.getCracks(), 1, "cracks");
		helper.assertValueEqual(sentry.getHealth(), sentry.getMaxHealth(), "health");
		helper.succeed();
	}

	@GameTest(environment = MIDNIGHT)
	public void swordHitFromBehindMissesTheHeart(GameTestHelper helper) {
		HeartwoodSentry sentry = sentry(helper);
		Player player = swordsman(helper, BEHIND, 0.0F);

		sentry.hurtServer(helper.getLevel(), helper.getLevel().damageSources().playerAttack(player), fullDamage(player));

		helper.assertValueEqual(sentry.getCracks(), 0, "cracks");
		helper.succeed();
	}

	@GameTest(environment = MIDNIGHT)
	public void aQuickSwordSwingCracksToo(GameTestHelper helper) {
		HeartwoodSentry sentry = sentry(helper);
		Player player = swordsman(helper, IN_FRONT, 180.0F);

		sentry.hurtServer(helper.getLevel(), helper.getLevel().damageSources().playerAttack(player), 0.2F * fullDamage(player));

		helper.assertValueEqual(sentry.getCracks(), 1, "cracks");
		helper.succeed();
	}

	@GameTest(environment = MIDNIGHT)
	public void bareHandCracksTheHeart(GameTestHelper helper) {
		HeartwoodSentry sentry = sentry(helper);
		Player player = swordsman(helper, IN_FRONT, 180.0F);
		player.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);

		sentry.hurtServer(helper.getLevel(), helper.getLevel().damageSources().playerAttack(player), fullDamage(player));

		helper.assertValueEqual(sentry.getCracks(), 1, "cracks");
		helper.succeed();
	}

	@GameTest(environment = MIDNIGHT)
	public void anyItemOrBareHandCanCrackEveryMobsHeart(GameTestHelper helper) {
		List<EntityType<? extends Mob>> types = List.of(ModEntities.HEARTWOOD_SENTRY, ModEntities.RUIN_SENTRY, ModEntities.ELDERWOOD);
		List<ItemStack> heldItems = List.of(new ItemStack(Items.IRON_AXE), new ItemStack(Items.IRON_PICKAXE),
				new ItemStack(Items.IRON_SHOVEL), new ItemStack(Items.IRON_HOE), new ItemStack(Items.STICK), ItemStack.EMPTY);
		for (EntityType<? extends Mob> type : types) {
			for (ItemStack held : heldItems) {
				Mob mob = helper.spawn(type, SENTRY);
				mob.setNoAi(true);
				mob.setYRot(0.0F);
				mob.setYHeadRot(0.0F);
				mob.setYBodyRot(0.0F);
				if (mob instanceof RuinSentry ruin) {
					ruin.setPost(mob.blockPosition());
				}
				Vec3 heart = mob instanceof Elderwood elderwood ? ElderwoodHeart.TRUNK_FRONT.box(elderwood).getCenter()
						: SentryHeart.box((AbstractSentry) mob).getCenter();
				Player player = helper.makeMockServerPlayer(GameType.SURVIVAL);
				player.snapTo(heart.add(0.0, -player.getEyeHeight(), 2.0), 180.0F, 0.0F);
				player.setYHeadRot(180.0F);
				player.setItemInHand(InteractionHand.MAIN_HAND, held.copy());
				player.attack(mob);
				int cracks = mob instanceof Elderwood elderwood ? elderwood.getCracks(ElderwoodHeart.TRUNK_FRONT)
						: ((AbstractSentry) mob).getCracks();
				helper.assertValueEqual(cracks, 1, type + " cracks after attacking with " + held);
				player.attack(mob);
				int repeatedCracks = mob instanceof Elderwood elderwood ? elderwood.getCracks(ElderwoodHeart.TRUNK_FRONT)
						: ((AbstractSentry) mob).getCracks();
				helper.assertValueEqual(repeatedCracks, 1, type + " must still enforce its crack cooldown with " + held);
				mob.discard();
			}
		}
		helper.succeed();
	}

	@GameTest(environment = MIDNIGHT, maxTicks = 30)
	public void changingFromAnAxeToAnEmptyHandWaitsForTheCrackCooldown(GameTestHelper helper) {
		HeartwoodSentry sentry = sentry(helper);
		Player player = swordsman(helper, IN_FRONT, 180.0F);
		player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.IRON_AXE));
		player.attack(sentry);
		helper.assertValueEqual(sentry.getCracks(), 1, "axe cracks");
		player.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
		player.attack(sentry);
		helper.assertValueEqual(sentry.getCracks(), 1, "changing held items cannot bypass the cooldown");
		helper.runAfterDelay(15, () -> {
			player.attack(sentry);
			helper.assertValueEqual(sentry.getCracks(), 2, "an empty hand cracks it again after the cooldown");
			helper.succeed();
		});
	}

	@GameTest(environment = MIDNIGHT, maxTicks = 40)
	public void chargedArrowOnTheHeartCracksIt(GameTestHelper helper) {
		HeartwoodSentry sentry = sentry(helper);
		shoot(helper, HEART_Y + 0.1, true);

		helper.succeedWhen(() -> helper.assertValueEqual(sentry.getCracks(), 1, "cracks"));
	}

	@GameTest(environment = MIDNIGHT, maxTicks = 40)
	public void unchargedArrowBouncesOff(GameTestHelper helper) {
		HeartwoodSentry sentry = sentry(helper);
		shoot(helper, HEART_Y + 0.1, false);

		helper.runAfterDelay(20, () -> {
			helper.assertValueEqual(sentry.getCracks(), 0, "cracks");
			helper.succeed();
		});
	}

	@GameTest(environment = MIDNIGHT, maxTicks = 40)
	public void chargedArrowInTheLegsBouncesOff(GameTestHelper helper) {
		HeartwoodSentry sentry = sentry(helper);
		shoot(helper, SENTRY.y + 0.3, true);

		helper.runAfterDelay(20, () -> {
			helper.assertValueEqual(sentry.getCracks(), 0, "cracks");
			helper.succeed();
		});
	}

	@GameTest(environment = MIDNIGHT, maxTicks = 120)
	public void lastCrackDestroysTheSentry(GameTestHelper helper) {
		HeartwoodSentry sentry = sentry(helper);
		Player player = swordsman(helper, IN_FRONT, 180.0F);
		Runnable hit = () -> sentry.hurtServer(helper.getLevel(), helper.getLevel().damageSources().playerAttack(player), fullDamage(player));

		GameTestSequence sequence = helper.startSequence().thenExecute(hit);
		for (int i = 1; i < HeartwoodSentry.MAX_CRACKS; i++) {
			sequence.thenExecute(() -> helper.assertTrue(sentry.isAlive(), "Sentry should survive until the last crack"))
					.thenExecuteAfter(15, hit);
		}
		sequence.thenExecute(() -> {
			helper.assertFalse(sentry.isAlive(), "Sentry should collapse on the last crack");
			helper.assertItemEntityPresent(Items.RESIN_CLUMP);
		}).thenSucceed();
	}

	@GameTest(environment = MIDNIGHT, maxTicks = 40)
	public void skeletonArrowOnTheHeartCracksIt(GameTestHelper helper) {
		HeartwoodSentry sentry = sentry(helper);
		Skeleton skeleton = helper.spawn(EntityTypes.SKELETON, new Vec3(1.5, 1.0, 7.0));
		skeleton.setNoAi(true);
		shoot(helper, skeleton, HEART_Y + 0.1, false);

		helper.succeedWhen(() -> {
			helper.assertValueEqual(sentry.getCracks(), 1, "cracks");
			helper.assertTrue(sentry.getTarget() == skeleton, "Sentry should turn on the skeleton that shot it");
		});
	}

	@GameTest(environment = MIDNIGHT)
	public void zombieMeleeDoesNotCrack(GameTestHelper helper) {
		HeartwoodSentry sentry = sentry(helper);
		Zombie zombie = helper.spawn(EntityTypes.ZOMBIE, IN_FRONT);
		zombie.setNoAi(true);

		sentry.hurtServer(helper.getLevel(), helper.getLevel().damageSources().mobAttack(zombie), 10.0F);

		helper.assertValueEqual(sentry.getCracks(), 0, "cracks");
		helper.succeed();
	}

	@GameTest(environment = MIDNIGHT)
	public void hitsDuringTheCrackCooldownDoNotCount(GameTestHelper helper) {
		HeartwoodSentry sentry = sentry(helper);
		Player player = swordsman(helper, IN_FRONT, 180.0F);

		for (int i = 0; i < 3; i++) {
			sentry.hurtServer(helper.getLevel(), helper.getLevel().damageSources().playerAttack(player), fullDamage(player));
		}

		helper.assertValueEqual(sentry.getCracks(), 1, "cracks");
		helper.succeed();
	}

	@GameTest(environment = NOON, skyAccess = true, maxTicks = 100)
	public void turningIntoAStatueMendsTheHeart(GameTestHelper helper) {
		HeartwoodSentry sentry = sentry(helper);
		sentry.setCracks(2);

		helper.succeedWhen(() -> {
			helper.assertTrue(sentry.isStatue(), "Sentry should be a statue");
			helper.assertValueEqual(sentry.getCracks(), 0, "cracks");
		});
	}

	@GameTest(environment = MIDNIGHT, maxTicks = HeartwoodSentry.HEAL_INTERVAL_TICKS + 100)
	public void doesNotHealWhileFighting(GameTestHelper helper) {
		HeartwoodSentry sentry = sentry(helper);
		Zombie zombie = helper.spawn(EntityTypes.ZOMBIE, new Vec3(4.5, 1.0, 4.5));
		zombie.setNoAi(true);
		sentry.setTarget(zombie);
		sentry.setCracks(1);

		helper.runAfterDelay(HeartwoodSentry.HEAL_INTERVAL_TICKS + 20, () -> {
			helper.assertValueEqual(sentry.getCracks(), 1, "cracks while fighting");
			helper.succeed();
		});
	}

	// Mock players of neighboring tests within 16 blocks pause the healing, so allow extra time.
	@GameTest(environment = MIDNIGHT, maxTicks = HeartwoodSentry.HEAL_INTERVAL_TICKS * 2)
	public void healsACrackWithNoPlayerAround(GameTestHelper helper) {
		HeartwoodSentry sentry = sentry(helper);
		sentry.setCracks(2);

		helper.succeedWhen(() -> helper.assertValueEqual(sentry.getCracks(), 1, "cracks"));
	}

	private static HeartwoodSentry sentry(GameTestHelper helper) {
		HeartwoodSentry sentry = helper.spawn(ModEntities.HEARTWOOD_SENTRY, SENTRY);
		sentry.setPedestal(helper.absolutePos(BlockPos.containing(SENTRY).below()));
		sentry.setNoAi(true);
		sentry.setYRot(0.0F);
		sentry.setYHeadRot(0.0F);
		sentry.setYBodyRot(0.0F);
		return sentry;
	}

	private static Player swordsman(GameTestHelper helper, Vec3 pos, float yRot) {
		Player player = helper.makeMockServerPlayer(GameType.SURVIVAL);
		player.snapTo(helper.absoluteVec(pos), yRot, 0.0F);
		player.setYHeadRot(yRot);
		player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.IRON_SWORD));
		return player;
	}

	private static float fullDamage(Player player) {
		return (float) player.getAttributeValue(Attributes.ATTACK_DAMAGE);
	}

	private static void shoot(GameTestHelper helper, double y, boolean charged) {
		shoot(helper, swordsman(helper, new Vec3(1.5, 1.0, 7.0), 180.0F), y, charged);
	}

	private static void shoot(GameTestHelper helper, Entity archer, double y, boolean charged) {
		Arrow arrow = helper.spawn(EntityTypes.ARROW, new Vec3(1.5, y, 5.0));
		arrow.setOwner(archer);
		arrow.setCritArrow(charged);
		arrow.shoot(0.0, 0.0, -1.0, 3.0F, 0.0F);
	}
}
