package com.fantasyfolk.ai.hunter;

import java.util.EnumSet;

import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.level.pathfinder.Path;

import com.fantasyfolk.FantasyFolk;
import com.fantasyfolk.inventory.WorkInventoryHolder;
import com.fantasyfolk.profession.ProfessionHolder;

/** Bounded bow routine using real arrows and the bow stack equipped by the Goblin. */
public final class HunterRangedAttackGoal<T extends PathfinderMob & ProfessionHolder & WorkInventoryHolder & HunterStateHolder>
		extends Goal {
	private static final double SPEED = 1.0D;
	private static final double MIN_RANGE = 4.0D;
	private static final double IDEAL_RANGE = 10.0D;
	private static final int DRAW_TICKS = 20;
	private static final int SHOT_INTERVAL_TICKS = 35;
	private static final int MAX_COMBAT_TICKS = 360;
	private static final int MAX_UNREACHABLE_TICKS = 50;

	private final HunterRuntime<T> runtime;
	private int combatTicks;
	private int cooldown;
	private int drawTicks;
	private int unreachableTicks;
	private boolean gaveUp;

	public HunterRangedAttackGoal(HunterRuntime<T> runtime) {
		this.runtime = runtime;
		setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
	}

	@Override
	public boolean canUse() {
		LivingEntity target = runtime.mob().getTarget();
		HuntingAreaContext area = runtime.area().orElse(null);
		return target != null
				&& area != null
				&& runtime.canWork()
				&& HunterNavigation.canReachTarget(runtime.mob(), target, area);
	}

	@Override
	public void start() {
		combatTicks = 0;
		cooldown = 0;
		drawTicks = 0;
		unreachableTicks = 0;
		gaveUp = false;
		runtime.mob().setAggressive(true);
		FantasyFolk.LOGGER.debug(
				"Hunter diagnostics: ranged goal started hunter={} target={}",
				runtime.mob().getUUID(), runtime.mob().getTarget().getUUID()
		);
	}

	@Override
	public boolean canContinueToUse() {
		LivingEntity target = runtime.mob().getTarget();
		return !gaveUp
				&& combatTicks < MAX_COMBAT_TICKS
				&& target != null
				&& target.isAlive()
				&& runtime.isValidTarget(target)
				&& !runtime.mob().isPassenger();
	}

	@Override
	public void tick() {
		T mob = runtime.mob();
		LivingEntity target = mob.getTarget();
		if (target == null) {
			return;
		}
		runtime.completeShot(target);
		if (!target.isAlive() || !runtime.isValidTarget(target)) {
			mob.setTarget(null);
			return;
		}
		combatTicks++;
		mob.lookAt(target, 30.0F, 30.0F);

		double distance = mob.distanceToSqr(target);
		if (distance > IDEAL_RANGE * IDEAL_RANGE || !mob.getSensing().hasLineOfSight(target)) {
			Path path = HunterNavigation.pathToTarget(mob, target, runtime.area().orElseThrow());
			if (path == null || !mob.getNavigation().moveTo(path, SPEED)) {
				unreachableTicks += 2;
			} else {
				unreachableTicks = Math.max(0, unreachableTicks - 1);
			}
		} else if (distance < MIN_RANGE * MIN_RANGE) {
			mob.getNavigation().stop();
		}

		if (mob.getNavigation().isDone() && distance > IDEAL_RANGE * IDEAL_RANGE) {
			unreachableTicks++;
		}
		if (mob.getNavigation().isStuck()) {
			unreachableTicks += 5;
		}
		gaveUp = unreachableTicks >= MAX_UNREACHABLE_TICKS;

		if (cooldown > 0) {
			cooldown--;
		}
		if (!mob.isUsingItem() && cooldown <= 0 && runtime.hasSafeLineOfSight(target)) {
			mob.startUsingItem(InteractionHand.MAIN_HAND);
			drawTicks = 0;
		}
		if (mob.isUsingItem()) {
			drawTicks++;
			if (drawTicks >= DRAW_TICKS) {
				runtime.beginShot(target);
				boolean fired = runtime.fireArrow(target);
				FantasyFolk.LOGGER.debug(
						"Hunter diagnostics: fireArrow hunter={} target={} fired={} cooldownBefore={}",
						runtime.mob().getUUID(), target.getUUID(), fired, cooldown
				);
				mob.stopUsingItem();
				cooldown = fired ? SHOT_INTERVAL_TICKS : 5;
				drawTicks = 0;
			}
		}
	}

	@Override
	public void stop() {
		T mob = runtime.mob();
		LivingEntity target = mob.getTarget();
		if (target != null) {
			runtime.completeShot(target);
			if (target.isAlive() && gaveUp) {
				runtime.rejectTarget(target);
			}
		}
		if (mob.isUsingItem()) {
			mob.stopUsingItem();
		}
		mob.setAggressive(false);
		mob.getNavigation().stop();
		mob.setTarget(null);
		FantasyFolk.LOGGER.debug(
				"Hunter diagnostics: ranged goal stopped hunter={} target={} gaveUp={} combatTicks={}",
				mob.getUUID(), target == null ? "none" : target.getUUID(), gaveUp, combatTicks
		);
	}

	@Override
	public boolean requiresUpdateEveryTick() {
		return true;
	}
}
