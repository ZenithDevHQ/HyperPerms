package com.hyperperms.commands;

import com.hyperperms.HyperPerms;
import com.hyperperms.migration.*;
import com.hyperperms.migration.luckperms.LuckPermsMigrator;
import com.hyperperms.util.Logger;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.command.system.AbstractCommand;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.command.system.arguments.system.OptionalArg;
import com.hypixel.hytale.server.core.command.system.arguments.types.ArgTypes;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * Migration command: /hp migrate <source> [options]
 * <p>
 * Supports migrating from:
 * <ul>
 *   <li>luckperms - LuckPerms permission plugin</li>
 * </ul>
 * <p>
 * Options:
 * <ul>
 *   <li>--dry-run - Preview without making changes (default)</li>
 *   <li>--confirm - Actually execute the migration</li>
 *   <li>--verbose - Show detailed output</li>
 *   <li>--merge - Merge conflicting data (default)</li>
 *   <li>--skip - Skip conflicting items</li>
 *   <li>--overwrite - Overwrite existing data</li>
 * </ul>
 */
public class MigrateSubCommand extends AbstractCommand {

    private final HyperPerms hyperPerms;
    private final Map<String, PermissionMigrator> migrators;
    private final OptionalArg<String> sourceArg;
    private final OptionalArg<String> option1Arg;
    private final OptionalArg<String> option2Arg;
    private final OptionalArg<String> option3Arg;

    @SuppressWarnings("this-escape")
    public MigrateSubCommand(HyperPerms hyperPerms) {
        super("migrate", "Migrate permissions from another plugin");
        this.hyperPerms = hyperPerms;
        this.migrators = new HashMap<>();
        
        // Register available migrators
        migrators.put("luckperms", new LuckPermsMigrator(hyperPerms));
        
        // Register arguments
        this.sourceArg = withOptionalArg("source", "Source plugin (e.g., luckperms)", ArgTypes.STRING);
        this.option1Arg = withOptionalArg("option1", "Migration option", ArgTypes.STRING);
        this.option2Arg = withOptionalArg("option2", "Migration option", ArgTypes.STRING);
        this.option3Arg = withOptionalArg("option3", "Migration option", ArgTypes.STRING);
    }

    @Override
    protected CompletableFuture<Void> execute(CommandContext ctx) {
        String source = ctx.get(sourceArg);
        String opt1 = ctx.get(option1Arg);
        String opt2 = ctx.get(option2Arg);
        String opt3 = ctx.get(option3Arg);
        
        // Show help if no source specified
        if (source == null || source.isEmpty()) {
            showHelp(ctx);
            return CompletableFuture.completedFuture(null);
        }
        
        source = source.toLowerCase();
        
        // Handle help explicitly
        if (source.equals("help") || source.equals("?")) {
            showHelp(ctx);
            return CompletableFuture.completedFuture(null);
        }
        
        // Get migrator
        PermissionMigrator migrator = migrators.get(source);
        if (migrator == null) {
            sendError(ctx, "Unknown migration source: " + source);
            sendInfo(ctx, "Available sources: " + String.join(", ", migrators.keySet()));
            return CompletableFuture.completedFuture(null);
        }
        
        // Check if migration is possible
        if (!migrator.canMigrate()) {
            sendError(ctx, "Cannot migrate from " + migrator.getSourceName() + ":");
            sendError(ctx, "  - Data not found or not accessible");
            sendInfo(ctx, "Ensure the source plugin's data exists in the plugins folder.");
            return CompletableFuture.completedFuture(null);
        }
        
        // Parse options from the option arguments
        String[] optionArgs = collectOptions(opt1, opt2, opt3);
        MigrationOptions options = parseOptions(optionArgs);
        boolean confirm = hasFlag(optionArgs, "--confirm");
        boolean verbose = hasFlag(optionArgs, "--verbose");
        
        // Update options with verbose flag
        options = MigrationOptions.builder()
            .conflictResolution(options.conflictResolution())
            .verbose(verbose)
            .skipDefaultUsers(options.skipDefaultUsers())
            .skipExpired(options.skipExpired())
            .maxNodeLength(options.maxNodeLength())
            .build();
        
        if (confirm) {
            return executeMigration(ctx, migrator, options);
        } else {
            return executePreview(ctx, migrator, options);
        }
    }
    
