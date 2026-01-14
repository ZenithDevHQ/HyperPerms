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
    private final String defaultGroup;

    public UserManagerImpl(@NotNull StorageProvider storage, @NotNull PermissionCache cache,
                           @NotNull String defaultGroup) {
        this.storage = storage;
        this.cache = cache;
        this.defaultGroup = defaultGroup;
    }

    @Override
    public CompletableFuture<Optional<User>> loadUser(@NotNull UUID uuid) {
        User cached = loadedUsers.get(uuid);
        if (cached != null) {
            return CompletableFuture.completedFuture(Optional.of(cached));
        }

        return storage.loadUser(uuid).thenApply(opt -> {
            opt.ifPresent(user -> loadedUsers.put(uuid, user));
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
        User user = getOrCreateUser(uuid);
        action.accept(user);
        cache.invalidate(uuid);
        return saveUser(user);
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
        return CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]));
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
                storage.saveUser(user);
            }
        }
        return total;
    }
}
