package com.fantasyfolk.ai.hunter;

import java.util.Comparator;
import java.util.EnumSet;
import java.util.List;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.goal.target.TargetGoal;

import com.fantasyfolk.inventory.WorkInventoryHolder;
import com.fantasyfolk.profession.ProfessionHolder;

/** Periodically selects the nearest eligible, loaded animal inside the configured area. */
public final class HunterTargetGoal<T extends PathfinderMob & ProfessionHolder & WorkInventoryHolder & HunterStateHolder>
		extends TargetGoal {
	private static final double DETECTION_RADIUS = 16.0D;
	private static final int SCAN_INTERVAL_TICKS = 20;

	private final HunterRuntime<T> runtime;
	private LivingEntity candidate;
	private int nextScanTick;

	public HunterTargetGoal(HunterRuntime<T> runtime) {
		super(runtime.mob(), true, false);
		this.runtime = runtime;
		setFlags(EnumSet.of(Flag.TARGET));
		setUnseenMemoryTicks(20);
	}

	@Override
	public boolean canUse() {
		T mob = runtime.mob();
		if (mob.tickCount < nextScanTick || mob.getTarget() != null || !runtime.canWork()
				|| !(mob.level() instanceof ServerLevel level)) {
			return false;
		}
		nextScanTick = mob.tickCount + SCAN_INTERVAL_TICKS;
		HuntingAreaContext area = runtime.area().orElse(null);
		if (area == null) {
			return false;
		}

		List<Entity> entities = level.getEntities(
				mob,
				mob.getBoundingBox().inflate(DETECTION_RADIUS),
				entity -> entity instanceof LivingEntity living
						&& runtime.isValidTarget(living)
						&& mob.getSensing().hasLineOfSight(living)
		);
		candidate = entities.stream()
				.map(LivingEntity.class::cast)
				.sorted(Comparator.comparingDouble(mob::distanceToSqr))
				.filter(target -> HunterNavigation.canReachTarget(mob, target, area))
				.findFirst()
				.orElse(null);
		return candidate != null;
	}

	@Override
	public void start() {
		targetMob = candidate;
		runtime.mob().setTarget(candidate);
		super.start();
	}

	@Override
	public boolean canContinueToUse() {
		LivingEntity target = runtime.mob().getTarget();
		return target != null && runtime.isValidTarget(target) && super.canContinueToUse();
	}

	@Override
	public void stop() {
		candidate = null;
		super.stop();
	}
}
