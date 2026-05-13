package com.faboit.cheeseutils.command;

import com.faboit.cheeseutils.config.ConfigService;
import com.faboit.cheeseutils.feature.spawn.SpawnManager;
import com.faboit.cheeseutils.util.PermissionUtil;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public final class SpawnCommand implements CommandExecutor {
    private final SpawnManager spawnManager;
    private final ConfigService configService;

    public SpawnCommand(SpawnManager spawnManager, ConfigService configService) {
        this.spawnManager = spawnManager;
        this.configService = configService;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("Players only.");
            return true;
        }
        if (command.getName().equalsIgnoreCase("setspawn")) {
            if (!PermissionUtil.has(sender, "cheeseutils.spawn.set")) {
                player.sendMessage(configService.message("no-permission"));
                return true;
            }
            spawnManager.setSpawn(player);
            return true;
        }
        spawnManager.teleportToSpawn(player);
        return true;
    }
}
