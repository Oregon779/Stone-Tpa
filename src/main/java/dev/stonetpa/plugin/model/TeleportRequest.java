package dev.stonetpa.plugin.model;

import org.bukkit.scheduler.BukkitTask;

import java.util.UUID;

public class TeleportRequest {

    private final UUID senderId;
    private final String senderName;
    private final UUID targetId;
    private final String targetName;
    private final RequestType type;
    private final int expireSeconds;
    private final long expiresAtMillis;
    private BukkitTask expireTask;

    public TeleportRequest(UUID senderId, String senderName, UUID targetId, String targetName,
                            RequestType type, int expireSeconds) {
        this.senderId = senderId;
        this.senderName = senderName;
        this.targetId = targetId;
        this.targetName = targetName;
        this.type = type;
        this.expireSeconds = expireSeconds;
        this.expiresAtMillis = System.currentTimeMillis() + (expireSeconds * 1000L);
    }

    public int getSecondsRemaining() {
        long remainingMillis = expiresAtMillis - System.currentTimeMillis();
        return (int) Math.max(0, Math.ceil(remainingMillis / 1000.0));
    }

    public UUID getSenderId() {
        return senderId;
    }

    public String getSenderName() {
        return senderName;
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

    public int getExpireSeconds() {
        return expireSeconds;
    }

    public BukkitTask getExpireTask() {
        return expireTask;
    }

    public void setExpireTask(BukkitTask expireTask) {
        this.expireTask = expireTask;
    }

    public void cancelExpireTask() {
        if (expireTask != null) {
            expireTask.cancel();
            expireTask = null;
        }
    }
}
