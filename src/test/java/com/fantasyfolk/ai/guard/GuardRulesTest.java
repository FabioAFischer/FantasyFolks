package com.fantasyfolk.ai.guard;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.UUID;

import net.minecraft.SharedConstants;
import net.minecraft.core.BlockPos;
import net.minecraft.server.Bootstrap;
import net.minecraft.world.entity.monster.Creeper;
import net.minecraft.world.entity.monster.Enderman;
import net.minecraft.world.entity.monster.illager.Pillager;
import net.minecraft.world.entity.monster.skeleton.Bogged;
import net.minecraft.world.entity.monster.skeleton.Skeleton;
import net.minecraft.world.entity.monster.skeleton.Stray;
import net.minecraft.world.entity.monster.spider.Spider;
import net.minecraft.world.entity.monster.zombie.Drowned;
import net.minecraft.world.entity.monster.zombie.Husk;
import net.minecraft.world.entity.monster.zombie.Zombie;
import net.minecraft.world.entity.monster.zombie.ZombieVillager;
import net.minecraft.world.entity.monster.zombie.ZombifiedPiglin;
import net.minecraft.world.level.Level;

import com.fantasyfolk.profession.ProfessionRegistry;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

class GuardRulesTest {
	@BeforeAll
	static void bootstrapMinecraftRegistries() {
		SharedConstants.tryDetectVersion();
		Bootstrap.bootStrap();
	}

	@Test
	void proactiveThreatsUseAnExplicitAllowList() {
		assertTrue(GuardThreatPolicy.isProactiveThreatType(Zombie.class));
		assertTrue(GuardThreatPolicy.isProactiveThreatType(Husk.class));
		assertTrue(GuardThreatPolicy.isProactiveThreatType(Drowned.class));
		assertTrue(GuardThreatPolicy.isProactiveThreatType(ZombieVillager.class));
		assertTrue(GuardThreatPolicy.isProactiveThreatType(Skeleton.class));
		assertTrue(GuardThreatPolicy.isProactiveThreatType(Stray.class));
		assertTrue(GuardThreatPolicy.isProactiveThreatType(Bogged.class));
		assertTrue(GuardThreatPolicy.isProactiveThreatType(Pillager.class));

		assertFalse(GuardThreatPolicy.isProactiveThreatType(ZombifiedPiglin.class));
		assertFalse(GuardThreatPolicy.isProactiveThreatType(Creeper.class));
		assertFalse(GuardThreatPolicy.isProactiveThreatType(Enderman.class));
		assertFalse(GuardThreatPolicy.isProactiveThreatType(Spider.class));
	}

	@Test
	void spidersAreReactiveButCreepersRemainExcluded() {
		assertTrue(GuardThreatPolicy.isReactiveThreatType(Spider.class));
		assertFalse(GuardThreatPolicy.isReactiveThreatType(Creeper.class));
		assertFalse(GuardThreatPolicy.isReactiveThreatType(Enderman.class));
	}

	@Test
	void patrolRadiusNeverExceedsCommunityOrConfiguredLimit() {
		GuardCommunityContext normal = context(32);
		GuardCommunityContext small = context(12);

		assertEquals(24, normal.patrolRadius());
		assertEquals(12, small.patrolRadius());
		assertTrue(normal.containsPatrolPosition(new BlockPos(24, 200, 0)));
		assertFalse(normal.containsPatrolPosition(new BlockPos(25, 64, 0)));
		assertTrue(small.contains(new BlockPos(0, -20, 12)));
		assertFalse(small.contains(new BlockPos(9, 64, 9)));
	}

	@Test
	void onlyTheGuardProfessionActivatesGuardRuntime() {
		assertTrue(GuardRuntime.isGuardProfession(ProfessionRegistry.GUARD.id().toString()));
		assertFalse(GuardRuntime.isGuardProfession(ProfessionRegistry.HUNTER.id().toString()));
		assertFalse(GuardRuntime.isGuardProfession(null));
	}

	private static GuardCommunityContext context(int radius) {
		return new GuardCommunityContext(
				UUID.randomUUID(),
				Level.OVERWORLD,
				new BlockPos(0, 64, 0),
				radius
		);
	}
}
