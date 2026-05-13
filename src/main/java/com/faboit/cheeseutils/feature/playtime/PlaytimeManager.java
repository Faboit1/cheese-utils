package com.faboit.cheeseutils.feature.playtime;

import com.faboit.cheeseutils.data.Storage;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class PlaytimeManager implements Listener {
    private final JavaPlugin plugin;
    private final Storage storage;

    /** Cached playtime data per player (loaded on join, updated on quit). */
    private final Map<UUID, Storage.PlaytimeRecord> cache = new ConcurrentHashMap<>();
    /** Session start timestamp in milliseconds. */
    private final Map<UUID, Long> sessionStart = new ConcurrentHashMap<>();

    public PlaytimeManager(JavaPlugin plugin, Storage storage) {
        this.plugin = plugin;
        this.storage = storage;
        // Periodic save every 5 minutes for online players
        Bukkit.getScheduler().runTaskTimerAsynchronously(plugin, this::flushAll, 20L * 60 * 5, 20L * 60 * 5);
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        UUID uuid = player.getUniqueId();
        long now = System.currentTimeMillis();
        sessionStart.put(uuid, now);
        storage.getPlaytime(uuid).thenAccept(record -> {
            if (record == null) {
                cache.put(uuid, new Storage.PlaytimeRecord(player.getName(), 0L, 0));
            } else {
                cache.put(uuid, record);
            }
            // Update join count immediately
            Storage.PlaytimeRecord existing = cache.get(uuid);
            Storage.PlaytimeRecord updated = new Storage.PlaytimeRecord(player.getName(), existing.timeSeconds(), existing.joins() + 1);
            cache.put(uuid, updated);
            storage.savePlaytime(uuid, updated.playerName(), updated.timeSeconds(), updated.joins());
        });
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        finalizeSession(event.getPlayer());
    }

    /** Called when the plugin disables to flush all remaining sessions. */
    public void onDisable() {
        for (Player player : Bukkit.getOnlinePlayers()) {
            finalizeSession(player);
        }
    }

    private void finalizeSession(Player player) {
        UUID uuid = player.getUniqueId();
        Long start = sessionStart.remove(uuid);
        if (start == null) {
            return;
        }
        long sessionSeconds = (System.currentTimeMillis() - start) / 1000L;
        Storage.PlaytimeRecord existing = cache.getOrDefault(uuid, new Storage.PlaytimeRecord(player.getName(), 0L, 0));
        Storage.PlaytimeRecord updated = new Storage.PlaytimeRecord(player.getName(), existing.timeSeconds() + sessionSeconds, existing.joins());
        cache.put(uuid, updated);
        storage.savePlaytime(uuid, updated.playerName(), updated.timeSeconds(), updated.joins());
    }

    private void flushAll() {
        for (Player player : Bukkit.getOnlinePlayers()) {
            UUID uuid = player.getUniqueId();
            Long start = sessionStart.get(uuid);
            if (start == null) {
                continue;
            }
            long sessionSeconds = (System.currentTimeMillis() - start) / 1000L;
            Storage.PlaytimeRecord existing = cache.getOrDefault(uuid, new Storage.PlaytimeRecord(player.getName(), 0L, 0));
            // Update cache with current session but don't reset the session start
            long total = existing.timeSeconds() + sessionSeconds;
            Storage.PlaytimeRecord updated = new Storage.PlaytimeRecord(player.getName(), total, existing.joins());
            cache.put(uuid, updated);
            // Reset session start so next flush calculates from now, avoiding double-counting
            sessionStart.put(uuid, System.currentTimeMillis());
            storage.savePlaytime(uuid, updated.playerName(), updated.timeSeconds(), updated.joins());
        }
    }

    /** Returns the current total playtime in seconds for a player, including current session. */
    public long getCurrentTimeSeconds(UUID uuid) {
        Storage.PlaytimeRecord record = cache.get(uuid);
        long base = record != null ? record.timeSeconds() : 0L;
        Long start = sessionStart.get(uuid);
        if (start != null) {
            base += (System.currentTimeMillis() - start) / 1000L;
        }
        return base;
    }

    public int getJoins(UUID uuid) {
        Storage.PlaytimeRecord record = cache.get(uuid);
        return record != null ? record.joins() : 0;
    }

    public String getPlayerName(UUID uuid) {
        Storage.PlaytimeRecord record = cache.get(uuid);
        return record != null ? record.playerName() : uuid.toString();
    }

    /** Format seconds into a human-friendly string: Xd Xh Xm Xs */
    public static String formatTime(long seconds) {
        long weeks = seconds / (7 * 24 * 3600);
        seconds %= 7 * 24 * 3600;
        long days = seconds / (24 * 3600);
        seconds %= 24 * 3600;
        long hours = seconds / 3600;
        seconds %= 3600;
        long minutes = seconds / 60;
        long secs = seconds % 60;
        StringBuilder sb = new StringBuilder();
        if (weeks > 0) sb.append(weeks).append("w ");
        if (days > 0) sb.append(days).append("d ");
        if (hours > 0) sb.append(hours).append("h ");
        if (minutes > 0) sb.append(minutes).append("m ");
        sb.append(secs).append("s");
        return sb.toString().trim();
    }
}
