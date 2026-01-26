package com.hyperperms.web;

import org.jetbrains.annotations.NotNull;

import java.util.List;

/**
 * Data Transfer Object representing a plugin's permission definitions.
 * Used to send installed plugin permissions to the web editor.
 */
public record PluginPermissions(
    @NotNull String pluginName,
    @NotNull String version,
    @NotNull List<PermissionInfo> permissions
) {
    /**
     * Information about a single permission node.
     */
    public record PermissionInfo(
        @NotNull String node,
        @NotNull String description,
        @NotNull String defaultValue
    ) {}
}
