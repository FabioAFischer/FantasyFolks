package com.fantasyfolk.ai.hunter;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.UUID;

import net.minecraft.SharedConstants;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponentMap;
import net.minecraft.server.Bootstrap;
import net.minecraft.world.entity.animal.chicken.Chicken;
import net.minecraft.world.entity.animal.cow.Cow;
import net.minecraft.world.entity.animal.pig.Pig;
import net.minecraft.world.entity.animal.rabbit.Rabbit;
import net.minecraft.world.entity.animal.sheep.Sheep;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

class HunterRulesTest {
	@BeforeAll
	static void bootstrapMinecraftRegistries() {
		SharedConstants.tryDetectVersion();
		Bootstrap.bootStrap();
		Items.BEEF.builtInRegistryHolder().bindComponents(DataComponentMap.EMPTY);
		Items.ARROW.builtInRegistryHolder().bindComponents(DataComponentMap.EMPTY);
		Items.BOW.builtInRegistryHolder().bindComponents(DataComponentMap.EMPTY);
	}

	@Test
	void allowListContainsOnlyTheFirstFiveHuntableSpecies() {
		assertEquals(5, HunterTargetPolicy.allowedSpecies().size());
		assertEquals(Cow.class, HunterTargetPolicy.speciesOfType(Cow.class));
		assertEquals(Pig.class, HunterTargetPolicy.speciesOfType(Pig.class));
		assertEquals(Chicken.class, HunterTargetPolicy.speciesOfType(Chicken.class));
		assertEquals(Sheep.class, HunterTargetPolicy.speciesOfType(Sheep.class));
		assertEquals(Rabbit.class, HunterTargetPolicy.speciesOfType(Rabbit.class));
	}

	@Test
	void lootPolicyAcceptsVanillaDropsAndRejectsSupplies() {
		assertTrue(HunterTargetPolicy.isPotentialLoot(new ItemStack(Items.BEEF)));
		assertFalse(HunterTargetPolicy.isPotentialLoot(new ItemStack(Items.ARROW)));
		assertFalse(HunterTargetPolicy.isPotentialLoot(new ItemStack(Items.BOW)));
	}

	@Test
	void configuredAreaIsHorizontalAndDimensionIndependentInItsContext() {
		HuntingAreaContext context = new HuntingAreaContext(
				UUID.randomUUID(), Level.NETHER, new BlockPos(0, 64, 0), 12,
				new BlockPos(0, 64, 0), 32
		);
		assertTrue(context.contains(new BlockPos(0, -20, 12)));
		assertFalse(context.contains(new BlockPos(13, 64, 0)));
	}
}
