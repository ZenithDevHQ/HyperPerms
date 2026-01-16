package com.hyperperms.chat;

import com.hyperperms.HyperPerms;
import com.hyperperms.util.Logger;
import com.hypixel.hytale.event.EventPriority;
import com.hypixel.hytale.event.EventRegistration;
import com.hypixel.hytale.event.EventRegistry;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.event.events.player.PlayerChatEvent;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.awt.Color;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Hytale chat event listener for HyperPerms.
 * 
 * <p>This listener intercepts player chat messages and applies:
 * <ul>
 *   <li>Prefix/suffix from permissions groups</li>
 *   <li>Custom formatting from config</li>
 *   <li>Color code processing</li>
 *   <li>Placeholder replacement</li>
 * </ul>
 * 
 * <p>The listener registers as an async event handler to avoid blocking
 * the main thread while loading player data.
 */
public class ChatListener {
    
    private final HyperPerms plugin;
    private final ChatManager chatManager;
    
    // Event registration handle (for unregistering)
    private EventRegistration<?, ?> chatEventRegistration;
    
    // Message conversion patterns
    private static final Pattern COLOR_CODE_PATTERN = Pattern.compile("§([0-9a-fk-or])");
    private static final Pattern HEX_COLOR_PATTERN = Pattern.compile("§x(§[0-9a-fA-F]){6}");
    private static final Pattern SIMPLE_HEX_PATTERN = Pattern.compile("#([0-9a-fA-F]{6})");
    
    // Configuration
    private volatile boolean allowPlayerColors = true;
    private volatile String colorPermission = "hyperperms.chat.color";
    
    /**
     * Creates a new ChatListener.
     *
     * @param plugin the HyperPerms plugin instance
     * @param chatManager the chat manager
     */
    public ChatListener(@NotNull HyperPerms plugin, @NotNull ChatManager chatManager) {
        this.plugin = Objects.requireNonNull(plugin, "plugin cannot be null");
        this.chatManager = Objects.requireNonNull(chatManager, "chatManager cannot be null");
    }
    
    /**
     * Registers the chat event listener with the Hytale event registry.
     *
     * @param eventRegistry the event registry
     */
    public void register(@NotNull EventRegistry eventRegistry) {
        Objects.requireNonNull(eventRegistry, "eventRegistry cannot be null");
        
        // Register as async GLOBAL handler since we may need to load data
        // and we want to handle ALL chat events regardless of key
        // Use FIRST priority to run BEFORE other chat plugins (like WerChat)
        // We'll store our prefix data for other plugins to use
        chatEventRegistration = eventRegistry.registerAsyncGlobal(
            EventPriority.FIRST,
            PlayerChatEvent.class,
            this::onPlayerChatAsync
        );
        
        Logger.info("Chat listener registered");
    }
    
    /**
     * Unregisters the chat event listener.
     * <p>
     * Note: Hytale's EventRegistry doesn't have a direct unregister method.
     * Event registrations are typically cleaned up when the plugin is disabled.
     *
     * @param eventRegistry the event registry (currently unused)
     */
    public void unregister(@NotNull EventRegistry eventRegistry) {
        // Hytale's EventRegistry doesn't have a direct unregister method
        // The registration is cleaned up automatically when the plugin shuts down
        if (chatEventRegistration != null) {
            chatEventRegistration = null;
            Logger.info("Chat listener marked for cleanup");
        }
    }
    
