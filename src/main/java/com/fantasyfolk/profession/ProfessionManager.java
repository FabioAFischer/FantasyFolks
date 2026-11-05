package com.fantasyfolk.profession;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;

import com.fantasyfolk.community.Community;
import com.fantasyfolk.community.CommunityManager;
import com.fantasyfolk.community.CommunityPermissions;
import com.fantasyfolk.community.CommunityState;

/** Server-side profession state transitions and atomic inventory transfers. */
public final class ProfessionManager {
	private ProfessionManager() {
	}

	public static AssignmentResult assign(ServerPlayer player, Entity entity, ProfessionType target) {
		if (!(entity instanceof ProfessionHolder holder)) {
			return AssignmentResult.NOT_A_PROFESSION_HOLDER;
		}
		if (!CommunityPermissions.canPerformPlayerAction(player)) {
			return AssignmentResult.NOT_AUTHORIZED;
		}

		Optional<Community> community = CommunityManager.findCommunityForMember(player.level().getServer(), entity.getUUID());
		if (community.isEmpty()) {
			return AssignmentResult.NO_COMMUNITY;
		}
		if (community.get().state() == CommunityState.INACTIVE) {
			return AssignmentResult.COMMUNITY_INACTIVE;
		}

		ItemStack offered = player.getItemInHand(InteractionHand.MAIN_HAND);
		if (!target.accepts(offered)) {
			return AssignmentResult.INCOMPATIBLE_EQUIPMENT;
		}

		String currentId = holder.getProfessionId();
		if (currentId != null && ProfessionRegistry.byId(currentId).isEmpty()) {
			return AssignmentResult.UNKNOWN_CURRENT_PROFESSION;
		}
		ItemStack currentEquipment = holder.getProfessionEquipment();
		if (currentId != null && target.id().toString().equals(currentId)
				&& ItemStack.isSameItemSameComponents(currentEquipment, offered)) {
			return AssignmentResult.ALREADY_ASSIGNED;
		}

		ItemStack oldEquipment = currentEquipment.copy();
		ItemStack newEquipment = offered.copyWithCount(1);
		ItemStack handAfterTransfer = offered.copy();
		handAfterTransfer.shrink(1);

		Inventory inventory = player.getInventory();
		List<ItemStack> snapshot = snapshot(inventory);
		if (!canReceive(inventory, handAfterTransfer, oldEquipment)) {
			return AssignmentResult.INVENTORY_FULL;
		}

		inventory.setItem(inventory.getSelectedSlot(), handAfterTransfer);
		if (!oldEquipment.isEmpty() && !insert(inventory, oldEquipment.copy())) {
			restore(inventory, snapshot);
			return AssignmentResult.INVENTORY_FULL;
		}
		holder.setProfession(target.id().toString(), newEquipment);
		return currentId == null ? AssignmentResult.ASSIGNED : AssignmentResult.CHANGED;
	}

	public static RemovalResult remove(ServerPlayer player, Entity entity) {
		if (!(entity instanceof ProfessionHolder holder)) {
			return RemovalResult.NOT_A_PROFESSION_HOLDER;
		}
		if (!CommunityPermissions.canPerformPlayerAction(player)) {
			return RemovalResult.NOT_AUTHORIZED;
		}
		if (CommunityManager.findCommunityForMember(player.level().getServer(), entity.getUUID()).isEmpty()) {
			return RemovalResult.NO_COMMUNITY;
		}
		if (!player.getItemInHand(InteractionHand.MAIN_HAND).isEmpty()) {
			return RemovalResult.HAND_NOT_EMPTY;
		}
		if (holder.getProfessionId() == null) {
			return RemovalResult.NO_PROFESSION;
		}

		ItemStack oldEquipment = holder.getProfessionEquipment().copy();
		Inventory inventory = player.getInventory();
		List<ItemStack> snapshot = snapshot(inventory);
		if (!canReceive(inventory, ItemStack.EMPTY, oldEquipment)) {
			return RemovalResult.INVENTORY_FULL;
		}

		if (!oldEquipment.isEmpty() && !insert(inventory, oldEquipment.copy())) {
			restore(inventory, snapshot);
			return RemovalResult.INVENTORY_FULL;
		}
		holder.clearProfession();
		return RemovalResult.REMOVED;
	}

