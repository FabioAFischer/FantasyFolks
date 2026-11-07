package com.fantasyfolk.community;

import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.minecraft.util.datafix.DataFixTypes;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;

import com.fantasyfolk.FantasyFolk;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/** Server-persistent registrations; item contents remain in vanilla block entities. */
public final class CommunityDepotStorage extends SavedData {
	public static final SavedDataType<CommunityDepotStorage> TYPE = new SavedDataType<>(
			FantasyFolk.id("community_depots"),
			CommunityDepotStorage::new,
			RecordCodecBuilder.create(instance -> instance.group(
					CommunityDepot.CODEC.listOf().fieldOf("depots").forGetter(CommunityDepotStorage::asList)
			).apply(instance, CommunityDepotStorage::new)),
			DataFixTypes.LEVEL
	);

	private final Map<UUID, CommunityDepot> depots = new LinkedHashMap<>();

	public CommunityDepotStorage() {
	}

	private CommunityDepotStorage(List<CommunityDepot> depots) {
		for (CommunityDepot depot : depots) {
			this.depots.put(depot.id(), depot);
		}
	}

	private List<CommunityDepot> asList() {
		return List.copyOf(depots.values());
	}

	public Collection<CommunityDepot> all() {
		return List.copyOf(depots.values());
	}

	public CommunityDepot get(UUID id) {
		return depots.get(id);
	}

	public void add(CommunityDepot depot) {
		depots.put(depot.id(), depot);
		setDirty();
	}

	public void remove(UUID id) {
		if (depots.remove(id) != null) {
			setDirty();
		}
	}

	public void markDirty() {
		setDirty();
	}
}