    /**
     * Handles the player chat event asynchronously.
     *
     * @param futureEvent the future containing the chat event
     * @return the modified future
     */
    private CompletableFuture<PlayerChatEvent> onPlayerChatAsync(
            CompletableFuture<PlayerChatEvent> futureEvent) {
        
        return futureEvent.thenCompose(event -> {
            try {
                Logger.info("[Chat Debug] Chat event received, cancelled=" + event.isCancelled());
                
                // If chat formatting is disabled or event is cancelled, pass through
                if (!chatManager.isEnabled()) {
                    Logger.info("[Chat Debug] Chat manager disabled, passing through");
                    return CompletableFuture.completedFuture(event);
                }
                
                if (event.isCancelled()) {
                    Logger.info("[Chat Debug] Event cancelled, passing through");
                    return CompletableFuture.completedFuture(event);
                }
                
                PlayerRef sender = event.getSender();
                if (sender == null) {
                    Logger.warn("[Chat Debug] Sender is null, passing through");
                    return CompletableFuture.completedFuture(event);
                }
                
                UUID uuid = sender.getUuid();
                String playerName = sender.getUsername();
                String content = event.getContent();
                
                Logger.info("[Chat Debug] Processing chat from " + playerName + ": " + content);
                
                // Process player colors in message if allowed
                String processedContent = processPlayerColors(uuid, content);
                
                // Format the chat message
                return chatManager.formatChatMessage(uuid, playerName, processedContent)
                    .thenApply(formattedMessage -> {
                        Logger.info("[Chat Debug] Formatted: " + formattedMessage.getFormatted());
                        
                        // Check if another plugin (like WerChat) already set a formatter
                        PlayerChatEvent.Formatter existingFormatter = event.getFormatter();
                        boolean hasCustomFormatter = existingFormatter != null 
                            && existingFormatter != PlayerChatEvent.DEFAULT_FORMATTER;
                        
                        if (hasCustomFormatter) {
                            // Wrap the existing formatter to inject our prefix/suffix
                            Logger.info("[Chat Debug] Wrapping existing formatter with prefix/suffix");
                            event.setFormatter(new WrappingFormatter(existingFormatter, formattedMessage));
                        } else {
                            // No other formatter - use our full formatter
                            event.setFormatter(new HyperPermsFormatter(formattedMessage));
                        }
                        return event;
                    })
                    .exceptionally(e -> {
                        Logger.warn("Failed to format chat message for %s: %s", playerName, e.getMessage());
                        e.printStackTrace();
                        // Return original event on error - don't set formatter
                        return event;
                    });
            } catch (Exception e) {
                Logger.warn("[Chat] Exception in chat handler: " + e.getMessage());
                e.printStackTrace();
                return CompletableFuture.completedFuture(event);
            }
        });
    }
    
    /**
     * Processes color codes in player messages if they have permission.
     */
    private String processPlayerColors(@NotNull UUID uuid, @NotNull String content) {
        if (!allowPlayerColors) {
            return content;
        }
        
        // Check if player has color permission
        if (!plugin.hasPermission(uuid, colorPermission)) {
            // Strip any color codes they tried to use
            return ColorUtil.stripColors(content);
        }
        
        // Player has permission - let colors through
        // They will be processed by the formatter
        return content;
    }
    
    // ========== Configuration Methods ==========
    
    /**
     * Sets whether players can use color codes in their messages.
     */
    public void setAllowPlayerColors(boolean allowPlayerColors) {
        this.allowPlayerColors = allowPlayerColors;
    }
    
    /**
     * Gets whether players can use color codes in their messages.
     */
    public boolean isAllowPlayerColors() {
        return allowPlayerColors;
    }
    
    /**
     * Sets the permission required for players to use color codes.
     */
    public void setColorPermission(@NotNull String colorPermission) {
        this.colorPermission = Objects.requireNonNull(colorPermission, "colorPermission cannot be null");
    }
    
    /**
     * Gets the permission required for players to use color codes.
     */
    @NotNull
    public String getColorPermission() {
        return colorPermission;
    }
    
    // ========== Message Conversion ==========
    
