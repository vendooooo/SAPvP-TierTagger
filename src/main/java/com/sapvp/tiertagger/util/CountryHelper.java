package com.sapvp.tiertagger.util;

import java.util.Locale;
import java.util.Map;

public final class CountryHelper {
	private static final Map<String, String> COUNTRY_ALIASES = Map.ofEntries(
		Map.entry("BRAZIL", "BR"),
		Map.entry("BRASIL", "BR"),
		Map.entry("ARGENTINA", "AR"),
		Map.entry("CHILE", "CL"),
		Map.entry("COLOMBIA", "CO"),
		Map.entry("PERU", "PE"),
		Map.entry("PARAGUAY", "PY"),
		Map.entry("URUGUAY", "UY"),
		Map.entry("VENEZUELA", "VE"),
		Map.entry("BOLIVIA", "BO"),
		Map.entry("ECUADOR", "EC"),
		Map.entry("UNITED_STATES", "US"),
		Map.entry("USA", "US"),
		Map.entry("CANADA", "CA"),
		Map.entry("MEXICO", "MX")
	);

	private CountryHelper() {
	}

	public static String normalizeCountryCode(String raw) {
		if (raw == null || raw.isBlank()) {
			return "BR";
		}

		String normalized = raw.trim().replace(' ', '_').replace('-', '_').toUpperCase(Locale.ROOT);
		if (normalized.length() == 2) {
			return normalized;
		}
		return COUNTRY_ALIASES.getOrDefault(normalized, "BR");
	}

	public static String flagEmoji(String code) {
		String normalized = normalizeCountryCode(code);
		if (normalized.length() != 2) {
			return "";
		}
		int first = Character.codePointAt(normalized, 0) - 'A' + 0x1F1E6;
		int second = Character.codePointAt(normalized, 1) - 'A' + 0x1F1E6;
		return new String(Character.toChars(first)) + new String(Character.toChars(second));
	}
}
