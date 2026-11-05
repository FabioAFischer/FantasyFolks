package com.fantasyfolk.ai.guard;

import java.util.UUID;

import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;

import com.fantasyfolk.community.Community;

/** Immutable, short-lived community snapshot used by Guard Goals. */
public record GuardCommunityContext(
		UUID id,
		ResourceKey<Level> dimension,
		BlockPos center,
		int radius
) {
	public static final int MAX_PATROL_RADIUS = 24;

	public GuardCommunityContext {
		center = center.immutable();
		radius = Math.max(0, radius);
	}

	public static GuardCommunityContext from(Community community) {
		return new GuardCommunityContext(
				community.id(),
				community.dimension(),
				community.center(),
				community.radius()
		);
	}

	public int patrolRadius() {
		return Math.min(MAX_PATROL_RADIUS, radius);
	}

	public boolean contains(BlockPos position) {
		return contains(position, radius);
	}

	public boolean containsPatrolPosition(BlockPos position) {
		return contains(position, patrolRadius());
	}

	private boolean contains(BlockPos position, int allowedRadius) {
		long dx = center.getX() - (long) position.getX();
		long dz = center.getZ() - (long) position.getZ();
		return dx * dx + dz * dz <= (long) allowedRadius * allowedRadius;
	}
}
