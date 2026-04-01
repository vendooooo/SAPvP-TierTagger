package com.sapvp.tiertagger.render;

import com.sapvp.tiertagger.SAPVPTierTaggerClient;
import com.sapvp.tiertagger.config.SapvpTierTaggerConfig;
import com.sapvp.tiertagger.model.SapvpModeRanking;
import com.sapvp.tiertagger.model.SapvpPlayerProfile;
import com.sapvp.tiertagger.util.ModeVisuals;
import com.sapvp.tiertagger.util.NametagSideOption;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.Entity;
import net.minecraft.text.MutableText;
import net.minecraft.text.Style;
import net.minecraft.text.Text;

import java.util.Optional;
import java.util.UUID;

public final class NametagTextFormatter {
	private NametagTextFormatter() {
	}

	public static Text format(Entity player, Text original) {
		MinecraftClient client = MinecraftClient.getInstance();
		SapvpTierTaggerConfig config = SAPVPTierTaggerClient.config();
		if (client.player != null && player != client.player) {
			double maxDistanceSq = (double) config.maxRenderDistance * config.maxRenderDistance;
			if (player.squaredDistanceTo(client.player) > maxDistanceSq) {
				return original;
			}
		}

		return format(player.getUuid(), player.getName().getString(), original);
	}

	public static Text format(UUID uuid, String fallbackName, Text original) {
		MinecraftClient client = MinecraftClient.getInstance();
		SapvpTierTaggerConfig config = SAPVPTierTaggerClient.config();
		if (!config.enabled || !config.renderNametags || !SAPVPTierTaggerClient.shouldRunInCurrentWorld(client)) {
			return original;
		}

		SAPVPTierTaggerClient.profileService().requestProfile(uuid, fallbackName);
		Optional<SapvpPlayerProfile> cached = SAPVPTierTaggerClient.profileService().getCached(uuid);
		if (cached.isEmpty() && fallbackName != null && !fallbackName.isBlank()) {
			cached = SAPVPTierTaggerClient.profileService().getCached(fallbackName);
		}
		if (cached.isEmpty()) {
			return original;
		}

		SapvpModeRanking ranking = cached.get().nametagRanking(config.nametagMode);
		if (ranking.tier().raw() <= 0) {
			return original;
		}

		MutableText tag = Text.empty();
		String icon = "";
		int iconColor = config.modeIconColor;
		if (config.showModeIconInNametag) {
			icon = modeGlyph(ranking.modeId(), config.modeIconStyle);
			iconColor = config.modeIconColorFor(ranking.modeId());
		} else if (config.showDiamondIcon) {
			icon = "\u2726";
			iconColor = config.diamondColor;
		}

		if (!icon.isBlank()) {
			tag.append(iconText(icon, iconColor, config.modeIconStyle));
			tag.append(Text.literal(" "));
		}

		tag.append(Text.literal(config.nametagFormat.replace("%tier%", ranking.tier().shortLabel()))
			.setStyle(color(config.tierColorFor(ranking.tier()))));

		if (NametagSideOption.fromId(config.nametagSide) == NametagSideOption.RIGHT) {
			return Text.empty()
				.append(original.copy())
				.append(Text.literal(" | ").setStyle(color(config.separatorColor)))
				.append(tag);
		}

		return Text.empty()
			.append(tag)
			.append(Text.literal(" | ").setStyle(color(config.separatorColor)))
			.append(original.copy());
	}

	private static Style color(int color) {
		return Style.EMPTY.withColor(color & 0x00FFFFFF);
	}

	private static String modeGlyph(String rawMode, String iconStyle) {
		if ("EMOJI".equalsIgnoreCase(iconStyle)) {
			return ModeVisuals.emoji(rawMode);
		}
		return ModeVisuals.iconGlyph(rawMode);
	}

	private static Text iconText(String glyph, int color, String iconStyle) {
		if ("EMOJI".equalsIgnoreCase(iconStyle)) {
			return Text.literal(glyph).setStyle(color(color));
		}
		return Text.literal(glyph).setStyle(ModeVisuals.saNametagIconStyle(color));
	}
}
