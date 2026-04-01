package com.sapvp.tiertagger.integration;

import com.sapvp.tiertagger.gui.SapvpConfigScreen;
import com.terraformersmc.modmenu.api.ConfigScreenFactory;
import com.terraformersmc.modmenu.api.ModMenuApi;

public final class SapvpModMenuIntegration implements ModMenuApi {
	@Override
	public ConfigScreenFactory<?> getModConfigScreenFactory() {
		return SapvpConfigScreen::create;
	}
}
