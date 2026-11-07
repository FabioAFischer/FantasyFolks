package com.fantasyfolk.community;

import java.util.Optional;
import java.util.UUID;

import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;

/** Server-side validation and lookup for community hunting areas. */
public final class HuntingAreaManager {
	private HuntingAreaManager() {
	}

	public static HuntingAreaStorage storage(MinecraftServer server) {
		return server.getDataStorage().computeIfAbsent(HuntingAreaStorage.TYPE);
	}

	public static Optional<HuntingArea> find(MinecraftServer server, UUID communityId) {
		return Optional.ofNullable(storage(server).get(communityId));
	}

	public static Optional<HuntingArea> findActive(ServerLevel level, UUID communityId) {
		Community community = CommunityManager.findById(level.getServer(), communityId).orElse(null);
		if (community == null || community.state() != CommunityState.ACTIVE
				|| !community.dimension().equals(level.dimension())) {
			return Optional.empty();
		}
		HuntingArea area = storage(level.getServer()).get(communityId);
		if (area == null || area.state() != HuntingAreaState.ACTIVE
				|| !area.dimension().equals(level.dimension())) {
			return Optional.empty();
		}
		return Optional.of(area);
	}

	public static boolean contains(Community community, HuntingArea area, BlockPos position) {
		return community.state() == CommunityState.ACTIVE
				&& community.dimension().equals(area.dimension())
				&& area.state() == HuntingAreaState.ACTIVE
				&& area.contains(position)
				&& community.isWithinTerritory(position);
	}

	public static ConfigurationResult configure(ServerLevel level, UUID communityId, BlockPos center) {
		Community community = CommunityManager.findById(level.getServer(), communityId).orElse(null);
		if (community == null) {
			return new ConfigurationResult(ConfigurationStatus.COMMUNITY_NOT_FOUND, null);
		}
		if (community.state() != CommunityState.ACTIVE) {
			return new ConfigurationResult(ConfigurationStatus.COMMUNITY_INACTIVE, null);
		}
		if (!community.dimension().equals(level.dimension())) {
			return new ConfigurationResult(ConfigurationStatus.DIMENSION_MISMATCH, null);
		}
		if (!community.isWithinTerritory(center)) {
			return new ConfigurationResult(ConfigurationStatus.OUTSIDE_TERRITORY, null);
		}

		HuntingAreaStorage storage = storage(level.getServer());
		HuntingArea area = storage.get(communityId);
		if (area == null) {
			area = new HuntingArea(communityId, level.dimension(), center);
			storage.add(area);
		} else {
			area.moveTo(center);
			storage.markDirty();
		}
		return new ConfigurationResult(ConfigurationStatus.CONFIGURED, area);
	}

	public static RemovalResult remove(ServerLevel level, UUID communityId) {
		Community community = CommunityManager.findById(level.getServer(), communityId).orElse(null);
		if (community == null) {
			return RemovalResult.COMMUNITY_NOT_FOUND;
		}
		if (!community.dimension().equals(level.dimension())) {
			return RemovalResult.DIMENSION_MISMATCH;
		}
		HuntingArea area = storage(level.getServer()).get(communityId);
		if (area == null || area.state() != HuntingAreaState.ACTIVE) {
			return RemovalResult.NOT_CONFIGURED;
		}
		area.deactivate();
		storage(level.getServer()).markDirty();
		return RemovalResult.REMOVED;
	}

	public static Optional<HuntingArea> activeAt(ServerLevel level, BlockPos position) {
		return storage(level.getServer()).all().stream()
				.filter(area -> area.state() == HuntingAreaState.ACTIVE)
				.filter(area -> area.dimension().equals(level.dimension()))
				.filter(area -> area.center().equals(position))
				.findFirst();
	}

	public record ConfigurationResult(ConfigurationStatus status, HuntingArea area) {
	}

	public enum ConfigurationStatus {
		CONFIGURED,
		COMMUNITY_NOT_FOUND,
		COMMUNITY_INACTIVE,
		DIMENSION_MISMATCH,
		OUTSIDE_TERRITORY
	}

	public enum RemovalResult {
		REMOVED,
		NOT_CONFIGURED,
		COMMUNITY_NOT_FOUND,
		DIMENSION_MISMATCH
	}
}
