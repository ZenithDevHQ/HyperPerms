package com.hyperperms.tablist;

import com.hyperperms.HyperPerms;
import com.hyperperms.chat.ColorUtil;
import com.hyperperms.util.Logger;
import com.hypixel.hytale.event.EventPriority;
import com.hypixel.hytale.event.EventRegistration;
import com.hypixel.hytale.event.EventRegistry;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.event.events.player.PlayerConnectEvent;
import com.hypixel.hytale.server.core.event.events.player.PlayerDisconnectEvent;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import org.jetbrains.annotations.NotNull;

import java.awt.Color;
import java.lang.reflect.Method;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * Hytale event listener for tab list name formatting.
 *
 * <p>This listener:
 * <ul>
 *   <li>Listens to PlayerConnectEvent to format tab list names</li>
 *   <li>Listens to PlayerDisconnectEvent to clean up</li>
 *   <li>Uses reflection to find available Hytale methods for setting display names</li>
 * </ul>
 */
public class TabListListener {

    private final HyperPerms plugin;
    private final TabListManager tabListManager;

    // Track connected players
    private final ConcurrentMap<UUID, PlayerRef> trackedPlayers = new ConcurrentHashMap<>();

    // Event registrations
    private EventRegistration<?, ?> connectRegistration;
    private EventRegistration<?, ?> disconnectRegistration;

    // Reflection cache for Hytale API methods
    private static Method setDisplayNameMethod;
    private static Method setTabListNameMethod;
    private static boolean methodsChecked = false;

    /**
     * Creates a new TabListListener.
     *
     * @param plugin the HyperPerms plugin instance
     * @param tabListManager the tab list manager
     */
    public TabListListener(@NotNull HyperPerms plugin, @NotNull TabListManager tabListManager) {
        this.plugin = Objects.requireNonNull(plugin, "plugin cannot be null");
        this.tabListManager = Objects.requireNonNull(tabListManager, "tabListManager cannot be null");

        // Check for available methods on first construction
        checkAvailableMethods();
    }

    /**
     * Checks for available display name methods using reflection.
     */
    private static synchronized void checkAvailableMethods() {
        if (methodsChecked) {
            return;
        }
        methodsChecked = true;

        try {
            // Try to find setDisplayName method on PlayerRef
            setDisplayNameMethod = PlayerRef.class.getMethod("setDisplayName", Message.class);
            Logger.debug("Found PlayerRef.setDisplayName(Message) method");
        } catch (NoSuchMethodException e) {
            Logger.debug("PlayerRef.setDisplayName(Message) not found");
        }

        try {
            // Try to find setTabListName method on PlayerRef
            setTabListNameMethod = PlayerRef.class.getMethod("setTabListName", Message.class);
            Logger.debug("Found PlayerRef.setTabListName(Message) method");
        } catch (NoSuchMethodException e) {
            Logger.debug("PlayerRef.setTabListName(Message) not found");
        }

        // Also try String variants
        if (setDisplayNameMethod == null) {
            try {
                setDisplayNameMethod = PlayerRef.class.getMethod("setDisplayName", String.class);
                Logger.debug("Found PlayerRef.setDisplayName(String) method");
            } catch (NoSuchMethodException e) {
                Logger.debug("PlayerRef.setDisplayName(String) not found");
            }
        }

        if (setTabListNameMethod == null) {
            try {
                setTabListNameMethod = PlayerRef.class.getMethod("setTabListName", String.class);
                Logger.debug("Found PlayerRef.setTabListName(String) method");
            } catch (NoSuchMethodException e) {
                Logger.debug("PlayerRef.setTabListName(String) not found");
            }
        }

        if (setDisplayNameMethod == null && setTabListNameMethod == null) {
            Logger.warn("No tab list name methods found on PlayerRef - tab list formatting may not work");
        }
    }

    /**
     * Registers the event listeners with the Hytale event registry.
     *
     * @param eventRegistry the event registry
     */
    public void register(@NotNull EventRegistry eventRegistry) {
        Objects.requireNonNull(eventRegistry, "eventRegistry cannot be null");

        // Register async handler for PlayerConnectEvent (sync, kicks off async formatting)
        connectRegistration = eventRegistry.register(
            PlayerConnectEvent.class,
            this::onPlayerConnect
        );

        // Register sync handler for PlayerDisconnectEvent
        disconnectRegistration = eventRegistry.register(
            PlayerDisconnectEvent.class,
            this::onPlayerDisconnect
        );

        // Set up the name applier callback
        tabListManager.setNameApplier(this::applyTabListName);

        Logger.info("Tab list listener registered");
    }

