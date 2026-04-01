package com.sapvp.tiertagger.service;

import com.sapvp.tiertagger.model.SapvpPlayerProfile;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;

public record ProfileLookupState(
	Optional<SapvpPlayerProfile> profile,
	boolean loading,
	boolean attempted,
	@Nullable String message
) {
	public static ProfileLookupState idle(Optional<SapvpPlayerProfile> profile) {
		return new ProfileLookupState(profile, false, false, null);
	}

	public static ProfileLookupState loading(Optional<SapvpPlayerProfile> profile) {
		return new ProfileLookupState(profile, true, true, null);
	}

	public static ProfileLookupState ready(Optional<SapvpPlayerProfile> profile) {
		return new ProfileLookupState(profile, false, true, null);
	}

	public static ProfileLookupState failed(Optional<SapvpPlayerProfile> profile, String message) {
		return new ProfileLookupState(profile, false, true, message);
	}
}
