package com.fantasyfolk.ai.guard;

import java.util.Comparator;
import java.util.EnumSet;
import java.util.List;
import java.util.Optional;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.goal.target.TargetGoal;

import com.fantasyfolk.profession.ProfessionHolder;

/** Periodically selects the nearest allow-listed hostile inside community territory. */
public final class GuardTargetGoal<T extends PathfinderMob & ProfessionHolder> extends TargetGoal {
	public static final double DETECTION_RADIUS = 12.0D;
	private static final int SCAN_INTERVAL_TICKS = 20;

	private final GuardRuntime<T> runtime;
	private LivingEntity candidate;
	private int nextScanTick;

	public GuardTargetGoal(GuardRuntime<T> runtime) {
		super(runtime.mob(), true, false);
		this.runtime = runtime;
		setFlags(EnumSet.of(Flag.TARGET));
		setUnseenMemoryTicks(40);
	}

	@Override
	public boolean canUse() {
		T mob = runtime.mob();
		if (mob.tickCount < nextScanTick || mob.getTarget() != null) {
			return false;
		}
		nextScanTick = mob.tickCount + SCAN_INTERVAL_TICKS;
		Optional<GuardCommunityContext> community = runtime.community();
		if (community.isEmpty() || !(mob.level() instanceof ServerLevel level)) {
			return false;
		}

		List<Entity> entities = level.getEntities(
				mob,
				mob.getBoundingBox().inflate(DETECTION_RADIUS),
				entity -> entity instanceof LivingEntity living
						&& runtime.isValidTarget(living, false)
						&& mob.getSensing().hasLineOfSight(living)
		);
		candidate = entities.stream()
				.map(LivingEntity.class::cast)
				.sorted(Comparator.comparingDouble(mob::distanceToSqr))
				.filter(target -> GuardNavigation.canReach(mob, target))
				.findFirst()
				.orElse(null);
		return candidate != null;
	}

	@Override
	public void start() {
		targetMob = candidate;
		mob.setTarget(candidate);
		super.start();
	}

	@Override
	public boolean canContinueToUse() {
		LivingEntity target = mob.getTarget();
		return target != null && runtime.isValidTarget(target, false) && super.canContinueToUse();
	}

	@Override
	public void stop() {
		candidate = null;
		super.stop();
	}
}
