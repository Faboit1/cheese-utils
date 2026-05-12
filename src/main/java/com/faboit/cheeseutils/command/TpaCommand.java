package com.faboit.cheeseutils.command;

import com.faboit.cheeseutils.config.ConfigService;
import com.faboit.cheeseutils.feature.settings.SettingsManager;
import com.faboit.cheeseutils.feature.tpa.TpaManager;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

public final class TpaCommand implements CommandExecutor {
    private final TpaManager tpaManager;
    private final SettingsManager settingsManager;
    private final ConfigService configService;
    private final JavaPlugin plugin;

    public TpaCommand(JavaPlugin plugin, TpaManager tpaManager, SettingsManager settingsManager, ConfigService configService) {
        this.plugin = plugin;
        this.tpaManager = tpaManager;
        this.settingsManager = settingsManager;
        this.configService = configService;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("Players only.");
            return true;
        }
        String cmd = command.getName().toLowerCase();
        if (cmd.equals("tpaccept")) {
            tpaManager.accept(player);
            return true;
        }
        if (cmd.equals("tpdeny")) {
            tpaManager.deny(player);
            return true;
        }
        if (cmd.equals("tpatoggle")) {
            toggle(player, "tpa-requests");
            return true;
        }
        if (cmd.equals("tpaheretoggle")) {
            toggle(player, "tpahere-requests");
            return true;
        }
        if (cmd.equals("tpaguitoggle")) {
            toggle(player, "tpa-confirm-menus");
            return true;
        }
        if (cmd.equals("tpauto")) {
            toggle(player, "tpa-auto-accept");
            return true;
        }
        if (args.length < 1) {
            player.sendMessage(configService.message("tpa.usage"));
            return true;
        }
        Player target = Bukkit.getPlayerExact(args[0]);
        if (target == null) {
            player.sendMessage(configService.message("player-not-found"));
            return true;
        }
        tpaManager.request(player, target, cmd.equals("tpahere"));
        return true;
    }

    private void toggle(Player player, String key) {
        settingsManager.toggle(player.getUniqueId(), key).whenComplete((next, throwable) -> Bukkit.getScheduler().runTask(plugin, () -> {
            if (throwable != null) {
                player.sendMessage(configService.message("settings.save-failed"));
                return;
            }
            player.sendMessage(configService.message("toggle.changed", new ConfigService.Placeholder("setting", key), new ConfigService.Placeholder("state", next)));
        }));
    }
}
