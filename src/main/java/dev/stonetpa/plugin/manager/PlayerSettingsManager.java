package dev.stonetpa.plugin.manager;

import dev.stonetpa.plugin.StoneTPA;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class PlayerSettingsManager {

    private final StoneTPA plugin;
    private File dataFile;
    private YamlConfiguration data;

    // A single dedicated thread guarantees writes hit disk in the exact order
    // they were queued on the main thread. The shared Bukkit async pool has
    // multiple worker threads with no ordering guarantee between independent
    // tasks - two settings changes made moments apart could otherwise race
    // and the OLDER content could finish writing last, silently reverting the
    // newer change for every player in the file (this is one shared file for
    // all players, not per-player).
    private final ExecutorService writer = Executors.newSingleThreadExecutor(r -> {
        Thread thread = new Thread(r, "StoneTPA-PlayerData-Writer");
        thread.setDaemon(true);
        return thread;
    });

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

        // Best-effort cleanup of a stray temp file left behind by a write that
        // crashed between the write and the atomic rename in save() below.
        // playerdata.yml itself is never touched by that scenario, so this is
        // just tidiness, not a correctness requirement.
        File staleTmp = new File(plugin.getDataFolder(), "playerdata.yml.tmp");
        if (staleTmp.exists()) {
            staleTmp.delete();
        }
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

        try {
            writer.execute(() -> writeAtomically(content));
        } catch (java.util.concurrent.RejectedExecutionException ex) {
            // Only happens after shutdown() below has already run (plugin
            // disabling/reloading); dropping the very last write here is
            // preferable to reviving a shut-down executor.
            plugin.getLogger().warning("Could not save playerdata.yml: plugin is shutting down.");
        }
    }

    private void writeAtomically(String content) {
        Path target = dataFile.toPath();
        Path tmp = target.resolveSibling(dataFile.getName() + ".tmp");
        try {
            Files.writeString(tmp, content);
            try {
                Files.move(tmp, target, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
            } catch (java.nio.file.AtomicMoveNotSupportedException ex) {
                // Some filesystems (certain network mounts) don't support an
                // atomic rename - a plain replace is still far better than
                // writing directly into the live file in place.
                Files.move(tmp, target, StandardCopyOption.REPLACE_EXISTING);
            }
        } catch (IOException ex) {
            plugin.getLogger().warning("Could not save playerdata.yml: " + ex.getMessage());
        }
    }

    /**
     * Waits briefly for any in-flight write to finish, then stops accepting
     * new ones. Called from onDisable() so a /reload or server stop right
     * after a settings change doesn't drop that last write on the floor.
     */
    public void shutdown() {
        writer.shutdown();
        try {
            if (!writer.awaitTermination(2, TimeUnit.SECONDS)) {
                plugin.getLogger().warning("Timed out waiting for playerdata.yml to finish saving.");
            }
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
        }
    }
}
