package com.faboit.cheeseutils.util;

import org.bukkit.command.CommandSender;

public final class PermissionUtil {
    private PermissionUtil() {
    }

    public static boolean has(CommandSender sender, String permission) {
        return sender.hasPermission("cheeseutils.admin") || sender.hasPermission(permission);
    }
}
