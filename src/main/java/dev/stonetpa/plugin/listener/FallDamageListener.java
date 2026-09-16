package dev.stonetpa.plugin.listener;

import dev.stonetpa.plugin.StoneTPA;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;

public class FallDamageListener implements Listener {

    private final StoneTPA plugin;

    public FallDamageListener(StoneTPA plugin) {
        this.plugin = plugin;
    }

    // ignoreCancelled=true: EntityDamageEvent fires for every entity on the
    // server (mobs included, not just players) on every damage tick - at
    // 250+ players plus their surrounding mobs that's a very high-frequency
    // event. Skipping already-cancelled events (e.g. creative mode, another
    // plugin's invulnerability logic) avoids the lookup below for cases
    // where the outcome is already decided.
    @EventHandler(ignoreCancelled = true)
    public void onDamage(EntityDamageEvent event) {
        // Cheapest possible check first (enum comparison, no allocation) -
        // this alone discards the vast majority of calls, since most damage
        // isn't fall damage at all.
        if (event.getCause() != EntityDamageEvent.DamageCause.FALL) {
            return;
        }
        if (!(event.getEntity() instanceof Player player)) {
            return;
        }
        if (plugin.getTeleportManager().isFallDamageImmune(player.getUniqueId())) {
            event.setCancelled(true);
        }
    }
}
