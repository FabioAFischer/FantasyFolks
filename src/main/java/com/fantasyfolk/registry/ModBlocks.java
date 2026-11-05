package com.fantasyfolk.registry;

import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;

import com.fantasyfolk.FantasyFolk;
import com.fantasyfolk.block.CommunityMarkerBlock;

/** Block registrations for Fantasy Folk. */
public final class ModBlocks {
	private static final ResourceKey<Block> COMMUNITY_MARKER_KEY =
			ResourceKey.create(Registries.BLOCK, FantasyFolk.id("community_marker"));

	public static final CommunityMarkerBlock COMMUNITY_MARKER = (CommunityMarkerBlock) Registry.register(
			BuiltInRegistries.BLOCK,
			COMMUNITY_MARKER_KEY,
			new CommunityMarkerBlock(BlockBehaviour.Properties.of()
					.setId(COMMUNITY_MARKER_KEY)
					.mapColor(MapColor.COLOR_PURPLE)
					.strength(2.0F))
	);

	private ModBlocks() {
	}

	public static void initialize() {
	}
}
