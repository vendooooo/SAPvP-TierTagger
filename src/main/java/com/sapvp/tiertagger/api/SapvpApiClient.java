package com.sapvp.tiertagger.api;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.sapvp.tiertagger.model.SapvpModeRanking;
import com.sapvp.tiertagger.model.SapvpPlayerProfile;
import com.sapvp.tiertagger.util.CountryHelper;
import com.sapvp.tiertagger.util.ModeVisuals;
import com.sapvp.tiertagger.util.TierLabel;
import com.sapvp.tiertagger.util.TierLabels;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;

public final class SapvpApiClient {
	private static final String BASE_URL = "https://www.sapvp.com";
	private static final String MOJANG_BASE_URL = "https://api.mojang.com";
	private static final String USER_AGENT = "SAPVPTierTagger/1.0";
	private static final Gson GSON = new Gson();
	private static final Logger LOGGER = LoggerFactory.getLogger(SapvpApiClient.class);

	private final ExecutorService executor = Executors.newCachedThreadPool(new SapvpThreadFactory());
	private final HttpClient httpClient = HttpClient.newBuilder()
		.connectTimeout(Duration.ofSeconds(10))
		.followRedirects(HttpClient.Redirect.NORMAL)
		.build();

	public CompletableFuture<Map<UUID, SapvpPlayerProfile>> fetchProfilesBulk(Collection<UUID> uuids) {
		List<String> requested = uuids.stream().map(UUID::toString).distinct().toList();
		if (requested.isEmpty()) {
			return CompletableFuture.completedFuture(Map.of());
		}
		return CompletableFuture.supplyAsync(() -> fetchProfilesBulkSync(requested), executor);
	}

	public CompletableFuture<@Nullable SapvpPlayerProfile> fetchProfile(UUID uuid, @Nullable String fallbackName) {
		return CompletableFuture.supplyAsync(() -> {
			SapvpPlayerProfile profile = fetchProfileByUuidSync(uuid);
			if (profile == null && fallbackName != null && !fallbackName.isBlank()) {
				profile = fetchProfileByNameSync(fallbackName);
			}
			return profile;
		}, executor);
	}

	public CompletableFuture<@Nullable SapvpPlayerProfile> fetchProfileByName(String nickname) {
		return CompletableFuture.supplyAsync(() -> fetchProfileByNameSync(nickname), executor);
	}

	public CompletableFuture<DebugLookupResult> debugProfileByName(String nickname) {
		return CompletableFuture.supplyAsync(() -> debugProfileByNameSync(nickname), executor);
	}

	private @Nullable SapvpPlayerProfile fetchProfileByNameSync(String nickname) {
		SapvpPlayerProfile profile = firstProfile(sendJsonSapvp("/api/players/search/nickname?name=" + encode(nickname)));
		if (profile != null) {
			return profile;
		}

		MinecraftIdentity identity = fetchMinecraftIdentity(nickname);
		if (identity != null) {
			profile = fetchProfileByUuidSync(identity.uuid());
			if (profile != null) {
				return profile;
			}
		}

		return firstProfile(sendJsonSapvp("/api/players/search/nickname/" + encode(nickname)));
	}

	private @Nullable SapvpPlayerProfile fetchProfileByUuidSync(UUID uuid) {
		SapvpPlayerProfile profile = firstProfile(sendJsonSapvp("/api/players/search/uuid?id=" + encode(uuid.toString())));
		if (profile != null) {
			return profile;
		}

		Map<UUID, SapvpPlayerProfile> bulkProfiles = fetchProfilesBulkSync(List.of(uuid.toString()));
		if (bulkProfiles.containsKey(uuid)) {
			return bulkProfiles.get(uuid);
		}
		if (!bulkProfiles.isEmpty()) {
			return bulkProfiles.values().iterator().next();
		}

		profile = firstProfile(sendJsonSapvp("/api/players/search/uuid/" + uuid));
		if (profile != null) {
			return profile;
		}

		return firstProfile(sendJsonSapvp("/api/players/uuid/" + uuid));
	}

