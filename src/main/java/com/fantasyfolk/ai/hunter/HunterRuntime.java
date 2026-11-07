package com.fantasyfolk.ai.hunter;

import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.phys.AABB;

import com.fantasyfolk.FantasyFolk;
import com.fantasyfolk.ai.CommunityDepositGoal;
import com.fantasyfolk.community.Community;
import com.fantasyfolk.community.CommunityManager;
import com.fantasyfolk.community.CommunityState;
import com.fantasyfolk.community.CommunityItemTransfer;
import com.fantasyfolk.community.HuntingArea;
import com.fantasyfolk.community.HuntingAreaManager;
import com.fantasyfolk.inventory.WorkInventoryHolder;
import com.fantasyfolk.profession.ProfessionEquipment;
import com.fantasyfolk.profession.ProfessionHolder;
import com.fantasyfolk.profession.ProfessionRegistry;

import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.entity.projectile.arrow.AbstractArrow;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.damagesource.DamageSource;

/** Shared server-side state and validation for all Hunter Goals on one entity. */
public final class HunterRuntime<T extends PathfinderMob & ProfessionHolder & WorkInventoryHolder & HunterStateHolder> {
	public static final int DAILY_KILL_LIMIT = 4;
	public static final int LOOT_SEARCH_RADIUS = 4;
	private static final int ACTIVE_REFRESH_TICKS = 20;
	private static final int MISSING_REFRESH_TICKS = 100;
	private static final int LOOT_WAIT_TICKS = 100;
	private static final int SHOT_CONTEXT_TICKS = 100;
	private static final int REJECTED_TARGET_TICKS = 100;

	private final T mob;
	private UUID cachedCommunityId;
	private HuntingAreaContext cachedArea;
	private int nextAreaRefreshTick;
	private int resolvedTick = Integer.MIN_VALUE;
	private HuntingAreaContext resolvedArea;
	private UUID rejectedTargetId;
	private int rejectedTargetUntilTick;
	private UUID shotTargetId;
	private Set<UUID> shotItemBaseline = Set.of();
	private int shotContextUntilTick;
	private PendingLoot pendingLoot;
	private boolean lootScanLogged;
	private int depositResolutionTick = Integer.MIN_VALUE;
	private boolean reachableDeposit;
	private String lastWorkDiagnostic;

	public HunterRuntime(T mob) {
		this.mob = mob;
	}

	public T mob() {
		return mob;
	}

	public Optional<HuntingAreaContext> area() {
		if (!isHunterReady()) {
			return Optional.empty();
		}
		if (resolvedTick == mob.tickCount) {
			return Optional.ofNullable(resolvedArea);
		}

		resolvedTick = mob.tickCount;
		resolvedArea = resolveArea();
		return Optional.ofNullable(resolvedArea);
	}

	public boolean isValidTarget(LivingEntity target) {
		Optional<HuntingAreaContext> context = area();
		return context.isPresent()
				&& canWork()
				&& !isInDanger()
				&& !isRejected(target)
				&& target != mob
				&& target.level() == mob.level()
				&& mob.distanceToSqr(target) <= 20.0D * 20.0D
				&& HunterTargetPolicy.isEligible((ServerLevel) mob.level(), context.orElseThrow(), mob, target);
	}

	public boolean canWork() {
		expireShotContext();
		Optional<HuntingAreaContext> huntingArea = area();
		boolean pending = hasPendingLoot();
		boolean depositable = hasDepositableWorkItems();
		boolean depositReachable = depositable && hasReachableDeposit();
		boolean danger = isInDanger();
		boolean ammunition = hasAmmunition();
		boolean capacity = hasWorkCapacity();
		String blockedBy = pending ? "pending-loot"
				: depositReachable ? "reachable-deposit"
				: huntingArea.isEmpty() ? "no-hunting-area"
				: danger ? "danger"
				: !ammunition ? "no-arrows"
				: !capacity ? "work-inventory-full"
				: null;
		logWorkDiagnostic(blockedBy, huntingArea.isPresent(), ammunition, capacity, depositable, depositReachable);
		if (blockedBy != null) {
			return false;
		}
		ServerLevel level = (ServerLevel) mob.level();
		prepareDay(level);
		return mob.getHunterDailyKills() < DAILY_KILL_LIMIT;
	}

