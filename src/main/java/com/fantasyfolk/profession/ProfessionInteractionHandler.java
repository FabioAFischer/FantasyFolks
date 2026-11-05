package com.fantasyfolk.profession;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import net.fabricmc.fabric.api.event.player.UseEntityCallback;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.EntityHitResult;

/** Sneak-interaction confirmation flow for assigning, changing and removing professions. */
public final class ProfessionInteractionHandler {
	private static final int CONFIRMATION_WINDOW_TICKS = 200;
	private static final Map<UUID, PendingAction> PENDING = new HashMap<>();

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
				|| hand != InteractionHand.MAIN_HAND
				|| !player.isSecondaryUseActive()) {
			return InteractionResult.PASS;
		}
		if (!(entity instanceof ProfessionHolder holder)) {
			PENDING.remove(serverPlayer.getUUID());
			return InteractionResult.PASS;
		}

		ItemStack held = serverPlayer.getItemInHand(InteractionHand.MAIN_HAND);
		ProfessionType target = ProfessionEquipment.professionFor(held).orElse(null);
		boolean removal = held.isEmpty() && holder.getProfessionId() != null;
		if (target == null && !removal) {
			PENDING.remove(serverPlayer.getUUID());
			return InteractionResult.PASS;
		}

		long now = serverPlayer.level().getServer().getTickCount();
		PendingAction pending = PENDING.get(serverPlayer.getUUID());
		if (pending == null || pending.expired(now) || !pending.matches(entity, target, removal, held)) {
			PENDING.put(serverPlayer.getUUID(), new PendingAction(
					entity.getUUID(),
					target == null ? null : target.id().toString(),
					removal,
					held.copy(),
					now + CONFIRMATION_WINDOW_TICKS
			));
			serverPlayer.sendSystemMessage(removal
					? Component.translatable("message.fantasyfolk.profession.confirm_remove")
					: Component.translatable("message.fantasyfolk.profession.confirm_assign", targetDisplay(target)));
			return InteractionResult.SUCCESS_SERVER;
		}

		PENDING.remove(serverPlayer.getUUID());
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

	private record PendingAction(
			UUID entityId,
			String professionId,
			boolean removal,
			ItemStack item,
			long expiresAt
	) {
		private boolean expired(long now) {
			return now > expiresAt;
		}

		private boolean matches(Entity entity, ProfessionType target, boolean currentRemoval, ItemStack currentItem) {
			String currentProfessionId = target == null ? null : target.id().toString();
			return entity.getUUID().equals(entityId)
					&& removal == currentRemoval
					&& java.util.Objects.equals(professionId, currentProfessionId)
					&& ItemStack.matches(item, currentItem);
		}
	}
}
