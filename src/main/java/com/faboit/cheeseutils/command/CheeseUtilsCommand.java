package com.faboit.cheeseutils.command;

import com.faboit.cheeseutils.config.ConfigService;
import com.faboit.cheeseutils.feature.migration.PlaytimeMigrationManager;
import com.faboit.cheeseutils.feature.migration.UltimateHomesMigrationManager;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.List;

public final class CheeseUtilsCommand implements CommandExecutor, TabCompleter {
    private static final String PERMISSION_ADMIN = "cheeseutils.admin";
    private static final String PERMISSION_MIGRATE = "cheeseutils.migrate";

    private final ConfigService configService;
    private final PlaytimeMigrationManager playtimeMigration;
    private final UltimateHomesMigrationManager ultHomesMigration;
    private final MiniMessage miniMessage = MiniMessage.miniMessage();

    public CheeseUtilsCommand(ConfigService configService,
                              PlaytimeMigrationManager playtimeMigration,
                              UltimateHomesMigrationManager ultHomesMigration) {
        this.configService = configService;
        this.playtimeMigration = playtimeMigration;
        this.ultHomesMigration = ultHomesMigration;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0 || args[0].equalsIgnoreCase("help")) {
            sendHelp(sender);
            return true;
        }

        if (args[0].equalsIgnoreCase("migrate")) {
            if (!sender.hasPermission(PERMISSION_ADMIN) && !sender.hasPermission(PERMISSION_MIGRATE)) {
                sender.sendMessage(configService.message("no-permission"));
                return true;
            }
            if (args.length < 2) {
                sender.sendMessage(miniMessage.deserialize(
                        "<yellow>Usage: /cheeseutils migrate <playtime|playtimepulse|ultimatehomes>"));
                return true;
            }
            switch (args[1].toLowerCase()) {
                case "playtime" -> playtimeMigration.migratePlayTime(sender);
                case "playtimepulse" -> playtimeMigration.migratePlaytimePulse(sender);
                case "ultimatehomes" -> {
                    if (sender instanceof Player player) {
                        ultHomesMigration.migrate(player, false);
                    } else {
                        sender.sendMessage("Players only for ultimatehomes migration.");
                    }
                }
                default -> sender.sendMessage(miniMessage.deserialize(
                        "<red>Unknown source. Use: playtime, playtimepulse, ultimatehomes"));
            }
            return true;
        }

        sendHelp(sender);
        return true;
    }

    private void sendHelp(CommandSender sender) {
        sender.sendMessage(miniMessage.deserialize("<gold><bold>CheeseUtils Commands</bold>"));
        sender.sendMessage(miniMessage.deserialize("<yellow>/combatstatus <gray>- Check your combat status"));
        sender.sendMessage(miniMessage.deserialize("<yellow>/tpa <player> <gray>- Send a TPA request"));
        sender.sendMessage(miniMessage.deserialize("<yellow>/tpahere <player> <gray>- Request player to teleport to you"));
        sender.sendMessage(miniMessage.deserialize("<yellow>/tpaccept <gray>- Accept a TPA request"));
        sender.sendMessage(miniMessage.deserialize("<yellow>/tpdeny <gray>- Deny a TPA request"));
        sender.sendMessage(miniMessage.deserialize("<yellow>/spawn <gray>- Teleport to spawn"));
        sender.sendMessage(miniMessage.deserialize("<yellow>/setspawn <gray>- Set the spawn point"));
        sender.sendMessage(miniMessage.deserialize("<yellow>/warp <name> <gray>- Teleport to a warp"));
        sender.sendMessage(miniMessage.deserialize("<yellow>/setwarp <name> <gray>- Create a warp"));
        sender.sendMessage(miniMessage.deserialize("<yellow>/delwarp <name> <gray>- Delete a warp"));
        sender.sendMessage(miniMessage.deserialize("<yellow>/warps <gray>- List all warps"));
        sender.sendMessage(miniMessage.deserialize("<yellow>/home <name> <gray>- Teleport to a home"));
        sender.sendMessage(miniMessage.deserialize("<yellow>/sethome <name> <gray>- Set a home"));
        sender.sendMessage(miniMessage.deserialize("<yellow>/delhome <name> <gray>- Delete a home"));
        sender.sendMessage(miniMessage.deserialize("<yellow>/homes <gray>- List your homes"));
        sender.sendMessage(miniMessage.deserialize("<yellow>/daily <gray>- Claim your daily reward"));
        sender.sendMessage(miniMessage.deserialize("<yellow>/pay <player> <amount> <gray>- Pay a player"));
        sender.sendMessage(miniMessage.deserialize("<yellow>/settings <gray>- Open settings GUI"));
        sender.sendMessage(miniMessage.deserialize("<yellow>/cheeseutils migrate <source> <gray>- Migrate data from another plugin"));
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            return List.of("help", "migrate");
        }
        if (args.length == 2 && args[0].equalsIgnoreCase("migrate")) {
            return List.of("playtime", "playtimepulse", "ultimatehomes");
        }
        return List.of();
    }
}
