package com.fantasyfolk.community;

import com.mojang.serialization.Codec;

import java.util.Locale;

/** Whether the community's recorded center is currently available. */
public enum CommunityState {
	ACTIVE,
	INACTIVE;

	public static final Codec<CommunityState> CODEC = Codec.STRING.xmap(
			value -> CommunityState.valueOf(value.toUpperCase(Locale.ROOT)),
			CommunityState::name
	);
}
