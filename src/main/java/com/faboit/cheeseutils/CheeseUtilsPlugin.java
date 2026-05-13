package com.faboit.cheeseutils;

import com.faboit.cheeseutils.api.CheeseUtilsApi;
import com.faboit.cheeseutils.bootstrap.ServiceRegistry;
import com.faboit.cheeseutils.command.*;
import com.faboit.cheeseutils.config.ConfigService;
import com.faboit.cheeseutils.data.SqlStorage;
import com.faboit.cheeseutils.data.Storage;
import com.faboit.cheeseutils.feature.combat.CombatManager;
import com.faboit.cheeseutils.feature.daily.DailyManager;
import com.faboit.cheeseutils.feature.home.HomeManager;
import com.faboit.cheeseutils.feature.migration.UltimateHomesMigrationManager;
import com.faboit.cheeseutils.feature.pay.FormulaEngine;
import com.faboit.cheeseutils.feature.pay.PayManager;
import com.faboit.cheeseutils.feature.settings.SettingsManager;
import com.faboit.cheeseutils.feature.spawn.SpawnManager;
import com.faboit.cheeseutils.feature.tpa.TpaManager;
import com.faboit.cheeseutils.feature.warp.WarpManager;
import com.faboit.cheeseutils.util.AsyncExecutor;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Bukkit;
import org.bukkit.command.PluginCommand;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.plugin.ServicesManager;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.Map;

public final class CheeseUtilsPlugin extends JavaPlugin implements Listener {
    private final ServiceRegistry services = new ServiceRegistry();
    private AsyncExecutor asyncExecutor;

    @Override
    public void onEnable() {
        MiniMessage miniMessage = MiniMessage.miniMessage();
        asyncExecutor = new AsyncExecutor();
        ConfigService configService = new ConfigService(this, miniMessage);
        configService.load();

        SqlStorage.DatabaseSettings db = new SqlStorage.DatabaseSettings(
                configService.config().getString("database.mode", "sqlite"),
                configService.config().getString("database.host", "127.0.0.1"),
                configService.config().getInt("database.port", 3306),
                configService.config().getString("database.name", "cheeseutils"),
                configService.config().getString("database.user", "root"),
                configService.config().getString("database.password", "password")
        );
        Storage storage = new SqlStorage(getDataFolder(), asyncExecutor, getLogger(), db);
        storage.initialize().join();

        SettingsManager settingsManager = new SettingsManager(this, configService, storage, miniMessage);
        CombatManager combatManager = new CombatManager(this, configService);
        TpaManager tpaManager = new TpaManager(this, configService, combatManager, settingsManager);
        HomeManager homeManager = new HomeManager(this, configService, storage);
        WarpManager warpManager = new WarpManager(configService, storage);
        SpawnManager spawnManager = new SpawnManager(this, configService, storage);
        DailyManager dailyManager = new DailyManager(this, configService, storage);
        PayManager payManager = new PayManager(configService, settingsManager, new FormulaEngine());
        UltimateHomesMigrationManager migrationManager = new UltimateHomesMigrationManager(getDataFolder(), storage, configService, getLogger());

        warpManager.load().join();
        spawnManager.load();

        services.register(ConfigService.class, configService);
        services.register(Storage.class, storage);
        services.register(SettingsManager.class, settingsManager);
        services.register(CombatManager.class, combatManager);
        services.register(TpaManager.class, tpaManager);
        services.register(HomeManager.class, homeManager);
        services.register(WarpManager.class, warpManager);
        services.register(SpawnManager.class, spawnManager);
        services.register(DailyManager.class, dailyManager);
        services.register(PayManager.class, payManager);

        registerCommands(configService, combatManager, tpaManager, settingsManager, spawnManager, warpManager, homeManager, migrationManager, dailyManager, payManager);
        Bukkit.getPluginManager().registerEvents(this, this);
        Bukkit.getPluginManager().registerEvents(combatManager, this);
        Bukkit.getPluginManager().registerEvents(settingsManager, this);
        combatManager.startTicker();

        ServicesManager servicesManager = Bukkit.getServicesManager();
        servicesManager.register(CheeseUtilsApi.class, new CheeseUtilsApiImpl(combatManager, settingsManager), this, org.bukkit.plugin.ServicePriority.Normal);
        getLogger().info("CheeseUtils enabled.");
    }

