package com.fantasyfolk.community;

import java.util.Optional;
import java.util.UUID;

/** Capability-like contract for any Fantasy Folk race that can belong to a community. */
public interface CommunityHolder {
	Optional<UUID> getCommunityId();
}
