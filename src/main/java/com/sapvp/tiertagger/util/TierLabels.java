package com.sapvp.tiertagger.util;

public final class TierLabels {
	private static final TierLabel UNRANKED = new TierLabel(0, 0, false, "UNR", "Unranked");

	private TierLabels() {
	}

	public static TierLabel fromRaw(int rawTier) {
		if (rawTier <= 0) {
			return UNRANKED;
		}

		int clamped = Math.max(1, Math.min(10, rawTier));
		int tierNumber = 5 - ((clamped - 1) / 2);
		boolean highTier = clamped % 2 == 0;
		String shortLabel = (highTier ? "HT" : "LT") + tierNumber;
		String fullLabel = (highTier ? "High" : "Low") + " Tier " + tierNumber;
		return new TierLabel(clamped, tierNumber, highTier, shortLabel, fullLabel);
	}

	public static TierLabel unranked() {
		return UNRANKED;
	}
}
