package dev.stonetpa.plugin.listener;

import dev.stonetpa.plugin.StoneTPA;
import dev.stonetpa.plugin.model.SendGuiHolder;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;

public class SendRequestGuiListener implements Listener {

    private final StoneTPA plugin;

    public SendRequestGuiListener(StoneTPA plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onClick(InventoryClickEvent event) {
        if (!(event.getView().getTopInventory().getHolder() instanceof SendGuiHolder holder)) {
            return;
        }

        event.setCancelled(true);

        if (event.getClickedInventory() == null || !event.getClickedInventory().equals(event.getView().getTopInventory())) {
            return;
        }

        if (!(event.getWhoClicked() instanceof Player sender)) {
            return;
        }

        int slot = event.getSlot();
        int sendSlot = plugin.getConfigManager().getInt("send-gui.send.slot", 11);
        int cancelSlot = plugin.getConfigManager().getInt("send-gui.cancel.slot", 15);

        if (slot == sendSlot) {
            plugin.getSendRequestGuiManager().playClickSound(sender);
            Player target = Bukkit.getPlayer(holder.getTargetId());
            sender.closeInventory();
            if (target == null || !target.isOnline()) {
                plugin.getMessageManager().sendChat(sender, "tpa.target-offline", null);
                return;
            }
            plugin.getRequestManager().sendRequest(sender, target, holder.getType());
        } else if (slot == cancelSlot) {
            plugin.getSendRequestGuiManager().playClickSound(sender);
            sender.closeInventory();
        }
    }

    @EventHandler
    public void onClose(InventoryCloseEvent event) {
        if (!(event.getInventory().getHolder() instanceof SendGuiHolder)) {
            return;
        }
        if (event.getPlayer() instanceof Player player) {
            plugin.getSendRequestGuiManager().stopTracking(player);
        }
    }
}
