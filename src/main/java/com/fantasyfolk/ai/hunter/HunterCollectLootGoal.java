package com.fantasyfolk.ai.hunter;

import java.util.EnumSet;
import java.util.Optional;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.level.pathfinder.Path;

import com.fantasyfolk.inventory.WorkInventoryHolder;
import com.fantasyfolk.profession.ProfessionHolder;

/** Collects only newly spawned drops associated with the Hunter's latest kill. */
public final class HunterCollectLootGoal<T extends PathfinderMob & ProfessionHolder & WorkInventoryHolder & HunterStateHolder>
		extends Goal {
	private static final double SPEED = 1.0D;
	private static final int MAX_COLLECTION_TICKS = 140;
	private static final int MAX_PATH_FAILURES = 3;

	private final HunterRuntime<T> runtime;
	private Path path;
	private int ticks;
	private int pathFailures;
	private boolean finished;

	public HunterCollectLootGoal(HunterRuntime<T> runtime) {
		this.runtime = runtime;
		setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
	}

	@Override
	public boolean canUse() {
		T mob = runtime.mob();
		return runtime.hasPendingLoot()
				&& !runtime.isInDanger()
				&& mob.getTarget() == null
				&& !(mob.level().isClientSide())
				&& runtime.area().isPresent();
	}

	@Override
	public void start() {
		ticks = 0;
		pathFailures = 0;
		finished = false;
		path = null;
		moveTowardsLoot();
	}

	@Override
	public boolean canContinueToUse() {
		return !finished
				&& ticks < MAX_COLLECTION_TICKS
				&& runtime.hasPendingLoot()
				&& !runtime.isInDanger()
				&& runtime.mob().getTarget() == null;
	}

	@Override
	public void tick() {
		ticks++;
		Optional<ItemEntity> candidate = runtime.findLootCandidate();
		if (candidate.isPresent()) {
			ItemEntity item = candidate.orElseThrow();
			runtime.mob().lookAt(item, 30.0F, 30.0F);
			if (runtime.mob().distanceToSqr(item) <= 6.25D) {
				if (runtime.collect(item) == 0) {
					finished = true;
				}
				return;
			}
			if (path == null || runtime.mob().getNavigation().isDone()) {
				path = createPath(item);
				if (path == null || !runtime.mob().getNavigation().moveTo(path, SPEED)) {
					registerPathFailure();
				} else {
					pathFailures = 0;
				}
			} else if (runtime.mob().getNavigation().isStuck()) {
					registerPathFailure();
			}
			return;
		}

		if (runtime.shouldFinishLootCollection()) {
			runtime.clearLoot();
			finished = true;
			return;
		}

		if (path == null || runtime.mob().getNavigation().isDone()) {
			moveTowardsLoot();
		}
		if (runtime.mob().getNavigation().isStuck()) {
			registerPathFailure();
		}
	}

	@Override
	public void stop() {
		runtime.mob().getNavigation().stop();
		if (!runtime.hasPendingLoot() || ticks >= MAX_COLLECTION_TICKS || finished) {
			runtime.clearLoot();
		}
		path = null;
	}

	@Override
	public boolean requiresUpdateEveryTick() {
		return true;
	}

	private void moveTowardsLoot() {
		if (!(runtime.mob().level() instanceof ServerLevel)) {
			finished = true;
			return;
		}
		path = createPath(runtime.lootPosition());
		if (path == null || !runtime.mob().getNavigation().moveTo(path, SPEED)) {
			registerPathFailure();
		} else {
			pathFailures = 0;
		}
	}

	private void registerPathFailure() {
		path = null;
		pathFailures++;
		finished = pathFailures >= MAX_PATH_FAILURES;
	}

	private Path createPath(ItemEntity item) {
		return createPath(item.blockPosition());
	}

	private Path createPath(net.minecraft.core.BlockPos position) {
		Path candidate = runtime.mob().getNavigation().createPath(position, 0);
		return HunterNavigation.staysInside(candidate, runtime.area().orElseThrow()) ? candidate : null;
	}
}
