package dev.stonetpa.plugin.listener;

import dev.stonetpa.plugin.StoneTPA;
import dev.stonetpa.plugin.model.SettingsGuiHolder;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;

public class SettingsGuiListener implements Listener {

    private final StoneTPA plugin;

    public SettingsGuiListener(StoneTPA plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onClick(InventoryClickEvent event) {
        if (!(event.getView().getTopInventory().getHolder() instanceof SettingsGuiHolder)) {
            return;
        }

        event.setCancelled(true);

        if (event.getClickedInventory() == null || !event.getClickedInventory().equals(event.getView().getTopInventory())) {
            return;
        }

        if (!(event.getWhoClicked() instanceof Player player)) {
            return;
        }

        int slot = event.getSlot();
        int requestsSlot = plugin.getConfigManager().getInt("gui.requests-toggle.slot", 11);
        int effectsSlot = plugin.getConfigManager().getInt("gui.effects-toggle.slot", 15);
        int closeSlot = plugin.getConfigManager().getInt("gui.close-button.slot", 22);

        if (slot == requestsSlot) {
            boolean newValue = !plugin.getPlayerSettingsManager().isAcceptingRequests(player.getUniqueId());
            plugin.getPlayerSettingsManager().setAcceptingRequests(player.getUniqueId(), newValue);
            plugin.getSettingsGuiManager().playClickSound(player);
            plugin.getSettingsGuiManager().refresh(player);
        } else if (slot == effectsSlot) {
            boolean newValue = !plugin.getPlayerSettingsManager().isEffectsEnabled(player.getUniqueId());
            plugin.getPlayerSettingsManager().setEffectsEnabled(player.getUniqueId(), newValue);
            plugin.getSettingsGuiManager().playClickSound(player);
            plugin.getSettingsGuiManager().refresh(player);
        } else if (slot == closeSlot) {
            plugin.getSettingsGuiManager().playClickSound(player);
            player.closeInventory();
        }
    }
}
