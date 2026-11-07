package com.fantasyfolk.ai.hunter;

import java.util.EnumSet;

import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.level.pathfinder.Path;

import com.fantasyfolk.inventory.WorkInventoryHolder;
import com.fantasyfolk.profession.ProfessionHolder;

/** Returns a Hunter to a safe loaded position in the configured area. */
public final class HunterReturnToHuntingAreaGoal<T extends PathfinderMob & ProfessionHolder & WorkInventoryHolder & HunterStateHolder>
		extends Goal {
	private static final double SPEED = 0.9D;
	private static final int MAX_TRAVEL_TICKS = 220;

	private final HunterRuntime<T> runtime;
	private Path path;
	private int travelTicks;
	private int nextAttemptTick;
	private boolean finished;

	public HunterReturnToHuntingAreaGoal(HunterRuntime<T> runtime) {
		this.runtime = runtime;
		setFlags(EnumSet.of(Flag.MOVE));
	}

	@Override
	public boolean canUse() {
		T mob = runtime.mob();
		if (mob.tickCount < nextAttemptTick || mob.getTarget() != null || mob.isPassenger()
				|| runtime.hasPendingLoot() || !runtime.canWork()) {
			return false;
		}
		HuntingAreaContext area = runtime.area().orElse(null);
		if (area == null) {
			return false;
		}
		path = HunterNavigation.findAreaPath(mob, area);
		if (path == null) {
			nextAttemptTick = mob.tickCount + 40;
			return false;
		}
		return true;
	}

	@Override
	public void start() {
		travelTicks = 0;
		finished = !runtime.mob().getNavigation().moveTo(path, SPEED);
	}

	@Override
	public boolean canContinueToUse() {
		T mob = runtime.mob();
		return !finished
				&& travelTicks < MAX_TRAVEL_TICKS
				&& mob.getTarget() == null
				&& !runtime.hasPendingLoot()
				&& !runtime.isInDanger()
				&& runtime.area().isPresent();
	}

	@Override
	public void tick() {
		travelTicks++;
		if (runtime.mob().getNavigation().isStuck() || runtime.mob().getNavigation().isDone()) {
			finished = true;
		}
	}

	@Override
	public void stop() {
		runtime.mob().getNavigation().stop();
		path = null;
		finished = false;
		nextAttemptTick = runtime.mob().tickCount + 20 + runtime.mob().getRandom().nextInt(41);
	}

	@Override
	public boolean requiresUpdateEveryTick() {
		return true;
	}
}