	public boolean hasAmmunition() {
		for (int slot = 0; slot < mob.getWorkInventory().getContainerSize(); slot++) {
			if (mob.getWorkInventory().getItem(slot).is(ItemTags.ARROWS)) {
				return true;
			}
		}
		return false;
	}

	public boolean hasWorkCapacity() {
		for (int slot = 0; slot < mob.getWorkInventory().getContainerSize(); slot++) {
			ItemStack stack = mob.getWorkInventory().getItem(slot);
			if (stack.isEmpty()) {
				return true;
			}
			if (stack.isStackable()
					&& stack.count() < Math.min(mob.getWorkInventory().getMaxStackSize(stack), stack.getMaxStackSize())) {
				return true;
			}
		}
		return false;
	}

	public boolean fireArrow(LivingEntity target) {
		if (!(mob.level() instanceof ServerLevel level) || !hasAmmunition()) {
			return false;
		}
		ItemStack bow = mob.getMainHandItem();
		ItemStack arrowSupply = findArrowSupply();
		if (arrowSupply.isEmpty()) {
			return false;
		}
		ItemStack projectileStack = arrowSupply.copyWithCount(1);
		AbstractArrow arrow = ProjectileUtil.getMobArrow(mob, projectileStack, 1.0F, bow);
		double dx = target.getX() - mob.getX();
		double dz = target.getZ() - mob.getZ();
		double dy = target.getY(0.3333333333333333D) - arrow.getY();
		double horizontalDistance = Math.sqrt(dx * dx + dz * dz);
		Projectile.spawnProjectileUsingShoot(
				arrow,
				level,
				projectileStack,
				dx,
				dy + horizontalDistance * 0.2D,
				dz,
				1.6F,
				rangedAttackUncertainty(level)
		);
		arrowSupply.shrink(1);
		bow.hurtAndBreak(1, mob, EquipmentSlot.MAINHAND);
		mob.swingForAttack(InteractionHand.MAIN_HAND);
		return true;
	}

	public boolean hasSafeLineOfSight(LivingEntity target) {
		if (!(mob.level() instanceof ServerLevel level)) {
			return false;
		}
		var from = mob.getEyePosition();
		var to = target.getEyePosition();
		if (level.clip(new net.minecraft.world.level.ClipContext(
				from,
				to,
				net.minecraft.world.level.ClipContext.Block.COLLIDER,
				net.minecraft.world.level.ClipContext.Fluid.NONE,
				mob
		)).getType() != net.minecraft.world.phys.HitResult.Type.MISS) {
			return false;
		}
		return level.getEntities(
				mob,
				new AABB(from, to).inflate(0.45D),
				entity -> entity != target
						&& entity.getBoundingBox().clip(from, to).isPresent()
						&& HunterTargetPolicy.isProtectedInTrajectory(entity)
			).isEmpty();
	}

	public boolean isInDanger() {
		if (!(mob.level() instanceof ServerLevel level)) {
			return false;
		}
		return !level.getEntities(
				mob,
				mob.getBoundingBox().inflate(8.0D),
				entity -> entity instanceof Enemy enemy && enemy != mob && entity.isAlive()
			).isEmpty();
	}

	public void beginShot(LivingEntity target) {
		if (!(mob.level() instanceof ServerLevel level)) {
			return;
		}
		if (!target.getUUID().equals(shotTargetId)) {
			shotItemBaseline = Set.of();
		}
		shotTargetId = target.getUUID();
		shotContextUntilTick = mob.tickCount + SHOT_CONTEXT_TICKS;
		Set<UUID> baseline = new HashSet<>();
		baseline.addAll(shotItemBaseline);
		BlockPos currentTargetPosition = target.blockPosition().immutable();
		for (Entity entity : level.getEntities(
				mob,
				new AABB(currentTargetPosition).inflate(LOOT_SEARCH_RADIUS),
				entity -> entity instanceof ItemEntity
		)) {
			baseline.add(entity.getUUID());
		}
		shotItemBaseline = Set.copyOf(baseline);
	}

	public void completeShot(LivingEntity target) {
		expireShotContext();
		if (shotTargetId == null || !shotTargetId.equals(target.getUUID())) {
			return;
		}
		/*
		 * Death attribution is handled by AFTER_KILLED_OTHER_ENTITY.  In
		 * particular, do not clear a live target here: an arrow can still be in
		 * flight after the combat Goal or TargetGoal has stopped.
		 */
		if (!target.isAlive()) {
			clearShotContext();
		}
	}

