package com.faboit.cheeseutils.command;

import com.faboit.cheeseutils.feature.daily.DailyManager;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public final class DailyCommand implements CommandExecutor {
    private final DailyManager dailyManager;

    public DailyCommand(DailyManager dailyManager) {
        this.dailyManager = dailyManager;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (sender instanceof Player player) {
            dailyManager.claim(player);
        }
        return true;
    }
}
