package com.fantasyfolk.client;

import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeDeformation;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.client.model.monster.piglin.AdultPiglinModel;

/**
 * Goblin-specific geometry that keeps the adult Piglin animation hierarchy.
 *
 * <p>The part names and parent-child relationships stay compatible with the
 * adult Piglin animation code, but every visible cube is defined here. This
 * gives the Goblin a compact silhouette without changing vanilla Piglins.</p>
 */
public final class GoblinModel extends AdultPiglinModel {
	private static final int TEXTURE_SIZE = 64;
	private static final float EAR_ANGLE_DEGREES = 40.0F;
	private static final float EAR_ANGLE_RADIANS = (float) Math.toRadians(EAR_ANGLE_DEGREES);
	private static final float NOSE_Z = -4.6F;
	private static final float TUSK_Z = -4.75F;

	public GoblinModel(ModelPart root) {
		super(root);
	}

	/**
	 * Creates the compact Goblin layer and its animation-compatible hierarchy.
	 *
	 * <p>The head keeps its v0.1 island. The compact torso and limbs use the
	 * remapped islands documented in {@code assets-dev/goblin/uv_mapping_v02.md}.</p>
	 */
	public static LayerDefinition createBodyLayer() {
		MeshDefinition mesh = new MeshDefinition();
		PartDefinition root = mesh.getRoot();

		root.addOrReplaceChild(
				"body",
				CubeListBuilder.create()
						.texOffs(16, 32)
						.addBox(-4.0F, 0.0F, -2.5F, 8.0F, 10.0F, 5.0F),
				PartPose.offset(0.0F, 6.0F, 0.0F)
		);

		PartDefinition head = root.addOrReplaceChild(
				"head",
				CubeListBuilder.create()
						.texOffs(0, 0)
						.addBox(-5.0F, -8.0F, -4.0F, 10.0F, 8.0F, 8.0F, CubeDeformation.NONE)
						.texOffs(31, 1)
						.addBox(-2.0F, -4.0F, NOSE_Z, 4.0F, 4.0F, 1.0F, CubeDeformation.NONE)
						.texOffs(2, 4)
						.addBox(2.0F, -2.0F, TUSK_Z, 1.0F, 2.0F, 1.0F, CubeDeformation.NONE)
						.texOffs(2, 0)
						.addBox(-3.0F, -2.0F, TUSK_Z, 1.0F, 2.0F, 1.0F, CubeDeformation.NONE),
				PartPose.offset(0.0F, 6.0F, 0.0F)
		);

		head.addOrReplaceChild(
				"left_ear",
				CubeListBuilder.create()
						.texOffs(51, 6)
						.addBox(0.0F, 0.0F, -2.0F, 1.0F, 5.0F, 4.0F, CubeDeformation.NONE),
				PartPose.offsetAndRotation(4.5F, -6.0F, 0.0F, 0.0F, 0.0F, -EAR_ANGLE_RADIANS)
		);
		head.addOrReplaceChild(
				"right_ear",
				CubeListBuilder.create()
						.texOffs(39, 6)
						.addBox(-1.0F, 0.0F, -2.0F, 1.0F, 5.0F, 4.0F, CubeDeformation.NONE),
				PartPose.offsetAndRotation(-4.5F, -6.0F, 0.0F, 0.0F, 0.0F, EAR_ANGLE_RADIANS)
		);
		head.addOrReplaceChild("hat", CubeListBuilder.create(), PartPose.ZERO);

		root.addOrReplaceChild(
				"right_arm",
				CubeListBuilder.create()
						.texOffs(40, 16)
						.addBox(-2.0F, -2.0F, -1.5F, 3.0F, 10.0F, 3.0F),
				PartPose.offset(-5.0F, 8.0F, 0.0F)
		);
		root.addOrReplaceChild(
				"left_arm",
				CubeListBuilder.create()
						.texOffs(32, 48)
						.addBox(-1.0F, -2.0F, -1.5F, 3.0F, 10.0F, 3.0F),
				PartPose.offset(5.0F, 8.0F, 0.0F)
		);
		root.addOrReplaceChild(
				"right_leg",
				CubeListBuilder.create()
						.texOffs(0, 16)
						.addBox(-2.0F, 0.0F, -2.0F, 4.0F, 8.0F, 4.0F),
				PartPose.offset(-1.9F, 16.0F, 0.0F)
		);
		root.addOrReplaceChild(
				"left_leg",
				CubeListBuilder.create()
						.texOffs(16, 48)
						.addBox(-2.0F, 0.0F, -2.0F, 4.0F, 8.0F, 4.0F),
				PartPose.offset(1.9F, 16.0F, 0.0F)
		);

		return LayerDefinition.create(mesh, TEXTURE_SIZE, TEXTURE_SIZE);
	}

	@Override
	protected float getDefaultEarAngleInDegrees() {
		return EAR_ANGLE_DEGREES;
	}
}
