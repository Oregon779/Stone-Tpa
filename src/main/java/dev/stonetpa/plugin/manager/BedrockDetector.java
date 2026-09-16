package dev.stonetpa.plugin.manager;

import dev.stonetpa.plugin.StoneTPA;
import org.bukkit.entity.Player;

import java.lang.reflect.Method;
import java.util.UUID;

public class BedrockDetector {

    private final StoneTPA plugin;
    private boolean floodgateAvailable;
    private Object floodgateApiInstance;
    private Method isFloodgatePlayerMethod;

    public BedrockDetector(StoneTPA plugin) {
        this.plugin = plugin;
        setup();
    }

    private void setup() {
        try {
            Class<?> apiClass = Class.forName("org.geysermc.floodgate.api.FloodgateApi");
            Method getInstance = apiClass.getMethod("getInstance");
            floodgateApiInstance = getInstance.invoke(null);
            isFloodgatePlayerMethod = apiClass.getMethod("isFloodgatePlayer", UUID.class);
            floodgateAvailable = true;
            plugin.getLogger().info("Floodgate detected - Bedrock players will get a command-only request message.");
        } catch (Throwable t) {
            floodgateAvailable = false;
        }
    }

    public boolean isBedrockPlayer(Player player) {
        if (!floodgateAvailable) {
            return false;
        }
        try {
            Object result = isFloodgatePlayerMethod.invoke(floodgateApiInstance, player.getUniqueId());
            return Boolean.TRUE.equals(result);
        } catch (Throwable t) {

            return false;
        }
    }
}
