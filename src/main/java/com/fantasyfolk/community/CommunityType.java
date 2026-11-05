package com.fantasyfolk.community;

import com.mojang.serialization.Codec;

import java.util.Locale;

/** The source of a registered community. */
public enum CommunityType {
	VANILLA,
	CUSTOM;

	public static final Codec<CommunityType> CODEC = Codec.STRING.xmap(
			value -> CommunityType.valueOf(value.toUpperCase(Locale.ROOT)),
			CommunityType::name
	);
}
