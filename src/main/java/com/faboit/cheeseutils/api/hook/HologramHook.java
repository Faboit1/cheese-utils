package com.faboit.cheeseutils.api.hook;

import org.bukkit.Location;

public interface HologramHook {
    boolean isEnabled();

    void upsert(String id, Location location, String line);

    void remove(String id);
}
