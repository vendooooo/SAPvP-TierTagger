package com.sapvp.tiertagger.service;

import org.jetbrains.annotations.Nullable;

import java.util.UUID;

public record PlayerLookupTarget(@Nullable UUID uuid, @Nullable String displayName) {
	public static PlayerLookupTarget forUuid(UUID uuid, String displayName) {
		return new PlayerLookupTarget(uuid, displayName);
	}

	public static PlayerLookupTarget forName(String displayName) {
		return new PlayerLookupTarget(null, displayName);
	}

	public String cacheKey() {
		return uuid != null ? uuid.toString() : displayName == null ? "" : displayName.toLowerCase();
	}
}