	private static List<ItemStack> snapshot(Inventory inventory) {
		List<ItemStack> snapshot = new ArrayList<>(inventory.getNonEquipmentItems().size());
		for (ItemStack stack : inventory.getNonEquipmentItems()) {
			snapshot.add(stack.copy());
		}
		return snapshot;
	}

	private static void restore(Inventory inventory, List<ItemStack> snapshot) {
		for (int i = 0; i < snapshot.size(); i++) {
			inventory.setItem(i, snapshot.get(i).copy());
		}
	}

	private static boolean canReceive(Inventory inventory, ItemStack handAfterTransfer, ItemStack incoming) {
		if (incoming.isEmpty()) {
			return true;
		}
		int remaining = incoming.count();
		int selected = inventory.getSelectedSlot();
		for (int i = 0; i < inventory.getNonEquipmentItems().size(); i++) {
			ItemStack existing = i == selected ? handAfterTransfer : inventory.getNonEquipmentItems().get(i);
			if (!existing.isEmpty() && canMerge(existing, incoming)) {
				remaining -= Math.min(remaining, stackCapacity(inventory, existing, incoming) - existing.count());
			}
		}
		for (int i = 0; i < inventory.getNonEquipmentItems().size() && remaining > 0; i++) {
			ItemStack existing = i == selected ? handAfterTransfer : inventory.getNonEquipmentItems().get(i);
			if (existing.isEmpty()) {
				remaining -= Math.min(remaining, stackCapacity(inventory, existing, incoming));
			}
		}
		return remaining <= 0;
	}

	private static boolean insert(Inventory inventory, ItemStack incoming) {
		int selected = inventory.getSelectedSlot();
		for (int i = 0; i < inventory.getNonEquipmentItems().size() && !incoming.isEmpty(); i++) {
			ItemStack existing = inventory.getNonEquipmentItems().get(i);
			if (!existing.isEmpty() && canMerge(existing, incoming)) {
				int amount = Math.min(incoming.count(), stackCapacity(inventory, existing, incoming) - existing.count());
				if (amount > 0) {
					existing.grow(amount);
					incoming.shrink(amount);
				}
			}
		}
		for (int i = 0; i < inventory.getNonEquipmentItems().size() && !incoming.isEmpty(); i++) {
			if (inventory.getNonEquipmentItems().get(i).isEmpty()) {
				inventory.setItem(i, incoming.copy());
				incoming.setCount(0);
			}
		}
		return incoming.isEmpty();
	}

	private static boolean canMerge(ItemStack first, ItemStack second) {
		return first.isStackable() && second.isStackable() && ItemStack.isSameItemSameComponents(first, second);
	}

	private static int stackCapacity(Inventory inventory, ItemStack existing, ItemStack incoming) {
		return Math.min(inventory.getMaxStackSize(incoming), incoming.getMaxStackSize());
	}

	public enum AssignmentResult {
		ASSIGNED,
		CHANGED,
		ALREADY_ASSIGNED,
		INCOMPATIBLE_EQUIPMENT,
		INVENTORY_FULL,
		NO_COMMUNITY,
		COMMUNITY_INACTIVE,
		UNKNOWN_CURRENT_PROFESSION,
		NOT_AUTHORIZED,
		NOT_A_PROFESSION_HOLDER
	}

	public enum RemovalResult {
		REMOVED,
		NO_PROFESSION,
		HAND_NOT_EMPTY,
		INVENTORY_FULL,
		NO_COMMUNITY,
		NOT_AUTHORIZED,
		NOT_A_PROFESSION_HOLDER
	}
}
