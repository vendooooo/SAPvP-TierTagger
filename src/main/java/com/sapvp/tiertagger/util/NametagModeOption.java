package com.sapvp.tiertagger.util;

public enum NametagModeOption {
	BEST("BEST"),
	NETHERITE_POT("NETHERITE_POT"),
	SWORD("SWORD"),
	AXE("AXE"),
	SMP("SMP"),
	MACE("MACE"),
	CRYSTAL("CRYSTAL");

	private final String modeId;

	NametagModeOption(String modeId) {
		this.modeId = modeId;
	}

	public String modeId() {
		return modeId;
	}

	public String displayLabel() {
		return ModeVisuals.configLabel(modeId);
	}

	public static NametagModeOption fromModeId(String rawMode) {
		String normalized = ModeVisuals.normalizeNametagMode(rawMode);
		for (NametagModeOption value : values()) {
			if (value.modeId.equals(normalized)) {
				return value;
			}
		}
		return BEST;
	}
}
