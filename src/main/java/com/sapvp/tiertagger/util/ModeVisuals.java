package com.sapvp.tiertagger.util;

import net.minecraft.text.Style;
import net.minecraft.text.StyleSpriteSource;
import net.minecraft.util.Identifier;

import java.util.List;
import java.util.Locale;

public final class ModeVisuals {
	private static final Identifier SA_ICON_FONT_ID = Identifier.of("sapvptiertagger", "default");
	private static final StyleSpriteSource.Font SA_ICON_FONT = new StyleSpriteSource.Font(SA_ICON_FONT_ID);
	private static final Identifier SA_NAMETAG_ICON_FONT_ID = Identifier.of("sapvptiertagger", "nametag_icons");
	private static final StyleSpriteSource.Font SA_NAMETAG_ICON_FONT = new StyleSpriteSource.Font(SA_NAMETAG_ICON_FONT_ID);
	private static final Identifier NETHERITE_POT_ICON = Identifier.of("sapvptiertagger", "font/netherite_pot.png");
	private static final Identifier SWORD_ICON = Identifier.of("sapvptiertagger", "font/sword.png");
	private static final Identifier AXE_ICON = Identifier.of("sapvptiertagger", "font/axe.png");
	private static final Identifier SMP_ICON = Identifier.of("sapvptiertagger", "font/smp.png");
	private static final Identifier MACE_ICON = Identifier.of("sapvptiertagger", "font/mace.png");
	private static final Identifier CRYSTAL_ICON = Identifier.of("sapvptiertagger", "font/crystal.png");
	private static final List<String> NAMETAG_MODES = List.of(
		"BEST",
		"NETHERITE_POT",
		"SWORD",
		"AXE",
		"SMP",
		"MACE",
		"CRYSTAL"
	);
	private static final List<String> SUPPORTED_TIERLIST_MODES = List.of(
		"NETHERITE_POT",
		"SWORD",
		"AXE",
		"SMP",
		"MACE",
		"CRYSTAL"
	);

	private ModeVisuals() {
	}

	public static String displayName(String rawMode) {
		String mode = normalize(rawMode);
		return switch (mode) {
			case "BEST" -> "Best";
			case "NETHERITE_POT" -> "NetheritePot";
			case "SWORD" -> "Sword";
			case "AXE" -> "Axe";
			case "SMP" -> "SMP";
			case "MACE" -> "Mace";
			case "CRYSTAL" -> "Crystal";
			default -> rawMode == null || rawMode.isBlank() ? "Unknown" : rawMode;
		};
	}

	public static String emoji(String rawMode) {
		String mode = normalize(rawMode);
		return switch (mode) {
			case "BEST" -> "\u2726";
			case "NETHERITE_POT" -> "\u2697";
			case "SWORD" -> "\uD83D\uDDE1";
			case "AXE" -> "\uD83E\uDE93";
			case "SMP" -> "\uD83D\uDEE1";
			case "MACE" -> "\uD83D\uDD28";
			case "CRYSTAL" -> "\u2726";
			default -> "\uD83C\uDFAF";
		};
	}

	public static int accentColor(String rawMode) {
		String mode = normalize(rawMode);
		return switch (mode) {
			case "BEST" -> 0xFFE8BA3A;
			case "NETHERITE_POT" -> 0xFF655B79;
			case "SWORD" -> 0xFF63D7D8;
			case "AXE" -> 0xFF8B5E3C;
			case "SMP" -> 0xFF4F8B62;
			case "MACE" -> 0xFFA9B1B9;
			case "CRYSTAL" -> 0xFFF38BA8;
			default -> 0xFFA6ADC8;
		};
	}

	public static String nametagToken(String rawMode) {
		String mode = normalize(rawMode);
		return switch (mode) {
			case "NETHERITE_POT" -> "NP";
			case "SWORD" -> "SW";
			case "AXE" -> "AX";
			case "SMP" -> "SM";
			case "MACE" -> "MC";
			case "CRYSTAL" -> "CR";
			default -> "TT";
		};
	}

	public static String normalize(String rawMode) {
		if (rawMode == null) {
			return "";
		}
		String normalized = rawMode.trim().replace(' ', '_').replace('-', '_').toUpperCase(Locale.ROOT);
		return switch (normalized) {
			case "NETHERITE_OP", "NETHERITE", "NETH_OP", "NETH_POT" -> "NETHERITE_POT";
			default -> normalized;
		};
	}

	public static List<String> nametagModes() {
		return NAMETAG_MODES;
	}

	public static List<String> supportedTierlistModes() {
		return SUPPORTED_TIERLIST_MODES;
	}

	public static String normalizeNametagMode(String rawMode) {
		String normalized = normalize(rawMode);
		return NAMETAG_MODES.contains(normalized) ? normalized : "BEST";
	}

	public static String nextNametagMode(String current) {
		String normalized = normalizeNametagMode(current);
		int index = NAMETAG_MODES.indexOf(normalized);
		return NAMETAG_MODES.get((index + 1) % NAMETAG_MODES.size());
	}

	public static String configLabel(String rawMode) {
		String normalized = normalizeNametagMode(rawMode);
		return "BEST".equals(normalized) ? "Melhor disponivel" : displayName(normalized);
	}

	public static boolean isSupportedTierlistMode(String rawMode) {
		return SUPPORTED_TIERLIST_MODES.contains(normalize(rawMode));
	}

	public static String iconGlyph(String rawMode) {
		String normalized = normalize(rawMode);
		return switch (normalized) {
			case "NETHERITE_POT" -> "\uE000";
			case "SWORD" -> "\uE001";
			case "AXE" -> "\uE002";
			case "SMP" -> "\uE003";
			case "MACE" -> "\uE004";
			case "CRYSTAL" -> "\uE005";
			default -> "\u2726";
		};
	}

	public static Identifier iconTexture(String rawMode) {
		String normalized = normalize(rawMode);
		return switch (normalized) {
			case "NETHERITE_POT" -> NETHERITE_POT_ICON;
			case "SWORD" -> SWORD_ICON;
			case "AXE" -> AXE_ICON;
			case "SMP" -> SMP_ICON;
			case "MACE" -> MACE_ICON;
			case "CRYSTAL" -> CRYSTAL_ICON;
			default -> NETHERITE_POT_ICON;
		};
	}

	public static Style saIconStyle(int color) {
		return Style.EMPTY.withColor(color & 0x00FFFFFF).withFont(SA_ICON_FONT);
	}

	public static Style saNametagIconStyle(int color) {
		return Style.EMPTY.withColor(color & 0x00FFFFFF).withFont(SA_NAMETAG_ICON_FONT);
	}
}
