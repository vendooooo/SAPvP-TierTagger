package com.sapvp.tiertagger.compat;

import net.fabricmc.loader.api.FabricLoader;
import org.slf4j.Logger;

import java.util.Set;

public final class SapvpCompatibility {
	private static final Set<String> TESTED_VERSIONS = Set.of("1.21.10", "1.21.11");

	private SapvpCompatibility() {
	}

	public static String runtimeMinecraftVersion() {
		return FabricLoader.getInstance()
			.getModContainer("minecraft")
			.map(container -> container.getMetadata().getVersion().getFriendlyString())
			.orElse("unknown");
	}

	public static boolean isTestedRuntime() {
		return TESTED_VERSIONS.contains(runtimeMinecraftVersion());
	}

	public static void logRuntimeStatus(Logger logger) {
		String runtime = runtimeMinecraftVersion();
		if (isTestedRuntime()) {
			logger.info("Running SAPVPTierTagger on tested Minecraft runtime {}", runtime);
			return;
		}

		logger.warn("Running SAPVPTierTagger on untested Minecraft runtime {}. Tested runtimes: {}", runtime, TESTED_VERSIONS);
	}
}
