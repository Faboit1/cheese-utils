package com.faboit.cheeseutils.command;

import com.faboit.cheeseutils.config.ConfigService;
import com.faboit.cheeseutils.feature.pay.PayManager;
import com.faboit.cheeseutils.feature.settings.SettingsManager;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

public final class PayCommand implements CommandExecutor {
    private final PayManager payManager;
    private final SettingsManager settingsManager;
    private final ConfigService configService;
    private final JavaPlugin plugin;

    public PayCommand(JavaPlugin plugin, PayManager payManager, SettingsManager settingsManager, ConfigService configService) {
        this.plugin = plugin;
        this.payManager = payManager;
        this.settingsManager = settingsManager;
        this.configService = configService;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            return true;
        }
        String cmd = command.getName().toLowerCase();
        if (cmd.equals("payblock")) {
            settingsManager.toggle(player.getUniqueId(), "pay-block").whenComplete((next, throwable) -> Bukkit.getScheduler().runTask(plugin, () -> {
                if (throwable != null) {
                    player.sendMessage(configService.message("settings.save-failed"));
                    return;
                }
                player.sendMessage(configService.message("toggle.changed", new ConfigService.Placeholder("setting", "pay-block"), new ConfigService.Placeholder("state", next)));
            }));
            return true;
        }
        if (cmd.equals("paytoggle")) {
            settingsManager.toggle(player.getUniqueId(), "payment-notifications").whenComplete((next, throwable) -> Bukkit.getScheduler().runTask(plugin, () -> {
                if (throwable != null) {
                    player.sendMessage(configService.message("settings.save-failed"));
                    return;
                }
                player.sendMessage(configService.message("toggle.changed", new ConfigService.Placeholder("setting", "payment-notifications"), new ConfigService.Placeholder("state", next)));
            }));
            return true;
        }
        if (args.length < 2) {
            player.sendMessage(configService.message("pay.usage"));
            return true;
        }
        double amount;
        try {
            amount = Double.parseDouble(args[1]);
        } catch (NumberFormatException exception) {
            player.sendMessage(configService.message("pay.number"));
            return true;
        }
        payManager.pay(player, Bukkit.getOfflinePlayer(args[0]), amount);
        return true;
    }
}