    /**
     * Unregisters the event listeners.
     *
     * @param eventRegistry the event registry
     */
    public void unregister(@NotNull EventRegistry eventRegistry) {
        // Clear registrations
        if (connectRegistration != null) {
            connectRegistration = null;
        }
        if (disconnectRegistration != null) {
            disconnectRegistration = null;
        }

        // Clear name applier
        tabListManager.setNameApplier(null);

        // Clear tracked players
        trackedPlayers.clear();

        Logger.info("Tab list listener marked for cleanup");
    }

    /**
     * Handles player connect event.
     *
     * @param event the player connect event
     */
    private void onPlayerConnect(PlayerConnectEvent event) {
        PlayerRef playerRef = event.getPlayerRef();
        if (playerRef == null) {
            return;
        }

        UUID uuid = playerRef.getUuid();
        String playerName = playerRef.getUsername();

        // Track the player
        trackedPlayers.put(uuid, playerRef);

        Logger.debug("Tab list: Player connecting: %s (%s)", playerName, uuid);

        // Format and apply tab list name asynchronously
        if (tabListManager.isEnabled()) {
            tabListManager.formatTabListName(uuid, playerName).thenAccept(formattedName -> {
                applyTabListName(uuid, formattedName);
            }).exceptionally(e -> {
                Logger.warn("Failed to format tab list name for %s: %s", playerName, e.getMessage());
                return null;
            });
        }
    }

    /**
     * Handles player disconnect event.
     *
     * @param event the player disconnect event
     */
    private void onPlayerDisconnect(PlayerDisconnectEvent event) {
        PlayerRef playerRef = event.getPlayerRef();
        if (playerRef == null) {
            return;
        }

        UUID uuid = playerRef.getUuid();

        // Untrack the player
        trackedPlayers.remove(uuid);

        // Invalidate cache
        tabListManager.invalidateCache(uuid);

        Logger.debug("Tab list: Player disconnected: %s", playerRef.getUsername());
    }

    /**
     * Applies the formatted tab list name to a player.
     *
     * @param uuid the player's UUID
     * @param formattedName the formatted name with color codes
     */
    private void applyTabListName(@NotNull UUID uuid, @NotNull String formattedName) {
        PlayerRef playerRef = trackedPlayers.get(uuid);
        if (playerRef == null) {
            Logger.debug("Tab list: Player %s not tracked, cannot apply name", uuid);
            return;
        }

        try {
            // Convert to Hytale Message
            Message nameMessage = toHytaleMessage(formattedName);

            // Try setTabListName first (more specific)
            if (setTabListNameMethod != null) {
                if (setTabListNameMethod.getParameterTypes()[0] == Message.class) {
                    setTabListNameMethod.invoke(playerRef, nameMessage);
                } else {
                    setTabListNameMethod.invoke(playerRef, formattedName);
                }
                Logger.debug("Tab list: Applied name via setTabListName: %s", formattedName);
                return;
            }

            // Fall back to setDisplayName
            if (setDisplayNameMethod != null) {
                if (setDisplayNameMethod.getParameterTypes()[0] == Message.class) {
                    setDisplayNameMethod.invoke(playerRef, nameMessage);
                } else {
                    setDisplayNameMethod.invoke(playerRef, formattedName);
                }
                Logger.debug("Tab list: Applied name via setDisplayName: %s", formattedName);
                return;
            }

            Logger.debug("Tab list: No method available to set display name for %s", uuid);
        } catch (Exception e) {
            Logger.warn("Tab list: Failed to apply name for %s: %s", uuid, e.getMessage());
        }
    }

    /**
     * Updates a specific player's tab list name.
     *
     * @param uuid the player's UUID
     */
    public void updatePlayer(@NotNull UUID uuid) {
        PlayerRef playerRef = trackedPlayers.get(uuid);
        if (playerRef == null) {
            return;
        }

        tabListManager.updatePlayer(uuid, playerRef.getUsername());
    }

