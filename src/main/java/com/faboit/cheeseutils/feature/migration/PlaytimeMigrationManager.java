package com.faboit.cheeseutils.feature.migration;

import com.faboit.cheeseutils.config.ConfigService;
import com.faboit.cheeseutils.data.Storage;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.FileReader;
import java.util.UUID;
import java.util.logging.Logger;

/**
 * Migrates playtime data from third-party plugins into CheeseUtils.
 *
 * <p>Supported sources:</p>
 * <ul>
 *   <li><b>PlayTime</b> – {@code plugins/PlayTime/userdata.json}
 *       JSON array: [{uuid, lastName, joins, time(seconds), session}, ...]</li>
 *   <li><b>PlaytimePulse</b> – {@code plugins/PlaytimePulse/playtime.yml}
 *       YAML: players.{uuid}.{name, seconds, joins}</li>
 * </ul>
 */
public final class PlaytimeMigrationManager {
    private final File pluginsFolder;
    private final Storage storage;
    private final ConfigService configService;
    private final Logger logger;

    public PlaytimeMigrationManager(File pluginsFolder, Storage storage, ConfigService configService, Logger logger) {
        this.pluginsFolder = pluginsFolder;
        this.storage = storage;
        this.configService = configService;
        this.logger = logger;
    }

    /** Migrate from plugins/PlayTime/userdata.json */
    public void migratePlayTime(CommandSender sender) {
        File source = new File(pluginsFolder, "PlayTime/userdata.json");
        if (!source.exists()) {
            sender.sendMessage(configService.message("cheeseutils.migrate.playtime-not-found"));
            return;
        }
        int count = 0;
        try (FileReader reader = new FileReader(source)) {
            JsonArray array = JsonParser.parseReader(reader).getAsJsonArray();
            for (JsonElement element : array) {
                JsonObject obj = element.getAsJsonObject();
                String uuidStr = obj.get("uuid").getAsString();
                String name = obj.get("lastName").getAsString();
                long timeSeconds = obj.get("time").getAsLong();
                int joins = obj.get("joins").getAsInt();
                UUID uuid;
                try {
                    uuid = UUID.fromString(uuidStr);
                } catch (IllegalArgumentException e) {
                    logger.warning("[PlaytimeMigration] Skipping invalid UUID: " + uuidStr);
                    continue;
                }
                storage.savePlaytime(uuid, name, timeSeconds, joins);
                count++;
            }
        } catch (Exception e) {
            logger.severe("[PlaytimeMigration] Failed to read PlayTime userdata.json: " + e.getMessage());
            sender.sendMessage(configService.message("cheeseutils.migrate.error"));
            return;
        }
        sender.sendMessage(configService.message("cheeseutils.migrate.done",
                new ConfigService.Placeholder("count", Integer.toString(count)),
                new ConfigService.Placeholder("source", "PlayTime")));
    }

    /** Migrate from plugins/PlaytimePulse/playtime.yml */
    public void migratePlaytimePulse(CommandSender sender) {
        File source = new File(pluginsFolder, "PlaytimePulse/playtime.yml");
        if (!source.exists()) {
            sender.sendMessage(configService.message("cheeseutils.migrate.playtimepulse-not-found"));
            return;
        }
        YamlConfiguration yaml = YamlConfiguration.loadConfiguration(source);
        ConfigurationSection players = yaml.getConfigurationSection("players");
        if (players == null) {
            sender.sendMessage(configService.message("cheeseutils.migrate.empty"));
            return;
        }
        int count = 0;
        for (String uuidStr : players.getKeys(false)) {
            ConfigurationSection section = players.getConfigurationSection(uuidStr);
            if (section == null) {
                continue;
            }
            UUID uuid;
            try {
                uuid = UUID.fromString(uuidStr);
            } catch (IllegalArgumentException e) {
                logger.warning("[PlaytimeMigration] Skipping invalid UUID: " + uuidStr);
                continue;
            }
            String name = section.getString("name", uuidStr);
            long timeSeconds = section.getLong("seconds", section.getLong("playtime", 0L));
            int joins = section.getInt("joins", 0);
            storage.savePlaytime(uuid, name, timeSeconds, joins);
            count++;
        }
        sender.sendMessage(configService.message("cheeseutils.migrate.done",
                new ConfigService.Placeholder("count", Integer.toString(count)),
                new ConfigService.Placeholder("source", "PlaytimePulse")));
    }
}
