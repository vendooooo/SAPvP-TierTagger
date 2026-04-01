package com.sapvp.tiertagger.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;

public final class ConfigManager {
	private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

	private final Path configPath = FabricLoader.getInstance().getConfigDir().resolve("sapvptiertagger.json");
	private SapvpTierTaggerConfig config = new SapvpTierTaggerConfig();

	public SapvpTierTaggerConfig get() {
		return config;
	}

	public void load() {
		if (!Files.exists(configPath)) {
			config.normalize();
			save();
			return;
		}

		try (Reader reader = Files.newBufferedReader(configPath)) {
			SapvpTierTaggerConfig loaded = GSON.fromJson(reader, SapvpTierTaggerConfig.class);
			if (loaded != null) {
				config = loaded;
			}
			config.normalize();
		} catch (IOException exception) {
			exception.printStackTrace();
		}
	}

	public void save() {
		try {
			config.normalize();
			Files.createDirectories(configPath.getParent());
			try (Writer writer = Files.newBufferedWriter(configPath)) {
				GSON.toJson(config, writer);
			}
		} catch (IOException exception) {
			exception.printStackTrace();
		}
	}
}
