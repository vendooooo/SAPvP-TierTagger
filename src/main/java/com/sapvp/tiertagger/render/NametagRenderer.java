package com.sapvp.tiertagger.render;

import com.sapvp.tiertagger.SAPVPTierTaggerClient;
import com.sapvp.tiertagger.config.SapvpTierTaggerConfig;
import com.sapvp.tiertagger.model.SapvpModeRanking;
import com.sapvp.tiertagger.model.SapvpPlayerProfile;
import com.sapvp.tiertagger.util.ModeVisuals;
import net.fabricmc.fabric.api.client.rendering.v1.world.WorldRenderContext;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.render.LightmapTextureManager;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.WorldRenderer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.Vec3d;
import org.joml.Matrix4f;

import java.util.Optional;

public final class NametagRenderer {
	private NametagRenderer() {
	}

	public static void render(WorldRenderContext context) {
		MinecraftClient client = MinecraftClient.getInstance();
		SapvpTierTaggerConfig config = SAPVPTierTaggerClient.config();
		if (!config.enabled || !config.renderNametags || !SAPVPTierTaggerClient.shouldRunInCurrentWorld(client)) {
			return;
		}
		if (client.world == null || client.player == null || context.gameRenderer() == null || context.matrices() == null) {
			return;
		}

		TextRenderer textRenderer = client.textRenderer;
		MatrixStack matrices = context.matrices();
		Vec3d cameraPos = context.gameRenderer().getCamera().getCameraPos();
		VertexConsumerProvider.Immediate vertices = client.getBufferBuilders().getEntityVertexConsumers();

		double maxDistanceSq = (double) config.maxRenderDistance * config.maxRenderDistance;
		for (AbstractClientPlayerEntity player : client.world.getPlayers()) {
			if (player == client.player || player.isInvisible()) {
				continue;
			}
			if (player.squaredDistanceTo(client.player) > maxDistanceSq) {
				continue;
			}

			SAPVPTierTaggerClient.profileService().requestProfile(player.getUuid(), player.getName().getString());
			Optional<SapvpPlayerProfile> profileResult = SAPVPTierTaggerClient.profileService().getCached(player.getUuid());
			if (profileResult.isEmpty()) {
				continue;
			}

			SapvpPlayerProfile profile = profileResult.get();
			renderTag(textRenderer, matrices, vertices, cameraPos, player, profile, config);
		}

		vertices.draw();
	}

	private static void renderTag(
		TextRenderer textRenderer,
		MatrixStack matrices,
		VertexConsumerProvider.Immediate vertices,
		Vec3d cameraPos,
		AbstractClientPlayerEntity player,
		SapvpPlayerProfile profile,
		SapvpTierTaggerConfig config
	) {
		double x = player.getX() - cameraPos.x;
		double y = player.getY() - cameraPos.y + player.getHeight() + config.nametagYOffset;
		double z = player.getZ() - cameraPos.z;

		SapvpModeRanking nametagRanking = profile.nametagRanking(config.nametagMode);
		String tagIcon = config.showModeIconInNametag
			? "[" + ModeVisuals.nametagToken(nametagRanking.modeId()) + "] "
			: config.showDiamondIcon ? "\u2666 " : "";
		String tier = config.nametagFormat.replace("%tier%", nametagRanking.tier().shortLabel());
		String tagText = tagIcon + tier;
		String separator = " | ";
		String name = profile.name();

		int tagTextWidth = textRenderer.getWidth(tagText);
		int separatorWidth = textRenderer.getWidth(separator);
		int nameWidth = textRenderer.getWidth(name);
		float totalWidth = config.hideVanillaNametags
			? tagTextWidth + separatorWidth + nameWidth
			: tagTextWidth;

		matrices.push();
		matrices.translate(x, y, z);
		matrices.multiply(MinecraftClient.getInstance().gameRenderer.getCamera().getRotation());
		float scale = 0.025F * config.nametagScale;
		matrices.scale(-scale, -scale, scale);

		Matrix4f positionMatrix = matrices.peek().getPositionMatrix();
		float drawX = config.hideVanillaNametags
			? -totalWidth / 2.0F
			: -(nameWidth / 2.0F) - tagTextWidth - 6.0F;
		int light = config.showThroughWalls
			? LightmapTextureManager.MAX_LIGHT_COORDINATE
			: WorldRenderer.getLightmapCoordinates(player.getEntityWorld(), player.getBlockPos());
		TextRenderer.TextLayerType layerType = config.showThroughWalls
			? TextRenderer.TextLayerType.SEE_THROUGH
			: TextRenderer.TextLayerType.NORMAL;

		int tagColor = config.showModeIconInNametag ? ModeVisuals.accentColor(nametagRanking.modeId()) : config.tierColor;
		int tagBackground = tintedBackground(tagColor, config.showThroughWalls ? 0x55 : 0x33);
		textRenderer.draw(tagText, drawX, 0, tagColor, true, positionMatrix, vertices, layerType, tagBackground, light);

		if (config.hideVanillaNametags) {
			drawX += tagTextWidth;
			textRenderer.draw(separator, drawX, 0, config.separatorColor, true, positionMatrix, vertices, layerType, tintedBackground(config.separatorColor, 0x22), light);
			drawX += separatorWidth;
			textRenderer.draw(name, drawX, 0, config.nameColor, true, positionMatrix, vertices, layerType, tintedBackground(config.nameColor, 0x1E), light);
		}

		matrices.pop();
	}

	private static int tintedBackground(int color, int alpha) {
		return (alpha << 24) | (color & 0x00FFFFFF);
	}
}

