package com.fantasyfolk.profession;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

import net.minecraft.resources.Identifier;

import com.fantasyfolk.FantasyFolk;

/** Central registry for profession definitions; entity classes do not own this knowledge. */
public final class ProfessionRegistry {
	private static final Map<Identifier, ProfessionType> TYPES = new LinkedHashMap<>();

	public static final ProfessionType GUARD = register(new ProfessionType(
			FantasyFolk.id("guard"),
			"profession.fantasyfolk.guard",
			ProfessionEquipment::isSword
	));
	public static final ProfessionType HUNTER = register(new ProfessionType(
			FantasyFolk.id("hunter"),
			"profession.fantasyfolk.hunter",
			ProfessionEquipment::isBow
	));

	private ProfessionRegistry() {
	}

	private static ProfessionType register(ProfessionType type) {
		TYPES.put(type.id(), type);
		return type;
	}

	public static void initialize() {
		// Kept as an explicit lifecycle hook for future data-driven registration.
	}

	public static Optional<ProfessionType> byId(Identifier id) {
		initialize();
		return Optional.ofNullable(TYPES.get(id));
	}

	public static Optional<ProfessionType> byId(String id) {
		Identifier parsed = Identifier.tryParse(id);
		return parsed == null ? Optional.empty() : byId(parsed);
	}

	public static Map<Identifier, ProfessionType> all() {
		initialize();
		return Collections.unmodifiableMap(TYPES);
	}
}
