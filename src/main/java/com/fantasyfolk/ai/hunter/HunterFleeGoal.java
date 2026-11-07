package com.fantasyfolk.ai.hunter;

import java.util.EnumSet;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.level.pathfinder.Path;

import com.fantasyfolk.inventory.WorkInventoryHolder;
import com.fantasyfolk.profession.ProfessionHolder;

/** Simple bounded retreat that gives survival priority without adding offensive monster combat. */
public final class HunterFleeGoal<T extends PathfinderMob & ProfessionHolder & WorkInventoryHolder & HunterStateHolder>
		extends Goal {
	private static final double SPEED = 1.2D;
	private static final int MAX_FLEE_TICKS = 100;

	private final HunterRuntime<T> runtime;
	private Path path;
	private int ticks;

	public HunterFleeGoal(HunterRuntime<T> runtime) {
		this.runtime = runtime;
		setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
	}

	@Override
	public boolean canUse() {
		T mob = runtime.mob();
		if (!runtime.isHunterProfession() || mob.isPassenger() || !(mob.level() instanceof ServerLevel level)) {
			return false;
		}
		Entity threat = level.getEntities(
				mob,
				mob.getBoundingBox().inflate(8.0D),
				entity -> entity instanceof Enemy && entity.isAlive()
		).stream().min((first, second) -> Double.compare(
				mob.distanceToSqr(first), mob.distanceToSqr(second)
		)).orElse(null);
		if (threat == null) {
			return false;
		}
		path = HunterNavigation.findAwayPath(mob, (net.minecraft.world.entity.LivingEntity) threat);
		return path != null;
	}

	@Override
	public void start() {
		ticks = 0;
		runtime.mob().setTarget(null);
		runtime.mob().getNavigation().moveTo(path, SPEED);
	}

	@Override
	public boolean canContinueToUse() {
		return ticks < MAX_FLEE_TICKS
				&& runtime.isInDanger()
				&& !runtime.mob().getNavigation().isDone();
	}

	@Override
	public void tick() {
		ticks++;
		if (runtime.mob().getNavigation().isStuck()) {
			ticks = MAX_FLEE_TICKS;
		}
	}

	@Override
	public void stop() {
		runtime.mob().getNavigation().stop();
		path = null;
	}

	@Override
	public boolean requiresUpdateEveryTick() {
		return true;
	}
}
