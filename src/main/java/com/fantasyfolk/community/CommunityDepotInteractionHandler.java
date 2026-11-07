package com.fantasyfolk.community;

import java.util.Optional;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.ItemStack;

import com.fantasyfolk.item.CommunityLedgerData;

/** Player-facing ledger flow for registering, inspecting and unlinking depots. */
public final class CommunityDepotInteractionHandler {
	private CommunityDepotInteractionHandler() {
	}

	public static InteractionResult handle(ServerLevel level, ServerPlayer player, BlockPos position, ItemStack ledger) {
		Optional<CommunityDepot> registered = CommunityDepotManager.findRegisteredAtIncludingInvalid(level, position);
		if (player.isShiftKeyDown()) {
			return handleRemoval(level, player, ledger, registered);
		}
		if (registered.isPresent()) {
			return inspect(level, player, registered.orElseThrow());
		}
		return handleRegistration(level, player, position, ledger);
	}

	private static InteractionResult handleRegistration(
			ServerLevel level,
			ServerPlayer player,
			BlockPos position,
			ItemStack ledger
	) {
		Optional<CommunityLedgerData.Selection> selection = CommunityLedgerData.read(ledger);
		Community selected;
		if (selection.isPresent()) {
			selected = selectedCommunity(level, player, ledger).orElse(null);
			if (selected == null) {
				return success(player, "message.fantasyfolk.ledger.invalid_selection");
			}
		} else {
			selected = nearbyActiveCommunity(level, position, player);
			if (selected == null) {
				return success(player, "message.fantasyfolk.depot.no_community");
			}
		}
		if (selected.state() != CommunityState.ACTIVE) {
			return success(player, "message.fantasyfolk.ledger.inactive");
		}
		if (!CommunityPermissions.canPerformPlayerAction(player)) {
			return success(player, "message.fantasyfolk.ledger.not_authorized");
		}

		BlockPos logicalPosition = CommunityDepotManager.logicalPosition(level, position).orElse(position);
		String target = level.dimension().identifier() + ":" + logicalPosition.asLong();
		ConfirmationService.Request request = new ConfirmationService.Request(
				"depot.register",
				target,
				selected.id().toString(),
				ledger
		);
		if (!ConfirmationService.checkOrArm(player.getUUID(), request, level.getServer().getTickCount())) {
			player.sendSystemMessage(Component.translatable(
					"message.fantasyfolk.depot.confirm_register",
					Component.translatable(typeKey(level, position)),
					selected.name()
			));
			return InteractionResult.SUCCESS_SERVER;
		}

		CommunityDepotManager.RegistrationResult result = CommunityDepotManager.register(
				level,
				selected.id(),
				position
		);
		if (result.status() == CommunityDepotManager.RegistrationStatus.REGISTERED
					|| result.status() == CommunityDepotManager.RegistrationStatus.ALREADY_REGISTERED) {
			CommunityLedgerData.select(ledger, selected);
		}
		player.sendSystemMessage(Component.translatable(registrationMessage(result.status()), selected.name()));
		return InteractionResult.SUCCESS_SERVER;
	}

	private static Community nearbyActiveCommunity(ServerLevel level, BlockPos position, ServerPlayer player) {
		Optional<Community> atContainer = CommunityManager.findNearby(level.getServer(), level.dimension(), position)
				.filter(community -> community.state() == CommunityState.ACTIVE);
		if (atContainer.isPresent()) {
			return atContainer.orElseThrow();
		}
		return CommunityManager.findNearby(level.getServer(), level.dimension(), player.blockPosition())
				.filter(community -> community.state() == CommunityState.ACTIVE)
				.orElse(null);
	}

	private static InteractionResult handleRemoval(
			ServerLevel level,
			ServerPlayer player,
			ItemStack ledger,
			Optional<CommunityDepot> registered
	) {
		if (registered.isEmpty()) {
			return success(player, "message.fantasyfolk.depot.not_registered");
		}
		CommunityDepot depot = registered.orElseThrow();
		Community selected = selectedCommunity(level, player, ledger).orElse(null);
		if (selected == null) {
			return success(player, "message.fantasyfolk.ledger.select_first");
		}
		if (!selected.id().equals(depot.communityId())) {
			return success(player, "message.fantasyfolk.depot.unlink_mismatch");
		}
		if (!CommunityPermissions.canPerformPlayerAction(player)) {
			return success(player, "message.fantasyfolk.ledger.not_authorized");
		}

		ConfirmationService.Request request = new ConfirmationService.Request(
				"depot.remove",
				depot.id().toString(),
				selected.id().toString(),
				ledger
		);
		if (!ConfirmationService.checkOrArm(player.getUUID(), request, level.getServer().getTickCount())) {
			player.sendSystemMessage(Component.translatable("message.fantasyfolk.depot.confirm_remove"));
			return InteractionResult.SUCCESS_SERVER;
		}

		CommunityDepotManager.RemovalResult result = CommunityDepotManager.remove(level, selected.id(), depot.id());
		player.sendSystemMessage(Component.translatable(removalMessage(result)));
		return InteractionResult.SUCCESS_SERVER;
	}

