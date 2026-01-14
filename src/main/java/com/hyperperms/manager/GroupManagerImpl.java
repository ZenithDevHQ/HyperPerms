package com.hyperperms.manager;

import com.hyperperms.api.HyperPermsAPI.GroupManager;
import com.hyperperms.cache.PermissionCache;
import com.hyperperms.model.Group;
import com.hyperperms.storage.StorageProvider;
import com.hyperperms.util.Logger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

/**
 * Implementation of the group manager.
 */
public final class GroupManagerImpl implements GroupManager {

    private final StorageProvider storage;
    private final PermissionCache cache;
    private final Map<String, Group> loadedGroups = new ConcurrentHashMap<>();

    public GroupManagerImpl(@NotNull StorageProvider storage, @NotNull PermissionCache cache) {
        this.storage = storage;
        this.cache = cache;
    }

    @Override
    @Nullable
    public Group getGroup(@NotNull String name) {
        return loadedGroups.get(name.toLowerCase());
    }

    @Override
    public CompletableFuture<Optional<Group>> loadGroup(@NotNull String name) {
        String lowerName = name.toLowerCase();
        Group cached = loadedGroups.get(lowerName);
        if (cached != null) {
            return CompletableFuture.completedFuture(Optional.of(cached));
        }

        return storage.loadGroup(lowerName).thenApply(opt -> {
            opt.ifPresent(group -> loadedGroups.put(lowerName, group));
            return opt;
        });
    }

    @Override
    @NotNull
    public Group createGroup(@NotNull String name) {
        String lowerName = name.toLowerCase();
        if (loadedGroups.containsKey(lowerName)) {
            throw new IllegalArgumentException("Group already exists: " + name);
        }

        Group group = new Group(lowerName);
        loadedGroups.put(lowerName, group);
        storage.saveGroup(group);
        Logger.info("Created group: " + name);
        return group;
    }

    @Override
    public CompletableFuture<Void> deleteGroup(@NotNull String name) {
        String lowerName = name.toLowerCase();
        loadedGroups.remove(lowerName);
        cache.invalidateAll(); // Group deletion affects many users
        return storage.deleteGroup(lowerName);
    }

    @Override
    public CompletableFuture<Void> saveGroup(@NotNull Group group) {
        loadedGroups.put(group.getName(), group);
        cache.invalidateAll(); // Group changes affect users
        return storage.saveGroup(group);
    }

    @Override
    public CompletableFuture<Void> modifyGroup(@NotNull String name, @NotNull Consumer<Group> action) {
        Group group = getGroup(name);
        if (group == null) {
            return CompletableFuture.failedFuture(
                    new IllegalArgumentException("Group not found: " + name));
        }
        action.accept(group);
        return saveGroup(group);
    }

    @Override
    @NotNull
    public Set<Group> getLoadedGroups() {
        return Collections.unmodifiableSet(new HashSet<>(loadedGroups.values()));
    }

    @Override
    @NotNull
    public Set<String> getGroupNames() {
        return Collections.unmodifiableSet(new HashSet<>(loadedGroups.keySet()));
    }

    /**
     * Loads all groups from storage.
     *
     * @return a future that completes when loaded
     */
    public CompletableFuture<Void> loadAll() {
        return storage.loadAllGroups().thenAccept(groups -> {
            loadedGroups.putAll(groups);
            Logger.info("Loaded %d groups from storage", groups.size());
        });
    }

    /**
     * Creates the default group if it doesn't exist.
     *
     * @param defaultGroupName the default group name
     */
    public void ensureDefaultGroup(@NotNull String defaultGroupName) {
        if (getGroup(defaultGroupName) == null) {
            Group defaultGroup = new Group(defaultGroupName, 0);
            defaultGroup.setDisplayName("Default");
            loadedGroups.put(defaultGroupName, defaultGroup);
            storage.saveGroup(defaultGroup);
            Logger.info("Created default group: " + defaultGroupName);
        }
    }

    /**
     * Cleans up expired permissions for all loaded groups.
     *
     * @return the total number of expired permissions removed
     */
    public int cleanupExpired() {
        int total = 0;
        for (Group group : loadedGroups.values()) {
            int removed = group.cleanupExpired();
            if (removed > 0) {
                total += removed;
                storage.saveGroup(group);
            }
        }
        if (total > 0) {
            cache.invalidateAll();
        }
        return total;
    }
}
