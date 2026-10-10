package com.dmaryanskiy.vanillareforged.elderwood;

import org.jspecify.annotations.Nullable;

import com.dmaryanskiy.vanillareforged.ModEntities;

import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.material.PushReaction;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

/**
 * The part of the Elderwood that can be hit where a branch heart hangs. A branch heart moves far
 * outside the trunk's box, where neither a crosshair nor an arrow would find the tree (vanilla's
 * multipart entities only work for the Ender Dragon), so the tree keeps this invisible entity on
 * each one and takes the hits it passes on. It's never saved: the tree makes its own again.
 */
public class ElderwoodBranchHeart extends Entity {
	private static final EntityDataAccessor<Boolean> OPEN = SynchedEntityData.defineId(ElderwoodBranchHeart.class,
			EntityDataSerializers.BOOLEAN);

	private @Nullable Elderwood tree;
	private ElderwoodHeart heart = ElderwoodHeart.RIGHT_BRANCH;

	public ElderwoodBranchHeart(EntityType<? extends ElderwoodBranchHeart> type, Level level) {
		super(type, level);
		this.noPhysics = true;
		this.setNoGravity(true);
	}

	public ElderwoodBranchHeart(Level level, Elderwood tree, ElderwoodHeart heart) {
		this(ModEntities.ELDERWOOD_BRANCH_HEART, level);
		this.tree = tree;
		this.heart = heart;
	}

	@Override
	protected void defineSynchedData(SynchedEntityData.Builder builder) {
		builder.define(OPEN, false);
	}

	@Override
	public void tick() {
		super.tick();
		if (this.level().isClientSide()) {
			return;
		}
		if (this.tree == null || this.tree.isRemoved()) {
			this.discard();
			return;
		}
		this.entityData.set(OPEN, this.tree.isAlive() && this.tree.isVulnerable(this.heart));
	}

	// Only while its heart is open: closed, it would get in the way of hits meant for the trunk.
	@Override
	public boolean isPickable() {
		return this.entityData.get(OPEN);
	}

	@Override
	public boolean hurtServer(ServerLevel level, DamageSource source, float damage) {
		return this.tree != null && this.tree.hurtHeart(level, this.heart, source, damage);
	}

	@Override
	public boolean isPushable() {
		return false;
	}

	@Override
	public PushReaction getPistonPushReaction() {
		return PushReaction.IGNORE_ENTITY;
	}

	@Override
	public boolean shouldBeSaved() {
		return false;
	}

	@Override
	protected void readAdditionalSaveData(ValueInput input) {
	}

	@Override
	protected void addAdditionalSaveData(ValueOutput output) {
	}
}
