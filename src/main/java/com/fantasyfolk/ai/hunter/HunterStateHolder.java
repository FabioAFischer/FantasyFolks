package com.fantasyfolk.ai.hunter;

/** Persistent per-hunter counters used by the bounded hunting routine. */
public interface HunterStateHolder {
	int getHunterDailyKills();

	long getHunterKillDay();

	void setHunterDailyKills(int kills);

	void setHunterKillDay(long day);
}
