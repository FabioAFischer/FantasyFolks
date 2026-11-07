package com.fantasyfolk.community;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.UUID;

import com.fantasyfolk.ai.hunter.HuntingAreaContext;
import com.mojang.serialization.JsonOps;

import net.minecraft.SharedConstants;
import net.minecraft.core.BlockPos;
import net.minecraft.server.Bootstrap;
import net.minecraft.world.level.Level;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

class HuntingAreaDataTest {
	@BeforeAll
	static void bootstrapMinecraftRegistries() {
		SharedConstants.tryDetectVersion();
		Bootstrap.bootStrap();
	}

	@Test
	void storageCodecPreservesAreaOwnershipLocationAndState() {
		HuntingAreaStorage storage = new HuntingAreaStorage();
		UUID communityId = UUID.randomUUID();
		HuntingArea area = new HuntingArea(communityId, Level.OVERWORLD, new BlockPos(20, 70, -4));
		area.deactivate();
		storage.add(area);

		var encoded = HuntingAreaStorage.TYPE.codec().encodeStart(JsonOps.INSTANCE, storage).result().orElseThrow();
		HuntingAreaStorage restored = HuntingAreaStorage.TYPE.codec().parse(JsonOps.INSTANCE, encoded).result().orElseThrow();
		HuntingArea restoredArea = restored.get(communityId);

		assertEquals(communityId, restoredArea.communityId());
		assertEquals(new BlockPos(20, 70, -4), restoredArea.center());
		assertEquals(HuntingArea.DEFAULT_RADIUS, restoredArea.radius());
		assertEquals(HuntingAreaState.INACTIVE, restoredArea.state());
	}

	@Test
	void operationalAreaUsesTheIntersectionWithCommunityTerritory() {
		HuntingAreaContext context = new HuntingAreaContext(
				UUID.randomUUID(),
				Level.OVERWORLD,
				new BlockPos(10, 64, 0),
				12,
				new BlockPos(0, 64, 0),
				12
		);

		assertTrue(context.contains(new BlockPos(10, 200, 0)));
		assertFalse(context.contains(new BlockPos(22, 64, 0)));
		assertFalse(context.contains(new BlockPos(0, 64, 13)));
	}
}
