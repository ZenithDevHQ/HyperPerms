package com.hyperperms.api;

import com.hyperperms.HyperPerms;
import com.hyperperms.tablist.TabListManager;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

/**
 * Simple static API for other plugins to get HyperPerms tab list data.
 * <p>
 * This is designed for integration with plugins that need access to
 * formatted tab list names and group weights for sorting.
 * <p>
 * Example usage from another plugin (direct or via reflection):
 * <pre>{@code
 * String tabListName = TabListAPI.getTabListName(playerUuid);
 * int weight = TabListAPI.getWeight(playerUuid);
 * }</pre>
 */
public final class TabListAPI {

    private TabListAPI() {} // Static API only

    // Cache for quick synchronous access
    private static final Map<UUID, CachedData> cache = new ConcurrentHashMap<>();
    private static final long CACHE_TTL_MS = 10000; // 10 seconds

    /**
     * Gets the formatted tab list name for a player (synchronous, uses cache).
     * <p>
     * For best results, the player should be online so their data is loaded.
     *
     * @param uuid the player's UUID
     * @return the formatted tab list name, or empty string if not available
     */
    @NotNull
    public static String getTabListName(@NotNull UUID uuid) {
        CachedData cached = cache.get(uuid);
        if (cached != null && !cached.isExpired()) {
            return cached.tabListName != null ? cached.tabListName : "";
        }

        // Try to get from manager synchronously (may block briefly)
        try {
            HyperPerms hp = HyperPerms.getInstance();
            if (hp == null || hp.getTabListManager() == null) {
                return "";
            }

            TabListManager manager = hp.getTabListManager();
            var user = hp.getUserManager().getUser(uuid);
            if (user == null) {
                return "";
            }

            // Use formatTabListName with a short timeout
            String name = manager.formatTabListName(uuid, user.getUsername())
                .get(100, TimeUnit.MILLISECONDS);

            updateCache(uuid, name, null);
            return name != null ? name : "";
        } catch (Exception e) {
            return "";
        }
    }

    /**
     * Gets the group weight for a player (for sorting purposes).
     *
     * @param uuid the player's UUID
     * @return the weight, or 0 if not available
     */
    public static int getWeight(@NotNull UUID uuid) {
        CachedData cached = cache.get(uuid);
        if (cached != null && !cached.isExpired() && cached.weight != null) {
            return cached.weight;
        }

        HyperPerms hp = HyperPerms.getInstance();
        if (hp == null || hp.getTabListManager() == null) {
            return 0;
        }

        int weight = hp.getTabListManager().getWeight(uuid);
        updateCache(uuid, null, weight);
        return weight;
    }

    /**
     * Gets the formatted tab list name asynchronously.
     *
     * @param uuid the player's UUID
     * @param playerName the player's display name
     * @return a future containing the formatted name
     */
    @NotNull
    public static CompletableFuture<String> getTabListNameAsync(@NotNull UUID uuid, @NotNull String playerName) {
        HyperPerms hp = HyperPerms.getInstance();
        if (hp == null || hp.getTabListManager() == null) {
            return CompletableFuture.completedFuture("");
        }

        return hp.getTabListManager().formatTabListName(uuid, playerName)
            .thenApply(name -> {
                updateCache(uuid, name, null);
                return name != null ? name : "";
            });
    }

    /**
     * Invalidates the cache for a player.
     * <p>
     * Call this when a player's permissions or groups change.
     *
     * @param uuid the player's UUID
     */
    public static void invalidate(@NotNull UUID uuid) {
        cache.remove(uuid);

        // Also invalidate the manager's cache
        HyperPerms hp = HyperPerms.getInstance();
        if (hp != null && hp.getTabListManager() != null) {
            hp.getTabListManager().invalidateCache(uuid);
        }
    }

    /**
     * Clears all cached data.
     */
    public static void invalidateAll() {
        cache.clear();

        // Also invalidate the manager's caches
        HyperPerms hp = HyperPerms.getInstance();
        if (hp != null && hp.getTabListManager() != null) {
            hp.getTabListManager().invalidateAllCaches();
        }
    }

    /**
     * Checks if tab list formatting is enabled.
     *
     * @return true if tab list formatting is enabled
     */
    public static boolean isEnabled() {
        HyperPerms hp = HyperPerms.getInstance();
        if (hp == null || hp.getTabListManager() == null) {
            return false;
        }
        return hp.getTabListManager().isEnabled();
    }

    /**
     * Checks if HyperPerms tab list API is available.
     *
     * @return true if HyperPerms is loaded, enabled, and has tab list support
     */
    public static boolean isAvailable() {
        HyperPerms hp = HyperPerms.getInstance();
        return hp != null && hp.isEnabled() && hp.getTabListManager() != null;
    }

    private static void updateCache(UUID uuid, @Nullable String tabListName, @Nullable Integer weight) {
        CachedData existing = cache.get(uuid);
        if (existing != null && !existing.isExpired()) {
            // Merge with existing
            cache.put(uuid, new CachedData(
                tabListName != null ? tabListName : existing.tabListName,
                weight != null ? weight : existing.weight
            ));
        } else {
            cache.put(uuid, new CachedData(tabListName, weight));
        }
    }

    private static class CachedData {
        final String tabListName;
        final Integer weight;
        final long timestamp;

        CachedData(String tabListName, Integer weight) {
            this.tabListName = tabListName;
            this.weight = weight;
            this.timestamp = System.currentTimeMillis();
        }

        boolean isExpired() {
            return System.currentTimeMillis() - timestamp > CACHE_TTL_MS;
        }
    }
}
