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

/** Prioritizes recent attackers of the Guard, linked inhabitants and territorial villagers. */
public final class GuardDefendAllyGoal<T extends PathfinderMob & ProfessionHolder> extends TargetGoal {
	private static final int SCAN_INTERVAL_TICKS = 10;
	private static final int RECENT_ATTACK_TICKS = 60;
	private static final double PROTECTION_SCAN_RADIUS = 12.0D;
	private static final double MAX_ATTACKER_DISTANCE_SQUARED = 16.0D * 16.0D;

	private final GuardRuntime<T> runtime;
	private LivingEntity candidate;
	private int nextScanTick;

	public GuardDefendAllyGoal(GuardRuntime<T> runtime) {
		super(runtime.mob(), true, false);
		this.runtime = runtime;
		setFlags(EnumSet.of(Flag.TARGET));
		setUnseenMemoryTicks(60);
	}

	@Override
	public boolean canUse() {
		T mob = runtime.mob();
		if (mob.tickCount < nextScanTick) {
			return false;
		}
		nextScanTick = mob.tickCount + SCAN_INTERVAL_TICKS;
		Optional<GuardCommunityContext> community = runtime.community();
		if (community.isEmpty() || !(mob.level() instanceof ServerLevel level)) {
			return false;
		}

		candidate = recentAllowedAttacker(mob);
		if (candidate == null) {
			List<Entity> nearby = level.getEntities(
					mob,
					mob.getBoundingBox().inflate(PROTECTION_SCAN_RADIUS),
					entity -> entity instanceof LivingEntity living
							&& runtime.isProtectedAlly(living, community.get())
			);
			candidate = nearby.stream()
					.map(LivingEntity.class::cast)
					.map(this::recentAllowedAttacker)
					.filter(java.util.Objects::nonNull)
					.min(Comparator.comparingDouble(mob::distanceToSqr))
					.orElse(null);
		}

		return candidate != null && GuardNavigation.canReach(mob, candidate);
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
		return target != null && runtime.isValidTarget(target, true) && super.canContinueToUse();
	}

	@Override
	public void stop() {
		candidate = null;
		super.stop();
	}

	private LivingEntity recentAllowedAttacker(LivingEntity protectedEntity) {
		LivingEntity attacker = protectedEntity.getLastHurtByMob();
		int elapsed = protectedEntity.tickCount - protectedEntity.getLastHurtByMobTimestamp();
		if (attacker == null
				|| elapsed < 0
				|| elapsed > RECENT_ATTACK_TICKS
				|| runtime.mob().distanceToSqr(attacker) > MAX_ATTACKER_DISTANCE_SQUARED
				|| !runtime.isValidTarget(attacker, true)) {
			return null;
		}
		return attacker;
	}
}
