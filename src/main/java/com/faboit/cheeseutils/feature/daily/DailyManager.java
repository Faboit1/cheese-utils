package com.faboit.cheeseutils.feature.daily;

import com.faboit.cheeseutils.config.ConfigService;
import com.faboit.cheeseutils.data.Storage;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.time.Duration;
import java.util.List;

public final class DailyManager {
    private final JavaPlugin plugin;
    private final ConfigService configService;
    private final Storage storage;

    public DailyManager(JavaPlugin plugin, ConfigService configService, Storage storage) {
        this.plugin = plugin;
        this.configService = configService;
        this.storage = storage;
    }

    public void claim(Player player) {
        long now = System.currentTimeMillis();
        storage.getDailyLastClaim(player.getUniqueId()).thenAccept(lastClaim -> {
            long cooldownMs = Duration.ofHours(configService.config().getLong("daily.cooldown-hours", 24)).toMillis();
            if (lastClaim > 0 && now - lastClaim < cooldownMs) {
                long remaining = (cooldownMs - (now - lastClaim)) / 1000L;
                player.sendMessage(configService.message("daily.cooldown", new ConfigService.Placeholder("seconds", Long.toString(remaining))));
                return;
            }
            storage.getDailyState(player.getUniqueId()).thenAccept(state -> {
                int streak = state.streak() + 1;
                storage.setDailyLastClaim(player.getUniqueId(), now, streak);
                List<String> rewards = configService.config().getStringList("daily.rewards.commands");
                Bukkit.getScheduler().runTask(plugin, () -> {
                    for (String command : rewards) {
                        Bukkit.dispatchCommand(Bukkit.getConsoleSender(), command.replace("{player}", player.getName()).replace("{streak}", Integer.toString(streak)));
                    }
                    player.sendMessage(configService.message("daily.claimed", new ConfigService.Placeholder("streak", Integer.toString(streak))));
                });
            });
        });
    }
}