    /**
     * Converts a HyperPerms formatted string to a Hytale Message.
     * Uses Hytale's native Message.color() API for proper color support.
     *
     * @param formatted the formatted string with color codes
     * @return a Hytale Message object
     */
    @NotNull
    public static Message toHytaleMessage(@NotNull String formatted) {
        if (formatted == null || formatted.isEmpty()) {
            return Message.raw("");
        }
        
        try {
            // Parse § codes and build Message with proper colors
            Message result = null;
            StringBuilder currentText = new StringBuilder();
            Color currentColor = null;
            boolean bold = false;
            boolean italic = false;
            
            int i = 0;
            while (i < formatted.length()) {
                char c = formatted.charAt(i);
                
                // Check for color/format code (§X)
                if (c == '§' && i + 1 < formatted.length()) {
                    // Flush current segment before changing format
                    if (currentText.length() > 0) {
                        Message segment = Message.raw(currentText.toString());
                        if (currentColor != null) {
                            segment = segment.color(currentColor);
                        }
                        if (bold) {
                            segment = segment.bold(true);
                        }
                        if (italic) {
                            segment = segment.italic(true);
                        }
                        result = (result == null) ? segment : Message.join(result, segment);
                        currentText.setLength(0);
                    }
                    
                    char code = Character.toLowerCase(formatted.charAt(i + 1));
                    
                    // Handle the code
                    switch (code) {
                        // Colors (reset format flags like Minecraft)
                        case '0' -> { currentColor = new Color(0x000000); bold = false; italic = false; }
                        case '1' -> { currentColor = new Color(0x0000AA); bold = false; italic = false; }
                        case '2' -> { currentColor = new Color(0x00AA00); bold = false; italic = false; }
                        case '3' -> { currentColor = new Color(0x00AAAA); bold = false; italic = false; }
                        case '4' -> { currentColor = new Color(0xAA0000); bold = false; italic = false; }
                        case '5' -> { currentColor = new Color(0xAA00AA); bold = false; italic = false; }
                        case '6' -> { currentColor = new Color(0xFFAA00); bold = false; italic = false; }
                        case '7' -> { currentColor = new Color(0xAAAAAA); bold = false; italic = false; }
                        case '8' -> { currentColor = new Color(0x555555); bold = false; italic = false; }
                        case '9' -> { currentColor = new Color(0x5555FF); bold = false; italic = false; }
                        case 'a' -> { currentColor = new Color(0x55FF55); bold = false; italic = false; }
                        case 'b' -> { currentColor = new Color(0x55FFFF); bold = false; italic = false; }
                        case 'c' -> { currentColor = new Color(0xFF5555); bold = false; italic = false; }
                        case 'd' -> { currentColor = new Color(0xFF55FF); bold = false; italic = false; }
                        case 'e' -> { currentColor = new Color(0xFFFF55); bold = false; italic = false; }
                        case 'f' -> { currentColor = new Color(0xFFFFFF); bold = false; italic = false; }
                        // Formats
                        case 'l' -> bold = true;
                        case 'o' -> italic = true;
                        // Reset
                        case 'r' -> { currentColor = null; bold = false; italic = false; }
                        // Unknown - skip
                        default -> { }
                    }
                    
                    i += 2;
                    continue;
                }
                
                // Regular character
                currentText.append(c);
                i++;
            }
            
            // Flush remaining segment
            if (currentText.length() > 0) {
                Message segment = Message.raw(currentText.toString());
                if (currentColor != null) {
                    segment = segment.color(currentColor);
                }
                if (bold) {
                    segment = segment.bold(true);
                }
                if (italic) {
                    segment = segment.italic(true);
                }
                result = (result == null) ? segment : Message.join(result, segment);
            }
            
            return result != null ? result : Message.raw("");
        } catch (Exception e) {
            Logger.warn("[Chat] Failed to parse colors, using stripped: " + e.getMessage());
            return Message.raw(ColorUtil.stripColors(formatted));
        }
    }
    
