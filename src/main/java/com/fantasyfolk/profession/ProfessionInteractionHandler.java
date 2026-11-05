package com.fantasyfolk.profession;

import net.fabricmc.fabric.api.event.player.UseEntityCallback;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.EntityHitResult;

import com.fantasyfolk.community.ConfirmationService;
import com.fantasyfolk.registry.ModItems;

/** Sneak-interaction confirmation flow for assigning, changing and removing professions. */
public final class ProfessionInteractionHandler {
	private ProfessionInteractionHandler() {
	}

	public static void initialize() {
		UseEntityCallback.EVENT.register(ProfessionInteractionHandler::interact);
	}

	private static InteractionResult interact(
			Player player,
			net.minecraft.world.level.Level level,
			InteractionHand hand,
			Entity entity,
			EntityHitResult hitResult
	) {
		if (level.isClientSide() || !(player instanceof ServerPlayer serverPlayer)
				|| hand != InteractionHand.MAIN_HAND) {
			return InteractionResult.PASS;
		}
		ItemStack held = serverPlayer.getItemInHand(InteractionHand.MAIN_HAND);
		if (held.is(ModItems.COMMUNITY_LEDGER)
				|| serverPlayer.getItemInHand(InteractionHand.OFF_HAND).is(ModItems.COMMUNITY_LEDGER)) {
			ConfirmationService.cancelIfActionStartsWith(serverPlayer.getUUID(), "profession.");
			return InteractionResult.PASS;
		}
		if (!player.isSecondaryUseActive()) {
			return InteractionResult.PASS;
		}
		if (!(entity instanceof ProfessionHolder holder)) {
			ConfirmationService.cancel(serverPlayer.getUUID());
			return InteractionResult.PASS;
		}

		ProfessionType target = ProfessionEquipment.professionFor(held).orElse(null);
		boolean removal = held.isEmpty() && holder.getProfessionId() != null;
		if (target == null && !removal) {
			ConfirmationService.cancel(serverPlayer.getUUID());
			return InteractionResult.PASS;
		}

		long now = serverPlayer.level().getServer().getTickCount();
		String professionId = target == null ? "" : target.id().toString();
		ConfirmationService.Request request = new ConfirmationService.Request(
				removal ? "profession.remove" : "profession.assign",
				entity.getUUID().toString(),
				professionId,
				held
		);
		if (!ConfirmationService.checkOrArm(serverPlayer.getUUID(), request, now)) {
			serverPlayer.sendSystemMessage(removal
					? Component.translatable("message.fantasyfolk.profession.confirm_remove")
					: Component.translatable("message.fantasyfolk.profession.confirm_assign", targetDisplay(target)));
			return InteractionResult.SUCCESS_SERVER;
		}

		if (removal) {
			ProfessionManager.RemovalResult result = ProfessionManager.remove(serverPlayer, entity);
			sendRemovalResult(serverPlayer, result);
		} else {
			ProfessionManager.AssignmentResult result = ProfessionManager.assign(serverPlayer, entity, target);
			sendAssignmentResult(serverPlayer, result, target);
		}
		return InteractionResult.SUCCESS_SERVER;
	}

	private static Component targetDisplay(ProfessionType target) {
		return Component.translatable(target.translationKey());
	}

	private static void sendAssignmentResult(ServerPlayer player, ProfessionManager.AssignmentResult result, ProfessionType target) {
		Component profession = targetDisplay(target);
		Component message = switch (result) {
			case ASSIGNED -> Component.translatable("message.fantasyfolk.profession.assigned", profession);
			case CHANGED -> Component.translatable("message.fantasyfolk.profession.changed", profession);
			case ALREADY_ASSIGNED -> Component.translatable("message.fantasyfolk.profession.already_assigned");
			case INCOMPATIBLE_EQUIPMENT -> Component.translatable("message.fantasyfolk.profession.incompatible");
			case INVENTORY_FULL -> Component.translatable("message.fantasyfolk.profession.inventory_full");
			case NO_COMMUNITY -> Component.translatable("message.fantasyfolk.profession.no_community");
			case COMMUNITY_INACTIVE -> Component.translatable("message.fantasyfolk.profession.community_inactive");
			case UNKNOWN_CURRENT_PROFESSION -> Component.translatable("message.fantasyfolk.profession.unknown_saved");
			case NOT_AUTHORIZED -> Component.translatable("message.fantasyfolk.profession.not_authorized");
			case NOT_A_PROFESSION_HOLDER -> Component.translatable("message.fantasyfolk.profession.not_supported");
		};
		player.sendSystemMessage(message);
	}

	private static void sendRemovalResult(ServerPlayer player, ProfessionManager.RemovalResult result) {
		Component message = switch (result) {
			case REMOVED -> Component.translatable("message.fantasyfolk.profession.removed");
			case NO_PROFESSION -> Component.translatable("message.fantasyfolk.profession.no_profession");
			case HAND_NOT_EMPTY -> Component.translatable("message.fantasyfolk.profession.empty_hand");
			case INVENTORY_FULL -> Component.translatable("message.fantasyfolk.profession.inventory_full");
			case NO_COMMUNITY -> Component.translatable("message.fantasyfolk.profession.no_community");
			case NOT_AUTHORIZED -> Component.translatable("message.fantasyfolk.profession.not_authorized");
			case NOT_A_PROFESSION_HOLDER -> Component.translatable("message.fantasyfolk.profession.not_supported");
		};
		player.sendSystemMessage(message);
	}

}
