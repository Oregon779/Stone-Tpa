package dev.stonetpa.plugin;

import dev.stonetpa.plugin.command.StoneTPACommand;
import dev.stonetpa.plugin.command.TpAcceptCommand;
import dev.stonetpa.plugin.command.TpDenyCommand;
import dev.stonetpa.plugin.command.TpaCancelCommand;
import dev.stonetpa.plugin.command.TpaCommand;
import dev.stonetpa.plugin.command.TpaHereCommand;
import dev.stonetpa.plugin.command.TpaSettingsCommand;
import dev.stonetpa.plugin.command.TpToggleCommand;
import dev.stonetpa.plugin.listener.FallDamageListener;
import dev.stonetpa.plugin.listener.PlayerQuitListener;
import dev.stonetpa.plugin.listener.SendRequestGuiListener;
import dev.stonetpa.plugin.listener.SettingsGuiListener;
import dev.stonetpa.plugin.listener.TeleportMoveListener;
import dev.stonetpa.plugin.manager.BedrockDetector;
import dev.stonetpa.plugin.manager.ConfigManager;
import dev.stonetpa.plugin.manager.CooldownManager;
import dev.stonetpa.plugin.manager.EffectManager;
import dev.stonetpa.plugin.manager.MessageManager;
import dev.stonetpa.plugin.manager.NotificationManager;
import dev.stonetpa.plugin.manager.PlayerSettingsManager;
import dev.stonetpa.plugin.manager.RequestManager;
import dev.stonetpa.plugin.manager.SendRequestGuiManager;
import dev.stonetpa.plugin.manager.SettingsGuiManager;
import dev.stonetpa.plugin.manager.TeleportManager;
import dev.stonetpa.plugin.manager.UpdateChecker;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;

public final class StoneTPA extends JavaPlugin {

    private ConfigManager configManager;
    private MessageManager messageManager;
    private CooldownManager cooldownManager;
    private PlayerSettingsManager playerSettingsManager;
    private RequestManager requestManager;
    private TeleportManager teleportManager;
    private NotificationManager notificationManager;
    private EffectManager effectManager;
    private SettingsGuiManager settingsGuiManager;
    private SendRequestGuiManager sendRequestGuiManager;
    private UpdateChecker updateChecker;
    private BedrockDetector bedrockDetector;

    @Override
    public void onEnable() {
        configManager = new ConfigManager(this);
        configManager.load();

        messageManager = new MessageManager(this);
        messageManager.load();

        cooldownManager = new CooldownManager();
        playerSettingsManager = new PlayerSettingsManager(this);
        playerSettingsManager.load();

        notificationManager = new NotificationManager(this);
        effectManager = new EffectManager(this);
        teleportManager = new TeleportManager(this);
        settingsGuiManager = new SettingsGuiManager(this);
        sendRequestGuiManager = new SendRequestGuiManager(this);
        requestManager = new RequestManager(this);
        updateChecker = new UpdateChecker(this);
        bedrockDetector = new BedrockDetector(this);

        registerCommands();
        registerListeners();
        updateChecker.start();

        getLogger().info("Config loaded (" + configManager.getLanguage() + ") - commands, listeners"
                + " and update checker ready.");
    }

    @Override
    public void onDisable() {
        if (updateChecker != null) {
            updateChecker.stop();
        }
        getLogger().info("StoneTPA has been disabled.");
    }

    public void reload() {
        configManager.reload();
        messageManager.load();
    }

    private void registerCommands() {
        TpaCommand tpaCommand = new TpaCommand(this);
        getCommand("tpa").setExecutor(tpaCommand);
        getCommand("tpa").setTabCompleter(tpaCommand);

        TpaHereCommand tpaHereCommand = new TpaHereCommand(this);
        getCommand("tpahere").setExecutor(tpaHereCommand);
        getCommand("tpahere").setTabCompleter(tpaHereCommand);

        TpAcceptCommand tpAcceptCommand = new TpAcceptCommand(this);
        getCommand("tpaccept").setExecutor(tpAcceptCommand);
        getCommand("tpaccept").setTabCompleter(tpAcceptCommand);

        TpDenyCommand tpDenyCommand = new TpDenyCommand(this);
        getCommand("tpdeny").setExecutor(tpDenyCommand);
        getCommand("tpdeny").setTabCompleter(tpDenyCommand);

        getCommand("tpacancel").setExecutor(new TpaCancelCommand(this));
        getCommand("tptoggle").setExecutor(new TpToggleCommand(this));
        getCommand("tpasettings").setExecutor(new TpaSettingsCommand(this));

        StoneTPACommand stoneTPACommand = new StoneTPACommand(this);
        getCommand("stonetpa").setExecutor(stoneTPACommand);
        getCommand("stonetpa").setTabCompleter(stoneTPACommand);
    }

    private void registerListeners() {
        PluginManager pm = getServer().getPluginManager();
        pm.registerEvents(new TeleportMoveListener(this), this);
        pm.registerEvents(new FallDamageListener(this), this);
        pm.registerEvents(new PlayerQuitListener(this), this);
        pm.registerEvents(new SettingsGuiListener(this), this);
        pm.registerEvents(new SendRequestGuiListener(this), this);
        pm.registerEvents(updateChecker, this);
    }

    public ConfigManager getConfigManager() {
        return configManager;
    }

    public MessageManager getMessageManager() {
        return messageManager;
    }

    public CooldownManager getCooldownManager() {
        return cooldownManager;
    }

    public PlayerSettingsManager getPlayerSettingsManager() {
        return playerSettingsManager;
    }

    public RequestManager getRequestManager() {
        return requestManager;
    }

    public TeleportManager getTeleportManager() {
        return teleportManager;
    }

    public NotificationManager getNotificationManager() {
        return notificationManager;
    }

    public EffectManager getEffectManager() {
        return effectManager;
    }

    public SettingsGuiManager getSettingsGuiManager() {
        return settingsGuiManager;
    }

    public SendRequestGuiManager getSendRequestGuiManager() {
        return sendRequestGuiManager;
    }

    public UpdateChecker getUpdateChecker() {
        return updateChecker;
    }

    public BedrockDetector getBedrockDetector() {
        return bedrockDetector;
    }
}
