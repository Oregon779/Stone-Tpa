package dev.stonetpa.plugin.listener;

import dev.stonetpa.plugin.StoneTPA;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;

public class PlayerQuitListener implements Listener {

    private final StoneTPA plugin;

    public PlayerQuitListener(StoneTPA plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        plugin.getRequestManager().handleQuit(player);
        plugin.getTeleportManager().cancelTeleport(player, true);

        plugin.getTeleportManager().cancelForDisconnectedOtherParty(player.getUniqueId());
        plugin.getSendRequestGuiManager().stopTracking(player);
    }
}
