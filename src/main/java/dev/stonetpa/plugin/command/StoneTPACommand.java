package dev.stonetpa.plugin.command;

import dev.stonetpa.plugin.StoneTPA;
import dev.stonetpa.plugin.manager.MessageManager;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class StoneTPACommand implements CommandExecutor, TabCompleter {

    private final StoneTPA plugin;

    public StoneTPACommand(StoneTPA plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0) {
            sendHelp(sender);
            return true;
        }

        switch (args[0].toLowerCase()) {
            case "reload" -> {
                if (!requireAdmin(sender)) {
                    return true;
                }
                plugin.reload();
                plugin.getMessageManager().sendChat(sender, "general.reload-success", null);
            }
            case "checkupdate" -> {
                if (!requireAdmin(sender)) {
                    return true;
                }
                plugin.getUpdateChecker().checkNow();
                plugin.getMessageManager().sendChat(sender, "update.check-triggered", null);
            }
            case "help" -> sendHelp(sender);
            default -> sendHelp(sender);
        }
        return true;
    }

    private boolean requireAdmin(CommandSender sender) {
        if (sender.hasPermission("stonetpa.admin")) {
            return true;
        }
        plugin.getMessageManager().sendChat(sender, "general.no-permission", null);
        return false;
    }

    private void sendHelp(CommandSender sender) {
        MessageManager mm = plugin.getMessageManager();
        boolean isAdmin = sender.hasPermission("stonetpa.admin");
        boolean canUse = sender.hasPermission("stonetpa.use");

        mm.sendRaw(sender, "help.header", null);
        if (canUse) {
            mm.sendRaw(sender, "help.tpa", null);
            mm.sendRaw(sender, "help.tpahere", null);
            mm.sendRaw(sender, "help.tpaccept", null);
            mm.sendRaw(sender, "help.tpdeny", null);
            mm.sendRaw(sender, "help.tpacancel", null);
            mm.sendRaw(sender, "help.tptoggle", null);
            mm.sendRaw(sender, "help.tpasettings", null);
        }
        if (isAdmin) {
            mm.sendRaw(sender, "help.reload", null);
            mm.sendRaw(sender, "help.checkupdate", null);
        }
        mm.sendRaw(sender, "help.help", null);
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            String partial = args[0].toLowerCase();
            return Stream.of("reload", "help", "checkupdate")
                    .filter(s -> s.startsWith(partial))
                    .collect(Collectors.toList());
        }
        return Collections.emptyList();
    }
}