    private CompletableFuture<Void> executePreview(CommandContext ctx, PermissionMigrator migrator, 
                                                    MigrationOptions options) {
        sendInfo(ctx, "Generating migration preview for " + migrator.getSourceName() + "...");
        sendInfo(ctx, "Source: " + migrator.getStorageDescription());
        
        return migrator.preview(options)
            .thenAccept(preview -> {
                // Send preview to player
                String[] lines = preview.toDisplayString(options.verbose()).split("\n");
                for (String line : lines) {
                    ctx.sender().sendMessage(parseColoredMessage(line));
                }
            })
            .exceptionally(e -> {
                sendError(ctx, "Failed to generate preview: " + e.getMessage());
                Logger.severe("Migration preview failed", e);
                return null;
            });
    }
    
    private CompletableFuture<Void> executeMigration(CommandContext ctx, PermissionMigrator migrator,
                                                      MigrationOptions options) {
        sendWarning(ctx, "Starting migration from " + migrator.getSourceName() + "...");
        sendInfo(ctx, "Source: " + migrator.getStorageDescription());
        sendInfo(ctx, "This may take a while for large datasets.");
        
        // Create progress callback that sends updates to player
        MigrationProgressCallback callback = new MigrationProgressCallback() {
            @Override
            public void onPhaseStart(@NotNull String phase, int totalItems) {
                if (totalItems > 0) {
                    sendInfo(ctx, "Starting: " + phase + " (" + totalItems + " items)");
                } else {
                    sendInfo(ctx, "Starting: " + phase);
                }
            }
            
            @Override
            public void onProgress(int itemsProcessed, @NotNull String currentItem) {
                // Only send progress updates for significant milestones
                // to avoid flooding the chat
            }
            
            @Override
            public void onPhaseComplete(@NotNull String phase, int itemsProcessed) {
                sendSuccess(ctx, "Completed: " + phase + " (" + itemsProcessed + " items)");
            }
            
            @Override
            public void onWarning(@NotNull String message) {
                sendWarning(ctx, message);
            }
            
            @Override
            public void onError(@NotNull String message, boolean fatal) {
                if (fatal) {
                    sendError(ctx, "FATAL: " + message);
                } else {
                    sendError(ctx, message);
                }
            }
        };
        
        return migrator.migrate(options, callback)
            .thenAccept(result -> {
                // Send result to player
                String[] lines = result.toDisplayString().split("\n");
                for (String line : lines) {
                    ctx.sender().sendMessage(parseColoredMessage(line));
                }
                
                if (result.success()) {
                    Logger.info("Migration from %s completed successfully", migrator.getSourceName());
                } else {
                    Logger.warn("Migration from %s failed: %s", migrator.getSourceName(), result.errorMessage());
                }
            })
            .exceptionally(e -> {
                sendError(ctx, "Migration failed: " + e.getMessage());
                Logger.severe("Migration failed", e);
                return null;
            });
    }
    
    private void showHelp(CommandContext ctx) {
        ctx.sender().sendMessage(Message.raw(""));
        ctx.sender().sendMessage(parseColoredMessage("§6=== HyperPerms Migration Tool ===§r"));
        ctx.sender().sendMessage(Message.raw(""));
        ctx.sender().sendMessage(parseColoredMessage("§eUsage:§r"));
        ctx.sender().sendMessage(parseColoredMessage("  §f/hp migrate <source>§7 - Preview migration (dry-run)"));
        ctx.sender().sendMessage(parseColoredMessage("  §f/hp migrate <source> --confirm§7 - Execute migration"));
        ctx.sender().sendMessage(Message.raw(""));
        ctx.sender().sendMessage(parseColoredMessage("§eAvailable Sources:§r"));
        
        for (Map.Entry<String, PermissionMigrator> entry : migrators.entrySet()) {
            PermissionMigrator m = entry.getValue();
            String status = m.canMigrate() ? "§a✓ Available" : "§c✗ Not found";
            ctx.sender().sendMessage(parseColoredMessage("  §f" + entry.getKey() + " §7- " + m.getSourceName() + " " + status));
        }
        
        ctx.sender().sendMessage(Message.raw(""));
        ctx.sender().sendMessage(parseColoredMessage("§eOptions:§r"));
        ctx.sender().sendMessage(parseColoredMessage("  §f--dry-run§7   - Preview without changes (default)"));
        ctx.sender().sendMessage(parseColoredMessage("  §f--confirm§7   - Execute the migration"));
        ctx.sender().sendMessage(parseColoredMessage("  §f--verbose§7   - Show detailed output"));
        ctx.sender().sendMessage(parseColoredMessage("  §f--merge§7     - Merge conflicting data (default)"));
        ctx.sender().sendMessage(parseColoredMessage("  §f--skip§7      - Skip conflicting items"));
        ctx.sender().sendMessage(parseColoredMessage("  §f--overwrite§7 - Replace existing data"));
        ctx.sender().sendMessage(Message.raw(""));
        ctx.sender().sendMessage(parseColoredMessage("§eExamples:§r"));
        ctx.sender().sendMessage(parseColoredMessage("  §f/hp migrate luckperms§7          - Preview migration"));
        ctx.sender().sendMessage(parseColoredMessage("  §f/hp migrate luckperms --confirm§7 - Execute migration"));
        ctx.sender().sendMessage(parseColoredMessage("  §f/hp migrate luckperms --verbose§7 - Detailed preview"));
        ctx.sender().sendMessage(Message.raw(""));
    }
    
