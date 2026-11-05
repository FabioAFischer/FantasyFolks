package com.fantasyfolk.community;

import com.mojang.serialization.JsonOps;

import net.minecraft.core.BlockPos;
import net.minecraft.SharedConstants;
import net.minecraft.server.Bootstrap;
import net.minecraft.world.level.Level;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CommunityDataTest {
	@BeforeAll
	static void bootstrapMinecraftRegistries() {
		SharedConstants.tryDetectVersion();
		Bootstrap.bootStrap();
	}

	@Test
	void aMemberCanOnlyBeAddedOnceAndMustUseTheCommunityDimension() {
		Community community = new Community(
				UUID.randomUUID(),
				CommunityType.CUSTOM,
				"test",
				Level.OVERWORLD,
				new BlockPos(10, 64, 10),
				CommunityManager.INITIAL_RADIUS,
				CommunityState.ACTIVE
		);
		UUID goblinId = UUID.randomUUID();

		assertTrue(community.addMember(goblinId, Level.OVERWORLD));
		assertFalse(community.addMember(goblinId, Level.OVERWORLD));
		assertFalse(community.addMember(UUID.randomUUID(), Level.NETHER));
		assertEquals(1, community.members().size());
	}

	@Test
	void communityStorageCodecPreservesCommunityAndLinks() {
		CommunityStorage storage = new CommunityStorage();
		Community community = new Community(
				UUID.randomUUID(),
				CommunityType.VANILLA,
				"vanilla-test",
				Level.OVERWORLD,
				new BlockPos(-20, 70, 35),
				CommunityManager.INITIAL_RADIUS,
				CommunityState.INACTIVE
		);
		UUID goblinId = UUID.randomUUID();
		assertTrue(community.addMember(goblinId, Level.OVERWORLD));
		storage.add(community);

		var encoded = CommunityStorage.TYPE.codec().encodeStart(JsonOps.INSTANCE, storage).result().orElseThrow();
		CommunityStorage decoded = CommunityStorage.TYPE.codec().parse(JsonOps.INSTANCE, encoded).result().orElseThrow();
		Community restored = decoded.get(community.id());

		assertEquals(community.id(), restored.id());
		assertEquals(community.type(), restored.type());
		assertEquals(community.dimension(), restored.dimension());
		assertEquals(community.center(), restored.center());
		assertEquals(CommunityState.INACTIVE, restored.state());
		assertTrue(restored.hasMember(goblinId));
	}
}
