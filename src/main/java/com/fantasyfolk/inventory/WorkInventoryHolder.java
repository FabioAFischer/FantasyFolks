package com.fantasyfolk.inventory;

import net.minecraft.world.item.ItemStack;

import com.fantasyfolk.community.CommunityHolder;

/** Common contract for Fantasy Folk entities that can carry work resources. */
public interface WorkInventoryHolder extends CommunityHolder {
	WorkInventory getWorkInventory();

	/** Allows a profession to reserve supplies in the shared work inventory. */
	default boolean canDepositWorkItem(ItemStack stack) {
		return true;
	}
}
