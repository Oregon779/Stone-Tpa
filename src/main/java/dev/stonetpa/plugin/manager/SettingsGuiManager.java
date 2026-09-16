package dev.stonetpa.plugin.manager;

import dev.stonetpa.plugin.StoneTPA;
import dev.stonetpa.plugin.model.SettingsGuiHolder;
import net.kyori.adventure.text.Component;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;

import java.util.List;
import java.util.Map;

public class SettingsGuiManager {

    private final StoneTPA plugin;

    public SettingsGuiManager(StoneTPA plugin) {
        this.plugin = plugin;
    }

    public void open(Player player) {
        ConfigManager cfg = plugin.getConfigManager();
        MessageManager mm = plugin.getMessageManager();

        int size = Math.max(9, Math.min(54, (cfg.getInt("gui.size", 27) / 9) * 9));
        Component title = mm.format(mm.getRaw("gui.title"), null);

        SettingsGuiHolder holder = new SettingsGuiHolder();
        Inventory inventory = plugin.getServer().createInventory(holder, size, title);
        holder.setInventory(inventory);

        if (cfg.getBoolean("gui.filler.enabled", true)) {
            applyBorder(inventory, size, cfg);
        }

        int headSlot = cfg.getInt("gui.player-head.slot", 4);
        if (cfg.getBoolean("gui.player-head.enabled", true) && headSlot >= 0 && headSlot < size) {
            inventory.setItem(headSlot, buildPlayerHeadItem(player));
        }

        int requestsSlot = cfg.getInt("gui.requests-toggle.slot", 11);
        if (requestsSlot >= 0 && requestsSlot < size) {
            inventory.setItem(requestsSlot, buildRequestsToggleItem(player));
        }

        int effectsSlot = cfg.getInt("gui.effects-toggle.slot", 15);
        if (effectsSlot >= 0 && effectsSlot < size) {
            inventory.setItem(effectsSlot, buildEffectsToggleItem(player));
        }

        if (cfg.getBoolean("gui.close-button.enabled", true)) {
            int closeSlot = cfg.getInt("gui.close-button.slot", 22);
            if (closeSlot >= 0 && closeSlot < size) {
                inventory.setItem(closeSlot, buildCloseButton());
            }
        }

        player.openInventory(inventory);
    }

    private void applyBorder(Inventory inventory, int size, ConfigManager cfg) {
        Material edgeMaterial = cfg.parseMaterial("gui.filler.material", Material.BLACK_STAINED_GLASS_PANE);
        Material accentMaterial = cfg.parseMaterial("gui.filler.accent-material", Material.PURPLE_STAINED_GLASS_PANE);

        ItemStack edge = buildFillerItem(edgeMaterial);
        ItemStack accent = buildFillerItem(accentMaterial);

        int rows = size / 9;
        for (int i = 0; i < size; i++) {
            int row = i / 9;
            int col = i % 9;
            boolean isTopOrBottom = row == 0 || row == rows - 1;
            boolean isEdgeColumn = col == 0 || col == 8;
            if (!isTopOrBottom && !isEdgeColumn) {
                continue;
            }
            boolean isCorner = isTopOrBottom && isEdgeColumn;
            inventory.setItem(i, isCorner ? accent : edge);
        }
    }

    private ItemStack buildFillerItem(Material material) {
        ItemStack filler = new ItemStack(material);
        ItemMeta fillerMeta = filler.getItemMeta();
        fillerMeta.displayName(Component.empty());
        filler.setItemMeta(fillerMeta);
        return filler;
    }

    private ItemStack buildPlayerHeadItem(Player player) {
        MessageManager mm = plugin.getMessageManager();

        ItemStack item = new ItemStack(Material.PLAYER_HEAD);
        SkullMeta meta = (SkullMeta) item.getItemMeta();
        meta.setOwningPlayer(player);
        meta.displayName(mm.format(mm.getRaw("gui.player-head-name"), Map.of("player", player.getName())));
        List<String> loreLines = mm.getRawList("gui.player-head-lore");
        meta.lore(loreLines.stream().map(line -> mm.format(line, (Map<String, String>) null)).toList());
        item.setItemMeta(meta);
        return item;
    }

    public void refresh(Player player) {
        Inventory top = player.getOpenInventory().getTopInventory();
        if (!(top.getHolder() instanceof SettingsGuiHolder)) {
            return;
        }

        ConfigManager cfg = plugin.getConfigManager();
        int requestsSlot = cfg.getInt("gui.requests-toggle.slot", 11);
        if (requestsSlot >= 0 && requestsSlot < top.getSize()) {
            top.setItem(requestsSlot, buildRequestsToggleItem(player));
        }

        int effectsSlot = cfg.getInt("gui.effects-toggle.slot", 15);
        if (effectsSlot >= 0 && effectsSlot < top.getSize()) {
            top.setItem(effectsSlot, buildEffectsToggleItem(player));
        }
    }

    private ItemStack buildRequestsToggleItem(Player player) {
        ConfigManager cfg = plugin.getConfigManager();
        boolean on = plugin.getPlayerSettingsManager().isAcceptingRequests(player.getUniqueId());
        Material material = cfg.parseMaterial(on ? "gui.requests-toggle.material-on" : "gui.requests-toggle.material-off",
                on ? Material.LIME_DYE : Material.GRAY_DYE);
        return buildToggleItem(material, "gui.requests-toggle.name", on ? "gui.requests-toggle.lore-on" : "gui.requests-toggle.lore-off");
    }

    private ItemStack buildEffectsToggleItem(Player player) {
        ConfigManager cfg = plugin.getConfigManager();
        boolean on = plugin.getPlayerSettingsManager().isEffectsEnabled(player.getUniqueId());
        Material material = cfg.parseMaterial(on ? "gui.effects-toggle.material-on" : "gui.effects-toggle.material-off",
                on ? Material.LIME_DYE : Material.GRAY_DYE);
        return buildToggleItem(material, "gui.effects-toggle.name", on ? "gui.effects-toggle.lore-on" : "gui.effects-toggle.lore-off");
    }

    private ItemStack buildToggleItem(Material material, String namePath, String lorePath) {
        MessageManager mm = plugin.getMessageManager();

        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        meta.displayName(mm.format(mm.getRaw(namePath), null));

        List<String> loreLines = mm.getRawList(lorePath);
        meta.lore(loreLines.stream().map(line -> mm.format(line, (Map<String, String>) null)).toList());

        item.setItemMeta(meta);
        return item;
    }

    private ItemStack buildCloseButton() {
        ConfigManager cfg = plugin.getConfigManager();
        MessageManager mm = plugin.getMessageManager();

        Material material = cfg.parseMaterial("gui.close-button.material", Material.BARRIER);
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        meta.displayName(mm.format(mm.getRaw("gui.close-button.name"), null));
        item.setItemMeta(meta);
        return item;
    }

    public void playClickSound(Player player) {
        ConfigManager cfg = plugin.getConfigManager();
        if (!cfg.getBoolean("gui.click-sound.enabled", true)) {
            return;
        }
        Sound sound = cfg.parseSound("gui.click-sound.sound", Sound.UI_BUTTON_CLICK);
        float volume = (float) cfg.getDouble("gui.click-sound.volume", 1.0);
        float pitch = (float) cfg.getDouble("gui.click-sound.pitch", 1.0);
        player.playSound(player.getLocation(), sound, volume, pitch);
    }
}
