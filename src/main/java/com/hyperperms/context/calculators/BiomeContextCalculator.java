package com.hyperperms.context.calculators;

import com.hyperperms.api.context.ContextSet;
import com.hyperperms.context.ContextCalculator;
import com.hyperperms.context.PlayerContextProvider;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;
import java.util.UUID;

/**
 * Context calculator that adds the player's current biome as context.
 * <p>
 * Adds context: {@code biome=<biomename>}
 * <p>
 * Biomes represent the environmental type at the player's location, such as
 * forest, desert, ocean, mountains, etc. This enables biome-specific permissions.
 * <p>
 * Example usage in permissions:
 * <pre>
 * /hp user Player permission set build.place biome=desert
 * /hp group Miner permission set build.break.ore biome=mountains
 * /hp user Player permission set entity.spawn.fish biome=ocean
 * </pre>
 * <p>
 * Biome names are normalized to lowercase with spaces replaced by underscores
 * for consistent matching (e.g., "Deep Ocean" becomes "deep_ocean").
 */
public final class BiomeContextCalculator implements ContextCalculator {

    /**
     * The context key for biome contexts.
     */
    public static final String KEY = "biome";

    private final PlayerContextProvider provider;

    /**
     * Creates a new biome context calculator.
     *
     * @param provider the player context provider
     */
    public BiomeContextCalculator(@NotNull PlayerContextProvider provider) {
        this.provider = Objects.requireNonNull(provider, "provider cannot be null");
    }

    @Override
    public void calculate(@NotNull UUID uuid, @NotNull ContextSet.Builder builder) {
        String biome = provider.getBiome(uuid);
        if (biome != null && !biome.isEmpty()) {
            // Normalize biome name: lowercase and replace spaces with underscores
            String normalizedBiome = biome.toLowerCase().replace(' ', '_');
            builder.add(KEY, normalizedBiome);
        }
    }
}
