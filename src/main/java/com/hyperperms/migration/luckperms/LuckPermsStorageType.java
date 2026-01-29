package com.hyperperms.migration.luckperms;

/**
 * Types of LuckPerms storage backends.
 */
public enum LuckPermsStorageType {
    
    /**
     * YAML file storage.
     * Location: plugins/LuckPerms/yaml-storage/
     */
    YAML("YAML files", "plugins/LuckPerms/yaml-storage/"),
    
    /**
     * JSON file storage.
     * Location: plugins/LuckPerms/json-storage/
     */
    JSON("JSON files", "plugins/LuckPerms/json-storage/"),
    
    /**
     * HOCON file storage.
     * Location: plugins/LuckPerms/hocon-storage/
     */
    HOCON("HOCON files", "plugins/LuckPerms/hocon-storage/"),
    
    /**
     * TOML file storage.
     * Location: plugins/LuckPerms/toml-storage/
     */
    TOML("TOML files", "plugins/LuckPerms/toml-storage/"),
    
    /**
     * H2 embedded database.
     * Location: plugins/LuckPerms/luckperms-h2.mv.db
     */
    H2("H2 database", "plugins/LuckPerms/luckperms-h2.mv.db"),
    
    /**
     * SQLite database.
     * Location: plugins/LuckPerms/luckperms-sqlite.db
     */
    SQLITE("SQLite database", "plugins/LuckPerms/luckperms-sqlite.db"),
    
    /**
     * MySQL/MariaDB database.
     * Connection details read from config.yml.
     */
    MYSQL("MySQL/MariaDB database", null),
    
    /**
     * PostgreSQL database.
     * Connection details read from config.yml.
     */
    POSTGRESQL("PostgreSQL database", null),
    
    /**
     * MongoDB.
     * Connection details read from config.yml.
     */
    MONGODB("MongoDB", null),
    
    /**
     * Storage type could not be detected.
     */
    UNKNOWN("Unknown", null);
    
    private final String displayName;
    private final String defaultPath;
    
    LuckPermsStorageType(String displayName, String defaultPath) {
        this.displayName = displayName;
        this.defaultPath = defaultPath;
    }
    
    public String getDisplayName() {
        return displayName;
    }
    
    public String getDefaultPath() {
        return defaultPath;
    }
    
    /**
     * Checks if this storage type uses file-based storage.
     */
    public boolean isFileBased() {
        return this == YAML || this == JSON || this == HOCON || this == TOML;
    }
    
    /**
     * Checks if this storage type uses an embedded database.
     */
    public boolean isEmbeddedDatabase() {
        return this == H2 || this == SQLITE;
    }
    
    /**
     * Checks if this storage type uses a remote database.
     */
    public boolean isRemoteDatabase() {
        return this == MYSQL || this == POSTGRESQL || this == MONGODB;
    }
}
