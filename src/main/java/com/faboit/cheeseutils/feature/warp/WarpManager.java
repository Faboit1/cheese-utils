package com.faboit.cheeseutils.feature.warp;

import com.faboit.cheeseutils.config.ConfigService;
import com.faboit.cheeseutils.data.SerializedLocation;
import com.faboit.cheeseutils.data.Storage;
import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

public final class WarpManager {
    private final ConfigService configService;
    private final Storage storage;
    private final Map<String, Storage.WarpRecord> warps = new HashMap<>();

    public WarpManager(ConfigService configService, Storage storage) {
        this.configService = configService;
        this.storage = storage;
    }

    public CompletableFuture<Void> load() {
        return storage.loadWarps().thenAccept(map -> {
            warps.clear();
            warps.putAll(map);
        });
    }

    public void setWarp(Player player, String name, String category) {
        Storage.WarpRecord record = new Storage.WarpRecord(SerializedLocation.from(player.getLocation()), category, "", false, false);
        storage.saveWarp(name, record.location(), record.category(), record.permission(), record.hidden(), record.adminOnly()).thenRun(() -> {
            warps.put(name.toLowerCase(), record);
            player.sendMessage(configService.message("warp.set", new ConfigService.Placeholder("name", name)));
        });
    }

    public void delWarp(Player player, String name) {
        storage.deleteWarp(name).thenRun(() -> {
            warps.remove(name.toLowerCase());
            player.sendMessage(configService.message("warp.deleted", new ConfigService.Placeholder("name", name)));
        });
    }

    public void warp(Player player, String name) {
        Storage.WarpRecord record = warps.get(name.toLowerCase());
        if (record == null) {
            player.sendMessage(configService.message("warp.not-found", new ConfigService.Placeholder("name", name)));
            return;
        }
        if (!record.permission().isBlank() && !player.hasPermission(record.permission()) && !player.hasPermission("cheeseutils.admin")) {
            player.sendMessage(configService.message("no-permission"));
            return;
        }
        player.teleportAsync(record.location().toBukkit());
        player.sendMessage(configService.message("warp.teleported", new ConfigService.Placeholder("name", name)));
    }

    public Map<String, Storage.WarpRecord> warps() {
        return Map.copyOf(warps);
    }
}
