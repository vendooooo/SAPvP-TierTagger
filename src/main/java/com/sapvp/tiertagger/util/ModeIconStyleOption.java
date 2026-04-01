package com.sapvp.tiertagger.util;

public enum ModeIconStyleOption {
	SA_ICONS("SA_ICONS", "SA Icons"),
	EMOJI("EMOJI", "Emoji");

	private final String id;
	private final String label;

	ModeIconStyleOption(String id, String label) {
		this.id = id;
		this.label = label;
	}

	public String id() {
		return id;
	}

	public String label() {
		return label;
	}

	public static ModeIconStyleOption fromId(String rawValue) {
		if (rawValue != null) {
			for (ModeIconStyleOption option : values()) {
				if (option.id.equalsIgnoreCase(rawValue)) {
					return option;
				}
			}
		}
		return SA_ICONS;
	}
}
