package com.faboit.cheeseutils.feature.tpa;

import com.faboit.cheeseutils.config.ConfigService;
import com.faboit.cheeseutils.feature.combat.CombatManager;
import com.faboit.cheeseutils.feature.settings.SettingsManager;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public final class TpaManager {
    public record Request(UUID from, UUID to, boolean here, long expiresAt) {}

    private final JavaPlugin plugin;
    private final ConfigService configService;
    private final CombatManager combatManager;
    private final SettingsManager settingsManager;
    private final Map<UUID, Request> requestsByTarget = new HashMap<>();
    private final Map<UUID, Long> cooldowns = new HashMap<>();

    public TpaManager(JavaPlugin plugin, ConfigService configService, CombatManager combatManager, SettingsManager settingsManager) {
        this.plugin = plugin;
        this.configService = configService;
        this.combatManager = combatManager;
        this.settingsManager = settingsManager;
    }

    public boolean canReceiveRequests(Player target, boolean here) {
        String key = here ? "tpahere-requests" : "tpa-requests";
        return settingsManager.isState(target.getUniqueId(), key, "enabled");
    }

    public void request(Player from, Player to, boolean here) {
        long now = System.currentTimeMillis();
        Long cooldownUntil = cooldowns.get(from.getUniqueId());
        if (cooldownUntil != null && cooldownUntil > now) {
            from.sendMessage(configService.message("tpa.cooldown", new ConfigService.Placeholder("seconds", Long.toString((cooldownUntil - now) / 1000L))));
            return;
        }
        if (combatManager.inCombat(from) && configService.config().getBoolean("tpa.block-in-combat", true)) {
            from.sendMessage(configService.message("tpa.blocked-combat"));
            return;
        }
        if (!canReceiveRequests(to, here)) {
            from.sendMessage(configService.message("tpa.target-disabled"));
            return;
        }
        long expires = now + configService.config().getLong("tpa.expiration-seconds", 30L) * 1000L;
        requestsByTarget.put(to.getUniqueId(), new Request(from.getUniqueId(), to.getUniqueId(), here, expires));
        cooldowns.put(from.getUniqueId(), now + configService.config().getLong("tpa.cooldown-seconds", 3L) * 1000L);
        from.sendMessage(configService.message("tpa.sent", new ConfigService.Placeholder("player", to.getName())));
        to.sendMessage(configService.message("tpa.received", new ConfigService.Placeholder("player", from.getName())));
    }

    public void deny(Player target) {
        Request request = requestsByTarget.remove(target.getUniqueId());
        if (request == null || request.expiresAt() < System.currentTimeMillis()) {
            target.sendMessage(configService.message("tpa.none"));
            return;
        }
        Player from = Bukkit.getPlayer(request.from());
        if (from != null) {
            from.sendMessage(configService.message("tpa.denied", new ConfigService.Placeholder("player", target.getName())));
        }
        target.sendMessage(configService.message("tpa.deny-self"));
    }

    public void accept(Player target) {
        Request request = requestsByTarget.remove(target.getUniqueId());
        if (request == null || request.expiresAt() < System.currentTimeMillis()) {
            target.sendMessage(configService.message("tpa.none"));
            return;
        }
        Player from = Bukkit.getPlayer(request.from());
        if (from == null) {
            target.sendMessage(configService.message("tpa.none"));
            return;
        }
        int warmup = configService.config().getInt("tpa.warmup-seconds", 3);
        target.sendMessage(configService.message("tpa.accepted"));
        from.sendMessage(configService.message("tpa.accepted-by", new ConfigService.Placeholder("player", target.getName())));
        Location fromLoc = from.getLocation().clone();
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            if (configService.config().getBoolean("tpa.cancel-on-move", true) && moved(fromLoc, from.getLocation())) {
                from.sendMessage(configService.message("tpa.move-cancel"));
                return;
            }
            if (request.here()) {
                target.teleportAsync(from.getLocation());
            } else {
                from.teleportAsync(target.getLocation());
            }
        }, warmup * 20L);
    }

    private boolean moved(Location first, Location second) {
        return first.getWorld() != second.getWorld() || first.distanceSquared(second) > 0.09;
    }
}
