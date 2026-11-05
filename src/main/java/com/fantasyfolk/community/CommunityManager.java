package com.fantasyfolk.community;

import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;

import com.fantasyfolk.block.CommunityMarkerBlock;
import com.fantasyfolk.world.VillageDetector;

import java.util.Comparator;
import java.util.Optional;
import java.util.UUID;

/** Server-side services for community registration and member links. */
public final class CommunityManager {
	public static final int INITIAL_RADIUS = 32;

	private CommunityManager() {
	}

	public static CommunityStorage storage(MinecraftServer server) {
		return server.getDataStorage().computeIfAbsent(CommunityStorage.TYPE);
	}

	public static RegistrationResult registerCustom(ServerLevel level, BlockPos center) {
		CommunityStorage storage = storage(level.getServer());
		Optional<Community> existing = findAt(storage, level.dimension(), center);
		if (existing.isPresent()) {
			return new RegistrationResult(RegistrationStatus.ALREADY_REGISTERED, existing.get());
		}
		if (hasOverlap(storage, level.dimension(), center, INITIAL_RADIUS)) {
			return new RegistrationResult(RegistrationStatus.OVERLAPS, null);
		}

		UUID id = UUID.randomUUID();
		Community community = new Community(
				id,
				CommunityType.CUSTOM,
				"custom-" + id.toString().substring(0, 8),
				level.dimension(),
				center,
				INITIAL_RADIUS,
				CommunityState.ACTIVE
		);
		storage.add(community);
		return new RegistrationResult(RegistrationStatus.REGISTERED, community);
	}

	public static RegistrationResult registerVanilla(ServerLevel level, BlockPos bellPos) {
		CommunityStorage storage = storage(level.getServer());
		Optional<Community> existing = findAt(storage, level.dimension(), bellPos);
		if (existing.isPresent()) {
			return new RegistrationResult(RegistrationStatus.ALREADY_REGISTERED, existing.get());
		}
		if (!VillageDetector.isRecognizableVillage(level, bellPos)) {
			return new RegistrationResult(RegistrationStatus.INVALID_VILLAGE, null);
		}
		if (hasOverlap(storage, level.dimension(), bellPos, INITIAL_RADIUS)) {
			return new RegistrationResult(RegistrationStatus.OVERLAPS, null);
		}

		UUID id = UUID.randomUUID();
		Community community = new Community(
				id,
				CommunityType.VANILLA,
				"vanilla-" + id.toString().substring(0, 8),
				level.dimension(),
				bellPos,
				INITIAL_RADIUS,
				CommunityState.ACTIVE
		);
		storage.add(community);
		return new RegistrationResult(RegistrationStatus.REGISTERED, community);
	}

	public static Optional<Community> findById(MinecraftServer server, UUID id) {
		CommunityStorage storage = storage(server);
		Community community = storage.get(id);
		if (community != null) {
			refreshLoadedState(server, storage, community);
		}
		return Optional.ofNullable(community);
	}

	public static boolean isMember(MinecraftServer server, UUID communityId, UUID entityId) {
		Community community = storage(server).get(communityId);
		return community != null && community.hasMember(entityId);
	}

	public static Optional<Community> findAt(
			MinecraftServer server,
			ResourceKey<Level> dimension,
			BlockPos center
	) {
		refreshLoadedStates(server);
		return findAt(storage(server), dimension, center);
	}

	public static Optional<Community> findNearby(MinecraftServer server, ResourceKey<Level> dimension, BlockPos pos) {
		refreshLoadedStates(server);
		return storage(server).all().stream()
				.filter(community -> community.dimension().equals(dimension))
				.filter(community -> horizontalDistanceSquared(community.center(), pos)
						<= (long) community.radius() * community.radius())
				.min(Comparator.comparingLong(community -> horizontalDistanceSquared(community.center(), pos)));
	}

	public static Optional<Community> findCommunityForMember(MinecraftServer server, UUID entityId) {
		return storage(server).all().stream()
				.filter(community -> community.hasMember(entityId))
				.findFirst();
	}

	public static LinkResult linkMember(ServerLevel level, UUID communityId, UUID entityId) {
		return linkMember(level, communityId, entityId, null);
	}

