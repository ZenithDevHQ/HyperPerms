package com.hyperperms.manager;

import com.hyperperms.api.HyperPermsAPI.TrackManager;
import com.hyperperms.model.Track;
import com.hyperperms.storage.StorageProvider;
import com.hyperperms.util.Logger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Implementation of the track manager.
 */
public final class TrackManagerImpl implements TrackManager {

    private final StorageProvider storage;
    private final Map<String, Track> loadedTracks = new ConcurrentHashMap<>();

    public TrackManagerImpl(@NotNull StorageProvider storage) {
        this.storage = storage;
    }

    @Override
    @Nullable
    public Track getTrack(@NotNull String name) {
        return loadedTracks.get(name.toLowerCase());
    }

    @Override
    public CompletableFuture<Optional<Track>> loadTrack(@NotNull String name) {
        String lowerName = name.toLowerCase();
        Track cached = loadedTracks.get(lowerName);
        if (cached != null) {
            return CompletableFuture.completedFuture(Optional.of(cached));
        }

        return storage.loadTrack(lowerName).thenApply(opt -> {
            opt.ifPresent(track -> loadedTracks.put(lowerName, track));
            return opt;
        });
    }

    @Override
    @NotNull
    public Track createTrack(@NotNull String name) {
        String lowerName = name.toLowerCase();
        Track track = new Track(lowerName);

        // putIfAbsent is atomic - prevents concurrent duplicate creation
        Track existing = loadedTracks.putIfAbsent(lowerName, track);
        if (existing != null) {
            throw new IllegalArgumentException("Track already exists: " + name);
        }

        storage.saveTrack(track);
        Logger.info("Created track: " + name);
        return track;
    }

    @Override
    public CompletableFuture<Void> deleteTrack(@NotNull String name) {
        String lowerName = name.toLowerCase();
        loadedTracks.remove(lowerName);
        return storage.deleteTrack(lowerName);
    }

    @Override
    public CompletableFuture<Void> saveTrack(@NotNull Track track) {
        loadedTracks.put(track.getName(), track);
        return storage.saveTrack(track);
    }

    @Override
    @NotNull
    public Set<Track> getLoadedTracks() {
        return Collections.unmodifiableSet(new HashSet<>(loadedTracks.values()));
    }

    @Override
    @NotNull
    public Set<String> getTrackNames() {
        return Collections.unmodifiableSet(new HashSet<>(loadedTracks.keySet()));
    }

    /**
     * Loads all tracks from storage.
     *
     * @return a future that completes when loaded
     */
    public CompletableFuture<Void> loadAll() {
        return storage.loadAllTracks().thenAccept(tracks -> {
            loadedTracks.putAll(tracks);
            Logger.info("Loaded %d tracks from storage", tracks.size());
        });
    }
}
