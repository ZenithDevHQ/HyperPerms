package com.hyperperms.api;

import com.hyperperms.HyperPerms;
import com.hyperperms.chat.PrefixSuffixResolver;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

/**
 * Simple static API for other plugins to get HyperPerms prefix/suffix data.
 * <p>
 * This is designed for integration with chat plugins like WerChat that handle
 * their own message broadcasting and need to include HyperPerms prefixes.
 * <p>
 * Example usage from another plugin (direct or via reflection):
 * <pre>{@code
 * String prefix = ChatAPI.getPrefix(playerUuid);
 * String suffix = ChatAPI.getSuffix(playerUuid);
 * String formatted = prefix + "[Channel] " + playerName + suffix + ": " + message;
 * }</pre>
 */
public final class ChatAPI {
    
    private ChatAPI() {} // Static API only
    
    // Cache for quick synchronous access (populated by async preload)
    private static final Map<UUID, CachedData> cache = new ConcurrentHashMap<>();
    private static final long CACHE_TTL_MS = 30000; // 30 seconds - longer TTL to reduce async fallback calls
    private static final long SYNC_TIMEOUT_MS = 500; // Timeout for synchronous fallback
    
    /**
     * Gets the prefix for a player (synchronous, uses cache).
     * <p>
     * For best results, call {@link #preload(UUID)} when the player joins
     * to ensure the cache is populated.
     *
     * @param uuid the player's UUID
     * @return the prefix string, or empty string if not available
     */
    @NotNull
    public static String getPrefix(@NotNull UUID uuid) {
        CachedData cached = cache.get(uuid);
        if (cached != null && !cached.isExpired()) {
            return cached.prefix != null ? cached.prefix : "";
        }
        
        // Use preload() which loads everything, then return prefix from cache
        // This eliminates the race condition between preload() and getPrefixAsync()
        try {
            preload(uuid).get(SYNC_TIMEOUT_MS, TimeUnit.MILLISECONDS);
            CachedData loadedData = cache.get(uuid);
            return (loadedData != null && loadedData.prefix != null) ? loadedData.prefix : "";
        } catch (Exception e) {
            return "";
        }
    }

    /**
     * Gets the suffix for a player (synchronous, uses cache).
     *
     * @param uuid the player's UUID
     * @return the suffix string, or empty string if not available
     */
    @NotNull
    public static String getSuffix(@NotNull UUID uuid) {
        CachedData cached = cache.get(uuid);
        if (cached != null && !cached.isExpired()) {
            return cached.suffix != null ? cached.suffix : "";
        }
        
        // Use preload() which loads everything, then return suffix from cache
        // This eliminates the race condition between preload() and getSuffixAsync()
        try {
            preload(uuid).get(SYNC_TIMEOUT_MS, TimeUnit.MILLISECONDS);
            CachedData loadedData = cache.get(uuid);
            return (loadedData != null && loadedData.suffix != null) ? loadedData.suffix : "";
        } catch (Exception e) {
            return "";
        }
    }

    /**
     * Gets the primary group name for a player.
     *
     * @param uuid the player's UUID
     * @return the group name, or "default" if not available
     */
    @NotNull
    public static String getPrimaryGroup(@NotNull UUID uuid) {
        CachedData cached = cache.get(uuid);
        if (cached != null && !cached.isExpired()) {
            return cached.primaryGroup != null ? cached.primaryGroup : "default";
        }
        return "default";
    }
    
    /**
     * Gets the prefix asynchronously.
     *
     * @param uuid the player's UUID
     * @return a future containing the prefix
     */
    @NotNull
    public static CompletableFuture<String> getPrefixAsync(@NotNull UUID uuid) {
        HyperPerms hp = HyperPerms.getInstance();
        if (hp == null || hp.getChatManager() == null) {
            return CompletableFuture.completedFuture("");
        }
        
        return hp.getChatManager().getPrefix(uuid)
            .thenApply(prefix -> {
                updateCache(uuid, prefix, null, null);
                return prefix != null ? prefix : "";
            });
    }
    
    /**
     * Gets the suffix asynchronously.
     *
     * @param uuid the player's UUID
     * @return a future containing the suffix
     */
    @NotNull
    public static CompletableFuture<String> getSuffixAsync(@NotNull UUID uuid) {
        HyperPerms hp = HyperPerms.getInstance();
        if (hp == null || hp.getChatManager() == null) {
            return CompletableFuture.completedFuture("");
        }
        
        return hp.getChatManager().getSuffix(uuid)
            .thenApply(suffix -> {
                updateCache(uuid, null, suffix, null);
                return suffix != null ? suffix : "";
            });
    }
    
    /**
     * Preloads prefix/suffix data for a player into the cache.
     * <p>
     * Call this when a player connects to ensure fast synchronous access later.
     *
     * @param uuid the player's UUID
     * @return a future that completes when data is cached
     */
    @NotNull
    public static CompletableFuture<Void> preload(@NotNull UUID uuid) {
        HyperPerms hp = HyperPerms.getInstance();
        if (hp == null || hp.getChatManager() == null) {
            return CompletableFuture.completedFuture(null);
        }
        
        return hp.getChatManager().getDisplayData(uuid)
            .thenAccept(displayData -> {
                cache.put(uuid, new CachedData(
                    displayData.getPrefix(),
                    displayData.getSuffix(),
                    displayData.getPrimaryGroupName()
                ));
            });
    }
    
    /**
     * Invalidates the cache for a player.
     * <p>
     * Call this when a player's permissions change.
     *
     * @param uuid the player's UUID
     */
    public static void invalidate(@NotNull UUID uuid) {
        cache.remove(uuid);
    }
    
    /**
     * Clears all cached data.
     */
    public static void invalidateAll() {
        cache.clear();
    }
    
    /**
     * Checks if HyperPerms is available.
     *
     * @return true if HyperPerms is loaded and enabled
     */
    public static boolean isAvailable() {
        HyperPerms hp = HyperPerms.getInstance();
        return hp != null && hp.isEnabled();
    }
    
    private static void updateCache(UUID uuid, @Nullable String prefix,
                                    @Nullable String suffix, @Nullable String group) {
        CachedData existing = cache.get(uuid);
        if (existing != null && !existing.isExpired()) {
            // Merge with existing - prevents partial cache entries from overwriting full ones
            cache.put(uuid, new CachedData(
                prefix != null ? prefix : existing.prefix,
                suffix != null ? suffix : existing.suffix,
                group != null ? group : existing.primaryGroup
            ));
        } else {
            // Only create new entry if we have at least prefix OR suffix
            // Prevents caching empty data that would cause prefix/suffix to be empty
            if (prefix != null || suffix != null) {
                cache.put(uuid, new CachedData(prefix, suffix, group));
            }
        }
    }
    
    private static class CachedData {
        final String prefix;
        final String suffix;
        final String primaryGroup;
        final long timestamp;
        
        CachedData(String prefix, String suffix, String primaryGroup) {
            this.prefix = prefix;
            this.suffix = suffix;
            this.primaryGroup = primaryGroup;
            this.timestamp = System.currentTimeMillis();
        }
        
        boolean isExpired() {
            return System.currentTimeMillis() - timestamp > CACHE_TTL_MS;
        }
    }
}
