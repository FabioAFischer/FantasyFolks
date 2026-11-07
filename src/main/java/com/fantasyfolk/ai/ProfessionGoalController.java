package com.fantasyfolk.ai;

import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.goal.GoalSelector;

import com.fantasyfolk.inventory.WorkInventoryHolder;
import com.fantasyfolk.ai.hunter.HunterCollectLootGoal;
import com.fantasyfolk.ai.hunter.HunterFleeGoal;
import com.fantasyfolk.ai.hunter.HunterReturnToHuntingAreaGoal;
import com.fantasyfolk.ai.hunter.HunterRangedAttackGoal;
import com.fantasyfolk.ai.hunter.HunterRuntime;
import com.fantasyfolk.ai.hunter.HunterStateHolder;
import com.fantasyfolk.ai.hunter.HunterTargetGoal;
import com.fantasyfolk.ai.guard.GuardDefendAllyGoal;
import com.fantasyfolk.ai.guard.GuardMeleeAttackGoal;
import com.fantasyfolk.ai.guard.GuardPatrolGoal;
import com.fantasyfolk.ai.guard.GuardReturnToTerritoryGoal;
import com.fantasyfolk.ai.guard.GuardRuntime;
import com.fantasyfolk.ai.guard.GuardTargetGoal;
import com.fantasyfolk.profession.ProfessionHolder;

/** Registers profession Goals once and coordinates their lifecycle across profession changes. */
public final class ProfessionGoalController<T extends PathfinderMob & ProfessionHolder & WorkInventoryHolder & HunterStateHolder> {
	private final GuardRuntime<T> guardRuntime;
	private final HunterRuntime<T> hunterRuntime;
	private boolean registered;

	public ProfessionGoalController(T mob) {
		this.guardRuntime = new GuardRuntime<>(mob);
		this.hunterRuntime = new HunterRuntime<>(mob);
	}

	public void registerGoals(GoalSelector goals, GoalSelector targets) {
		if (registered) {
			return;
		}
		registered = true;

		targets.addGoal(1, new GuardDefendAllyGoal<>(guardRuntime));
		targets.addGoal(2, new GuardTargetGoal<>(guardRuntime));
		targets.addGoal(2, new HunterTargetGoal<>(hunterRuntime));
		goals.addGoal(1, new HunterFleeGoal<>(hunterRuntime));
		goals.addGoal(2, new GuardMeleeAttackGoal<>(guardRuntime));
		goals.addGoal(3, new CommunityDepositGoal<>(mob()));
		goals.addGoal(3, new HunterCollectLootGoal<>(hunterRuntime));
		goals.addGoal(4, new HunterRangedAttackGoal<>(hunterRuntime));
		goals.addGoal(4, new GuardReturnToTerritoryGoal<>(guardRuntime));
		goals.addGoal(5, new HunterReturnToHuntingAreaGoal<>(hunterRuntime));
		goals.addGoal(6, new GuardPatrolGoal<>(guardRuntime));
	}

	public boolean allowsOrdinaryWandering() {
		return guardRuntime.community().isEmpty() && hunterRuntime.area().isEmpty();
	}

	public void onProfessionChanged() {
		guardRuntime.onProfessionChanged();
		hunterRuntime.onProfessionChanged();
	}

	private T mob() {
		return guardRuntime.mob();
	}
}
