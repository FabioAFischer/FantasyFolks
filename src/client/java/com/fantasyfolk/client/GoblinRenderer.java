package com.fantasyfolk.client;

import com.mojang.blaze3d.vertex.PoseStack;

import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.HumanoidMobRenderer;
import net.minecraft.client.renderer.entity.state.PiglinRenderState;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.monster.piglin.PiglinArmPose;

import com.fantasyfolk.FantasyFolk;
import com.fantasyfolk.entity.Goblin;
import com.fantasyfolk.ai.hunter.HunterRuntime;
import com.fantasyfolk.profession.ProfessionEquipment;

/**
 * Goblin renderer using Goblin-owned geometry and adult Piglin animations.
 *
 * <p>The renderer owns the Piglin-compatible render state values needed by the
 * reused model; no Piglin entity AI or behavior is used.</p>
 */
public final class GoblinRenderer extends HumanoidMobRenderer<Goblin, PiglinRenderState, GoblinModel> {
	private static final Identifier GOBLIN_TEXTURE = FantasyFolk.id("textures/entity/goblin.png");

	public GoblinRenderer(EntityRendererProvider.Context context) {
		super(context, new GoblinModel(context.bakeLayer(GoblinModelLayers.GOBLIN)), 0.42F);
	}

	@Override
	public PiglinRenderState createRenderState() {
		return new PiglinRenderState();
	}

	@Override
	public void extractRenderState(Goblin goblin, PiglinRenderState state, float partialTick) {
		super.extractRenderState(goblin, state, partialTick);
		if (HunterRuntime.isHunterProfession(goblin.getProfessionId())
				&& goblin.isUsingItem()) {
			state.armPose = PiglinArmPose.CROSSBOW_CHARGE;
			state.maxCrossbowChageDuration = 20.0F;
		} else if (HunterRuntime.isHunterProfession(goblin.getProfessionId()) && goblin.isAggressive()) {
			state.armPose = PiglinArmPose.CROSSBOW_HOLD;
		} else {
			state.armPose = goblin.isAggressive() && ProfessionEquipment.isSword(goblin.getMainHandItem())
					? PiglinArmPose.ATTACKING_WITH_MELEE_WEAPON
					: PiglinArmPose.DEFAULT;
		}
		state.isBrute = false;
		state.isConverting = false;
	}

	@Override
	public Identifier getTextureLocation(PiglinRenderState state) {
		return GOBLIN_TEXTURE;
	}

	@Override
	protected void scale(PiglinRenderState state, PoseStack poseStack) {
		poseStack.scale(0.85F, 0.85F, 0.85F);
	}
}
