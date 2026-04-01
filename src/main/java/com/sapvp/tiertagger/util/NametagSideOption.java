package com.sapvp.tiertagger.util;

public enum NametagSideOption {
	LEFT("LEFT", "Esquerda"),
	RIGHT("RIGHT", "Direita");

	private final String id;
	private final String label;

	NametagSideOption(String id, String label) {
		this.id = id;
		this.label = label;
	}

	public String id() {
		return id;
	}

	public String label() {
		return label;
	}

	public static NametagSideOption fromId(String value) {
		if (value == null || value.isBlank()) {
			return LEFT;
		}
		for (NametagSideOption option : values()) {
			if (option.id.equalsIgnoreCase(value)) {
				return option;
			}
		}
		return LEFT;
	}
}
