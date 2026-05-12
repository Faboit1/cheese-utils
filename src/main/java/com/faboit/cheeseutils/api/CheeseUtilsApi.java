package com.faboit.cheeseutils.api;

import org.bukkit.entity.Player;

public interface CheeseUtilsApi {
    boolean isInCombat(Player player);

    long combatRemainingMillis(Player player);

    boolean isPayBlocked(Player player);

    boolean canReceiveTpa(Player player);
}
