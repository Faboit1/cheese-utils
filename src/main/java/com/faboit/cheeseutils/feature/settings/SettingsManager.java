package com.faboit.cheeseutils.feature.settings;

import com.faboit.cheeseutils.config.ConfigService;
import com.faboit.cheeseutils.config.SettingDefinition;
import com.faboit.cheeseutils.data.Storage;
import com.faboit.cheeseutils.util.VersionSupport;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public final class SettingsManager implements Listener {
    private static final int SAVE_SLOT = 35;

    private final JavaPlugin plugin;
    private final ConfigService configService;
    private final Storage storage;
    private final MiniMessage miniMessage;

    private final Map<UUID, Map<String, String>> persistedState = new HashMap<>();
    private final Map<UUID, Session> sessions = new HashMap<>();

    public SettingsManager(JavaPlugin plugin, ConfigService configService, Storage storage, MiniMessage miniMessage) {
        this.plugin = plugin;
        this.configService = configService;
        this.storage = storage;
        this.miniMessage = miniMessage;
    }

    public CompletableFuture<Void> load(UUID uuid) {
        return storage.loadSettings(uuid).thenAccept(map -> persistedState.put(uuid, new HashMap<>(map)));
    }

    public String state(UUID uuid, String key) {
        Map<String, String> map = persistedState.computeIfAbsent(uuid, ignored -> new HashMap<>());
        return map.getOrDefault(key, defaultState(key));
    }

    public boolean isState(UUID uuid, String key, String expected) {
        return state(uuid, key).equalsIgnoreCase(expected);
    }

    public CompletableFuture<Void> setState(UUID uuid, String key, String state) {
        Map<String, String> current = persistedState.computeIfAbsent(uuid, ignored -> new HashMap<>());
        current.put(key, state);
        return storage.saveSettings(uuid, current);
    }

    public CompletableFuture<String> toggle(UUID uuid, String key) {
        String current = state(uuid, key);
        String next = current.equalsIgnoreCase("enabled") ? "disabled" : "enabled";
        return setState(uuid, key, next).thenApply(ignored -> next);
    }

    public boolean usesDialogApi() {
        if (!VersionSupport.supportsDialogs()) {
            return false;
        }
        return !configService.config().getBoolean("settings.force-chest-gui-on-modern", false);
    }

    public void open(Player player) {
        if (usesDialogApi()) {
            player.sendMessage(configService.message("settings.dialog-fallback-info"));
        }
        openChest(player);
    }

    private void openChest(Player player) {
        List<SettingDefinition> definitions = configService.settingDefinitions();
        int size = Math.max(9, configService.settingsConfig().getInt("gui-size", 36));
        Component title = miniMessage.deserialize(configService.settingsConfig().getString("gui-title", "<dark_gray>Settings"));
        Inventory inventory = Bukkit.createInventory(player, size, title);

        Map<String, String> staged = new HashMap<>();
        Map<String, String> current = persistedState.computeIfAbsent(player.getUniqueId(), ignored -> new HashMap<>());
        for (SettingDefinition definition : definitions) {
            String state = current.getOrDefault(definition.id(), definition.defaultState());
            staged.put(definition.id(), state);
            inventory.setItem(definition.slot(), buildItem(definition, definition.byId(state)));
        }
        inventory.setItem(SAVE_SLOT, saveButton());
        sessions.put(player.getUniqueId(), new Session(inventory, staged));
        player.openInventory(inventory);
    }

    private ItemStack saveButton() {
        ItemStack item = new ItemStack(Material.EMERALD_BLOCK);
        ItemMeta meta = item.getItemMeta();
        meta.displayName(miniMessage.deserialize("<green><bold>Save"));
        meta.lore(List.of(miniMessage.deserialize("<gray>Click to apply staged settings")));
        item.setItemMeta(meta);
        return item;
    }

    private ItemStack buildItem(SettingDefinition definition, SettingDefinition.State state) {
        ItemStack item = new ItemStack(definition.material());
        ItemMeta meta = item.getItemMeta();
        meta.displayName(miniMessage.deserialize(definition.name()));
        List<Component> lore = new ArrayList<>();
        for (String line : state.lore()) {
            lore.add(miniMessage.deserialize(line));
        }
        lore.add(miniMessage.deserialize("<gray>State: <yellow>" + state.id()));
        lore.add(miniMessage.deserialize("<dark_gray>Left click to cycle"));
        meta.lore(lore);
        item.setItemMeta(meta);
        return item;
    }

    private String defaultState(String key) {
        return configService.settingDefinitions().stream()
                .filter(definition -> definition.id().equalsIgnoreCase(key))
                .findFirst()
                .map(SettingDefinition::defaultState)
                .orElse("enabled");
    }

    @EventHandler
    public void onClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) {
            return;
        }
        Session session = sessions.get(player.getUniqueId());
        if (session == null || event.getInventory() != session.inventory()) {
            return;
        }
        event.setCancelled(true);
        if (event.getRawSlot() == SAVE_SLOT) {
            save(player, session);
            return;
        }
        SettingDefinition definition = configService.settingDefinitions().stream()
                .filter(it -> it.slot() == event.getRawSlot())
                .findFirst().orElse(null);
        if (definition == null) {
            return;
        }
        String current = session.staged().getOrDefault(definition.id(), definition.defaultState());
        SettingDefinition.State next = definition.next(current);
        session.staged().put(definition.id(), next.id());
        event.getInventory().setItem(event.getRawSlot(), buildItem(definition, next));
        try {
            player.playSound(player.getLocation(), Sound.valueOf(next.sound().toUpperCase()), 0.8f, 1.2f);
        } catch (Exception ignored) {
        }
    }

    private void save(Player player, Session session) {
        persistedState.put(player.getUniqueId(), new HashMap<>(session.staged()));
        storage.saveSettings(player.getUniqueId(), session.staged()).whenComplete((ignored, throwable) -> Bukkit.getScheduler().runTask(plugin, () -> {
            if (throwable != null) {
                player.sendMessage(configService.message("settings.save-failed"));
                return;
            }
            applyStateCommands(player, session.staged());
            player.sendMessage(configService.message("settings.saved"));
            player.closeInventory();
        }));
    }

    private void applyStateCommands(Player player, Map<String, String> staged) {
        for (SettingDefinition definition : configService.settingDefinitions()) {
            String stateId = staged.getOrDefault(definition.id(), definition.defaultState());
            SettingDefinition.State state = definition.byId(stateId);
            for (String command : state.commands()) {
                String parsed = command
                        .replace("{player}", player.getName())
                        .replace("{state}", stateId)
                        .replace("{setting}", definition.id());
                if ("console".equalsIgnoreCase(state.executor())) {
                    Bukkit.dispatchCommand(Bukkit.getConsoleSender(), parsed);
                } else {
                    Bukkit.dispatchCommand(player, parsed);
                }
            }
        }
    }

    @EventHandler
    public void onClose(InventoryCloseEvent event) {
        sessions.remove(event.getPlayer().getUniqueId());
    }

    private record Session(Inventory inventory, Map<String, String> staged) {}
}
