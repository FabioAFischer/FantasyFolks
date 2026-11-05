package com.fantasyfolk.community;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.minecraft.core.BlockPos;
import net.minecraft.core.UUIDUtil;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;

import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/** Persistent, race-independent data for a Fantasy Folk community. */
public final class Community {
	public static final Codec<Community> CODEC = RecordCodecBuilder.create(instance -> instance.group(
			UUIDUtil.CODEC.fieldOf("id").forGetter(Community::id),
			CommunityType.CODEC.fieldOf("type").forGetter(Community::type),
			Codec.STRING.fieldOf("name").forGetter(Community::name),
			ResourceKey.codec(Registries.DIMENSION).fieldOf("dimension").forGetter(Community::dimension),
			BlockPos.CODEC.fieldOf("center").forGetter(Community::center),
			Codec.INT.fieldOf("radius").forGetter(Community::radius),
			CommunityState.CODEC.fieldOf("state").forGetter(Community::state),
			CommunityMember.CODEC.listOf().fieldOf("members").forGetter(community -> List.copyOf(community.members))
	).apply(instance, Community::fromCodec));

	private final UUID id;
	private final CommunityType type;
	private final String name;
	private final ResourceKey<Level> dimension;
	private final BlockPos center;
	private final int radius;
	private CommunityState state;
	private final Set<CommunityMember> members;

	public Community(
			UUID id,
			CommunityType type,
			String name,
			ResourceKey<Level> dimension,
			BlockPos center,
			int radius,
			CommunityState state
	) {
		this(id, type, name, dimension, center, radius, state, List.of());
	}

	private Community(
			UUID id,
			CommunityType type,
			String name,
			ResourceKey<Level> dimension,
			BlockPos center,
			int radius,
			CommunityState state,
			Collection<CommunityMember> members
	) {
		this.id = id;
		this.type = type;
		this.name = name;
		this.dimension = dimension;
		this.center = center.immutable();
		this.radius = radius;
		this.state = state;
		this.members = new LinkedHashSet<>(members);
	}

	private static Community fromCodec(
			UUID id,
			CommunityType type,
			String name,
			ResourceKey<Level> dimension,
			BlockPos center,
			int radius,
			CommunityState state,
			List<CommunityMember> members
	) {
		return new Community(id, type, name, dimension, center, radius, state, members);
	}

	public UUID id() {
		return id;
	}

	public CommunityType type() {
		return type;
	}

	public String name() {
		return name;
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

	public CommunityState state() {
		return state;
	}

	public Set<CommunityMember> members() {
		return Set.copyOf(members);
	}

	public boolean hasMember(UUID entityId) {
		return members.stream().anyMatch(member -> member.entityId().equals(entityId));
	}

	public boolean isWithinTerritory(BlockPos position) {
		long dx = center.getX() - (long) position.getX();
		long dz = center.getZ() - (long) position.getZ();
		return dx * dx + dz * dz <= (long) radius * radius;
	}

	public boolean addMember(UUID entityId, ResourceKey<Level> memberDimension) {
		if (!dimension.equals(memberDimension) || hasMember(entityId)) {
			return false;
		}
		return members.add(new CommunityMember(entityId, memberDimension));
	}

	public boolean removeMember(UUID entityId) {
		return members.removeIf(member -> member.entityId().equals(entityId));
	}

	public void setState(CommunityState state) {
		this.state = state;
	}
}
