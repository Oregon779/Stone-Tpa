package dev.stonetpa.plugin.model;

import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.jetbrains.annotations.NotNull;

import java.util.UUID;

public class SendGuiHolder implements InventoryHolder {

    private final UUID targetId;
    private final String targetName;
    private final RequestType type;
    private Inventory inventory;

    public SendGuiHolder(UUID targetId, String targetName, RequestType type) {
        this.targetId = targetId;
        this.targetName = targetName;
        this.type = type;
    }

    public UUID getTargetId() {
        return targetId;
    }

    public String getTargetName() {
        return targetName;
    }

    public RequestType getType() {
        return type;
    }

    @Override
    public @NotNull Inventory getInventory() {
        return inventory;
    }

    public void setInventory(Inventory inventory) {
        this.inventory = inventory;
    }
}
