package com.sapvp.tiertagger.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.sapvp.tiertagger.SAPVPTierTaggerClient;
import com.sapvp.tiertagger.api.SapvpApiClient.DebugLookupResult;
import com.sapvp.tiertagger.model.SapvpPlayerProfile;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.minecraft.client.MinecraftClient;
import net.minecraft.command.CommandRegistryAccess;
import net.minecraft.text.Text;

import java.util.ArrayList;
import java.util.List;

public final class SapvpClientCommands {
	private SapvpClientCommands() {
	}

	public static void register(CommandDispatcher<FabricClientCommandSource> dispatcher, CommandRegistryAccess access) {
		var infoNode = ClientCommandManager.literal("info")
			.executes(context -> openLookTarget(context.getSource()))
			.then(ClientCommandManager.argument("player", StringArgumentType.greedyString())
				.executes(context -> openByName(
					context.getSource(),
					StringArgumentType.getString(context, "player")
				)));

		var configNode = ClientCommandManager.literal("config")
			.executes(context -> openConfig(context.getSource()));

		var nearbyNode = ClientCommandManager.literal("nearby")
			.executes(context -> openMainMenu(context.getSource()));

		var debugNode = ClientCommandManager.literal("debug")
			.then(ClientCommandManager.argument("player", StringArgumentType.greedyString())
				.executes(context -> debugByName(
					context.getSource(),
					StringArgumentType.getString(context, "player")
				)));

		dispatcher.register(ClientCommandManager.literal("sapvp")
			.executes(context -> openMainMenu(context.getSource()))
			.then(infoNode)
			.then(configNode)
			.then(nearbyNode)
			.then(debugNode));

		dispatcher.register(ClientCommandManager.literal("sapvptier")
			.executes(context -> openMainMenu(context.getSource()))
			.then(ClientCommandManager.literal("info")
				.executes(context -> openLookTarget(context.getSource()))
				.then(ClientCommandManager.argument("player", StringArgumentType.greedyString())
					.executes(context -> openByName(
						context.getSource(),
						StringArgumentType.getString(context, "player")
					))))
			.then(ClientCommandManager.literal("debug")
				.then(ClientCommandManager.argument("player", StringArgumentType.greedyString())
					.executes(context -> debugByName(context.getSource(), StringArgumentType.getString(context, "player")))))
			.then(ClientCommandManager.literal("config")
				.executes(context -> openConfig(context.getSource())))
			.then(ClientCommandManager.literal("player")
				.then(ClientCommandManager.argument("name", StringArgumentType.greedyString())
					.executes(context -> openByName(context.getSource(), StringArgumentType.getString(context, "name"))))));
	}

	private static int openMainMenu(FabricClientCommandSource source) {
		SAPVPTierTaggerClient.openMainMenu();
		source.sendFeedback(Text.literal("Abrindo menu SAPVP..."));
		return 1;
	}

	private static int openLookTarget(FabricClientCommandSource source) {
		SAPVPTierTaggerClient.openLookTargetProfile(MinecraftClient.getInstance(), true);
		return 1;
	}

	private static int openByName(FabricClientCommandSource source, String name) {
		if (name == null || name.isBlank()) {
			source.sendError(Text.literal("Use /sapvp info <nick>."));
			return 0;
		}
		SAPVPTierTaggerClient.openProfileByName(name.trim());
		source.sendFeedback(Text.literal("Consultando SAPVP para " + name.trim() + "..."));
		return 1;
	}

	private static int openConfig(FabricClientCommandSource source) {
		SAPVPTierTaggerClient.openConfigScreen();
		source.sendFeedback(Text.literal("Abrindo configuracao SAPVP..."));
		return 1;
	}

	private static int debugByName(FabricClientCommandSource source, String name) {
		String nickname = name == null ? "" : name.trim();
		if (nickname.isBlank()) {
			source.sendError(Text.literal("Use /sapvp debug <nick>."));
			return 0;
		}

		source.sendFeedback(Text.literal("Debug SAPVP para " + nickname + "..."));
		SAPVPTierTaggerClient.profileService().debugProfileByName(nickname)
			.thenAccept(result -> MinecraftClient.getInstance().execute(() -> sendDebugMessages(source, nickname, result)))
			.exceptionally(throwable -> {
				MinecraftClient.getInstance().execute(() ->
					source.sendError(Text.literal("Falha no debug SAPVP: " + throwable.getMessage()))
				);
				return null;
			});
		return 1;
	}

	private static void sendDebugMessages(FabricClientCommandSource source, String nickname, DebugLookupResult result) {
		source.sendFeedback(Text.literal("[SAPVP Debug] nick=" + nickname));
		source.sendFeedback(Text.literal("[SAPVP Debug] endpoint=" + result.path()));
		source.sendFeedback(Text.literal("[SAPVP Debug] status=" + result.statusCode()));

		SapvpPlayerProfile profile = result.profile();
		if (profile != null) {
			source.sendFeedback(Text.literal("[SAPVP Debug] parsed=" + profile.name()
				+ " | country=" + profile.countryCodeOrFallback()
				+ " | points=" + profile.points()
				+ " | global=#" + profile.globalRank()
				+ " | rankings=" + profile.rankings().size()));
		} else {
			source.sendFeedback(Text.literal("[SAPVP Debug] parsed=nenhum perfil valido"));
		}

		if (result.error() != null && !result.error().isBlank()) {
			source.sendError(Text.literal("[SAPVP Debug] erro=" + result.error()));
		}

		if (result.rawBody() != null && !result.rawBody().isBlank()) {
			for (String chunk : splitForChat("[SAPVP Debug] raw=" + result.rawBody(), 220)) {
				source.sendFeedback(Text.literal(chunk));
			}
		}
	}

	private static List<String> splitForChat(String text, int maxLength) {
		List<String> chunks = new ArrayList<>();
		for (int index = 0; index < text.length(); index += maxLength) {
			chunks.add(text.substring(index, Math.min(text.length(), index + maxLength)));
		}
		return chunks;
	}
}
