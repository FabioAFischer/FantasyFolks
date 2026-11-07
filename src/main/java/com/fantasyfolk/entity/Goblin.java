package com.fantasyfolk.entity;

import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.server.level.ServerLevel;

import com.fantasyfolk.ai.ProfessionAwareRandomStrollGoal;
import com.fantasyfolk.ai.ProfessionGoalController;
import com.fantasyfolk.ai.hunter.HunterStateHolder;
import com.fantasyfolk.ai.hunter.HunterStatePersistence;
import com.fantasyfolk.community.Community;
import com.fantasyfolk.community.CommunityManager;
import com.fantasyfolk.inventory.WorkInventory;
import com.fantasyfolk.inventory.WorkInventoryHolder;
import com.fantasyfolk.inventory.WorkInventoryPersistence;
import com.fantasyfolk.profession.ProfessionHolder;
import com.fantasyfolk.profession.ProfessionPersistence;
import com.fantasyfolk.profession.ProfessionRegistry;

import java.util.Optional;
import java.util.UUID;

/**
 * Neutral, persistent Goblin prototype.
 */
public class Goblin extends PathfinderMob implements ProfessionHolder, WorkInventoryHolder, HunterStateHolder {
	private String professionId;
	private ProfessionGoalController<Goblin> professionGoals;
	private final WorkInventory workInventory = new WorkInventory();
	private int hunterDailyKills;
	private long hunterKillDay;

	public Goblin(EntityType<? extends Goblin> entityType, Level level) {
		super(entityType, level);
		setPersistenceRequired();
	}

	public static AttributeSupplier.Builder createAttributes() {
		return Mob.createMobAttributes()
				.add(Attributes.MAX_HEALTH, 20.0D)
				.add(Attributes.MOVEMENT_SPEED, 0.28D)
				.add(Attributes.ATTACK_DAMAGE, 4.0D)
				.add(Attributes.FOLLOW_RANGE, 16.0D);
	}

	/** Resolves the persisted community link without retaining unloaded entity references. */
	public Optional<UUID> getCommunityId() {
		if (level().isClientSide() || level().getServer() == null) {
			return Optional.empty();
		}
		return CommunityManager.findCommunityForMember(level().getServer(), getUUID()).map(Community::id);
	}

	@Override
	public String getProfessionId() {
		return professionId;
	}

	@Override
	public ItemStack getProfessionEquipment() {
		return getMainHandItem().copy();
	}

	@Override
	public WorkInventory getWorkInventory() {
		return workInventory;
	}

	@Override
	public boolean canDepositWorkItem(ItemStack stack) {
		return !(ProfessionRegistry.HUNTER.id().toString().equals(professionId) && stack.is(ItemTags.ARROWS));
	}

	@Override
	public int getHunterDailyKills() {
		return hunterDailyKills;
	}

	@Override
	public long getHunterKillDay() {
		return hunterKillDay;
	}

	@Override
	public void setHunterDailyKills(int kills) {
		hunterDailyKills = Math.max(0, kills);
	}

	@Override
	public void setHunterKillDay(long day) {
		hunterKillDay = Math.max(0L, day);
	}

	@Override
	public void setProfession(String professionId, ItemStack equipment) {
		this.professionId = professionId;
		setItemSlot(EquipmentSlot.MAINHAND, equipment.isEmpty() ? ItemStack.EMPTY : equipment.copyWithCount(1));
		notifyProfessionChanged();
	}

	@Override
	public void clearProfession() {
		professionId = null;
		setItemSlot(EquipmentSlot.MAINHAND, ItemStack.EMPTY);
		notifyProfessionChanged();
	}

	@Override
	protected void addAdditionalSaveData(ValueOutput output) {
		super.addAdditionalSaveData(output);
		ProfessionPersistence.save(this, output);
		WorkInventoryPersistence.save(workInventory, output);
		HunterStatePersistence.save(this, output);
	}

	@Override
	protected void readAdditionalSaveData(ValueInput input) {
		super.readAdditionalSaveData(input);
		ProfessionPersistence.load(this, input);
		WorkInventoryPersistence.load(workInventory, input);
		HunterStatePersistence.load(this, input);
	}

	@Override
	protected void dropCustomDeathLoot(ServerLevel level, DamageSource source, boolean recentlyHit) {
		super.dropCustomDeathLoot(level, source, recentlyHit);
		WorkInventoryPersistence.drop(this, level, workInventory);
	}

	@Override
	protected void registerGoals() {
		professionGoals = new ProfessionGoalController<>(this);
		professionGoals.registerGoals(goalSelector, targetSelector);
		goalSelector.addGoal(7, new ProfessionAwareRandomStrollGoal(
				this,
				1.0D,
				professionGoals::allowsOrdinaryWandering
		));
		goalSelector.addGoal(8, new LookAtPlayerGoal(this, Player.class, 8.0F));
		goalSelector.addGoal(9, new RandomLookAroundGoal(this));
	}

	private void notifyProfessionChanged() {
		if (professionGoals != null) {
			professionGoals.onProfessionChanged();
		}
	}
}
