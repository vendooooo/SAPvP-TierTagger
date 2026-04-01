package com.sapvp.tiertagger.service;

import com.sapvp.tiertagger.api.SapvpApiClient;
import com.sapvp.tiertagger.api.SapvpApiClient.DebugLookupResult;
import com.sapvp.tiertagger.config.ConfigManager;
import com.sapvp.tiertagger.model.SapvpPlayerProfile;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

public final class SapvpProfileService {
	private static final Logger LOGGER = LoggerFactory.getLogger(SapvpProfileService.class);

	private final ConfigManager configManager;
	private final SapvpApiClient apiClient = new SapvpApiClient();
	private final Map<UUID, CachedProfile> byUuid = new ConcurrentHashMap<>();
	private final Map<String, CachedProfile> byName = new ConcurrentHashMap<>();
	private final Map<String, LookupStatus> lookupStatuses = new ConcurrentHashMap<>();
	private final Map<String, CompletableFuture<?>> inFlight = new ConcurrentHashMap<>();

	private long clientTickCounter;
	private long nextNearbyRefreshTick;

	public SapvpProfileService(ConfigManager configManager) {
		this.configManager = configManager;
	}

	public void tick(MinecraftClient client) {
		var config = configManager.get();
		if (client.world == null || client.player == null || !config.enabled) {
			clientTickCounter = 0L;
			nextNearbyRefreshTick = 0L;
			return;
		}

		clientTickCounter++;
		if (clientTickCounter < nextNearbyRefreshTick) {
			return;
		}

		nextNearbyRefreshTick = clientTickCounter + Math.max(20, config.nearbyRefreshTicks);
		requestNearbyProfiles(client, client.world.getPlayers());
	}

	public void clear() {
		byUuid.clear();
		byName.clear();
		lookupStatuses.clear();
		inFlight.clear();
	}

	public Optional<SapvpPlayerProfile> getCached(UUID uuid) {
		return Optional.ofNullable(byUuid.get(uuid)).map(CachedProfile::profile);
	}

	public Optional<SapvpPlayerProfile> getCached(String playerName) {
		if (playerName == null || playerName.isBlank()) {
			return Optional.empty();
		}
		return Optional.ofNullable(byName.get(playerName.toLowerCase())).map(CachedProfile::profile);
	}

	public Optional<SapvpPlayerProfile> lookup(PlayerLookupTarget target) {
		if (target.uuid() != null) {
			Optional<SapvpPlayerProfile> byId = getCached(target.uuid());
			if (byId.isPresent()) {
				return byId;
			}
		}
		return target.displayName() == null ? Optional.empty() : getCached(target.displayName());
	}

	public ProfileLookupState lookupState(PlayerLookupTarget target) {
		Optional<SapvpPlayerProfile> profile = lookup(target);
		if (profile.isPresent()) {
			return ProfileLookupState.ready(profile);
		}

		LookupStatus status = getStatus(target);
		if (status == null) {
			return ProfileLookupState.idle(profile);
		}
		if (status.loading()) {
			return ProfileLookupState.loading(profile);
		}
		if (status.message() != null && !status.message().isBlank()) {
			return ProfileLookupState.failed(profile, status.message());
		}
		return ProfileLookupState.ready(profile);
	}

	public void requestProfile(UUID uuid, @Nullable String fallbackName) {
		CachedProfile cached = byUuid.get(uuid);
		if (isFresh(cached)) {
			markReady(cached.profile());
			return;
		}

		String key = "uuid:" + uuid;
		if (inFlight.containsKey(key)) {
			return;
		}
		markLoading(key, fallbackName);

		CompletableFuture<?> future = apiClient.fetchProfile(uuid, fallbackName)
			.thenAccept(profile -> {
				if (profile != null) {
					cache(profile);
					cacheAlias(uuid, fallbackName, profile);
					return;
				}
				markFailure(key, fallbackName, "Jogador nao encontrado na SAPVP.");
			})
			.whenComplete((unused, throwable) -> {
				inFlight.remove(key);
				if (throwable != null) {
					LOGGER.warn("Failed to request SAPVP profile for {}", uuid, throwable);
					markFailure(key, fallbackName, "Falha ao consultar a SAPVP.");
				}
			});
		inFlight.put(key, future);
	}

	public void requestProfileByName(String nickname) {
		if (nickname == null || nickname.isBlank()) {
			return;
		}

		String lowered = nickname.toLowerCase();
		CachedProfile cached = byName.get(lowered);
		if (isFresh(cached)) {
			markReady(cached.profile());
			return;
		}

		String key = "name:" + lowered;
		if (inFlight.containsKey(key)) {
			return;
		}
		markLoading(key, nickname);

		CompletableFuture<?> future = apiClient.fetchProfileByName(nickname)
			.thenAccept(profile -> {
				if (profile != null) {
					cache(profile);
					return;
				}
				markFailure(key, nickname, "Jogador nao encontrado na SAPVP.");
			})
			.whenComplete((unused, throwable) -> {
				inFlight.remove(key);
				if (throwable != null) {
					LOGGER.warn("Failed to request SAPVP profile for {}", nickname, throwable);
					markFailure(key, nickname, "Falha ao consultar a SAPVP.");
				}
			});
		inFlight.put(key, future);
	}

