package com.fantasyfolk.community;

import net.fabricmc.fabric.api.event.player.UseBlockCallback;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.level.block.Blocks;

/** Player-facing hooks kept separate from persistent community data. */
public final class CommunityEvents {
	private CommunityEvents() {
	}

	public static void initialize() {
		UseBlockCallback.EVENT.register((player, level, hand, hit) -> {
			if (level.isClientSide()
					|| !player.isShiftKeyDown()
					|| !player.getItemInHand(hand).isEmpty()
					|| !level.getBlockState(hit.getBlockPos()).is(Blocks.BELL)
					|| !(level instanceof net.minecraft.server.level.ServerLevel serverLevel)) {
				return InteractionResult.PASS;
			}

			if (!CommunityPermissions.canPerformPlayerAction(player)) {
				return InteractionResult.PASS;
			}

			BlockPos bellPos = hit.getBlockPos();
			CommunityManager.RegistrationResult result = CommunityManager.registerVanilla(serverLevel, bellPos);
			player.sendSystemMessage(Component.literal(vanillaMessage(result)));
			return InteractionResult.SUCCESS_SERVER;
		});
	}

	private static String vanillaMessage(CommunityManager.RegistrationResult result) {
		return switch (result.status()) {
			case REGISTERED -> "Vila VANILLA registrada: " + result.community().id();
			case ALREADY_REGISTERED -> "Esta vila já está registrada: " + result.community().id();
			case INVALID_VILLAGE -> "Este sino não possui evidências suficientes de uma vila vanilla (villager + POI adicional).";
			case OVERLAPS -> "O território da vila sobrepõe uma comunidade existente.";
		};
	}
}
