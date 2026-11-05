package com.fantasyfolk.entity;

import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.RandomStrollGoal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

import com.fantasyfolk.community.Community;
import com.fantasyfolk.community.CommunityManager;
import com.fantasyfolk.profession.ProfessionHolder;
import com.fantasyfolk.profession.ProfessionPersistence;

import java.util.Optional;
import java.util.UUID;

/**
 * Neutral, persistent Goblin prototype.
 */
public class Goblin extends PathfinderMob implements ProfessionHolder {
	private String professionId;
	private ItemStack professionEquipment = ItemStack.EMPTY;

	public Goblin(EntityType<? extends Goblin> entityType, Level level) {
		super(entityType, level);
		setPersistenceRequired();
	}

	public static AttributeSupplier.Builder createAttributes() {
		return Mob.createMobAttributes()
				.add(Attributes.MAX_HEALTH, 20.0D)
				.add(Attributes.MOVEMENT_SPEED, 0.28D);
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
		return professionEquipment.copy();
	}

	@Override
	public void setProfession(String professionId, ItemStack equipment) {
		this.professionId = professionId;
		this.professionEquipment = equipment.isEmpty() ? ItemStack.EMPTY : equipment.copyWithCount(1);
		setItemSlot(EquipmentSlot.MAINHAND, this.professionEquipment.copy());
	}

	@Override
	public void clearProfession() {
		professionId = null;
		professionEquipment = ItemStack.EMPTY;
		setItemSlot(EquipmentSlot.MAINHAND, ItemStack.EMPTY);
	}

	@Override
	protected void addAdditionalSaveData(ValueOutput output) {
		super.addAdditionalSaveData(output);
		ProfessionPersistence.save(this, output);
	}

	@Override
	protected void readAdditionalSaveData(ValueInput input) {
		super.readAdditionalSaveData(input);
		ProfessionPersistence.load(this, input);
	}

	@Override
	protected void registerGoals() {
		goalSelector.addGoal(6, new RandomStrollGoal(this, 1.0D));
		goalSelector.addGoal(7, new LookAtPlayerGoal(this, Player.class, 8.0F));
		goalSelector.addGoal(8, new RandomLookAroundGoal(this));
	}
}
