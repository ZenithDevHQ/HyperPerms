package com.hyperperms.context.calculators;

import com.hyperperms.api.context.ContextSet;
import com.hyperperms.context.ContextCalculator;
import com.hyperperms.context.PlayerContextProvider;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;
import java.util.UUID;

/**
 * Context calculator that adds the player's current region/zone as context.
 * <p>
 * Adds context: {@code region=<regionname>}
 * <p>
 * Regions are typically defined by protection plugins, server configuration,
 * or Hytale's built-in zone system (when available).
 * <p>
 * Example usage in permissions:
 * <pre>
 * /hp user Player permission set build.place region=spawn
 * /hp group Builder permission set build.* region=creative_area
 * /hp user Player permission set teleport.use region=hub
 * </pre>
 * <p>
 * This calculator integrates with any region/zone system that provides region data
 * through the {@link PlayerContextProvider}. If no region system is available or
 * the player is not in a defined region, no context is added.
 */
public final class RegionContextCalculator implements ContextCalculator {

    /**
     * The context key for region contexts.
     */
    public static final String KEY = "region";

    private final PlayerContextProvider provider;

    /**
     * Creates a new region context calculator.
     *
     * @param provider the player context provider
     */
    public RegionContextCalculator(@NotNull PlayerContextProvider provider) {
        this.provider = Objects.requireNonNull(provider, "provider cannot be null");
    }

    @Override
    public void calculate(@NotNull UUID uuid, @NotNull ContextSet.Builder builder) {
        String region = provider.getRegion(uuid);
        if (region != null && !region.isEmpty()) {
            // Normalize region name: lowercase and replace spaces with underscores
            String normalizedRegion = region.toLowerCase().replace(' ', '_');
            builder.add(KEY, normalizedRegion);
        }
    }
}
