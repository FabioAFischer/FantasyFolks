package com.fantasyfolk.community;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.minecraft.core.UUIDUtil;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;

import java.util.UUID;

/** A persisted entity reference. It deliberately does not retain an Entity object. */
public record CommunityMember(UUID entityId, ResourceKey<Level> dimension) {
	public static final Codec<CommunityMember> CODEC = RecordCodecBuilder.create(instance -> instance.group(
			UUIDUtil.CODEC.fieldOf("entity_id").forGetter(CommunityMember::entityId),
			ResourceKey.codec(Registries.DIMENSION).fieldOf("dimension").forGetter(CommunityMember::dimension)
	).apply(instance, CommunityMember::new));
}
