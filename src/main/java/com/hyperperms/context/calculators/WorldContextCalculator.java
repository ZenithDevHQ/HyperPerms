package com.hyperperms.context.calculators;

import com.hyperperms.api.context.ContextSet;
import com.hyperperms.context.ContextCalculator;
import com.hyperperms.context.PlayerContextProvider;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;
import java.util.UUID;

/**
 * Context calculator that adds the player's current world as context.
 * <p>
 * Adds context: {@code world=<worldname>}
 * <p>
 * Example usage in permissions:
 * <pre>
 * /hp user Player permission set some.permission world=nether
 * </pre>
 */
public final class WorldContextCalculator implements ContextCalculator {

    /**
     * The context key for world contexts.
     */
    public static final String KEY = "world";

    private final PlayerContextProvider provider;

    /**
     * Creates a new world context calculator.
     *
     * @param provider the player context provider
     */
    public WorldContextCalculator(@NotNull PlayerContextProvider provider) {
        this.provider = Objects.requireNonNull(provider, "provider cannot be null");
    }

    @Override
    public void calculate(@NotNull UUID uuid, @NotNull ContextSet.Builder builder) {
        String world = provider.getWorld(uuid);
        if (world != null && !world.isEmpty()) {
            builder.add(KEY, world.toLowerCase());
        }
    }
}
