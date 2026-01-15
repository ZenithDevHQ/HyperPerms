package com.hyperperms.api.events;

import com.hyperperms.api.context.ContextSet;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

/**
 * Event fired when a player's context changes.
 * <p>
 * Context changes occur when:
 * <ul>
 *   <li>A player changes worlds</li>
 *   <li>A player's game mode changes</li>
 *   <li>A player enters/exits a region</li>
 *   <li>The time of day changes (dawn/day/dusk/night transition)</li>
 *   <li>A player moves to a different biome</li>
 * </ul>
 * <p>
 * This event is useful for:
 * <ul>
 *   <li>Logging context changes for debugging</li>
 *   <li>Triggering actions when players enter specific contexts</li>
 *   <li>Updating UI elements that display context information</li>
 * </ul>
 */
public final class ContextChangeEvent implements HyperPermsEvent {

    private final UUID uuid;
    private final String contextKey;
    private final String oldValue;
    private final String newValue;
    private final ContextSet currentContexts;

    /**
     * Creates a new context change event.
     *
     * @param uuid            the player's UUID
     * @param contextKey      the context key that changed (e.g., "world", "gamemode", "region")
     * @param oldValue        the previous value, or null if newly added
     * @param newValue        the new value, or null if removed
     * @param currentContexts the player's current full context set
     */
    public ContextChangeEvent(@NotNull UUID uuid, @NotNull String contextKey,
                               @Nullable String oldValue, @Nullable String newValue,
                               @NotNull ContextSet currentContexts) {
        this.uuid = uuid;
        this.contextKey = contextKey;
        this.oldValue = oldValue;
        this.newValue = newValue;
        this.currentContexts = currentContexts;
    }

    @Override
    public EventType getType() {
        return EventType.CONTEXT_CHANGE;
    }

    /**
     * Gets the UUID of the player whose context changed.
     *
     * @return the player's UUID
     */
    @NotNull
    public UUID getUuid() {
        return uuid;
    }

    /**
     * Gets the context key that changed.
     * <p>
     * Common keys include:
     * <ul>
     *   <li>{@code world} - The player's current world</li>
     *   <li>{@code gamemode} - The player's game mode</li>
     *   <li>{@code region} - The region/zone the player is in</li>
     *   <li>{@code biome} - The biome at the player's location</li>
     *   <li>{@code time} - The current time period (day/night/dawn/dusk)</li>
     * </ul>
     *
     * @return the context key
     */
    @NotNull
    public String getContextKey() {
        return contextKey;
    }

    /**
     * Gets the previous value of the context.
     *
     * @return the old value, or null if this context was newly added
     */
    @Nullable
    public String getOldValue() {
        return oldValue;
    }

    /**
     * Gets the new value of the context.
     *
     * @return the new value, or null if this context was removed
     */
    @Nullable
    public String getNewValue() {
        return newValue;
    }

    /**
     * Gets the player's current full context set after this change.
     *
     * @return the current contexts
     */
    @NotNull
    public ContextSet getCurrentContexts() {
        return currentContexts;
    }

    /**
     * Checks if this is a world change event.
     *
     * @return true if the context key is "world"
     */
    public boolean isWorldChange() {
        return "world".equals(contextKey);
    }

    /**
     * Checks if this is a game mode change event.
     *
     * @return true if the context key is "gamemode"
     */
    public boolean isGameModeChange() {
        return "gamemode".equals(contextKey);
    }

    /**
     * Checks if this is a region change event.
     *
     * @return true if the context key is "region"
     */
    public boolean isRegionChange() {
        return "region".equals(contextKey);
    }

    /**
     * Checks if this is a biome change event.
     *
     * @return true if the context key is "biome"
     */
    public boolean isBiomeChange() {
        return "biome".equals(contextKey);
    }

    /**
     * Checks if this is a time change event.
     *
     * @return true if the context key is "time"
     */
    public boolean isTimeChange() {
        return "time".equals(contextKey);
    }

    @Override
    public String toString() {
        return "ContextChangeEvent{uuid=" + uuid + ", key=" + contextKey +
                ", oldValue=" + oldValue + ", newValue=" + newValue + "}";
    }
}
