package com.fantasyfolk.community;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import com.fantasyfolk.FantasyFolk;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.minecraft.util.datafix.DataFixTypes;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;

/** Server-persistent store for community hunting areas. */
public final class HuntingAreaStorage extends SavedData {
	public static final SavedDataType<HuntingAreaStorage> TYPE = new SavedDataType<>(
			FantasyFolk.id("hunting_areas"),
			HuntingAreaStorage::new,
			RecordCodecBuilder.create(instance -> instance.group(
					HuntingArea.CODEC.listOf().fieldOf("areas").forGetter(HuntingAreaStorage::asList)
			).apply(instance, HuntingAreaStorage::new)),
			DataFixTypes.LEVEL
	);

	private final Map<UUID, HuntingArea> areas = new LinkedHashMap<>();

	public HuntingAreaStorage() {
	}

	private HuntingAreaStorage(List<HuntingArea> areas) {
		for (HuntingArea area : areas) {
			this.areas.put(area.communityId(), area);
		}
	}

	private List<HuntingArea> asList() {
		return List.copyOf(areas.values());
	}

	public Collection<HuntingArea> all() {
		return List.copyOf(areas.values());
	}

	public HuntingArea get(UUID communityId) {
		return areas.get(communityId);
	}

	public void add(HuntingArea area) {
		areas.put(area.communityId(), area);
		setDirty();
	}

	public void markDirty() {
		setDirty();
	}
}
