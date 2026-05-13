package com.faboit.cheeseutils.config;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public final class ConfigService {
    private final JavaPlugin plugin;
    private final MiniMessage miniMessage;

    private FileConfiguration config;
    private FileConfiguration settingsConfig;
    private FileConfiguration messages;

    public ConfigService(JavaPlugin plugin, MiniMessage miniMessage) {
        this.plugin = plugin;
        this.miniMessage = miniMessage;
    }

    public void load() {
        ensure("config.yml");
        ensure("messages.yml");
        ensure("settings.yml");
        config = YamlConfiguration.loadConfiguration(new File(plugin.getDataFolder(), "config.yml"));
        settingsConfig = YamlConfiguration.loadConfiguration(new File(plugin.getDataFolder(), "settings.yml"));
        messages = YamlConfiguration.loadConfiguration(new File(plugin.getDataFolder(), "messages.yml"));
    }

    public void reload() {
        load();
    }

    private void ensure(String fileName) {
        if (!plugin.getDataFolder().exists()) {
            plugin.getDataFolder().mkdirs();
        }
        File file = new File(plugin.getDataFolder(), fileName);
        if (!file.exists()) {
            plugin.saveResource(fileName, false);
        }
    }

    public FileConfiguration config() {
        return config;
    }

    public FileConfiguration settingsConfig() {
        return settingsConfig;
    }

    public Component message(String key) {
        String raw = messages.getString(key, "<red>Missing message: " + key);
        return miniMessage.deserialize(raw);
    }

    public Component message(String key, Placeholder... placeholders) {
        String raw = messages.getString(key, "<red>Missing message: " + key);
        for (Placeholder placeholder : placeholders) {
            raw = raw.replace("{" + placeholder.key() + "}", placeholder.value());
        }
        return miniMessage.deserialize(raw);
    }

    public Sound sound(String path, Sound fallback) {
        String configured = config.getString(path);
        if (configured == null || configured.isBlank()) {
            return fallback;
        }
        try {
            return Sound.valueOf(configured.toUpperCase());
        } catch (IllegalArgumentException ignored) {
            return fallback;
        }
    }

    public record Placeholder(String key, String value) {}

    public List<SettingDefinition> settingDefinitions() {
        List<SettingDefinition> output = new ArrayList<>();
        ConfigurationSection root = settingsConfig.getConfigurationSection("settings");
        if (root == null) {
            return output;
        }
        for (String id : root.getKeys(false)) {
            ConfigurationSection section = root.getConfigurationSection(id);
            if (section == null) {
                continue;
            }
            int slot = section.getInt("slot", 0);
            String materialRaw = section.getString("material", "PAPER");
            Material material = Material.matchMaterial(materialRaw);
            if (material == null) {
                material = Material.PAPER;
            }
            String name = section.getString("name", id);
            String defaultState = section.getString("default-state", "enabled");
            List<SettingDefinition.State> states = new ArrayList<>();
            ConfigurationSection stateSection = section.getConfigurationSection("states");
            if (stateSection != null) {
                for (String stateId : stateSection.getKeys(false)) {
                    ConfigurationSection current = stateSection.getConfigurationSection(stateId);
                    if (current == null) {
                        continue;
                    }
                    List<String> lore = current.getStringList("lore");
                    List<String> commands = current.getStringList("commands");
                    String executor = current.getString("executor", "player");
                    String sound = current.getString("sound", "UI_BUTTON_CLICK");
                    states.add(new SettingDefinition.State(stateId, lore, commands, executor, sound));
                }
            }
            output.add(new SettingDefinition(id, slot, material, name, defaultState, states));
        }
        return output;
    }
}
