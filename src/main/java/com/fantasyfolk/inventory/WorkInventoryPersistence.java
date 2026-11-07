package com.fantasyfolk.inventory;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

/** Persistence and death-drop helpers for reusable NPC work inventories. */
public final class WorkInventoryPersistence {
	private WorkInventoryPersistence() {
	}

	public static void save(WorkInventory inventory, ValueOutput output) {
		inventory.save(output);
	}

	public static void load(WorkInventory inventory, ValueInput input) {
		inventory.load(input);
	}

	public static void drop(Entity entity, ServerLevel level, WorkInventory inventory) {
		for (int slot = 0; slot < inventory.getContainerSize(); slot++) {
			ItemStack stack = inventory.removeItemNoUpdate(slot);
			if (!stack.isEmpty()) {
				entity.spawnAtLocation(level, stack);
			}
		}
	}
}
