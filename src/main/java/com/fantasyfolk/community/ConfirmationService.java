package com.fantasyfolk.community;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.world.item.ItemStack;

/** Short-lived, server-side confirmation requests shared by interaction flows. */
public final class ConfirmationService {
	public static final long WINDOW_TICKS = 200;
	private static final Map<UUID, PendingRequest> PENDING = new HashMap<>();

	private ConfirmationService() {
	}

	public static void initialize() {
		ServerTickEvents.END_SERVER_TICK.register(server -> cleanup(server.getTickCount()));
	}

	public static boolean checkOrArm(UUID playerId, Request request, long now) {
		cleanup(now);
		PendingRequest previous = PENDING.get(playerId);
		if (previous != null && previous.expiresAt() >= now && previous.request().matches(request)) {
			PENDING.remove(playerId);
			return true;
		}

		PENDING.put(playerId, new PendingRequest(request, now + WINDOW_TICKS));
		return false;
	}

	public static void cancel(UUID playerId) {
		PENDING.remove(playerId);
	}

	public static void cancelIfActionStartsWith(UUID playerId, String prefix) {
		PendingRequest pending = PENDING.get(playerId);
		if (pending != null && pending.request().action().startsWith(prefix)) {
			PENDING.remove(playerId);
		}
	}

	private static void cleanup(long now) {
		PENDING.entrySet().removeIf(entry -> entry.getValue().expiresAt() < now);
	}

	public record Request(String action, String target, String selection, ItemStack item) {
		public Request {
			item = item.copy();
		}

		private boolean matches(Request other) {
			return action.equals(other.action)
					&& target.equals(other.target)
					&& selection.equals(other.selection)
					&& ItemStack.matches(item, other.item);
		}
	}

	private record PendingRequest(Request request, long expiresAt) {
	}
}
