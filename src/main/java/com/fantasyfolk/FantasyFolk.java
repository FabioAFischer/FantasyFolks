package com.fantasyfolk;

import net.fabricmc.api.ModInitializer;

import net.minecraft.resources.Identifier;

import com.fantasyfolk.registry.ModEntities;
import com.fantasyfolk.registry.ModItems;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Entry point for the Fantasy Folk mod.
 *
 * <p>This first milestone only establishes the mod lifecycle. Content will be
 * added in later milestones.</p>
 */
public final class FantasyFolk implements ModInitializer {
	public static final String MOD_ID = "fantasyfolk";
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	@Override
	public void onInitialize() {
		ModEntities.initialize();
		ModItems.initialize();
		LOGGER.info("Fantasy Folk initialized.");
	}

	public static Identifier id(String path) {
		return Identifier.fromNamespaceAndPath(MOD_ID, path);
	}
}
