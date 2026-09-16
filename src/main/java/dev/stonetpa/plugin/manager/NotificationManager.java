package dev.stonetpa.plugin.manager;

import dev.stonetpa.plugin.StoneTPA;
import dev.stonetpa.plugin.model.MessageDisplayType;
import net.kyori.adventure.bossbar.BossBar;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.title.Title;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class NotificationManager {

    private final StoneTPA plugin;
    private final Map<UUID, BossBar> activeBossBars = new ConcurrentHashMap<>();

    private static final Map<String, String> BOSSBAR_COLOR_ALIASES = Map.of(
            "GOLD", "YELLOW",
            "ORANGE", "YELLOW",
            "MAGENTA", "PURPLE",
            "CYAN", "BLUE",
            "GRAY", "WHITE",
            "GREY", "WHITE"
    );

    private static final Map<String, String> BOSSBAR_STYLE_ALIASES = Map.of(
            "SOLID", "PROGRESS",
            "BAR", "PROGRESS",
            "FULL", "PROGRESS",
            "NONE", "PROGRESS",
            "SEGMENTED", "NOTCHED_10",
            "SEGMENTS", "NOTCHED_10"
    );

    public NotificationManager(StoneTPA plugin) {
        this.plugin = plugin;
    }

    public void sendCountdown(Player player, int secondsRemaining, String otherPartyName) {
        Map<String, String> placeholders = Map.of(
                "seconds", String.valueOf(secondsRemaining),
                "player", otherPartyName
        );
        MessageDisplayType type = plugin.getConfigManager().getCountdownNotificationType();
        dispatch(player, "teleport.countdown", type, placeholders, false);
    }

    public void sendArrival(Player player, String otherPartyName) {
        MessageDisplayType type = plugin.getConfigManager().getArrivalNotificationType();
        dispatch(player, "teleport.arrival", type, Map.of("player", otherPartyName), true);
    }

    public void clearBossBar(Player player) {
        BossBar existing = activeBossBars.remove(player.getUniqueId());
        if (existing != null) {
            player.hideBossBar(existing);
        }
    }

    private void dispatch(Player player, String section, MessageDisplayType type, Map<String, String> placeholders, boolean autoHide) {
        ConfigManager cfg = plugin.getConfigManager();
        MessageManager mm = plugin.getMessageManager();

        switch (type) {
            case CHAT -> player.sendMessage(mm.format(cfg.getString(section + ".messages.text", ""), placeholders));
            case ACTIONBAR -> player.sendActionBar(mm.format(cfg.getString(section + ".messages.text", ""), placeholders));
            case BOSSBAR -> showBossBar(player, mm.format(cfg.getString(section + ".messages.bossbar", ""), placeholders), autoHide);
            case TITLE -> showTitle(player,
                    mm.format(cfg.getString(section + ".messages.title", ""), placeholders),
                    mm.format(cfg.getString(section + ".messages.subtitle", ""), placeholders));
        }
    }

    private void showTitle(Player player, Component title, Component subtitle) {
        ConfigManager cfg = plugin.getConfigManager();
        int fadeIn = cfg.getInt("teleport.title.fade-in-ticks", 5);
        int stay = cfg.getInt("teleport.title.stay-ticks", 30);
        int fadeOut = cfg.getInt("teleport.title.fade-out-ticks", 5);

        Title.Times times = Title.Times.times(Duration.ofMillis(fadeIn * 50L), Duration.ofMillis(stay * 50L),
                Duration.ofMillis(fadeOut * 50L));
        player.showTitle(Title.title(title, subtitle, times));
    }

    private void showBossBar(Player player, Component component, boolean autoHide) {
        BossBar existing = activeBossBars.get(player.getUniqueId());
        if (existing != null) {

            existing.name(component);
            if (autoHide) {
                scheduleAutoHide(player);
            }
            return;
        }

        ConfigManager cfg = plugin.getConfigManager();
        String colorName = cfg.getString("teleport.bossbar.color", "PURPLE");
        String styleName = cfg.getString("teleport.bossbar.style", "PROGRESS");

        BossBar.Color color;
        try {
            color = BossBar.Color.valueOf(colorName.toUpperCase());
        } catch (IllegalArgumentException ex) {
            String alias = BOSSBAR_COLOR_ALIASES.get(colorName.toUpperCase());
            color = alias != null ? BossBar.Color.valueOf(alias) : BossBar.Color.PURPLE;
        }
        BossBar.Overlay overlay;
        try {
            overlay = BossBar.Overlay.valueOf(styleName.toUpperCase());
        } catch (IllegalArgumentException ex) {
            String alias = BOSSBAR_STYLE_ALIASES.get(styleName.toUpperCase());
            overlay = alias != null ? BossBar.Overlay.valueOf(alias) : BossBar.Overlay.PROGRESS;
        }

        BossBar bar = BossBar.bossBar(component, 1.0f, color, overlay);
        player.showBossBar(bar);
        activeBossBars.put(player.getUniqueId(), bar);

        if (autoHide) {
            scheduleAutoHide(player);
        }
    }

    private void scheduleAutoHide(Player player) {
        ConfigManager cfg = plugin.getConfigManager();
        int duration = cfg.getInt("teleport.bossbar.duration-seconds", 5);
        UUID uuid = player.getUniqueId();
        BossBar barAtScheduleTime = activeBossBars.get(uuid);

        Bukkit.getScheduler().runTaskLater(plugin, () -> {

            BossBar current = activeBossBars.get(uuid);
            if (current == barAtScheduleTime) {
                activeBossBars.remove(uuid);
                if (player.isOnline()) {
                    player.hideBossBar(current);
                }
            }
        }, Math.max(1, duration) * 20L);
    }
}
