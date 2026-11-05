package com.fantasyfolk.community;

import com.mojang.serialization.JsonOps;

import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponentMap;
import net.minecraft.SharedConstants;
import net.minecraft.server.Bootstrap;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;

import com.fantasyfolk.item.CommunityLedgerData;

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
		Items.BOOK.builtInRegistryHolder().bindComponents(DataComponentMap.EMPTY);
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

	@Test
	void communityTerritoryUsesThePersistedHorizontalRadius() {
		Community community = new Community(
				UUID.randomUUID(),
				CommunityType.CUSTOM,
				"territory",
				Level.OVERWORLD,
				new BlockPos(0, 64, 0),
				5,
				CommunityState.ACTIVE
		);

		assertTrue(community.isWithinTerritory(new BlockPos(3, 200, 4)));
		assertFalse(community.isWithinTerritory(new BlockPos(4, 64, 4)));
	}

	@Test
	void ledgerSelectionPersistsOnlyTheCommunityReference() {
		Community community = new Community(
				UUID.randomUUID(),
				CommunityType.VANILLA,
				"persistent",
				Level.NETHER,
				new BlockPos(4, 70, -9),
				32,
				CommunityState.ACTIVE
		);
		ItemStack ledger = new ItemStack(Items.BOOK);

		CommunityLedgerData.select(ledger, community);

		CommunityLedgerData.Selection selection = CommunityLedgerData.read(ledger).orElseThrow();
		assertEquals(community.id(), selection.id());
		assertEquals(community.dimension(), selection.dimension());
	}

	@Test
	void professionPriorityDoesNotCancelACommunityConfirmation() {
		UUID playerId = UUID.randomUUID();
		ConfirmationService.Request request = new ConfirmationService.Request(
				"community.link",
				"entity",
				"community",
				ItemStack.EMPTY
		);

		assertFalse(ConfirmationService.checkOrArm(playerId, request, 100));
		ConfirmationService.cancelIfActionStartsWith(playerId, "profession.");
		assertTrue(ConfirmationService.checkOrArm(playerId, request, 101));
	}
}
