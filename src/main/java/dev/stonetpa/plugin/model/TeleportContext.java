package dev.stonetpa.plugin.model;

import org.bukkit.entity.Player;

import java.util.UUID;
import java.util.function.Consumer;

public record TeleportContext(UUID otherPartyId, String otherPartyName, Consumer<Player> onCompleteCallback) {

    public static TeleportContext of(UUID otherPartyId, String otherPartyName) {
        return new TeleportContext(otherPartyId, otherPartyName, null);
    }

    public static TeleportContext of(UUID otherPartyId, String otherPartyName, Consumer<Player> onCompleteCallback) {
        return new TeleportContext(otherPartyId, otherPartyName, onCompleteCallback);
    }

    public void onComplete(Player player) {
        if (onCompleteCallback != null) {
            onCompleteCallback.accept(player);
        }
    }
}
