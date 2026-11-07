package com.fantasyfolk.community;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.minecraft.core.BlockPos;
import net.minecraft.core.UUIDUtil;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;

import java.util.Optional;
import java.util.UUID;

/** Persistent registration for one logical vanilla chest or barrel. */
public final class CommunityDepot {
	public static final Codec<CommunityDepot> CODEC = RecordCodecBuilder.create(instance -> instance.group(
			UUIDUtil.CODEC.fieldOf("id").forGetter(CommunityDepot::id),
			UUIDUtil.CODEC.fieldOf("community_id").forGetter(CommunityDepot::communityId),
			ResourceKey.codec(Registries.DIMENSION).fieldOf("dimension").forGetter(CommunityDepot::dimension),
			BlockPos.CODEC.fieldOf("primary").forGetter(CommunityDepot::primary),
			BlockPos.CODEC.optionalFieldOf("secondary").forGetter(depot -> Optional.ofNullable(depot.secondary)),
			DepotType.CODEC.fieldOf("type").forGetter(CommunityDepot::type),
			DepotState.CODEC.fieldOf("state").forGetter(CommunityDepot::state)
	).apply(instance, CommunityDepot::fromCodec));

	private final UUID id;
	private final UUID communityId;
	private final ResourceKey<Level> dimension;
	private BlockPos primary;
	private BlockPos secondary;
	private final DepotType type;
	private DepotState state;

	public CommunityDepot(
			UUID id,
			UUID communityId,
			ResourceKey<Level> dimension,
			BlockPos primary,
			BlockPos secondary,
			DepotType type
	) {
		this(id, communityId, dimension, primary, secondary, type, DepotState.ACTIVE);
	}

	private CommunityDepot(
			UUID id,
			UUID communityId,
			ResourceKey<Level> dimension,
			BlockPos primary,
			BlockPos secondary,
			DepotType type,
			DepotState state
	) {
		this.id = id;
		this.communityId = communityId;
		this.dimension = dimension;
		this.primary = primary.immutable();
		this.secondary = secondary == null ? null : secondary.immutable();
		this.type = type;
		this.state = state;
	}

	private static CommunityDepot fromCodec(
			UUID id,
			UUID communityId,
			ResourceKey<Level> dimension,
			BlockPos primary,
			Optional<BlockPos> secondary,
			DepotType type,
			DepotState state
	) {
		return new CommunityDepot(id, communityId, dimension, primary, secondary.orElse(null), type, state);
	}

	public UUID id() {
		return id;
	}

	public UUID communityId() {
		return communityId;
	}

	public ResourceKey<Level> dimension() {
		return dimension;
	}

	public BlockPos primary() {
		return primary;
	}

	public Optional<BlockPos> secondary() {
		return Optional.ofNullable(secondary);
	}

	public DepotType type() {
		return type;
	}

	public DepotState state() {
		return state;
	}

	public boolean hasPosition(BlockPos position) {
		return primary.equals(position) || position.equals(secondary);
	}

	public void reconcile(BlockPos primary, BlockPos secondary) {
		this.primary = primary.immutable();
		this.secondary = secondary == null ? null : secondary.immutable();
		this.state = DepotState.ACTIVE;
	}

	public void markInvalid() {
		state = DepotState.INVALID;
	}
}
