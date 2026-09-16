package dev.stonetpa.plugin.command;

import dev.stonetpa.plugin.StoneTPA;
import dev.stonetpa.plugin.manager.MessageManager;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

public class TpAcceptCommand implements CommandExecutor, TabCompleter {

    private final StoneTPA plugin;

    public TpAcceptCommand(StoneTPA plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        MessageManager mm = plugin.getMessageManager();

        if (!(sender instanceof Player player)) {
            mm.sendChat(sender, "general.player-only", null);
            return true;
        }

        if (!player.hasPermission("stonetpa.use")) {
            mm.sendChat(player, "general.no-permission", null);
            return true;
        }

        String specifiedSender = args.length > 0 ? args[0] : null;
        plugin.getRequestManager().accept(player, specifiedSender);
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1 && sender instanceof Player player) {
            String partial = args[0].toLowerCase();
            return plugin.getRequestManager().getIncomingSenderNames(player.getUniqueId()).stream()
                    .filter(name -> name.toLowerCase().startsWith(partial))
                    .collect(Collectors.toList());
        }
        return Collections.emptyList();
    }
}