    private void registerCommands(ConfigService configService,
                                  CombatManager combatManager,
                                  TpaManager tpaManager,
                                  SettingsManager settingsManager,
                                  SpawnManager spawnManager,
                                  WarpManager warpManager,
                                  HomeManager homeManager,
                                  UltimateHomesMigrationManager migrationManager,
                                  DailyManager dailyManager,
                                  PayManager payManager) {
        register("combatstatus", new CombatCommand(combatManager, configService, configService::reload));
        register("combatreload", new CombatCommand(combatManager, configService, configService::reload));

        TpaCommand tpaCommand = new TpaCommand(this, tpaManager, settingsManager, configService);
        for (String command : new String[]{"tpa", "tpahere", "tpaccept", "tpdeny", "tpauto", "tpatoggle", "tpaheretoggle", "tpaguitoggle"}) {
            register(command, tpaCommand);
        }

        SpawnCommand spawnCommand = new SpawnCommand(spawnManager, configService);
        register("spawn", spawnCommand);
        register("setspawn", spawnCommand);

        WarpCommand warpCommand = new WarpCommand(warpManager, configService);
        for (String command : new String[]{"warp", "setwarp", "delwarp", "warps"}) {
            register(command, warpCommand);
        }

        HomeCommand homeCommand = new HomeCommand(homeManager, migrationManager, configService);
        for (String command : new String[]{"home", "sethome", "delhome", "homes"}) {
            register(command, homeCommand);
        }

        register("daily", new DailyCommand(dailyManager));

        PayCommand payCommand = new PayCommand(this, payManager, settingsManager, configService);
        for (String command : new String[]{"pay", "paytoggle", "payblock"}) {
            register(command, payCommand);
        }

        register("settings", new SettingsCommand(settingsManager));
    }

    private void register(String name, org.bukkit.command.CommandExecutor executor) {
        PluginCommand command = getCommand(name);
        if (command != null) {
            command.setExecutor(executor);
        } else {
            getLogger().warning("Missing command in plugin.yml: " + name);
        }
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        services.get(SettingsManager.class).load(player.getUniqueId());
        services.get(HomeManager.class).load(player.getUniqueId());
        if (config().getBoolean("spawn.first-join-teleport", true) && !player.hasPlayedBefore()) {
            Bukkit.getScheduler().runTaskLater(this, () -> services.get(SpawnManager.class).teleportToSpawn(player), 20L);
        } else if (config().getBoolean("spawn.teleport-on-join", false)) {
            Bukkit.getScheduler().runTaskLater(this, () -> services.get(SpawnManager.class).teleportToSpawn(player), 20L);
        }
    }

    private org.bukkit.configuration.file.FileConfiguration config() {
        return services.get(ConfigService.class).config();
    }

    @Override
    public void onDisable() {
        Storage storage = services.get(Storage.class);
        storage.close();
        if (asyncExecutor != null) {
            asyncExecutor.close();
        }
    }

    private record CheeseUtilsApiImpl(CombatManager combatManager, SettingsManager settingsManager) implements CheeseUtilsApi {
        @Override
        public boolean isInCombat(Player player) {
            return combatManager.inCombat(player);
        }

        @Override
        public long combatRemainingMillis(Player player) {
            return combatManager.remainingMillis(player);
        }

        @Override
        public boolean isPayBlocked(Player player) {
            return settingsManager.isState(player.getUniqueId(), "pay-block", "enabled");
        }

        @Override
        public boolean canReceiveTpa(Player player) {
            return settingsManager.isState(player.getUniqueId(), "tpa-requests", "enabled");
        }
    }
}
