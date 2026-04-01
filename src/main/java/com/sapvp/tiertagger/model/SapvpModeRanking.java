package com.sapvp.tiertagger.model;

import com.sapvp.tiertagger.util.TierLabel;

public record SapvpModeRanking(
	String modeId,
	String displayName,
	String emoji,
	TierLabel tier,
	int points,
	boolean restricted
) {
}
