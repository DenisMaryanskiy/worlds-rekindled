package com.dmaryanskiy.vanillareforged.client;

import com.dmaryanskiy.vanillareforged.VanillaReforged;
import com.mojang.blaze3d.vertex.PoseStack;

import net.fabricmc.fabric.api.client.rendering.v1.ArmorRenderer;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.state.HumanoidRenderState;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.Identifier;
import net.minecraft.util.LightCoordsUtil;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;

public class PaleMaskRenderer implements ArmorRenderer {
	public static final ModelLayerLocation LAYER = new ModelLayerLocation(VanillaReforged.id("pale_mask"), "main");
	private static final Identifier TEXTURE = VanillaReforged.id("textures/entity/pale_mask/pale_mask.png");
	// Only the eye is opaque here; drawn at full brightness so it glows like the Creaking's eyes.
	private static final Identifier EYES_TEXTURE = VanillaReforged.id("textures/entity/pale_mask/pale_mask_eyes.png");

	private final PaleMaskModel model;

	public PaleMaskRenderer(EntityRendererProvider.Context context) {
		this.model = new PaleMaskModel(context.bakeLayer(LAYER));
	}

	@Override
	public void render(PoseStack poseStack, SubmitNodeCollector submitNodeCollector, ItemStack stack,
			HumanoidRenderState state, EquipmentSlot slot, int light, HumanoidModel<HumanoidRenderState> contextModel) {
		RenderType base = stack.hasFoil() ? RenderTypes.armorCutoutNoCullGlint(TEXTURE) : RenderTypes.armorCutoutNoCull(TEXTURE);
		ArmorRenderer.submitTransformCopyingModel(contextModel, state, model, state, false, submitNodeCollector, poseStack,
				base, light, OverlayTexture.NO_OVERLAY, state.outlineColor);
		ArmorRenderer.submitTransformCopyingModel(contextModel, state, model, state, false, submitNodeCollector.order(1),
				poseStack, RenderTypes.armorCutoutNoCull(EYES_TEXTURE), LightCoordsUtil.FULL_BRIGHT, OverlayTexture.NO_OVERLAY,
				state.outlineColor);
	}
}
