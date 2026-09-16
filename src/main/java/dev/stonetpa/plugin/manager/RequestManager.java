package dev.stonetpa.plugin.manager;

import dev.stonetpa.plugin.StoneTPA;
import dev.stonetpa.plugin.model.RequestType;
import dev.stonetpa.plugin.model.TeleportContext;
import dev.stonetpa.plugin.model.TeleportRequest;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class RequestManager {

    private final StoneTPA plugin;

    private final Map<UUID, TeleportRequest> outgoingBySender = new HashMap<>();
    private final Map<UUID, List<TeleportRequest>> incomingByTarget = new HashMap<>();

    public RequestManager(StoneTPA plugin) {
        this.plugin = plugin;
    }

    public void sendRequest(Player sender, Player target, RequestType type) {
        MessageManager mm = plugin.getMessageManager();
        ConfigManager cfg = plugin.getConfigManager();

        if (sender.getUniqueId().equals(target.getUniqueId())) {
            mm.sendChat(sender, "tpa.cannot-target-self", null);
            return;
        }

        if (!plugin.getPlayerSettingsManager().isAcceptingRequests(target.getUniqueId())
                && !sender.hasPermission("stonetpa.bypass.toggle")) {
            mm.sendChat(sender, "tpa.target-requests-disabled", Map.of("player", target.getName()));
            return;
        }

        if (!sender.hasPermission("stonetpa.bypass.cooldown")) {
            int cooldownSeconds = cfg.getRequestCooldownSeconds();
            int remaining = plugin.getCooldownManager().getRemaining(sender.getUniqueId(), cooldownSeconds);
            if (remaining > 0) {
                mm.sendChat(sender, "tpa.cooldown", Map.of("seconds", String.valueOf(remaining)));
                return;
            }
        }

        TeleportRequest existingOutgoing = outgoingBySender.get(sender.getUniqueId());
        if (existingOutgoing != null) {
            mm.sendChat(sender, "tpa.already-have-outgoing", Map.of("player", existingOutgoing.getTargetName()));
            return;
        }

        plugin.getCooldownManager().setUsed(sender.getUniqueId());

        int expireSeconds = cfg.getRequestExpireSeconds();
        TeleportRequest request = new TeleportRequest(sender.getUniqueId(), sender.getName(),
                target.getUniqueId(), target.getName(), type, expireSeconds);

        outgoingBySender.put(sender.getUniqueId(), request);
        incomingByTarget.computeIfAbsent(target.getUniqueId(), id -> new ArrayList<>()).add(request);

        request.setExpireTask(Bukkit.getScheduler().runTaskLater(plugin,
                () -> expireRequest(request), expireSeconds * 20L));

        Map<String, String> senderPlaceholders = Map.of("player", target.getName(), "seconds", String.valueOf(expireSeconds));
        mm.sendChat(sender, type == RequestType.TPA ? "tpa.sent-tpa" : "tpa.sent-tpahere", senderPlaceholders);

        Map<String, String> targetPlaceholders = Map.of("player", sender.getName(), "seconds", String.valueOf(expireSeconds));
        mm.sendRaw(target, type == RequestType.TPA ? "tpa.request-received-tpa" : "tpa.request-received-tpahere", targetPlaceholders);

        if (plugin.getBedrockDetector().isBedrockPlayer(target)) {

            mm.sendRaw(target, "tpa.request-actions-bedrock", targetPlaceholders);
        } else {
            mm.sendRaw(target, "tpa.request-actions", targetPlaceholders);
        }
    }

    public void accept(Player accepter, String specifiedSenderName) {
        MessageManager mm = plugin.getMessageManager();
        TeleportRequest request = resolveIncoming(accepter, specifiedSenderName, true);
        if (request == null) {
            return;
        }

        removeRequest(request);

        Player sender = Bukkit.getPlayer(request.getSenderId());
        if (sender == null || !sender.isOnline()) {
            mm.sendChat(accepter, "tpa.target-offline", null);
            return;
        }

        mm.sendChat(accepter, "tpa.accepted-notify-target", Map.of("player", sender.getName()));
        mm.sendChat(sender, "tpa.accepted-notify-sender", Map.of("player", accepter.getName()));

        if (request.getType() == RequestType.TPA) {
            plugin.getTeleportManager().startTeleport(sender, accepter.getLocation(),
                    TeleportContext.of(accepter.getUniqueId(), accepter.getName()));
        } else {
            plugin.getTeleportManager().startTeleport(accepter, sender.getLocation(),
                    TeleportContext.of(sender.getUniqueId(), sender.getName()));
        }
    }

    public void deny(Player accepter, String specifiedSenderName) {
        MessageManager mm = plugin.getMessageManager();
        TeleportRequest request = resolveIncoming(accepter, specifiedSenderName, false);
        if (request == null) {
            return;
        }

        removeRequest(request);

        mm.sendChat(accepter, "tpa.denied-notify-target", Map.of("player", request.getSenderName()));
        Player sender = Bukkit.getPlayer(request.getSenderId());
        if (sender != null && sender.isOnline()) {
            mm.sendChat(sender, "tpa.denied-notify-sender", Map.of("player", accepter.getName()));
        }
    }

    public void cancel(Player sender) {
        MessageManager mm = plugin.getMessageManager();
        TeleportRequest request = outgoingBySender.get(sender.getUniqueId());
        if (request == null) {
            mm.sendChat(sender, "tpa.no-pending-outgoing", null);
            return;
        }

        removeRequest(request);

        mm.sendChat(sender, "tpa.cancelled-notify-sender", Map.of("player", request.getTargetName()));
        Player target = Bukkit.getPlayer(request.getTargetId());
        if (target != null && target.isOnline()) {
            mm.sendChat(target, "tpa.cancelled-notify-target", Map.of("player", sender.getName()));
        }
    }

    public java.util.List<String> getIncomingSenderNames(UUID accepterId) {
        List<TeleportRequest> incoming = incomingByTarget.get(accepterId);
        if (incoming == null || incoming.isEmpty()) {
            return java.util.List.of();
        }
        return incoming.stream().map(TeleportRequest::getSenderName).toList();
    }

    private TeleportRequest resolveIncoming(Player accepter, String specifiedSenderName, boolean forAccept) {
        MessageManager mm = plugin.getMessageManager();
        List<TeleportRequest> incoming = incomingByTarget.get(accepter.getUniqueId());

        if (incoming == null || incoming.isEmpty()) {
            mm.sendChat(accepter, forAccept ? "tpa.no-pending-incoming" : "tpa.no-pending-incoming-deny", null);
            return null;
        }

        if (specifiedSenderName != null && !specifiedSenderName.isBlank()) {
            for (TeleportRequest request : incoming) {
                if (request.getSenderName().equalsIgnoreCase(specifiedSenderName)) {
                    return request;
                }
            }
            mm.sendChat(accepter, "tpa.no-pending-incoming-named", Map.of("player", specifiedSenderName));
            return null;
        }

        if (incoming.size() > 1) {
            mm.sendChat(accepter, "tpa.multiple-pending-specify", null);
            return null;
        }

        return incoming.get(0);
    }

    private void removeRequest(TeleportRequest request) {
        request.cancelExpireTask();
        outgoingBySender.remove(request.getSenderId());

        List<TeleportRequest> incoming = incomingByTarget.get(request.getTargetId());
        if (incoming != null) {
            incoming.remove(request);
            if (incoming.isEmpty()) {
                incomingByTarget.remove(request.getTargetId());
            }
        }
    }

    private void expireRequest(TeleportRequest request) {
        removeRequest(request);

        MessageManager mm = plugin.getMessageManager();
        Player sender = Bukkit.getPlayer(request.getSenderId());
        if (sender != null && sender.isOnline()) {
            mm.sendChat(sender, "tpa.expired-notify-sender", Map.of("player", request.getTargetName()));
        }
        Player target = Bukkit.getPlayer(request.getTargetId());
        if (target != null && target.isOnline()) {
            mm.sendChat(target, "tpa.expired-notify-target", Map.of("player", request.getSenderName()));
        }
    }

    public void handleQuit(Player player) {
        UUID uuid = player.getUniqueId();

        TeleportRequest outgoing = outgoingBySender.get(uuid);
        if (outgoing != null) {
            removeRequest(outgoing);
        }

        List<TeleportRequest> incoming = new ArrayList<>(incomingByTarget.getOrDefault(uuid, List.of()));
        for (TeleportRequest request : incoming) {
            removeRequest(request);
        }
    }
}
