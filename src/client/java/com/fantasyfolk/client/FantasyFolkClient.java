package com.fantasyfolk.client;

import net.fabricmc.api.ClientModInitializer;

import net.minecraft.client.renderer.entity.EntityRenderers;

import com.fantasyfolk.registry.ModEntities;

/**
 * Client entrypoint for Fantasy Folk rendering.
 */
public final class FantasyFolkClient implements ClientModInitializer {
	@Override
	public void onInitializeClient() {
		EntityRenderers.register(ModEntities.GOBLIN, GoblinRenderer::new);
	}
}
