package com.fantasyfolk.profession;

import java.util.Objects;
import java.util.function.Predicate;

import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;

/** Immutable definition of a profession and the equipment that activates it. */
public record ProfessionType(
		Identifier id,
		String translationKey,
		Predicate<ItemStack> acceptsEquipment
) {
	public ProfessionType {
		Objects.requireNonNull(id, "id");
		Objects.requireNonNull(translationKey, "translationKey");
		Objects.requireNonNull(acceptsEquipment, "acceptsEquipment");
	}

	public boolean accepts(ItemStack stack) {
		return !stack.isEmpty() && acceptsEquipment.test(stack);
	}
}
