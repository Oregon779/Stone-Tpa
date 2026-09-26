package dev.stonetpa.plugin.manager;

import dev.stonetpa.plugin.StoneTPA;
import dev.stonetpa.plugin.model.RequestType;
import dev.stonetpa.plugin.model.SendGuiHolder;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.scheduler.BukkitTask;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class SendRequestGuiManager {

    private record OpenSendGui(Player sender, Player target, SendGuiHolder holder) {
    }

    private final StoneTPA plugin;

    // One shared repeating task refreshes every open Send-GUI instead of
    // scheduling a separate BukkitTask per viewer. With up to ~300 players
    // potentially having this GUI open at once, that would mean ~300
    // individual scheduler entries doing near-identical work every second;
    // one task iterating a small map does the same job with a single
    // scheduler entry. It only runs while at least one Send-GUI is open.
    private final Map<UUID, OpenSendGui> openGuis = new HashMap<>();
    private BukkitTask sharedTickTask;

    public SendRequestGuiManager(StoneTPA plugin) {
        this.plugin = plugin;
    }

    public void open(Player sender, Player target, RequestType type) {
        ConfigManager cfg = plugin.getConfigManager();
        if (!cfg.getBoolean("send-gui.enabled", true)) {

            plugin.getRequestManager().sendRequest(sender, target, type);
            return;
        }

        MessageManager mm = plugin.getMessageManager();
        boolean isTpa = type == RequestType.TPA;

        int size = Math.max(9, Math.min(54, (cfg.getInt("send-gui.size", 27) / 9) * 9));
        Component title = mm.format(mm.getRaw(isTpa ? "gui.send.title-tpa" : "gui.send.title-tpahere"), null);

        SendGuiHolder holder = new SendGuiHolder(target.getUniqueId(), target.getName(), type);
        Inventory inventory = plugin.getServer().createInventory(holder, size, title);
        holder.setInventory(inventory);

        if (cfg.getBoolean("send-gui.filler.enabled", true)) {
            applyBorder(inventory, size, cfg);
        }

        int headSlot = cfg.getInt("send-gui.target-head.slot", 13);
        if (headSlot >= 0 && headSlot < size) {
            inventory.setItem(headSlot, buildTargetHeadItem(target));
        }

        int dimensionSlot = cfg.getInt("send-gui.dimension-info.slot", 14);
        if (dimensionSlot >= 0 && dimensionSlot < size) {
            inventory.setItem(dimensionSlot, buildDimensionItem(target));
        }

        int pingSlot = cfg.getInt("send-gui.ping-info.slot", 12);
        if (pingSlot >= 0 && pingSlot < size) {
            inventory.setItem(pingSlot, buildPingItem(target));
        }

        int sendSlot = cfg.getInt("send-gui.send.slot", 11);
        if (sendSlot >= 0 && sendSlot < size) {
            inventory.setItem(sendSlot, buildButtonItem("send-gui.send.material", Material.LIME_CONCRETE,
                    "gui.send.send-name", "gui.send.send-lore"));
        }

        int cancelSlot = cfg.getInt("send-gui.cancel.slot", 15);
        if (cancelSlot >= 0 && cancelSlot < size) {
            inventory.setItem(cancelSlot, buildButtonItem("send-gui.cancel.material", Material.RED_CONCRETE,
                    "gui.send.cancel-name", "gui.send.cancel-lore"));
        }

        sender.openInventory(inventory);
        startTicking(sender, target, holder);
    }

    private void startTicking(Player sender, Player target, SendGuiHolder holder) {
        openGuis.put(sender.getUniqueId(), new OpenSendGui(sender, target, holder));
        if (sharedTickTask == null) {
            sharedTickTask = Bukkit.getScheduler().runTaskTimer(plugin, this::tickAll, 20L, 20L);
        }
    }

    private void tickAll() {
        if (openGuis.isEmpty()) {
            return;
        }

        ConfigManager cfg = plugin.getConfigManager();
        int dimensionSlot = cfg.getInt("send-gui.dimension-info.slot", 14);
        int pingSlot = cfg.getInt("send-gui.ping-info.slot", 12);

        List<UUID> toRemove = new ArrayList<>();
        for (OpenSendGui open : openGuis.values()) {
            Player sender = open.sender();
            Player target = open.target();

            if (!sender.isOnline() || !isShowing(sender, open.holder())) {
                toRemove.add(sender.getUniqueId());
                continue;
            }
            if (!target.isOnline()) {
                sender.closeInventory();
                toRemove.add(sender.getUniqueId());
                continue;
            }

            Inventory top = sender.getOpenInventory().getTopInventory();
            if (dimensionSlot >= 0 && dimensionSlot < top.getSize()) {
                top.setItem(dimensionSlot, buildDimensionItem(target));
            }
            if (pingSlot >= 0 && pingSlot < top.getSize()) {
                top.setItem(pingSlot, buildPingItem(target));
            }
        }

        for (UUID uuid : toRemove) {
            openGuis.remove(uuid);
        }
        stopTickingIfIdle();
    }

    private void stopTickingIfIdle() {
        if (openGuis.isEmpty() && sharedTickTask != null) {
            sharedTickTask.cancel();
            sharedTickTask = null;
        }
    }

    private boolean isShowing(Player player, SendGuiHolder holder) {
        return player.getOpenInventory().getTopInventory().getHolder() == holder;
    }

    public void stopTracking(Player sender) {
        openGuis.remove(sender.getUniqueId());
        stopTickingIfIdle();
    }

    private void applyBorder(Inventory inventory, int size, ConfigManager cfg) {
        Material edgeMaterial = cfg.parseMaterial("send-gui.filler.material", Material.BLACK_STAINED_GLASS_PANE);
        Material accentMaterial = cfg.parseMaterial("send-gui.filler.accent-material", Material.PURPLE_STAINED_GLASS_PANE);

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

    private ItemStack buildTargetHeadItem(Player target) {
        MessageManager mm = plugin.getMessageManager();

        ItemStack item = new ItemStack(Material.PLAYER_HEAD);
        SkullMeta meta = (SkullMeta) item.getItemMeta();
        meta.setOwningPlayer(target);
        meta.displayName(mm.format(mm.getRaw("gui.send.target-head-name"), Map.of("player", target.getName())));
        item.setItemMeta(meta);
        return item;
    }

    private ItemStack buildDimensionItem(Player target) {
        ConfigManager cfg = plugin.getConfigManager();
        MessageManager mm = plugin.getMessageManager();

        Material material = cfg.parseMaterial("send-gui.dimension-info.material", Material.COMPASS);
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();

        Map<String, String> placeholders = Map.of("dimension", dimensionName(target.getWorld().getEnvironment()));
        meta.displayName(mm.format(mm.getRaw("gui.send.dimension-name"), null));
        List<String> loreLines = mm.getRawList("gui.send.dimension-lore");
        meta.lore(loreLines.stream().map(line -> mm.format(line, placeholders)).toList());

        item.setItemMeta(meta);
        return item;
    }

    private ItemStack buildPingItem(Player target) {
        ConfigManager cfg = plugin.getConfigManager();
        MessageManager mm = plugin.getMessageManager();

        Material material = cfg.parseMaterial("send-gui.ping-info.material", Material.CLOCK);
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();

        int ping = target.getPing();
        String color = ping <= 100 ? "&a" : ping <= 250 ? "&e" : "&c";
        Map<String, String> placeholders = Map.of("ping", color + ping + " ms");

        meta.displayName(mm.format(mm.getRaw("gui.send.ping-name"), null));
        List<String> loreLines = mm.getRawList("gui.send.ping-lore");
        meta.lore(loreLines.stream().map(line -> mm.format(line, placeholders)).toList());

        item.setItemMeta(meta);
        return item;
    }

    private String dimensionName(World.Environment environment) {
        MessageManager mm = plugin.getMessageManager();
        return switch (environment) {
            case NETHER -> mm.getRaw("dimension.nether");
            case THE_END -> mm.getRaw("dimension.the_end");
            default -> mm.getRaw("dimension.overworld");
        };
    }

    private ItemStack buildButtonItem(String materialPath, Material fallback, String namePath, String lorePath) {
        ConfigManager cfg = plugin.getConfigManager();
        MessageManager mm = plugin.getMessageManager();

        Material material = cfg.parseMaterial(materialPath, fallback);
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        meta.displayName(mm.format(mm.getRaw(namePath), null));

        List<String> loreLines = mm.getRawList(lorePath);
        meta.lore(loreLines.stream().map(line -> mm.format(line, (Map<String, String>) null)).toList());

        item.setItemMeta(meta);
        return item;
    }

    public void playClickSound(Player player) {
        ConfigManager cfg = plugin.getConfigManager();
        if (!cfg.getBoolean("send-gui.click-sound.enabled", true)) {
            return;
        }
        Sound sound = cfg.parseSound("send-gui.click-sound.sound", Sound.UI_BUTTON_CLICK);
        float volume = (float) cfg.getDouble("send-gui.click-sound.volume", 1.0);
        float pitch = (float) cfg.getDouble("send-gui.click-sound.pitch", 1.0);
        player.playSound(player.getLocation(), sound, volume, pitch);
    }
}
