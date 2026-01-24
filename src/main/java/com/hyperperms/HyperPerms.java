package com.hyperperms;

import com.hyperperms.api.HyperPermsAPI;
import com.hyperperms.api.PermissionCheckBuilder;
import com.hyperperms.api.context.ContextSet;
import com.hyperperms.api.events.EventBus;
import com.hyperperms.api.events.PermissionCheckEvent;
import com.hyperperms.cache.CacheInvalidator;
import com.hyperperms.cache.PermissionCache;
import com.hyperperms.resolver.PermissionTrace;
import com.hyperperms.config.HyperPermsConfig;
import com.hyperperms.context.ContextManager;
import com.hyperperms.context.PlayerContextProvider;
import com.hyperperms.context.calculators.BiomeContextCalculator;
import com.hyperperms.context.calculators.GameModeContextCalculator;
import com.hyperperms.context.calculators.RegionContextCalculator;
import com.hyperperms.context.calculators.ServerContextCalculator;
import com.hyperperms.context.calculators.TimeContextCalculator;
import com.hyperperms.context.calculators.WorldContextCalculator;
import com.hyperperms.integration.FactionIntegration;
import com.hyperperms.integration.WerChatIntegration;
import com.hyperperms.update.UpdateChecker;
import com.hyperperms.registry.PermissionRegistry;
import com.hyperperms.manager.GroupManagerImpl;
import com.hyperperms.manager.TrackManagerImpl;
import com.hyperperms.manager.UserManagerImpl;
import com.hyperperms.model.User;
import com.hyperperms.resolver.PermissionResolver;
import com.hyperperms.resolver.WildcardMatcher.TriState;
import com.hyperperms.storage.StorageFactory;
import com.hyperperms.storage.StorageProvider;
import com.hyperperms.task.ExpiryCleanupTask;
import com.hyperperms.util.Logger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.nio.file.Path;
import java.util.UUID;
import java.util.concurrent.*;
import java.util.logging.Level;

/**
 * Main plugin class for HyperPerms.
 * <p>
 * This is the entry point for the plugin. Use {@link #getApi()} to access the API.
 */
public final class HyperPerms implements HyperPermsAPI {

    public static final String VERSION = BuildInfo.VERSION;
    
    private static HyperPerms instance;

    private final Path dataDirectory;
    private final java.util.logging.Logger parentLogger;

    // Core components
    private HyperPermsConfig config;
    private StorageProvider storage;
    private PermissionCache cache;
    private CacheInvalidator cacheInvalidator;
    private PermissionResolver resolver;
    private EventBus eventBus;
    private ContextManager contextManager;
    private PlayerContextProvider playerContextProvider;
    private com.hyperperms.registry.PermissionRegistry permissionRegistry;

    // Chat system
    private com.hyperperms.chat.ChatManager chatManager;

    // Tab list system
    private com.hyperperms.tablist.TabListManager tabListManager;
    
    // Faction integration (optional - soft dependency on HyFactions)
    @Nullable
    private FactionIntegration factionIntegration;
    
    // WerChat integration (optional - soft dependency on WerChat)
    @Nullable
    private WerChatIntegration werchatIntegration;

    // Web editor
    private com.hyperperms.web.WebEditorService webEditorService;

    // Backup system
    private com.hyperperms.backup.BackupManager backupManager;

    // Update checker
    @Nullable
    private UpdateChecker updateChecker;

    // Managers
    private UserManagerImpl userManager;
    private GroupManagerImpl groupManager;
    private TrackManagerImpl trackManager;

    // Tasks
    private ScheduledExecutorService scheduler;
    private ScheduledFuture<?> expiryTask;

    // State
    private volatile boolean enabled = false;
    private volatile boolean verboseMode = false;

    /**
     * Creates a new HyperPerms instance.
     *
     * @param dataDirectory the plugin data directory
     * @param parentLogger  the parent logger
     */
    public HyperPerms(@NotNull Path dataDirectory, @NotNull java.util.logging.Logger parentLogger) {
        this.dataDirectory = dataDirectory;
        this.parentLogger = parentLogger;
    }

    /**
     * Gets the API instance.
     *
     * @return the API, or null if not enabled
     */
    @Nullable
    public static HyperPermsAPI getApi() {
        return instance;
    }

    /**
     * Gets the plugin instance.
     *
     * @return the instance, or null if not enabled
     */
    @Nullable
    public static HyperPerms getInstance() {
        return instance;
    }

