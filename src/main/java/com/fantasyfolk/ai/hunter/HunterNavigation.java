package com.fantasyfolk.ai.hunter;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.util.GoalUtils;
import net.minecraft.world.entity.ai.util.LandRandomPos;
import net.minecraft.world.level.pathfinder.Path;
import net.minecraft.world.phys.Vec3;

/** Bounded vanilla-navigation helpers for hunting and retreating. */
final class HunterNavigation {
	private static final int DESTINATION_ATTEMPTS = 8;
	private static final int VERTICAL_RANGE = 7;

	private HunterNavigation() {
	}

	static Path findAreaPath(PathfinderMob mob, HuntingAreaContext area) {
		Vec3 center = Vec3.atBottomCenterOf(area.center());
		boolean outside = !area.contains(mob.blockPosition());
		for (int attempt = 0; attempt < DESTINATION_ATTEMPTS; attempt++) {
			Vec3 position = outside
					? LandRandomPos.getPosTowards(mob, area.radius(), VERTICAL_RANGE, center)
					: LandRandomPos.getPos(mob, area.radius(), VERTICAL_RANGE);
			if (position == null) {
				continue;
			}
			BlockPos candidate = BlockPos.containing(position);
			if (!area.contains(candidate) || !isSafeCandidate(mob, candidate)) {
				continue;
			}
			Path path = mob.getNavigation().createPath(candidate, 0);
			if (isReachable(path) && pathStaysInside(path, area)) {
				return path;
			}
		}
		return null;
	}

	static Path findAwayPath(PathfinderMob mob, LivingEntity threat) {
		for (int attempt = 0; attempt < DESTINATION_ATTEMPTS; attempt++) {
			Vec3 position = LandRandomPos.getPosAway(mob, 10, VERTICAL_RANGE, threat.position());
			if (position == null) {
				continue;
			}
			BlockPos candidate = BlockPos.containing(position);
			if (!isSafeCandidate(mob, candidate)) {
				continue;
			}
			Path path = mob.getNavigation().createPath(candidate, 0);
			if (isReachable(path)) {
				return path;
			}
		}
		return null;
	}

	static boolean canReachTarget(PathfinderMob mob, LivingEntity target, HuntingAreaContext area) {
		Path path = pathToTarget(mob, target, area);
		return isReachable(path) && pathStaysInside(path, area);
	}

	static Path pathToTarget(PathfinderMob mob, LivingEntity target, HuntingAreaContext area) {
		Path path = mob.getNavigation().createPath(target, 0);
		return isReachable(path) && pathStaysInside(path, area) ? path : null;
	}

	static boolean staysInside(Path path, HuntingAreaContext area) {
		return isReachable(path) && pathStaysInside(path, area);
	}

	private static boolean isSafeCandidate(PathfinderMob mob, BlockPos candidate) {
		return mob.level().isLoaded(candidate)
				&& Math.abs(candidate.getY() - mob.blockPosition().getY()) <= 10
				&& mob.getNavigation().isStableDestination(candidate)
				&& !GoalUtils.isWater(mob, candidate)
				&& !GoalUtils.isSolid(mob, candidate)
				&& !GoalUtils.hasMalus(mob, candidate);
	}

	private static boolean pathStaysInside(Path path, HuntingAreaContext area) {
		for (int index = 0; index < path.getNodeCount(); index++) {
			if (!area.contains(path.getNodePos(index))) {
				return false;
			}
		}
		return true;
	}

	private static boolean isReachable(Path path) {
		return path != null && path.canReach() && path.getNodeCount() > 0;
	}
}
