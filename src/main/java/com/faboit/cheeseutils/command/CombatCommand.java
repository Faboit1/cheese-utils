package com.faboit.cheeseutils.command;

import com.faboit.cheeseutils.config.ConfigService;
import com.faboit.cheeseutils.feature.combat.CombatManager;
import com.faboit.cheeseutils.util.PermissionUtil;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public final class CombatCommand implements CommandExecutor {
    private final CombatManager combatManager;
    private final ConfigService configService;
    private final Runnable reloader;

    public CombatCommand(CombatManager combatManager, ConfigService configService, Runnable reloader) {
        this.combatManager = combatManager;
        this.configService = configService;
        this.reloader = reloader;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("Players only.");
            return true;
        }
        if (command.getName().equalsIgnoreCase("combatstatus")) {
            long remaining = combatManager.remainingMillis(player) / 1000L;
            player.sendMessage(configService.message("combat.status", new ConfigService.Placeholder("seconds", Long.toString(remaining))));
            return true;
        }
        if (!PermissionUtil.has(sender, "cheeseutils.combat.reload")) {
            sender.sendMessage(configService.message("no-permission"));
            return true;
        }
        reloader.run();
        sender.sendMessage(configService.message("reload.ok"));
        return true;
    }
}
