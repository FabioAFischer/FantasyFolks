package com.fantasyfolk.community;

import net.fabricmc.fabric.api.event.player.UseBlockCallback;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.BlockHitResult;

import com.fantasyfolk.item.CommunityLedgerItem;

/** Player-facing hooks kept separate from persistent community data. */
public final class CommunityEvents {
	private CommunityEvents() {
	}

	public static void initialize() {
		UseBlockCallback.EVENT.register((player, level, hand, hit) -> {
			InteractionResult depotResult = handleLedgerDepotInteraction(player, level, hand, hit);
			if (depotResult != InteractionResult.PASS) {
				return depotResult;
			}

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

	private static InteractionResult handleLedgerDepotInteraction(
			Player player,
			Level level,
			InteractionHand hand,
			BlockHitResult hit
	) {
		if (!(player.getItemInHand(hand).getItem() instanceof CommunityLedgerItem)
				|| !isVanillaContainer(level, hit.getBlockPos())) {
			return InteractionResult.PASS;
		}

		// UseBlockCallback also runs on the client. Consuming the predicted click
		// there prevents a chest/barrel screen from opening before the server
		// processes the ledger action.
		if (level.isClientSide()) {
			return InteractionResult.SUCCESS;
		}
		if (!(level instanceof ServerLevel serverLevel)
				|| !(player instanceof ServerPlayer serverPlayer)
				|| !CommunityDepotManager.isSupportedContainer(serverLevel, hit.getBlockPos())) {
			return InteractionResult.PASS;
		}

		// This is the single server-side entry point for depot interactions. In
		// particular, do not leave an unregistered container to Item.useOn(), or
		// vanilla may consume the click first and open it.
		return CommunityDepotInteractionHandler.handle(
				serverLevel,
				serverPlayer,
				hit.getBlockPos(),
				player.getItemInHand(hand)
		);
	}

	private static boolean isVanillaContainer(
			Level level,
			BlockPos position
	) {
		return level.getBlockState(position).is(Blocks.CHEST)
				|| level.getBlockState(position).is(Blocks.BARREL);
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
