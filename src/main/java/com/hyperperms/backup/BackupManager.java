package com.hyperperms.backup;

import com.hyperperms.HyperPerms;
import com.hyperperms.storage.StorageProvider;
import com.hyperperms.util.Logger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Consumer;

/**
 * Manages automatic and manual backups for HyperPerms data.
 * 
 * <p>Features:
 * <ul>
 *   <li>Automatic scheduled backups at configurable intervals</li>
 *   <li>Manual backup creation with custom names</li>
 *   <li>Automatic cleanup of old backups (configurable max count)</li>
 *   <li>Backup restoration with safety checks</li>
 *   <li>Event callbacks for backup operations</li>
 * </ul>
 * 
 * <p>Example usage:
 * <pre>{@code
 * BackupManager backups = hyperPerms.getBackupManager();
 * 
 * // Create a manual backup
 * backups.createBackup("before-update").thenAccept(name -> {
 *     System.out.println("Created backup: " + name);
 * });
 * 
 * // List all backups
 * backups.listBackups().thenAccept(list -> {
 *     list.forEach(System.out::println);
 * });
 * 
 * // Restore a backup
 * backups.restoreBackup("backup-name").thenAccept(success -> {
 *     if (success) System.out.println("Restored!");
 * });
 * }</pre>
 */
public class BackupManager {
    
    private static final DateTimeFormatter BACKUP_NAME_FORMAT = 
        DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm-ss").withZone(ZoneId.systemDefault());
    
    private final HyperPerms plugin;
    private final ScheduledExecutorService scheduler;
    
    // Configuration
    private volatile boolean autoBackupEnabled = true;
    private volatile int maxBackups = 10;
    private volatile int backupIntervalSeconds = 3600; // 1 hour
    private volatile boolean backupOnSave = false;
    
    // State
    private final AtomicBoolean running = new AtomicBoolean(false);
    private final AtomicLong lastBackupTime = new AtomicLong(0);
    private ScheduledFuture<?> autoBackupTask;
    
    // Event callbacks
    private final List<Consumer<BackupEvent>> eventListeners = new CopyOnWriteArrayList<>();
    