    /**
     * Updates all connected players' tab list names.
     */
    public void updateAllPlayers() {
        for (var entry : trackedPlayers.entrySet()) {
            UUID uuid = entry.getKey();
            PlayerRef playerRef = entry.getValue();
            tabListManager.updatePlayer(uuid, playerRef.getUsername());
        }
    }

    /**
     * Gets the number of tracked players.
     *
     * @return the tracked player count
     */
    public int getTrackedPlayerCount() {
        return trackedPlayers.size();
    }

    /**
     * Converts a formatted string with color codes to a Hytale Message.
     *
     * @param formatted the formatted string
     * @return the Hytale Message
     */
    @NotNull
    public static Message toHytaleMessage(@NotNull String formatted) {
        if (formatted == null || formatted.isEmpty()) {
            return Message.raw("");
        }

        try {
            // Parse color codes and build Message
            Message result = null;
            StringBuilder currentText = new StringBuilder();
            Color currentColor = null;
            boolean bold = false;
            boolean italic = false;

            int i = 0;
            while (i < formatted.length()) {
                char c = formatted.charAt(i);

                // Check for color/format code
                if (c == '§' && i + 1 < formatted.length()) {
                    // Flush current segment
                    if (currentText.length() > 0) {
                        Message segment = Message.raw(currentText.toString());
                        if (currentColor != null) {
                            segment = segment.color(currentColor);
                        }
                        if (bold) {
                            segment = segment.bold(true);
                        }
                        if (italic) {
                            segment = segment.italic(true);
                        }
                        result = (result == null) ? segment : Message.join(result, segment);
                        currentText.setLength(0);
                    }

                    char code = Character.toLowerCase(formatted.charAt(i + 1));

                    // Handle code
                    switch (code) {
                        case '0' -> { currentColor = new Color(0x000000); bold = false; italic = false; }
                        case '1' -> { currentColor = new Color(0x0000AA); bold = false; italic = false; }
                        case '2' -> { currentColor = new Color(0x00AA00); bold = false; italic = false; }
                        case '3' -> { currentColor = new Color(0x00AAAA); bold = false; italic = false; }
                        case '4' -> { currentColor = new Color(0xAA0000); bold = false; italic = false; }
                        case '5' -> { currentColor = new Color(0xAA00AA); bold = false; italic = false; }
                        case '6' -> { currentColor = new Color(0xFFAA00); bold = false; italic = false; }
                        case '7' -> { currentColor = new Color(0xAAAAAA); bold = false; italic = false; }
                        case '8' -> { currentColor = new Color(0x555555); bold = false; italic = false; }
                        case '9' -> { currentColor = new Color(0x5555FF); bold = false; italic = false; }
                        case 'a' -> { currentColor = new Color(0x55FF55); bold = false; italic = false; }
                        case 'b' -> { currentColor = new Color(0x55FFFF); bold = false; italic = false; }
                        case 'c' -> { currentColor = new Color(0xFF5555); bold = false; italic = false; }
                        case 'd' -> { currentColor = new Color(0xFF55FF); bold = false; italic = false; }
                        case 'e' -> { currentColor = new Color(0xFFFF55); bold = false; italic = false; }
                        case 'f' -> { currentColor = new Color(0xFFFFFF); bold = false; italic = false; }
                        case 'l' -> bold = true;
                        case 'o' -> italic = true;
                        case 'r' -> { currentColor = null; bold = false; italic = false; }
                        default -> { }
                    }

                    i += 2;
                    continue;
                }

                currentText.append(c);
                i++;
            }

            // Flush remaining
            if (currentText.length() > 0) {
                Message segment = Message.raw(currentText.toString());
                if (currentColor != null) {
                    segment = segment.color(currentColor);
                }
                if (bold) {
                    segment = segment.bold(true);
                }
                if (italic) {
                    segment = segment.italic(true);
                }
                result = (result == null) ? segment : Message.join(result, segment);
            }

            return result != null ? result : Message.raw("");
        } catch (Exception e) {
            Logger.warn("Tab list: Failed to parse colors: %s", e.getMessage());
            return Message.raw(ColorUtil.stripColors(formatted));
        }
    }
}
