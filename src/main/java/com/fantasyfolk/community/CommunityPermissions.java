package com.fantasyfolk.community;

import net.minecraft.world.entity.player.Player;

/** Central seam for the future trust/reputation system. */
public final class CommunityPermissions {
	private CommunityPermissions() {
	}

	/** Development policy: normal player community actions are temporarily allowed. */
	public static boolean canPerformPlayerAction(Player player) {
		return true;
	}
}
