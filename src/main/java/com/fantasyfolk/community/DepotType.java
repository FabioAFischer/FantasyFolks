package com.fantasyfolk.community;

import com.mojang.serialization.Codec;

import java.util.Locale;

/** Vanilla container types supported by community depots. */
public enum DepotType {
	CHEST,
	BARREL;

	public static final Codec<DepotType> CODEC = Codec.STRING.xmap(
			value -> DepotType.valueOf(value.toUpperCase(Locale.ROOT)),
			DepotType::name
	);
}
