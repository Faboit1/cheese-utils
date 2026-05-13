package com.faboit.cheeseutils.command;

import com.faboit.cheeseutils.config.ConfigService;
import com.faboit.cheeseutils.feature.warp.WarpManager;
import com.faboit.cheeseutils.util.PermissionUtil;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public final class WarpCommand implements CommandExecutor {
    private final WarpManager warpManager;
    private final ConfigService configService;

    public WarpCommand(WarpManager warpManager, ConfigService configService) {
        this.warpManager = warpManager;
        this.configService = configService;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("Players only.");
            return true;
        }
        String cmd = command.getName().toLowerCase();
        if (cmd.equals("setwarp")) {
            if (!PermissionUtil.has(sender, "cheeseutils.warp.admin")) {
                player.sendMessage(configService.message("no-permission"));
                return true;
            }
            if (args.length < 1) {
                player.sendMessage(configService.message("warp.set-usage"));
                return true;
            }
            warpManager.setWarp(player, args[0], args.length >= 2 ? args[1] : "default");
            return true;
        }
        if (cmd.equals("delwarp")) {
            if (!PermissionUtil.has(sender, "cheeseutils.warp.admin")) {
                player.sendMessage(configService.message("no-permission"));
                return true;
            }
            if (args.length < 1) {
                player.sendMessage(configService.message("warp.del-usage"));
                return true;
            }
            warpManager.delWarp(player, args[0]);
            return true;
        }
        if (cmd.equals("warps")) {
            player.sendMessage(configService.message("warp.list", new ConfigService.Placeholder("warps", String.join(", ", warpManager.warps().keySet()))));
            return true;
        }
        if (args.length < 1) {
            player.sendMessage(configService.message("warp.usage"));
            return true;
        }
        warpManager.warp(player, args[0]);
        return true;
    }
}
