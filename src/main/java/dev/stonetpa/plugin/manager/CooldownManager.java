package dev.stonetpa.plugin.manager;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class CooldownManager {

    private final Map<UUID, Long> lastUse = new HashMap<>();

    public int getRemaining(UUID uuid, int cooldownSeconds) {
        Long last = lastUse.get(uuid);
        if (last == null) {
            return 0;
        }
        long elapsedSeconds = (System.currentTimeMillis() - last) / 1000L;
        long remaining = cooldownSeconds - elapsedSeconds;
        if (remaining <= 0) {
            lastUse.remove(uuid);
            return 0;
        }
        return (int) remaining;
    }

    public void setUsed(UUID uuid) {
        lastUse.put(uuid, System.currentTimeMillis());
    }

    /**
     * Drops a player's cooldown entry. Without this, every player who ever
     * sent a single request leaves one UUID -> timestamp entry behind
     * forever - getRemaining() only cleans up an entry once someone looks it
     * up again, which never happens for a player who left for good.
     */
    public void clear(UUID uuid) {
        lastUse.remove(uuid);
    }
}
