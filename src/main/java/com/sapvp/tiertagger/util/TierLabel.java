package com.sapvp.tiertagger.util;

public record TierLabel(int raw, int tierNumber, boolean highTier, String shortLabel, String fullLabel) {
}
