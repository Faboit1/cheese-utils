package com.faboit.cheeseutils.feature.migration;

import com.faboit.cheeseutils.config.ConfigService;
import com.faboit.cheeseutils.data.SerializedLocation;
import com.faboit.cheeseutils.data.Storage;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.UUID;
import java.util.logging.Logger;

public final class UltimateHomesMigrationManager {
    private final File dataFolder;
    private final Storage storage;
    private final ConfigService configService;
    private final Logger logger;

    public UltimateHomesMigrationManager(File dataFolder, Storage storage, ConfigService configService, Logger logger) {
        this.dataFolder = dataFolder;
        this.storage = storage;
        this.configService = configService;
        this.logger = logger;
    }

    public void migrate(Player sender, boolean dryRun) {
        File source = new File(dataFolder.getParentFile(), "UltimateHomes/playerdata");
        if (!source.exists()) {
            sender.sendMessage(configService.message("migrate.not-found"));
            return;
        }
        if (!dryRun) {
            backup(source);
        }
        File[] files = source.listFiles((dir, name) -> name.endsWith(".yml"));
        if (files == null || files.length == 0) {
            sender.sendMessage(configService.message("migrate.empty"));
            return;
        }

        int migrated = 0;
        for (File file : files) {
            YamlConfiguration yaml = YamlConfiguration.loadConfiguration(file);
            ConfigurationSection homes = yaml.getConfigurationSection("player.homes");
            if (homes == null) {
                continue;
            }
            UUID uuid;
            try {
                uuid = UUID.fromString(file.getName().replace(".yml", ""));
            } catch (Exception exception) {
                logger.warning("Skipping invalid uuid file: " + file.getName());
                continue;
            }
            for (String homeName : homes.getKeys(false)) {
                ConfigurationSection section = homes.getConfigurationSection(homeName);
                if (section == null) {
                    continue;
                }
                SerializedLocation location = new SerializedLocation(
                        section.getString("world", "world"),
                        section.getDouble("x"),
                        section.getDouble("y"),
                        section.getDouble("z"),
                        (float) section.getDouble("yaw"),
                        (float) section.getDouble("pitch")
                );
                if (!dryRun) {
                    storage.saveHome(uuid, homeName, location, "OAK_DOOR");
                }
                migrated++;
            }
        }
        sender.sendMessage(configService.message("migrate.done", new ConfigService.Placeholder("count", Integer.toString(migrated))));
    }

    private void backup(File source) {
        File target = new File(dataFolder, "migration-backup/ultimatehomes-playerdata");
        if (!target.exists()) {
            target.mkdirs();
        }
        File[] files = source.listFiles((dir, name) -> name.endsWith(".yml"));
        if (files == null) {
            return;
        }
        for (File file : files) {
            try {
                Files.copy(file.toPath(), new File(target, file.getName()).toPath(), StandardCopyOption.REPLACE_EXISTING);
            } catch (IOException exception) {
                logger.warning("Failed to backup migration file " + file.getName() + ": " + exception.getMessage());
            }
        }
    }
}
