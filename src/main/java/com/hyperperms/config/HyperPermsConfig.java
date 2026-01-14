package com.hyperperms.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.hyperperms.util.Logger;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

/**
 * Main configuration wrapper for HyperPerms.
 */
public final class HyperPermsConfig {

    private static final String CONFIG_FILE = "config.json";
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    private final Path configFile;
    private JsonObject config;

    public HyperPermsConfig(@NotNull Path dataDirectory) {
        this.configFile = dataDirectory.resolve(CONFIG_FILE);
    }

    /**
     * Loads the configuration from disk, creating defaults if necessary.
     */
    public void load() {
        try {
            if (!Files.exists(configFile)) {
                saveDefaultConfig();
            }
            String json = Files.readString(configFile);
            config = GSON.fromJson(json, JsonObject.class);
            if (config == null) {
                config = createDefaultConfig();
            }
            Logger.info("Configuration loaded");
        } catch (IOException e) {
            Logger.severe("Failed to load configuration", e);
            config = createDefaultConfig();
        }
    }

    /**
     * Reloads the configuration from disk.
     */
    public void reload() {
        load();
    }

    /**
     * Saves the current configuration to disk.
     */
    public void save() {
        try {
            Files.createDirectories(configFile.getParent());
            Files.writeString(configFile, GSON.toJson(config));
        } catch (IOException e) {
            Logger.severe("Failed to save configuration", e);
        }
    }

    private void saveDefaultConfig() throws IOException {
        Files.createDirectories(configFile.getParent());
        Files.writeString(configFile, GSON.toJson(createDefaultConfig()));
        Logger.info("Created default configuration file");
    }

    private JsonObject createDefaultConfig() {
        JsonObject root = new JsonObject();

        // Storage settings
        JsonObject storage = new JsonObject();
        storage.addProperty("type", "json");

        JsonObject jsonSettings = new JsonObject();
        jsonSettings.addProperty("directory", "data");
        storage.add("json", jsonSettings);

        JsonObject sqliteSettings = new JsonObject();
        sqliteSettings.addProperty("file", "hyperperms.db");
        storage.add("sqlite", sqliteSettings);

        JsonObject mysqlSettings = new JsonObject();
        mysqlSettings.addProperty("host", "localhost");
        mysqlSettings.addProperty("port", 3306);
        mysqlSettings.addProperty("database", "hyperperms");
        mysqlSettings.addProperty("username", "root");
        mysqlSettings.addProperty("password", "");
        mysqlSettings.addProperty("poolSize", 10);
        storage.add("mysql", mysqlSettings);

        root.add("storage", storage);

        // Cache settings
        JsonObject cache = new JsonObject();
        cache.addProperty("enabled", true);
        cache.addProperty("expirySeconds", 300);
        cache.addProperty("maxSize", 10000);
        root.add("cache", cache);

        // Default settings
        JsonObject defaults = new JsonObject();
        defaults.addProperty("group", "default");
        defaults.addProperty("createDefaultGroup", true);
        root.add("defaults", defaults);

        // Task settings
        JsonObject tasks = new JsonObject();
        tasks.addProperty("expiryCheckIntervalSeconds", 60);
        tasks.addProperty("autoSaveIntervalSeconds", 300);
        root.add("tasks", tasks);

        // Verbose settings
        JsonObject verbose = new JsonObject();
        verbose.addProperty("enabledByDefault", false);
        verbose.addProperty("logToConsole", true);
        root.add("verbose", verbose);

        // Server settings (for context)
        JsonObject server = new JsonObject();
        server.addProperty("name", "");
        root.add("server", server);

        return root;
    }

    // ==================== Getters ====================

    @NotNull
    public String getStorageType() {
        return getNestedString("storage", "type", "json");
    }

    @NotNull
    public String getJsonDirectory() {
        return getNestedString("storage", "json", "directory", "data");
    }

    @NotNull
    public String getSqliteFile() {
        return getNestedString("storage", "sqlite", "file", "hyperperms.db");
    }

    @NotNull
    public String getMysqlHost() {
        return getNestedString("storage", "mysql", "host", "localhost");
    }

    public int getMysqlPort() {
        return getNestedInt("storage", "mysql", "port", 3306);
    }

    @NotNull
    public String getMysqlDatabase() {
        return getNestedString("storage", "mysql", "database", "hyperperms");
    }

