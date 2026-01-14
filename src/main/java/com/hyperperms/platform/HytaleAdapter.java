package com.hyperperms.platform;

import com.hyperperms.HyperPerms;
import com.hyperperms.context.PlayerContextProvider;
import com.hyperperms.util.Logger;
import com.hypixel.hytale.protocol.GameMode;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Adapter that bridges HyperPerms with Hytale's player and world systems.
 * <p>
 * This class:
 * <ul>
 *   <li>Implements {@link PlayerContextProvider} for context calculators</li>
 *   <li>Tracks online players and their current state</li>
 *   <li>Provides access to player world and game mode information</li>
 * </ul>
 */
public class HytaleAdapter implements PlayerContextProvider {

    private final HyperPerms hyperPerms;
    private final HyperPermsPlugin plugin;

    // Track online players by UUID
    private final Map<UUID, PlayerData> playerData = new ConcurrentHashMap<>();

    /**
     * Creates a new HytaleAdapter.
     *
     * @param hyperPerms the HyperPerms instance
     * @param plugin     the HyperPerms plugin instance
     */
    public HytaleAdapter(@NotNull HyperPerms hyperPerms, @NotNull HyperPermsPlugin plugin) {
        this.hyperPerms = hyperPerms;
        this.plugin = plugin;
    }

    // ==================== PlayerContextProvider Implementation ====================

    @Override
    @Nullable
    public String getWorld(@NotNull UUID uuid) {
        PlayerData data = playerData.get(uuid);
        if (data != null) {
            return data.worldName;
        }
        return null;
    }

    @Override
    @Nullable
    public String getGameMode(@NotNull UUID uuid) {
        PlayerData data = playerData.get(uuid);
        if (data == null || data.playerRef == null) {
            return null;
        }

        try {
            // Get the Player entity from the PlayerRef
            Player player = getPlayerEntity(data.playerRef);
            if (player != null) {
                GameMode gameMode = player.getGameMode();
                if (gameMode != null) {
                    return gameMode.name().toLowerCase();
                }
            }
        } catch (Exception e) {
            Logger.debug("Failed to get game mode for %s: %s", uuid, e.getMessage());
        }

        return data.gameMode; // Fall back to cached value
    }

    @Override
    public boolean isOnline(@NotNull UUID uuid) {
        return playerData.containsKey(uuid);
    }

    // ==================== Player Tracking ====================

    /**
     * Tracks a player when they connect.
     *
     * @param playerRef the player reference
     * @param worldName the initial world name
     */
    public void trackPlayer(@NotNull PlayerRef playerRef, @Nullable String worldName) {
        UUID uuid = playerRef.getUuid();
        PlayerData data = new PlayerData(playerRef, worldName);
        playerData.put(uuid, data);
        Logger.debug("Tracking player: %s in world: %s", playerRef.getUsername(), worldName);
    }

    /**
     * Untracks a player when they disconnect.
     *
     * @param uuid the player's UUID
     */
    public void untrackPlayer(@NotNull UUID uuid) {
        PlayerData removed = playerData.remove(uuid);
        if (removed != null) {
            Logger.debug("Untracked player: %s", uuid);
        }
    }

    /**
     * Updates a player's world when they change worlds.
     *
     * @param uuid      the player's UUID
     * @param worldName the new world name
     */
    public void updatePlayerWorld(@NotNull UUID uuid, @Nullable String worldName) {
        PlayerData data = playerData.get(uuid);
        if (data != null) {
            String oldWorld = data.worldName;
            data.worldName = worldName;
            Logger.debug("Player %s world changed: %s -> %s", uuid, oldWorld, worldName);
        }
    }

    /**
     * Updates a player's game mode.
     *
     * @param uuid     the player's UUID
     * @param gameMode the new game mode name
     */
    public void updatePlayerGameMode(@NotNull UUID uuid, @NotNull String gameMode) {
        PlayerData data = playerData.get(uuid);
        if (data != null) {
            data.gameMode = gameMode.toLowerCase();
            Logger.debug("Player %s game mode changed to: %s", uuid, gameMode);

            // Invalidate cache when game mode changes
            hyperPerms.getCacheInvalidator().invalidateContextCache(uuid);
        }
    }

    // ==================== Utility Methods ====================

    /**
     * Gets the Player entity from a PlayerRef.
     *
     * @param playerRef the player reference
     * @return the Player entity, or null if not available
     */
    @Nullable
    private Player getPlayerEntity(@NotNull PlayerRef playerRef) {
        try {
            var holder = playerRef.getHolder();
            if (holder != null) {
                return holder.getComponent(Player.getComponentType());
            }
        } catch (Exception e) {
            // Player may not be fully initialized yet
            Logger.debug("Could not get Player entity: %s", e.getMessage());
        }
        return null;
    }

    /**
     * Gets a tracked player's data.
     *
     * @param uuid the player's UUID
     * @return the player data, or null if not tracked
     */
    @Nullable
    public PlayerData getPlayerData(@NotNull UUID uuid) {
        return playerData.get(uuid);
    }

    /**
     * Gets the PlayerRef for a tracked player.
     *
     * @param uuid the player's UUID
     * @return the PlayerRef, or null if not tracked
     */
    @Nullable
    public PlayerRef getPlayerRef(@NotNull UUID uuid) {
        PlayerData data = playerData.get(uuid);
        return data != null ? data.playerRef : null;
    }

    /**
     * Gets the username for a tracked player.
     *
     * @param uuid the player's UUID
     * @return the username, or null if not tracked
     */
    @Nullable
    public String getUsername(@NotNull UUID uuid) {
        PlayerData data = playerData.get(uuid);
        if (data != null && data.playerRef != null) {
            return data.playerRef.getUsername();
        }
        return null;
    }

    /**
     * Gets all online player UUIDs.
     *
     * @return set of online player UUIDs
     */
    @NotNull
    public java.util.Set<UUID> getOnlinePlayers() {
        return java.util.Collections.unmodifiableSet(playerData.keySet());
    }

    /**
     * Gets the count of online players.
     *
     * @return the number of online players
     */
    public int getOnlinePlayerCount() {
        return playerData.size();
    }

    /**
     * Shuts down the adapter and clears all tracked data.
     */
    public void shutdown() {
        playerData.clear();
        Logger.debug("HytaleAdapter shutdown complete");
    }

    // ==================== Inner Classes ====================

    /**
     * Holds tracked data for an online player.
     */
    public static class PlayerData {
        final PlayerRef playerRef;
        volatile String worldName;
        volatile String gameMode;

        PlayerData(PlayerRef playerRef, String worldName) {
            this.playerRef = playerRef;
            this.worldName = worldName;
            this.gameMode = "adventure"; // Default game mode
        }
    }
}