    private MigrationOptions parseOptions(String[] args) {
        ConflictResolution resolution = ConflictResolution.MERGE;
        
        for (String arg : args) {
            switch (arg.toLowerCase()) {
                case "--merge" -> resolution = ConflictResolution.MERGE;
                case "--skip" -> resolution = ConflictResolution.SKIP;
                case "--overwrite" -> resolution = ConflictResolution.OVERWRITE;
            }
        }
        
        return MigrationOptions.builder()
            .conflictResolution(resolution)
            .skipExpired(true)
            .build();
    }
    
    /**
     * Collects non-null option arguments into an array.
     */
    private String[] collectOptions(String... options) {
        return java.util.Arrays.stream(options)
            .filter(o -> o != null && !o.isEmpty())
            .toArray(String[]::new);
    }
    
    private boolean hasFlag(String[] args, String flag) {
        for (String arg : args) {
            if (arg.equalsIgnoreCase(flag)) {
                return true;
            }
        }
        return false;
    }
    
    // Message helpers
    
    private void sendSuccess(CommandContext ctx, String message) {
        ctx.sender().sendMessage(parseColoredMessage("§a[Migration] " + message));
    }
    
    private void sendInfo(CommandContext ctx, String message) {
        ctx.sender().sendMessage(parseColoredMessage("§7[Migration] " + message));
    }
    
    private void sendWarning(CommandContext ctx, String message) {
        ctx.sender().sendMessage(parseColoredMessage("§e[Migration] " + message));
    }
    
    private void sendError(CommandContext ctx, String message) {
        ctx.sender().sendMessage(parseColoredMessage("§c[Migration] " + message));
    }
    
    /**
     * Parses a string with § color codes into a Message.
     * Handles §a, §c, §e, §f, §6, §7, §r for common colors.
     */
    private Message parseColoredMessage(String text) {
        // Simple implementation - in production, use a proper parser
        Message message = Message.raw("");
        StringBuilder current = new StringBuilder();
        java.awt.Color currentColor = null;
        boolean bold = false;
        
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            
            if (c == '§' && i + 1 < text.length()) {
                // Flush current segment
                if (current.length() > 0) {
                    Message segment = Message.raw(current.toString());
                    if (currentColor != null) {
                        segment = segment.color(currentColor);
                    }
                    if (bold) {
                        segment = segment.bold(true);
                    }
                    message = message.insert(segment);
                    current = new StringBuilder();
                }
                
                char code = text.charAt(i + 1);
                currentColor = switch (code) {
                    case '0' -> java.awt.Color.BLACK;
                    case '1' -> new java.awt.Color(0x0000AA);
                    case '2' -> new java.awt.Color(0x00AA00);
                    case '3' -> new java.awt.Color(0x00AAAA);
                    case '4' -> new java.awt.Color(0xAA0000);
                    case '5' -> new java.awt.Color(0xAA00AA);
                    case '6' -> new java.awt.Color(0xFFAA00);
                    case '7' -> new java.awt.Color(0xAAAAAA);
                    case '8' -> new java.awt.Color(0x555555);
                    case '9' -> new java.awt.Color(0x5555FF);
                    case 'a' -> new java.awt.Color(0x55FF55);
                    case 'b' -> new java.awt.Color(0x55FFFF);
                    case 'c' -> new java.awt.Color(0xFF5555);
                    case 'd' -> new java.awt.Color(0xFF55FF);
                    case 'e' -> new java.awt.Color(0xFFFF55);
                    case 'f' -> java.awt.Color.WHITE;
                    case 'r' -> { bold = false; yield null; }
                    case 'l' -> { bold = true; yield currentColor; }
                    default -> currentColor;
                };
                i++; // Skip code character
            } else {
                current.append(c);
            }
        }
        
        // Flush remaining
        if (current.length() > 0) {
            Message segment = Message.raw(current.toString());
            if (currentColor != null) {
                segment = segment.color(currentColor);
            }
            if (bold) {
                segment = segment.bold(true);
            }
            message = message.insert(segment);
        }
        
        return message;
    }
}
