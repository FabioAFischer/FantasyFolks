package com.fantasyfolk.inventory;

import java.util.Optional;

import net.fabricmc.fabric.api.event.player.UseEntityCallback;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.EntityHitResult;

import com.fantasyfolk.community.Community;
import com.fantasyfolk.community.CommunityItemTransfer;
import com.fantasyfolk.community.CommunityManager;
import com.fantasyfolk.community.CommunityPermissions;
import com.fantasyfolk.item.CommunityLedgerData;
import com.fantasyfolk.profession.ProfessionEquipment;
import com.fantasyfolk.registry.ModItems;

/** Server-only manual delivery of one ordinary item to an NPC work inventory. */
public final class WorkInventoryInteractionHandler {
	private WorkInventoryInteractionHandler() {
	}

	public static void initialize() {
		UseEntityCallback.EVENT.register(WorkInventoryInteractionHandler::interact);
	}

	private static InteractionResult interact(
			Player player,
			net.minecraft.world.level.Level level,
			InteractionHand hand,
			Entity entity,
			EntityHitResult hitResult
	) {
		if (level.isClientSide() || hand != InteractionHand.MAIN_HAND
				|| !(player instanceof ServerPlayer serverPlayer)
				|| !(level instanceof ServerLevel serverLevel)
				|| player.isSecondaryUseActive()
				|| !(entity instanceof WorkInventoryHolder holder)) {
			return InteractionResult.PASS;
		}

		ItemStack offered = serverPlayer.getItemInHand(InteractionHand.MAIN_HAND);
		if (offered.is(ModItems.COMMUNITY_LEDGER)
				|| serverPlayer.getItemInHand(InteractionHand.OFF_HAND).is(ModItems.COMMUNITY_LEDGER)
				|| !isDeliverable(offered)) {
			return InteractionResult.PASS;
		}
		Optional<java.util.UUID> communityId = holder.getCommunityId();
		if (communityId.isEmpty()) {
			return InteractionResult.PASS;
		}
		Optional<Community> community = communityId
				.flatMap(id -> CommunityManager.findById(serverLevel.getServer(), id));
		if (community.isEmpty() || community.orElseThrow().state() != com.fantasyfolk.community.CommunityState.ACTIVE
				|| !community.orElseThrow().dimension().equals(serverLevel.dimension())) {
			serverPlayer.sendSystemMessage(Component.translatable("message.fantasyfolk.work.no_active_community"));
			return InteractionResult.SUCCESS_SERVER;
		}
		if (!CommunityPermissions.canPerformPlayerAction(serverPlayer)) {
			serverPlayer.sendSystemMessage(Component.translatable("message.fantasyfolk.ledger.not_authorized"));
			return InteractionResult.SUCCESS_SERVER;
		}

		int transferred = CommunityItemTransfer.transfer(
				serverPlayer.getInventory(),
				serverPlayer.getInventory().getSelectedSlot(),
				holder.getWorkInventory(),
				1
		);
		serverPlayer.sendSystemMessage(Component.translatable(
				transferred > 0
						? "message.fantasyfolk.work.delivered"
						: "message.fantasyfolk.work.inventory_full"
		));
		return InteractionResult.SUCCESS_SERVER;
	}

	private static boolean isDeliverable(ItemStack stack) {
		return !stack.isEmpty()
				&& !stack.is(ModItems.COMMUNITY_LEDGER)
				&& CommunityLedgerData.read(stack).isEmpty()
				&& ProfessionEquipment.professionFor(stack).isEmpty()
				&& stack.get(DataComponents.FOOD) == null
				&& stack.get(DataComponents.CONSUMABLE) == null
				&& stack.get(DataComponents.USE_REMAINDER) == null
				&& stack.get(DataComponents.POTION_CONTENTS) == null;
	}
}
