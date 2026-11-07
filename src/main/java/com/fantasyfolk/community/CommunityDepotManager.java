package com.fantasyfolk.community;

import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.function.Predicate;

import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.Container;
import net.minecraft.world.level.block.BarrelBlock;
import net.minecraft.world.level.block.ChestBlock;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.ChestType;

/** Server-side registration, reconciliation and lookup service for community depots. */
public final class CommunityDepotManager {
	private CommunityDepotManager() {
	}

	public static CommunityDepotStorage storage(MinecraftServer server) {
		return server.getDataStorage().computeIfAbsent(CommunityDepotStorage.TYPE);
	}

	public static boolean isSupportedContainer(ServerLevel level, BlockPos position) {
		return inspect(level, position).isPresent();
	}

	public static Optional<BlockPos> logicalPosition(ServerLevel level, BlockPos position) {
		return inspect(level, position).map(CurrentContainer::primary);
	}

	public static Optional<DepotType> typeAt(ServerLevel level, BlockPos position) {
		return inspect(level, position).map(CurrentContainer::type);
	}

	public static Optional<CommunityDepot> findRegisteredAt(ServerLevel level, BlockPos position) {
		return findRegistered(level, position, false);
	}

	public static Optional<CommunityDepot> findRegisteredAtIncludingInvalid(ServerLevel level, BlockPos position) {
		return findRegistered(level, position, true);
	}

	public static Optional<Container> open(ServerLevel level, UUID depotId) {
		CommunityDepot depot = storage(level.getServer()).get(depotId);
		if (depot == null || depot.state() != DepotState.ACTIVE
				|| !depot.dimension().equals(level.dimension())) {
			return Optional.empty();
		}
		return resolveActive(level, depot).map(CurrentContainer::container);
	}

	public static RegistrationResult register(ServerLevel level, UUID communityId, BlockPos position) {
		Community community = CommunityManager.findById(level.getServer(), communityId).orElse(null);
		if (community == null) {
			return new RegistrationResult(RegistrationStatus.COMMUNITY_NOT_FOUND, null);
		}
		if (community.state() != CommunityState.ACTIVE) {
			return new RegistrationResult(RegistrationStatus.COMMUNITY_INACTIVE, null);
		}
		if (!community.dimension().equals(level.dimension())) {
			return new RegistrationResult(RegistrationStatus.DIMENSION_MISMATCH, null);
		}

		CurrentContainer current = inspect(level, position).orElse(null);
		if (current == null) {
			return new RegistrationResult(RegistrationStatus.UNSUPPORTED_CONTAINER, null);
		}
		if (!community.isWithinTerritory(current.primary())
				|| current.secondary() != null && !community.isWithinTerritory(current.secondary())) {
			return new RegistrationResult(RegistrationStatus.OUTSIDE_TERRITORY, null);
		}

		CommunityDepotStorage storage = storage(level.getServer());
		for (CommunityDepot depot : storage.all()) {
			if (depot.state() != DepotState.ACTIVE || !depot.dimension().equals(level.dimension())) {
				continue;
			}
			if (overlaps(depot, current)) {
				if (resolveActive(level, depot).isPresent()) {
					return new RegistrationResult(
							depot.communityId().equals(communityId)
									? RegistrationStatus.ALREADY_REGISTERED
									: RegistrationStatus.OWNED_BY_OTHER_COMMUNITY,
							depot
					);
				}
			}
		}

		CommunityDepot depot = new CommunityDepot(
			UUID.randomUUID(),
			communityId,
			level.dimension(),
			current.primary(),
			current.secondary(),
			current.type()
		);
		storage.add(depot);
		return new RegistrationResult(RegistrationStatus.REGISTERED, depot);
	}

	public static RemovalResult remove(ServerLevel level, UUID communityId, UUID depotId) {
		CommunityDepot depot = storage(level.getServer()).get(depotId);
		if (depot == null) {
			return RemovalResult.NOT_FOUND;
		}
		if (!depot.communityId().equals(communityId)) {
			return RemovalResult.OWNED_BY_OTHER_COMMUNITY;
		}
		if (!depot.dimension().equals(level.dimension())) {
			return RemovalResult.DIMENSION_MISMATCH;
		}
		storage(level.getServer()).remove(depotId);
		return RemovalResult.REMOVED;
	}

