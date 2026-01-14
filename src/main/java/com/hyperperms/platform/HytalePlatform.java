package com.hyperperms.platform;

import com.hyperperms.HyperPerms;
import com.hyperperms.util.Logger;
import org.jetbrains.annotations.NotNull;

import java.nio.file.Path;

/**
 * Hytale platform integration.
 * <p>
 * This class handles integration with Hytale's server API. When the actual
 * Hytale server API becomes available, this class will need to be updated
 * to properly integrate with:
 * <ul>
 *   <li>PermissionsModule - for permission checks</li>
 *   <li>PlayerManager - for player join/leave events</li>
 *   <li>CommandRegistry - for command registration</li>
 *   <li>EventBus - for server events</li>
 * </ul>
 */
public final class HytalePlatform {

    private final HyperPerms plugin;

    public HytalePlatform(@NotNull HyperPerms plugin) {
        this.plugin = plugin;
    }

    /**
     * Registers the plugin with Hytale's permission system.
     * <p>
     * When the Hytale API is available, this should:
     * <pre>
     * PermissionsModule.getInstance().registerProvider(new HyperPermsProvider());
     * </pre>
     */
    public void registerPermissionProvider() {
        Logger.info("Permission provider registration ready (awaiting Hytale API)");
        // TODO: Implement when Hytale API is available
        // PermissionsModule.getInstance().registerProvider(this::checkPermission);
    }

    /**
     * Registers event listeners for player join/leave.
     * <p>
     * When the Hytale API is available, this should:
     * <pre>
     * EventBus.subscribe(PlayerJoinEvent.class, this::onPlayerJoin);
     * EventBus.subscribe(PlayerLeaveEvent.class, this::onPlayerLeave);
     * </pre>
     */
    public void registerEventListeners() {
        Logger.info("Event listeners ready (awaiting Hytale API)");
        // TODO: Implement when Hytale API is available
    }

    /**
     * Registers commands with Hytale's command system.
     * <p>
     * When the Hytale API is available, this should:
     * <pre>
     * CommandRegistry.register("hp", new HyperPermsCommand());
     * CommandRegistry.registerAlias("hp", "hyperperms");
     * </pre>
     */
    public void registerCommands() {
        Logger.info("Commands ready (awaiting Hytale API)");
        // TODO: Implement when Hytale API is available
    }

    /**
     * Gets the data directory for the plugin.
     *
     * @param baseDir the server plugins directory
     * @return the plugin data directory
     */
    @NotNull
    public static Path getDataDirectory(@NotNull Path baseDir) {
        return baseDir.resolve("HyperPerms");
    }

    // ==================== Event Handlers ====================

    /**
     * Called when a player joins the server.
     * Loads the player's permission data.
     *
     * @param playerId the player's UUID (as string from Hytale)
     * @param username the player's username
     */
    public void onPlayerJoin(@NotNull String playerId, @NotNull String username) {
        java.util.UUID uuid = java.util.UUID.fromString(playerId);
        plugin.getUserManager().loadUser(uuid).thenAccept(opt -> {
            var user = opt.orElseGet(() -> plugin.getUserManager().getOrCreateUser(uuid));
            user.setUsername(username);
            Logger.debug("Loaded permissions for %s", username);
        });
    }

    /**
     * Called when a player leaves the server.
     * Saves and optionally unloads the player's data.
     *
     * @param playerId the player's UUID (as string from Hytale)
     */
    public void onPlayerLeave(@NotNull String playerId) {
        java.util.UUID uuid = java.util.UUID.fromString(playerId);
        var user = plugin.getUserManager().getUser(uuid);
        if (user != null) {
            plugin.getUserManager().saveUser(user);
            // Optionally unload to save memory:
            // plugin.getUserManager().unload(uuid);
        }
    }

    // ==================== Permission Provider ====================

    /**
     * Permission check callback for Hytale's permission system.
     *
     * @param playerId   the player UUID
     * @param permission the permission to check
     * @return true if the player has the permission
     */
    public boolean checkPermission(@NotNull String playerId, @NotNull String permission) {
        java.util.UUID uuid = java.util.UUID.fromString(playerId);
        return plugin.hasPermission(uuid, permission);
    }
}
