package com.hyperperms.storage;

import com.hyperperms.model.Group;
import com.hyperperms.model.Track;
import com.hyperperms.model.User;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/**
 * Interface for permission data storage backends.
 * <p>
 * All operations should be non-blocking where possible. Implementations
 * should handle their own threading and connection management.
 */
public interface StorageProvider {

    /**
     * Gets the name of this storage provider.
     *
     * @return the provider name
     */
    @NotNull
    String getName();

    /**
     * Initializes the storage provider.
     * Called once during plugin startup.
     *
     * @return a future that completes when initialization is done
     */
    CompletableFuture<Void> init();

    /**
     * Shuts down the storage provider.
     * Called once during plugin shutdown.
     *
     * @return a future that completes when shutdown is done
     */
    CompletableFuture<Void> shutdown();

    // ==================== User Operations ====================

    /**
     * Loads a user from storage.
     *
     * @param uuid the user's UUID
     * @return the user, or empty if not found
     */
    CompletableFuture<Optional<User>> loadUser(@NotNull UUID uuid);

    /**
     * Saves a user to storage.
     *
     * @param user the user to save
     * @return a future that completes when saved
     */
    CompletableFuture<Void> saveUser(@NotNull User user);

    /**
     * Deletes a user from storage.
     *
     * @param uuid the user's UUID
     * @return a future that completes when deleted
     */
    CompletableFuture<Void> deleteUser(@NotNull UUID uuid);

    /**
     * Loads all users from storage.
     *
     * @return a map of UUID to User
     */
    CompletableFuture<Map<UUID, User>> loadAllUsers();

    /**
     * Gets the UUIDs of all stored users.
     *
     * @return the set of UUIDs
     */
    CompletableFuture<Set<UUID>> getUserUuids();

    /**
     * Looks up a user's UUID by their username.
     *
     * @param username the username
     * @return the UUID, or empty if not found
     */
    CompletableFuture<Optional<UUID>> lookupUuid(@NotNull String username);

    // ==================== Group Operations ====================

    /**
     * Loads a group from storage.
     *
     * @param name the group name
     * @return the group, or empty if not found
     */
    CompletableFuture<Optional<Group>> loadGroup(@NotNull String name);

    /**
     * Saves a group to storage.
     *
     * @param group the group to save
     * @return a future that completes when saved
     */
    CompletableFuture<Void> saveGroup(@NotNull Group group);

    /**
     * Deletes a group from storage.
     *
     * @param name the group name
     * @return a future that completes when deleted
     */
    CompletableFuture<Void> deleteGroup(@NotNull String name);

    /**
     * Loads all groups from storage.
     *
     * @return a map of name to Group
     */
    CompletableFuture<Map<String, Group>> loadAllGroups();

    /**
     * Gets the names of all stored groups.
     *
     * @return the set of group names
     */
    CompletableFuture<Set<String>> getGroupNames();

    // ==================== Track Operations ====================

    /**
     * Loads a track from storage.
     *
     * @param name the track name
     * @return the track, or empty if not found
     */
    CompletableFuture<Optional<Track>> loadTrack(@NotNull String name);

    /**
     * Saves a track to storage.
     *
     * @param track the track to save
     * @return a future that completes when saved
     */
    CompletableFuture<Void> saveTrack(@NotNull Track track);

    /**
     * Deletes a track from storage.
     *
     * @param name the track name
     * @return a future that completes when deleted
     */
    CompletableFuture<Void> deleteTrack(@NotNull String name);

    /**
     * Loads all tracks from storage.
     *
     * @return a map of name to Track
     */
    CompletableFuture<Map<String, Track>> loadAllTracks();

    /**
     * Gets the names of all stored tracks.
     *
     * @return the set of track names
     */
    CompletableFuture<Set<String>> getTrackNames();

    // ==================== Bulk Operations ====================

    /**
     * Saves all pending data.
     *
     * @return a future that completes when all data is saved
     */
    CompletableFuture<Void> saveAll();

    /**
     * Checks if the storage is functioning properly.
     *
     * @return true if healthy
     */
    boolean isHealthy();
}