	private static InteractionResult inspect(ServerLevel level, ServerPlayer player, CommunityDepot depot) {
		Community community = CommunityManager.findById(level.getServer(), depot.communityId()).orElse(null);
		if (community == null) {
			return success(player, "message.fantasyfolk.depot.missing_community");
		}
		if (depot.state() == DepotState.INVALID) {
			return success(player, "message.fantasyfolk.depot.invalid");
		}
		Optional<CommunityDepotManager.DepotInfo> info = CommunityDepotManager.info(level, depot);
		if (info.isEmpty()) {
			return success(player, "message.fantasyfolk.depot.invalid");
		}
		CommunityDepotManager.DepotInfo depotInfo = info.orElseThrow();
		String stateKey = depot.state() == DepotState.INVALID
				? "community.fantasyfolk.depot.state.invalid"
				: community.state() == CommunityState.ACTIVE
						? "community.fantasyfolk.depot.state.available"
						: "community.fantasyfolk.depot.state.community_inactive";
		player.sendSystemMessage(Component.translatable(
				"message.fantasyfolk.depot.info",
				community.name(),
				Component.translatable(typeKey(depotInfo.type())),
				Component.translatable(stateKey),
				depotInfo.occupiedSlots(),
				depotInfo.capacity(),
				depotInfo.capacity() - depotInfo.occupiedSlots()
		));
		return InteractionResult.SUCCESS_SERVER;
	}

	private static Optional<Community> selectedCommunity(ServerLevel level, ServerPlayer player, ItemStack ledger) {
		return CommunityLedgerData.read(ledger)
				.flatMap(selection -> {
					Optional<Community> community = CommunityManager.findById(level.getServer(), selection.id());
					if (community.isEmpty() || !selection.dimension().equals(community.orElseThrow().dimension())
							|| !selection.dimension().equals(level.dimension())) {
						return Optional.empty();
					}
					return community;
				});
	}

	private static String typeKey(ServerLevel level, BlockPos position) {
		return CommunityDepotManager.typeAt(level, position)
				.map(CommunityDepotInteractionHandler::typeKey)
				.orElse("community.fantasyfolk.depot.type.container");
	}

	private static String typeKey(DepotType type) {
		return "community.fantasyfolk.depot.type." + type.name().toLowerCase(java.util.Locale.ROOT);
	}

	private static String registrationMessage(CommunityDepotManager.RegistrationStatus status) {
		return switch (status) {
			case REGISTERED -> "message.fantasyfolk.depot.registered";
			case ALREADY_REGISTERED -> "message.fantasyfolk.depot.already_registered";
			case COMMUNITY_NOT_FOUND -> "message.fantasyfolk.ledger.missing_community";
			case COMMUNITY_INACTIVE -> "message.fantasyfolk.ledger.inactive";
			case DIMENSION_MISMATCH -> "message.fantasyfolk.ledger.wrong_dimension";
			case OUTSIDE_TERRITORY -> "message.fantasyfolk.depot.outside_territory";
			case UNSUPPORTED_CONTAINER -> "message.fantasyfolk.depot.unsupported";
			case OWNED_BY_OTHER_COMMUNITY -> "message.fantasyfolk.depot.owned_other";
		};
	}

	private static String removalMessage(CommunityDepotManager.RemovalResult result) {
		return switch (result) {
			case REMOVED -> "message.fantasyfolk.depot.removed";
			case NOT_FOUND -> "message.fantasyfolk.depot.not_registered";
			case OWNED_BY_OTHER_COMMUNITY -> "message.fantasyfolk.depot.unlink_mismatch";
			case DIMENSION_MISMATCH -> "message.fantasyfolk.ledger.wrong_dimension";
		};
	}

	private static InteractionResult success(ServerPlayer player, String key) {
		player.sendSystemMessage(Component.translatable(key));
		return InteractionResult.SUCCESS_SERVER;
	}
}