    @NotNull
    public String getMysqlUsername() {
        return getNestedString("storage", "mysql", "username", "root");
    }

    @NotNull
    public String getMysqlPassword() {
        return getNestedString("storage", "mysql", "password", "");
    }

    public int getMysqlPoolSize() {
        return getNestedInt("storage", "mysql", "poolSize", 10);
    }

    public boolean isCacheEnabled() {
        return getNestedBoolean("cache", "enabled", true);
    }

    public int getCacheExpirySeconds() {
        return getNestedInt("cache", "expirySeconds", 300);
    }

    public int getCacheMaxSize() {
        return getNestedInt("cache", "maxSize", 10000);
    }

    @NotNull
    public String getDefaultGroup() {
        return getNestedString("defaults", "group", "default");
    }

    public boolean shouldCreateDefaultGroup() {
        return getNestedBoolean("defaults", "createDefaultGroup", true);
    }

    public int getExpiryCheckInterval() {
        return getNestedInt("tasks", "expiryCheckIntervalSeconds", 60);
    }

    public int getAutoSaveInterval() {
        return getNestedInt("tasks", "autoSaveIntervalSeconds", 300);
    }

    public boolean isVerboseEnabledByDefault() {
        return getNestedBoolean("verbose", "enabledByDefault", false);
    }

    public boolean shouldLogVerboseToConsole() {
        return getNestedBoolean("verbose", "logToConsole", true);
    }

    /**
     * Gets the server name for context-based permissions.
     *
     * @return the server name, or empty string if not configured
     */
    @NotNull
    public String getServerName() {
        return getNestedString("server", "name", "");
    }

    // ==================== Helper Methods ====================

    private String getNestedString(String... path) {
        JsonObject current = config;
        for (int i = 0; i < path.length - 2; i++) {
            if (current.has(path[i]) && current.get(path[i]).isJsonObject()) {
                current = current.getAsJsonObject(path[i]);
            } else {
                return path[path.length - 1];
            }
        }
        String key = path[path.length - 2];
        String defaultValue = path[path.length - 1];
        if (current.has(key) && current.get(key).isJsonPrimitive()) {
            return current.get(key).getAsString();
        }
        return defaultValue;
    }

    private int getNestedInt(String section, String key, int defaultValue) {
        if (config.has(section) && config.get(section).isJsonObject()) {
            JsonObject obj = config.getAsJsonObject(section);
            if (obj.has(key) && obj.get(key).isJsonPrimitive()) {
                return obj.get(key).getAsInt();
            }
        }
        return defaultValue;
    }

    private int getNestedInt(String section, String subsection, String key, int defaultValue) {
        if (config.has(section) && config.get(section).isJsonObject()) {
            JsonObject obj = config.getAsJsonObject(section);
            if (obj.has(subsection) && obj.get(subsection).isJsonObject()) {
                JsonObject sub = obj.getAsJsonObject(subsection);
                if (sub.has(key) && sub.get(key).isJsonPrimitive()) {
                    return sub.get(key).getAsInt();
                }
            }
        }
        return defaultValue;
    }

    private boolean getNestedBoolean(String section, String key, boolean defaultValue) {
        if (config.has(section) && config.get(section).isJsonObject()) {
            JsonObject obj = config.getAsJsonObject(section);
            if (obj.has(key) && obj.get(key).isJsonPrimitive()) {
                return obj.get(key).getAsBoolean();
            }
        }
        return defaultValue;
    }

    private String getNestedString(String section, String key, String defaultValue) {
        if (config.has(section) && config.get(section).isJsonObject()) {
            JsonObject obj = config.getAsJsonObject(section);
            if (obj.has(key) && obj.get(key).isJsonPrimitive()) {
                return obj.get(key).getAsString();
            }
        }
        return defaultValue;
    }

    private String getNestedString(String section, String subsection, String key, String defaultValue) {
        if (config.has(section) && config.get(section).isJsonObject()) {
            JsonObject obj = config.getAsJsonObject(section);
            if (obj.has(subsection) && obj.get(subsection).isJsonObject()) {
                JsonObject sub = obj.getAsJsonObject(subsection);
                if (sub.has(key) && sub.get(key).isJsonPrimitive()) {
                    return sub.get(key).getAsString();
                }
            }
        }
        return defaultValue;
    }
}
