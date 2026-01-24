package com.hyperperms.manager;

import com.hyperperms.api.HyperPermsAPI.UserManager;
import com.hyperperms.cache.PermissionCache;
import com.hyperperms.model.User;
import com.hyperperms.storage.StorageProvider;
import com.hyperperms.util.Logger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

/**
 * Implementation of the user manager.
 */
public final class UserManagerImpl implements UserManager {

    private final StorageProvider storage;
    private final PermissionCache cache;
    private final Map<UUID, User> loadedUsers = new ConcurrentHashMap<>();
    private final Map<UUID, Object> userLocks = new ConcurrentHashMap<>();
    private final String defaultGroup;

    public UserManagerImpl(@NotNull StorageProvider storage, @NotNull PermissionCache cache,
                           @NotNull String defaultGroup) {
        this.storage = storage;
        this.cache = cache;
        this.defaultGroup = defaultGroup;
    }

    @Override
    public CompletableFuture<Optional<User>> loadUser(@NotNull UUID uuid) {
        // Check cache first - if already loaded, return immediately
        User cached = loadedUsers.get(uuid);
        if (cached != null) {
            return CompletableFuture.completedFuture(Optional.of(cached));
        }

        return storage.loadUser(uuid).thenApply(opt -> {
            if (opt.isPresent()) {
                User loaded = opt.get();
                // compute() is atomic - captures the result directly to avoid TOCTOU
                User result = loadedUsers.compute(uuid, (key, existing) -> {
                    if (existing == null || existing.getPrimaryGroup().equals(defaultGroup)) {
                        return loaded;
                    }
                    return existing;
                });
                cache.invalidate(uuid);
                return Optional.of(result);
            }
            return opt;
        });
    }

    @Override
    @Nullable
    public User getUser(@NotNull UUID uuid) {
        return loadedUsers.get(uuid);
    }

    @Override
    @NotNull
    public User getOrCreateUser(@NotNull UUID uuid) {
        return loadedUsers.computeIfAbsent(uuid, id -> {
            User user = new User(id, null);
            user.setPrimaryGroup(defaultGroup);
            return user;
        });
    }

    @Override
    public CompletableFuture<Void> saveUser(@NotNull User user) {
        loadedUsers.put(user.getUuid(), user);
        return storage.saveUser(user);
    }

    @Override
    public CompletableFuture<Void> modifyUser(@NotNull UUID uuid, @NotNull Consumer<User> action) {
        // Use per-entity locks to prevent concurrent modification lost updates
        Object lock = userLocks.computeIfAbsent(uuid, k -> new Object());

        return CompletableFuture.runAsync(() -> {
            synchronized (lock) {
                User user = getOrCreateUser(uuid);
                action.accept(user);
            }
        }).thenCompose(v -> {
            // Re-fetch the user to save (modifications already applied)
            User user = getOrCreateUser(uuid);
            return saveUser(user);
        }).thenRun(() -> {
            // Invalidate cache AFTER save completes to prevent stale reads
            cache.invalidate(uuid);
        });
    }

    @Override
    @NotNull
    public Set<User> getLoadedUsers() {
        return Collections.unmodifiableSet(new HashSet<>(loadedUsers.values()));
    }

    @Override
    public boolean isLoaded(@NotNull UUID uuid) {
        return loadedUsers.containsKey(uuid);
    }

    @Override
    public void unload(@NotNull UUID uuid) {
        loadedUsers.remove(uuid);
        cache.invalidate(uuid);
    }

    /**
     * Loads all users from storage.
     *
     * @return a future that completes when loaded
     */
    public CompletableFuture<Void> loadAll() {
        return storage.loadAllUsers().thenAccept(users -> {
            loadedUsers.putAll(users);
            Logger.info("Loaded %d users from storage", users.size());
        });
    }

    /**
     * Saves all loaded users.
     *
     * @return a future that completes when saved
     */
    public CompletableFuture<Void> saveAll() {
        List<CompletableFuture<Void>> futures = new ArrayList<>();
        for (User user : loadedUsers.values()) {
            if (user.hasData()) {
                futures.add(storage.saveUser(user));
            }
        }
        return CompletableFuture.allOf(futures.toArray(new CompletableFuture<?>[0]));
    }

    /**
     * Cleans up expired permissions for all loaded users.
     *
     * @return the total number of expired permissions removed
     */
    public int cleanupExpired() {
        int total = 0;
        for (User user : loadedUsers.values()) {
            int removed = user.cleanupExpired();
            if (removed > 0) {
                total += removed;
                cache.invalidate(user.getUuid());
                // Capture user reference to avoid issues if unloaded during save
                final User userToSave = user;
                storage.saveUser(userToSave).exceptionally(e -> {
                    Logger.severe("Failed to save user after expired permission cleanup: " + userToSave.getUuid(), e);
                    return null;
                });
            }
        }
        return total;
    }
}
