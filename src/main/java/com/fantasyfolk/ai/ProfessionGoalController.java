package com.fantasyfolk.ai;

import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.goal.GoalSelector;

import com.fantasyfolk.ai.guard.GuardDefendAllyGoal;
import com.fantasyfolk.ai.guard.GuardMeleeAttackGoal;
import com.fantasyfolk.ai.guard.GuardPatrolGoal;
import com.fantasyfolk.ai.guard.GuardReturnToTerritoryGoal;
import com.fantasyfolk.ai.guard.GuardRuntime;
import com.fantasyfolk.ai.guard.GuardTargetGoal;
import com.fantasyfolk.profession.ProfessionHolder;

/** Registers profession Goals once and coordinates their lifecycle across profession changes. */
public final class ProfessionGoalController<T extends PathfinderMob & ProfessionHolder> {
	private final GuardRuntime<T> guardRuntime;
	private boolean registered;

	public ProfessionGoalController(T mob) {
		this.guardRuntime = new GuardRuntime<>(mob);
	}

	public void registerGoals(GoalSelector goals, GoalSelector targets) {
		if (registered) {
			return;
		}
		registered = true;

		targets.addGoal(1, new GuardDefendAllyGoal<>(guardRuntime));
		targets.addGoal(2, new GuardTargetGoal<>(guardRuntime));
		goals.addGoal(2, new GuardMeleeAttackGoal<>(guardRuntime));
		goals.addGoal(4, new GuardReturnToTerritoryGoal<>(guardRuntime));
		goals.addGoal(5, new GuardPatrolGoal<>(guardRuntime));
	}

	public boolean allowsOrdinaryWandering() {
		return guardRuntime.community().isEmpty();
	}

	public void onProfessionChanged() {
		guardRuntime.onProfessionChanged();
	}
}
