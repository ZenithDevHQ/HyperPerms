package com.hyperperms.context;

import com.hyperperms.api.context.ContextSet;
import org.jetbrains.annotations.NotNull;

import java.util.UUID;

/**
 * Interface for calculating player contexts.
 * <p>
 * Context calculators determine the current contextual state of a player,
 * such as which world they are in or what gamemode they are using.
 * <p>
 * Implementations should be registered with the {@link ContextManager}.
 */
@FunctionalInterface
public interface ContextCalculator {

    /**
     * Calculates contexts for a player and adds them to the builder.
     *
     * @param uuid    the player's UUID
     * @param builder the context set builder to add contexts to
     */
    void calculate(@NotNull UUID uuid, @NotNull ContextSet.Builder builder);
}
