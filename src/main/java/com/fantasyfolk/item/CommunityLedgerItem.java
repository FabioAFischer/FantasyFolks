package com.fantasyfolk.item;

import java.util.Optional;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;

import com.fantasyfolk.block.CommunityMarkerBlock;
import com.fantasyfolk.community.Community;
import com.fantasyfolk.community.CommunityHolder;
import com.fantasyfolk.community.CommunityManager;
import com.fantasyfolk.community.CommunityPermissions;
import com.fantasyfolk.community.ConfirmationService;
import com.fantasyfolk.world.VillageDetector;

/** Reusable in-game ledger for selecting, inspecting and managing communities. */
public final class CommunityLedgerItem extends Item {
	public CommunityLedgerItem(Properties properties) {
		super(properties);
	}

	@Override
	public InteractionResult useOn(UseOnContext context) {
		if (context.getLevel().isClientSide()) {
			return InteractionResult.SUCCESS;
		}
		if (!(context.getLevel() instanceof ServerLevel level)
				|| !(context.getPlayer() instanceof ServerPlayer player)) {
			return InteractionResult.PASS;
		}

		BlockPos clickedPos = context.getClickedPos();
		if (level.getBlockState(clickedPos).is(CommunityMarkerBlock.INSTANCE)) {
			return selectRegisteredCommunity(level, player, clickedPos, context.getItemInHand());
		}
		if (level.getBlockState(clickedPos).is(Blocks.BELL)) {
			return selectOrRegisterVanilla(level, player, clickedPos, context.getItemInHand());
		}
		return InteractionResult.PASS;
	}

	@Override
	public InteractionResult use(Level level, Player player, InteractionHand hand) {
		if (level.isClientSide()) {
			return InteractionResult.SUCCESS;
		}
		if (!(level instanceof ServerLevel serverLevel) || !(player instanceof ServerPlayer serverPlayer)) {
			return InteractionResult.PASS;
		}

		ItemStack stack = serverPlayer.getItemInHand(hand);
		Optional<CommunityLedgerData.Selection> selection = CommunityLedgerData.read(stack);
		if (selection.isEmpty()) {
			serverPlayer.sendSystemMessage(Component.translatable("message.fantasyfolk.ledger.no_selection"));
			return InteractionResult.SUCCESS_SERVER;
		}

		CommunityLedgerData.Selection selected = selection.get();
		Optional<Community> community = CommunityManager.findById(serverLevel.getServer(), selected.id());
		if (community.isEmpty()) {
			serverPlayer.sendSystemMessage(Component.translatable(
					"message.fantasyfolk.ledger.missing_community",
					selected.id().toString()
			));
			return InteractionResult.SUCCESS_SERVER;
		}

		sendCommunityInfo(serverPlayer, community.get());
		return InteractionResult.SUCCESS_SERVER;
	}

	@Override
	public InteractionResult interactLivingEntity(
			ItemStack stack,
			Player player,
			LivingEntity entity,
			InteractionHand hand
	) {
		if (player.level().isClientSide()) {
			return InteractionResult.SUCCESS;
		}
		if (!(player instanceof ServerPlayer serverPlayer)
				|| !(player.level() instanceof ServerLevel level)) {
			return InteractionResult.PASS;
		}
		if (!(entity instanceof CommunityHolder holder)) {
			serverPlayer.sendSystemMessage(Component.translatable("message.fantasyfolk.ledger.unsupported_entity"));
			return InteractionResult.SUCCESS_SERVER;
		}

		Optional<CommunityLedgerData.Selection> selection = CommunityLedgerData.read(stack);
		if (selection.isEmpty()) {
			serverPlayer.sendSystemMessage(Component.translatable("message.fantasyfolk.ledger.select_first"));
			return InteractionResult.SUCCESS_SERVER;
		}
		Optional<Community> communityOptional = CommunityManager.findById(level.getServer(), selection.get().id());
		if (communityOptional.isEmpty()) {
			serverPlayer.sendSystemMessage(Component.translatable(
					"message.fantasyfolk.ledger.missing_community",
					selection.get().id().toString()
			));
			return InteractionResult.SUCCESS_SERVER;
		}
		Community community = communityOptional.get();
		if (!community.dimension().equals(selection.get().dimension())) {
			serverPlayer.sendSystemMessage(Component.translatable("message.fantasyfolk.ledger.invalid_selection"));
			return InteractionResult.SUCCESS_SERVER;
		}
		if (!community.dimension().equals(level.dimension())) {
			serverPlayer.sendSystemMessage(Component.translatable("message.fantasyfolk.ledger.wrong_dimension"));
			return InteractionResult.SUCCESS_SERVER;
		}
		if (!CommunityPermissions.canPerformPlayerAction(serverPlayer)) {
			serverPlayer.sendSystemMessage(Component.translatable("message.fantasyfolk.ledger.not_authorized"));
			return InteractionResult.SUCCESS_SERVER;
		}

		Optional<java.util.UUID> linkedCommunity = holder.getCommunityId();
		if (serverPlayer.isShiftKeyDown()) {
			return unlink(serverPlayer, holder, entity, community, linkedCommunity, stack);
		}
		return link(serverPlayer, entity, community, linkedCommunity, stack);
	}