	private Map<UUID, SapvpPlayerProfile> fetchProfilesBulkSync(List<String> requested) {
		List<SapvpPlayerProfile> parsed = new ArrayList<>();
		JsonObject body = new JsonObject();
		JsonArray players = new JsonArray();
		requested.forEach(players::add);
		body.add("players", players);

		JsonElement officialShape = postJsonSapvp(
			"/api/players/search/list/uuid?page=0&size=" + requested.size(),
			GSON.toJson(body)
		);
		parsed.addAll(parsePlayers(officialShape));

		if (parsed.isEmpty()) {
			JsonElement fallbackShape = postJsonSapvp(
				"/api/players/search/list/uuid",
				GSON.toJson(requested)
			);
			parsed.addAll(parsePlayers(fallbackShape));
		}

		Map<UUID, SapvpPlayerProfile> profiles = new LinkedHashMap<>();
		for (SapvpPlayerProfile profile : parsed) {
			profiles.put(profile.uuid(), profile);
		}
		return profiles;
	}

	private DebugLookupResult debugProfileByNameSync(String nickname) {
		MinecraftIdentity identity = fetchMinecraftIdentity(nickname);

		List<String> directPaths = List.of(
			"/api/players/search/nickname?name=" + encode(nickname),
			"/api/players/search/nickname/" + encode(nickname)
		);
		for (String path : directPaths) {
			RawResponse response = sendRawSapvp(path);
			if (response == null) {
				continue;
			}

			SapvpPlayerProfile profile = parseFirstProfile(response.body());
			String error = profile == null ? "A API respondeu, mas o parser nao encontrou um jogador valido." : null;
			return new DebugLookupResult(path, response.statusCode(), response.body(), profile, error);
		}

		if (identity != null) {
			SapvpPlayerProfile profile = fetchProfileByUuidSync(identity.uuid());
			if (profile != null) {
				return new DebugLookupResult(
					"/api/players/search/list/uuid?page=0&size=1",
					200,
					"{\"fallback\":\"mojang_uuid\",\"uuid\":\"" + identity.uuid() + "\"}",
					profile,
					null
				);
			}
			return new DebugLookupResult(
				"/api/players/search/list/uuid?page=0&size=1",
				-1,
				"{\"fallback\":\"mojang_uuid\",\"uuid\":\"" + identity.uuid() + "\"}",
				null,
				"Nao encontrei resposta valida nem por nickname nem por Mojang UUID."
			);
		}

		return new DebugLookupResult(
			directPaths.getFirst(),
			-1,
			null,
			null,
			"Nenhuma resposta valida recebida da SAPVP e o nick nao foi resolvido pela Mojang."
		);
	}

	private @Nullable MinecraftIdentity fetchMinecraftIdentity(String nickname) {
		RawResponse response = executeRaw(
			requestBuilder(URI.create(MOJANG_BASE_URL + "/users/profiles/minecraft/" + encode(nickname)))
				.GET()
				.build()
		);
		if (response == null) {
			return null;
		}

		try {
			JsonObject object = JsonParser.parseString(response.body()).getAsJsonObject();
			String rawId = stringValue(object, "id");
			String resolvedName = stringValue(object, "name");
			if (rawId == null || rawId.length() != 32) {
				return null;
			}

			String dashed = rawId.replaceFirst(
				"([0-9a-fA-F]{8})([0-9a-fA-F]{4})([0-9a-fA-F]{4})([0-9a-fA-F]{4})([0-9a-fA-F]{12})",
				"$1-$2-$3-$4-$5"
			);
			return new MinecraftIdentity(UUID.fromString(dashed), resolvedName == null ? nickname : resolvedName);
		} catch (RuntimeException exception) {
			LOGGER.debug("Failed to parse Mojang profile for {}", nickname, exception);
			return null;
		}
	}

