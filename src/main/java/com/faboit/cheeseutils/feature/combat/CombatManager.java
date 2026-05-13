package com.faboit.cheeseutils.feature.combat;

import com.faboit.cheeseutils.config.ConfigService;
import net.kyori.adventure.bossbar.BossBar;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.event.player.PlayerGameModeChangeEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public final class CombatManager implements Listener {
    private final JavaPlugin plugin;
    private final ConfigService configService;
    private final Map<UUID, Long> combatUntil = new HashMap<>();
    private final Map<UUID, BossBar> bossBars = new HashMap<>();

    public CombatManager(JavaPlugin plugin, ConfigService configService) {
        this.plugin = plugin;
        this.configService = configService;
    }

    public void startTicker() {
        Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            long now = System.currentTimeMillis();
            combatUntil.entrySet().removeIf(entry -> {
                Player player = Bukkit.getPlayer(entry.getKey());
                if (player == null) {
                    return false;
                }
                long remaining = entry.getValue() - now;
                if (remaining <= 0L) {
                    clear(player, true);
                    return true;
                }
                long seconds = Math.max(1L, remaining / 1000L);
                player.sendActionBar(configService.message("combat.actionbar", new ConfigService.Placeholder("seconds", Long.toString(seconds))));
                BossBar bossBar = bossBars.computeIfAbsent(player.getUniqueId(), ignored -> BossBar.bossBar(Component.empty(), 1.0f, BossBar.Color.RED, BossBar.Overlay.PROGRESS));
                bossBar.name(configService.message("combat.bossbar", new ConfigService.Placeholder("seconds", Long.toString(seconds))));
                bossBar.progress(Math.min(1f, Math.max(0f, remaining / (float) (combatDurationSeconds() * 1000L))));
                player.showBossBar(bossBar);
                return false;
            });
        }, 20L, 20L);
    }

    public void tag(Player one, Player two) {
        if (!enabledWorld(one) || !enabledWorld(two)) {
            return;
        }
        if (one.hasPermission("cheeseutils.combat.bypass") || two.hasPermission("cheeseutils.combat.bypass")) {
            return;
        }
        long until = System.currentTimeMillis() + combatDurationSeconds() * 1000L;
        combatUntil.put(one.getUniqueId(), until);
        combatUntil.put(two.getUniqueId(), until);
        one.sendMessage(configService.message("combat.tagged"));
        two.sendMessage(configService.message("combat.tagged"));
    }

    public long combatDurationSeconds() {
        return configService.config().getLong("combat.timer-seconds", 15L);
    }

    public boolean inCombat(Player player) {
        Long until = combatUntil.get(player.getUniqueId());
        return until != null && until > System.currentTimeMillis();
    }

    public long remainingMillis(Player player) {
        Long until = combatUntil.get(player.getUniqueId());
        if (until == null) {
            return 0L;
        }
        return Math.max(0L, until - System.currentTimeMillis());
    }

    private boolean enabledWorld(Player player) {
        Set<String> disabled = new HashSet<>(configService.config().getStringList("combat.disabled-worlds"));
        return !disabled.contains(player.getWorld().getName());
    }

    private void clear(Player player, boolean notify) {
        combatUntil.remove(player.getUniqueId());
        BossBar bar = bossBars.remove(player.getUniqueId());
        if (bar != null) {
            player.hideBossBar(bar);
        }
        if (notify) {
            player.sendMessage(configService.message("combat.cleared"));
            player.playSound(player.getLocation(), Sound.UI_TOAST_IN, 0.5f, 1.2f);
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onDamage(EntityDamageByEntityEvent event) {
        if (event.getEntity() instanceof Player victim && event.getDamager() instanceof Player attacker) {
            tag(attacker, victim);
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onCommand(PlayerCommandPreprocessEvent event) {
        if (!inCombat(event.getPlayer())) {
            return;
        }
        if (event.getPlayer().hasPermission("cheeseutils.combat.bypass")) {
            return;
        }
        String base = event.getMessage().split(" ")[0].toLowerCase();
        if (configService.config().getStringList("combat.blocked-commands").contains(base.replace("/", ""))) {
            event.setCancelled(true);
            event.getPlayer().sendMessage(configService.message("combat.blocked-command"));
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onTeleport(PlayerTeleportEvent event) {
        if (!inCombat(event.getPlayer()) || event.getPlayer().hasPermission("cheeseutils.combat.bypass")) {
            return;
        }
        if (configService.config().getBoolean("combat.prevent-teleport", true)) {
            event.setCancelled(true);
            event.getPlayer().sendMessage(configService.message("combat.blocked-teleport"));
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onGamemode(PlayerGameModeChangeEvent event) {
        if (event.getNewGameMode() == GameMode.SURVIVAL || event.getPlayer().hasPermission("cheeseutils.combat.bypass")) {
            return;
        }
        if (inCombat(event.getPlayer()) && configService.config().getBoolean("combat.prevent-gamemode", true)) {
            event.setCancelled(true);
            event.getPlayer().sendMessage(configService.message("combat.blocked-gamemode"));
        }
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        if (!inCombat(event.getPlayer())) {
            return;
        }
        String punishment = configService.config().getString("combat.logout-punishment", "death");
        if ("death".equalsIgnoreCase(punishment)) {
            event.getPlayer().setHealth(0.0);
        }
    }
}
