package com.faboit.cheeseutils.feature.spawn;

import com.faboit.cheeseutils.config.ConfigService;
import com.faboit.cheeseutils.data.SerializedLocation;
import com.faboit.cheeseutils.data.Storage;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.concurrent.atomic.AtomicReference;

public final class SpawnManager {
    private final JavaPlugin plugin;
    private final ConfigService configService;
    private final Storage storage;
    private final AtomicReference<SerializedLocation> spawn = new AtomicReference<>();

    public SpawnManager(JavaPlugin plugin, ConfigService configService, Storage storage) {
        this.plugin = plugin;
        this.configService = configService;
        this.storage = storage;
    }

    public void load() {
        storage.loadSpawn().thenAccept(spawn::set);
    }

    public void setSpawn(Player player) {
        SerializedLocation location = SerializedLocation.from(player.getLocation());
        storage.saveSpawn(location).thenRun(() -> {
            spawn.set(location);
            player.sendMessage(configService.message("spawn.set"));
        });
    }

    public void teleportToSpawn(Player player) {
        SerializedLocation location = spawn.get();
        if (location == null) {
            player.sendMessage(configService.message("spawn.not-set"));
            return;
        }
        int warmup = configService.config().getInt("spawn.warmup-seconds", 3);
        Location base = player.getLocation().clone();
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            if (configService.config().getBoolean("spawn.cancel-on-move", true) && moved(base, player.getLocation())) {
                player.sendMessage(configService.message("spawn.cancelled-move"));
                return;
            }
            player.teleportAsync(location.toBukkit());
            player.sendMessage(configService.message("spawn.teleported"));
        }, warmup * 20L);
    }

    private boolean moved(Location first, Location second) {
        return first.getWorld() != second.getWorld() || first.distanceSquared(second) > 0.09;
    }
}
