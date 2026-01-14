package com.hyperperms;

import com.hyperperms.api.HyperPermsAPI;
import com.hyperperms.api.context.ContextSet;
import com.hyperperms.api.events.EventBus;
import com.hyperperms.api.events.PermissionCheckEvent;
import com.hyperperms.cache.CacheInvalidator;
import com.hyperperms.cache.PermissionCache;
import com.hyperperms.resolver.PermissionTrace;
import com.hyperperms.config.HyperPermsConfig;
import com.hyperperms.context.ContextManager;
import com.hyperperms.context.PlayerContextProvider;
import com.hyperperms.context.calculators.GameModeContextCalculator;
import com.hyperperms.context.calculators.ServerContextCalculator;
import com.hyperperms.context.calculators.WorldContextCalculator;
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

            // Ensure default group exists
            if (config.shouldCreateDefaultGroup()) {
                groupManager.ensureDefaultGroup(config.getDefaultGroup());
            }

            // Initialize resolver
            resolver = new PermissionResolver(groupManager::getGroup);

            // Initialize context system
            contextManager = new ContextManager();
            playerContextProvider = PlayerContextProvider.EMPTY; // Will be set by platform
            registerDefaultContextCalculators();

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

        // Load user and resolve
        User user = userManager.getOrCreateUser(uuid);
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

        // Server context (only if configured)
        String serverName = config.getServerName();
        if (!serverName.isEmpty()) {
            contextManager.registerCalculator(new ServerContextCalculator(serverName));
        }

        Logger.debug("Registered %d context calculators", contextManager.getCalculatorCount());
    }
}
