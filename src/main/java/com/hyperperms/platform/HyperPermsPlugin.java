package com.hyperperms.platform;

import com.hyperperms.HyperPerms;
import com.hyperperms.HyperPermsBootstrap;
import com.hyperperms.util.Logger;
import com.hypixel.hytale.server.core.event.events.ecs.ChangeGameModeEvent;
import com.hypixel.hytale.server.core.event.events.player.AddPlayerToWorldEvent;
import com.hypixel.hytale.server.core.event.events.player.PlayerConnectEvent;
import com.hypixel.hytale.server.core.event.events.player.PlayerDisconnectEvent;
import com.hypixel.hytale.server.core.permissions.PermissionsModule;
import com.hypixel.hytale.server.core.plugin.JavaPlugin;
import com.hypixel.hytale.server.core.plugin.JavaPluginInit;
import com.hypixel.hytale.server.core.universe.PlayerRef;

import java.util.logging.Level;

/**
 * Main Hytale plugin class for HyperPerms.
 * <p>
 * This class integrates HyperPerms with the Hytale server by:
 * <ul>
 *   <li>Registering as a permission provider</li>
 *   <li>Listening to player connect/disconnect events</li>
 *   <li>Tracking world and game mode changes for contexts</li>
 * </ul>
 */
public class HyperPermsPlugin extends JavaPlugin {

    private HyperPerms hyperPerms;
    private HyperPermsPermissionProvider permissionProvider;
    private HytaleAdapter adapter;
    private com.hyperperms.chat.ChatListener chatListener;
    private com.hyperperms.tablist.TabListListener tabListListener;

    /**
     * Creates a new HyperPermsPlugin instance.
     * Called by the Hytale plugin loader.
     *
     * @param init the plugin initialization data
     */
    public HyperPermsPlugin(JavaPluginInit init) {
        super(init);
    }

    @Override
    protected void setup() {
        // Initialize HyperPerms core
        hyperPerms = new HyperPerms(getDataDirectory(), java.util.logging.Logger.getLogger("HyperPerms"));

        // Register the global instance for API access
        HyperPermsBootstrap.setInstance(hyperPerms);

        // Create the platform adapter
        adapter = new HytaleAdapter(hyperPerms, this);

        // Create the permission provider
        permissionProvider = new HyperPermsPermissionProvider(hyperPerms);

        getLogger().at(Level.INFO).log("HyperPerms setup complete");
    }

    @Override
    protected void start() {
        // Enable HyperPerms core
        hyperPerms.enable();

        // Set the player context provider for context calculators
        hyperPerms.setPlayerContextProvider(adapter);

        // Register as a permission provider with Hytale
        registerPermissionProvider();

        // Register commands
        registerCommands();

        // Register event listeners
        registerEventListeners();

        getLogger().at(Level.INFO).log("HyperPerms v%s enabled!", getManifest().getVersion());
    }

    @Override
    protected void shutdown() {
        // Unregister chat listener
        if (chatListener != null) {
            try {
                chatListener.unregister(getEventRegistry());
            } catch (Exception e) {
                getLogger().at(Level.WARNING).withCause(e).log("Failed to unregister chat listener");
            }
        }

        // Unregister tab list listener
        if (tabListListener != null) {
            try {
                tabListListener.unregister(getEventRegistry());
            } catch (Exception e) {
                getLogger().at(Level.WARNING).withCause(e).log("Failed to unregister tab list listener");
            }
        }

        // Unregister permission provider
        if (permissionProvider != null) {
            try {
                PermissionsModule.get().removeProvider(permissionProvider);
            } catch (Exception e) {
                getLogger().at(Level.WARNING).withCause(e).log("Failed to unregister permission provider");
            }
        }

        // Disable HyperPerms core
        if (hyperPerms != null) {
            hyperPerms.disable();
        }

        // Clear the global instance
        HyperPermsBootstrap.setInstance(null);

        // Clear the adapter
        if (adapter != null) {
            adapter.shutdown();
        }

        getLogger().at(Level.INFO).log("HyperPerms disabled");
    }