    /**
     * Enables the plugin.
     */
    public void enable() {
        if (enabled) {
            return;
        }

        long startTime = System.currentTimeMillis();
        instance = this;

        try {
            // Initialize logger
            Logger.init(parentLogger);
            Logger.info("Enabling HyperPerms...");

            // Load configuration
            config = new HyperPermsConfig(dataDirectory);
            config.load();

            // Initialize storage
            storage = StorageFactory.createStorage(config, dataDirectory);
            storage.init().join();

            // Initialize cache
            cache = new PermissionCache(
                    config.getCacheMaxSize(),
                    config.getCacheExpirySeconds(),
                    config.isCacheEnabled()
            );
            cacheInvalidator = new CacheInvalidator(cache);

            // Initialize event bus
            eventBus = new EventBus();

            // Initialize managers
            groupManager = new GroupManagerImpl(storage, cache);
            trackManager = new TrackManagerImpl(storage);
            userManager = new UserManagerImpl(storage, cache, config.getDefaultGroup());

            // Load data
            groupManager.loadAll().join();
            trackManager.loadAll().join();
            userManager.loadAll().join();

            // Load default groups on first run if no groups exist
            if (groupManager.getLoadedGroups().isEmpty()) {
                loadDefaultGroups();
            }

            // Ensure default group exists (fallback if default-groups.json missing)
            if (config.shouldCreateDefaultGroup()) {
                groupManager.ensureDefaultGroup(config.getDefaultGroup());
            }

            // Initialize resolver
            resolver = new PermissionResolver(groupManager::getGroup);

            // Initialize context system
            contextManager = new ContextManager();
            playerContextProvider = PlayerContextProvider.EMPTY; // Will be set by platform
            registerDefaultContextCalculators();

            // Initialize permission registry
            permissionRegistry = com.hyperperms.registry.PermissionRegistry.getInstance();
            permissionRegistry.registerBuiltInPermissions();

            // Initialize chat manager
            chatManager = new com.hyperperms.chat.ChatManager(this);
            chatManager.loadConfig();

            // Initialize tab list manager
            tabListManager = new com.hyperperms.tablist.TabListManager(this);
            tabListManager.loadConfig();

            // Initialize faction integration (soft dependency on HyFactions)
            factionIntegration = new FactionIntegration(this);
            factionIntegration.setEnabled(config.isFactionIntegrationEnabled());
            factionIntegration.setNoFactionDefault(config.getFactionNoFactionDefault());
            factionIntegration.setNoRankDefault(config.getFactionNoRankDefault());
            factionIntegration.setFactionFormat(config.getFactionFormat());
            factionIntegration.setPrefixEnabled(config.isFactionPrefixEnabled());
            factionIntegration.setPrefixFormat(config.getFactionPrefixFormat());
            factionIntegration.setShowRank(config.isFactionShowRank());
            factionIntegration.setPrefixWithRankFormat(config.getFactionPrefixWithRankFormat());
            chatManager.setFactionIntegration(factionIntegration);
            
            // Initialize WerChat integration (soft dependency on WerChat)
            werchatIntegration = new WerChatIntegration(this);
            werchatIntegration.setEnabled(config.isWerChatIntegrationEnabled());
            werchatIntegration.setNoChannelDefault(config.getWerChatNoChannelDefault());
            werchatIntegration.setChannelFormat(config.getWerChatChannelFormat());
            chatManager.setWerChatIntegration(werchatIntegration);

            // Initialize web editor service
            webEditorService = new com.hyperperms.web.WebEditorService(this);

            // Initialize backup manager
            backupManager = new com.hyperperms.backup.BackupManager(this);
            backupManager.start();

            // Initialize update checker
            if (config.isUpdateCheckEnabled()) {
                updateChecker = new UpdateChecker(this, VERSION, config.getUpdateCheckUrl());
                // Check for updates asynchronously
                updateChecker.checkForUpdates().thenAccept(info -> {
                    if (info != null) {
                        Logger.info("[Update] A new version is available: v%s (current: v%s)", info.version(), VERSION);
                        if (config.isUpdateChangelogEnabled() && info.changelog() != null && !info.changelog().isEmpty()) {
                            Logger.info("[Update] Changelog: %s", info.changelog());
                        }
                    }
                });
            }

            // Start scheduled tasks
            scheduler = Executors.newSingleThreadScheduledExecutor(r -> {
                Thread t = new Thread(r, "HyperPerms-Scheduler");
                t.setDaemon(true);
                return t;
            });

            expiryTask = scheduler.scheduleAtFixedRate(
                    new ExpiryCleanupTask(userManager, groupManager, eventBus),
                    config.getExpiryCheckInterval(),
                    config.getExpiryCheckInterval(),
                    TimeUnit.SECONDS
            );

            // Set verbose mode
            verboseMode = config.isVerboseEnabledByDefault();

            enabled = true;
            long elapsed = System.currentTimeMillis() - startTime;
            Logger.info("HyperPerms enabled in %dms", elapsed);

        } catch (Exception e) {
            Logger.severe("Failed to enable HyperPerms", e);
            disable();
            throw new RuntimeException("Failed to enable HyperPerms", e);
        }
    }

