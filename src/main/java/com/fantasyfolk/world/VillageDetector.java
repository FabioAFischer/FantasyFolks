package com.fantasyfolk.world;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.ai.village.poi.PoiManager;
import net.minecraft.world.entity.ai.village.poi.PoiType;
import net.minecraft.world.entity.ai.village.poi.PoiTypes;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.entity.EntityTypeTest;
import net.minecraft.world.phys.AABB;

import java.util.function.Predicate;

/** Local, non-invasive recognition of an existing vanilla village. */
public final class VillageDetector {
	private static final Predicate<Holder<PoiType>> SUPPORTING_POI = holder -> !holder.is(PoiTypes.MEETING);

	private VillageDetector() {
	}

	public static boolean isRecognizableVillage(ServerLevel level, BlockPos bellPos) {
		if (!hasBellAnchor(level, bellPos)) {
			return false;
		}

		PoiManager poiManager = level.getPoiManager();
		boolean isMeetingPoint = poiManager.existsAtPosition(PoiTypes.MEETING, bellPos);
		long supportingPois = poiManager.getCountInRange(
				SUPPORTING_POI,
				bellPos,
				32,
				PoiManager.Occupancy.ANY
		);
		int villagers = level.getEntities(
				EntityTypeTest.forClass(Villager.class),
				new AABB(
						bellPos.getX() - 32,
						bellPos.getY() - 32,
						bellPos.getZ() - 32,
						bellPos.getX() + 33,
						bellPos.getY() + 33,
						bellPos.getZ() + 33
				),
				villager -> true
		).size();

		// A bell alone is not enough: require the meeting POI, a loaded villager,
		// and at least one other POI such as a bed or workstation.
		return isMeetingPoint && villagers > 0 && supportingPois > 0;
	}

	public static boolean hasBellAnchor(ServerLevel level, BlockPos pos) {
		BlockState state = level.getBlockState(pos);
		return state.is(Blocks.BELL);
	}
}
