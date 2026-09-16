package dev.stonetpa.plugin.model;

import org.bukkit.Location;
import org.bukkit.scheduler.BukkitTask;

import java.util.UUID;

public class PendingTeleport {

    private final UUID playerId;
    private final Location destination;
    private final TeleportContext context;
    private BukkitTask task;

    public PendingTeleport(UUID playerId, Location destination, TeleportContext context) {
        this.playerId = playerId;
        this.destination = destination;
        this.context = context;
    }

    public UUID getPlayerId() {
        return playerId;
    }

    public Location getDestination() {
        return destination;
    }

    public TeleportContext getContext() {
        return context;
    }

    public BukkitTask getTask() {
        return task;
    }

    public void setTask(BukkitTask task) {
        this.task = task;
    }
}
