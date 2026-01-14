package com.hyperperms.api.context;

import org.jetbrains.annotations.NotNull;

import java.util.Objects;

/**
 * Represents a single context key-value pair.
 * <p>
 * Contexts are used to conditionally apply permissions based on the player's
 * current state. Examples include:
 * <ul>
 *   <li>{@code world=overworld} - only applies in the overworld</li>
 *   <li>{@code gamemode=creative} - only applies in creative mode</li>
 *   <li>{@code server=lobby} - only applies on the lobby server</li>
 * </ul>
 *
 * @param key   the context key (e.g., "world", "gamemode")
 * @param value the context value (e.g., "overworld", "creative")
 */
public record Context(@NotNull String key, @NotNull String value) implements Comparable<Context> {

    /**
     * Common context keys.
     */
    public static final String WORLD_KEY = "world";
    public static final String SERVER_KEY = "server";
    public static final String GAMEMODE_KEY = "gamemode";

    public Context {
        Objects.requireNonNull(key, "key cannot be null");
        Objects.requireNonNull(value, "value cannot be null");
        key = key.toLowerCase();
        value = value.toLowerCase();
    }

    /**
     * Creates a world context.
     *
     * @param worldName the world name
     * @return a new world context
     */
    public static Context world(@NotNull String worldName) {
        return new Context(WORLD_KEY, worldName);
    }

    /**
     * Creates a server context.
     *
     * @param serverName the server name
     * @return a new server context
     */
    public static Context server(@NotNull String serverName) {
        return new Context(SERVER_KEY, serverName);
    }

    /**
     * Creates a gamemode context.
     *
     * @param gameMode the game mode
     * @return a new gamemode context
     */
    public static Context gameMode(@NotNull String gameMode) {
        return new Context(GAMEMODE_KEY, gameMode);
    }

    @Override
    public int compareTo(@NotNull Context other) {
        int keyCompare = this.key.compareTo(other.key);
        if (keyCompare != 0) {
            return keyCompare;
        }
        return this.value.compareTo(other.value);
    }

    @Override
    public String toString() {
        return key + "=" + value;
    }

    /**
     * Parses a context from a string in the format "key=value".
     *
     * @param str the string to parse
     * @return the parsed context
     * @throws IllegalArgumentException if the string is not in the correct format
     */
    public static Context parse(@NotNull String str) {
        Objects.requireNonNull(str, "str cannot be null");
        int idx = str.indexOf('=');
        if (idx == -1 || idx == 0 || idx == str.length() - 1) {
            throw new IllegalArgumentException("Invalid context format: " + str + ". Expected 'key=value'");
        }
        return new Context(str.substring(0, idx), str.substring(idx + 1));
    }
}
