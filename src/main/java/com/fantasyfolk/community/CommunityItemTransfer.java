package com.fantasyfolk.community;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;

import net.minecraft.world.Container;
import net.minecraft.world.item.ItemStack;

/** Atomic, component-aware item transfer service shared by NPCs and future professions. */
public final class CommunityItemTransfer {
	private CommunityItemTransfer() {
	}

	public static int transfer(Container source, int sourceSlot, Container destination, int requested) {
		if (requested <= 0 || sourceSlot < 0 || sourceSlot >= source.getContainerSize()) {
			return 0;
		}

		ItemStack sourceStack = source.getItem(sourceSlot);
		if (sourceStack.isEmpty()) {
			return 0;
		}
		int transferred = transfer(sourceStack, destination, requested);
		if (transferred > 0) {
			source.setChanged();
		}
		return transferred;
	}

	/** Transfers from a real stack, preserving components and mutating only its count. */
	public static int transfer(ItemStack sourceStack, Container destination, int requested) {
		if (sourceStack.isEmpty() || requested <= 0) {
			return 0;
		}

		int requestedAmount = Math.min(requested, sourceStack.count());
		int remaining = requestedAmount;
		List<SlotTransfer> plan = new ArrayList<>();
		for (int slot = 0; slot < destination.getContainerSize() && remaining > 0; slot++) {
			ItemStack existing = destination.getItem(slot);
			if (!destination.canPlaceItem(slot, sourceStack)) {
				continue;
			}

			int capacity;
			if (existing.isEmpty()) {
				capacity = Math.min(destination.getMaxStackSize(sourceStack), sourceStack.getMaxStackSize());
			} else if (existing.isStackable()
					&& ItemStack.isSameItemSameComponents(existing, sourceStack)) {
				capacity = Math.min(destination.getMaxStackSize(existing), existing.getMaxStackSize()) - existing.count();
			} else {
				continue;
			}

			int amount = Math.min(remaining, Math.max(0, capacity));
			if (amount > 0) {
				plan.add(new SlotTransfer(slot, amount, existing.isEmpty()));
				remaining -= amount;
			}
		}

		int transferred = requestedAmount - remaining;
		if (transferred == 0) {
			return 0;
		}

		for (SlotTransfer transfer : plan) {
			if (transfer.emptySlot()) {
				destination.setItem(transfer.slot(), sourceStack.copyWithCount(transfer.amount()));
			} else {
				destination.getItem(transfer.slot()).grow(transfer.amount());
			}
		}
		destination.setChanged();
		sourceStack.shrink(transferred);
		return transferred;
	}

	public static int transferAll(Container source, Container destination) {
		return transferAll(source, destination, stack -> true);
	}

	public static int transferAll(Container source, Container destination, Predicate<ItemStack> allowed) {
		int transferred = 0;
		for (int slot = 0; slot < source.getContainerSize(); slot++) {
			ItemStack stack = source.getItem(slot);
			if (!stack.isEmpty() && allowed.test(stack)) {
				transferred += transfer(source, slot, destination, stack.count());
			}
		}
		return transferred;
	}

	public static boolean canReceiveAny(Container source, Container destination) {
		return canReceiveAny(source, destination, stack -> true);
	}

	public static boolean canReceiveAny(Container source, Container destination, Predicate<ItemStack> allowed) {
		for (int slot = 0; slot < source.getContainerSize(); slot++) {
			ItemStack stack = source.getItem(slot);
			if (!stack.isEmpty() && allowed.test(stack) && canReceive(stack, destination)) {
				return true;
			}
		}
		return false;
	}

	private static boolean canReceive(ItemStack stack, Container destination) {
		for (int slot = 0; slot < destination.getContainerSize(); slot++) {
			ItemStack existing = destination.getItem(slot);
			if (!destination.canPlaceItem(slot, stack)) {
				continue;
			}
			if (existing.isEmpty()) {
				return true;
			}
			if (existing.isStackable()
					&& ItemStack.isSameItemSameComponents(existing, stack)
					&& existing.count() < Math.min(destination.getMaxStackSize(existing), existing.getMaxStackSize())) {
				return true;
			}
		}
		return false;
	}

	private record SlotTransfer(int slot, int amount, boolean emptySlot) {
	}
}
