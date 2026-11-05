package com.fantasyfolk.registry;

import net.fabricmc.fabric.api.creativetab.v1.CreativeModeTabEvents;

import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.SpawnEggItem;

import com.fantasyfolk.FantasyFolk;
import com.fantasyfolk.item.CommunityLedgerItem;
import com.fantasyfolk.registry.ModBlocks;

/**
 * Item registrations for Fantasy Folk.
 */
public final class ModItems {
	private static final ResourceKey<Item> GOBLIN_SPAWN_EGG_KEY =
			ResourceKey.create(Registries.ITEM, FantasyFolk.id("goblin_spawn_egg"));
	private static final ResourceKey<Item> COMMUNITY_MARKER_KEY =
			ResourceKey.create(Registries.ITEM, FantasyFolk.id("community_marker"));
	private static final ResourceKey<Item> COMMUNITY_LEDGER_KEY =
			ResourceKey.create(Registries.ITEM, FantasyFolk.id("community_ledger"));

	public static final Item COMMUNITY_MARKER = Registry.register(
			BuiltInRegistries.ITEM,
			COMMUNITY_MARKER_KEY,
			new BlockItem(ModBlocks.COMMUNITY_MARKER, new Item.Properties().setId(COMMUNITY_MARKER_KEY))
	);

	public static final Item GOBLIN_SPAWN_EGG = Registry.register(
			BuiltInRegistries.ITEM,
			GOBLIN_SPAWN_EGG_KEY,
			new SpawnEggItem(new Item.Properties()
					.setId(GOBLIN_SPAWN_EGG_KEY)
					.spawnEgg(ModEntities.GOBLIN))
	);

	public static final Item COMMUNITY_LEDGER = Registry.register(
			BuiltInRegistries.ITEM,
			COMMUNITY_LEDGER_KEY,
			new CommunityLedgerItem(new Item.Properties()
					.stacksTo(1)
					.setId(COMMUNITY_LEDGER_KEY))
	);

	private ModItems() {
	}

	public static void initialize() {
		CreativeModeTabEvents.modifyOutputEvent(CreativeModeTabs.FUNCTIONAL_BLOCKS)
				.register(creativeTab -> {
					creativeTab.accept(COMMUNITY_MARKER);
					creativeTab.accept(COMMUNITY_LEDGER);
				});
		CreativeModeTabEvents.modifyOutputEvent(CreativeModeTabs.SPAWN_EGGS)
				.register(creativeTab -> creativeTab.accept(GOBLIN_SPAWN_EGG));
	}
}
