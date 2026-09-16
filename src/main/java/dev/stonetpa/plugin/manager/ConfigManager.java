package dev.stonetpa.plugin.manager;

import dev.stonetpa.plugin.StoneTPA;
import dev.stonetpa.plugin.config.ConfigUpdater;
import dev.stonetpa.plugin.model.MessageDisplayType;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.List;

public class ConfigManager {

    private static final String RESOURCE_PATH = "config.yml";

    private final StoneTPA plugin;
    private File configFile;
    private YamlConfiguration config;

    public ConfigManager(StoneTPA plugin) {
        this.plugin = plugin;
    }

    public void load() {
        configFile = new File(plugin.getDataFolder(), "config.yml");
        if (!configFile.exists()) {
            plugin.saveResource(RESOURCE_PATH, false);
        }

        try {
            ConfigUpdater.UpdateResult result = ConfigUpdater.update(plugin, RESOURCE_PATH, configFile);
            if (result.addedKeys() > 0) {
                plugin.getLogger().info("Added " + result.addedKeys() + " new option(s) to config.yml");
            }
        } catch (IOException ex) {
            plugin.getLogger().warning("Failed to update config.yml: " + ex.getMessage());
        }

        config = YamlConfiguration.loadConfiguration(configFile);
    }

    public void reload() {
        load();
    }

    public String getString(String path, String def) {
        return config.getString(path, def);
    }

    public int getInt(String path, int def) {
        return config.getInt(path, def);
    }

    public double getDouble(String path, double def) {
        return config.getDouble(path, def);
    }

    public boolean getBoolean(String path, boolean def) {
        return config.getBoolean(path, def);
    }

    public List<String> getStringList(String path) {
        return config.getStringList(path);
    }

    public String getLanguage() {
        return config.getString("language", "en");
    }

    public MessageDisplayType getCountdownNotificationType() {
        return MessageDisplayType.fromConfig(getString("teleport.countdown.notification", "ACTIONBAR"),
                MessageDisplayType.ACTIONBAR);
    }

    public MessageDisplayType getArrivalNotificationType() {
        return MessageDisplayType.fromConfig(getString("teleport.arrival.notification", "TITLE"),
                MessageDisplayType.TITLE);
    }

    public int getRequestExpireSeconds() {
        return Math.max(5, getInt("request.expire-seconds", 60));
    }

    public int getRequestCooldownSeconds() {
        return Math.max(0, getInt("request.cooldown-seconds", 10));
    }

    public int getTeleportDelaySeconds() {
        return Math.max(0, getInt("teleport.delay-seconds", 3));
    }

    public Material parseMaterial(String path, Material fallback) {
        String name = getString(path, fallback.name());
        try {
            Material material = Material.matchMaterial(name);
            return material != null ? material : fallback;
        } catch (IllegalArgumentException ex) {
            plugin.getLogger().warning("Invalid material '" + name + "' at '" + path + "' in config.yml, using " + fallback + " instead.");
            return fallback;
        }
    }

    public Sound parseSound(String path, Sound fallback) {
        String name = getString(path, fallback.name());
        try {
            return Sound.valueOf(name.toUpperCase());
        } catch (IllegalArgumentException ex) {
            plugin.getLogger().warning("Invalid sound '" + name + "' at '" + path + "' in config.yml, using " + fallback + " instead.");
            return fallback;
        }
    }

    public org.bukkit.Color parseColor(String path, org.bukkit.Color fallback) {
        String hex = getString(path, null);
        if (hex == null || hex.isBlank()) {
            return fallback;
        }
        try {
            String clean = hex.trim();
            if (clean.startsWith("#")) {
                clean = clean.substring(1);
            }
            int rgb = Integer.parseInt(clean, 16);
            return org.bukkit.Color.fromRGB(rgb);
        } catch (Exception ex) {
            plugin.getLogger().warning("Invalid color '" + hex + "' at '" + path + "' in config.yml, using default instead.");
            return fallback;
        }
    }
}
