package com.hyperperms.storage;

import com.hyperperms.config.HyperPermsConfig;
import com.hyperperms.storage.json.JsonStorageProvider;
import com.hyperperms.util.Logger;
import org.jetbrains.annotations.NotNull;

import java.nio.file.Path;

/**
 * Factory for creating the appropriate {@link StorageProvider} based on configuration.
 */
public final class StorageFactory {

    private StorageFactory() {}

    /**
     * Creates a storage provider based on the configuration.
     *
     * @param config        the plugin configuration
     * @param dataDirectory the plugin's data directory
     * @return the storage provider
     */
    @NotNull
    public static StorageProvider createStorage(@NotNull HyperPermsConfig config, @NotNull Path dataDirectory) {
        String type = config.getStorageType().toLowerCase();

        return switch (type) {
            case "json", "file", "flatfile" -> {
                Path jsonDir = dataDirectory.resolve(config.getJsonDirectory());
                Logger.info("Using JSON storage at: " + jsonDir);
                yield new JsonStorageProvider(jsonDir);
            }
            case "sqlite" -> {
                // TODO: Implement SQLite storage
                Logger.warn("SQLite storage not yet implemented, falling back to JSON");
                Path jsonDir = dataDirectory.resolve(config.getJsonDirectory());
                yield new JsonStorageProvider(jsonDir);
            }
            case "mysql", "mariadb" -> {
                // TODO: Implement MySQL storage
                Logger.warn("MySQL storage not yet implemented, falling back to JSON");
                Path jsonDir = dataDirectory.resolve(config.getJsonDirectory());
                yield new JsonStorageProvider(jsonDir);
            }
            default -> {
                Logger.warn("Unknown storage type '%s', falling back to JSON", type);
                Path jsonDir = dataDirectory.resolve(config.getJsonDirectory());
                yield new JsonStorageProvider(jsonDir);
            }
        };
    }
}