	public static Optional<DepositTarget> findAvailable(
			ServerLevel level,
			UUID communityId,
			Container workInventory,
			Set<BlockPos> excluded
	) {
		return findAvailable(level, communityId, workInventory, excluded, stack -> true);
	}

	public static Optional<DepositTarget> findAvailable(
			ServerLevel level,
			UUID communityId,
			Container workInventory,
			Set<BlockPos> excluded,
			Predicate<net.minecraft.world.item.ItemStack> allowed
	) {
		Community community = CommunityManager.findById(level.getServer(), communityId).orElse(null);
		if (community == null || community.state() != CommunityState.ACTIVE
				|| !community.dimension().equals(level.dimension())) {
			return Optional.empty();
		}

		return storage(level.getServer()).all().stream()
				.filter(depot -> depot.state() == DepotState.ACTIVE)
				.filter(depot -> depot.communityId().equals(communityId))
				.filter(depot -> depot.dimension().equals(level.dimension()))
				.map(depot -> resolveActive(level, depot).map(current -> new ResolvedDepot(depot, current)))
				.flatMap(Optional::stream)
				.filter(resolved -> !excluded.contains(resolved.current().primary()))
				.filter(resolved -> CommunityItemTransfer.canReceiveAny(
						workInventory, resolved.current().container(), allowed
				))
				.map(resolved -> new DepositTarget(resolved.depot().id(), resolved.current().primary()))
				.findFirst();
	}

	public static int deposit(ServerLevel level, UUID communityId, UUID depotId, Container workInventory) {
		return deposit(level, communityId, depotId, workInventory, stack -> true);
	}

	public static int deposit(
			ServerLevel level,
			UUID communityId,
			UUID depotId,
			Container workInventory,
			Predicate<net.minecraft.world.item.ItemStack> allowed
	) {
		Community community = CommunityManager.findById(level.getServer(), communityId).orElse(null);
		CommunityDepot depot = storage(level.getServer()).get(depotId);
		if (community == null || community.state() != CommunityState.ACTIVE || depot == null
				|| depot.state() != DepotState.ACTIVE || !depot.communityId().equals(communityId)
				|| !depot.dimension().equals(level.dimension())) {
			return 0;
		}
		return resolveActive(level, depot)
				.map(current -> CommunityItemTransfer.transferAll(workInventory, current.container(), allowed))
				.orElse(0);
	}

	public static Optional<DepotInfo> info(ServerLevel level, CommunityDepot depot) {
		if (depot.state() != DepotState.ACTIVE) {
			return Optional.empty();
		}
		return resolveActive(level, depot).map(current -> {
			int occupied = 0;
			for (int slot = 0; slot < current.container().getContainerSize(); slot++) {
				if (!current.container().getItem(slot).isEmpty()) {
					occupied++;
				}
			}
			return new DepotInfo(depot.type(), occupied, current.container().getContainerSize());
		});
	}

	private static Optional<CommunityDepot> findRegistered(ServerLevel level, BlockPos position, boolean includeInvalid) {
		CurrentContainer clicked = inspect(level, position).orElse(null);
		for (CommunityDepot depot : storage(level.getServer()).all()) {
			if (!depot.dimension().equals(level.dimension())) {
				continue;
			}
			boolean matches = depot.hasPosition(position)
					|| clicked != null && overlaps(depot, clicked);
			if (!matches) {
				continue;
			}
			if (depot.state() == DepotState.INVALID) {
				if (includeInvalid) {
					return Optional.of(depot);
				}
				continue;
			}
			if (resolveActive(level, depot).isPresent()) {
				return Optional.of(depot);
			}
		}
		return Optional.empty();
	}

	private static Optional<CurrentContainer> resolveActive(ServerLevel level, CommunityDepot depot) {
		if (depot.state() != DepotState.ACTIVE || !depot.dimension().equals(level.dimension())) {
			return Optional.empty();
		}

		CurrentContainer current = inspect(level, depot.primary()).orElse(null);
		if (current == null && depot.secondary().isPresent()) {
			current = inspect(level, depot.secondary().orElseThrow()).orElse(null);
		}
		if (current == null || current.type() != depot.type()) {
			depot.markInvalid();
			storage(level.getServer()).markDirty();
			return Optional.empty();
		}

		if (current.primary().equals(depot.primary())
				|| depot.hasPosition(current.primary())
				|| current.secondary() != null && depot.hasPosition(current.secondary())) {
			if (!samePositions(depot, current)) {
				depot.reconcile(current.primary(), current.secondary());
				storage(level.getServer()).markDirty();
			}
			return Optional.of(current);
		}

		depot.markInvalid();
		storage(level.getServer()).markDirty();
		return Optional.empty();
	}