    /**
     * Parses a formatted string with color codes into a Hytale Message.
     */
    private static Message parseFormattedString(@NotNull String formatted) {
        // State tracking
        StringBuilder currentSegment = new StringBuilder();
        Message result = Message.empty();
        
        Color currentColor = null;
        boolean bold = false;
        boolean italic = false;
        boolean underline = false;
        boolean strikethrough = false;
        boolean magic = false; // Obfuscated
        
        int i = 0;
        while (i < formatted.length()) {
            char c = formatted.charAt(i);
            
            // Check for hex color (§x§r§r§g§g§b§b)
            if (c == '§' && i + 13 < formatted.length() && formatted.charAt(i + 1) == 'x') {
                // Flush current segment
                if (currentSegment.length() > 0) {
                    result = appendSegment(result, currentSegment.toString(), currentColor, bold, italic);
                    currentSegment.setLength(0);
                }
                
                // Parse hex color
                try {
                    StringBuilder hex = new StringBuilder("#");
                    for (int j = 0; j < 6; j++) {
                        int idx = i + 2 + (j * 2) + 1; // Skip §x and then each §
                        hex.append(formatted.charAt(idx));
                    }
                    currentColor = Color.decode(hex.toString());
                    i += 14; // Skip §x§r§r§g§g§b§b
                    continue;
                } catch (Exception e) {
                    // Fall through to normal processing
                }
            }
            
            // Check for simple hex (#RRGGBB) - shouldn't normally be here but just in case
            if (c == '#' && i + 6 < formatted.length()) {
                String possibleHex = formatted.substring(i, i + 7);
                if (SIMPLE_HEX_PATTERN.matcher(possibleHex).matches()) {
                    // Flush current segment
                    if (currentSegment.length() > 0) {
                        result = appendSegment(result, currentSegment.toString(), currentColor, bold, italic);
                        currentSegment.setLength(0);
                    }
                    
                    try {
                        currentColor = Color.decode(possibleHex);
                        i += 7;
                        continue;
                    } catch (Exception e) {
                        // Fall through
                    }
                }
            }
            
            // Check for standard color/format code (§X)
            if (c == '§' && i + 1 < formatted.length()) {
                char code = Character.toLowerCase(formatted.charAt(i + 1));
                
                // Flush current segment before applying new format
                if (currentSegment.length() > 0 && (isColorCode(code) || code == 'r')) {
                    result = appendSegment(result, currentSegment.toString(), currentColor, bold, italic);
                    currentSegment.setLength(0);
                }
                
                // Handle the code
                switch (code) {
                    // Colors
                    case '0': currentColor = new Color(0x000000); resetFormats(); break;
                    case '1': currentColor = new Color(0x0000AA); resetFormats(); break;
                    case '2': currentColor = new Color(0x00AA00); resetFormats(); break;
                    case '3': currentColor = new Color(0x00AAAA); resetFormats(); break;
                    case '4': currentColor = new Color(0xAA0000); resetFormats(); break;
                    case '5': currentColor = new Color(0xAA00AA); resetFormats(); break;
                    case '6': currentColor = new Color(0xFFAA00); resetFormats(); break;
                    case '7': currentColor = new Color(0xAAAAAA); resetFormats(); break;
                    case '8': currentColor = new Color(0x555555); resetFormats(); break;
                    case '9': currentColor = new Color(0x5555FF); resetFormats(); break;
                    case 'a': currentColor = new Color(0x55FF55); resetFormats(); break;
                    case 'b': currentColor = new Color(0x55FFFF); resetFormats(); break;
                    case 'c': currentColor = new Color(0xFF5555); resetFormats(); break;
                    case 'd': currentColor = new Color(0xFF55FF); resetFormats(); break;
                    case 'e': currentColor = new Color(0xFFFF55); resetFormats(); break;
                    case 'f': currentColor = new Color(0xFFFFFF); resetFormats(); break;
                    
                    // Formats
                    case 'k': magic = true; break;
                    case 'l': bold = true; break;
                    case 'm': strikethrough = true; break;
                    case 'n': underline = true; break;
                    case 'o': italic = true; break;
                    
                    // Reset
                    case 'r':
                        currentColor = null;
                        bold = italic = underline = strikethrough = magic = false;
                        break;
                    
                    default:
                        // Unknown code, include literally
                        currentSegment.append(c).append(formatted.charAt(i + 1));
                        break;
                }
                
                i += 2;
                continue;
            }
            
            // Regular character
            currentSegment.append(c);
            i++;
        }
        
        // Flush remaining segment
        if (currentSegment.length() > 0) {
            result = appendSegment(result, currentSegment.toString(), currentColor, bold, italic);
        }
        
        return result;
    }
    
    /**
     * Helper method to reset format flags (can't actually use in static context,
     * but leaving for clarity - the actual implementation handles this inline).
     */
    private static void resetFormats() {
        // This is handled inline in the switch statement
    }
    
    /**
     * Checks if a character is a color code (0-9, a-f).
     */
    private static boolean isColorCode(char c) {
        return (c >= '0' && c <= '9') || (c >= 'a' && c <= 'f');
    }
    
    /**
     * Appends a text segment to the message with styling.
     */
    private static Message appendSegment(
            @NotNull Message base,
            @NotNull String text,
            @Nullable Color color,
            boolean bold,
            boolean italic) {
        
        if (text.isEmpty()) {
            return base;
        }
        
        Message segment = Message.raw(text);
        
        if (color != null) {
            segment = segment.color(color);
        }
        if (bold) {
            segment = segment.bold(true);
        }
        if (italic) {
            segment = segment.italic(true);
        }
        
        // Join with existing message - handle null from getRawText()
        String baseText = base.getRawText();
        if (baseText == null || baseText.isEmpty()) {
            return segment;
        }
        return Message.join(base, segment);
    }
    
    // ========== Inner Classes ==========
    
    /**
     * Custom formatter that uses HyperPerms formatted messages.
     */
    private static class HyperPermsFormatter implements PlayerChatEvent.Formatter {
        
        private final ChatManager.FormattedChatMessage formattedMessage;
        
        HyperPermsFormatter(@NotNull ChatManager.FormattedChatMessage formattedMessage) {
            this.formattedMessage = formattedMessage;
        }
        
