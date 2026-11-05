package com.fantasyfolk.ai.guard;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.illager.Pillager;
import net.minecraft.world.entity.monster.skeleton.AbstractSkeleton;
import net.minecraft.world.entity.monster.spider.Spider;
import net.minecraft.world.entity.monster.zombie.Zombie;
import net.minecraft.world.entity.monster.zombie.ZombifiedPiglin;

/** Central allow-list for threats a community Guard may engage. */
public final class GuardThreatPolicy {
	private GuardThreatPolicy() {
	}

	public static boolean isProactiveThreat(LivingEntity entity) {
		return isProactiveThreatType(entity.getClass());
	}

	public static boolean isReactiveThreat(LivingEntity entity) {
		return isReactiveThreatType(entity.getClass());
	}

	public static boolean isProactiveThreatType(Class<?> type) {
		boolean allowedZombie = Zombie.class.isAssignableFrom(type)
				&& !ZombifiedPiglin.class.isAssignableFrom(type);
		return allowedZombie
				|| AbstractSkeleton.class.isAssignableFrom(type)
				|| Pillager.class.isAssignableFrom(type);
	}

	public static boolean isReactiveThreatType(Class<?> type) {
		return isProactiveThreatType(type) || Spider.class.isAssignableFrom(type);
	}
}
