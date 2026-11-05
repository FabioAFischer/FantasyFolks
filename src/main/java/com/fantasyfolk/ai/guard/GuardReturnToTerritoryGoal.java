package com.fantasyfolk.ai.guard;

import java.util.EnumSet;
import java.util.Optional;

import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.level.pathfinder.Path;

import com.fantasyfolk.profession.ProfessionHolder;

/** Guides an operational Guard back toward its community without teleporting or loading chunks. */
public final class GuardReturnToTerritoryGoal<T extends PathfinderMob & ProfessionHolder> extends Goal {
	private static final double SPEED = 1.05D;
	private static final int MAX_PATH_TICKS = 200;

	private final GuardRuntime<T> runtime;
	private Path path;
	private int pathTicks;
	private int nextAttemptTick;

	public GuardReturnToTerritoryGoal(GuardRuntime<T> runtime) {
		this.runtime = runtime;
		setFlags(EnumSet.of(Flag.MOVE));
	}

	@Override
	public boolean canUse() {
		T mob = runtime.mob();
		if (mob.tickCount < nextAttemptTick || mob.getTarget() != null || mob.isPassenger()) {
			return false;
		}
		Optional<GuardCommunityContext> community = runtime.community();
		if (community.isEmpty() || community.get().contains(mob.blockPosition())) {
			return false;
		}

		path = GuardNavigation.findReturnPath(mob, community.get());
		if (path == null) {
			nextAttemptTick = mob.tickCount + 40;
			return false;
		}
		return true;
	}

	@Override
	public void start() {
		pathTicks = 0;
		if (!runtime.mob().getNavigation().moveTo(path, SPEED)) {
			pathTicks = MAX_PATH_TICKS;
		}
	}

	@Override
	public boolean canContinueToUse() {
		T mob = runtime.mob();
		return pathTicks < MAX_PATH_TICKS
				&& mob.getTarget() == null
				&& !mob.getNavigation().isDone()
				&& runtime.community().filter(context -> !context.contains(mob.blockPosition())).isPresent();
	}

	@Override
	public void tick() {
		pathTicks++;
		if (runtime.mob().getNavigation().isStuck()) {
			pathTicks = MAX_PATH_TICKS;
		}
	}

	@Override
	public void stop() {
		T mob = runtime.mob();
		mob.getNavigation().stop();
		path = null;
		nextAttemptTick = mob.tickCount + 20;
	}

	@Override
	public boolean requiresUpdateEveryTick() {
		return true;
	}
}
