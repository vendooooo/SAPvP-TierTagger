package com.sapvp.tiertagger.model;

import com.sapvp.tiertagger.util.CountryHelper;
import com.sapvp.tiertagger.util.ModeVisuals;
import com.sapvp.tiertagger.util.PointsTitleResolver;
import com.sapvp.tiertagger.util.TierLabel;
import com.sapvp.tiertagger.util.TierLabels;
import org.jetbrains.annotations.Nullable;

import java.util.Comparator;
import java.util.List;
import java.util.UUID;

public record SapvpPlayerProfile(
	UUID uuid,
	String name,
	String countryCode,
	int points,
	int globalRank,
	List<SapvpModeRanking> rankings,
	long fetchedAt
) {
	public @Nullable SapvpModeRanking bestRanking() {
		return rankings.stream()
			.max(Comparator.comparingInt((SapvpModeRanking value) -> value.tier().raw())
				.thenComparingInt(SapvpModeRanking::points))
			.orElse(null);
	}

	public TierLabel bestTier() {
		return rankings.stream()
			.max(Comparator.comparingInt((SapvpModeRanking value) -> value.tier().raw())
				.thenComparingInt(SapvpModeRanking::points))
			.map(SapvpModeRanking::tier)
			.orElse(TierLabels.unranked());
	}

	public @Nullable SapvpModeRanking rankingForMode(String rawMode) {
		String normalized = ModeVisuals.normalize(rawMode);
		return rankings.stream()
			.filter(ranking -> ranking.modeId().equals(normalized))
			.findFirst()
			.orElse(null);
	}

	public SapvpModeRanking nametagRanking(String configuredMode) {
		String normalized = ModeVisuals.normalizeNametagMode(configuredMode);
		if ("BEST".equals(normalized)) {
			SapvpModeRanking best = bestRanking();
			if (best != null) {
				return best;
			}
			return syntheticRanking("BEST");
		}

		SapvpModeRanking selected = rankingForMode(normalized);
		return selected != null ? selected : syntheticRanking(normalized);
	}

	public String bestTierShort() {
		return bestTier().shortLabel();
	}

	public String bestTierFull() {
		return bestTier().fullLabel();
	}

	public String pointsTitle() {
		return PointsTitleResolver.resolve(points);
	}

	public String countryCodeOrFallback() {
		return CountryHelper.normalizeCountryCode(countryCode);
	}

	public String countryFlag() {
		return CountryHelper.flagEmoji(countryCodeOrFallback());
	}

	private static SapvpModeRanking syntheticRanking(String mode) {
		String normalized = ModeVisuals.normalize(mode);
		return new SapvpModeRanking(
			normalized,
			ModeVisuals.displayName(normalized),
			ModeVisuals.emoji(normalized),
			TierLabels.unranked(),
			0,
			false
		);
	}
}