    /**
     * Registers HyperPerms as a permission provider with Hytale.
     */
    private void registerPermissionProvider() {
        try {
            PermissionsModule.get().addProvider(permissionProvider);
            getLogger().at(Level.INFO).log("Registered HyperPerms as permission provider");
        } catch (Exception e) {
            getLogger().at(Level.SEVERE).withCause(e).log("Failed to register permission provider");
        }
    }

    /**
     * Registers HyperPerms commands with Hytale.
     */
    private void registerCommands() {
        try {
            HyperPermsCommand command = new HyperPermsCommand(hyperPerms);
            getCommandRegistry().registerCommand(command);
            getLogger().at(Level.INFO).log("Registered /hp command");
        } catch (Exception e) {
            getLogger().at(Level.SEVERE).withCause(e).log("Failed to register commands");
        }
    }

    /**
     * Registers event listeners for player lifecycle events.
     */
    private void registerEventListeners() {
        // Player connect event - load user data
        getEventRegistry().register(PlayerConnectEvent.class, this::onPlayerConnect);

        // Player disconnect event - save and cleanup
        getEventRegistry().register(PlayerDisconnectEvent.class, this::onPlayerDisconnect);

        // Player added to world event - track world changes (keyed event)
        getEventRegistry().registerGlobal(AddPlayerToWorldEvent.class, this::onPlayerAddedToWorld);

        // Game mode change event - update context cache
        getEventRegistry().registerGlobal(ChangeGameModeEvent.class, this::onGameModeChange);

        // Register chat listener (if chat formatting is enabled)
        registerChatListener();

        // Register tab list listener (if tab list formatting is enabled)
        registerTabListListener();

        getLogger().at(Level.INFO).log("Registered event listeners");
    }

    /**
     * Registers the chat listener for formatting player chat messages.
     */
    private void registerChatListener() {
        try {
            var chatManager = hyperPerms.getChatManager();
            if (chatManager != null && chatManager.isEnabled()) {
                chatListener = new com.hyperperms.chat.ChatListener(hyperPerms, chatManager);

                // Load config settings
                var config = hyperPerms.getConfig();
                if (config != null) {
                    chatListener.setAllowPlayerColors(config.isAllowPlayerColors());
                    chatListener.setColorPermission(config.getColorPermission());
                }

                chatListener.register(getEventRegistry());
                getLogger().at(Level.INFO).log("Chat formatting enabled");
            } else {
                getLogger().at(Level.INFO).log("Chat formatting is disabled in config");
            }
        } catch (Exception e) {
            getLogger().at(Level.WARNING).withCause(e).log("Failed to register chat listener");
        }
    }

    /**
     * Registers the tab list listener for formatting player tab list names.
     */
    private void registerTabListListener() {
        try {
            var tabListManager = hyperPerms.getTabListManager();
            if (tabListManager != null && tabListManager.isEnabled()) {
                tabListListener = new com.hyperperms.tablist.TabListListener(hyperPerms, tabListManager);
                tabListListener.register(getEventRegistry());
                getLogger().at(Level.INFO).log("Tab list formatting enabled");
            } else {
                getLogger().at(Level.INFO).log("Tab list formatting is disabled in config");
            }
        } catch (Exception e) {
            getLogger().at(Level.WARNING).withCause(e).log("Failed to register tab list listener");
        }
    }

