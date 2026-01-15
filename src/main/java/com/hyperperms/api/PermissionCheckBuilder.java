package com.hyperperms.api;

import com.hyperperms.HyperPerms;
import com.hyperperms.api.context.Context;
import com.hyperperms.api.context.ContextSet;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;
import java.util.UUID;

/**
 * Builder pattern for permission checks with custom contexts.
 * <p>
 * This builder provides a fluent API for constructing permission checks
 * with specific contextual requirements.
 * <p>
 * Example usage:
 * <pre>
 * // Check if player can build in the nether while in survival mode
 * boolean canBuild = HyperPerms.getInstance()
 *     .check(playerUuid)
 *     .permission("build.place")
 *     .inWorld("nether")
 *     .withGamemode("survival")
 *     .result();
 *
 * // Check with custom contexts
 * boolean canFly = HyperPerms.getInstance()
 *     .check(playerUuid)
 *     .permission("movement.fly")
 *     .withContext("time", "night")
 *     .withContext("region", "spawn")
 *     .result();
 *
 * // Check multiple permissions at once
 * boolean hasAny = HyperPerms.getInstance()
 *     .check(playerUuid)
 *     .hasAny("admin.bypass", "mod.bypass", "vip.bypass");
 * </pre>
 */
public final class PermissionCheckBuilder {

    private final HyperPermsAPI api;
    private final UUID uuid;
    private String permission;
    private final ContextSet.Builder contextBuilder;
    private boolean usePlayerContexts = true;

    /**
     * Creates a new permission check builder.
     *
     * @param api  the HyperPerms API instance
     * @param uuid the player UUID to check
     */
    public PermissionCheckBuilder(@NotNull HyperPermsAPI api, @NotNull UUID uuid) {
        this.api = Objects.requireNonNull(api, "api");
        this.uuid = Objects.requireNonNull(uuid, "uuid");
        this.contextBuilder = ContextSet.builder();
    }

    /**
     * Sets the permission to check.
     *
     * @param permission the permission node
     * @return this builder
     */
    @NotNull
    public PermissionCheckBuilder permission(@NotNull String permission) {
        this.permission = Objects.requireNonNull(permission, "permission");
        return this;
    }

    /**
     * Adds a world context to the check.
     *
     * @param world the world name
     * @return this builder
     */
    @NotNull
    public PermissionCheckBuilder inWorld(@NotNull String world) {
        contextBuilder.add("world", world.toLowerCase());
        return this;
    }

    /**
     * Adds a game mode context to the check.
     *
     * @param gamemode the game mode (survival, creative, adventure, spectator)
     * @return this builder
     */
    @NotNull
    public PermissionCheckBuilder withGamemode(@NotNull String gamemode) {
        contextBuilder.add("gamemode", gamemode.toLowerCase());
        return this;
    }

    /**
     * Adds a region context to the check.
     *
     * @param region the region name
     * @return this builder
     */
    @NotNull
    public PermissionCheckBuilder inRegion(@NotNull String region) {
        contextBuilder.add("region", region.toLowerCase());
        return this;
    }

    /**
     * Adds a biome context to the check.
     *
     * @param biome the biome name
     * @return this builder
     */
    @NotNull
    public PermissionCheckBuilder inBiome(@NotNull String biome) {
        contextBuilder.add("biome", biome.toLowerCase());
        return this;
    }

    /**
     * Adds a time context to the check.
     *
     * @param time the time period (day, night, dawn, dusk)
     * @return this builder
     */
    @NotNull
    public PermissionCheckBuilder atTime(@NotNull String time) {
        contextBuilder.add("time", time.toLowerCase());
        return this;
    }

    /**
     * Adds a server context to the check.
     *
     * @param server the server name
     * @return this builder
     */
    @NotNull
    public PermissionCheckBuilder onServer(@NotNull String server) {
        contextBuilder.add("server", server.toLowerCase());
        return this;
    }

    /**
     * Adds a custom context to the check.
     *
     * @param key   the context key
     * @param value the context value
     * @return this builder
     */
    @NotNull
    public PermissionCheckBuilder withContext(@NotNull String key, @NotNull String value) {
        contextBuilder.add(key, value);
        return this;
    }

    /**
     * Adds multiple custom contexts to the check.
     *
     * @param contexts the contexts to add
     * @return this builder
     */
    @NotNull
    public PermissionCheckBuilder withContexts(@NotNull ContextSet contexts) {
        for (Context ctx : contexts.toSet()) {
            contextBuilder.add(ctx.key(), ctx.value());
        }
        return this;
    }

    /**
     * Controls whether to include the player's current contexts in the check.
     * <p>
     * By default, the player's current contexts (world, gamemode, etc.) are included.
     * Set this to false to only use explicitly specified contexts.
     *
     * @param use true to include player contexts, false to only use specified contexts
     * @return this builder
     */
    @NotNull
    public PermissionCheckBuilder usePlayerContexts(boolean use) {
        this.usePlayerContexts = use;
        return this;
    }

    /**
     * Executes the permission check and returns the result.
     *
     * @return true if the player has the permission
     * @throws IllegalStateException if no permission was specified
     */
    public boolean result() {
        if (permission == null) {
            throw new IllegalStateException("No permission specified. Call permission() first.");
        }

        ContextSet contexts = buildContexts();
        return api.hasPermission(uuid, permission, contexts);
    }

    /**
     * Checks if the player has any of the specified permissions.
     *
     * @param permissions the permissions to check
     * @return true if the player has at least one of the permissions
     */
    public boolean hasAny(@NotNull String... permissions) {
        if (permissions.length == 0) {
            return false;
        }

        ContextSet contexts = buildContexts();
        for (String perm : permissions) {
            if (api.hasPermission(uuid, perm, contexts)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Checks if the player has all of the specified permissions.
     *
     * @param permissions the permissions to check
     * @return true if the player has all of the permissions
     */
    public boolean hasAll(@NotNull String... permissions) {
        if (permissions.length == 0) {
            return true;
        }

        ContextSet contexts = buildContexts();
        for (String perm : permissions) {
            if (!api.hasPermission(uuid, perm, contexts)) {
                return false;
            }
        }
        return true;
    }

    /**
     * Builds the final context set for the check.
     */
    private ContextSet buildContexts() {
        if (usePlayerContexts) {
            // Start with player's current contexts
            ContextSet playerContexts = api.getContexts(uuid);
            ContextSet.Builder merged = ContextSet.builder();
            
            // Add player contexts first
            for (Context ctx : playerContexts.toSet()) {
                merged.add(ctx.key(), ctx.value());
            }
            
            // Override with explicitly specified contexts
            for (Context ctx : contextBuilder.build().toSet()) {
                merged.add(ctx.key(), ctx.value());
            }
            
            return merged.build();
        } else {
            return contextBuilder.build();
        }
    }

    /**
     * Gets the UUID being checked.
     *
     * @return the player UUID
     */
    @NotNull
    public UUID getUuid() {
        return uuid;
    }

    /**
     * Gets the permission being checked.
     *
     * @return the permission, or null if not set
     */
    public String getPermission() {
        return permission;
    }
}
