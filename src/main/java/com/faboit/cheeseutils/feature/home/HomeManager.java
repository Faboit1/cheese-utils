package com.faboit.cheeseutils.feature.home;

import com.faboit.cheeseutils.config.ConfigService;
import com.faboit.cheeseutils.data.SerializedLocation;
import com.faboit.cheeseutils.data.Storage;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public final class HomeManager {
    private final JavaPlugin plugin;
    private final ConfigService configService;
    private final Storage storage;
    private final Map<UUID, Map<String, Storage.HomeRecord>> homes = new HashMap<>();

    public HomeManager(JavaPlugin plugin, ConfigService configService, Storage storage) {
        this.plugin = plugin;
        this.configService = configService;
        this.storage = storage;
    }

    public CompletableFuture<Void> load(UUID uuid) {
        return storage.loadHomes(uuid).thenAccept(data -> homes.put(uuid, data));
    }

    public int maxHomes(Player player) {
        if (player.hasPermission("cheeseutils.admin")) {
            return Integer.MAX_VALUE;
        }
        int fallback = configService.config().getInt("homes.default-max", 3);
        int best = 0;
        for (int i = 1; i <= 100; i++) {
            if (player.hasPermission("cheeseutils.homes.max." + i)) {
                best = Math.max(best, i);
            }
        }
        return best == 0 ? fallback : best;
    }

    public void setHome(Player player, String homeName) {
        Map<String, Storage.HomeRecord> playerHomes = homes.computeIfAbsent(player.getUniqueId(), ignored -> new HashMap<>());
        if (!playerHomes.containsKey(homeName.toLowerCase()) && playerHomes.size() >= maxHomes(player)) {
            player.sendMessage(configService.message("homes.limit", new ConfigService.Placeholder("max", Integer.toString(maxHomes(player)))));
            return;
        }
        SerializedLocation location = SerializedLocation.from(player.getLocation());
        storage.saveHome(player.getUniqueId(), homeName, location, "OAK_DOOR").thenRun(() -> {
            playerHomes.put(homeName.toLowerCase(), new Storage.HomeRecord(location, "OAK_DOOR"));
            player.sendMessage(configService.message("homes.set", new ConfigService.Placeholder("name", homeName)));
        });
    }

    public void delHome(Player player, String homeName) {
        storage.deleteHome(player.getUniqueId(), homeName).thenRun(() -> {
            homes.computeIfAbsent(player.getUniqueId(), ignored -> new HashMap<>()).remove(homeName.toLowerCase());
            player.sendMessage(configService.message("homes.deleted", new ConfigService.Placeholder("name", homeName)));
        });
    }

    public void home(Player player, String homeName) {
        Storage.HomeRecord record = homes.computeIfAbsent(player.getUniqueId(), ignored -> new HashMap<>()).get(homeName.toLowerCase());
        if (record == null) {
            player.sendMessage(configService.message("homes.not-found", new ConfigService.Placeholder("name", homeName)));
            return;
        }
        int warmup = configService.config().getInt("homes.warmup-seconds", 3);
        Location base = player.getLocation().clone();
        player.sendMessage(configService.message("homes.warmup", new ConfigService.Placeholder("seconds", Integer.toString(warmup))));
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            if (configService.config().getBoolean("homes.cancel-on-move", true) && moved(base, player.getLocation())) {
                player.sendMessage(configService.message("homes.cancelled-move"));
                return;
            }
            player.teleportAsync(record.location().toBukkit());
            player.sendMessage(configService.message("homes.teleported", new ConfigService.Placeholder("name", homeName)));
        }, warmup * 20L);
    }

    private boolean moved(Location first, Location second) {
        return first.getWorld() != second.getWorld() || first.distanceSquared(second) > 0.09;
    }

    public Map<String, Storage.HomeRecord> homes(UUID uuid) {
        return homes.getOrDefault(uuid, Map.of());
    }
}
