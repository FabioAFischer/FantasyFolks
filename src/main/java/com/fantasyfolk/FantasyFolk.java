package com.fantasyfolk;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.entity.event.v1.ServerEntityCombatEvents;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;

import net.minecraft.resources.Identifier;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.gamerules.GameRules;
import net.minecraft.world.phys.AABB;

import com.fantasyfolk.registry.ModEntities;
import com.fantasyfolk.registry.ModBlocks;
import com.fantasyfolk.registry.ModItems;
import com.fantasyfolk.command.CommunityCommands;
import com.fantasyfolk.community.CommunityEvents;
import com.fantasyfolk.community.ConfirmationService;
import com.fantasyfolk.ai.hunter.HunterRuntime;
import com.fantasyfolk.entity.Goblin;
import com.fantasyfolk.inventory.WorkInventoryInteractionHandler;
import com.fantasyfolk.profession.ProfessionInteractionHandler;
import com.fantasyfolk.profession.ProfessionRegistry;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Entry point for the Fantasy Folk mod.
 *
 * <p>This first milestone only establishes the mod lifecycle. Content will be
 * added in later milestones.</p>
 */
public final class FantasyFolk implements ModInitializer {
	public static final String MOD_ID = "fantasyfolk";
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	@Override
	public void onInitialize() {
		ModBlocks.initialize();
		ModEntities.initialize();
		ModItems.initialize();
		ProfessionRegistry.initialize();
		CommunityEvents.initialize();
		ConfirmationService.initialize();
		CommunityCommands.initialize();
		ProfessionInteractionHandler.initialize();
		WorkInventoryInteractionHandler.initialize();
		ServerEntityCombatEvents.AFTER_KILLED_OTHER_ENTITY.register(
				(ServerLevel level, Entity attacker, LivingEntity killed, DamageSource source) -> {
					if (isGoblinDamage(source)) {
						LOGGER.debug(
								"Hunter diagnostics: kill callback attacker={} target={} targetAlive={} targetRemoved={} mobDrops={} source={} direct={}",
								attacker.getUUID(), killed.getUUID(), killed.isAlive(), killed.isRemoved(),
								level.getGameRules().get(GameRules.MOB_DROPS),
								describe(source.getEntity()), describe(source.getDirectEntity())
						);
					}
					Goblin goblin = hunterFrom(attacker, source);
					if (goblin != null) {
						goblin.onHunterKill(level, killed, source);
					}
				}
		);
		ServerLivingEntityEvents.AFTER_DAMAGE.register(
				(LivingEntity damaged, DamageSource source, float baseDamage, float damageTaken, boolean blocked) -> {
					if (isGoblinDamage(source)) {
						LOGGER.debug(
								"Hunter diagnostics: damage target={} base={} taken={} blocked={} source={} direct={}",
								damaged.getUUID(), baseDamage, damageTaken, blocked,
								describe(source.getEntity()), describe(source.getDirectEntity())
						);
					}
				}
		);
		ServerLivingEntityEvents.AFTER_DEATH.register((killed, source) -> {
			if (!(killed.level() instanceof ServerLevel level) || !isGoblinDamage(source)) {
				return;
			}
			long nearbyItems = level.getEntities(
					killed,
					new AABB(killed.blockPosition()).inflate(HunterRuntime.LOOT_SEARCH_RADIUS),
					entity -> entity instanceof ItemEntity
			).size();
			LOGGER.debug(
					"Hunter diagnostics: after-death target={} position={} mobDrops={} nearbyItemEntities={} source={} direct={}",
					killed.getUUID(), killed.blockPosition(), level.getGameRules().get(GameRules.MOB_DROPS),
					nearbyItems, describe(source.getEntity()), describe(source.getDirectEntity())
			);
		});
		LOGGER.info("Fantasy Folk initialized.");
	}

	private static boolean isGoblinDamage(DamageSource source) {
		return isGoblin(source.getEntity()) || isGoblin(source.getDirectEntity())
				|| source.getEntity() instanceof Projectile projectile && projectile.getOwner() instanceof Goblin
				|| source.getDirectEntity() instanceof Projectile projectile && projectile.getOwner() instanceof Goblin;
	}

	private static boolean isGoblin(Entity entity) {
		return entity instanceof Goblin;
	}

	private static Goblin hunterFrom(Entity attacker, DamageSource source) {
		if (attacker instanceof Goblin goblin) {
			return goblin;
		}
		for (Entity entity : new Entity[] {source.getEntity(), source.getDirectEntity()}) {
			if (entity instanceof Goblin goblin) {
				return goblin;
			}
			if (entity instanceof Projectile projectile && projectile.getOwner() instanceof Goblin goblin) {
				return goblin;
			}
		}
		return null;
	}

	private static String describe(Entity entity) {
		return entity == null ? "none" : entity.getType().toString() + "/" + entity.getUUID();
	}

	public static Identifier id(String path) {
		return Identifier.fromNamespaceAndPath(MOD_ID, path);
	}
}