	private static InteractionResult selectRegisteredCommunity(
			ServerLevel level,
			ServerPlayer player,
			BlockPos pos,
			ItemStack stack
	) {
		Optional<Community> community = CommunityManager.findAt(level.getServer(), level.dimension(), pos);
		if (community.isEmpty()) {
			player.sendSystemMessage(Component.translatable("message.fantasyfolk.ledger.community_not_found"));
			return InteractionResult.SUCCESS_SERVER;
		}
		select(player, stack, community.get());
		return InteractionResult.SUCCESS_SERVER;
	}

	private static InteractionResult selectOrRegisterVanilla(
			ServerLevel level,
			ServerPlayer player,
			BlockPos bellPos,
			ItemStack stack
	) {
		Optional<Community> existing = CommunityManager.findAt(level.getServer(), level.dimension(), bellPos);
		if (existing.isPresent()) {
			select(player, stack, existing.get());
			return InteractionResult.SUCCESS_SERVER;
		}
		if (!VillageDetector.isRecognizableVillage(level, bellPos)) {
			player.sendSystemMessage(Component.translatable("message.fantasyfolk.ledger.invalid_village"));
			return InteractionResult.SUCCESS_SERVER;
		}
		if (!CommunityPermissions.canPerformPlayerAction(player)) {
			player.sendSystemMessage(Component.translatable("message.fantasyfolk.ledger.not_authorized"));
			return InteractionResult.SUCCESS_SERVER;
		}

		String target = level.dimension().identifier() + ":" + bellPos.asLong();
		ConfirmationService.Request request = new ConfirmationService.Request(
				"community.register_vanilla",
				target,
				"",
				stack
		);
		long now = level.getServer().getTickCount();
		if (!ConfirmationService.checkOrArm(player.getUUID(), request, now)) {
			player.sendSystemMessage(Component.translatable("message.fantasyfolk.ledger.confirm_register"));
			return InteractionResult.SUCCESS_SERVER;
		}

		CommunityManager.RegistrationResult result = CommunityManager.registerVanilla(level, bellPos);
		if (result.community() != null
				&& (result.status() == CommunityManager.RegistrationStatus.REGISTERED
				|| result.status() == CommunityManager.RegistrationStatus.ALREADY_REGISTERED)) {
			select(player, stack, result.community());
			return InteractionResult.SUCCESS_SERVER;
		}
		player.sendSystemMessage(Component.translatable(registrationMessage(result.status())));
		return InteractionResult.SUCCESS_SERVER;
	}

	private static InteractionResult link(
			ServerPlayer player,
			LivingEntity entity,
			Community community,
			Optional<java.util.UUID> linkedCommunity,
			ItemStack stack
	) {
		if (linkedCommunity.isPresent()) {
			player.sendSystemMessage(Component.translatable(
					linkedCommunity.get().equals(community.id())
							? "message.fantasyfolk.ledger.already_linked"
							: "message.fantasyfolk.ledger.linked_other"
			));
			return InteractionResult.SUCCESS_SERVER;
		}

		ConfirmationService.Request request = new ConfirmationService.Request(
				"community.link",
				entity.getUUID().toString(),
				community.id().toString(),
				stack
		);
		if (!ConfirmationService.checkOrArm(player.getUUID(), request, player.level().getServer().getTickCount())) {
			player.sendSystemMessage(Component.translatable(
					"message.fantasyfolk.ledger.confirm_link",
					community.name()
			));
			return InteractionResult.SUCCESS_SERVER;
		}

		CommunityManager.LinkResult result = CommunityManager.linkMember(
				(ServerLevel) player.level(),
				community.id(),
				entity
		);
		player.sendSystemMessage(Component.translatable(linkMessage(result), community.name()));
		return InteractionResult.SUCCESS_SERVER;
	}

