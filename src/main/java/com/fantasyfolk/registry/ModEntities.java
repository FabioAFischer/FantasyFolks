package com.fantasyfolk.registry;

import net.fabricmc.fabric.api.object.builder.v1.entity.FabricDefaultAttributeRegistry;

import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;

import com.fantasyfolk.FantasyFolk;
import com.fantasyfolk.entity.Goblin;

/**
 * Entity type registrations for Fantasy Folk.
 */
public final class ModEntities {
	private static final ResourceKey<EntityType<?>> GOBLIN_KEY =
			ResourceKey.create(Registries.ENTITY_TYPE, FantasyFolk.id("goblin"));

	public static final EntityType<Goblin> GOBLIN = Registry.register(
			BuiltInRegistries.ENTITY_TYPE,
			GOBLIN_KEY,
			EntityType.Builder.of(Goblin::new, MobCategory.CREATURE)
					.sized(0.6F, 1.45F)
					.clientTrackingRange(8)
					.build(GOBLIN_KEY)
	);

	private ModEntities() {
	}

	public static void initialize() {
		FabricDefaultAttributeRegistry.register(GOBLIN, Goblin.createAttributes());
	}
}
