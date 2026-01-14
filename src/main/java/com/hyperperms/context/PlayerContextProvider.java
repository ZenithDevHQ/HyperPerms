package com.hyperperms.context;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

/**
 * Provider interface for obtaining player contextual information.
 * <p>
 * This interface abstracts platform-specific player data retrieval.
 * Implementations should be provided by the platform adapter (e.g., HytalePlatform).
 * <p>
 * When the Hytale API becomes available, the implementation should query
 * the actual player state from the server.
 */
public interface PlayerContextProvider {

    /**
     * Gets the name of the world the player is currently in.
     *
     * @param uuid the player's UUID
     * @return the world name, or null if the player is not online or world is unknown
     */
    @Nullable
    String getWorld(@NotNull UUID uuid);

    /**
     * Gets the player's current game mode.
     *
     * @param uuid the player's UUID
     * @return the game mode name (e.g., "survival", "creative", "adventure"),
     *         or null if the player is not online
     */
    @Nullable
    String getGameMode(@NotNull UUID uuid);

    /**
     * Checks if the player is currently online.
     *
     * @param uuid the player's UUID
     * @return true if the player is online
     */
    boolean isOnline(@NotNull UUID uuid);

    /**
     * A no-op implementation that returns null for all queries.
     * Used as a default when no platform is available.
     */
    PlayerContextProvider EMPTY = new PlayerContextProvider() {
        @Override
        public @Nullable String getWorld(@NotNull UUID uuid) {
            return null;
        }

        @Override
        public @Nullable String getGameMode(@NotNull UUID uuid) {
            return null;
        }

        @Override
        public boolean isOnline(@NotNull UUID uuid) {
            return false;
        }
    };
}
