package com.fantasyfolk.ai.guard;

import java.util.EnumSet;
import java.util.Optional;

import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.level.pathfinder.Path;

import com.fantasyfolk.profession.ProfessionHolder;

/** Chooses bounded, reachable patrol destinations and pauses briefly at each one. */
public final class GuardPatrolGoal<T extends PathfinderMob & ProfessionHolder> extends Goal {
	private static final double SPEED = 0.9D;
	private static final int MAX_TRAVEL_TICKS = 200;

	private final GuardRuntime<T> runtime;
	private Path path;
	private int travelTicks;
	private int waitTicks = -1;
	private int nextAttemptTick;
	private boolean finished;

	public GuardPatrolGoal(GuardRuntime<T> runtime) {
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
		if (community.isEmpty() || !community.get().contains(mob.blockPosition())) {
			return false;
		}

		path = GuardNavigation.findPatrolPath(mob, community.get());
		if (path == null) {
			nextAttemptTick = mob.tickCount + 40;
			return false;
		}
		return true;
	}

	@Override
	public void start() {
		travelTicks = 0;
		waitTicks = -1;
		finished = !runtime.mob().getNavigation().moveTo(path, SPEED);
	}

	@Override
	public boolean canContinueToUse() {
		T mob = runtime.mob();
		return !finished
				&& travelTicks < MAX_TRAVEL_TICKS
				&& mob.getTarget() == null
				&& runtime.community().filter(context -> context.contains(mob.blockPosition())).isPresent();
	}

	@Override
	public void tick() {
		T mob = runtime.mob();
		if (waitTicks >= 0) {
			if (waitTicks-- <= 0) {
				finished = true;
			}
			return;
		}

		travelTicks++;
		if (mob.getNavigation().isStuck()) {
			finished = true;
		} else if (mob.getNavigation().isDone()) {
			waitTicks = 30 + mob.getRandom().nextInt(41);
		}
	}

	@Override
	public void stop() {
		T mob = runtime.mob();
		mob.getNavigation().stop();
		path = null;
		finished = false;
		nextAttemptTick = mob.tickCount + 20 + mob.getRandom().nextInt(41);
	}

	@Override
	public boolean requiresUpdateEveryTick() {
		return true;
	}
}
