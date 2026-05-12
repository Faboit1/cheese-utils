package com.faboit.cheeseutils.data;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public interface Storage extends AutoCloseable {
    CompletableFuture<Void> initialize();

    CompletableFuture<Map<String, String>> loadSettings(UUID uuid);

    CompletableFuture<Void> saveSettings(UUID uuid, Map<String, String> states);

    CompletableFuture<Void> saveHome(UUID uuid, String homeName, SerializedLocation location, String icon);

    CompletableFuture<Void> deleteHome(UUID uuid, String homeName);

    CompletableFuture<Map<String, HomeRecord>> loadHomes(UUID uuid);

    CompletableFuture<Void> saveWarp(String warpName, SerializedLocation location, String category, String permission, boolean hidden, boolean adminOnly);

    CompletableFuture<Void> deleteWarp(String warpName);

    CompletableFuture<Map<String, WarpRecord>> loadWarps();

    CompletableFuture<Void> saveSpawn(SerializedLocation location);

    CompletableFuture<SerializedLocation> loadSpawn();

    CompletableFuture<Long> getDailyLastClaim(UUID uuid);

    CompletableFuture<Void> setDailyLastClaim(UUID uuid, long epochMillis, int streak);

    CompletableFuture<DailyState> getDailyState(UUID uuid);

    record DailyState(long lastClaim, int streak) { }

    record HomeRecord(SerializedLocation location, String icon) { }

    record WarpRecord(SerializedLocation location, String category, String permission, boolean hidden, boolean adminOnly) { }
}
