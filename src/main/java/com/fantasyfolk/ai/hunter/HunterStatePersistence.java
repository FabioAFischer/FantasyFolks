package com.fantasyfolk.ai.hunter;

import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

/** NBT/value I/O for the hunter's persistent daily quota. */
public final class HunterStatePersistence {
	private static final String DAILY_KILLS = "fantasyfolk_hunter_daily_kills";
	private static final String KILL_DAY = "fantasyfolk_hunter_kill_day";

	private HunterStatePersistence() {
	}

	public static void save(HunterStateHolder holder, ValueOutput output) {
		if (holder.getHunterDailyKills() > 0 || holder.getHunterKillDay() > 0) {
			output.putInt(DAILY_KILLS, holder.getHunterDailyKills());
			output.putLong(KILL_DAY, holder.getHunterKillDay());
		}
	}

	public static void load(HunterStateHolder holder, ValueInput input) {
		holder.setHunterDailyKills(Math.max(0, input.getIntOr(DAILY_KILLS, 0)));
		holder.setHunterKillDay(Math.max(0L, input.getLongOr(KILL_DAY, 0L)));
	}
}
