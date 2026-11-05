package com.fantasyfolk.profession;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import net.minecraft.SharedConstants;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.component.DataComponentMap;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.Bootstrap;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.storage.TagValueInput;
import net.minecraft.world.level.storage.TagValueOutput;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

class ProfessionDataTest {
	@BeforeAll
	static void bootstrapMinecraftRegistries() {
		SharedConstants.tryDetectVersion();
		Bootstrap.bootStrap();
		bindItemComponents(Items.BOW);
		bindItemComponents(Items.IRON_SWORD);
		bindItemComponents(Items.STICK);
	}

	private static void bindItemComponents(net.minecraft.world.item.Item item) {
		item.builtInRegistryHolder().bindComponents(DataComponentMap.EMPTY);
	}

	@Test
	void equipmentMappingRecognizesSwordsAndBowOnly() {
		assertEquals(ProfessionRegistry.HUNTER, ProfessionEquipment.professionFor(new ItemStack(Items.BOW)).orElseThrow());
		assertTrue(ProfessionEquipment.professionFor(new ItemStack(Items.STICK)).isEmpty());
		assertTrue(ProfessionRegistry.all().containsKey(ProfessionRegistry.GUARD.id()));
		assertFalse(ProfessionEquipment.isSword(new ItemStack(Items.STICK)));
	}

	@Test
	void persistenceKeepsUnknownProfessionIdAndItemComponents() {
		ItemStack equipment = new ItemStack(Items.BOW);
		equipment.set(DataComponents.DAMAGE, 7);
		equipment.set(DataComponents.CUSTOM_NAME, Component.literal("bow with history"));
		TestHolder original = new TestHolder();
		original.setProfession("fantasyfolk:future_profession", equipment);

		TagValueOutput output = TagValueOutput.createWithContext(
				ProblemReporter.DISCARDING,
				RegistryAccess.fromRegistryOfRegistries(BuiltInRegistries.REGISTRY)
		);
		ProfessionPersistence.save(original, output);
		CompoundTag tag = output.buildResult();

		TestHolder restored = new TestHolder();
		ProfessionPersistence.load(restored, TagValueInput.create(
				ProblemReporter.DISCARDING,
				RegistryAccess.fromRegistryOfRegistries(BuiltInRegistries.REGISTRY),
				tag
		));

		assertEquals("fantasyfolk:future_profession", restored.getProfessionId());
		assertTrue(ProfessionRegistry.byId(restored.getProfessionId()).isEmpty());
		assertTrue(ItemStack.matches(equipment.copyWithCount(1), restored.getProfessionEquipment()));
	}

	private static final class TestHolder implements ProfessionHolder {
		private String id;
		private ItemStack equipment = ItemStack.EMPTY;

		@Override
		public String getProfessionId() {
			return id;
		}

		@Override
		public ItemStack getProfessionEquipment() {
			return equipment.copy();
		}

		@Override
		public void setProfession(String professionId, ItemStack equipment) {
			this.id = professionId;
			this.equipment = equipment.isEmpty() ? ItemStack.EMPTY : equipment.copyWithCount(1);
		}

		@Override
		public void clearProfession() {
			id = null;
			equipment = ItemStack.EMPTY;
		}
	}
}
