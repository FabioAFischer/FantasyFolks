package com.fantasyfolk.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;

import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;

import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.commands.arguments.UuidArgument;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;

import com.fantasyfolk.community.Community;
import com.fantasyfolk.community.CommunityManager;
import com.fantasyfolk.community.CommunityPermissions;
import com.fantasyfolk.entity.Goblin;

import java.util.UUID;

/** Temporary operator commands used to inspect and edit community links. */
public final class CommunityCommands {
	private CommunityCommands() {
	}

	public static void initialize() {
		CommandRegistrationCallback.EVENT.register(CommunityCommands::register);
	}

	private static void register(
			CommandDispatcher<CommandSourceStack> dispatcher,
			CommandBuildContext registryAccess,
			net.minecraft.commands.Commands.CommandSelection environment
	) {
		dispatcher.register(Commands.literal("fantasyfolk")
				.then(Commands.literal("village")
						.requires(Commands.hasPermission(Commands.LEVEL_ADMINS))
						.then(Commands.literal("info")
								.executes(context -> infoNearby(context.getSource()))
								.then(Commands.argument("community", UuidArgument.uuid())
										.executes(CommunityCommands::infoById)))
						.then(Commands.literal("list")
								.executes(context -> list(context.getSource())))
						.then(Commands.literal("link")
								.then(Commands.argument("community", UuidArgument.uuid())
										.then(Commands.argument("entity", EntityArgument.entity())
												.executes(CommunityCommands::link))))
						.then(Commands.literal("unlink")
								.then(Commands.argument("entity", EntityArgument.entity())
										.executes(CommunityCommands::unlink)))));
	}

	private static int infoNearby(CommandSourceStack source) {
		BlockPos pos = BlockPos.containing(source.getPosition());
		return CommunityManager.findNearby(source.getServer(), source.getLevel().dimension(), pos)
				.map(community -> sendCommunity(source, community))
				.orElseGet(() -> {
					source.sendFailure(Component.literal("Nenhuma comunidade encontrada neste território."));
					return 0;
				});
	}

	private static int infoById(CommandContext<CommandSourceStack> context) {
		UUID id = UuidArgument.getUuid(context, "community");
		return CommunityManager.findById(context.getSource().getServer(), id)
				.map(community -> sendCommunity(context.getSource(), community))
				.orElseGet(() -> {
					context.getSource().sendFailure(Component.literal("Comunidade inexistente: " + id));
					return 0;
				});
	}

	private static int list(CommandSourceStack source) {
		CommunityManager.refreshLoadedStates(source.getServer());
		var communities = CommunityManager.storage(source.getServer()).all();
		if (communities.isEmpty()) {
			source.sendSuccess(() -> Component.literal("Nenhuma comunidade registrada."), false);
			return 0;
		}
		for (Community community : communities) {
			sendCommunity(source, community);
		}
		return communities.size();
	}

	private static int link(CommandContext<CommandSourceStack> context) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
		CommandSourceStack source = context.getSource();
		Entity entity = EntityArgument.getEntity(context, "entity");
		if (!(entity instanceof Goblin)) {
			source.sendFailure(Component.literal("A entidade precisa ser um habitante do Fantasy Folk, atualmente um Goblin."));
			return 0;
		}
		if (!CommunityPermissions.canPerformPlayerAction(source.getPlayer())) {
			source.sendFailure(Component.literal("Você não tem autorização para administrar esta comunidade."));
			return 0;
		}

		UUID communityId = UuidArgument.getUuid(context, "community");
		CommunityManager.LinkResult result = CommunityManager.linkMember(
				entity.level() instanceof ServerLevel serverLevel ? serverLevel : source.getLevel(),
				communityId,
				entity.getUUID()
		);
		if (result == CommunityManager.LinkResult.LINKED) {
			source.sendSuccess(() -> Component.literal("Entidade vinculada à comunidade " + communityId + "."), true);
			return 1;
		}

		source.sendFailure(Component.literal(linkFailureMessage(result)));
		return 0;
	}

	private static int unlink(CommandContext<CommandSourceStack> context) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
		CommandSourceStack source = context.getSource();
		Entity entity = EntityArgument.getEntity(context, "entity");
		if (!(entity instanceof Goblin)) {
			source.sendFailure(Component.literal("A entidade precisa ser um habitante do Fantasy Folk, atualmente um Goblin."));
			return 0;
		}

		if (CommunityManager.unlinkMember(source.getServer(), entity.getUUID()).isPresent()) {
			source.sendSuccess(() -> Component.literal("Vínculo comunitário removido de " + entity.getUUID() + "."), true);
			return 1;
		}
		source.sendFailure(Component.literal("A entidade não possui vínculo comunitário."));
		return 0;
	}

	private static int sendCommunity(CommandSourceStack source, Community community) {
		source.sendSuccess(() -> Component.literal(formatCommunity(community)), false);
		return 1;
	}

	private static String formatCommunity(Community community) {
		return String.format(
				"[%s] %s | id=%s | dimensão=%s | centro=%s,%s,%s | raio=%d | estado=%s | habitantes=%d",
				community.type(),
				community.name(),
				community.id(),
				community.dimension().identifier(),
				community.center().getX(),
				community.center().getY(),
				community.center().getZ(),
				community.radius(),
				community.state(),
				community.members().size()
		);
	}

	private static String linkFailureMessage(CommunityManager.LinkResult result) {
		return switch (result) {
			case COMMUNITY_NOT_FOUND -> "Comunidade inexistente.";
			case DIMENSION_MISMATCH -> "A entidade e a comunidade estão em dimensões diferentes.";
			case COMMUNITY_INACTIVE -> "A comunidade está INACTIVE e não aceita novos habitantes.";
			case ALREADY_MEMBER -> "A entidade já pertence a esta comunidade.";
			case ALREADY_LINKED -> "A entidade já está vinculada a outra comunidade.";
			case LINKED -> "Entidade vinculada.";
		};
	}
}
