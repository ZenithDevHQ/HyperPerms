package com.hyperperms.migration;

import org.jetbrains.annotations.NotNull;

import java.util.concurrent.CompletableFuture;

/**
 * Interface for permission system migrators.
 * <p>
 * Each implementation handles migration from a specific permission plugin
 * (e.g., LuckPerms, PermissionsEx) to HyperPerms.
 */
public interface PermissionMigrator {

    /**
     * Gets the name of the source permission system.
     *
     * @return the source name (e.g., "LuckPerms")
     */
    @NotNull
    String getSourceName();

    /**
     * Checks if the source permission system data is available and can be migrated.
     *
     * @return true if migration source is available
     */
    boolean canMigrate();

    /**
     * Gets a description of the detected storage type.
     *
     * @return storage description (e.g., "YAML storage (plugins/LuckPerms/yaml-storage/)")
     */
    @NotNull
    String getStorageDescription();

    /**
     * Generates a preview of the migration without making any changes.
     * This is the "dry-run" mode.
     *
     * @param options migration options
     * @return a preview of what would be migrated
     */
    CompletableFuture<MigrationPreview> preview(@NotNull MigrationOptions options);

    /**
     * Executes the migration.
     * <p>
     * This method should:
     * <ol>
     *   <li>Create a backup of existing HyperPerms data</li>
     *   <li>Perform the migration atomically</li>
     *   <li>Roll back on any failure</li>
     * </ol>
     *
     * @param options migration options
     * @param callback progress callback for status updates
     * @return the migration result
     */
    CompletableFuture<MigrationResult> migrate(@NotNull MigrationOptions options, 
                                                @NotNull MigrationProgressCallback callback);
}