	private @Nullable SapvpPlayerProfile parseFirstProfile(String rawBody) {
		try {
			return firstProfile(JsonParser.parseString(rawBody));
		} catch (RuntimeException exception) {
			return null;
		}
	}

	private @Nullable SapvpPlayerProfile firstProfile(@Nullable JsonElement element) {
		List<SapvpPlayerProfile> parsed = parsePlayers(element);
		return parsed.isEmpty() ? null : parsed.getFirst();
	}

	private @Nullable JsonElement sendJsonSapvp(String path) {
		return execute(
			requestBuilder(URI.create(BASE_URL + path))
				.GET()
				.build()
		);
	}

	private @Nullable RawResponse sendRawSapvp(String path) {
		return executeRaw(
			requestBuilder(URI.create(BASE_URL + path))
				.GET()
				.build()
		);
	}

	private @Nullable JsonElement postJsonSapvp(String path, String body) {
		return execute(
			requestBuilder(URI.create(BASE_URL + path))
				.header("Content-Type", "application/json")
				.POST(HttpRequest.BodyPublishers.ofString(body, StandardCharsets.UTF_8))
				.build()
		);
	}

	private HttpRequest.Builder requestBuilder(URI uri) {
		return HttpRequest.newBuilder(uri)
			.timeout(Duration.ofSeconds(10))
			.header("Accept", "application/json")
			.header("User-Agent", USER_AGENT);
	}

	private @Nullable JsonElement execute(HttpRequest request) {
		RawResponse response = executeRaw(request);
		if (response == null || response.body() == null || response.body().isBlank()) {
			return null;
		}
		try {
			return JsonParser.parseString(response.body());
		} catch (RuntimeException exception) {
			LOGGER.warn("SAPVP response parse failed for {}", request.uri(), exception);
			return null;
		}
	}

	private @Nullable RawResponse executeRaw(HttpRequest request) {
		try {
			HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
			if (response.statusCode() < 200 || response.statusCode() >= 300) {
				LOGGER.debug("Request failed with status {} for {}", response.statusCode(), request.uri());
				return null;
			}
			String body = response.body();
			if (body == null || body.isBlank()) {
				LOGGER.debug("Request returned empty body for {}", request.uri());
				return null;
			}
			return new RawResponse(response.statusCode(), body);
		} catch (IOException exception) {
			LOGGER.warn("Request failed for {}", request.uri(), exception);
			return null;
		} catch (InterruptedException exception) {
			Thread.currentThread().interrupt();
			LOGGER.warn("Request interrupted for {}", request.uri(), exception);
			return null;
		}
	}

	private List<SapvpPlayerProfile> parsePlayers(@Nullable JsonElement root) {
		List<SapvpPlayerProfile> players = new ArrayList<>();
		for (JsonObject object : extractObjects(root)) {
			SapvpPlayerProfile profile = parsePlayer(object);
			if (profile != null) {
				players.add(profile);
			}
		}
		return players;
	}

	private List<JsonObject> extractObjects(@Nullable JsonElement root) {
		if (root == null || root.isJsonNull()) {
			return List.of();
		}

		List<JsonObject> objects = new ArrayList<>();
		if (root.isJsonArray()) {
			for (JsonElement element : root.getAsJsonArray()) {
				if (element.isJsonObject()) {
					objects.add(element.getAsJsonObject());
				}
			}
			return objects;
		}

		if (!root.isJsonObject()) {
			return List.of();
		}

		JsonObject object = root.getAsJsonObject();
		if (object.has("error")) {
			return List.of();
		}

		if (object.has("content") && object.get("content").isJsonArray()) {
			for (JsonElement element : object.getAsJsonArray("content")) {
				if (element.isJsonObject()) {
					objects.add(element.getAsJsonObject());
				}
			}
			return objects;
		}

		objects.add(object);
		return objects;
	}

