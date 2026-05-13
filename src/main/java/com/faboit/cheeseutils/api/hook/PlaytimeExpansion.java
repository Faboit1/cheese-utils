package com.faboit.cheeseutils.api.hook;

import com.faboit.cheeseutils.feature.playtime.PlaytimeManager;
import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.OfflinePlayer;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * PlaceholderAPI expansion providing %playtime_*% placeholders.
 *
 * <ul>
 *   <li>%playtime_player%        – player name</li>
 *   <li>%playtime_time%          – formatted time (Xw Xd Xh Xm Xs)</li>
 *   <li>%playtime_time_seconds%  – total seconds</li>
 *   <li>%playtime_time_minutes%  – total minutes</li>
 *   <li>%playtime_time_hours%    – total hours</li>
 *   <li>%playtime_time_days%     – total days</li>
 *   <li>%playtime_time_weeks%    – total weeks</li>
 *   <li>%playtime_timesjoined%   – times joined</li>
 * </ul>
 */
public final class PlaytimeExpansion extends PlaceholderExpansion {
    private final PlaytimeManager playtimeManager;

    public PlaytimeExpansion(PlaytimeManager playtimeManager) {
        this.playtimeManager = playtimeManager;
    }

    @Override
    public @NotNull String getIdentifier() {
        return "playtime";
    }

    @Override
    public @NotNull String getAuthor() {
        return "Faboit1";
    }

    @Override
    public @NotNull String getVersion() {
        return "1.0.0";
    }

    @Override
    public boolean persist() {
        return true;
    }

    @Override
    public @Nullable String onRequest(OfflinePlayer player, @NotNull String params) {
        if (player == null) {
            return "";
        }
        long seconds = playtimeManager.getCurrentTimeSeconds(player.getUniqueId());
        return switch (params.toLowerCase()) {
            case "player" -> playtimeManager.getPlayerName(player.getUniqueId());
            case "time" -> PlaytimeManager.formatTime(seconds);
            case "time_seconds" -> Long.toString(seconds);
            case "time_minutes" -> Long.toString(seconds / 60);
            case "time_hours" -> Long.toString(seconds / 3600);
            case "time_days" -> Long.toString(seconds / (24 * 3600));
            case "time_weeks" -> Long.toString(seconds / (7 * 24 * 3600));
            case "timesjoined" -> Integer.toString(playtimeManager.getJoins(player.getUniqueId()));
            default -> null;
        };
    }
}
