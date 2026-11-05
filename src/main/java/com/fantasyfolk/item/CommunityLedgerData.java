package com.fantasyfolk.item;

import java.util.Optional;
import java.util.UUID;

import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.Level;

import com.fantasyfolk.community.Community;

/** Persistent community reference stored on each ledger item stack. */
public final class CommunityLedgerData {
	private static final String COMMUNITY_ID = "fantasyfolk_community_id";
	private static final String COMMUNITY_DIMENSION = "fantasyfolk_community_dimension";

	private CommunityLedgerData() {
	}

	public static void select(ItemStack stack, Community community) {
		CustomData.update(DataComponents.CUSTOM_DATA, stack, tag -> {
			tag.putString(COMMUNITY_ID, community.id().toString());
			tag.putString(COMMUNITY_DIMENSION, community.dimension().identifier().toString());
		});
	}

	public static Optional<Selection> read(ItemStack stack) {
		CustomData customData = stack.get(DataComponents.CUSTOM_DATA);
		if (customData == null || customData.isEmpty()) {
			return Optional.empty();
		}

		CompoundTag tag = customData.copyTag();
		try {
			UUID id = UUID.fromString(tag.getStringOr(COMMUNITY_ID, ""));
			Identifier dimensionId = Identifier.tryParse(tag.getStringOr(COMMUNITY_DIMENSION, ""));
			if (dimensionId == null) {
				return Optional.empty();
			}
			return Optional.of(new Selection(id, ResourceKey.create(Registries.DIMENSION, dimensionId)));
		} catch (IllegalArgumentException exception) {
			return Optional.empty();
		}
	}

	public record Selection(UUID id, ResourceKey<Level> dimension) {
	}
}
