package com.sapvp.tiertagger.config;

import com.sapvp.tiertagger.util.ModeVisuals;
import com.sapvp.tiertagger.util.ModeIconStyleOption;
import com.sapvp.tiertagger.util.NametagSideOption;
import com.sapvp.tiertagger.util.TierLabel;

public final class SapvpTierTaggerConfig {
	public static final int DEFAULT_TIER_COLOR = 0xFFE8BA3A;
	public static final int LEGACY_HT1_COLOR = 0xFFFFD54A;
	public static final int LEGACY_LT1_COLOR = 0xFFE0B848;
	public static final int LEGACY_HT2_COLOR = 0xFF8BE07B;
	public static final int LEGACY_LT2_COLOR = 0xFF66C25B;
	public static final int LEGACY_HT3_COLOR = 0xFF6FE6E1;
	public static final int LEGACY_LT3_COLOR = 0xFF41C7C2;
	public static final int LEGACY_HT4_COLOR = 0xFF78B8FF;
	public static final int LEGACY_LT4_COLOR = 0xFF4A8FE8;
	public static final int LEGACY_HT5_COLOR = 0xFFC7B8FF;
	public static final int LEGACY_LT5_COLOR = 0xFF8D85B8;
	public static final int DEFAULT_HT1_COLOR = 0xFFE8BA3A;
	public static final int DEFAULT_LT1_COLOR = 0xFFD5B355;
	public static final int DEFAULT_HT2_COLOR = 0xFFC4D3E7;
	public static final int DEFAULT_LT2_COLOR = 0xFFA0A7B2;
	public static final int DEFAULT_HT3_COLOR = 0xFFF89F5A;
	public static final int DEFAULT_LT3_COLOR = 0xFFC67B42;
	public static final int DEFAULT_HT4_COLOR = 0xFF81749A;
	public static final int DEFAULT_LT4_COLOR = 0xFF655B79;
	public static final int DEFAULT_HT5_COLOR = 0xFF8F82A8;
	public static final int DEFAULT_LT5_COLOR = 0xFF655B79;
	public static final int DEFAULT_SWORD_ICON_COLOR = 0xFF63D7D8;
	public static final int DEFAULT_CRYSTAL_ICON_COLOR = 0xFFF38BA8;
	public static final int DEFAULT_MACE_ICON_COLOR = 0xFFA9B1B9;
	public static final int DEFAULT_SMP_ICON_COLOR = 0xFF4F8B62;
	public static final int DEFAULT_AXE_ICON_COLOR = 0xFF8B5E3C;
	public static final int DEFAULT_NETHERITE_POT_ICON_COLOR = 0xFF655B79;

	public boolean enabled = true;
	public boolean renderNametags = true;
	public boolean hideVanillaNametags = false;
	public boolean onlyInMultiplayer = true;
	public boolean showThroughWalls = true;
	public boolean showDiamondIcon = true;
	public boolean showModeIconInNametag = true;
	public String modeIconStyle = "SA_ICONS";
	public int cacheCooldownSeconds = 20;
	public int nearbyRefreshTicks = 80;
	public int requestRadius = 64;
	public int maxTrackedPlayers = 32;
	public int maxRenderDistance = 96;
	public float nametagYOffset = 0.55F;
	public float nametagScale = 1.0F;
	public int diamondColor = 0xFFFFFFFF;
	public int modeIconColor = 0xFFFFFFFF;
	public int tierColor = DEFAULT_TIER_COLOR;
	public int separatorColor = 0xFFB7C0CC;
	public int nameColor = 0xFFFFFFFF;
	public String nametagFormat = "%tier%";
	public String nametagMode = "BEST";
	public String nametagSide = "LEFT";
	public int ht1Color = DEFAULT_HT1_COLOR;
	public int lt1Color = DEFAULT_LT1_COLOR;
	public int ht2Color = DEFAULT_HT2_COLOR;
	public int lt2Color = DEFAULT_LT2_COLOR;
	public int ht3Color = DEFAULT_HT3_COLOR;
	public int lt3Color = DEFAULT_LT3_COLOR;
	public int ht4Color = DEFAULT_HT4_COLOR;
	public int lt4Color = DEFAULT_LT4_COLOR;
	public int ht5Color = DEFAULT_HT5_COLOR;
	public int lt5Color = DEFAULT_LT5_COLOR;
	public int swordIconColor = DEFAULT_SWORD_ICON_COLOR;
	public int crystalIconColor = DEFAULT_CRYSTAL_ICON_COLOR;
	public int maceIconColor = DEFAULT_MACE_ICON_COLOR;
	public int smpIconColor = DEFAULT_SMP_ICON_COLOR;
	public int axeIconColor = DEFAULT_AXE_ICON_COLOR;
	public int netheritePotIconColor = DEFAULT_NETHERITE_POT_ICON_COLOR;

