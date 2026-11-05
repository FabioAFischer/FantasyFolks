package com.fantasyfolk.ai;

import java.util.function.BooleanSupplier;

import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.goal.RandomStrollGoal;

/** Keeps ordinary wandering available only when no profession activity owns movement. */
public final class ProfessionAwareRandomStrollGoal extends RandomStrollGoal {
	private final BooleanSupplier allowed;

	public ProfessionAwareRandomStrollGoal(PathfinderMob mob, double speedModifier, BooleanSupplier allowed) {
		super(mob, speedModifier);
		this.allowed = allowed;
	}

	@Override
	public boolean canUse() {
		return allowed.getAsBoolean() && super.canUse();
	}

	@Override
	public boolean canContinueToUse() {
		return allowed.getAsBoolean() && super.canContinueToUse();
	}
}