	private static InteractionResult unlink(
			ServerPlayer player,
			CommunityHolder holder,
			LivingEntity entity,
			Community community,
			Optional<java.util.UUID> linkedCommunity,
			ItemStack stack
	) {
		if (linkedCommunity.isEmpty()) {
			player.sendSystemMessage(Component.translatable("message.fantasyfolk.ledger.not_linked"));
			return InteractionResult.SUCCESS_SERVER;
		}
		if (!linkedCommunity.get().equals(community.id())) {
			player.sendSystemMessage(Component.translatable("message.fantasyfolk.ledger.unlink_mismatch"));
			return InteractionResult.SUCCESS_SERVER;
		}

		ConfirmationService.Request request = new ConfirmationService.Request(
				"community.unlink",
				entity.getUUID().toString(),
				community.id().toString(),
				stack
		);
		if (!ConfirmationService.checkOrArm(player.getUUID(), request, player.level().getServer().getTickCount())) {
			player.sendSystemMessage(Component.translatable("message.fantasyfolk.ledger.confirm_unlink"));
			return InteractionResult.SUCCESS_SERVER;
		}

		Optional<Community> current = CommunityManager.findCommunityForMember(player.level().getServer(), entity.getUUID());
		if (current.isEmpty() || !current.get().id().equals(community.id()) || holder.getCommunityId().isEmpty()) {
			player.sendSystemMessage(Component.translatable("message.fantasyfolk.ledger.unlink_mismatch"));
			return InteractionResult.SUCCESS_SERVER;
		}
		CommunityManager.unlinkMember(player.level().getServer(), entity.getUUID());
		player.sendSystemMessage(Component.translatable("message.fantasyfolk.ledger.unlinked", community.name()));
		return InteractionResult.SUCCESS_SERVER;
	}

	private static void select(ServerPlayer player, ItemStack stack, Community community) {
		CommunityLedgerData.select(stack, community);
		player.sendSystemMessage(Component.translatable(
				"message.fantasyfolk.ledger.selected",
				community.name(),
				Component.translatable(typeKey(community)),
				Component.translatable(stateKey(community))
		));
	}

	private static void sendCommunityInfo(ServerPlayer player, Community community) {
		player.sendSystemMessage(Component.translatable(
				"message.fantasyfolk.ledger.info",
				community.name(),
				Component.translatable(typeKey(community)),
				Component.translatable(stateKey(community)),
				community.dimension().identifier().toString(),
				community.center().getX(),
				community.center().getY(),
				community.center().getZ(),
				community.radius(),
				community.members().size()
		));
	}

	private static String typeKey(Community community) {
		return "community.fantasyfolk.type." + community.type().name().toLowerCase(java.util.Locale.ROOT);
	}

	private static String stateKey(Community community) {
		return "community.fantasyfolk.state." + community.state().name().toLowerCase(java.util.Locale.ROOT);
	}

	private static String registrationMessage(CommunityManager.RegistrationStatus status) {
		return switch (status) {
			case INVALID_VILLAGE -> "message.fantasyfolk.ledger.invalid_village";
			case OVERLAPS -> "message.fantasyfolk.ledger.overlaps";
			case REGISTERED, ALREADY_REGISTERED -> "message.fantasyfolk.ledger.community_not_found";
		};
	}

	private static String linkMessage(CommunityManager.LinkResult result) {
		return switch (result) {
			case LINKED -> "message.fantasyfolk.ledger.linked";
			case COMMUNITY_NOT_FOUND -> "message.fantasyfolk.ledger.missing_community";
			case DIMENSION_MISMATCH -> "message.fantasyfolk.ledger.wrong_dimension";
			case COMMUNITY_INACTIVE -> "message.fantasyfolk.ledger.inactive";
			case ALREADY_MEMBER, ALREADY_LINKED -> "message.fantasyfolk.ledger.already_linked";
			case OUTSIDE_TERRITORY -> "message.fantasyfolk.ledger.outside_territory";
			case UNSUPPORTED_ENTITY -> "message.fantasyfolk.ledger.unsupported_entity";
		};
	}
}
