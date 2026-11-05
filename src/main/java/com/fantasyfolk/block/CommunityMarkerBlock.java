package com.fantasyfolk.block;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;

import com.fantasyfolk.community.CommunityManager;

/** The player-placeable center for a CUSTOM community. */
public final class CommunityMarkerBlock extends Block {
	public static CommunityMarkerBlock INSTANCE;

	public CommunityMarkerBlock(BlockBehaviour.Properties properties) {
		super(properties);
		INSTANCE = this;
	}

	@Override
	public void setPlacedBy(Level level, BlockPos pos, BlockState state, LivingEntity placer, ItemStack stack) {
		super.setPlacedBy(level, pos, state, placer, stack);
		if (!(level instanceof ServerLevel serverLevel)) {
			return;
		}

		CommunityManager.RegistrationResult result = CommunityManager.registerCustom(serverLevel, pos);
		if (result.status() == CommunityManager.RegistrationStatus.REGISTERED) {
			if (placer instanceof ServerPlayer player) {
				player.sendSystemMessage(Component.literal(
						"Comunidade CUSTOM criada: " + result.community().id()
				));
			}
			return;
		}

		serverLevel.removeBlock(pos, false);
		if (placer instanceof ServerPlayer player) {
			player.sendSystemMessage(Component.literal(registrationFailureMessage(result)));
		}
	}

	@Override
	public void playerDestroy(
			ServerLevel level,
			ServerPlayer player,
			BlockPos pos,
			BlockState state,
			net.minecraft.world.level.block.entity.BlockEntity blockEntity,
			ItemStack tool
	) {
		super.playerDestroy(level, player, pos, state, blockEntity, tool);
		CommunityManager.markInactiveAt(level, pos);
	}

	@Override
	public void destroy(LevelAccessor level, BlockPos pos, BlockState state) {
		super.destroy(level, pos, state);
		if (level instanceof ServerLevel serverLevel) {
			CommunityManager.markInactiveAt(serverLevel, pos);
		}
	}

	private static String registrationFailureMessage(CommunityManager.RegistrationResult result) {
		return switch (result.status()) {
			case ALREADY_REGISTERED -> "Já existe uma comunidade neste centro.";
			case OVERLAPS -> "O território do marco sobrepõe uma comunidade existente.";
			case INVALID_VILLAGE -> "O sino não atende aos critérios de uma vila vanilla.";
			case REGISTERED -> "Comunidade criada.";
		};
	}
}