        @Override
        public Message format(PlayerRef sender, String content) {
            try {
                String formatted = formattedMessage.getFormatted();
                Logger.info("[Chat Debug] Formatting message: " + formatted);
                
                if (formatted == null || formatted.isEmpty()) {
                    Logger.warn("[Chat] Formatted message was empty, using fallback");
                    String fallback = sender.getUsername() + ": " + content;
                    return Message.raw(fallback);
                }
                
                // Convert our formatted string to a Hytale Message
                Message result = toHytaleMessage(formatted);
                
                if (result == null) {
                    Logger.warn("[Chat] toHytaleMessage returned null, using fallback");
                    return Message.raw(formatted);
                }
                
                return result;
            } catch (Exception e) {
                Logger.warn("[Chat] Error formatting message: " + e.getMessage());
                e.printStackTrace();
                // Fallback to simple format
                return Message.raw(sender.getUsername() + ": " + content);
            }
        }
    }
    
    /**
     * Formatter that wraps another plugin's formatter (like WerChat) and injects
     * HyperPerms prefix/suffix into the output.
     * <p>
     * This allows both plugins to coexist: WerChat handles channel formatting,
     * HyperPerms injects rank prefixes/suffixes.
     */
    private static class WrappingFormatter implements PlayerChatEvent.Formatter {
        
        private final PlayerChatEvent.Formatter wrapped;
        private final ChatManager.FormattedChatMessage formattedMessage;
        
        WrappingFormatter(@NotNull PlayerChatEvent.Formatter wrapped,
                         @NotNull ChatManager.FormattedChatMessage formattedMessage) {
            this.wrapped = wrapped;
            this.formattedMessage = formattedMessage;
        }
        
        @Override
        public Message format(PlayerRef sender, String content) {
            try {
                // Get the prefix and suffix from HyperPerms
                String prefix = formattedMessage.getPrefix();
                String suffix = formattedMessage.getSuffix();
                
                Logger.info("[Chat Debug] Wrapping formatter - prefix: '" + prefix + "', suffix: '" + suffix + "'");
                
                // Let the wrapped formatter (e.g., WerChat) format the message
                Message wrappedResult = wrapped.format(sender, content);
                
                if (wrappedResult == null) {
                    Logger.warn("[Chat] Wrapped formatter returned null");
                    return Message.raw(sender.getUsername() + ": " + content);
                }
                
                // If we have no prefix/suffix, just return the wrapped result
                if ((prefix == null || prefix.isEmpty()) && (suffix == null || suffix.isEmpty())) {
                    return wrappedResult;
                }
                
                // Build the final message: wrapped output (channel) + prefix + rest
                // WerChat outputs: [Global] PlayerName: message
                // We want: [Global][Owner] PlayerName: message
                // So we need to inject prefix after the channel tag
                
                // Get the raw text to find where to inject
                String wrappedText = wrappedResult.getRawText();
                
                if (wrappedText != null && prefix != null && !prefix.isEmpty()) {
                    // Find the player name in the wrapped output
                    String playerName = sender.getUsername();
                    int playerNameIndex = wrappedText.indexOf(playerName);
                    
                    if (playerNameIndex > 0) {
                        // Insert prefix right before the player name
                        // This turns "[Global] Player:" into "[Global][Owner] Player:"
                        String before = wrappedText.substring(0, playerNameIndex);
                        String after = wrappedText.substring(playerNameIndex);
                        String newText = before + ColorUtil.colorize(prefix) + after;
                        return toHytaleMessage(newText);
                    }
                }
                
                // Fallback: just append prefix after wrapped result
                if (prefix != null && !prefix.isEmpty()) {
                    Message prefixMsg = toHytaleMessage(ColorUtil.colorize(prefix));
                    return Message.join(wrappedResult, prefixMsg);
                }
                
                return wrappedResult;
            } catch (Exception e) {
                Logger.warn("[Chat] Error in wrapping formatter: " + e.getMessage());
                e.printStackTrace();
                // Fallback to wrapped result or simple format
                try {
                    return wrapped.format(sender, content);
                } catch (Exception e2) {
                    return Message.raw(sender.getUsername() + ": " + content);
                }
            }
        }
    }
    
    /**
     * Alternative formatter that allows dynamic formatting at send time.
     * Useful if you want different formatting per recipient.
     */
    public static class DynamicFormatter implements PlayerChatEvent.Formatter {
        
        private final ChatManager chatManager;
        
        public DynamicFormatter(@NotNull ChatManager chatManager) {
            this.chatManager = chatManager;
        }
        
        @Override
        public Message format(PlayerRef sender, String content) {
            // This is called synchronously, so we use cached data
            // The async handler should have already loaded the data
            return Message.raw(content);
        }
    }
}
