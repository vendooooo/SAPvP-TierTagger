package com.sapvp.tiertagger;

import com.sapvp.tiertagger.command.SapvpClientCommands;
import com.sapvp.tiertagger.compat.SapvpCompatibility;
import com.sapvp.tiertagger.config.ConfigManager;
import com.sapvp.tiertagger.config.SapvpTierTaggerConfig;
import com.sapvp.tiertagger.gui.SapvpConfigScreen;
import com.sapvp.tiertagger.gui.SapvpInfoScreen;
import com.sapvp.tiertagger.gui.SapvpMainScreen;
import com.sapvp.tiertagger.service.SapvpAvatarService;
import com.sapvp.tiertagger.service.PlayerLookupTarget;
import com.sapvp.tiertagger.service.SapvpProfileService;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.text.Text;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.lwjgl.glfw.GLFW;

import java.util.UUID;

public final class SAPVPTierTaggerClient implements ClientModInitializer {
	public static final String MOD_ID = "sapvptiertagger";
	private static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	private static ConfigManager configManager;
	private static SapvpProfileService profileService;
	private static SapvpAvatarService avatarService;
	private static KeyBinding openProfileKey;
	private static KeyBinding openConfigKey;
	private static Runnable pendingUiAction;
	private static int pendingUiDelayTicks;

	@Override
	public void onInitializeClient() {
		SapvpCompatibility.logRuntimeStatus(LOGGER);

		configManager = new ConfigManager();
		configManager.load();
		profileService = new SapvpProfileService(configManager);
		avatarService = new SapvpAvatarService();

		openProfileKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
			"key.sapvptiertagger.open_profile",
			GLFW.GLFW_KEY_G,
			KeyBinding.Category.MISC
		));
		openConfigKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
			"key.sapvptiertagger.open_config",
			GLFW.GLFW_KEY_O,
			KeyBinding.Category.MISC
		));

		ClientCommandRegistrationCallback.EVENT.register(SapvpClientCommands::register);

		ClientTickEvents.END_CLIENT_TICK.register(client -> {
			profileService.tick(client);

			while (openProfileKey.wasPressed()) {
				openLookTargetProfile(client, true);
			}

			while (openConfigKey.wasPressed()) {
				openConfigScreen();
			}

			if (pendingUiAction != null) {
				if (pendingUiDelayTicks > 0) {
					pendingUiDelayTicks--;
				} else {
					Runnable action = pendingUiAction;
					pendingUiAction = null;
					action.run();
				}
			}
		});
	}

	public static ConfigManager getConfigManager() {
		return configManager;
	}

	public static SapvpTierTaggerConfig config() {
		return configManager.get();
	}

	public static SapvpProfileService profileService() {
		return profileService;
	}

	public static SapvpAvatarService avatarService() {
		return avatarService;
	}

	public static void saveConfig() {
		configManager.save();
	}

	public static boolean shouldRunInCurrentWorld(MinecraftClient client) {
		if (client.world == null) {
			return false;
		}
		return !config().onlyInMultiplayer || client.getCurrentServerEntry() != null;
	}

	public static void openLookTargetProfile(MinecraftClient client, boolean notifyOnMiss) {
		AbstractClientPlayerEntity target = getLookedAtPlayer(client);
		if (target == null) {
			if (notifyOnMiss && client.player != null) {
				client.player.sendMessage(Text.literal("Olhe para um jogador ou use /sapvp info <nick>."), true);
			}
			return;
		}

		openProfile(PlayerLookupTarget.forUuid(target.getUuid(), target.getName().getString()));
	}

	public static void openProfile(PlayerLookupTarget target) {
		MinecraftClient client = MinecraftClient.getInstance();
		queueUiAction(() -> {
			if (target.uuid() != null) {
				profileService.requestProfile(target.uuid(), target.displayName());
			} else if (target.displayName() != null && !target.displayName().isBlank()) {
				profileService.requestProfileByName(target.displayName());
			}

			client.setScreen(new SapvpInfoScreen(client.currentScreen, target));
		});
	}

	public static void openProfile(UUID uuid, String name) {
		openProfile(PlayerLookupTarget.forUuid(uuid, name));
	}

	public static void openProfileByName(String name) {
		openProfile(PlayerLookupTarget.forName(name));
	}

	public static void openMainMenu() {
		MinecraftClient client = MinecraftClient.getInstance();
		queueUiAction(() -> client.setScreen(new SapvpMainScreen(client.currentScreen)));
	}

	public static void openConfigScreen() {
		MinecraftClient client = MinecraftClient.getInstance();
		queueUiAction(() -> client.setScreen(SapvpConfigScreen.create(client.currentScreen)));
	}

	@Nullable
	public static AbstractClientPlayerEntity getLookedAtPlayer(MinecraftClient client) {
		if (client.targetedEntity instanceof AbstractClientPlayerEntity player) {
			return player;
		}
		return null;
	}

	private static void queueUiAction(Runnable action) {
		pendingUiAction = action;
		pendingUiDelayTicks = 1;
	}
}