	private @Nullable SapvpPlayerProfile parsePlayer(JsonObject object) {
		String uuidString = stringValue(object, "uuid");
		String name = fallbackString(object, "name", "nickname");
		if (uuidString == null || name == null) {
			return null;
		}

		UUID uuid;
		try {
			uuid = UUID.fromString(uuidString);
		} catch (IllegalArgumentException exception) {
			return null;
		}

		int points = intValue(object, "points", "score", "value");
		int globalRank = intValue(object, "overall", "globalRank", "rank");
		String country = CountryHelper.normalizeCountryCode(fallbackString(object, "country", "region"));

		List<SapvpModeRanking> rankings = new ArrayList<>();
		if (object.has("rankings") && object.get("rankings").isJsonArray()) {
			for (JsonElement element : object.getAsJsonArray("rankings")) {
				if (!element.isJsonObject()) {
					continue;
				}
				SapvpModeRanking ranking = parseRanking(element.getAsJsonObject());
				if (ranking != null) {
					rankings.add(ranking);
				}
			}
		}

		rankings.sort(Comparator
			.comparingInt((SapvpModeRanking ranking) -> ranking.tier().raw()).reversed()
			.thenComparingInt(SapvpModeRanking::points).reversed()
			.thenComparing(SapvpModeRanking::displayName));

		return new SapvpPlayerProfile(uuid, name, country, points, globalRank, List.copyOf(rankings), System.currentTimeMillis());
	}

	private @Nullable SapvpModeRanking parseRanking(JsonObject object) {
		String rawMode = fallbackString(object, "tierList", "mode", "modality", "name");
		if (rawMode == null) {
			return null;
		}

		int rawTier = intValue(object, "tier", "value");
		TierLabel label = TierLabels.fromRaw(rawTier);
		String normalizedMode = ModeVisuals.normalize(rawMode);
		return new SapvpModeRanking(
			normalizedMode,
			ModeVisuals.displayName(normalizedMode),
			ModeVisuals.emoji(normalizedMode),
			label,
			intValue(object, "points", "value"),
			booleanValue(object, "restricted")
		);
	}

	private static @Nullable String stringValue(JsonObject object, String key) {
		if (!object.has(key) || object.get(key).isJsonNull()) {
			return null;
		}
		JsonElement element = object.get(key);
		return element.isJsonPrimitive() ? element.getAsString() : null;
	}

	private static @Nullable String fallbackString(JsonObject object, String... keys) {
		for (String key : keys) {
			String value = stringValue(object, key);
			if (value != null && !value.isBlank()) {
				return value;
			}
		}
		return null;
	}

	private static int intValue(JsonObject object, String... keys) {
		for (String key : keys) {
			if (!object.has(key) || object.get(key).isJsonNull()) {
				continue;
			}
			try {
				return Math.round(object.get(key).getAsFloat());
			} catch (NumberFormatException | UnsupportedOperationException ignored) {
			}
		}
		return 0;
	}

	private static boolean booleanValue(JsonObject object, String key) {
		if (!object.has(key) || object.get(key).isJsonNull()) {
			return false;
		}
		try {
			return object.get(key).getAsBoolean();
		} catch (UnsupportedOperationException exception) {
			return false;
		}
	}

	private static String encode(String value) {
		return URLEncoder.encode(value, StandardCharsets.UTF_8);
	}

	private record RawResponse(int statusCode, String body) {
	}

	private record MinecraftIdentity(UUID uuid, String name) {
	}

	public record DebugLookupResult(
		String path,
		int statusCode,
		@Nullable String rawBody,
		@Nullable SapvpPlayerProfile profile,
		@Nullable String error
	) {
	}

	private static final class SapvpThreadFactory implements ThreadFactory {
		private final AtomicInteger counter = new AtomicInteger(1);

		@Override
		public Thread newThread(Runnable runnable) {
			Thread thread = new Thread(runnable, "sapvp-api-" + counter.getAndIncrement());
			thread.setDaemon(true);
			return thread;
		}
	}
}
