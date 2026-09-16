package dev.stonetpa.plugin.command;

import dev.stonetpa.plugin.StoneTPA;
import dev.stonetpa.plugin.manager.MessageManager;
import dev.stonetpa.plugin.model.RequestType;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class TpaCommand implements CommandExecutor, TabCompleter {

    private final StoneTPA plugin;

    public TpaCommand(StoneTPA plugin) {
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

        if (args.length < 1) {
            mm.sendChat(player, "tpa.usage-tpa", null);
            return true;
        }

        Player target = Bukkit.getPlayerExact(args[0]);
        if (target == null) {
            mm.sendChat(player, "general.player-not-found", Map.of("player", args[0]));
            return true;
        }

        if (target.getUniqueId().equals(player.getUniqueId())) {
            mm.sendChat(player, "tpa.cannot-target-self", null);
            return true;
        }

        plugin.getSendRequestGuiManager().open(player, target, RequestType.TPA);
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            String partial = args[0].toLowerCase();
            return Bukkit.getOnlinePlayers().stream()
                    .map(Player::getName)
                    .filter(name -> !name.equalsIgnoreCase(sender.getName()))
                    .filter(name -> name.toLowerCase().startsWith(partial))
                    .collect(Collectors.toList());
        }
        return Collections.emptyList();
    }
}
