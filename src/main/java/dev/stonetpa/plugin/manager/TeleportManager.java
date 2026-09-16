package dev.stonetpa.plugin.manager;

import dev.stonetpa.plugin.StoneTPA;
import dev.stonetpa.plugin.model.PendingTeleport;
import dev.stonetpa.plugin.model.TeleportContext;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Owns the teleport delay/cancellation state machine. The cinematic
 * countdown sequence itself lives entirely in EffectManager, which calls
 * back here exactly when the jump should actually happen - this class only
 * tracks pending teleports, sends the once-per-second notification text,
 * and performs the actual Bukkit teleport safely on the main thread.
 */
public class TeleportManager {

    private final StoneTPA plugin;
    private final Map<UUID, PendingTeleport> pending = new HashMap<>();
    private final Set<UUID> fallDamageImmune = new HashSet<>();

    public TeleportManager(StoneTPA plugin) {
        this.plugin = plugin;
    }

    public boolean hasPending(UUID uuid) {
        return pending.containsKey(uuid);
    }

    public PendingTeleport getPending(UUID uuid) {
        return pending.get(uuid);
    }

    public void startTeleport(Player player, Location destination, TeleportContext context) {
        ConfigManager cfg = plugin.getConfigManager();
        int delay = cfg.getTeleportDelaySeconds();
        boolean bypassDelay = player.hasPermission("stonetpa.bypass.delay");

        if (delay <= 0 || bypassDelay) {
            executeTeleport(player, destination, context);
            return;
        }

        if (pending.containsKey(player.getUniqueId())) {
            plugin.getMessageManager().sendChat(player, "tpa.already-teleporting", null);
            return;
        }

        boolean blindness = cfg.getBoolean("teleport.blindness-during-delay", false);
        if (blindness) {
            player.addPotionEffect(new PotionEffect(PotionEffectType.BLINDNESS, (delay + 1) * 20, 1, false, false, false));
        }

        PendingTeleport pendingTeleport = new PendingTeleport(player.getUniqueId(), destination, context);
        pending.put(player.getUniqueId(), pendingTeleport);

        plugin.getNotificationManager().sendCountdown(player, delay, context.otherPartyName());

        // EffectManager owns the entire cinematic sequence and only calls
        // us back once the jump should actually happen.
        plugin.getEffectManager().startCountdown(player, delay, () -> {
            pending.remove(player.getUniqueId());
            plugin.getNotificationManager().clearBossBar(player);
            executeTeleport(player, destination, context);
        });

        BukkitTask messageTask = new BukkitRunnable() {
            int remaining = delay;

            @Override
            public void run() {
                remaining--;

                if (!pending.containsKey(player.getUniqueId())) {
                    // Either cancelled, or already completed by EffectManager's callback.
                    cancel();
                    return;
                }

                if (!player.isOnline()) {
                    pending.remove(player.getUniqueId());
                    plugin.getEffectManager().stopCountdown(player.getUniqueId());
                    plugin.getNotificationManager().clearBossBar(player);
                    cancel();
                    return;
                }

                if (remaining <= 0) {
                    // The actual jump is triggered by EffectManager's onComplete
                    // callback once its own sequence finishes - this task's only
                    // job was sending countdown text, so it's done.
                    cancel();
                    return;
                }

                try {
                    plugin.getNotificationManager().sendCountdown(player, remaining, context.otherPartyName());
                } catch (Exception ex) {
                    plugin.getLogger().warning("Error while sending countdown notification for "
                            + player.getName() + ": " + ex);
                }
            }
        }.runTaskTimer(plugin, 20L, 20L);

        pendingTeleport.setTask(messageTask);
    }

    public void cancelTeleport(Player player, boolean silent) {
        PendingTeleport pendingTeleport = pending.remove(player.getUniqueId());
        if (pendingTeleport == null) {
            return;
        }
        if (pendingTeleport.getTask() != null) {
            pendingTeleport.getTask().cancel();
        }
        plugin.getEffectManager().stopCountdown(player.getUniqueId());
        plugin.getNotificationManager().clearBossBar(player);
        player.removePotionEffect(PotionEffectType.BLINDNESS);
        if (!silent) {
            plugin.getMessageManager().sendChat(player, "tpa.cancelled-move", null);
        }
    }

    public void cancelForDisconnectedOtherParty(UUID otherPartyId) {
        for (PendingTeleport pendingTeleport : new java.util.ArrayList<>(pending.values())) {
            if (otherPartyId.equals(pendingTeleport.getContext().otherPartyId())) {
                Player player = Bukkit.getPlayer(pendingTeleport.getPlayerId());
                if (player != null) {
                    pending.remove(player.getUniqueId());
                    if (pendingTeleport.getTask() != null) {
                        pendingTeleport.getTask().cancel();
                    }
                    plugin.getEffectManager().stopCountdown(player.getUniqueId());
                    plugin.getNotificationManager().clearBossBar(player);
                    player.removePotionEffect(PotionEffectType.BLINDNESS);
                    plugin.getMessageManager().sendChat(player, "tpa.cancelled-target-left", null);
                }
            }
        }
    }

    private void executeTeleport(Player player, Location destination, TeleportContext context) {
        ConfigManager cfg = plugin.getConfigManager();

        if (cfg.getBoolean("teleport.disable-fall-damage", true)) {
            fallDamageImmune.add(player.getUniqueId());
            Bukkit.getScheduler().runTaskLater(plugin, () -> fallDamageImmune.remove(player.getUniqueId()), 100L);
        }

        // teleportAsync keeps chunk loading off the main thread; Paper
        // guarantees the completion of this future runs the actual
        // teleport back on the main thread.
        player.teleportAsync(destination).thenAccept(success -> {
            if (!Boolean.TRUE.equals(success)) {
                return;
            }
            Bukkit.getScheduler().runTask(plugin, () -> {
                try {
                    player.removePotionEffect(PotionEffectType.BLINDNESS);
                    plugin.getEffectManager().playArrivalEffect(player);
                    plugin.getNotificationManager().sendArrival(player, context.otherPartyName());
                    context.onComplete(player);
                } catch (Exception ex) {
                    plugin.getLogger().warning("Error while playing arrival effects for "
                            + player.getName() + ": " + ex);
                }
            });
        }).exceptionally(ex -> {
            plugin.getLogger().warning("Teleport for " + player.getName() + " failed: " + ex);
            return null;
        });
    }

    public boolean isFallDamageImmune(UUID uuid) {
        return fallDamageImmune.contains(uuid);
    }
}
