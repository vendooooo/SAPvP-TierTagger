package com.sapvp.tiertagger.service;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.sapvp.tiertagger.SAPVPTierTaggerClient;
import com.sapvp.tiertagger.model.SapvpPlayerProfile;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.texture.NativeImage;
import net.minecraft.client.texture.NativeImageBackedTexture;
import net.minecraft.util.Identifier;
import org.jetbrains.annotations.Nullable;

import java.io.InputStream;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public final class SapvpAvatarService {
	private static final String MOJANG_PROFILE_URL = "https://api.mojang.com/users/profiles/minecraft/";
	private static final String MOJANG_SESSION_URL = "https://sessionserver.mojang.com/session/minecraft/profile/";
	private static final HttpClient HTTP = HttpClient.newBuilder()
		.connectTimeout(Duration.ofSeconds(8))
		.followRedirects(HttpClient.Redirect.NORMAL)
		.build();

	private final ConcurrentMap<String, Identifier> skinCache = new ConcurrentHashMap<>();
	private final ConcurrentMap<String, Boolean> skinInFlight = new ConcurrentHashMap<>();
	private final ConcurrentMap<String, Identifier> headCache = new ConcurrentHashMap<>();
	private final ConcurrentMap<String, Boolean> headInFlight = new ConcurrentHashMap<>();

	public @Nullable Identifier getOrRequestSkinTexture(MinecraftClient client, PlayerLookupTarget target, @Nullable SapvpPlayerProfile profile) {
		UUID uuid = profile != null ? profile.uuid() : target.uuid();
		String name = profile != null ? profile.name() : target.displayName();
		String key = cacheKey(uuid, name);
		if (key.isBlank()) {
			return null;
		}

		Identifier cached = skinCache.get(key);
		if (cached != null) {
			return cached;
		}

		requestSkinTexture(client, key, uuid, name);
		return null;
	}

	public @Nullable Identifier getOrRequestHeadTexture(MinecraftClient client, PlayerLookupTarget target, @Nullable SapvpPlayerProfile profile) {
		UUID uuid = profile != null ? profile.uuid() : target.uuid();
		String name = profile != null ? profile.name() : target.displayName();
		String key = cacheKey(uuid, name);
		if (key.isBlank()) {
			return null;
		}

		Identifier cached = headCache.get(key);
		if (cached != null) {
			return cached;
		}

		requestHeadTexture(client, key, uuid, name);
		return null;
	}

	private void requestSkinTexture(MinecraftClient client, String key, @Nullable UUID uuid, @Nullable String name) {
		if (skinInFlight.putIfAbsent(key, Boolean.TRUE) != null) {
			return;
		}

		Thread.startVirtualThread(() -> {
			try {
				String skinUrl = resolveSkinUrl(uuid, name);
				if (skinUrl == null || skinUrl.isBlank()) {
					return;
				}

				HttpRequest request = HttpRequest.newBuilder(URI.create(skinUrl))
					.timeout(Duration.ofSeconds(12))
					.header("User-Agent", "SAPVPTierTagger/1.0.0")
					.GET()
					.build();
				HttpResponse<InputStream> response = HTTP.send(request, HttpResponse.BodyHandlers.ofInputStream());
				if (response.statusCode() < 200 || response.statusCode() >= 300) {
					return;
				}

				try (InputStream body = response.body(); NativeImage image = NativeImage.read(body)) {
					NativeImageBackedTexture texture = new NativeImageBackedTexture(() -> "SAPVP Skin " + key, image);
					Identifier textureId = Identifier.of(SAPVPTierTaggerClient.MOD_ID, "skins/" + key);
					client.execute(() -> {
						client.getTextureManager().destroyTexture(textureId);
						client.getTextureManager().registerTexture(textureId, texture);
						skinCache.put(key, textureId);
					});
				}
			} catch (Exception ignored) {
			} finally {
				skinInFlight.remove(key);
			}
		});
	}

	private void requestHeadTexture(MinecraftClient client, String key, @Nullable UUID uuid, @Nullable String name) {
		if (headInFlight.putIfAbsent(key, Boolean.TRUE) != null) {
			return;
		}

		Thread.startVirtualThread(() -> {
			try {
				for (String url : headUrls(uuid, name)) {
					HttpRequest request = HttpRequest.newBuilder(URI.create(url))
						.timeout(Duration.ofSeconds(12))
						.header("User-Agent", "SAPVPTierTagger/1.0.0")
						.GET()
						.build();

					HttpResponse<InputStream> response = HTTP.send(request, HttpResponse.BodyHandlers.ofInputStream());
					if (response.statusCode() < 200 || response.statusCode() >= 300) {
						continue;
					}

					try (InputStream body = response.body(); NativeImage image = NativeImage.read(body)) {
						NativeImageBackedTexture texture = new NativeImageBackedTexture(() -> "SAPVP Head " + key, image);
						Identifier textureId = Identifier.of(SAPVPTierTaggerClient.MOD_ID, "heads/" + key);
						client.execute(() -> {
							client.getTextureManager().destroyTexture(textureId);
							client.getTextureManager().registerTexture(textureId, texture);
							headCache.put(key, textureId);
						});
						return;
					}
				}
			} catch (Exception ignored) {
			} finally {
				headInFlight.remove(key);
			}
		});
	}

	private static List<String> headUrls(@Nullable UUID uuid, @Nullable String name) {
		String uuidKey = uuid == null ? "" : uuid.toString().replace("-", "");
		String cleanName = name == null ? "" : name.trim();
		List<String> urls = new ArrayList<>();
		if (!uuidKey.isBlank()) {
			urls.add("https://mc-heads.net/avatar/" + uuidKey + "/64");
			return List.of(
				"https://crafatar.com/avatars/" + uuidKey + "?size=64&overlay=true",
				"https://mc-heads.net/avatar/" + uuidKey + "/64"
			);
		}
		if (!cleanName.isBlank()) {
			urls.add("https://mc-heads.net/avatar/" + cleanName + "/64");
			urls.add("https://crafatar.com/avatars/" + cleanName + "?size=64&overlay=true");
			return urls;
		}
		return List.of();
	}

	private @Nullable String resolveSkinUrl(@Nullable UUID uuid, @Nullable String name) {
		if (uuid != null) {
			String byUuid = fetchSkinUrlByUuid(uuid);
			if (byUuid != null) {
				return byUuid;
			}
		}

		if (name != null && !name.isBlank()) {
			UUID mojangUuid = fetchUuidByName(name.trim());
			if (mojangUuid != null) {
				return fetchSkinUrlByUuid(mojangUuid);
			}
		}

		return null;
	}

	private @Nullable UUID fetchUuidByName(String name) {
		String encodedName = URLEncoder.encode(name, StandardCharsets.UTF_8);
		JsonObject object = fetchJsonObject(MOJANG_PROFILE_URL + encodedName);
		if (object == null || !object.has("id")) {
			return null;
		}

		String rawId = object.get("id").getAsString();
		if (rawId == null || rawId.length() != 32) {
			return null;
		}

		String dashed = rawId.replaceFirst(
			"([0-9a-fA-F]{8})([0-9a-fA-F]{4})([0-9a-fA-F]{4})([0-9a-fA-F]{4})([0-9a-fA-F]{12})",
			"$1-$2-$3-$4-$5"
		);
		try {
			return UUID.fromString(dashed);
		} catch (IllegalArgumentException exception) {
			return null;
		}
	}

	private @Nullable String fetchSkinUrlByUuid(UUID uuid) {
		String uuidKey = uuid.toString().replace("-", "");
		JsonObject object = fetchJsonObject(MOJANG_SESSION_URL + uuidKey);
		if (object == null || !object.has("properties") || !object.get("properties").isJsonArray()) {
			return null;
		}

		JsonArray properties = object.getAsJsonArray("properties");
		for (JsonElement propertyElement : properties) {
			if (!propertyElement.isJsonObject()) {
				continue;
			}

			JsonObject property = propertyElement.getAsJsonObject();
			if (!"textures".equals(property.has("name") ? property.get("name").getAsString() : "")) {
				continue;
			}
			if (!property.has("value")) {
				continue;
			}

			try {
				String decoded = new String(Base64.getDecoder().decode(property.get("value").getAsString()), StandardCharsets.UTF_8);
				JsonObject textureRoot = JsonParser.parseString(decoded).getAsJsonObject();
				JsonObject textures = textureRoot.getAsJsonObject("textures");
				if (textures == null || !textures.has("SKIN")) {
					continue;
				}
				JsonObject skin = textures.getAsJsonObject("SKIN");
				if (skin != null && skin.has("url")) {
					return skin.get("url").getAsString();
				}
			} catch (RuntimeException ignored) {
			}
		}
		return null;
	}

	private @Nullable JsonObject fetchJsonObject(String url) {
		try {
			HttpRequest request = HttpRequest.newBuilder(URI.create(url))
				.timeout(Duration.ofSeconds(10))
				.header("Accept", "application/json")
				.header("User-Agent", "SAPVPTierTagger/1.0.0")
				.GET()
				.build();

			HttpResponse<String> response = HTTP.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
			if (response.statusCode() < 200 || response.statusCode() >= 300 || response.body() == null || response.body().isBlank()) {
				return null;
			}

			JsonElement element = JsonParser.parseString(response.body());
			return element.isJsonObject() ? element.getAsJsonObject() : null;
		} catch (Exception ignored) {
			return null;
		}
	}

	private static String cacheKey(@Nullable UUID uuid, @Nullable String name) {
		if (uuid != null) {
			return uuid.toString().replace("-", "");
		}
		if (name != null && !name.isBlank()) {
			return name.trim().toLowerCase().replaceAll("[^a-z0-9_]", "");
		}
		return "";
	}
}
