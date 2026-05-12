package com.faboit.cheeseutils.api.hook;

import org.bukkit.OfflinePlayer;

public interface EconomyHook {
    boolean isEnabled();

    double balance(OfflinePlayer player);

    boolean withdraw(OfflinePlayer player, double amount);

    boolean deposit(OfflinePlayer player, double amount);
}
