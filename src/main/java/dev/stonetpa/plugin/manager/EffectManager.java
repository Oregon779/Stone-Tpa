package dev.stonetpa.plugin.manager;

import dev.stonetpa.plugin.StoneTPA;
import org.bukkit.Bukkit;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * A single double-helix spiral of colored particles wrapping around the
 * player for the whole countdown, nothing else - then a small burst and
 * sound the instant they teleport. No arrival effect at the destination by
 * default. Configurable in config.yml under teleport.countdown.*.
 */
public class EffectManager {

    private final StoneTPA plugin;
    private final Map<UUID, BukkitTask> countdownTasks = new HashMap<>();
    private final java.util.Set<Particle> warnedParticles = ConcurrentHashMap.newKeySet();

    public EffectManager(StoneTPA plugin) {
        this.plugin = plugin;
    }

    private boolean effectsEnabledFor(Player player) {
        return plugin.getPlayerSettingsManager().isEffectsEnabled(player.getUniqueId());
    }

    public void startCountdown(Player player, int totalSeconds, Runnable onComplete) {
        stopCountdown(player.getUniqueId());

        if (!effectsEnabledFor(player)) {
            BukkitTask task = Bukkit.getScheduler().runTaskLater(plugin, onComplete, (long) totalSeconds * 20L);
            countdownTasks.put(player.getUniqueId(), task);
            return;
        }

        ConfigManager cfg = plugin.getConfigManager();
        SpiralSettings s = new SpiralSettings(cfg);
        int totalTicks = Math.max(1, totalSeconds * 20);

        BukkitTask task = new BukkitRunnable() {
            int elapsed = 0;
            double angle = 0.0;
            final Location point = player.getLocation();

            @Override
            public void run() {
                if (!player.isOnline()) {
                    countdownTasks.remove(player.getUniqueId());
                    cancel();
                    return;
                }

                Location base = player.getLocation();
                double y = s.height * ((elapsed % s.cycleTicks) / (double) s.cycleTicks);
                double progress = y / s.height;
                Color color = lerpColor(s.colorBottom, s.colorTop, progress);
                Particle.DustOptions dust = new Particle.DustOptions(color, s.dustSize);
                double angleRad = Math.toRadians(angle);

                spawnDust(player, base, point, angleRad, s.radius, y, dust);
                spawnDust(player, base, point, angleRad + Math.PI, s.radius, y, dust);

                if (elapsed % 20 == 0) {
                    float pitch = s.pitchStart + (s.pitchEnd - s.pitchStart) * (float) (elapsed / (double) totalTicks);
                    player.playSound(base, s.sound, s.soundVolume, pitch);
                    player.playSound(base, s.tickSound, s.tickVolume, pitch);
                }

                angle = (angle + s.rotationSpeed) % 360.0;
                elapsed++;

                if (elapsed >= totalTicks) {
                    playDeparture(player, base, s);
                    countdownTasks.remove(player.getUniqueId());
                    cancel();
                    onComplete.run();
                }
            }
        }.runTaskTimer(plugin, 0L, 1L);

        countdownTasks.put(player.getUniqueId(), task);
    }

    public void stopCountdown(UUID playerId) {
        BukkitTask task = countdownTasks.remove(playerId);
        if (task != null) {
            task.cancel();
        }
    }

    private void playDeparture(Player player, Location base, SpiralSettings s) {
        player.playSound(base, s.departureSound, s.soundVolume, 1.0f);
        // FLASH is deliberately avoided here - it requires extra data this
        // server doesn't support (see plugin log history).
        safeSpawnParticle(player, s.departureParticle, base.clone().add(0, 1.1, 0), s.departureCount, 0.4, 0.5, 0.4, 0.1);
    }

    // ==================================================================
    // Arrival - disabled by default; no show at the destination.
    // ==================================================================

    public void playArrivalEffect(Player player) {
        playArrivalEffect(player, null);
    }

    public void playArrivalEffect(Player player, Player witness) {
        ConfigManager cfg = plugin.getConfigManager();
        if (!cfg.getBoolean("teleport.arrival.enabled", false)) {
            return;
        }
        if (!effectsEnabledFor(player)) {
            return;
        }
        Sound sound = cfg.parseSound("teleport.arrival.sound", Sound.ITEM_TOTEM_USE);
        float volume = (float) cfg.getDouble("teleport.arrival.sound-volume", 0.6);
        player.playSound(player.getLocation(), sound, volume, 1.0f);
    }

    // ==================================================================
    // Shared helpers
    // ==================================================================

