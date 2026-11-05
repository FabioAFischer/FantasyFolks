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

/** Server-persistent store for all communities in all dimensions. */
public final class CommunityStorage extends SavedData {
	public static final SavedDataType<CommunityStorage> TYPE = new SavedDataType<>(
			FantasyFolk.id("communities"),
			CommunityStorage::new,
			RecordCodecBuilder.create(instance -> instance.group(
					Community.CODEC.listOf().fieldOf("communities").forGetter(CommunityStorage::asList)
			).apply(instance, CommunityStorage::new)),
			DataFixTypes.LEVEL
	);

	private final Map<UUID, Community> communities = new LinkedHashMap<>();

	public CommunityStorage() {
	}

	private CommunityStorage(List<Community> communities) {
		for (Community community : communities) {
			this.communities.put(community.id(), community);
		}
	}

	private List<Community> asList() {
		return List.copyOf(communities.values());
	}

	public Collection<Community> all() {
		return List.copyOf(communities.values());
	}

	public Community get(UUID id) {
		return communities.get(id);
	}

	public void add(Community community) {
		communities.put(community.id(), community);
		setDirty();
	}

	public void markDirty() {
		setDirty();
	}
}
