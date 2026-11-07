package com.fantasyfolk.ai;

import java.util.EnumSet;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.entity.ai.util.GoalUtils;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.level.pathfinder.Path;

import com.fantasyfolk.community.Community;
import com.fantasyfolk.community.CommunityDepotManager;
import com.fantasyfolk.community.CommunityItemTransfer;
import com.fantasyfolk.community.CommunityManager;
import com.fantasyfolk.community.CommunityState;
import com.fantasyfolk.entity.Goblin;
import com.fantasyfolk.inventory.WorkInventoryHolder;

/** Moves a work-inventory holder to a registered community depot and deposits resources. */
public final class CommunityDepositGoal<T extends PathfinderMob & WorkInventoryHolder> extends Goal {
	private static final double SPEED = 1.0D;
	private static final int MAX_TRAVEL_TICKS = 240;
	private static final int MAX_PATH_FAILURES = 3;
	private static final int RETRY_INTERVAL_TICKS = 60;
	private static final double MAX_INTERACTION_DISTANCE_SQUARED = 6.25D;

	private final T mob;
	private Path path;
	private CommunityDepotManager.DepositTarget target;
	private UUID communityId;
	private Community cachedCommunity;
	private int nextCommunityCheckTick;
	private int travelTicks;
	private int pathFailures;
	private int nextAttemptTick;
	private boolean finished;
	private final Set<net.minecraft.core.BlockPos> attempted = new HashSet<>();

	public CommunityDepositGoal(T mob) {
		this.mob = mob;
		setFlags(EnumSet.of(Flag.MOVE));
	}

	/**
	 * Used by profession Goals to decide whether delivery can actually take
	 * precedence over ordinary work. A registered container that cannot be
	 * reached must not permanently disable the profession's primary activity.
	 */
	public static boolean hasReachableDeposit(PathfinderMob mob) {
		if (!(mob.level() instanceof ServerLevel level) || !(mob instanceof WorkInventoryHolder holder)) {
			return false;
		}
		UUID communityId = holder.getCommunityId().orElse(null);
		if (communityId == null) {
			return false;
		}
		Community community = CommunityManager.findById(level.getServer(), communityId)
				.filter(found -> found.state() == CommunityState.ACTIVE)
				.filter(found -> found.dimension().equals(level.dimension()))
				.orElse(null);
		if (community == null) {
			return false;
		}
		CommunityDepotManager.DepositTarget target = CommunityDepotManager.findAvailable(
				level, communityId, holder.getWorkInventory(), Set.of(), holder::canDepositWorkItem
		).orElse(null);
		return target != null && createReachablePath(mob, level, community, target) != null;
	}

	@Override
	public boolean canUse() {
		if (mob.tickCount < nextAttemptTick || mob.isPassenger() || mob.getTarget() != null
				|| hasPendingHunterLoot()
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
		path = createPath(level, community, target);
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
		pathFailures = 0;
		finished = !mob.getNavigation().moveTo(path, SPEED);
	}

	@Override
	public boolean canContinueToUse() {
		return !finished
				&& travelTicks < MAX_TRAVEL_TICKS
				&& !mob.getWorkInventory().isEmpty()
				&& hasDepositableItems()
				&& !hasPendingHunterLoot()
				&& mob.getTarget() == null
				&& target != null
				&& cachedCommunity != null;
	}

	@Override
	public void tick() {
		ServerLevel level = (ServerLevel) mob.level();
		if (target == null || cachedCommunity == null) {
			finished = true;
			return;
		}

		travelTicks++;
		if (isAtInteractionPosition(level, target)) {
			int transferred = CommunityDepotManager.deposit(
					level,
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
			Community community = currentCommunity(level).orElse(null);
			target = community == null ? null : findTarget(level);
			path = community == null || target == null ? null : createPath(level, community, target);
			if (path == null || !mob.getNavigation().moveTo(path, SPEED)) {
				registerPathFailure();
			} else {
				pathFailures = 0;
			}
			return;
		}

		if (mob.getNavigation().isStuck()) {
			registerPathFailure();
			return;
		}
		if (mob.getNavigation().isDone()) {
			path = createPath(level, cachedCommunity, target);
			if (path == null || !mob.getNavigation().moveTo(path, SPEED)) {
				registerPathFailure();
			} else {
				pathFailures = 0;
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

	private boolean hasPendingHunterLoot() {
		return mob instanceof Goblin goblin && goblin.hasPendingHunterLoot();
	}

	private Path createPath(
			ServerLevel level,
			Community community,
			CommunityDepotManager.DepositTarget depot
	) {
		return createReachablePath(mob, level, community, depot);
	}

	private static Path createReachablePath(
			PathfinderMob mob,
			ServerLevel level,
			Community community,
			CommunityDepotManager.DepositTarget depot
	) {
		Set<BlockPos> candidates = depot.interactionPositions().stream()
				.filter(position -> isUsableInteractionPosition(mob, level, position))
				.collect(java.util.stream.Collectors.toUnmodifiableSet());
		if (candidates.isEmpty()) {
			return null;
		}

		Path candidate = mob.getNavigation().createPath(candidates, 1);
		if (candidate == null || !candidate.canReach() || candidate.getNodeCount() == 0
				|| candidate.getEndNode() == null
				|| !candidates.contains(candidate.getEndNode().asBlockPos())) {
			return null;
		}
		for (int index = 0; index < candidate.getNodeCount(); index++) {
			if (!community.isWithinTerritory(candidate.getNodePos(index))) {
				return null;
			}
		}
		return candidate;
	}

	private static boolean isUsableInteractionPosition(PathfinderMob mob, ServerLevel level, BlockPos position) {
		return level.isLoaded(position)
				&& Math.abs(position.getY() - mob.blockPosition().getY()) <= 10
				&& mob.getNavigation().isStableDestination(position)
				&& !GoalUtils.isWater(mob, position)
				&& !GoalUtils.isSolid(mob, position)
				&& !GoalUtils.hasMalus(mob, position);
	}

	private boolean isAtInteractionPosition(
			ServerLevel level,
			CommunityDepotManager.DepositTarget depot
	) {
		for (BlockPos containerPosition : depot.containerPositions()) {
			if (mob.distanceToSqr(
					containerPosition.getX() + 0.5D,
					containerPosition.getY() + 0.5D,
					containerPosition.getZ() + 0.5D
			) > MAX_INTERACTION_DISTANCE_SQUARED) {
				continue;
			}
			BlockHitResult hit = level.clip(new ClipContext(
					mob.getEyePosition(),
					Vec3.atCenterOf(containerPosition),
					ClipContext.Block.COLLIDER,
					ClipContext.Fluid.NONE,
					mob
			));
			if (hit.getType() == HitResult.Type.BLOCK && hit.getBlockPos().equals(containerPosition)) {
				return true;
			}
		}
		return false;
	}

	private void registerPathFailure() {
		path = null;
		pathFailures++;
		finished = pathFailures >= MAX_PATH_FAILURES;
	}
}