    /**
     * Creates a new BackupManager.
     *
     * @param plugin the HyperPerms plugin instance
     */
    public BackupManager(@NotNull HyperPerms plugin) {
        this.plugin = Objects.requireNonNull(plugin, "plugin cannot be null");
        this.scheduler = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "HyperPerms-BackupManager");
            t.setDaemon(true);
            return t;
        });
    }
    
    /**
     * Starts the backup manager, enabling automatic backups if configured.
     */
    public void start() {
        if (running.compareAndSet(false, true)) {
            loadConfig();
            if (autoBackupEnabled) {
                scheduleAutoBackup();
            }
            Logger.info("BackupManager started (auto-backup: %s, interval: %ds, max: %d)",
                autoBackupEnabled, backupIntervalSeconds, maxBackups);
        }
    }
    
    /**
     * Stops the backup manager and cancels any scheduled tasks.
     */
    public void stop() {
        if (running.compareAndSet(true, false)) {
            if (autoBackupTask != null) {
                autoBackupTask.cancel(false);
                autoBackupTask = null;
            }
            Logger.info("BackupManager stopped");
        }
    }
    
    /**
     * Shuts down the backup manager completely.
     * Should be called when the plugin is disabled.
     */
    public void shutdown() {
        stop();
        scheduler.shutdown();
        try {
            if (!scheduler.awaitTermination(5, TimeUnit.SECONDS)) {
                scheduler.shutdownNow();
            }
        } catch (InterruptedException e) {
            scheduler.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }
    
    /**
     * Loads configuration from the plugin config.
     */
    public void loadConfig() {
        var config = plugin.getConfig();
        if (config != null) {
            autoBackupEnabled = config.isAutoBackupEnabled();
            maxBackups = config.getMaxBackups();
            backupIntervalSeconds = config.getAutoBackupIntervalSeconds();
            backupOnSave = config.isBackupOnSave();
        }
    }
    
    /**
     * Schedules automatic backups.
     */
    private void scheduleAutoBackup() {
        if (autoBackupTask != null) {
            autoBackupTask.cancel(false);
        }
        
        autoBackupTask = scheduler.scheduleAtFixedRate(
            () -> {
                try {
                    createAutoBackup();
                } catch (Exception e) {
                    Logger.warn("Auto-backup failed: %s", e.getMessage());
                }
            },
            backupIntervalSeconds,
            backupIntervalSeconds,
            TimeUnit.SECONDS
        );
        
        Logger.debug("Auto-backup scheduled every %d seconds", backupIntervalSeconds);
    }
    
    /**
     * Creates an automatic backup.
     */
    private void createAutoBackup() {
        String name = "auto-" + BACKUP_NAME_FORMAT.format(Instant.now());
        
        createBackupInternal(name, BackupType.AUTOMATIC).thenCompose(backupName -> {
            if (backupName != null) {
                Logger.info("Auto-backup created: %s", backupName);
                fireEvent(new BackupEvent(BackupEvent.Type.CREATED, backupName, BackupType.AUTOMATIC));
                return cleanupOldBackups();
            }
            return CompletableFuture.completedFuture(null);
        }).exceptionally(e -> {
            Logger.warn("Auto-backup failed: %s", e.getMessage());
            fireEvent(new BackupEvent(BackupEvent.Type.FAILED, name, BackupType.AUTOMATIC, e.getMessage()));
            return null;
        });
    }
    
    // ==================== Public API ====================
    
    /**
     * Creates a backup with an auto-generated name.
     *
     * @return a future containing the backup name
     */
    public CompletableFuture<String> createBackup() {
        return createBackup(null);
    }
    
    /**
     * Creates a backup with the specified name.
     *
     * @param name the backup name (or null for auto-generated)
     * @return a future containing the backup name
     */
    public CompletableFuture<String> createBackup(@Nullable String name) {
        String backupName = name;
        if (backupName == null || backupName.isEmpty()) {
            backupName = "manual-" + BACKUP_NAME_FORMAT.format(Instant.now());
        }
        
        // Sanitize name
        backupName = sanitizeBackupName(backupName);
        
        String finalName = backupName;
        return createBackupInternal(finalName, BackupType.MANUAL)
            .thenApply(result -> {
                if (result != null) {
                    Logger.info("Backup created: %s", result);
                    fireEvent(new BackupEvent(BackupEvent.Type.CREATED, result, BackupType.MANUAL));
                }
                return result;
            })
            .exceptionally(e -> {
                Logger.warn("Backup failed: %s", e.getMessage());
                fireEvent(new BackupEvent(BackupEvent.Type.FAILED, finalName, BackupType.MANUAL, e.getMessage()));
                return null;
            });
    }
    
    /**
     * Creates a backup before save (if enabled in config).
     * Called internally by HyperPerms before saving data.
     */
    public CompletableFuture<Void> createBackupOnSave() {
        if (!backupOnSave || !running.get()) {
            return CompletableFuture.completedFuture(null);
        }
        
        String name = "save-" + BACKUP_NAME_FORMAT.format(Instant.now());
        return createBackupInternal(name, BackupType.ON_SAVE)
            .thenAccept(result -> {
                if (result != null) {
                    Logger.debug("Pre-save backup created: %s", result);
                    fireEvent(new BackupEvent(BackupEvent.Type.CREATED, result, BackupType.ON_SAVE));
                }
            })
            .exceptionally(e -> {
                Logger.warn("Pre-save backup failed: %s", e.getMessage());
                return null;
            });
    }
    
    /**
     * Internal backup creation.
     */
    private CompletableFuture<String> createBackupInternal(String name, BackupType type) {
        StorageProvider storage = plugin.getStorage();
        if (storage == null) {
            return CompletableFuture.failedFuture(new IllegalStateException("Storage not initialized"));
        }
        
        return storage.createBackup(name).thenApply(result -> {
            lastBackupTime.set(System.currentTimeMillis());
            return result;
        });
    }
    
    /**
     * Restores a backup.
     *
     * @param name the backup name
     * @return a future indicating success
     */
    public CompletableFuture<Boolean> restoreBackup(@NotNull String name) {
        Objects.requireNonNull(name, "name cannot be null");
        
        StorageProvider storage = plugin.getStorage();
        if (storage == null) {
            return CompletableFuture.failedFuture(new IllegalStateException("Storage not initialized"));
        }
        
        Logger.info("Restoring backup: %s", name);
        fireEvent(new BackupEvent(BackupEvent.Type.RESTORING, name, null));
        
        return storage.restoreBackup(name).thenApply(success -> {
            if (success) {
                Logger.info("Backup restored successfully: %s", name);
                fireEvent(new BackupEvent(BackupEvent.Type.RESTORED, name, null));
            } else {
                Logger.warn("Failed to restore backup: %s", name);
                fireEvent(new BackupEvent(BackupEvent.Type.FAILED, name, null, "Restore failed"));
            }
            return success;
        }).exceptionally(e -> {
            Logger.warn("Backup restore error: %s", e.getMessage());
            fireEvent(new BackupEvent(BackupEvent.Type.FAILED, name, null, e.getMessage()));
            return false;
        });
    }
    
    /**
     * Lists all available backups.
     *
     * @return a future containing the list of backup names
     */
    public CompletableFuture<List<String>> listBackups() {
        StorageProvider storage = plugin.getStorage();
        if (storage == null) {
            return CompletableFuture.completedFuture(Collections.emptyList());
        }
        
        return storage.listBackups();
    }
    
    /**
     * Gets information about all backups.
     *
     * @return a future containing backup information
     */
    public CompletableFuture<List<BackupInfo>> getBackupInfo() {
        return listBackups().thenApply(names -> {
            List<BackupInfo> info = new ArrayList<>();
            for (String name : names) {
                info.add(parseBackupInfo(name));
            }
            // Sort by timestamp (newest first)
            info.sort((a, b) -> Long.compare(b.getTimestamp(), a.getTimestamp()));
            return info;
        });
    }
    
    /**
     * Parses backup info from a backup name.
     */
    private BackupInfo parseBackupInfo(String name) {
        BackupType type = BackupType.MANUAL;
        String datePart = name;
        if (name.startsWith("auto-")) {
            type = BackupType.AUTOMATIC;
            datePart = name.substring("auto-".length());
        } else if (name.startsWith("save-")) {
            type = BackupType.ON_SAVE;
            datePart = name.substring("save-".length());
        } else if (name.startsWith("manual-")) {
            datePart = name.substring("manual-".length());
        } else if (name.startsWith("pre-restore-")) {
            datePart = name.substring("pre-restore-".length());
        }

        // Try to parse timestamp from the date portion
        long timestamp = 0;
        try {
            Instant instant = BACKUP_NAME_FORMAT.parse(datePart, Instant::from);
            timestamp = instant.toEpochMilli();
        } catch (Exception e) {
            // Use current time if parsing fails
            timestamp = System.currentTimeMillis();
        }

        return new BackupInfo(name, type, timestamp);
    }
    
    /**
     * Deletes a backup.
     *
     * @param name the backup name
     * @return a future indicating success
     */
    public CompletableFuture<Boolean> deleteBackup(@NotNull String name) {
        Objects.requireNonNull(name, "name cannot be null");
        
        StorageProvider storage = plugin.getStorage();
        if (storage == null) {
            return CompletableFuture.failedFuture(new IllegalStateException("Storage not initialized"));
        }
        
        return storage.deleteBackup(name).thenApply(success -> {
            if (success) {
                Logger.info("Backup deleted: %s", name);
                fireEvent(new BackupEvent(BackupEvent.Type.DELETED, name, null));
            }
            return success;
        });
    }
    
    /**
     * Cleans up old backups, keeping only the most recent ones.
     *
     * @return a future completing when cleanup is done
     */
    public CompletableFuture<Void> cleanupOldBackups() {
        return getBackupInfo().thenCompose(backups -> {
            if (backups.size() <= maxBackups) {
                return CompletableFuture.completedFuture(null);
            }
            
            // Delete oldest backups (already sorted newest-first)
            List<CompletableFuture<Boolean>> deletions = new ArrayList<>();
            for (int i = maxBackups; i < backups.size(); i++) {
                BackupInfo backup = backups.get(i);
                Logger.debug("Deleting old backup: %s", backup.getName());
                deletions.add(deleteBackup(backup.getName()));
            }
            
            return CompletableFuture.allOf(deletions.toArray(new CompletableFuture<?>[0]));
        });
    }
    
    // ==================== Configuration Methods ====================
    
    public boolean isAutoBackupEnabled() {
        return autoBackupEnabled;
    }
    
    public void setAutoBackupEnabled(boolean enabled) {
        this.autoBackupEnabled = enabled;
        if (running.get()) {
            if (enabled && autoBackupTask == null) {
                scheduleAutoBackup();
            } else if (!enabled && autoBackupTask != null) {
                autoBackupTask.cancel(false);
                autoBackupTask = null;
            }
        }
    }
    
    public int getMaxBackups() {
        return maxBackups;
    }
    
    public void setMaxBackups(int maxBackups) {
        this.maxBackups = Math.max(1, maxBackups);
    }
    
    public int getBackupIntervalSeconds() {
        return backupIntervalSeconds;
    }
    
    public void setBackupIntervalSeconds(int seconds) {
        this.backupIntervalSeconds = Math.max(60, seconds); // Minimum 1 minute
        if (running.get() && autoBackupEnabled) {
            scheduleAutoBackup(); // Reschedule with new interval
        }
    }
    
    public boolean isBackupOnSave() {
        return backupOnSave;
    }
    
    public void setBackupOnSave(boolean backupOnSave) {
        this.backupOnSave = backupOnSave;
    }
    
    /**
     * Gets the time of the last backup.
     *
     * @return milliseconds since epoch, or 0 if no backup has been made
     */
    public long getLastBackupTime() {
        return lastBackupTime.get();
    }
    
    /**
     * Gets the time until the next automatic backup.
     *
     * @return seconds until next backup, or -1 if auto-backup is disabled
     */
    public long getSecondsUntilNextBackup() {
        if (!autoBackupEnabled || autoBackupTask == null) {
            return -1;
        }
        return autoBackupTask.getDelay(TimeUnit.SECONDS);
    }
    
    // ==================== Event System ====================
    
    /**
     * Registers an event listener.
     *
     * @param listener the listener
     */
    public void addEventListener(@NotNull Consumer<BackupEvent> listener) {
        eventListeners.add(Objects.requireNonNull(listener, "listener cannot be null"));
    }
    
    /**
     * Removes an event listener.
     *
     * @param listener the listener
     */
    public void removeEventListener(@NotNull Consumer<BackupEvent> listener) {
        eventListeners.remove(listener);
    }
    
    private void fireEvent(BackupEvent event) {
        for (Consumer<BackupEvent> listener : eventListeners) {
            try {
                listener.accept(event);
            } catch (Exception e) {
                Logger.warn("Backup event listener error: %s", e.getMessage());
            }
        }
    }
    
    // ==================== Utility Methods ====================
    
    /**
     * Sanitizes a backup name for safe file system usage.
     */
    private String sanitizeBackupName(String name) {
        return name.replaceAll("[^a-zA-Z0-9_-]", "_");
    }
    
    // ==================== Inner Classes ====================
    
    /**
     * Type of backup.
     */
    public enum BackupType {
        MANUAL,
        AUTOMATIC,
        ON_SAVE
    }
    
    /**
     * Information about a backup.
     */
    public static class BackupInfo {
        private final String name;
        private final BackupType type;
        private final long timestamp;
        
        public BackupInfo(String name, BackupType type, long timestamp) {
            this.name = name;
            this.type = type;
            this.timestamp = timestamp;
        }
        
        public String getName() {
            return name;
        }
        
        public BackupType getType() {
            return type;
        }
        
        public long getTimestamp() {
            return timestamp;
        }
        
        public Instant getInstant() {
            return Instant.ofEpochMilli(timestamp);
        }
        
        public String getFormattedTime() {
            return BACKUP_NAME_FORMAT.format(getInstant());
        }
        
        @Override
        public String toString() {
            return String.format("%s (%s, %s)", name, type, getFormattedTime());
        }
    }
    
    /**
     * Event fired when backup operations occur.
     */
    public static class BackupEvent {
        
        public enum Type {
            CREATED,
            DELETED,
            RESTORING,
            RESTORED,
            FAILED
        }
        
        private final Type type;
        private final String backupName;
        private final BackupType backupType;
        private final String error;
        
        public BackupEvent(Type type, String backupName, BackupType backupType) {
            this(type, backupName, backupType, null);
        }
        
        public BackupEvent(Type type, String backupName, BackupType backupType, String error) {
            this.type = type;
            this.backupName = backupName;
            this.backupType = backupType;
            this.error = error;
        }
        
        public Type getType() {
            return type;
        }
        
        public String getBackupName() {
            return backupName;
        }
        
        public BackupType getBackupType() {
            return backupType;
        }
        
        public String getError() {
            return error;
        }
        
        public boolean isSuccess() {
            return type != Type.FAILED;
        }
        
        @Override
        public String toString() {
            return String.format("BackupEvent{type=%s, name=%s, backupType=%s, error=%s}",
                type, backupName, backupType, error);
        }
    }
}
