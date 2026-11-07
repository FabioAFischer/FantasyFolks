package com.fantasyfolk.ai.guard;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.navigation.PathNavigation;
import net.minecraft.world.entity.ai.util.GoalUtils;
import net.minecraft.world.entity.ai.util.LandRandomPos;
import net.minecraft.world.level.pathfinder.Path;
import net.minecraft.world.phys.Vec3;

/** Bounded path selection helpers shared by Guard movement Goals. */
final class GuardNavigation {
	private static final int DESTINATION_ATTEMPTS = 8;
	private static final int PATROL_VERTICAL_RANGE = 7;
	private static final int RETURN_HORIZONTAL_STEP = 16;
	private static final int RETURN_VERTICAL_RANGE = 8;

	private GuardNavigation() {
	}

	static Path findPatrolPath(PathfinderMob mob, GuardCommunityContext community) {
		int radius = community.patrolRadius();
		if (radius <= 0) {
			return null;
		}

		for (int attempt = 0; attempt < DESTINATION_ATTEMPTS; attempt++) {
			Vec3 position = LandRandomPos.getPos(mob, radius, PATROL_VERTICAL_RANGE);
			if (position == null) {
				continue;
			}
			BlockPos candidate = BlockPos.containing(position);
			if (!community.containsPatrolPosition(candidate) || !isSafeCandidate(mob, candidate)) {
				continue;
			}

			Path path = mob.getNavigation().createPath(candidate, 0);
			if (isReachable(path) && pathStaysInside(path, community)) {
				return path;
			}
		}
		return null;
	}

	static Path findReturnPath(PathfinderMob mob, GuardCommunityContext community) {
		Vec3 center = Vec3.atBottomCenterOf(community.center());
		double currentDistance = horizontalDistanceSquared(mob.blockPosition(), community.center());
		for (int attempt = 0; attempt < DESTINATION_ATTEMPTS; attempt++) {
			Vec3 position = LandRandomPos.getPosTowards(
					mob,
					RETURN_HORIZONTAL_STEP,
					RETURN_VERTICAL_RANGE,
					center
			);
			if (position == null) {
				continue;
			}
			BlockPos candidate = BlockPos.containing(position);
			if (horizontalDistanceSquared(candidate, community.center()) >= currentDistance
					|| !isSafeCandidate(mob, candidate)) {
				continue;
			}

			Path path = mob.getNavigation().createPath(candidate, 0);
			if (isReachable(path)) {
				return path;
			}
		}
		return null;
	}

	static boolean canReach(PathfinderMob mob, LivingEntity target) {
		if (mob.isWithinMeleeAttackRange(target)) {
			return true;
		}
		Path path = mob.getNavigation().createPath(target, 0);
		return isReachable(path);
	}

	private static boolean isSafeCandidate(PathfinderMob mob, BlockPos candidate) {
		PathNavigation navigation = mob.getNavigation();
		return mob.level().isLoaded(candidate)
				&& Math.abs(candidate.getY() - mob.blockPosition().getY()) <= 10
				&& navigation.isStableDestination(candidate)
				&& !GoalUtils.isWater(mob, candidate)
				&& !GoalUtils.isSolid(mob, candidate)
				&& !GoalUtils.hasMalus(mob, candidate);
	}

	private static boolean pathStaysInside(Path path, GuardCommunityContext community) {
		for (int index = 0; index < path.getNodeCount(); index++) {
			if (!community.contains(path.getNodePos(index))) {
				return false;
			}
		}
		return true;
	}

	private static boolean isReachable(Path path) {
		return path != null && path.canReach() && path.getNodeCount() > 0;
	}

	private static double horizontalDistanceSquared(BlockPos first, BlockPos second) {
		long dx = first.getX() - (long) second.getX();
		long dz = first.getZ() - (long) second.getZ();
		return dx * dx + dz * dz;
	}
}
