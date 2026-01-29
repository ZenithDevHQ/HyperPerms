package com.hyperperms.migration;

import org.jetbrains.annotations.NotNull;

/**
 * Callback interface for migration progress updates.
 * <p>
 * Implementations can use this to display progress bars or log messages
 * during long-running migrations.
 */
public interface MigrationProgressCallback {
    
    /**
     * Called when starting a new phase of the migration.
     *
     * @param phase the phase name
     * @param totalItems total items to process in this phase, or -1 if unknown
     */
    void onPhaseStart(@NotNull String phase, int totalItems);
    
    /**
     * Called when progress is made in the current phase.
     *
     * @param itemsProcessed number of items processed so far
     * @param currentItem description of the current item being processed
     */
    void onProgress(int itemsProcessed, @NotNull String currentItem);
    
    /**
     * Called when a phase is completed.
     *
     * @param phase the phase name
     * @param itemsProcessed total items processed in this phase
     */
    void onPhaseComplete(@NotNull String phase, int itemsProcessed);
    
    /**
     * Called when a warning occurs (but migration continues).
     *
     * @param message the warning message
     */
    void onWarning(@NotNull String message);
    
    /**
     * Called when an error occurs (may or may not stop migration).
     *
     * @param message the error message
     * @param fatal whether this error will stop the migration
     */
    void onError(@NotNull String message, boolean fatal);
    
    /**
     * A no-op callback that ignores all progress updates.
     */
    MigrationProgressCallback NOOP = new MigrationProgressCallback() {
        @Override
        public void onPhaseStart(@NotNull String phase, int totalItems) {}
        
        @Override
        public void onProgress(int itemsProcessed, @NotNull String currentItem) {}
        
        @Override
        public void onPhaseComplete(@NotNull String phase, int itemsProcessed) {}
        
        @Override
        public void onWarning(@NotNull String message) {}
        
        @Override
        public void onError(@NotNull String message, boolean fatal) {}
    };
    
    /**
     * A callback that logs to console.
     */
    static MigrationProgressCallback logging() {
        return new MigrationProgressCallback() {
            private int lastPercentage = -1;
            private int totalItems = 0;
            
            @Override
            public void onPhaseStart(@NotNull String phase, int totalItems) {
                this.totalItems = totalItems;
                this.lastPercentage = -1;
                if (totalItems > 0) {
                    com.hyperperms.util.Logger.info("[Migration] Starting: %s (%d items)", phase, totalItems);
                } else {
                    com.hyperperms.util.Logger.info("[Migration] Starting: %s", phase);
                }
            }
            
            @Override
            public void onProgress(int itemsProcessed, @NotNull String currentItem) {
                if (totalItems > 0) {
                    int percentage = (itemsProcessed * 100) / totalItems;
                    if (percentage > lastPercentage && percentage % 10 == 0) {
                        lastPercentage = percentage;
                        com.hyperperms.util.Logger.info("[Migration] Progress: %d%%", percentage);
                    }
                }
            }
            
            @Override
            public void onPhaseComplete(@NotNull String phase, int itemsProcessed) {
                com.hyperperms.util.Logger.info("[Migration] Completed: %s (%d items)", phase, itemsProcessed);
            }
            
            @Override
            public void onWarning(@NotNull String message) {
                com.hyperperms.util.Logger.warn("[Migration] Warning: %s", message);
            }
            
            @Override
            public void onError(@NotNull String message, boolean fatal) {
                if (fatal) {
                    com.hyperperms.util.Logger.severe("[Migration] Fatal Error: %s", message);
                } else {
                    com.hyperperms.util.Logger.warn("[Migration] Error: %s", message);
                }
            }
        };
    }
}
