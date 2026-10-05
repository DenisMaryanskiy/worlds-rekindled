package com.dmaryanskiy.vanillareforged.client;

import com.dmaryanskiy.vanillareforged.VanillaReforged;
import com.dmaryanskiy.vanillareforged.petrification.Petrification;

import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElement;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.hud.VanillaHudElements;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.resources.Identifier;
import net.minecraft.util.ARGB;
import net.minecraft.world.entity.player.Player;

/** Stone creeping in from the screen edges as petrification fills, drawn like powder snow's frost outline. */
public class PetrificationOverlay implements HudElement {
	private static final Identifier ID = VanillaReforged.id("petrification");
	private static final Identifier TEXTURE = VanillaReforged.id("textures/misc/petrification_outline.png");

	public static void register() {
		HudElementRegistry.attachElementAfter(VanillaHudElements.MISC_OVERLAYS, ID, new PetrificationOverlay());
	}

	@Override
	public void extractRenderState(GuiGraphicsExtractor graphics, DeltaTracker deltaTracker) {
		Player player = Minecraft.getInstance().player;
		float progress = player == null ? 0.0F : Petrification.getProgress(player);
		if (progress > 0.0F) {
			graphics.blit(RenderPipelines.GUI_TEXTURED, TEXTURE, 0, 0, 0.0F, 0.0F, graphics.guiWidth(), graphics.guiHeight(),
					graphics.guiWidth(), graphics.guiHeight(), ARGB.white(progress));
		}
	}
}
