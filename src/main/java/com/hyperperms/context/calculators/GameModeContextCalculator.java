package com.hyperperms.context.calculators;

import com.hyperperms.api.context.ContextSet;
import com.hyperperms.context.ContextCalculator;
import com.hyperperms.context.PlayerContextProvider;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;
import java.util.UUID;

/**
 * Context calculator that adds the player's current game mode as context.
 * <p>
 * Adds context: {@code gamemode=<mode>}
 * <p>
 * Common game modes:
 * <ul>
 *   <li>{@code survival} - Standard survival mode</li>
 *   <li>{@code creative} - Creative mode with unlimited resources</li>
 *   <li>{@code adventure} - Adventure mode with interaction restrictions</li>
 *   <li>{@code spectator} - Spectator mode (if available)</li>
 * </ul>
 * <p>
 * Example usage in permissions:
 * <pre>
 * /hp user Player permission set some.permission gamemode=creative
 * </pre>
 */
public final class GameModeContextCalculator implements ContextCalculator {

    /**
     * The context key for game mode contexts.
     */
    public static final String KEY = "gamemode";

    private final PlayerContextProvider provider;

    /**
     * Creates a new game mode context calculator.
     *
     * @param provider the player context provider
     */
    public GameModeContextCalculator(@NotNull PlayerContextProvider provider) {
        this.provider = Objects.requireNonNull(provider, "provider cannot be null");
    }

    @Override
    public void calculate(@NotNull UUID uuid, @NotNull ContextSet.Builder builder) {
        String gameMode = provider.getGameMode(uuid);
        if (gameMode != null && !gameMode.isEmpty()) {
            builder.add(KEY, gameMode.toLowerCase());
        }
    }
}
