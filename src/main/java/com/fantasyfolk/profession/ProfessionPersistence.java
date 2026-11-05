package com.fantasyfolk.profession;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

/** Encodes profession state inside the entity's normal Minecraft save data. */
public final class ProfessionPersistence {
	private static final String PROFESSION_ID = "fantasyfolk_profession";
	private static final String EQUIPMENT = "fantasyfolk_profession_equipment";

	private ProfessionPersistence() {
	}

	public static void save(ProfessionHolder holder, ValueOutput output) {
		String professionId = holder.getProfessionId();
		if (professionId == null || professionId.isBlank()) {
			return;
		}

		output.putString(PROFESSION_ID, professionId);
		ItemStack equipment = holder.getProfessionEquipment();
		if (!equipment.isEmpty()) {
			output.store(EQUIPMENT, ItemStack.CODEC, equipment.copy());
		}
	}

	public static void load(ProfessionHolder holder, ValueInput input) {
		String professionId = input.getStringOr(PROFESSION_ID, "");
		if (professionId.isBlank()) {
			holder.clearProfession();
			return;
		}

		ItemStack equipment = input.read(EQUIPMENT, ItemStack.CODEC).orElse(ItemStack.EMPTY);
		// Keep unknown IDs and their item stack intact. The manager will refuse an
		// unsafe assignment until a compatible definition is available again.
		holder.setProfession(professionId, equipment);
	}
}
