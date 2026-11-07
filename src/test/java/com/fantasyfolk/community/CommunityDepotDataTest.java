package com.fantasyfolk.community;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.UUID;

import com.mojang.serialization.JsonOps;

import net.minecraft.SharedConstants;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.component.DataComponentMap;
import net.minecraft.server.Bootstrap;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

class CommunityDepotDataTest {
	@BeforeAll
	static void bootstrapMinecraftRegistries() {
		SharedConstants.tryDetectVersion();
		Bootstrap.bootStrap();
		Items.STICK.builtInRegistryHolder().bindComponents(DataComponentMap.EMPTY);
		Items.BOOK.builtInRegistryHolder().bindComponents(DataComponentMap.EMPTY);
	}

	@Test
	void depotStorageCodecPreservesOwnershipAndDoubleChestCoordinates() {
		CommunityDepotStorage storage = new CommunityDepotStorage();
		UUID communityId = UUID.randomUUID();
		CommunityDepot depot = new CommunityDepot(
				UUID.randomUUID(),
				communityId,
				Level.OVERWORLD,
				new BlockPos(4, 64, 4),
				new BlockPos(5, 64, 4),
				DepotType.CHEST
		);
		storage.add(depot);

		var encoded = CommunityDepotStorage.TYPE.codec().encodeStart(JsonOps.INSTANCE, storage).result().orElseThrow();
		CommunityDepotStorage restored = CommunityDepotStorage.TYPE.codec().parse(JsonOps.INSTANCE, encoded).result().orElseThrow();
		CommunityDepot restoredDepot = restored.get(depot.id());

		assertEquals(communityId, restoredDepot.communityId());
		assertEquals(depot.primary(), restoredDepot.primary());
		assertEquals(depot.secondary(), restoredDepot.secondary());
		assertEquals(DepotType.CHEST, restoredDepot.type());
	}

	@Test
	void transferMovesOnlyTheAmountAcceptedByTheDestination() {
		ItemStack sourceStack = stack(Items.STICK, 8);
		ItemStack destinationStack = stack(Items.STICK, 63);
		SimpleContainer source = new SimpleContainer(sourceStack);
		SimpleContainer destination = new SimpleContainer(destinationStack);

		int transferred = CommunityItemTransfer.transfer(source, 0, destination, 8);

		assertEquals(1, transferred);
		assertEquals(7, source.getItem(0).count());
		assertEquals(64, destination.getItem(0).count());
	}

	@Test
	void transferLeavesSourceUntouchedWhenDestinationCannotAccept() {
		SimpleContainer source = new SimpleContainer(stack(Items.STICK, 8));
		SimpleContainer destination = new SimpleContainer(new ItemStack(Items.BOOK, 64));

		int transferred = CommunityItemTransfer.transfer(source, 0, destination, 8);

		assertEquals(0, transferred);
		assertEquals(8, source.getItem(0).count());
		assertTrue(destination.getItem(0).is(Items.BOOK));
	}

	private static ItemStack stack(net.minecraft.world.item.Item item, int count) {
		ItemStack stack = new ItemStack(item, count);
		stack.set(DataComponents.MAX_STACK_SIZE, 64);
		return stack;
	}
}