	public long cooldownMillis() {
		return Math.max(2, cacheCooldownSeconds) * 1000L;
	}

	public void normalize() {
		nametagMode = ModeVisuals.normalizeNametagMode(nametagMode);
		nametagSide = NametagSideOption.fromId(nametagSide).id();
		modeIconStyle = ModeIconStyleOption.fromId(modeIconStyle).id();
		normalizeColors();
	}

	public void normalizeColors() {
		diamondColor = ensureAlpha(diamondColor);
		modeIconColor = ensureAlpha(modeIconColor == 0 ? diamondColor : modeIconColor);
		tierColor = ensureAlpha(tierColor == 0 ? DEFAULT_TIER_COLOR : tierColor);
		separatorColor = ensureAlpha(separatorColor);
		nameColor = ensureAlpha(nameColor);
		ht1Color = migrateTierColor(ht1Color, LEGACY_HT1_COLOR, DEFAULT_HT1_COLOR);
		lt1Color = migrateTierColor(lt1Color, LEGACY_LT1_COLOR, DEFAULT_LT1_COLOR);
		ht2Color = migrateTierColor(ht2Color, LEGACY_HT2_COLOR, DEFAULT_HT2_COLOR);
		lt2Color = migrateTierColor(lt2Color, LEGACY_LT2_COLOR, DEFAULT_LT2_COLOR);
		ht3Color = migrateTierColor(ht3Color, LEGACY_HT3_COLOR, DEFAULT_HT3_COLOR);
		lt3Color = migrateTierColor(lt3Color, LEGACY_LT3_COLOR, DEFAULT_LT3_COLOR);
		ht4Color = migrateTierColor(ht4Color, LEGACY_HT4_COLOR, DEFAULT_HT4_COLOR);
		lt4Color = migrateTierColor(lt4Color, LEGACY_LT4_COLOR, DEFAULT_LT4_COLOR);
		ht5Color = migrateTierColor(ht5Color, LEGACY_HT5_COLOR, DEFAULT_HT5_COLOR);
		lt5Color = migrateTierColor(lt5Color, LEGACY_LT5_COLOR, DEFAULT_LT5_COLOR);
		swordIconColor = ensureAlpha(swordIconColor == 0 ? DEFAULT_SWORD_ICON_COLOR : swordIconColor);
		crystalIconColor = ensureAlpha(crystalIconColor == 0 ? DEFAULT_CRYSTAL_ICON_COLOR : crystalIconColor);
		maceIconColor = ensureAlpha(maceIconColor == 0 ? DEFAULT_MACE_ICON_COLOR : maceIconColor);
		smpIconColor = ensureAlpha(smpIconColor == 0 ? DEFAULT_SMP_ICON_COLOR : smpIconColor);
		axeIconColor = ensureAlpha(axeIconColor == 0 ? DEFAULT_AXE_ICON_COLOR : axeIconColor);
		netheritePotIconColor = ensureAlpha(netheritePotIconColor == 0 ? DEFAULT_NETHERITE_POT_ICON_COLOR : netheritePotIconColor);
	}

	public int tierColorFor(TierLabel tier) {
		if (tier == null || tier.raw() <= 0) {
			return tierColor;
		}

		return switch (tier.shortLabel()) {
			case "HT1" -> ht1Color;
			case "LT1" -> lt1Color;
			case "HT2" -> ht2Color;
			case "LT2" -> lt2Color;
			case "HT3" -> ht3Color;
			case "LT3" -> lt3Color;
			case "HT4" -> ht4Color;
			case "LT4" -> lt4Color;
			case "HT5" -> ht5Color;
			case "LT5" -> lt5Color;
			default -> tierColor;
		};
	}

	public int modeIconColorFor(String rawMode) {
		return switch (ModeVisuals.normalize(rawMode)) {
			case "SWORD" -> swordIconColor;
			case "CRYSTAL" -> crystalIconColor;
			case "MACE" -> maceIconColor;
			case "SMP" -> smpIconColor;
			case "AXE" -> axeIconColor;
			case "NETHERITE_POT" -> netheritePotIconColor;
			default -> modeIconColor;
		};
	}

	private static int ensureAlpha(int color) {
		return (color & 0xFF000000) == 0 ? color | 0xFF000000 : color;
	}

	private static int migrateTierColor(int currentColor, int legacyDefault, int newDefault) {
		int normalized = ensureAlpha(currentColor == 0 ? legacyDefault : currentColor);
		return normalized == ensureAlpha(legacyDefault) ? ensureAlpha(newDefault) : normalized;
	}
}
