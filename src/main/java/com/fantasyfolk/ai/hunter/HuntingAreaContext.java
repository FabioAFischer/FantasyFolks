package com.fantasyfolk.ai.hunter;

import java.util.UUID;

import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;

import com.fantasyfolk.community.Community;
import com.fantasyfolk.community.HuntingArea;

/** Immutable short-lived snapshot combining a community and its hunting area. */
public record HuntingAreaContext(
		UUID communityId,
		ResourceKey<Level> dimension,
		BlockPos center,
		int radius,
		BlockPos communityCenter,
		int communityRadius
) {
	public HuntingAreaContext {
		center = center.immutable();
		radius = Math.max(1, radius);
		communityCenter = communityCenter.immutable();
		communityRadius = Math.max(0, communityRadius);
	}

	public static HuntingAreaContext from(Community community, HuntingArea area) {
		return new HuntingAreaContext(
				community.id(),
				community.dimension(),
				area.center(),
				area.radius(),
				community.center(),
				community.radius()
		);
	}

	public boolean contains(BlockPos position) {
		return containsCircle(position, center, radius) && containsCircle(position, communityCenter, communityRadius);
	}

	public boolean containsCircle(BlockPos position) {
		return containsCircle(position, center, radius);
	}

	private static boolean containsCircle(BlockPos position, BlockPos circleCenter, int circleRadius) {
		long dx = circleCenter.getX() - (long) position.getX();
		long dz = circleCenter.getZ() - (long) position.getZ();
		return dx * dx + dz * dz <= (long) circleRadius * circleRadius;
	}
}
