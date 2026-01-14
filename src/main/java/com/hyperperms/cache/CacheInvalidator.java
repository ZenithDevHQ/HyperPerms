package com.hyperperms.cache;

import com.hyperperms.util.Logger;
import org.jetbrains.annotations.NotNull;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Manages targeted cache invalidation by tracking user-group memberships.
 * <p>
 * This allows efficient invalidation when a group changes - only users
 * belonging to that group (directly or through inheritance) need to be
 * invalidated, rather than clearing the entire cache.
 */
public final class CacheInvalidator {

    private final PermissionCache cache;

    // Maps group name -> set of user UUIDs that inherit from this group
    private final Map<String, Set<UUID>> groupMembers = new ConcurrentHashMap<>();

    // Maps user UUID -> set of groups they belong to (for reverse lookup)
    private final Map<UUID, Set<String>> userGroups = new ConcurrentHashMap<>();

    /**
     * Creates a new cache invalidator.
     *
     * @param cache the permission cache to invalidate
     */
    public CacheInvalidator(@NotNull PermissionCache cache) {
        this.cache = Objects.requireNonNull(cache, "cache cannot be null");
    }

    /**
     * Registers a user's group memberships.
     * <p>
     * This should be called when a user's permissions are resolved,
     * passing all groups they inherit from (directly or indirectly).
     *
     * @param uuid   the user's UUID
     * @param groups the groups the user belongs to
     */
    public void registerUserGroups(@NotNull UUID uuid, @NotNull Collection<String> groups) {
        // Remove old mappings
        Set<String> oldGroups = userGroups.get(uuid);
        if (oldGroups != null) {
            for (String group : oldGroups) {
                Set<UUID> members = groupMembers.get(group);
                if (members != null) {
                    members.remove(uuid);
                }
            }
        }

        // Add new mappings
        Set<String> newGroups = ConcurrentHashMap.newKeySet();
        newGroups.addAll(groups);
        userGroups.put(uuid, newGroups);

        for (String group : groups) {
            groupMembers.computeIfAbsent(group, k -> ConcurrentHashMap.newKeySet()).add(uuid);
        }
    }

    /**
     * Unregisters a user's group memberships.
     * <p>
     * This should be called when a user logs out or is unloaded.
     *
     * @param uuid the user's UUID
     */
    public void unregisterUser(@NotNull UUID uuid) {
        Set<String> groups = userGroups.remove(uuid);
        if (groups != null) {
            for (String group : groups) {
                Set<UUID> members = groupMembers.get(group);
                if (members != null) {
                    members.remove(uuid);
                }
            }
        }
    }

    /**
     * Invalidates cache entries for all users in a group.
     * <p>
     * This is called when a group's permissions change.
     *
     * @param groupName the group that changed
     * @return the number of users invalidated
     */
    public int invalidateGroup(@NotNull String groupName) {
        Set<UUID> members = groupMembers.get(groupName.toLowerCase());
        if (members == null || members.isEmpty()) {
            Logger.debug("No cached users in group '%s' to invalidate", groupName);
            return 0;
        }

        int count = 0;
        for (UUID uuid : members) {
            cache.invalidate(uuid);
            count++;
        }

        Logger.debug("Invalidated cache for %d users in group '%s'", count, groupName);
        return count;
    }

    /**
     * Invalidates cache entries for all users in multiple groups.
     *
     * @param groupNames the groups that changed
     * @return the number of unique users invalidated
     */
    public int invalidateGroups(@NotNull Collection<String> groupNames) {
        Set<UUID> toInvalidate = new HashSet<>();

        for (String groupName : groupNames) {
            Set<UUID> members = groupMembers.get(groupName.toLowerCase());
            if (members != null) {
                toInvalidate.addAll(members);
            }
        }

        if (toInvalidate.isEmpty()) {
            return 0;
        }

        for (UUID uuid : toInvalidate) {
            cache.invalidate(uuid);
        }

        Logger.debug("Invalidated cache for %d users across %d groups",
                toInvalidate.size(), groupNames.size());
        return toInvalidate.size();
    }

    /**
     * Invalidates a specific user's cache.
     *
     * @param uuid the user's UUID
     */
    public void invalidateUser(@NotNull UUID uuid) {
        cache.invalidate(uuid);
    }

    /**
     * Gets all users that belong to a group.
     *
     * @param groupName the group name
     * @return unmodifiable set of user UUIDs
     */
    @NotNull
    public Set<UUID> getUsersInGroup(@NotNull String groupName) {
        Set<UUID> members = groupMembers.get(groupName.toLowerCase());
        if (members == null) {
            return Collections.emptySet();
        }
        return Collections.unmodifiableSet(new HashSet<>(members));
    }

    /**
     * Gets all groups a user belongs to.
     *
     * @param uuid the user's UUID
     * @return unmodifiable set of group names
     */
    @NotNull
    public Set<String> getGroupsForUser(@NotNull UUID uuid) {
        Set<String> groups = userGroups.get(uuid);
        if (groups == null) {
            return Collections.emptySet();
        }
        return Collections.unmodifiableSet(new HashSet<>(groups));
    }

    /**
     * Gets the number of tracked users.
     *
     * @return the user count
     */
    public int getTrackedUserCount() {
        return userGroups.size();
    }

    /**
     * Gets the number of tracked groups.
     *
     * @return the group count
     */
    public int getTrackedGroupCount() {
        return groupMembers.size();
    }

    /**
     * Clears all tracking data.
     */
    public void clear() {
        groupMembers.clear();
        userGroups.clear();
    }

    @Override
    public String toString() {
        return String.format("CacheInvalidator{users=%d, groups=%d}",
                getTrackedUserCount(), getTrackedGroupCount());
    }
}
