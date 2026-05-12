package com.faboit.cheeseutils.data;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;

public record SerializedLocation(String world, double x, double y, double z, float yaw, float pitch) {
    public static SerializedLocation from(Location location) {
        return new SerializedLocation(location.getWorld().getName(), location.getX(), location.getY(), location.getZ(), location.getYaw(), location.getPitch());
    }

    public Location toBukkit() {
        World targetWorld = Bukkit.getWorld(world);
        if (targetWorld == null) {
            throw new IllegalStateException("World not found: " + world);
        }
        return new Location(targetWorld, x, y, z, yaw, pitch);
    }
}
