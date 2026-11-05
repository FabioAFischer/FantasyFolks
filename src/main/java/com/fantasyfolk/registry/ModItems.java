package com.fantasyfolk.registry;

import net.fabricmc.fabric.api.creativetab.v1.CreativeModeTabEvents;

import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.SpawnEggItem;

import com.fantasyfolk.FantasyFolk;

/**
 * Item registrations for Fantasy Folk.
 */
public final class ModItems {
	private static final ResourceKey<Item> GOBLIN_SPAWN_EGG_KEY =
			ResourceKey.create(Registries.ITEM, FantasyFolk.id("goblin_spawn_egg"));

	public static final Item GOBLIN_SPAWN_EGG = Registry.register(
			BuiltInRegistries.ITEM,
			GOBLIN_SPAWN_EGG_KEY,
			new SpawnEggItem(new Item.Properties()
					.setId(GOBLIN_SPAWN_EGG_KEY)
					.spawnEgg(ModEntities.GOBLIN))
	);

	private ModItems() {
	}

	public static void initialize() {
		CreativeModeTabEvents.modifyOutputEvent(CreativeModeTabs.SPAWN_EGGS)
				.register(creativeTab -> creativeTab.accept(GOBLIN_SPAWN_EGG));
	}
}