	public CompletableFuture<DebugLookupResult> debugProfileByName(String nickname) {
		return apiClient.debugProfileByName(nickname);
	}

	private void requestNearbyProfiles(MinecraftClient client, Collection<AbstractClientPlayerEntity> players) {
		var config = configManager.get();
		double requestRadiusSq = (double) config.requestRadius * config.requestRadius;
		int maxTrackedPlayers = Math.max(1, config.maxTrackedPlayers);

		List<AbstractClientPlayerEntity> nearby = players.stream()
			.filter(player -> player != client.player)
			.filter(player -> !player.isInvisible())
			.filter(player -> player.squaredDistanceTo(client.player) <= requestRadiusSq)
			.sorted(Comparator.comparingDouble(player -> player.squaredDistanceTo(client.player)))
			.limit(maxTrackedPlayers)
			.toList();

		List<UUID> bulkRequest = new ArrayList<>();
		for (AbstractClientPlayerEntity player : nearby) {
			if (!isFresh(byUuid.get(player.getUuid()))) {
				bulkRequest.add(player.getUuid());
			}
		}

		if (bulkRequest.isEmpty()) {
			return;
		}

		String key = "bulk:" + bulkRequest.hashCode();
		if (inFlight.containsKey(key)) {
			return;
		}

		CompletableFuture<?> future = apiClient.fetchProfilesBulk(bulkRequest)
			.thenAccept(map -> {
				for (SapvpPlayerProfile profile : map.values()) {
					cache(profile);
				}
			})
			.whenComplete((unused, throwable) -> {
				inFlight.remove(key);
				if (throwable != null) {
					LOGGER.debug("Bulk SAPVP lookup failed", throwable);
				}
			});
		inFlight.put(key, future);
	}

	private void cache(SapvpPlayerProfile profile) {
		CachedProfile cached = new CachedProfile(profile, System.currentTimeMillis());
		byUuid.put(profile.uuid(), cached);
		byName.put(profile.name().toLowerCase(), cached);
		lookupStatuses.put(uuidKey(profile.uuid()), new LookupStatus(false, null));
		lookupStatuses.put(nameKey(profile.name()), new LookupStatus(false, null));
	}

	private void cacheAlias(UUID requestedUuid, @Nullable String requestedName, SapvpPlayerProfile profile) {
		CachedProfile cached = new CachedProfile(profile, System.currentTimeMillis());
		byUuid.put(requestedUuid, cached);
		lookupStatuses.put(uuidKey(requestedUuid), new LookupStatus(false, null));

		if (requestedName != null && !requestedName.isBlank()) {
			byName.put(requestedName.toLowerCase(), cached);
			lookupStatuses.put(nameKey(requestedName), new LookupStatus(false, null));
		}
	}

	private boolean isFresh(@Nullable CachedProfile cached) {
		if (cached == null) {
			return false;
		}
		return System.currentTimeMillis() - cached.updatedAt() <= configManager.get().cooldownMillis();
	}

	private void markReady(SapvpPlayerProfile profile) {
		lookupStatuses.put(uuidKey(profile.uuid()), new LookupStatus(false, null));
		lookupStatuses.put(nameKey(profile.name()), new LookupStatus(false, null));
	}

	private void markLoading(String key, @Nullable String name) {
		lookupStatuses.put(key, new LookupStatus(true, null));
		if (name != null && !name.isBlank()) {
			lookupStatuses.put(nameKey(name), new LookupStatus(true, null));
		}
	}

	private void markFailure(String key, @Nullable String name, String message) {
		lookupStatuses.put(key, new LookupStatus(false, message));
		if (name != null && !name.isBlank()) {
			lookupStatuses.put(nameKey(name), new LookupStatus(false, message));
		}
	}

	private @Nullable LookupStatus getStatus(PlayerLookupTarget target) {
		if (target.uuid() != null) {
			LookupStatus status = lookupStatuses.get(uuidKey(target.uuid()));
			if (status != null) {
				return status;
			}
		}
		if (target.displayName() != null && !target.displayName().isBlank()) {
			return lookupStatuses.get(nameKey(target.displayName()));
		}
		return null;
	}

	private static String uuidKey(UUID uuid) {
		return "uuid:" + uuid;
	}

	private static String nameKey(String name) {
		return "name:" + name.toLowerCase();
	}

	private record CachedProfile(SapvpPlayerProfile profile, long updatedAt) {
	}

	private record LookupStatus(boolean loading, @Nullable String message) {
	}
}
