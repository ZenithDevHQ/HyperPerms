package com.hyperperms.chat;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.awt.Color;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Comprehensive color utility for handling all color code formats.
 * <p>
 * Supports:
 * <ul>
 *   <li>Legacy color codes: {@code &a}, {@code &c}, {@code &l}, etc.</li>
 *   <li>Hex colors: {@code &#RRGGBB}, {@code &#RGB}, {@code &x&R&R&G&G&B&B}</li>
 *   <li>Named colors: {@code {red}}, {@code {gold}}, {@code {aqua}}</li>
 *   <li>Gradients: {@code <gradient:#FF0000:#0000FF>text</gradient>}</li>
 *   <li>Rainbow: {@code <rainbow>text</rainbow>}</li>
 * </ul>
 * <p>
 * The section symbol (§) is used as the final color code character for Hytale.
 * If Hytale uses a different character, update {@link #COLOR_CHAR}.
 */
public final class ColorUtil {

    /**
     * The color code character used by Hytale.
     * This is the character that precedes color codes in the final output.
     */
    public static final char COLOR_CHAR = '§';

    /**
     * The alternate color code character used in configuration/input.
     * Users type this, and it gets translated to {@link #COLOR_CHAR}.
     */
    public static final char ALT_COLOR_CHAR = '&';

    // ==================== Patterns ====================

    /**
     * Pattern for legacy color codes: &0-9, &a-f, &k-o, &r (case insensitive)
     */
    private static final Pattern LEGACY_PATTERN = Pattern.compile(
        "[" + ALT_COLOR_CHAR + COLOR_CHAR + "]([0-9a-fk-orA-FK-OR])"
    );

    /**
     * Pattern for 6-digit hex codes: &#RRGGBB or &x&R&R&G&G&B&B
     */
    private static final Pattern HEX_PATTERN = Pattern.compile(
        "[" + ALT_COLOR_CHAR + COLOR_CHAR + "]#([A-Fa-f0-9]{6})"
    );

    /**
     * Pattern for 3-digit hex codes: &#RGB (expands to &#RRGGBB)
     */
    private static final Pattern HEX_SHORT_PATTERN = Pattern.compile(
        "[" + ALT_COLOR_CHAR + COLOR_CHAR + "]#([A-Fa-f0-9]{3})(?![A-Fa-f0-9])"
    );

    /**
     * Pattern for Bukkit-style hex: &x&R&R&G&G&B&B
     */
    private static final Pattern BUKKIT_HEX_PATTERN = Pattern.compile(
        "[" + ALT_COLOR_CHAR + COLOR_CHAR + "]x" +
        "([" + ALT_COLOR_CHAR + COLOR_CHAR + "][A-Fa-f0-9]){6}"
    );

    /**
     * Pattern for named colors: {red}, {gold}, {aqua}
     */
    private static final Pattern NAMED_COLOR_PATTERN = Pattern.compile(
        "\\{([a-zA-Z_]+)\\}"
    );

    /**
     * Pattern for gradients: <gradient:#RRGGBB:#RRGGBB>text</gradient>
     */
    private static final Pattern GRADIENT_PATTERN = Pattern.compile(
        "<gradient:([#A-Fa-f0-9]+):([#A-Fa-f0-9]+)>(.*?)</gradient>",
        Pattern.DOTALL
    );

    /**
     * Pattern for multi-stop gradients: <gradient:#RRGGBB:#RRGGBB:#RRGGBB>text</gradient>
     */
    private static final Pattern MULTI_GRADIENT_PATTERN = Pattern.compile(
        "<gradient:([#A-Fa-f0-9:]+)>(.*?)</gradient>",
        Pattern.DOTALL
    );

    /**
     * Pattern for rainbow text: <rainbow>text</rainbow> or <rainbow:phase>text</rainbow>
     */
    private static final Pattern RAINBOW_PATTERN = Pattern.compile(
        "<rainbow(?::([0-9.]+))?>(.*?)</rainbow>",
        Pattern.DOTALL
    );

    /**
     * Pattern to strip all color codes
     */
    private static final Pattern STRIP_PATTERN = Pattern.compile(
        "[" + COLOR_CHAR + "][0-9a-fk-orA-FK-OR]|" +
        "[" + COLOR_CHAR + "]x([" + COLOR_CHAR + "][A-Fa-f0-9]){6}|" +
        "[" + COLOR_CHAR + "]#[A-Fa-f0-9]{6}"
    );

    // ==================== Named Colors ====================

    /**
     * Map of named colors to their hex values.
     * Includes all Minecraft colors plus extended palette.
     */
    private static final Map<String, String> NAMED_COLORS = new HashMap<>();

    static {
        // Standard Minecraft colors
        NAMED_COLORS.put("black", "000000");
        NAMED_COLORS.put("dark_blue", "0000AA");
        NAMED_COLORS.put("dark_green", "00AA00");
        NAMED_COLORS.put("dark_aqua", "00AAAA");
        NAMED_COLORS.put("dark_red", "AA0000");
        NAMED_COLORS.put("dark_purple", "AA00AA");
        NAMED_COLORS.put("gold", "FFAA00");
        NAMED_COLORS.put("gray", "AAAAAA");
        NAMED_COLORS.put("grey", "AAAAAA"); // Alias
        NAMED_COLORS.put("dark_gray", "555555");
        NAMED_COLORS.put("dark_grey", "555555"); // Alias
        NAMED_COLORS.put("blue", "5555FF");
        NAMED_COLORS.put("green", "55FF55");
        NAMED_COLORS.put("aqua", "55FFFF");
        NAMED_COLORS.put("cyan", "55FFFF"); // Alias
        NAMED_COLORS.put("red", "FF5555");
        NAMED_COLORS.put("light_purple", "FF55FF");
        NAMED_COLORS.put("pink", "FF55FF"); // Alias
        NAMED_COLORS.put("magenta", "FF55FF"); // Alias
        NAMED_COLORS.put("yellow", "FFFF55");
        NAMED_COLORS.put("white", "FFFFFF");

        // Extended palette
        NAMED_COLORS.put("orange", "FF8800");
        NAMED_COLORS.put("lime", "88FF00");
        NAMED_COLORS.put("teal", "00AA88");
        NAMED_COLORS.put("indigo", "4B0082");
        NAMED_COLORS.put("violet", "8800FF");
        NAMED_COLORS.put("coral", "FF7F50");
        NAMED_COLORS.put("salmon", "FA8072");
        NAMED_COLORS.put("crimson", "DC143C");
        NAMED_COLORS.put("maroon", "800000");
        NAMED_COLORS.put("olive", "808000");
        NAMED_COLORS.put("navy", "000080");
        NAMED_COLORS.put("forest", "228B22");
        NAMED_COLORS.put("sky", "87CEEB");
        NAMED_COLORS.put("royal", "4169E1");
        NAMED_COLORS.put("tan", "D2B48C");
        NAMED_COLORS.put("bronze", "CD7F32");
        NAMED_COLORS.put("silver", "C0C0C0");
        NAMED_COLORS.put("platinum", "E5E4E2");
    }

    /**
     * Map of legacy codes to their color names.
     */
    private static final Map<Character, String> LEGACY_TO_NAME = new HashMap<>();

    static {
        LEGACY_TO_NAME.put('0', "black");
        LEGACY_TO_NAME.put('1', "dark_blue");
        LEGACY_TO_NAME.put('2', "dark_green");
        LEGACY_TO_NAME.put('3', "dark_aqua");
        LEGACY_TO_NAME.put('4', "dark_red");
        LEGACY_TO_NAME.put('5', "dark_purple");
        LEGACY_TO_NAME.put('6', "gold");
        LEGACY_TO_NAME.put('7', "gray");
        LEGACY_TO_NAME.put('8', "dark_gray");
        LEGACY_TO_NAME.put('9', "blue");
        LEGACY_TO_NAME.put('a', "green");
        LEGACY_TO_NAME.put('b', "aqua");
        LEGACY_TO_NAME.put('c', "red");
        LEGACY_TO_NAME.put('d', "light_purple");
        LEGACY_TO_NAME.put('e', "yellow");
        LEGACY_TO_NAME.put('f', "white");
    }

    private ColorUtil() {}

    // ==================== Main Colorization Methods ====================

    /**
     * Translates all color codes in a string to the final format.
     * This is the main method to call for colorizing text.
     * <p>
     * Processes in order:
     * <ol>
     *   <li>Named colors ({red}, {gold})</li>
     *   <li>Gradients and rainbows</li>
     *   <li>Short hex (&#RGB)</li>
     *   <li>Full hex (&#RRGGBB)</li>
     *   <li>Bukkit hex (&x&R&R&G&G&B&B)</li>
     *   <li>Legacy codes (&a, &c)</li>
     * </ol>
     *
     * @param text the input text with color codes
     * @return the colorized text, or empty string if input is null
     */
    @NotNull
    public static String colorize(@Nullable String text) {
        if (text == null || text.isEmpty()) {
            return "";
        }

        String result = text;

        // Process named colors first
        result = processNamedColors(result);

        // Process gradients
        result = processGradients(result);

        // Process rainbow
        result = processRainbow(result);

        // Process short hex (&#RGB -> &#RRGGBB)
        result = expandShortHex(result);

        // Process hex colors (&#RRGGBB -> formatted hex)
        result = processHexColors(result);

        // Process Bukkit-style hex (&x&R&R&G&G&B&B)
        result = processBukkitHex(result);

        // Process legacy color codes (&a -> §a)
        result = translateLegacyCodes(result);

        return result;
    }

    /**
     * Translates only legacy color codes (& -> §).
     * Use this for simple colorization without hex/gradient support.
     *
     * @param text the input text
     * @return the text with translated color codes
     */
    @NotNull
    public static String translateLegacyCodes(@Nullable String text) {
        if (text == null || text.isEmpty()) {
            return "";
        }

        char[] chars = text.toCharArray();
        for (int i = 0; i < chars.length - 1; i++) {
            if (chars[i] == ALT_COLOR_CHAR && isColorCode(chars[i + 1])) {
                chars[i] = COLOR_CHAR;
                chars[i + 1] = Character.toLowerCase(chars[i + 1]);
            }
        }
        return new String(chars);
    }

    /**
     * Strips all color codes from text.
     *
     * @param text the input text
     * @return text without any color codes
     */
    @NotNull
    public static String stripColors(@Nullable String text) {
        if (text == null || text.isEmpty()) {
            return "";
        }

        // First colorize to normalize all formats
        String normalized = colorize(text);
        
        // Then strip
        return STRIP_PATTERN.matcher(normalized).replaceAll("");
    }

    /**
     * Strips only the color character prefix from text, preserving the codes themselves.
     * Useful for storing text without the § character.
     *
     * @param text the input text
     * @return text with & instead of §
     */
    @NotNull
    public static String decolorize(@Nullable String text) {
        if (text == null || text.isEmpty()) {
            return "";
        }
        return text.replace(COLOR_CHAR, ALT_COLOR_CHAR);
    }

    // ==================== Hex Color Methods ====================

    /**
     * Expands short hex codes (&#RGB) to full hex (&#RRGGBB).
     */
    @NotNull
    private static String expandShortHex(@NotNull String text) {
        Matcher matcher = HEX_SHORT_PATTERN.matcher(text);
        StringBuilder result = new StringBuilder();

        while (matcher.find()) {
            String shortHex = matcher.group(1);
            // Expand each character: #RGB -> #RRGGBB
            String fullHex = "" + shortHex.charAt(0) + shortHex.charAt(0)
                               + shortHex.charAt(1) + shortHex.charAt(1)
                               + shortHex.charAt(2) + shortHex.charAt(2);
            matcher.appendReplacement(result, ALT_COLOR_CHAR + "#" + fullHex);
        }
        matcher.appendTail(result);

        return result.toString();
    }

    /**
     * Processes hex colors (&#RRGGBB) to the final format.
     * Converts to Bukkit-style &x&R&R&G&G&B&B format, then to §x§R§R§G§G§B§B.
     */
    @NotNull
    private static String processHexColors(@NotNull String text) {
        Matcher matcher = HEX_PATTERN.matcher(text);
        StringBuilder result = new StringBuilder();

        while (matcher.find()) {
            String hex = matcher.group(1);
            String replacement = hexToMinecraftFormat(hex);
            matcher.appendReplacement(result, Matcher.quoteReplacement(replacement));
        }
        matcher.appendTail(result);

        return result.toString();
    }

    /**
     * Processes Bukkit-style hex codes (&x&R&R&G&G&B&B).
     */
    @NotNull
    private static String processBukkitHex(@NotNull String text) {
        Matcher matcher = BUKKIT_HEX_PATTERN.matcher(text);
        StringBuilder result = new StringBuilder();

        while (matcher.find()) {
            String match = matcher.group();
            // Replace all & with §
            String replacement = match.replace(ALT_COLOR_CHAR, COLOR_CHAR);
            matcher.appendReplacement(result, Matcher.quoteReplacement(replacement));
        }
        matcher.appendTail(result);

        return result.toString();
    }

    /**
     * Converts a 6-digit hex color to Minecraft's format (§x§R§R§G§G§B§B).
     *
     * @param hex the hex color without # prefix (e.g., "FF0000")
     * @return the Minecraft-formatted color code
     */
    @NotNull
    public static String hexToMinecraftFormat(@NotNull String hex) {
        if (hex.startsWith("#")) {
            hex = hex.substring(1);
        }
        if (hex.length() != 6) {
            return "";
        }

        StringBuilder result = new StringBuilder();
        result.append(COLOR_CHAR).append('x');
        for (char c : hex.toLowerCase().toCharArray()) {
            result.append(COLOR_CHAR).append(c);
        }
        return result.toString();
    }

    /**
     * Converts a hex color to the nearest legacy color code.
     * Useful for clients that don't support hex colors.
     *
     * @param hex the hex color (with or without #)
     * @return the nearest legacy color code (e.g., "§c" for red)
     */
    @NotNull
    public static String hexToLegacy(@NotNull String hex) {
        Color color = parseHexColor(hex);
        if (color == null) {
            return "" + COLOR_CHAR + 'f'; // Default to white
        }

        // Find the closest legacy color
        char closestCode = 'f';
        double closestDistance = Double.MAX_VALUE;

        for (Map.Entry<Character, String> entry : LEGACY_TO_NAME.entrySet()) {
            String colorHex = NAMED_COLORS.get(entry.getValue());
            if (colorHex != null) {
                Color legacyColor = parseHexColor(colorHex);
                if (legacyColor != null) {
                    double distance = colorDistance(color, legacyColor);
                    if (distance < closestDistance) {
                        closestDistance = distance;
                        closestCode = entry.getKey();
                    }
                }
            }
        }

        return "" + COLOR_CHAR + closestCode;
    }

    // ==================== Named Color Methods ====================

    /**
     * Processes named colors ({red}, {gold}) to hex codes.
     */
    @NotNull
    private static String processNamedColors(@NotNull String text) {
        Matcher matcher = NAMED_COLOR_PATTERN.matcher(text);
        StringBuilder result = new StringBuilder();

        while (matcher.find()) {
            String colorName = matcher.group(1).toLowerCase();
            String hex = NAMED_COLORS.get(colorName);
            if (hex != null) {
                matcher.appendReplacement(result, ALT_COLOR_CHAR + "#" + hex);
            }
            // If color not found, leave the {name} as-is
        }
        matcher.appendTail(result);

        return result.toString();
    }

    /**
     * Gets the hex code for a named color.
     *
     * @param name the color name (case insensitive)
     * @return the hex code without #, or null if not found
     */
    @Nullable
    public static String getNamedColor(@NotNull String name) {
        return NAMED_COLORS.get(name.toLowerCase());
    }

    /**
     * Registers a custom named color.
     *
     * @param name the color name (will be lowercased)
     * @param hex  the hex code without # (e.g., "FF0000")
     */
    public static void registerNamedColor(@NotNull String name, @NotNull String hex) {
        NAMED_COLORS.put(name.toLowerCase(), hex.toUpperCase());
    }

    // ==================== Gradient Methods ====================

    /**
     * Processes gradient tags in text.
     */
    @NotNull
    private static String processGradients(@NotNull String text) {
        // First try multi-stop gradients
        Matcher multiMatcher = MULTI_GRADIENT_PATTERN.matcher(text);
        StringBuilder multiResult = new StringBuilder();

        while (multiMatcher.find()) {
            String colorsStr = multiMatcher.group(1);
            String content = multiMatcher.group(2);

            String[] colorParts = colorsStr.split(":");
            if (colorParts.length >= 2) {
                String[] hexColors = new String[colorParts.length];
                for (int i = 0; i < colorParts.length; i++) {
                    hexColors[i] = colorParts[i].replace("#", "");
                }
                String gradientText = applyGradient(content, hexColors);
                multiMatcher.appendReplacement(multiResult, Matcher.quoteReplacement(gradientText));
            }
        }
        multiMatcher.appendTail(multiResult);

        return multiResult.toString();
    }

    /**
     * Applies a gradient between multiple colors to text.
     *
     * @param text   the text to apply gradient to
     * @param colors hex colors (without #) in order
     * @return the text with gradient colors applied
     */
    @NotNull
    public static String applyGradient(@NotNull String text, @NotNull String... colors) {
        if (text.isEmpty() || colors.length < 2) {
            return text;
        }

        // Strip existing colors from text for clean gradient
        String stripped = stripColors(text);
        if (stripped.isEmpty()) {
            return text;
        }

        // Parse colors
        Color[] parsedColors = new Color[colors.length];
        for (int i = 0; i < colors.length; i++) {
            parsedColors[i] = parseHexColor(colors[i]);
            if (parsedColors[i] == null) {
                parsedColors[i] = Color.WHITE;
            }
        }

        StringBuilder result = new StringBuilder();
        int totalChars = stripped.length();

        for (int i = 0; i < totalChars; i++) {
            // Calculate position in gradient (0.0 to 1.0)
            float position = totalChars > 1 ? (float) i / (totalChars - 1) : 0;

            // Find which segment we're in
            float segmentLength = 1.0f / (colors.length - 1);
            int segment = Math.min((int) (position / segmentLength), colors.length - 2);
            float segmentPosition = (position - segment * segmentLength) / segmentLength;

            // Interpolate between the two colors of this segment
            Color color = interpolateColor(parsedColors[segment], parsedColors[segment + 1], segmentPosition);

            // Convert to hex and add character
            String hex = String.format("%02x%02x%02x", color.getRed(), color.getGreen(), color.getBlue());
            result.append(hexToMinecraftFormat(hex)).append(stripped.charAt(i));
        }

        return result.toString();
    }

    /**
     * Creates a two-color gradient.
     *
     * @param text      the text
     * @param startHex  start color hex (without #)
     * @param endHex    end color hex (without #)
     * @return gradient text
     */
    @NotNull
    public static String gradient(@NotNull String text, @NotNull String startHex, @NotNull String endHex) {
        return applyGradient(text, startHex, endHex);
    }

    // ==================== Rainbow Methods ====================

    /**
     * Processes rainbow tags in text.
     */
    @NotNull
    private static String processRainbow(@NotNull String text) {
        Matcher matcher = RAINBOW_PATTERN.matcher(text);
        StringBuilder result = new StringBuilder();

        while (matcher.find()) {
            String phaseStr = matcher.group(1);
            String content = matcher.group(2);

            float phase = 0;
            if (phaseStr != null && !phaseStr.isEmpty()) {
                try {
                    phase = Float.parseFloat(phaseStr);
                } catch (NumberFormatException ignored) {}
            }

            String rainbowText = applyRainbow(content, phase);
            matcher.appendReplacement(result, Matcher.quoteReplacement(rainbowText));
        }
        matcher.appendTail(result);

        return result.toString();
    }

    /**
     * Applies rainbow colors to text.
     *
     * @param text  the text
     * @param phase starting phase (0.0 to 1.0)
     * @return rainbow-colored text
     */
    @NotNull
    public static String applyRainbow(@NotNull String text, float phase) {
        String stripped = stripColors(text);
        if (stripped.isEmpty()) {
            return text;
        }

        StringBuilder result = new StringBuilder();
        int length = stripped.length();

        for (int i = 0; i < length; i++) {
            float hue = (phase + (float) i / length) % 1.0f;
            Color color = Color.getHSBColor(hue, 1.0f, 1.0f);

            String hex = String.format("%02x%02x%02x", color.getRed(), color.getGreen(), color.getBlue());
            result.append(hexToMinecraftFormat(hex)).append(stripped.charAt(i));
        }

        return result.toString();
    }

    /**
     * Applies rainbow colors to text with default phase.
     *
     * @param text the text
     * @return rainbow-colored text
     */
    @NotNull
    public static String rainbow(@NotNull String text) {
        return applyRainbow(text, 0);
    }

    // ==================== Utility Methods ====================

    /**
     * Checks if a character is a valid color/format code.
     */
    private static boolean isColorCode(char c) {
        return "0123456789AaBbCcDdEeFfKkLlMmNnOoRr".indexOf(c) >= 0;
    }

    /**
     * Parses a hex color string to a Color object.
     *
     * @param hex hex string (with or without #)
     * @return Color object, or null if invalid
     */
    @Nullable
    public static Color parseHexColor(@Nullable String hex) {
        if (hex == null || hex.isEmpty()) {
            return null;
        }

        if (hex.startsWith("#")) {
            hex = hex.substring(1);
        }

        // Expand short hex
        if (hex.length() == 3) {
            hex = "" + hex.charAt(0) + hex.charAt(0)
                     + hex.charAt(1) + hex.charAt(1)
                     + hex.charAt(2) + hex.charAt(2);
        }

        if (hex.length() != 6) {
            return null;
        }

        try {
            int r = Integer.parseInt(hex.substring(0, 2), 16);
            int g = Integer.parseInt(hex.substring(2, 4), 16);
            int b = Integer.parseInt(hex.substring(4, 6), 16);
            return new Color(r, g, b);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    /**
     * Interpolates between two colors.
     *
     * @param start    start color
     * @param end      end color
     * @param position position between 0.0 and 1.0
     * @return interpolated color
     */
    @NotNull
    private static Color interpolateColor(@NotNull Color start, @NotNull Color end, float position) {
        position = Math.max(0, Math.min(1, position));
        int r = (int) (start.getRed() + (end.getRed() - start.getRed()) * position);
        int g = (int) (start.getGreen() + (end.getGreen() - start.getGreen()) * position);
        int b = (int) (start.getBlue() + (end.getBlue() - start.getBlue()) * position);
        return new Color(r, g, b);
    }

    /**
     * Calculates the Euclidean distance between two colors in RGB space.
     */
    private static double colorDistance(@NotNull Color c1, @NotNull Color c2) {
        int rDiff = c1.getRed() - c2.getRed();
        int gDiff = c1.getGreen() - c2.getGreen();
        int bDiff = c1.getBlue() - c2.getBlue();
        return Math.sqrt(rDiff * rDiff + gDiff * gDiff + bDiff * bDiff);
    }

    /**
     * Validates a hex color string.
     *
     * @param hex the hex string to validate
     * @return true if valid hex color
     */
    public static boolean isValidHex(@Nullable String hex) {
        return parseHexColor(hex) != null;
    }

    /**
     * Converts a Color object to hex string.
     *
     * @param color the color
     * @return hex string with # prefix (e.g., "#FF0000")
     */
    @NotNull
    public static String colorToHex(@NotNull Color color) {
        return String.format("#%02X%02X%02X", color.getRed(), color.getGreen(), color.getBlue());
    }

    /**
     * Gets the legacy color code character for a named color.
     *
     * @param name the color name
     * @return the legacy code (e.g., 'c' for red), or 'f' if not found
     */
    public static char getNamedColorLegacyCode(@NotNull String name) {
        String normalized = name.toLowerCase();
        for (Map.Entry<Character, String> entry : LEGACY_TO_NAME.entrySet()) {
            if (entry.getValue().equals(normalized)) {
                return entry.getKey();
            }
        }
        // Check aliases
        switch (normalized) {
            case "grey": return '7';
            case "dark_grey": return '8';
            case "cyan": return 'b';
            case "pink": case "magenta": return 'd';
            default: return 'f';
        }
    }

    // ==================== Format Code Helpers ====================

    /**
     * Bold format code.
     */
    public static final String BOLD = "" + COLOR_CHAR + 'l';

    /**
     * Italic format code.
     */
    public static final String ITALIC = "" + COLOR_CHAR + 'o';

    /**
     * Underline format code.
     */
    public static final String UNDERLINE = "" + COLOR_CHAR + 'n';

    /**
     * Strikethrough format code.
     */
    public static final String STRIKETHROUGH = "" + COLOR_CHAR + 'm';

    /**
     * Obfuscated/magic format code.
     */
    public static final String OBFUSCATED = "" + COLOR_CHAR + 'k';

    /**
     * Reset format code.
     */
    public static final String RESET = "" + COLOR_CHAR + 'r';

    /**
     * Wraps text in bold formatting.
     */
    @NotNull
    public static String bold(@NotNull String text) {
        return BOLD + text + RESET;
    }

    /**
     * Wraps text in italic formatting.
     */
    @NotNull
    public static String italic(@NotNull String text) {
        return ITALIC + text + RESET;
    }

    /**
     * Wraps text in underline formatting.
     */
    @NotNull
    public static String underline(@NotNull String text) {
        return UNDERLINE + text + RESET;
    }

    /**
     * Wraps text in strikethrough formatting.
     */
    @NotNull
    public static String strikethrough(@NotNull String text) {
        return STRIKETHROUGH + text + RESET;
    }
}
