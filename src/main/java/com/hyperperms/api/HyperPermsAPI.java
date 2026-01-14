package com.hyperperms.api;

import com.hyperperms.api.context.ContextSet;
import com.hyperperms.api.events.EventBus;
import com.hyperperms.model.Group;
import com.hyperperms.model.Track;
import com.hyperperms.model.User;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

/**
 * Main API interface for HyperPerms.
 * <p>
 * Obtain an instance using {@code HyperPerms.getApi()}.
 */
public interface HyperPermsAPI {

    // ==================== Permission Checks ====================

    /**
     * Checks if a user has a permission.
     *
     * @param uuid       the user UUID
     * @param permission the permission to check
     * @return true if the user has the permission
     */
    boolean hasPermission(@NotNull UUID uuid, @NotNull String permission);

    /**
     * Checks if a user has a permission in a specific context.
     *
     * @param uuid       the user UUID
     * @param permission the permission to check
     * @param contexts   the contexts to check in
     * @return true if the user has the permission
     */
    boolean hasPermission(@NotNull UUID uuid, @NotNull String permission, @NotNull ContextSet contexts);

    // ==================== User Management ====================

    /**
     * Gets the user manager.
     *
     * @return the user manager
     */
    @NotNull
    UserManager getUserManager();

    /**
     * Gets the group manager.
     *
     * @return the group manager
     */
    @NotNull
    GroupManager getGroupManager();

    /**
     * Gets the track manager.
     *
     * @return the track manager
     */
    @NotNull
    TrackManager getTrackManager();

    // ==================== Events ====================

    /**
     * Gets the event bus for subscribing to events.
     *
     * @return the event bus
     */
    @NotNull
    EventBus getEventBus();

    // ==================== Context ====================

    /**
     * Gets the current context for a user.
     *
     * @param uuid the user UUID
     * @return the current contexts
     */
    @NotNull
    ContextSet getContexts(@NotNull UUID uuid);

    // ==================== User Manager Interface ====================

    /**
     * Manager for user permission data.
     */
    interface UserManager {

        /**
         * Gets a user, loading from storage if necessary.
         *
         * @param uuid the user UUID
         * @return the user, or empty if not found
         */
        CompletableFuture<Optional<User>> loadUser(@NotNull UUID uuid);

        /**
         * Gets a cached user, or null if not loaded.
         *
         * @param uuid the user UUID
         * @return the user, or null
         */
        @Nullable
        User getUser(@NotNull UUID uuid);

        /**
         * Gets or creates a user.
         *
         * @param uuid the user UUID
         * @return the user (never null)
         */
        @NotNull
        User getOrCreateUser(@NotNull UUID uuid);

        /**
         * Saves a user to storage.
         *
         * @param user the user to save
         * @return a future that completes when saved
         */
        CompletableFuture<Void> saveUser(@NotNull User user);

        /**
         * Modifies a user and saves the changes.
         *
         * @param uuid   the user UUID
         * @param action the modification to apply
         * @return a future that completes when saved
         */
        CompletableFuture<Void> modifyUser(@NotNull UUID uuid, @NotNull Consumer<User> action);

        /**
         * Gets all loaded users.
         *
         * @return set of loaded users
         */
        @NotNull
        Set<User> getLoadedUsers();

        /**
         * Checks if a user is loaded.
         *
         * @param uuid the user UUID
         * @return true if loaded
         */
        boolean isLoaded(@NotNull UUID uuid);

        /**
         * Unloads a user from cache.
         *
         * @param uuid the user UUID
         */
        void unload(@NotNull UUID uuid);
    }

    // ==================== Group Manager Interface ====================

    /**
     * Manager for permission groups.
     */
    interface GroupManager {

        /**
         * Gets a group by name.
         *
         * @param name the group name
         * @return the group, or null if not found
         */
        @Nullable
        Group getGroup(@NotNull String name);

        /**
         * Gets a group, loading from storage if necessary.
         *
         * @param name the group name
         * @return the group, or empty if not found
         */
        CompletableFuture<Optional<Group>> loadGroup(@NotNull String name);

        /**
         * Creates a new group.
         *
         * @param name the group name
         * @return the created group
         * @throws IllegalArgumentException if the group already exists
         */
        @NotNull
        Group createGroup(@NotNull String name);

        /**
         * Deletes a group.
         *
         * @param name the group name
         * @return a future that completes when deleted
         */
        CompletableFuture<Void> deleteGroup(@NotNull String name);

        /**
         * Saves a group to storage.
         *
         * @param group the group to save
         * @return a future that completes when saved
         */
        CompletableFuture<Void> saveGroup(@NotNull Group group);

        /**
         * Modifies a group and saves the changes.
         *
         * @param name   the group name
         * @param action the modification to apply
         * @return a future that completes when saved
         */
        CompletableFuture<Void> modifyGroup(@NotNull String name, @NotNull Consumer<Group> action);

        /**
         * Gets all loaded groups.
         *
         * @return set of loaded groups
         */
        @NotNull
        Set<Group> getLoadedGroups();

        /**
         * Gets the names of all groups.
         *
         * @return set of group names
         */
        @NotNull
        Set<String> getGroupNames();
    }

    // ==================== Track Manager Interface ====================

    /**
     * Manager for promotion tracks.
     */
    interface TrackManager {

        /**
         * Gets a track by name.
         *
         * @param name the track name
         * @return the track, or null if not found
         */
        @Nullable
        Track getTrack(@NotNull String name);

        /**
         * Gets a track, loading from storage if necessary.
         *
         * @param name the track name
         * @return the track, or empty if not found
         */
        CompletableFuture<Optional<Track>> loadTrack(@NotNull String name);

        /**
         * Creates a new track.
         *
         * @param name the track name
         * @return the created track
         * @throws IllegalArgumentException if the track already exists
         */
        @NotNull
        Track createTrack(@NotNull String name);

        /**
         * Deletes a track.
         *
         * @param name the track name
         * @return a future that completes when deleted
         */
        CompletableFuture<Void> deleteTrack(@NotNull String name);

        /**
         * Saves a track to storage.
         *
         * @param track the track to save
         * @return a future that completes when saved
         */
        CompletableFuture<Void> saveTrack(@NotNull Track track);

        /**
         * Gets all loaded tracks.
         *
         * @return set of loaded tracks
         */
        @NotNull
        Set<Track> getLoadedTracks();

        /**
         * Gets the names of all tracks.
         *
         * @return set of track names
         */
        @NotNull
        Set<String> getTrackNames();
    }
}
