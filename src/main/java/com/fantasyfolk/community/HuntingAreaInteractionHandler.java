package com.fantasyfolk.community;

import java.util.Optional;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;

import com.fantasyfolk.item.CommunityLedgerData;

/** Player-facing confirmation flow for configuring and removing hunting areas. */
public final class HuntingAreaInteractionHandler {
	private HuntingAreaInteractionHandler() {
	}

	public static boolean isSupportedTerrain(ServerLevel level, BlockPos position) {
		var state = level.getBlockState(position);
		return state.is(Blocks.GRASS_BLOCK)
				|| state.is(Blocks.DIRT)
				|| state.is(Blocks.COARSE_DIRT)
				|| state.is(Blocks.STONE);
	}

	public static InteractionResult handle(
			ServerLevel level,
			ServerPlayer player,
			BlockPos position,
			ItemStack ledger
	) {
		if (!player.isShiftKeyDown()) {
			return InteractionResult.PASS;
		}

		Community selected = selectedCommunity(level, ledger).orElse(null);
		if (selected == null) {
			return success(player, "message.fantasyfolk.ledger.select_first");
		}
		if (!selected.dimension().equals(level.dimension())) {
			return success(player, "message.fantasyfolk.ledger.wrong_dimension");
		}
		if (selected.state() != CommunityState.ACTIVE) {
			return success(player, "message.fantasyfolk.ledger.inactive");
		}
		if (!selected.isWithinTerritory(position)) {
			return success(player, "message.fantasyfolk.hunting_area.outside_territory");
		}
		if (!CommunityPermissions.canPerformPlayerAction(player)) {
			return success(player, "message.fantasyfolk.ledger.not_authorized");
		}

		Optional<HuntingArea> configured = HuntingAreaManager.find(level.getServer(), selected.id())
				.filter(area -> area.state() == HuntingAreaState.ACTIVE);
		if (configured.isPresent() && configured.orElseThrow().center().equals(position)) {
			return remove(level, player, selected, configured.orElseThrow(), ledger);
		}
		return configure(level, player, selected, position, ledger);
	}

	private static InteractionResult configure(
			ServerLevel level,
			ServerPlayer player,
			Community community,
			BlockPos position,
			ItemStack ledger
	) {
		String target = level.dimension().identifier() + ":" + position.asLong();
		ConfirmationService.Request request = new ConfirmationService.Request(
				"hunting_area.configure",
				target,
				community.id().toString(),
				ledger
		);
		if (!ConfirmationService.checkOrArm(player.getUUID(), request, level.getServer().getTickCount())) {
			player.sendSystemMessage(Component.translatable(
					"message.fantasyfolk.hunting_area.confirm_configure",
					community.name()
			));
			return InteractionResult.SUCCESS_SERVER;
		}

		HuntingAreaManager.ConfigurationResult result = HuntingAreaManager.configure(level, community.id(), position);
		player.sendSystemMessage(Component.translatable(configurationMessage(result.status()), community.name()));
		return InteractionResult.SUCCESS_SERVER;
	}

	private static InteractionResult remove(
			ServerLevel level,
			ServerPlayer player,
			Community community,
			HuntingArea area,
			ItemStack ledger
	) {
		ConfirmationService.Request request = new ConfirmationService.Request(
				"hunting_area.remove",
				area.communityId().toString(),
				community.id().toString(),
				ledger
		);
		if (!ConfirmationService.checkOrArm(player.getUUID(), request, level.getServer().getTickCount())) {
			player.sendSystemMessage(Component.translatable("message.fantasyfolk.hunting_area.confirm_remove"));
			return InteractionResult.SUCCESS_SERVER;
		}

		HuntingAreaManager.RemovalResult result = HuntingAreaManager.remove(level, community.id());
		player.sendSystemMessage(Component.translatable(removalMessage(result)));
		return InteractionResult.SUCCESS_SERVER;
	}

	private static Optional<Community> selectedCommunity(ServerLevel level, ItemStack ledger) {
		return CommunityLedgerData.read(ledger).flatMap(selection -> {
			Optional<Community> community = CommunityManager.findById(level.getServer(), selection.id());
			if (community.isEmpty() || !selection.dimension().equals(community.orElseThrow().dimension())) {
				return Optional.empty();
			}
			return community;
		});
	}

	private static String configurationMessage(HuntingAreaManager.ConfigurationStatus status) {
		return switch (status) {
			case CONFIGURED -> "message.fantasyfolk.hunting_area.configured";
			case COMMUNITY_NOT_FOUND -> "message.fantasyfolk.ledger.missing_community";
			case COMMUNITY_INACTIVE -> "message.fantasyfolk.ledger.inactive";
			case DIMENSION_MISMATCH -> "message.fantasyfolk.ledger.wrong_dimension";
			case OUTSIDE_TERRITORY -> "message.fantasyfolk.hunting_area.outside_territory";
		};
	}

	private static String removalMessage(HuntingAreaManager.RemovalResult result) {
		return switch (result) {
			case REMOVED -> "message.fantasyfolk.hunting_area.removed";
			case NOT_CONFIGURED -> "message.fantasyfolk.hunting_area.not_configured";
			case COMMUNITY_NOT_FOUND -> "message.fantasyfolk.ledger.missing_community";
			case DIMENSION_MISMATCH -> "message.fantasyfolk.ledger.wrong_dimension";
		};
	}

	private static InteractionResult success(ServerPlayer player, String key) {
		player.sendSystemMessage(Component.translatable(key));
		return InteractionResult.SUCCESS_SERVER;
	}
}
