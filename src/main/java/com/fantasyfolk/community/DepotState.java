package com.fantasyfolk.community;

import com.mojang.serialization.Codec;

import java.util.Locale;

/** Operational state of a persisted depot registration. */
public enum DepotState {
	ACTIVE,
	INVALID;

	public static final Codec<DepotState> CODEC = Codec.STRING.xmap(
			value -> DepotState.valueOf(value.toUpperCase(Locale.ROOT)),
			DepotState::name
	);
}
