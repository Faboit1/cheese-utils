package com.faboit.cheeseutils.command;

import com.faboit.cheeseutils.config.ConfigService;
import com.faboit.cheeseutils.feature.home.HomeManager;
import com.faboit.cheeseutils.feature.migration.UltimateHomesMigrationManager;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public final class HomeCommand implements CommandExecutor {
    private final HomeManager homeManager;
    private final UltimateHomesMigrationManager migrationManager;
    private final ConfigService configService;

    public HomeCommand(HomeManager homeManager, UltimateHomesMigrationManager migrationManager, ConfigService configService) {
        this.homeManager = homeManager;
        this.migrationManager = migrationManager;
        this.configService = configService;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("Players only.");
            return true;
        }
        String cmd = command.getName().toLowerCase();
        if (cmd.equals("sethome")) {
            if (args.length < 1) {
                player.sendMessage(configService.message("homes.set-usage"));
                return true;
            }
            homeManager.setHome(player, args[0]);
            return true;
        }
        if (cmd.equals("delhome")) {
            if (args.length < 1) {
                player.sendMessage(configService.message("homes.del-usage"));
                return true;
            }
            homeManager.delHome(player, args[0]);
            return true;
        }
        if (cmd.equals("homes")) {
            if (args.length >= 2 && args[0].equalsIgnoreCase("migrate")) {
                boolean dryRun = args[1].equalsIgnoreCase("dryrun");
                boolean ultimate = args[1].equalsIgnoreCase("ultimatehomes");
                if (dryRun || ultimate) {
                    migrationManager.migrate(player, dryRun);
                    return true;
                }
            }
            player.sendMessage(configService.message("homes.list", new ConfigService.Placeholder("homes", String.join(", ", homeManager.homes(player.getUniqueId()).keySet()))));
            return true;
        }
        if (args.length < 1) {
            player.sendMessage(configService.message("homes.usage"));
            return true;
        }
        homeManager.home(player, args[0]);
        return true;
    }
}
