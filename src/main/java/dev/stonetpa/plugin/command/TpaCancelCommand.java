package dev.stonetpa.plugin.command;

import dev.stonetpa.plugin.StoneTPA;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class TpaCancelCommand implements CommandExecutor {

    private final StoneTPA plugin;

    public TpaCancelCommand(StoneTPA plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            plugin.getMessageManager().sendChat(sender, "general.player-only", null);
            return true;
        }

        if (!player.hasPermission("stonetpa.use")) {
            plugin.getMessageManager().sendChat(player, "general.no-permission", null);
            return true;
        }

        plugin.getRequestManager().cancel(player);
        return true;
    }
}
