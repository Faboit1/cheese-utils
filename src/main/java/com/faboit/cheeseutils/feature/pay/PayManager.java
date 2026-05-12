package com.faboit.cheeseutils.feature.pay;

import com.faboit.cheeseutils.config.ConfigService;
import com.faboit.cheeseutils.feature.settings.SettingsManager;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public final class PayManager {
    private final ConfigService configService;
    private final SettingsManager settingsManager;
    private final FormulaEngine formulaEngine;
    private final Map<UUID, Long> cooldowns = new HashMap<>();

    public PayManager(ConfigService configService, SettingsManager settingsManager, FormulaEngine formulaEngine) {
        this.configService = configService;
        this.settingsManager = settingsManager;
        this.formulaEngine = formulaEngine;
    }

    public boolean isPayBlocked(Player player) {
        return settingsManager.isState(player.getUniqueId(), "pay-block", "enabled");
    }

    public void pay(Player from, OfflinePlayer target, double amount) {
        if (!(target.getPlayer() instanceof Player onlineTarget)) {
            from.sendMessage(configService.message("pay.target-offline"));
            return;
        }
        if (onlineTarget.getUniqueId().equals(from.getUniqueId())) {
            from.sendMessage(configService.message("pay.self"));
            return;
        }
        if (isPayBlocked(onlineTarget)) {
            from.sendMessage(configService.message("pay.blocked"));
            return;
        }
        long now = System.currentTimeMillis();
        long cooldown = cooldowns.getOrDefault(from.getUniqueId(), 0L);
        if (cooldown > now) {
            from.sendMessage(configService.message("pay.cooldown"));
            return;
        }
        double minimum = configService.config().getDouble("pay.minimum", 1.0);
        if (amount < minimum) {
            from.sendMessage(configService.message("pay.minimum", new ConfigService.Placeholder("amount", Double.toString(minimum))));
            return;
        }
        String formula = configService.config().getString("pay.daily-limit-formula", "1000*{daysplayed}+10000");
        Map<String, Double> values = Map.of(
                "daysplayed", Math.max(1D, from.getStatistic(org.bukkit.Statistic.PLAY_ONE_MINUTE) / (20.0 * 60 * 60 * 24)),
                "kills", (double) from.getStatistic(org.bukkit.Statistic.PLAYER_KILLS),
                "deaths", (double) from.getStatistic(org.bukkit.Statistic.DEATHS),
                "balance", 0.0
        );
        double cap = formulaEngine.evaluate(formula, values);
        if (amount > cap && !from.hasPermission("cheeseutils.pay.bypasslimit") && !from.hasPermission("cheeseutils.admin")) {
            from.sendMessage(configService.message("pay.over-limit", new ConfigService.Placeholder("max", Double.toString(cap))));
            return;
        }
        cooldowns.put(from.getUniqueId(), now + configService.config().getLong("pay.cooldown-seconds", 3L) * 1000L);
        double taxRate = configService.config().getDouble("pay.tax-percent", 0.0) / 100.0;
        double taxed = amount - (amount * taxRate);
        from.sendMessage(configService.message("pay.sent", new ConfigService.Placeholder("player", onlineTarget.getName()), new ConfigService.Placeholder("amount", String.format("%.2f", amount))));
        if (settingsManager.isState(onlineTarget.getUniqueId(), "payment-notifications", "enabled")) {
            onlineTarget.sendMessage(configService.message("pay.received", new ConfigService.Placeholder("player", from.getName()), new ConfigService.Placeholder("amount", String.format("%.2f", taxed))));
        }
    }
}
