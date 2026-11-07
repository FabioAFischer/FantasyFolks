package com.fantasyfolk.inventory;

import net.minecraft.world.SimpleContainer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

/** Nine-slot work inventory kept separate from profession equipment. */
public final class WorkInventory extends SimpleContainer {
	public static final int CAPACITY = 9;
	private static final String ITEMS = "fantasyfolk_work_inventory";

	public WorkInventory() {
		super(CAPACITY);
	}

	public void save(ValueOutput output) {
		if (!isEmpty()) {
			storeAsItemList(output.list(ITEMS, ItemStack.CODEC));
		}
	}

	public void load(ValueInput input) {
		fromItemList(input.listOrEmpty(ITEMS, ItemStack.CODEC));
	}
}
