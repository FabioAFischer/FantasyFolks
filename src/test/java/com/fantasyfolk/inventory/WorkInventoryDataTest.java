package com.fantasyfolk.inventory;

import static org.junit.jupiter.api.Assertions.assertEquals;

import net.minecraft.SharedConstants;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.component.DataComponentMap;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.Bootstrap;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.storage.TagValueInput;
import net.minecraft.world.level.storage.TagValueOutput;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

class WorkInventoryDataTest {
	@BeforeAll
	static void bootstrapMinecraftRegistries() {
		SharedConstants.tryDetectVersion();
		Bootstrap.bootStrap();
		Items.STICK.builtInRegistryHolder().bindComponents(DataComponentMap.EMPTY);
	}

	@Test
	void workInventoryPersistsRealItemStacksAndCapacity() {
		WorkInventory original = new WorkInventory();
		ItemStack stick = new ItemStack(Items.STICK, 5);
		stick.set(DataComponents.MAX_STACK_SIZE, 64);
		original.setItem(0, stick);

		TagValueOutput output = TagValueOutput.createWithContext(
				ProblemReporter.DISCARDING,
				RegistryAccess.fromRegistryOfRegistries(BuiltInRegistries.REGISTRY)
		);
		original.save(output);
		CompoundTag tag = output.buildResult();

		WorkInventory restored = new WorkInventory();
		restored.load(TagValueInput.create(
				ProblemReporter.DISCARDING,
				RegistryAccess.fromRegistryOfRegistries(BuiltInRegistries.REGISTRY),
				tag
		));

		assertEquals(9, restored.getContainerSize());
		assertEquals(Items.STICK, restored.getItem(0).getItem());
		assertEquals(5, restored.getItem(0).count());
	}
}