	/** Called once by Fabric after this entity has actually killed another entity. */
	public void onHunterKill(ServerLevel level, LivingEntity target, DamageSource source) {
		expireShotContext();
		if (!isHunterProfession()
				|| level != mob.level()
				|| shotTargetId == null
				|| !shotTargetId.equals(target.getUUID())
				|| !wasKilledByHunter(target, source)
				|| !HunterTargetPolicy.isAllowedSpecies(target)) {
			return;
		}

		HuntingAreaContext context = area().orElse(null);
		BlockPos deathPosition = target.blockPosition().immutable();
		if (context == null || !context.dimension().equals(level.dimension()) || !context.contains(deathPosition)) {
			clearShotContext();
			return;
		}

		/*
		 * Do not scan the death position here. This callback can run after
		 * vanilla has spawned the drops; adding those entities now would make
		 * the real drops part of the baseline and make them uncollectable.
		 */
		boolean recorded = recordKill(deathPosition, shotItemBaseline);
		FantasyFolk.LOGGER.debug(
				"Hunter diagnostics: pending loot hunter={} target={} recorded={} deathPosition={} baselineItems={} expiresAt={}",
				mob.getUUID(), target.getUUID(), recorded, deathPosition, shotItemBaseline.size(),
				recorded ? mob.tickCount + LOOT_WAIT_TICKS : -1
		);
		clearShotContext();
	}

	public boolean hasPendingLoot() {
		return pendingLoot != null && mob.tickCount <= pendingLoot.expiresAt()
				&& area().isPresent();
	}

	public BlockPos lootPosition() {
		return pendingLoot == null ? mob.blockPosition() : pendingLoot.position();
	}

	public Optional<ItemEntity> findLootCandidate() {
		if (!hasPendingLoot() || !(mob.level() instanceof ServerLevel level)) {
			return Optional.empty();
		}
		HuntingAreaContext context = area().orElseThrow();
		List<ItemEntity> nearbyItems = level.getEntities(
				mob,
				new AABB(pendingLoot.position()).inflate(LOOT_SEARCH_RADIUS),
				entity -> entity instanceof ItemEntity
		).stream().map(ItemEntity.class::cast).toList();
		Optional<ItemEntity> candidate = nearbyItems.stream()
				.filter(item -> item.isAlive()
						&& !pendingLoot.baseline().contains(item.getUUID())
						&& item.getAge() <= LOOT_WAIT_TICKS
						&& context.contains(item.blockPosition())
						&& isEligibleLoot(item))
				.min((first, second) -> Double.compare(mob.distanceToSqr(first), mob.distanceToSqr(second)));
		if (!lootScanLogged) {
			long newItems = nearbyItems.stream()
					.filter(item -> !pendingLoot.baseline().contains(item.getUUID()))
					.count();
			long eligibleItems = nearbyItems.stream()
					.filter(item -> item.isAlive()
							&& !pendingLoot.baseline().contains(item.getUUID())
							&& item.getAge() <= LOOT_WAIT_TICKS
							&& context.contains(item.blockPosition())
							&& isEligibleLoot(item))
					.count();
			FantasyFolk.LOGGER.debug(
					"Hunter diagnostics: loot scan hunter={} position={} nearby={} new={} eligible={} candidate={} expiresAt={}",
					mob.getUUID(), pendingLoot.position(), nearbyItems.size(), newItems, eligibleItems,
					candidate.map(item -> item.getUUID().toString()).orElse("none"), pendingLoot.expiresAt()
			);
			lootScanLogged = true;
		}
		return candidate;
	}

	public int collect(ItemEntity item) {
		if (!hasPendingLoot() || !isEligibleLoot(item)) {
			return 0;
		}
		ItemStack stack = item.getItem();
		int transferred = CommunityItemTransfer.transfer(stack, mob.getWorkInventory(), stack.count());
		if (transferred > 0) {
			pendingLoot = new PendingLoot(
					pendingLoot.position(), pendingLoot.baseline(), pendingLoot.expiresAt(), true
			);
			if (stack.isEmpty()) {
				item.discard();
			} else {
				item.setItem(stack);
			}
			FantasyFolk.LOGGER.debug(
					"Hunter diagnostics: loot transfer hunter={} item={} transferred={} remaining={}",
					mob.getUUID(), item.getUUID(), transferred, stack.count()
			);
		}
		return transferred;
	}