    /**
     * Handles player connect event.
     * Loads or creates the user's permission data.
     *
     * @param event the player connect event
     */
    private void onPlayerConnect(PlayerConnectEvent event) {
        PlayerRef playerRef = event.getPlayerRef();
        java.util.UUID uuid = playerRef.getUuid();
        String username = playerRef.getUsername();
        String worldName = event.getWorld() != null ? event.getWorld().getName() : null;

        Logger.debug("Player connecting: %s (%s)", username, uuid);

        // Track the player in the adapter
        adapter.trackPlayer(playerRef, worldName);

        // Load user permissions async
        hyperPerms.getUserManager().loadUser(uuid).thenAccept(opt -> {
            boolean isNewUser = opt.isEmpty();
            var user = opt.orElseGet(() -> hyperPerms.getUserManager().getOrCreateUser(uuid));
            user.setUsername(username);

            // Save new users to persist their default group assignment
            if (isNewUser) {
                hyperPerms.getUserManager().saveUser(user).thenRun(() -> {
                    Logger.debug("Created and saved new user %s with default group: %s", 
                        username, user.getPrimaryGroup());
                });
            }

            // Prime the cache
            hyperPerms.getContextManager().getContexts(uuid);

            // Preload ChatAPI cache for external plugins
            com.hyperperms.api.ChatAPI.preload(uuid);

            Logger.debug("Loaded permissions for %s", username);
        }).exceptionally(e -> {
            Logger.severe("Failed to load permissions for %s", e, username);
            return null;
        });
    }

    /**
     * Handles player disconnect event.
     * Saves user data and cleans up caches.
     *
     * @param event the player disconnect event
     */
    private void onPlayerDisconnect(PlayerDisconnectEvent event) {
        PlayerRef playerRef = event.getPlayerRef();
        java.util.UUID uuid = playerRef.getUuid();
        String username = playerRef.getUsername();

        Logger.debug("Player disconnecting: %s", username);

        // Save user data
        var user = hyperPerms.getUserManager().getUser(uuid);
        if (user != null) {
            hyperPerms.getUserManager().saveUser(user).thenRun(() -> {
                Logger.debug("Saved permissions for %s", username);
            }).exceptionally(e -> {
                Logger.severe("Failed to save permissions for %s", e, username);
                return null;
            });
        }

        // Clear from cache
        hyperPerms.getCache().invalidate(uuid);

        // Clear ChatAPI cache for external plugins
        com.hyperperms.api.ChatAPI.invalidate(uuid);

        // Untrack the player
        adapter.untrackPlayer(uuid);
    }

    /**
     * Handles player added to world event.
     * Updates context data when player changes world.
     *
     * @param event the add player to world event
     */
    private void onPlayerAddedToWorld(AddPlayerToWorldEvent event) {
        // Get player from holder
        var holder = event.getHolder();
        PlayerRef playerRef = holder.getComponent(PlayerRef.getComponentType());

        if (playerRef == null) {
            return;
        }

        java.util.UUID uuid = playerRef.getUuid();
        String worldName = event.getWorld() != null ? event.getWorld().getName() : null;

        Logger.debug("Player %s moved to world: %s", playerRef.getUsername(), worldName);

        // Update world tracking
        adapter.updatePlayerWorld(uuid, worldName);

        // Invalidate context-sensitive cache entries
        hyperPerms.getCacheInvalidator().invalidateContextCache(uuid);
    }

    /**
     * Handles game mode change event.
     * Updates context data when player's game mode changes.
     *
     * @param event the game mode change event
     */
    private void onGameModeChange(ChangeGameModeEvent event) {
        // This event is entity-scoped, need to get the entity reference
        // For now, we'll invalidate all affected caches when game modes change
        // A more targeted approach would require getting the entity from the event context

        Logger.debug("Game mode change detected: %s", event.getGameMode().name());

        // The event doesn't directly provide the player, but since it's registered globally,
        // we need to handle it through the ECS system. For simplicity, we track game mode
        // changes through the adapter's polling mechanism.
    }

    /**
     * Gets the HyperPerms instance.
     *
     * @return the HyperPerms instance
     */
    public HyperPerms getHyperPerms() {
        return hyperPerms;
    }

    /**
     * Gets the platform adapter.
     *
     * @return the adapter
     */
    public HytaleAdapter getAdapter() {
        return adapter;
    }
}