    /**
     * Disables the plugin.
     */
    public void disable() {
        if (!enabled && instance == null) {
            return;
        }

        Logger.info("Disabling HyperPerms...");

        // Stop backup manager
        if (backupManager != null) {
            backupManager.shutdown();
        }

        // Stop scheduled tasks
        if (expiryTask != null) {
            expiryTask.cancel(false);
        }
        if (scheduler != null) {
            scheduler.shutdown();
            try {
                scheduler.awaitTermination(5, TimeUnit.SECONDS);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }

        // Save all data
        if (userManager != null) {
            try {
                userManager.saveAll().get(10, TimeUnit.SECONDS);
            } catch (Exception e) {
                Logger.warn("Failed to save users on shutdown");
            }
        }

        // Shutdown storage
        if (storage != null) {
            try {
                storage.shutdown().get(5, TimeUnit.SECONDS);
            } catch (Exception e) {
                Logger.warn("Failed to shutdown storage cleanly");
            }
        }

        // Clear event bus
        if (eventBus != null) {
            eventBus.clear();
        }

        enabled = false;
        instance = null;
        Logger.info("HyperPerms disabled");
    }

    /**
     * Reloads the plugin configuration and data.
     */
    public void reload() {
        Logger.info("Reloading HyperPerms...");

        // Reload config
        config.reload();

        // Clear caches
        cache.invalidateAll();

        // Reload data
        groupManager.loadAll().join();
        trackManager.loadAll().join();

        // Update cache settings
        cache.setEnabled(config.isCacheEnabled());

        Logger.info("HyperPerms reloaded");
    }

    // ==================== HyperPermsAPI Implementation ====================

    @Override
    public boolean hasPermission(@NotNull UUID uuid, @NotNull String permission) {
        return hasPermission(uuid, permission, ContextSet.empty());
    }

    @Override
    public boolean hasPermission(@NotNull UUID uuid, @NotNull String permission, @NotNull ContextSet contexts) {
        // Try cache first
        var cachedPerms = cache.get(uuid, contexts);
        if (cachedPerms != null) {
            if (verboseMode) {
                PermissionTrace trace = cachedPerms.checkWithTrace(permission);
                fireCheckEvent(uuid, permission, contexts, trace.result(), trace);
                return trace.result().asBoolean();
            } else {
                TriState result = cachedPerms.check(permission);
                fireCheckEvent(uuid, permission, contexts, result, null);
                return result.asBoolean();
            }
        }

        // Load user from memory, or from storage if not yet loaded
        User user = userManager.getUser(uuid);
        if (user == null) {
            // User not in memory - load from storage synchronously
            // This ensures we get the correct permissions even if called before async load completes
            var loadResult = userManager.loadUser(uuid).join();
            if (loadResult.isPresent()) {
                user = loadResult.get();
            } else {
                user = userManager.getOrCreateUser(uuid);
            }
        }

        var resolved = resolver.resolve(user, contexts);
        cache.put(uuid, contexts, resolved);

        if (verboseMode) {
            PermissionTrace trace = resolved.checkWithTrace(permission);
            fireCheckEvent(uuid, permission, contexts, trace.result(), trace);
            return trace.result().asBoolean();
        } else {
            TriState result = resolved.check(permission);
            fireCheckEvent(uuid, permission, contexts, result, null);
            return result.asBoolean();
        }
    }

    /**
     * Creates a permission check builder for fluent permission checks with contexts.
     * <p>
     * Example usage:
     * <pre>
     * boolean canBuild = HyperPerms.getInstance()
     *     .check(playerUuid)
     *     .permission("build.place")
     *     .inWorld("nether")
     *     .withGamemode("survival")
     *     .result();
     * </pre>
     *
     * @param uuid the player UUID
     * @return a new permission check builder
     */
    @NotNull
    public PermissionCheckBuilder check(@NotNull UUID uuid) {
        return new PermissionCheckBuilder(this, uuid);
    }

    private void fireCheckEvent(UUID uuid, String permission, ContextSet contexts, TriState result,
                                 PermissionTrace trace) {
        if (verboseMode) {
            if (trace != null) {
                Logger.debug("Permission check: %s has %s = %s (from %s via %s)",
                        uuid, permission, result, trace.getSourceDescription(), trace.matchType());
            } else {
                Logger.debug("Permission check: %s has %s = %s", uuid, permission, result);
            }
        }
        eventBus.fire(new PermissionCheckEvent(uuid, permission, contexts, result, "resolver", trace));
    }

    @Override
    @NotNull
    public UserManager getUserManager() {
        return userManager;
    }

    @Override
    @NotNull
    public GroupManager getGroupManager() {
        return groupManager;
    }

    @Override
    @NotNull
    public TrackManager getTrackManager() {
        return trackManager;
    }

    @Override
    @NotNull
    public EventBus getEventBus() {
        return eventBus;
    }

    @Override
    @NotNull
    public ContextSet getContexts(@NotNull UUID uuid) {
        return contextManager.getContexts(uuid);
    }

    // ==================== Accessors ====================

    /**
     * Gets the configuration.
     *
     * @return the config
     */
    @NotNull
    public HyperPermsConfig getConfig() {
        return config;
    }

    /**
     * Gets the storage provider.
     *
     * @return the storage
     */
    @NotNull
    public StorageProvider getStorage() {
        return storage;
    }

    /**
     * Gets the permission cache.
     *
     * @return the cache
     */
    @NotNull
    public PermissionCache getCache() {
        return cache;
    }

    /**
     * Gets the cache invalidator.
     *
     * @return the cache invalidator
     */
    @NotNull
    public CacheInvalidator getCacheInvalidator() {
        return cacheInvalidator;
    }

    /**
     * Gets the permission resolver.
     *
     * @return the resolver
     */
    @NotNull
    public PermissionResolver getResolver() {
        return resolver;
    }

    /**
     * Checks if verbose mode is enabled.
     *
     * @return true if verbose
     */
    public boolean isVerboseMode() {
        return verboseMode;
    }

    /**
     * Sets verbose mode.
     *
     * @param verbose true to enable
     */
    public void setVerboseMode(boolean verbose) {
        this.verboseMode = verbose;
    }

    /**
     * Checks if the plugin is enabled.
     *
     * @return true if enabled
     */
    public boolean isEnabled() {
        return enabled;
    }

    /**
     * Gets the context manager.
     *
     * @return the context manager
     */
    @NotNull
    public ContextManager getContextManager() {
        return contextManager;
    }

    /**
     * Gets the permission registry.
     * <p>
     * The permission registry tracks all registered permissions from HyperPerms
     * and external plugins, with descriptions and categories.
     *
     * @return the permission registry
     */
    @NotNull
    public com.hyperperms.registry.PermissionRegistry getPermissionRegistry() {
        return permissionRegistry;
    }

    /**
     * Sets the player context provider.
     * <p>
     * This should be called by the platform adapter to provide
     * player-specific context data like world and game mode.
     *
     * @param provider the player context provider
     */
    public void setPlayerContextProvider(@NotNull PlayerContextProvider provider) {
        this.playerContextProvider = provider;
        // Re-register calculators with new provider
        contextManager.clear();
        registerDefaultContextCalculators();
    }

    /**
     * Registers the default context calculators.
     */
    private void registerDefaultContextCalculators() {
        // World context
        contextManager.registerCalculator(new WorldContextCalculator(playerContextProvider));

        // Game mode context
        contextManager.registerCalculator(new GameModeContextCalculator(playerContextProvider));

        // Time context (day/night/dawn/dusk)
        contextManager.registerCalculator(new com.hyperperms.context.calculators.TimeContextCalculator(playerContextProvider));

        // Biome context
        contextManager.registerCalculator(new com.hyperperms.context.calculators.BiomeContextCalculator(playerContextProvider));

        // Region context
        contextManager.registerCalculator(new com.hyperperms.context.calculators.RegionContextCalculator(playerContextProvider));

        // Server context (only if configured)
        String serverName = config.getServerName();
        if (!serverName.isEmpty()) {
            contextManager.registerCalculator(new ServerContextCalculator(serverName));
        }

        Logger.debug("Registered %d context calculators", contextManager.getCalculatorCount());
    }

    /**
     * Loads default groups from the default-groups.json resource.
     * <p>
     * This is called on first run when no groups exist in storage.
     * It creates a standard group hierarchy: default -> member -> builder -> moderator -> admin -> owner
     */
    private void loadDefaultGroups() {
        Logger.info("No groups found, loading default groups...");
        
        try (var inputStream = getClass().getClassLoader().getResourceAsStream("default-groups.json")) {
            if (inputStream == null) {
                Logger.warn("default-groups.json not found in resources, skipping default group creation");
                return;
            }

            String json = new String(inputStream.readAllBytes(), java.nio.charset.StandardCharsets.UTF_8);
            com.google.gson.JsonObject root = com.google.gson.JsonParser.parseString(json).getAsJsonObject();
            com.google.gson.JsonObject groups = root.getAsJsonObject("groups");

            if (groups == null) {
                Logger.warn("No 'groups' object found in default-groups.json");
                return;
            }

            int created = 0;
            for (var entry : groups.entrySet()) {
                String groupName = entry.getKey();
                com.google.gson.JsonObject groupData = entry.getValue().getAsJsonObject();

                // Create the group
                com.hyperperms.model.Group group = groupManager.createGroup(groupName);

                // Set weight
                if (groupData.has("weight")) {
                    group.setWeight(groupData.get("weight").getAsInt());
                }

                // Set prefix
                if (groupData.has("prefix")) {
                    group.setPrefix(groupData.get("prefix").getAsString());
                }

                // Set suffix
                if (groupData.has("suffix")) {
                    group.setSuffix(groupData.get("suffix").getAsString());
                }

                // Add permissions
                if (groupData.has("permissions")) {
                    for (var perm : groupData.getAsJsonArray("permissions")) {
                        group.addNode(com.hyperperms.model.Node.builder(perm.getAsString()).build());
                    }
                }

                // Add parent groups (will be resolved after all groups are created)
                if (groupData.has("parents")) {
                    for (var parent : groupData.getAsJsonArray("parents")) {
                        group.addParent(parent.getAsString());
                    }
                }

                // Save the group
                groupManager.saveGroup(group).join();
                created++;
                Logger.debug("Created default group: %s (weight=%d)", groupName, group.getWeight());
            }

            Logger.info("Loaded %d default groups from default-groups.json", created);

        } catch (Exception e) {
            Logger.warn("Failed to load default groups: %s", e.getMessage());
            Logger.debug("Stack trace: ", e);
        }
    }

    /**
     * Gets the chat manager.
     * <p>
     * The chat manager handles prefix/suffix resolution and chat formatting.
     *
     * @return the chat manager, or null if not yet initialized
     */
    @Nullable
    public com.hyperperms.chat.ChatManager getChatManager() {
        return chatManager;
    }

    /**
     * Gets the tab list manager.
     * <p>
     * The tab list manager handles tab list name formatting.
     *
     * @return the tab list manager, or null if not yet initialized
     */
    @Nullable
    public com.hyperperms.tablist.TabListManager getTabListManager() {
        return tabListManager;
    }

    /**
     * Gets the faction integration.
     * <p>
     * The faction integration provides HyFactions support for chat placeholders.
     * Returns null if HyFactions is not installed.
     *
     * @return the faction integration, or null if HyFactions is not available
     */
    @Nullable
    public FactionIntegration getFactionIntegration() {
        return factionIntegration;
    }

    /**
     * Gets the WerChat integration.
     * <p>
     * The WerChat integration provides WerChat support for chat channel placeholders.
     * Returns null if WerChat is not installed.
     *
     * @return the WerChat integration, or null if WerChat is not available
     */
    @Nullable
    public WerChatIntegration getWerChatIntegration() {
        return werchatIntegration;
    }

    /**
     * Gets the backup manager.
     * <p>
     * The backup manager handles automatic and manual backups.
     *
     * @return the backup manager, or null if not yet initialized
     */
    @Nullable
    public com.hyperperms.backup.BackupManager getBackupManager() {
        return backupManager;
    }


    /**
     * Gets the web editor service.
     * <p>
     * The web editor service handles communication with the remote web editor.
     *
     * @return the web editor service, or null if not yet initialized
     */
    @Nullable
    public com.hyperperms.web.WebEditorService getWebEditorService() {
        return webEditorService;
    }


    /**
     * Gets the plugin version.
     *
     * @return the current plugin version
     */
    @NotNull
    public String getVersion() {
        return VERSION;
    }

    /**
     * Gets the update checker.
     * <p>
     * The update checker handles checking for and downloading plugin updates.
     *
     * @return the update checker, or null if update checking is disabled
     */
    @Nullable
    public UpdateChecker getUpdateChecker() {
        return updateChecker;
    }

    /**
     * Gets the plugin data directory.
     *
     * @return the data directory path
     */
    @NotNull
    public Path getDataDirectory() {
        return dataDirectory;
    }
}