	/** Returns true only after at least one real drop was transferred and none remains eligible. */
	public boolean shouldFinishLootCollection() {
		return hasPendingLoot() && pendingLoot.collectedAny() && findLootCandidate().isEmpty();
	}

	public void clearLoot() {
		pendingLoot = null;
		lootScanLogged = false;
	}

	public void rejectTarget(LivingEntity target) {
		rejectedTargetId = target.getUUID();
		rejectedTargetUntilTick = mob.tickCount + REJECTED_TARGET_TICKS;
	}

	public boolean isHunterProfession() {
		return isHunterProfession(mob.getProfessionId());
	}

	public static boolean isHunterProfession(String professionId) {
		return professionId != null && ProfessionRegistry.HUNTER.id().toString().equals(professionId);
	}

	public void onProfessionChanged() {
		cachedArea = null;
		nextAreaRefreshTick = 0;
		resolvedTick = Integer.MIN_VALUE;
		resolvedArea = null;
		rejectedTargetId = null;
		depositResolutionTick = Integer.MIN_VALUE;
		reachableDeposit = false;
		lastWorkDiagnostic = null;
		clearShotContext();
		clearLoot();
		mob.setTarget(null);
		mob.getNavigation().stop();
		if (mob.isUsingItem()) {
			mob.stopUsingItem();
		}
		mob.setAggressive(false);
	}

	private boolean isHunterReady() {
		return !mob.level().isClientSide()
				&& mob.isAlive()
				&& !mob.isNoAi()
				&& isHunterProfession()
				&& ProfessionEquipment.isValidBow(mob.getMainHandItem());
	}

	private HuntingAreaContext resolveArea() {
		if (!(mob.level() instanceof ServerLevel level)) {
			return null;
		}
		if (mob.tickCount >= nextAreaRefreshTick) {
			Community community = findCommunity(level);
			if (community == null) {
				cachedArea = null;
				nextAreaRefreshTick = mob.tickCount + MISSING_REFRESH_TICKS;
			} else {
				cachedCommunityId = community.id();
				HuntingArea area = HuntingAreaManager.findActive(level, community.id()).orElse(null);
				cachedArea = area == null ? null : HuntingAreaContext.from(community, area);
				nextAreaRefreshTick = mob.tickCount + ACTIVE_REFRESH_TICKS;
			}
		}
		if (cachedArea == null || !cachedArea.dimension().equals(level.dimension())) {
			return null;
		}
		return cachedArea;
	}

	private Community findCommunity(ServerLevel level) {
		Community community = cachedCommunityId == null
				? null
				: CommunityManager.findById(level.getServer(), cachedCommunityId).orElse(null);
		if (community != null && community.state() == CommunityState.ACTIVE && community.hasMember(mob.getUUID())) {
			return community;
		}
		cachedCommunityId = null;
		return CommunityManager.findCommunityForMember(level.getServer(), mob.getUUID())
				.filter(found -> found.state() == CommunityState.ACTIVE)
				.filter(found -> found.dimension().equals(level.dimension()))
				.orElse(null);
	}

	private void prepareDay(ServerLevel level) {
		long day = level.getOverworldClockTime() / 24000L;
		if (mob.getHunterKillDay() != day) {
			mob.setHunterKillDay(day);
			mob.setHunterDailyKills(0);
		}
	}

	private boolean recordKill(BlockPos position, Set<UUID> baseline) {
		if (!(mob.level() instanceof ServerLevel level)) {
			return false;
		}
		HuntingAreaContext context = area().orElse(null);
		if (context == null || !context.contains(position)) {
			return false;
		}
		prepareDay(level);
		if (mob.getHunterDailyKills() >= DAILY_KILL_LIMIT) {
			return false;
		}
		mob.setHunterDailyKills(mob.getHunterDailyKills() + 1);
		pendingLoot = new PendingLoot(position.immutable(), Set.copyOf(baseline), mob.tickCount + LOOT_WAIT_TICKS, false);
		lootScanLogged = false;
		return true;
	}

