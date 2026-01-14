package com.hyperperms.util;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Utility class for text and chat formatting.
 */
public final class TextUtil {

    // Color code pattern (Minecraft-style: &a, &b, etc.)
    private static final Pattern COLOR_PATTERN = Pattern.compile("&([0-9a-fk-or])");

    // Prefix for plugin messages
    private static final String PREFIX = "&8[&bHyperPerms&8] &7";

    private TextUtil() {}

    /**
     * Formats a message with the plugin prefix.
     *
     * @param message the message
     * @return the formatted message
     */
    @NotNull
    public static String prefix(@NotNull String message) {
        return PREFIX + message;
    }

    /**
     * Translates color codes from '&' to the appropriate format.
     * <p>
     * Note: When Hytale's text API is available, this should convert
     * to Hytale's native format.
     *
     * @param text the text with color codes
     * @return the translated text
     */
    @NotNull
    public static String colorize(@NotNull String text) {
        // TODO: Convert to Hytale's text format when API is available
        // For now, return as-is or convert to ANSI for console
        return text;
    }

    /**
     * Strips all color codes from text.
     *
     * @param text the text
     * @return the text without color codes
     */
    @NotNull
    public static String stripColor(@NotNull String text) {
        return COLOR_PATTERN.matcher(text).replaceAll("");
    }

    /**
     * Formats a boolean value for display.
     *
     * @param value the value
     * @return "&atrue" or "&cfalse"
     */
    @NotNull
    public static String formatBoolean(boolean value) {
        return value ? "&atrue" : "&cfalse";
    }

    /**
     * Truncates a string to a maximum length.
     *
     * @param text      the text
     * @param maxLength the maximum length
     * @return the truncated text
     */
    @NotNull
    public static String truncate(@NotNull String text, int maxLength) {
        if (text.length() <= maxLength) {
            return text;
        }
        return text.substring(0, maxLength - 3) + "...";
    }

    /**
     * Joins strings with a separator.
     *
     * @param separator the separator
     * @param items     the items to join
     * @return the joined string
     */
    @NotNull
    public static String join(@NotNull String separator, @NotNull Iterable<String> items) {
        StringBuilder sb = new StringBuilder();
        boolean first = true;
        for (String item : items) {
            if (!first) {
                sb.append(separator);
            }
            sb.append(item);
            first = false;
        }
        return sb.toString();
    }

    /**
     * Creates a progress bar string.
     *
     * @param current the current value
     * @param max     the maximum value
     * @param length  the bar length in characters
     * @return the progress bar
     */
    @NotNull
    public static String progressBar(int current, int max, int length) {
        if (max <= 0) {
            return "[" + "=".repeat(length) + "]";
        }
        int filled = (int) ((double) current / max * length);
        filled = Math.max(0, Math.min(length, filled));
        return "&8[&a" + "|".repeat(filled) + "&7" + "|".repeat(length - filled) + "&8]";
    }

    /**
     * Formats a number with thousand separators.
     *
     * @param number the number
     * @return the formatted number
     */
    @NotNull
    public static String formatNumber(long number) {
        return String.format("%,d", number);
    }

    /**
     * Capitalizes the first letter of a string.
     *
     * @param text the text
     * @return the capitalized text
     */
    @NotNull
    public static String capitalize(@Nullable String text) {
        if (text == null || text.isEmpty()) {
            return "";
        }
        return Character.toUpperCase(text.charAt(0)) + text.substring(1).toLowerCase();
    }

    /**
     * Formats a permission node for display.
     *
     * @param permission the permission
     * @param granted    whether it's granted
     * @return the formatted permission
     */
    @NotNull
    public static String formatPermission(@NotNull String permission, boolean granted) {
        String color = granted ? "&a" : "&c";
        String symbol = granted ? "+" : "-";
        return color + symbol + " " + permission;
    }
}
