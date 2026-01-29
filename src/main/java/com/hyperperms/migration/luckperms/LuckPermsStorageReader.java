package com.hyperperms.migration.luckperms;

import com.hyperperms.migration.luckperms.LuckPermsData.*;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.util.Map;
import java.util.UUID;

/**
 * Interface for reading LuckPerms data from various storage backends.
 */
public interface LuckPermsStorageReader {
    
    /**
     * Gets the storage type this reader handles.
     *
     * @return the storage type
     */
    @NotNull
    LuckPermsStorageType getStorageType();
    
    /**
     * Gets a description of the storage location.
     *
     * @return storage location description
     */
    @NotNull
    String getStorageDescription();
    
    /**
     * Checks if this storage is available and readable.
     *
     * @return true if storage can be read
     */
    boolean isAvailable();
    
    /**
     * Reads all groups from storage.
     *
     * @return map of group name to group data
     * @throws IOException if an I/O error occurs
     */
    @NotNull
    Map<String, LPGroup> readGroups() throws IOException;
    
    /**
     * Reads all users from storage.
     *
     * @return map of UUID to user data
     * @throws IOException if an I/O error occurs
     */
    @NotNull
    Map<UUID, LPUser> readUsers() throws IOException;
    
    /**
     * Reads all tracks from storage.
     *
     * @return map of track name to track data
     * @throws IOException if an I/O error occurs
     */
    @NotNull
    Map<String, LPTrack> readTracks() throws IOException;
    
    /**
     * Reads all data at once.
     *
     * @return complete data set
     * @throws IOException if an I/O error occurs
     */
    @NotNull
    default LPDataSet readAll() throws IOException {
        return new LPDataSet(readGroups(), readUsers(), readTracks());
    }
    
    /**
     * Gets the total number of users (for progress reporting).
     * This should be fast and not require reading all user data.
     *
     * @return estimated user count, or -1 if unknown
     */
    default int estimateUserCount() {
        return -1;
    }
    
    /**
     * Closes any resources held by this reader.
     */
    default void close() {}
}