    private Color lerpColor(Color a, Color b, double t) {
        double clamped = Math.max(0.0, Math.min(1.0, t));
        int r = (int) Math.round(a.getRed() + (b.getRed() - a.getRed()) * clamped);
        int g = (int) Math.round(a.getGreen() + (b.getGreen() - a.getGreen()) * clamped);
        int bl = (int) Math.round(a.getBlue() + (b.getBlue() - a.getBlue()) * clamped);
        return Color.fromRGB(r, g, bl);
    }

    private void spawnDust(Player player, Location base, Location reusable, double angleRad, double radius,
                            double yOffset, Particle.DustOptions dust) {
        double x = Math.cos(angleRad) * radius;
        double z = Math.sin(angleRad) * radius;
        reusable.setWorld(base.getWorld());
        reusable.setX(base.getX() + x);
        reusable.setY(base.getY() + yOffset);
        reusable.setZ(base.getZ() + z);
        if (warnedParticles.contains(Particle.DUST)) {
            return;
        }
        try {
            player.spawnParticle(Particle.DUST, reusable, 1, 0, 0, 0, 0, dust);
        } catch (Exception ex) {
            if (warnedParticles.add(Particle.DUST)) {
                plugin.getLogger().warning("Colored dust particles could not be spawned on this server version ("
                        + ex.getMessage() + ") - skipping from now on.");
            }
        }
    }

    private void safeSpawnParticle(Player player, Particle particle, Location location, int count,
                                    double offsetX, double offsetY, double offsetZ, double extra) {
        if (warnedParticles.contains(particle)) {
            return;
        }
        try {
            player.spawnParticle(particle, location, count, offsetX, offsetY, offsetZ, extra);
        } catch (Exception ex) {
            if (warnedParticles.add(particle)) {
                plugin.getLogger().warning("Particle " + particle + " could not be spawned on this server "
                        + "version (" + ex.getMessage() + ") - skipping it from now on. Pick a different "
                        + "particle for it in config.yml.");
            }
        }
    }

    private Particle parseParticle(String name, Particle fallback) {
        try {
            return Particle.valueOf(name.toUpperCase());
        } catch (IllegalArgumentException ex) {
            plugin.getLogger().warning("Invalid particle '" + name + "' in config.yml, using " + fallback + " instead.");
            return fallback;
        }
    }

    // ==================================================================
    // All config values for one countdown run, read once at start.
    // ==================================================================

    private final class SpiralSettings {

        final double radius, height;
        final int cycleTicks;
        final double rotationSpeed;
        final float dustSize;
        final Color colorBottom, colorTop;

        final Sound sound;
        final float pitchStart, pitchEnd, soundVolume;
        final Sound tickSound;
        final float tickVolume;

        final Sound departureSound;
        final Particle departureParticle;
        final int departureCount;

        SpiralSettings(ConfigManager cfg) {
            radius = cfg.getDouble("teleport.countdown.spiral.radius", 0.5);
            height = cfg.getDouble("teleport.countdown.spiral.height", 2.2);
            cycleTicks = Math.max(1, cfg.getInt("teleport.countdown.spiral.cycle-ticks", 30));
            rotationSpeed = cfg.getDouble("teleport.countdown.spiral.rotation-speed-degrees", 45.0);
            dustSize = (float) cfg.getDouble("teleport.countdown.spiral.size", 1.1);
            colorBottom = cfg.parseColor("teleport.countdown.spiral.color-bottom", Color.fromRGB(0x00B4D8));
            colorTop = cfg.parseColor("teleport.countdown.spiral.color-top", Color.fromRGB(0x7B2CBF));

            sound = cfg.parseSound("teleport.countdown.spiral.sound", Sound.BLOCK_AMETHYST_BLOCK_CHIME);
            pitchStart = (float) cfg.getDouble("teleport.countdown.spiral.pitch-start", 0.8);
            pitchEnd = (float) cfg.getDouble("teleport.countdown.spiral.pitch-end", 1.6);
            soundVolume = (float) cfg.getDouble("teleport.countdown.spiral.sound-volume", 0.6);
            tickSound = cfg.parseSound("teleport.countdown.spiral.tick-sound", Sound.BLOCK_NOTE_BLOCK_PLING);
            tickVolume = (float) cfg.getDouble("teleport.countdown.spiral.tick-volume", 0.5);

            departureSound = cfg.parseSound("teleport.countdown.departure.sound", Sound.ITEM_TOTEM_USE);
            departureParticle = parseParticle(cfg.getString("teleport.countdown.departure.particle", "TOTEM_OF_UNDYING"), Particle.TOTEM_OF_UNDYING);
            departureCount = Math.max(1, cfg.getInt("teleport.countdown.departure.count", 30));
        }
    }
}
