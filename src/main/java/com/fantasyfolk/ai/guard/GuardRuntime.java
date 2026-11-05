package com.fantasyfolk.ai.guard;

import java.util.Optional;
import java.util.UUID;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.npc.villager.Villager;

import com.fantasyfolk.community.Community;
import com.fantasyfolk.community.CommunityHolder;
import com.fantasyfolk.community.CommunityManager;
import com.fantasyfolk.community.CommunityState;
import com.fantasyfolk.profession.ProfessionEquipment;
import com.fantasyfolk.profession.ProfessionHolder;
import com.fantasyfolk.profession.ProfessionRegistry;

/** Shared server-side state and validation used by every Guard Goal for one entity. */
public final class GuardRuntime<T extends PathfinderMob & ProfessionHolder> {
	private static final int ACTIVE_REFRESH_TICKS = 20;
	private static final int MISSING_REFRESH_TICKS = 100;

	private final T mob;
	private UUID cachedCommunityId;
	private GuardCommunityContext cachedCommunity;
	private int nextCommunityRefreshTick;
	private int resolvedTick = Integer.MIN_VALUE;
	private GuardCommunityContext resolvedCommunity;
	private UUID rejectedTargetId;
	private int rejectedTargetUntilTick;

	public GuardRuntime(T mob) {
		this.mob = mob;
	}

	public T mob() {
		return mob;
	}

	public Optional<GuardCommunityContext> community() {
		if (!isGuardReady()) {
			return Optional.empty();
		}
		if (resolvedTick == mob.tickCount) {
			return Optional.ofNullable(resolvedCommunity);
		}

		resolvedTick = mob.tickCount;
		resolvedCommunity = resolveCommunity();
		return Optional.ofNullable(resolvedCommunity);
	}

	public boolean isValidTarget(LivingEntity target, boolean reactive) {
		Optional<GuardCommunityContext> context = community();
		if (context.isEmpty()
				|| target == mob
				|| target.isRemoved()
				|| !target.isAlive()
				|| target.isInvulnerable()
				|| target.level() != mob.level()
				|| !context.get().contains(target.blockPosition())
				|| isProtectedAlly(target, context.get())
				|| !canSelectTarget(target)) {
			return false;
		}

		boolean allowedThreat = reactive
				? GuardThreatPolicy.isReactiveThreat(target)
				: GuardThreatPolicy.isProactiveThreat(target);
		return allowedThreat && mob.canAttack(target);
	}

	public boolean isProtectedAlly(LivingEntity entity, GuardCommunityContext context) {
		if (entity == mob) {
			return true;
		}
		if (entity instanceof Villager) {
			return context.contains(entity.blockPosition());
		}
		if (entity instanceof CommunityHolder && mob.level() instanceof ServerLevel level) {
			return CommunityManager.isMember(level.getServer(), context.id(), entity.getUUID());
		}
		return false;
	}

	public void rejectTarget(LivingEntity target, int ticks) {
		rejectedTargetId = target.getUUID();
		rejectedTargetUntilTick = mob.tickCount + ticks;
	}

	public void onProfessionChanged() {
		cachedCommunity = null;
		nextCommunityRefreshTick = 0;
		resolvedTick = Integer.MIN_VALUE;
		resolvedCommunity = null;
		rejectedTargetId = null;
		mob.setTarget(null);
		mob.getNavigation().stop();
		mob.setAggressive(false);
	}

	public static boolean isGuardProfession(String professionId) {
		return professionId != null && ProfessionRegistry.GUARD.id().toString().equals(professionId);
	}

	private boolean isGuardReady() {
		return !mob.level().isClientSide()
				&& mob.isAlive()
				&& !mob.isNoAi()
				&& isGuardProfession(mob.getProfessionId())
				&& ProfessionEquipment.isSword(mob.getMainHandItem());
	}

	private GuardCommunityContext resolveCommunity() {
		if (!(mob.level() instanceof ServerLevel level)) {
			return null;
		}
		if (mob.tickCount >= nextCommunityRefreshTick) {
			Community community = findCommunity(level);
			if (community == null) {
				cachedCommunity = null;
				nextCommunityRefreshTick = mob.tickCount + MISSING_REFRESH_TICKS;
			} else {
				cachedCommunityId = community.id();
				cachedCommunity = community.state() == CommunityState.ACTIVE
						? GuardCommunityContext.from(community)
						: null;
				nextCommunityRefreshTick = mob.tickCount + ACTIVE_REFRESH_TICKS;
			}
		}

		if (cachedCommunity == null || !cachedCommunity.dimension().equals(level.dimension())) {
			return null;
		}
		return cachedCommunity;
	}

	private Community findCommunity(ServerLevel level) {
		Community community = cachedCommunityId == null
				? null
				: CommunityManager.findById(level.getServer(), cachedCommunityId).orElse(null);
		if (community != null && community.hasMember(mob.getUUID())) {
			return community;
		}

		cachedCommunityId = null;
		return CommunityManager.findCommunityForMember(level.getServer(), mob.getUUID()).orElse(null);
	}

	private boolean canSelectTarget(LivingEntity target) {
		if (rejectedTargetId == null) {
			return true;
		}
		if (mob.tickCount >= rejectedTargetUntilTick) {
			rejectedTargetId = null;
			return true;
		}
		return !rejectedTargetId.equals(target.getUUID());
	}
}