	public static LinkResult linkMember(ServerLevel level, UUID communityId, Entity entity) {
		if (!(entity instanceof CommunityHolder)) {
			return LinkResult.UNSUPPORTED_ENTITY;
		}
		if (!(entity.level() instanceof ServerLevel entityLevel)
				|| !entityLevel.dimension().equals(level.dimension())) {
			return LinkResult.DIMENSION_MISMATCH;
		}
		return linkMember(level, communityId, entity.getUUID(), entity.blockPosition());
	}

	private static LinkResult linkMember(
			ServerLevel level,
			UUID communityId,
			UUID entityId,
			BlockPos entityPosition
	) {
		CommunityStorage storage = storage(level.getServer());
		Community community = storage.get(communityId);
		if (community == null) {
			return LinkResult.COMMUNITY_NOT_FOUND;
		}
		if (!community.dimension().equals(level.dimension())) {
			return LinkResult.DIMENSION_MISMATCH;
		}
		if (community.state() == CommunityState.INACTIVE) {
			return LinkResult.COMMUNITY_INACTIVE;
		}
		if (entityPosition != null && !community.isWithinTerritory(entityPosition)) {
			return LinkResult.OUTSIDE_TERRITORY;
		}
		if (community.hasMember(entityId)) {
			return LinkResult.ALREADY_MEMBER;
		}
		if (findCommunityForMember(level.getServer(), entityId).isPresent()) {
			return LinkResult.ALREADY_LINKED;
		}

		community.addMember(entityId, level.dimension());
		storage.markDirty();
		return LinkResult.LINKED;
	}

	public static Optional<Community> unlinkMember(MinecraftServer server, UUID entityId) {
		CommunityStorage storage = storage(server);
		for (Community community : storage.all()) {
			if (community.removeMember(entityId)) {
				storage.markDirty();
				return Optional.of(community);
			}
		}
		return Optional.empty();
	}

	public static void markInactiveAt(ServerLevel level, BlockPos center) {
		CommunityStorage storage = storage(level.getServer());
		for (Community community : storage.all()) {
			if (community.dimension().equals(level.dimension()) && community.center().equals(center)
					&& community.state() == CommunityState.ACTIVE) {
				community.setState(CommunityState.INACTIVE);
				storage.markDirty();
			}
		}
	}

	public static void refreshLoadedStates(MinecraftServer server) {
		CommunityStorage storage = storage(server);
		for (Community community : storage.all()) {
			refreshLoadedState(server, storage, community);
		}
	}

	private static void refreshLoadedState(
			MinecraftServer server,
			CommunityStorage storage,
			Community community
	) {
		if (community.state() != CommunityState.ACTIVE) {
			return;
		}
		ServerLevel level = server.getLevel(community.dimension());
		if (level == null || !level.isLoaded(community.center())) {
			return;
		}
		boolean anchorPresent = switch (community.type()) {
			case CUSTOM -> level.getBlockState(community.center()).is(CommunityMarkerBlock.INSTANCE);
			case VANILLA -> VillageDetector.hasBellAnchor(level, community.center());
		};
		if (!anchorPresent) {
			community.setState(CommunityState.INACTIVE);
			storage.markDirty();
		}
	}

	private static Optional<Community> findAt(CommunityStorage storage, ResourceKey<Level> dimension, BlockPos center) {
		return storage.all().stream()
				.filter(community -> community.dimension().equals(dimension))
				.filter(community -> community.center().equals(center))
				.findFirst();
	}

	private static boolean hasOverlap(CommunityStorage storage, ResourceKey<Level> dimension, BlockPos center, int radius) {
		return storage.all().stream()
				.filter(community -> community.dimension().equals(dimension))
				.anyMatch(community -> horizontalDistanceSquared(community.center(), center)
						< (long) (community.radius() + radius) * (community.radius() + radius));
	}

	private static long horizontalDistanceSquared(BlockPos first, BlockPos second) {
		long dx = first.getX() - (long) second.getX();
		long dz = first.getZ() - (long) second.getZ();
		return dx * dx + dz * dz;
	}

	public enum RegistrationStatus {
		REGISTERED,
		ALREADY_REGISTERED,
		INVALID_VILLAGE,
		OVERLAPS
	}

	public record RegistrationResult(RegistrationStatus status, Community community) {
	}

	public enum LinkResult {
		LINKED,
		COMMUNITY_NOT_FOUND,
		DIMENSION_MISMATCH,
		COMMUNITY_INACTIVE,
		ALREADY_MEMBER,
		ALREADY_LINKED,
		OUTSIDE_TERRITORY,
		UNSUPPORTED_ENTITY
	}
}
