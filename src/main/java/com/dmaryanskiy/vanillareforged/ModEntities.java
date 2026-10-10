package com.dmaryanskiy.vanillareforged;

import com.dmaryanskiy.vanillareforged.sentry.HeartwoodSentry;
import com.dmaryanskiy.vanillareforged.watcher.Watcher;

import net.fabricmc.fabric.api.object.builder.v1.entity.FabricDefaultAttributeRegistry;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;

public final class ModEntities {
	public static final EntityType<HeartwoodSentry> HEARTWOOD_SENTRY = register("heartwood_sentry",
			EntityType.Builder.of(HeartwoodSentry::new, MobCategory.MISC).sized(0.9F, 2.7F).eyeHeight(2.3F).clientTrackingRange(10));
	public static final EntityType<Watcher> WATCHER = register("watcher",
			EntityType.Builder.of(Watcher::new, MobCategory.MONSTER).sized(2.0F, 4.6F).eyeHeight(4.0F).clientTrackingRange(10));

	private ModEntities() {
	}

	public static void initialize() {
		FabricDefaultAttributeRegistry.register(HEARTWOOD_SENTRY, HeartwoodSentry.createAttributes());
		FabricDefaultAttributeRegistry.register(WATCHER, Watcher.createAttributes());
	}

	private static <T extends Entity> EntityType<T> register(String name, EntityType.Builder<T> builder) {
		ResourceKey<EntityType<?>> key = ResourceKey.create(Registries.ENTITY_TYPE, VanillaReforged.id(name));
		return Registry.register(BuiltInRegistries.ENTITY_TYPE, key, builder.build(key));
	}
}
