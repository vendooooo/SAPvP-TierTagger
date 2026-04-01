package com.sapvp.tiertagger.util;

public final class PointsTitleResolver {
	private PointsTitleResolver() {
	}

	public static String resolve(int points) {
		if (points >= 350) {
			return "Legend";
		}
		if (points >= 300) {
			return "Grandmaster";
		}
		if (points >= 250) {
			return "Combat Master";
		}
		if (points >= 200) {
			return "Elite";
		}
		if (points >= 150) {
			return "Vanguard";
		}
		if (points >= 100) {
			return "Duelist";
		}
		if (points >= 50) {
			return "Brawler";
		}
		if (points > 0) {
			return "Rookie";
		}
		return "Unranked";
	}
}
