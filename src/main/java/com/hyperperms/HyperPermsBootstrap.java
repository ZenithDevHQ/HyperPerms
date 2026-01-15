package com.hyperperms;

import com.hyperperms.util.Logger;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.logging.ConsoleHandler;
import java.util.logging.Level;

/**
 * Bootstrap class for HyperPerms.
 * <p>
 * This class provides the entry point for loading the plugin. When the Hytale
 * server API is available, this should be called from the main plugin class
 * that implements Hytale's plugin interface.
 * <p>
 * Example integration:
 * <pre>
 * public class HyperPermsPlugin implements HytalePlugin {
 *     private HyperPerms hyperPerms;
 *
 *     public void onEnable() {
 *         hyperPerms = HyperPermsBootstrap.create(getDataFolder().toPath(), getLogger());
 *         hyperPerms.enable();
 *     }
 *
 *     public void onDisable() {
 *         if (hyperPerms != null) {
 *             hyperPerms.disable();
 *         }
 *     }
 * }
 * </pre>
 */
public final class HyperPermsBootstrap {

    private static HyperPerms instance;

    private HyperPermsBootstrap() {}

    /**
     * Gets the running HyperPerms instance.
     *
     * @return the instance, or null if not initialized
     */
    public static HyperPerms getInstance() {
        return instance;
    }

    /**
     * Sets the running HyperPerms instance.
     * Should only be called by the platform plugin.
     *
     * @param hyperPerms the instance
     */
    public static void setInstance(HyperPerms hyperPerms) {
        instance = hyperPerms;
    }

    /**
     * Creates a new HyperPerms instance.
     *
     * @param dataDirectory the plugin data directory
     * @param logger        the parent logger
     * @return the HyperPerms instance
     */
    public static HyperPerms create(Path dataDirectory, java.util.logging.Logger logger) {
        return new HyperPerms(dataDirectory, logger);
    }

    /**
     * Standalone entry point for testing.
     * <p>
     * This allows running HyperPerms outside of the Hytale server environment
     * for testing and development purposes.
     *
     * @param args command line arguments (optional: data directory path)
     */
    public static void main(String[] args) {
        // Configure logging
        java.util.logging.Logger logger = java.util.logging.Logger.getLogger("HyperPerms");
        logger.setLevel(Level.ALL);
        ConsoleHandler handler = new ConsoleHandler();
        handler.setLevel(Level.ALL);
        logger.addHandler(handler);

        // Determine data directory
        Path dataDir;
        if (args.length > 0) {
            dataDir = Paths.get(args[0]);
        } else {
            dataDir = Paths.get(".", "hyperperms-data");
        }

        Logger.init(logger);
        Logger.info("Starting HyperPerms in standalone mode...");
        Logger.info("Data directory: " + dataDir.toAbsolutePath());

        // Create and enable
        HyperPerms hyperPerms = create(dataDir, logger);

        // Add shutdown hook
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            Logger.info("Shutting down...");
            hyperPerms.disable();
        }));

        try {
            hyperPerms.enable();
            Logger.info("HyperPerms is running. Press Ctrl+C to stop.");

            // Keep the main thread alive
            Thread.currentThread().join();
        } catch (Exception e) {
            Logger.severe("Failed to start HyperPerms", e);
            hyperPerms.disable();
            System.exit(1);
        }
    }
}
