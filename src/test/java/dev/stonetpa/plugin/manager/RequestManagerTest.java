package dev.stonetpa.plugin.manager;

import dev.stonetpa.plugin.StoneTPA;
import dev.stonetpa.plugin.model.RequestType;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitScheduler;
import org.bukkit.scheduler.BukkitTask;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Real MockBukkit could not be used here (see the pom.xml comment on the
 * test dependencies), so Player/Bukkit are hand-mocked with plain Mockito.
 * This exercises exactly the risky bits of the request state machine called
 * out in the task: self-targeting, the accept-requests toggle and its
 * bypass permission, the cooldown and its bypass permission, the
 * one-outgoing-request-at-a-time guard, both accept directions (TPA vs.
 * TPA_HERE teleport the opposite player), multiple simultaneous incoming
 * requests needing a name, expiry, and PlayerQuitEvent cleanup.
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class RequestManagerTest {

    @Mock
    private StoneTPA plugin;
    @Mock
    private MessageManager messageManager;
    @Mock
    private ConfigManager configManager;
    @Mock
    private PlayerSettingsManager playerSettingsManager;
    @Mock
    private CooldownManager cooldownManager;
    @Mock
    private BedrockDetector bedrockDetector;
    @Mock
    private TeleportManager teleportManager;
    @Mock
    private BukkitScheduler scheduler;

    private MockedStatic<Bukkit> bukkitStatic;
    private RequestManager requestManager;

    @BeforeEach
    void setUp() {
        when(plugin.getMessageManager()).thenReturn(messageManager);
        when(plugin.getConfigManager()).thenReturn(configManager);
        when(plugin.getPlayerSettingsManager()).thenReturn(playerSettingsManager);
        when(plugin.getCooldownManager()).thenReturn(cooldownManager);
        when(plugin.getBedrockDetector()).thenReturn(bedrockDetector);
        when(plugin.getTeleportManager()).thenReturn(teleportManager);

        when(configManager.getRequestCooldownSeconds()).thenReturn(10);
        when(configManager.getRequestExpireSeconds()).thenReturn(60);
        when(playerSettingsManager.isAcceptingRequests(any())).thenReturn(true);
        when(bedrockDetector.isBedrockPlayer(any())).thenReturn(false);

        bukkitStatic = org.mockito.Mockito.mockStatic(Bukkit.class);
        bukkitStatic.when(Bukkit::getScheduler).thenReturn(scheduler);
        when(scheduler.runTaskLater(eq(plugin), any(Runnable.class), anyLong()))
                .thenReturn(mock(BukkitTask.class));

        requestManager = new RequestManager(plugin);
    }

    @AfterEach
    void tearDown() {
        bukkitStatic.close();
    }

    private Player mockPlayer(String name) {
        Player p = mock(Player.class);
        UUID uuid = UUID.randomUUID();
        when(p.getUniqueId()).thenReturn(uuid);
        when(p.getName()).thenReturn(name);
        when(p.isOnline()).thenReturn(true);
        when(p.getLocation()).thenReturn(mock(Location.class));
        bukkitStatic.when(() -> Bukkit.getPlayer(uuid)).thenReturn(p);
        return p;
    }

    @Test
    void cannotSendARequestToYourself() {
        Player sender = mockPlayer("Steve");

        requestManager.sendRequest(sender, sender, RequestType.TPA);

        verify(messageManager).sendChat(eq(sender), eq("tpa.cannot-target-self"), any());
        assertTrue(requestManager.getIncomingSenderNames(sender.getUniqueId()).isEmpty());
    }

    @Test
    void requestIsRejectedWhenTargetDisabledIncomingRequests() {
        Player sender = mockPlayer("Steve");
        Player target = mockPlayer("Alex");
        when(playerSettingsManager.isAcceptingRequests(target.getUniqueId())).thenReturn(false);
        when(sender.hasPermission("stonetpa.bypass.toggle")).thenReturn(false);

        requestManager.sendRequest(sender, target, RequestType.TPA);

        verify(messageManager).sendChat(eq(sender), eq("tpa.target-requests-disabled"), any());
        assertTrue(requestManager.getIncomingSenderNames(target.getUniqueId()).isEmpty());
    }

    @Test
    void bypassTogglePermissionOverridesDisabledRequests() {
        Player sender = mockPlayer("Steve");
        Player target = mockPlayer("Alex");
        when(playerSettingsManager.isAcceptingRequests(target.getUniqueId())).thenReturn(false);
        when(sender.hasPermission("stonetpa.bypass.toggle")).thenReturn(true);

        requestManager.sendRequest(sender, target, RequestType.TPA);

        assertEquals(1, requestManager.getIncomingSenderNames(target.getUniqueId()).size());
    }

    @Test
    void requestIsRejectedWhileOnCooldown() {
        Player sender = mockPlayer("Steve");
        Player target = mockPlayer("Alex");
        when(sender.hasPermission("stonetpa.bypass.cooldown")).thenReturn(false);
        when(cooldownManager.getRemaining(sender.getUniqueId(), 10)).thenReturn(7);

        requestManager.sendRequest(sender, target, RequestType.TPA);

        verify(messageManager).sendChat(eq(sender), eq("tpa.cooldown"), eq(java.util.Map.of("seconds", "7")));
        verify(cooldownManager, never()).setUsed(any());
        assertTrue(requestManager.getIncomingSenderNames(target.getUniqueId()).isEmpty());
    }

    @Test
    void bypassCooldownPermissionSkipsTheCooldownCheckEntirely() {
        Player sender = mockPlayer("Steve");
        Player target = mockPlayer("Alex");
        when(sender.hasPermission("stonetpa.bypass.cooldown")).thenReturn(true);

        requestManager.sendRequest(sender, target, RequestType.TPA);

        verify(cooldownManager, never()).getRemaining(any(), org.mockito.ArgumentMatchers.anyInt());
        verify(cooldownManager).setUsed(sender.getUniqueId());
        assertEquals(1, requestManager.getIncomingSenderNames(target.getUniqueId()).size());
    }

    @Test
    void cannotSendASecondRequestWhileOneIsAlreadyOutgoing() {
        Player sender = mockPlayer("Steve");
        Player target = mockPlayer("Alex");
        Player anotherTarget = mockPlayer("Notch");

        requestManager.sendRequest(sender, target, RequestType.TPA);
        requestManager.sendRequest(sender, anotherTarget, RequestType.TPA);

        verify(messageManager).sendChat(eq(sender), eq("tpa.already-have-outgoing"), any());
        assertTrue(requestManager.getIncomingSenderNames(anotherTarget.getUniqueId()).isEmpty());
        // The blocked second attempt must not reset/consume the cooldown again.
        verify(cooldownManager, times(1)).setUsed(sender.getUniqueId());
    }

    @Test
    void acceptingATpaRequestTeleportsTheSenderToTheAccepter() {
        Player sender = mockPlayer("Steve");
        Player target = mockPlayer("Alex");
        requestManager.sendRequest(sender, target, RequestType.TPA);

        requestManager.accept(target, null);

        // Calling target.getLocation() (a mock invocation) INSIDE the
        // argument list of another mock's verify(...) call corrupts
        // Mockito's matcher stack ("0 matchers expected, 1 recorded") - it
        // must be evaluated in its own statement first.
        Location targetLocation = target.getLocation();
        verify(teleportManager).startTeleport(eq(sender), eq(targetLocation), any());
        assertTrue(requestManager.getIncomingSenderNames(target.getUniqueId()).isEmpty());
    }

    @Test
    void acceptingATpaHereRequestTeleportsTheAccepterToTheSender() {
        Player sender = mockPlayer("Steve");
        Player target = mockPlayer("Alex");
        requestManager.sendRequest(sender, target, RequestType.TPA_HERE);

        requestManager.accept(target, null);

        Location senderLocation = sender.getLocation();
        verify(teleportManager).startTeleport(eq(target), eq(senderLocation), any());
    }

    @Test
    void denyingRemovesTheRequestAndNotifiesBothSides() {
        Player sender = mockPlayer("Steve");
        Player target = mockPlayer("Alex");
        requestManager.sendRequest(sender, target, RequestType.TPA);

        requestManager.deny(target, null);

        verify(messageManager).sendChat(eq(target), eq("tpa.denied-notify-target"), any());
        verify(messageManager).sendChat(eq(sender), eq("tpa.denied-notify-sender"), any());
        assertTrue(requestManager.getIncomingSenderNames(target.getUniqueId()).isEmpty());
    }

    @Test
    void cancelingNotifiesBothSidesAndClearsTheRequest() {
        Player sender = mockPlayer("Steve");
        Player target = mockPlayer("Alex");
        requestManager.sendRequest(sender, target, RequestType.TPA);

        requestManager.cancel(sender);

        verify(messageManager).sendChat(eq(sender), eq("tpa.cancelled-notify-sender"), any());
        verify(messageManager).sendChat(eq(target), eq("tpa.cancelled-notify-target"), any());
        assertTrue(requestManager.getIncomingSenderNames(target.getUniqueId()).isEmpty());
    }

    @Test
    void cancelingWithNoOutgoingRequestJustInformsTheSender() {
        Player sender = mockPlayer("Steve");

        requestManager.cancel(sender);

        verify(messageManager).sendChat(eq(sender), eq("tpa.no-pending-outgoing"), any());
    }

    @Test
    void acceptingWithMultipleIncomingRequestsRequiresANameThenWorks() {
        Player senderA = mockPlayer("Steve");
        Player senderB = mockPlayer("Notch");
        Player target = mockPlayer("Alex");
        requestManager.sendRequest(senderA, target, RequestType.TPA);
        requestManager.sendRequest(senderB, target, RequestType.TPA);

        requestManager.accept(target, null);
        verify(messageManager).sendChat(eq(target), eq("tpa.multiple-pending-specify"), any());
        verify(teleportManager, never()).startTeleport(any(), any(), any());

        requestManager.accept(target, "Steve");
        verify(teleportManager).startTeleport(eq(senderA), any(), any());
        // The other request from Notch must still be untouched.
        assertEquals(1, requestManager.getIncomingSenderNames(target.getUniqueId()).size());
    }

    @Test
    void expiryFiresAfterTheScheduledDelayAndNotifiesBothSides() {
        Player sender = mockPlayer("Steve");
        Player target = mockPlayer("Alex");
        requestManager.sendRequest(sender, target, RequestType.TPA);

        ArgumentCaptor<Runnable> captor = ArgumentCaptor.forClass(Runnable.class);
        verify(scheduler).runTaskLater(eq(plugin), captor.capture(), eq(60L * 20L));

        // Simulate the delay elapsing, instead of actually waiting for it.
        captor.getValue().run();

        verify(messageManager).sendChat(eq(sender), eq("tpa.expired-notify-sender"), any());
        verify(messageManager).sendChat(eq(target), eq("tpa.expired-notify-target"), any());
        assertTrue(requestManager.getIncomingSenderNames(target.getUniqueId()).isEmpty());
    }

    @Test
    void handleQuitClearsBothOutgoingAndIncomingRequestsForThatPlayer() {
        Player bystander = mockPlayer("Bystander");
        Player leaver = mockPlayer("Leaver");
        Player thirdParty = mockPlayer("ThirdParty");

        // leaver has an outgoing request to bystander...
        requestManager.sendRequest(leaver, bystander, RequestType.TPA);
        // ...and an incoming request from thirdParty.
        requestManager.sendRequest(thirdParty, leaver, RequestType.TPA);

        requestManager.handleQuit(leaver);

        assertTrue(requestManager.getIncomingSenderNames(bystander.getUniqueId()).isEmpty(),
                "leaver's outgoing request must be gone from the target's incoming list");
        assertTrue(requestManager.getIncomingSenderNames(leaver.getUniqueId()).isEmpty(),
                "the request incoming to the player who left must be gone too");
    }
}
