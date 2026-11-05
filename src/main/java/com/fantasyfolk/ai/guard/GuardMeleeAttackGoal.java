package com.fantasyfolk.ai.guard;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;

import com.fantasyfolk.profession.ProfessionHolder;

/** Vanilla melee combat constrained by profession, community territory and bounded path failure. */
public final class GuardMeleeAttackGoal<T extends PathfinderMob & ProfessionHolder> extends MeleeAttackGoal {
	private static final double SPEED = 1.15D;
	private static final int MAX_COMBAT_TICKS = 400;
	private static final int MAX_UNREACHABLE_TICKS = 60;
	private static final int TARGET_RETRY_COOLDOWN = 100;

	private final GuardRuntime<T> runtime;
	private int combatTicks;
	private int unreachableTicks;
	private boolean gaveUp;

	public GuardMeleeAttackGoal(GuardRuntime<T> runtime) {
		super(runtime.mob(), SPEED, true);
		this.runtime = runtime;
	}

	@Override
	public boolean canUse() {
		LivingEntity target = mob.getTarget();
		return target != null && runtime.isValidTarget(target, true) && super.canUse();
	}

	@Override
	public void start() {
		combatTicks = 0;
		unreachableTicks = 0;
		gaveUp = false;
		super.start();
	}

	@Override
	public boolean canContinueToUse() {
		LivingEntity target = mob.getTarget();
		return !gaveUp
				&& combatTicks < MAX_COMBAT_TICKS
				&& target != null
				&& runtime.isValidTarget(target, true)
				&& super.canContinueToUse();
	}

	@Override
	public void tick() {
		super.tick();
		combatTicks++;
		LivingEntity target = mob.getTarget();
		if (target == null || mob.isWithinMeleeAttackRange(target)) {
			unreachableTicks = 0;
			return;
		}

		if (mob.getNavigation().isDone()) {
			unreachableTicks++;
		} else if (unreachableTicks > 0) {
			unreachableTicks--;
		}
		if (mob.getNavigation().isStuck()) {
			unreachableTicks += 5;
		}
		gaveUp = unreachableTicks >= MAX_UNREACHABLE_TICKS;
	}

	@Override
	public void stop() {
		LivingEntity target = mob.getTarget();
		if (target != null && target.isAlive()
				&& (gaveUp || combatTicks >= MAX_COMBAT_TICKS
				|| runtime.community().filter(context -> context.contains(target.blockPosition())).isEmpty())) {
			runtime.rejectTarget(target, TARGET_RETRY_COOLDOWN);
		}
		super.stop();
		mob.setTarget(null);
		mob.getNavigation().stop();
		mob.setAggressive(false);
	}
}
