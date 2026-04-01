package com.sapvp.tiertagger.util;

import com.sapvp.tiertagger.SAPVPTierTaggerClient;
import com.sapvp.tiertagger.config.SapvpTierTaggerConfig;

public final class TierVisuals {
	private TierVisuals() {
	}

	public static int accentColor(TierLabel tier) {
		if (tier == null || tier.raw() <= 0) {
			return 0xFF6C7086;
		}

		try {
			return SAPVPTierTaggerClient.config().tierColorFor(tier);
		} catch (RuntimeException ignored) {
			return defaultAccentColor(tier);
		}
	}

	public static int backgroundColor(TierLabel tier) {
		if (tier == null || tier.raw() <= 0) {
			return 0x2D161E2A;
		}

		return switch (tier.raw()) {
			case 10 -> 0xFF6D5D2C;
			case 9 -> 0xFF584C25;
			case 8 -> 0xFF5E6979;
			case 7 -> 0xFF4A505A;
			case 6 -> 0xFF6B4B36;
			case 5 -> 0xFF593722;
			case 4 -> 0xFF303144;
			case 3 -> 0xFF2C2E40;
			case 2 -> 0xFF2B2C3D;
			case 1 -> 0xFF262A3A;
			default -> 0x2D161E2A;
		};
	}

	private static int defaultAccentColor(TierLabel tier) {
		return switch (tier.raw()) {
			case 10 -> SapvpTierTaggerConfig.DEFAULT_HT1_COLOR;
			case 9 -> SapvpTierTaggerConfig.DEFAULT_LT1_COLOR;
			case 8 -> SapvpTierTaggerConfig.DEFAULT_HT2_COLOR;
			case 7 -> SapvpTierTaggerConfig.DEFAULT_LT2_COLOR;
			case 6 -> SapvpTierTaggerConfig.DEFAULT_HT3_COLOR;
			case 5 -> SapvpTierTaggerConfig.DEFAULT_LT3_COLOR;
			case 4 -> SapvpTierTaggerConfig.DEFAULT_HT4_COLOR;
			case 3 -> SapvpTierTaggerConfig.DEFAULT_LT4_COLOR;
			case 2 -> SapvpTierTaggerConfig.DEFAULT_HT5_COLOR;
			case 1 -> SapvpTierTaggerConfig.DEFAULT_LT5_COLOR;
			default -> 0xFFA6ADC8;
		};
	}
}
