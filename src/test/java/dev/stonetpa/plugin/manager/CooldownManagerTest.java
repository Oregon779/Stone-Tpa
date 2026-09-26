package dev.stonetpa.plugin.manager;

import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CooldownManagerTest {

    @Test
    void freshPlayerHasNoCooldown() {
        CooldownManager manager = new CooldownManager();
        assertEquals(0, manager.getRemaining(UUID.randomUUID(), 10));
    }

    @Test
    void afterUseRemainingIsCloseToTheFullCooldown() {
        CooldownManager manager = new CooldownManager();
        UUID uuid = UUID.randomUUID();

        manager.setUsed(uuid);
        int remaining = manager.getRemaining(uuid, 10);

        // Allow a small tolerance instead of sleeping in the test - this
        // should be essentially instantaneous.
        assertTrue(remaining >= 9 && remaining <= 10,
                "expected ~10s remaining right after setUsed(), got " + remaining);
    }

    @Test
    void aZeroSecondCooldownIsAlwaysExpired() {
        CooldownManager manager = new CooldownManager();
        UUID uuid = UUID.randomUUID();

        manager.setUsed(uuid);

        assertEquals(0, manager.getRemaining(uuid, 0));
    }

    @Test
    void expiredCooldownEntryIsRemovedOnLookup() {
        CooldownManager manager = new CooldownManager();
        UUID uuid = UUID.randomUUID();

        // A cooldown that is already in the past (e.g. one that expired
        // between two lookups) must report 0 and clean itself up, not just
        // report a negative/garbage value.
        manager.setUsed(uuid);
        assertEquals(0, manager.getRemaining(uuid, -5));
        // A second lookup after the lazy cleanup must behave exactly like a
        // player who never used the command at all.
        assertEquals(0, manager.getRemaining(uuid, 10));
    }

    @Test
    void clearRemovesAPlayersEntryImmediately() {
        CooldownManager manager = new CooldownManager();
        UUID uuid = UUID.randomUUID();

        manager.setUsed(uuid);
        assertTrue(manager.getRemaining(uuid, 600) > 0);

        manager.clear(uuid);

        assertEquals(0, manager.getRemaining(uuid, 600));
    }

    @Test
    void differentPlayersDoNotShareState() {
        CooldownManager manager = new CooldownManager();
        UUID a = UUID.randomUUID();
        UUID b = UUID.randomUUID();

        manager.setUsed(a);

        assertTrue(manager.getRemaining(a, 30) > 0);
        assertEquals(0, manager.getRemaining(b, 30));
    }
}
