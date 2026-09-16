package dev.stonetpa.plugin.manager;

import dev.stonetpa.plugin.StoneTPA;
import org.bukkit.Bukkit;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.UUID;

public class PlayerSettingsManager {

    private final StoneTPA plugin;
    private File dataFile;
    private YamlConfiguration data;

    public PlayerSettingsManager(StoneTPA plugin) {
        this.plugin = plugin;
    }

    public void load() {
        dataFile = new File(plugin.getDataFolder(), "playerdata.yml");
        if (!dataFile.exists()) {
            try {
                plugin.getDataFolder().mkdirs();
                dataFile.createNewFile();
            } catch (IOException ex) {
                plugin.getLogger().warning("Could not create playerdata.yml: " + ex.getMessage());
            }
        }
        data = YamlConfiguration.loadConfiguration(dataFile);
    }

    public boolean isAcceptingRequests(UUID uuid) {
        return data.getBoolean(uuid + ".accepting-requests", true);
    }

    public void setAcceptingRequests(UUID uuid, boolean accepting) {
        data.set(uuid + ".accepting-requests", accepting);
        save();
    }

    public boolean isEffectsEnabled(UUID uuid) {
        return data.getBoolean(uuid + ".effects-enabled", true);
    }

    public void setEffectsEnabled(UUID uuid, boolean enabled) {
        data.set(uuid + ".effects-enabled", enabled);
        save();
    }

    private void save() {
        String content;
        try {
            content = data.saveToString();
        } catch (Exception ex) {
            plugin.getLogger().warning("Could not serialize playerdata.yml: " + ex.getMessage());
            return;
        }

        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            try {
                Files.writeString(dataFile.toPath(), content);
            } catch (IOException ex) {
                plugin.getLogger().warning("Could not save playerdata.yml: " + ex.getMessage());
            }
        });
    }
}
