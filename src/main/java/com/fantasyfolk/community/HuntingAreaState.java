package com.fantasyfolk.community;

import java.util.Locale;

import com.mojang.serialization.Codec;

/** Lifecycle state of a persisted community hunting area. */
public enum HuntingAreaState {
	ACTIVE,
	INACTIVE;

	public static final Codec<HuntingAreaState> CODEC = Codec.STRING.xmap(
			value -> HuntingAreaState.valueOf(value.toUpperCase(Locale.ROOT)),
			HuntingAreaState::name
	);
}
