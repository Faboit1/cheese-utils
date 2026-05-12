package com.faboit.cheeseutils.util;

import org.bukkit.Bukkit;

public final class VersionSupport {
    private VersionSupport() {
    }

    public static boolean supportsDialogs() {
        String version = Bukkit.getMinecraftVersion();
        if (compare(version, "1.21.6") < 0) {
            return false;
        }
        try {
            Class.forName("io.papermc.paper.dialog.Dialog");
            return true;
        } catch (ClassNotFoundException ignored) {
            return false;
        }
    }

    static int compare(String a, String b) {
        String[] aParts = a.split("\\.");
        String[] bParts = b.split("\\.");
        int max = Math.max(aParts.length, bParts.length);
        for (int i = 0; i < max; i++) {
            int ai = i < aParts.length ? parse(aParts[i]) : 0;
            int bi = i < bParts.length ? parse(bParts[i]) : 0;
            if (ai != bi) {
                return Integer.compare(ai, bi);
            }
        }
        return 0;
    }

    private static int parse(String raw) {
        StringBuilder builder = new StringBuilder();
        for (char c : raw.toCharArray()) {
            if (Character.isDigit(c)) {
                builder.append(c);
            } else {
                break;
            }
        }
        return builder.isEmpty() ? 0 : Integer.parseInt(builder.toString());
    }
}
