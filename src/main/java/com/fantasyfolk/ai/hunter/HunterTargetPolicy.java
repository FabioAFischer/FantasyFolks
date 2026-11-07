package com.fantasyfolk.ai.hunter;

import java.util.List;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Leashable;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.animal.chicken.Chicken;
import net.minecraft.world.entity.animal.cow.Cow;
import net.minecraft.world.entity.animal.pig.Pig;
import net.minecraft.world.entity.animal.rabbit.Rabbit;
import net.minecraft.world.entity.animal.sheep.Sheep;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.level.block.state.BlockState;

import com.fantasyfolk.community.CommunityHolder;

/** Central allow-list and protection rules for the first hunter implementation. */
public final class HunterTargetPolicy {
	private static final int MIN_ADULTS_PER_SPECIES = 2;
	private static final int ENCLOSURE_SCAN_RADIUS = 4;
	private static final int MIN_ENCLOSURE_BARRIERS = 8;
	private static final List<Class<? extends LivingEntity>> ALLOWED_SPECIES = List.of(
			Cow.class,
			Pig.class,
			Chicken.class,
			Sheep.class,
			Rabbit.class
	);

	private HunterTargetPolicy() {
	}

	public static List<Class<? extends LivingEntity>> allowedSpecies() {
		return ALLOWED_SPECIES;
	}

	public static boolean isEligible(ServerLevel level, HuntingAreaContext area, LivingEntity hunter, LivingEntity target) {
		return isBaseEligible(level, area, hunter, target)
				&& adultCount(level, area, speciesOf(target)) >= MIN_ADULTS_PER_SPECIES;
	}

	public static boolean isPotentialLoot(ItemStack stack) {
		return stack.is(Items.BEEF)
				|| stack.is(Items.LEATHER)
				|| stack.is(Items.PORKCHOP)
				|| stack.is(Items.CHICKEN)
				|| stack.is(Items.FEATHER)
				|| stack.is(Items.MUTTON)
				|| stack.is(ItemTags.WOOL)
				|| stack.is(Items.RABBIT)
				|| stack.is(Items.RABBIT_HIDE)
				|| stack.is(Items.RABBIT_FOOT);
	}

	public static boolean isAllowedSpecies(LivingEntity target) {
		return speciesOf(target) != LivingEntity.class;
	}

	public static boolean isProtectedInTrajectory(Entity entity) {
		if (entity instanceof Player || entity instanceof Villager || entity instanceof CommunityHolder) {
			return true;
		}
		return entity instanceof LivingEntity;
	}

	public static int adultCount(ServerLevel level, HuntingAreaContext area, Class<? extends LivingEntity> species) {
		if (species == null || species == LivingEntity.class) {
			return 0;
		}
		return (int) level.getEntities(
				(Entity) null,
				new net.minecraft.world.phys.AABB(area.center()).inflate(area.radius(), 32.0D, area.radius()),
				entity -> entity instanceof LivingEntity living
						&& species.isAssignableFrom(living.getClass())
						&& isBaseEligible(level, area, null, living)
			).stream().count();
	}

	public static Class<? extends LivingEntity> speciesOf(LivingEntity target) {
		return speciesOfType(target.getClass());
	}

	public static Class<? extends LivingEntity> speciesOfType(Class<?> type) {
		if (Cow.class.isAssignableFrom(type)) {
			return Cow.class;
		}
		if (Pig.class.isAssignableFrom(type)) {
			return Pig.class;
		}
		if (Chicken.class.isAssignableFrom(type)) {
			return Chicken.class;
		}
		if (Sheep.class.isAssignableFrom(type)) {
			return Sheep.class;
		}
		if (Rabbit.class.isAssignableFrom(type)) {
			return Rabbit.class;
		}
		return LivingEntity.class;
	}

	private static boolean isTamed(LivingEntity target) {
		return target instanceof TamableAnimal tamable && tamable.isTame();
	}

	private static boolean isLeashed(LivingEntity target) {
		return target instanceof Leashable leashable && leashable.isLeashed();
	}

	private static boolean isBaseEligible(
			ServerLevel level,
			HuntingAreaContext area,
			LivingEntity hunter,
			LivingEntity target
	) {
		return target != hunter
				&& target.isAlive()
				&& !target.isRemoved()
				&& (hunter == null || target.level() == hunter.level())
				&& isAllowedSpecies(target)
				&& !target.isBaby()
				&& !target.hasCustomName()
				&& !isTamed(target)
				&& !isLeashed(target)
				&& !target.isPassenger()
				&& target.getPassengers().isEmpty()
				&& !isRecognizableEnclosure(level, target)
				&& !(target instanceof CommunityHolder)
				&& area.contains(target.blockPosition());
	}

	private static boolean isRecognizableEnclosure(ServerLevel level, LivingEntity target) {
		BlockPos origin = target.blockPosition();
		int barriers = 0;
		for (int x = -ENCLOSURE_SCAN_RADIUS; x <= ENCLOSURE_SCAN_RADIUS; x++) {
			for (int z = -ENCLOSURE_SCAN_RADIUS; z <= ENCLOSURE_SCAN_RADIUS; z++) {
				for (int y = 0; y <= 1; y++) {
					BlockState state = level.getBlockState(origin.offset(x, y, z));
					if (state.is(BlockTags.FENCES) || state.is(BlockTags.FENCE_GATES) || state.is(BlockTags.WALLS)) {
						barriers++;
					}
				}
			}
		}
		return barriers >= MIN_ENCLOSURE_BARRIERS;
	}

}
