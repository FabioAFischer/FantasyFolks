package com.fantasyfolk.community;

import java.util.UUID;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.minecraft.core.BlockPos;
import net.minecraft.core.UUIDUtil;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;

/** Persistent location and state for the one hunting area owned by a community. */
public final class HuntingArea {
	public static final int DEFAULT_RADIUS = 12;
	public static final Codec<HuntingArea> CODEC = RecordCodecBuilder.create(instance -> instance.group(
			UUIDUtil.CODEC.fieldOf("community_id").forGetter(HuntingArea::communityId),
			ResourceKey.codec(Registries.DIMENSION).fieldOf("dimension").forGetter(HuntingArea::dimension),
			BlockPos.CODEC.fieldOf("center").forGetter(HuntingArea::center),
			Codec.INT.fieldOf("radius").forGetter(HuntingArea::radius),
			HuntingAreaState.CODEC.fieldOf("state").forGetter(HuntingArea::state)
	).apply(instance, HuntingArea::new));

	private final UUID communityId;
	private final ResourceKey<Level> dimension;
	private BlockPos center;
	private final int radius;
	private HuntingAreaState state;

	public HuntingArea(UUID communityId, ResourceKey<Level> dimension, BlockPos center) {
		this(communityId, dimension, center, DEFAULT_RADIUS, HuntingAreaState.ACTIVE);
	}

	private HuntingArea(
			UUID communityId,
			ResourceKey<Level> dimension,
			BlockPos center,
			int radius,
			HuntingAreaState state
	) {
		this.communityId = communityId;
		this.dimension = dimension;
		this.center = center.immutable();
		this.radius = Math.max(1, radius);
		this.state = state;
	}

	public UUID communityId() {
		return communityId;
	}

	public ResourceKey<Level> dimension() {
		return dimension;
	}

	public BlockPos center() {
		return center;
	}

	public int radius() {
		return radius;
	}

	public HuntingAreaState state() {
		return state;
	}

	public boolean contains(BlockPos position) {
		long dx = center.getX() - (long) position.getX();
		long dz = center.getZ() - (long) position.getZ();
		return dx * dx + dz * dz <= (long) radius * radius;
	}

	public void moveTo(BlockPos newCenter) {
		center = newCenter.immutable();
		state = HuntingAreaState.ACTIVE;
	}

	public void activate() {
		state = HuntingAreaState.ACTIVE;
	}

	public void deactivate() {
		state = HuntingAreaState.INACTIVE;
	}
}