	private boolean wasKilledByHunter(LivingEntity target, DamageSource eventSource) {
		if (target.getLastHurtByMob() == mob) {
			return true;
		}
		DamageSource source = target.getLastDamageSource();
		return isFromHunter(source) || isFromHunter(eventSource);
	}

	private boolean isFromHunter(DamageSource source) {
		if (source == null) {
			return false;
		}
		Entity sourceEntity = source.getEntity();
		Entity directEntity = source.getDirectEntity();
		return sourceEntity == mob
				|| directEntity == mob
				|| sourceEntity instanceof Projectile projectile && projectile.getOwner() == mob
				|| directEntity instanceof Projectile projectile && projectile.getOwner() == mob;
	}

	private void expireShotContext() {
		if (shotTargetId != null && mob.tickCount > shotContextUntilTick) {
			clearShotContext();
		}
	}

	private void clearShotContext() {
		shotTargetId = null;
		shotItemBaseline = Set.of();
		shotContextUntilTick = 0;
	}

	private ItemStack findArrowSupply() {
		for (int slot = 0; slot < mob.getWorkInventory().getContainerSize(); slot++) {
			ItemStack stack = mob.getWorkInventory().getItem(slot);
			if (stack.is(ItemTags.ARROWS) && !stack.isEmpty()) {
				return stack;
			}
		}
		return ItemStack.EMPTY;
	}

	private boolean hasDepositableWorkItems() {
		for (int slot = 0; slot < mob.getWorkInventory().getContainerSize(); slot++) {
			ItemStack stack = mob.getWorkInventory().getItem(slot);
			if (!stack.isEmpty() && !stack.is(ItemTags.ARROWS) && mob.canDepositWorkItem(stack)) {
				return true;
			}
		}
		return false;
	}

	private boolean hasReachableDeposit() {
		if (depositResolutionTick == mob.tickCount) {
			return reachableDeposit;
		}
		depositResolutionTick = mob.tickCount;
		reachableDeposit = CommunityDepositGoal.hasReachableDeposit(mob);
		return reachableDeposit;
	}

	private void logWorkDiagnostic(
			String blockedBy,
			boolean huntingArea,
			boolean ammunition,
			boolean capacity,
			boolean depositable,
			boolean depositReachable
	) {
		int arrows = 0;
		for (int slot = 0; slot < mob.getWorkInventory().getContainerSize(); slot++) {
			ItemStack stack = mob.getWorkInventory().getItem(slot);
			if (stack.is(ItemTags.ARROWS)) {
				arrows += stack.count();
			}
		}
		String diagnostic = blockedBy + "|profession=" + mob.getProfessionId()
				+ "|bow=" + ProfessionEquipment.isValidBow(mob.getMainHandItem())
				+ "|area=" + huntingArea + "|arrows=" + arrows
				+ "|depositable=" + depositable + "|reachableDeposit=" + depositReachable
				+ "|pendingLoot=" + hasPendingLoot() + "|capacity=" + capacity
				+ "|ammunition=" + ammunition;
		if (!diagnostic.equals(lastWorkDiagnostic)) {
			lastWorkDiagnostic = diagnostic;
			FantasyFolk.LOGGER.debug("Hunter diagnostics: work state hunter={} {}", mob.getUUID(), diagnostic);
		}
	}

	private float rangedAttackUncertainty(ServerLevel level) {
		return 0.1F;
	}

	private boolean isRejected(LivingEntity target) {
		if (rejectedTargetId == null) {
			return false;
		}
		if (mob.tickCount >= rejectedTargetUntilTick) {
			rejectedTargetId = null;
			return false;
		}
		return rejectedTargetId.equals(target.getUUID());
	}

	private boolean isEligibleLoot(ItemEntity item) {
		if (item.getOwner() != null && item.getOwner() != mob) {
			return false;
		}
		ItemStack stack = item.getItem();
		return !stack.isEmpty() && !stack.is(ItemTags.ARROWS) && !stack.is(Items.BOW)
				&& !stack.is(Items.CROSSBOW)
				&& HunterTargetPolicy.isPotentialLoot(stack)
				&& com.fantasyfolk.profession.ProfessionEquipment.professionFor(stack).isEmpty();
	}

	private record PendingLoot(BlockPos position, Set<UUID> baseline, int expiresAt, boolean collectedAny) {
	}
}
