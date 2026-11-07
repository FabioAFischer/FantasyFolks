package com.fantasyfolk.ai;

import java.util.EnumSet;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.level.pathfinder.Path;

import com.fantasyfolk.community.Community;
import com.fantasyfolk.community.CommunityDepotManager;
import com.fantasyfolk.community.CommunityItemTransfer;
import com.fantasyfolk.community.CommunityManager;
import com.fantasyfolk.community.CommunityState;
import com.fantasyfolk.inventory.WorkInventoryHolder;

/** Moves a work-inventory holder to a registered community depot and deposits resources. */
public final class CommunityDepositGoal<T extends PathfinderMob & WorkInventoryHolder> extends Goal {
	private static final double SPEED = 1.0D;
	private static final int MAX_TRAVEL_TICKS = 240;
	private static final int RETRY_INTERVAL_TICKS = 60;

	private final T mob;
	private Path path;
	private CommunityDepotManager.DepositTarget target;
	private UUID communityId;
	private Community cachedCommunity;
	private int nextCommunityCheckTick;
	private int travelTicks;
	private int nextAttemptTick;
	private boolean finished;
	private final Set<net.minecraft.core.BlockPos> attempted = new HashSet<>();

	public CommunityDepositGoal(T mob) {
		this.mob = mob;
		setFlags(EnumSet.of(Flag.MOVE));
	}

	@Override
	public boolean canUse() {
		if (mob.tickCount < nextAttemptTick || mob.isPassenger() || mob.getTarget() != null
				|| mob.getWorkInventory().isEmpty()
				|| !hasDepositableItems()
				|| !(mob.level() instanceof ServerLevel level)) {
			return false;
		}

		Community community = currentCommunity(level).orElse(null);
		if (community == null) {
			nextAttemptTick = mob.tickCount + RETRY_INTERVAL_TICKS;
			return false;
		}
		communityId = community.id();
		attempted.clear();
		target = findTarget(level);
		if (target == null) {
			nextAttemptTick = mob.tickCount + RETRY_INTERVAL_TICKS;
			return false;
		}
		path = createPath(level, community, target.position());
		if (path == null) {
			attempted.add(target.position());
			nextAttemptTick = mob.tickCount + RETRY_INTERVAL_TICKS;
			return false;
		}
		return true;
	}

	@Override
	public void start() {
		travelTicks = 0;
		finished = !mob.getNavigation().moveTo(path, SPEED);
	}

	@Override
	public boolean canContinueToUse() {
		return !finished
				&& travelTicks < MAX_TRAVEL_TICKS
				&& !mob.getWorkInventory().isEmpty()
				&& hasDepositableItems()
				&& mob.getTarget() == null
				&& cachedCommunity != null;
	}

	@Override
	public void tick() {
		if (mob.getNavigation().isStuck()) {
			finished = true;
			return;
		}
		travelTicks++;
		if (mob.distanceToSqr(target.position().getX() + 0.5D, target.position().getY(), target.position().getZ() + 0.5D) <= 9.0D
				|| mob.getNavigation().isDone()) {
			int transferred = CommunityDepotManager.deposit(
					(ServerLevel) mob.level(),
					communityId,
					target.depotId(),
					mob.getWorkInventory(),
					mob::canDepositWorkItem
			);
			if (transferred > 0 && !hasDepositableItems()) {
				finished = true;
				return;
			}
			attempted.add(target.position());
			Community community = currentCommunity((ServerLevel) mob.level()).orElse(null);
			target = community == null ? null : findTarget((ServerLevel) mob.level());
			path = community == null || target == null ? null : createPath((ServerLevel) mob.level(), community, target.position());
			if (path == null || !mob.getNavigation().moveTo(path, SPEED)) {
				finished = true;
			}
		}
	}

	@Override
	public void stop() {
		mob.getNavigation().stop();
		path = null;
		target = null;
		communityId = null;
		nextAttemptTick = mob.tickCount + (mob.getWorkInventory().isEmpty() ? 20 : RETRY_INTERVAL_TICKS);
	}

	@Override
	public boolean requiresUpdateEveryTick() {
		return true;
	}

	private Optional<Community> currentCommunity(ServerLevel level) {
		if (mob.tickCount < nextCommunityCheckTick) {
			return Optional.ofNullable(cachedCommunity);
		}
		nextCommunityCheckTick = mob.tickCount + 20;
		cachedCommunity = mob.getCommunityId()
				.flatMap(id -> CommunityManager.findById(level.getServer(), id))
				.filter(community -> community.state() == CommunityState.ACTIVE)
				.filter(community -> community.dimension().equals(level.dimension()))
				.orElse(null);
		return Optional.ofNullable(cachedCommunity);
	}

	private CommunityDepotManager.DepositTarget findTarget(ServerLevel level) {
		return CommunityDepotManager.findAvailable(
				level, communityId, mob.getWorkInventory(), attempted, mob::canDepositWorkItem
		).orElse(null);
	}

	private boolean hasDepositableItems() {
		for (int slot = 0; slot < mob.getWorkInventory().getContainerSize(); slot++) {
			var stack = mob.getWorkInventory().getItem(slot);
			if (!stack.isEmpty() && mob.canDepositWorkItem(stack)) {
				return true;
			}
		}
		return false;
	}

	private Path createPath(ServerLevel level, Community community, net.minecraft.core.BlockPos position) {
		if (!level.isLoaded(position)) {
			return null;
		}
		Path candidate = mob.getNavigation().createPath(position, 1);
		if (candidate == null || !candidate.canReach() || candidate.getNodeCount() == 0) {
			return null;
		}
		for (int index = 0; index < candidate.getNodeCount(); index++) {
			if (!community.isWithinTerritory(candidate.getNodePos(index))) {
				return null;
			}
		}
		return candidate;
	}
}
