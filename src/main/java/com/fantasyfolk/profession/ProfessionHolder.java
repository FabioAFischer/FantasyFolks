package com.fantasyfolk.profession;

import net.minecraft.world.item.ItemStack;

import com.fantasyfolk.community.CommunityHolder;

/** Capability-like contract for any Fantasy Folk race that can receive a profession. */
public interface ProfessionHolder extends CommunityHolder {
	/** Returns the persisted ID, including an unknown ID loaded from an older/newer version. */
	String getProfessionId();

	ItemStack getProfessionEquipment();

	void setProfession(String professionId, ItemStack equipment);

	void clearProfession();
}
