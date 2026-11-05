package com.fantasyfolk.profession;

import java.util.Optional;

import net.minecraft.tags.ItemTags;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/** Equipment-to-profession mapping. Future tools can be added here without changing entities. */
public final class ProfessionEquipment {
	private ProfessionEquipment() {
	}

	public static Optional<ProfessionType> professionFor(ItemStack stack) {
		return ProfessionRegistry.all().values().stream()
				.filter(type -> type.accepts(stack))
				.findFirst();
	}

	public static boolean isSword(ItemStack stack) {
		return stack.is(ItemTags.SWORDS);
	}

	public static boolean isBow(ItemStack stack) {
		return stack.getItem() == Items.BOW;
	}
}