	private static boolean samePositions(CommunityDepot depot, CurrentContainer current) {
		return depot.primary().equals(current.primary())
				&& depot.secondary().equals(Optional.ofNullable(current.secondary()));
	}

	private static boolean overlaps(CommunityDepot depot, CurrentContainer current) {
		return depot.hasPosition(current.primary())
				|| current.secondary() != null && depot.hasPosition(current.secondary());
	}

	private static Optional<CurrentContainer> inspect(ServerLevel level, BlockPos position) {
		if (!level.isLoaded(position)) {
			return Optional.empty();
		}
		BlockState state = level.getBlockState(position);
		if (state.is(Blocks.CHEST) && state.getBlock() instanceof ChestBlock chest) {
			ChestType chestType = state.getValue(ChestBlock.TYPE);
			if (chestType != ChestType.SINGLE) {
				BlockPos partner = ChestBlock.getConnectedBlockPos(position, state);
				if (!level.isLoaded(partner) || !isDoubleChestHalf(level, partner, position)) {
					return Optional.empty();
				}
				BlockPos primary = compare(position, partner) <= 0 ? position : partner;
				Container container = ChestBlock.getContainer(chest, state, level, position, false);
				if (container == null) {
					return Optional.empty();
				}
				return Optional.of(new CurrentContainer(
						DepotType.CHEST,
						primary.immutable(),
						(primary.equals(position) ? partner : position).immutable(),
						container
				));
			}
			if (level.getBlockEntity(position) instanceof Container container) {
				return Optional.of(new CurrentContainer(DepotType.CHEST, position.immutable(), null, container));
			}
		}
		if (state.is(Blocks.BARREL) && state.getBlock() instanceof BarrelBlock
				&& level.getBlockEntity(position) instanceof Container container) {
			return Optional.of(new CurrentContainer(DepotType.BARREL, position.immutable(), null, container));
		}
		return Optional.empty();
	}

	private static boolean isDoubleChestHalf(ServerLevel level, BlockPos partner, BlockPos original) {
		BlockState partnerState = level.getBlockState(partner);
		return partnerState.is(Blocks.CHEST)
				&& partnerState.getBlock() instanceof ChestBlock
				&& partnerState.getValue(ChestBlock.TYPE) != ChestType.SINGLE
				&& ChestBlock.getConnectedBlockPos(partner, partnerState).equals(original);
	}

	private static int compare(BlockPos first, BlockPos second) {
		if (first.getX() != second.getX()) {
			return Integer.compare(first.getX(), second.getX());
		}
		if (first.getY() != second.getY()) {
			return Integer.compare(first.getY(), second.getY());
		}
		return Integer.compare(first.getZ(), second.getZ());
	}

	public record DepositTarget(UUID depotId, BlockPos position) {
	}

	public record DepotInfo(DepotType type, int occupiedSlots, int capacity) {
	}

	private record CurrentContainer(
			DepotType type,
			BlockPos primary,
			BlockPos secondary,
			Container container
	) {
	}

	private record ResolvedDepot(CommunityDepot depot, CurrentContainer current) {
	}

	public record RegistrationResult(RegistrationStatus status, CommunityDepot depot) {
	}

	public enum RegistrationStatus {
		REGISTERED,
		ALREADY_REGISTERED,
		COMMUNITY_NOT_FOUND,
		COMMUNITY_INACTIVE,
		DIMENSION_MISMATCH,
		OUTSIDE_TERRITORY,
		UNSUPPORTED_CONTAINER,
		OWNED_BY_OTHER_COMMUNITY
	}

	public enum RemovalResult {
		REMOVED,
		NOT_FOUND,
		OWNED_BY_OTHER_COMMUNITY,
		DIMENSION_MISMATCH
	}
}
