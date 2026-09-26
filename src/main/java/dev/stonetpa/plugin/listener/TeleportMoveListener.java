package dev.stonetpa.plugin.listener;

import dev.stonetpa.plugin.StoneTPA;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerTeleportEvent;

public class TeleportMoveListener implements Listener {

    private final StoneTPA plugin;

    public TeleportMoveListener(StoneTPA plugin) {
        this.plugin = plugin;
    }

    // ignoreCancelled=true: skip moves already blocked by another plugin
    // (region protection, anti-cheat) - avoids doing any work at all for
    // those, which matters once several plugins are stacked on a busy server.
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onMove(PlayerMoveEvent event) {
        Player player = event.getPlayer();

        // PlayerMoveEvent fires on nearly every client movement packet - with
        // 250+ players that's potentially thousands of calls per second on
        // the main thread. hasPending() is an O(1) HashMap lookup that is
        // false for the overwhelming majority of players at any given moment
        // (only those actively mid-teleport are ever in the map), so it must
        // run BEFORE the config lookup below, not after: checking a boolean
        // config value first meant paying a YamlConfiguration path lookup on
        // every single move event for every single player, even though only
        // a handful of players are ever actually teleporting at once.
        if (!plugin.getTeleportManager().hasPending(player.getUniqueId())) {
            return;
        }

        if (!plugin.getConfigManager().getBoolean("teleport.cancel-on-move", true)) {
            return;
        }

        Location from = event.getFrom();
        Location to = event.getTo();
        if (to == null) {
            return;
        }

        // Block-coordinate comparison instead of Location#distance(): avoids
        // a sqrt() call on every move event just to detect "did they move at
        // all", which is all we need here (no need for the exact distance).
        if (from.getBlockX() != to.getBlockX()
                || from.getBlockY() != to.getBlockY()
                || from.getBlockZ() != to.getBlockZ()) {
            plugin.getTeleportManager().cancelTeleport(player, false);
        }
    }

    // Covers teleports PlayerMoveEvent never sees: an ender pearl, /warp,
    // /spawn, another plugin's teleport, ... Safe against cancelling our OWN
    // accepted teleport: TeleportManager already removes the player from
    // "pending" before it ever calls player.teleportAsync(), so by the time
    // that teleport fires this event, hasPending() is already false here.
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onTeleport(PlayerTeleportEvent event) {
        Player player = event.getPlayer();

        if (!plugin.getTeleportManager().hasPending(player.getUniqueId())) {
            return;
        }

        if (!plugin.getConfigManager().getBoolean("teleport.cancel-on-move", true)) {
            return;
        }

        plugin.getTeleportManager().cancelTeleport(player, false);
    }
}
