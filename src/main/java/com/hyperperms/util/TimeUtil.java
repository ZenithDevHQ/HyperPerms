package com.hyperperms.util;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Utility class for parsing and formatting durations and times.
 */
public final class TimeUtil {

    // Pattern for duration strings like "1d", "2h", "30m", "1d2h30m"
    private static final Pattern DURATION_PATTERN = Pattern.compile(
            "(?:(\\d+)w)?(?:(\\d+)d)?(?:(\\d+)h)?(?:(\\d+)m)?(?:(\\d+)s)?",
            Pattern.CASE_INSENSITIVE
    );

    private TimeUtil() {}

    /**
     * Parses a duration string into a Duration.
     * <p>
     * Supported formats:
     * <ul>
     *   <li>{@code 30s} - 30 seconds</li>
     *   <li>{@code 5m} - 5 minutes</li>
     *   <li>{@code 2h} - 2 hours</li>
     *   <li>{@code 1d} - 1 day</li>
     *   <li>{@code 1w} - 1 week</li>
     *   <li>{@code 1d2h30m} - 1 day, 2 hours, 30 minutes</li>
     * </ul>
     *
     * @param input the duration string
     * @return the parsed duration, or empty if invalid
     */
    public static Optional<Duration> parseDuration(@Nullable String input) {
        if (input == null || input.isBlank()) {
            return Optional.empty();
        }

        // Handle special cases
        String trimmed = input.trim().toLowerCase();
        if (trimmed.equals("permanent") || trimmed.equals("perm") || trimmed.equals("forever")) {
            return Optional.empty(); // Indicates permanent
        }

        Matcher matcher = DURATION_PATTERN.matcher(trimmed);
        if (!matcher.matches()) {
            return Optional.empty();
        }

        long totalSeconds = 0;

        // Weeks
        if (matcher.group(1) != null) {
            totalSeconds += Long.parseLong(matcher.group(1)) * 7 * 24 * 60 * 60;
        }
        // Days
        if (matcher.group(2) != null) {
            totalSeconds += Long.parseLong(matcher.group(2)) * 24 * 60 * 60;
        }
        // Hours
        if (matcher.group(3) != null) {
            totalSeconds += Long.parseLong(matcher.group(3)) * 60 * 60;
        }
        // Minutes
        if (matcher.group(4) != null) {
            totalSeconds += Long.parseLong(matcher.group(4)) * 60;
        }
        // Seconds
        if (matcher.group(5) != null) {
            totalSeconds += Long.parseLong(matcher.group(5));
        }

        if (totalSeconds == 0) {
            return Optional.empty();
        }

        return Optional.of(Duration.ofSeconds(totalSeconds));
    }

    /**
     * Formats a duration into a human-readable string.
     *
     * @param duration the duration
     * @return the formatted string
     */
    @NotNull
    public static String formatDuration(@NotNull Duration duration) {
        if (duration.isNegative() || duration.isZero()) {
            return "now";
        }

        long days = duration.toDays();
        long hours = duration.toHoursPart();
        long minutes = duration.toMinutesPart();
        long seconds = duration.toSecondsPart();

        StringBuilder sb = new StringBuilder();
        if (days > 0) {
            sb.append(days).append("d ");
        }
        if (hours > 0) {
            sb.append(hours).append("h ");
        }
        if (minutes > 0) {
            sb.append(minutes).append("m ");
        }
        if (seconds > 0 || sb.isEmpty()) {
            sb.append(seconds).append("s");
        }

        return sb.toString().trim();
    }

    /**
     * Formats the time remaining until an instant.
     *
     * @param expiry the expiry instant
     * @return the formatted string, or "expired" if in the past
     */
    @NotNull
    public static String formatTimeUntil(@NotNull Instant expiry) {
        Instant now = Instant.now();
        if (now.isAfter(expiry)) {
            return "expired";
        }
        return formatDuration(Duration.between(now, expiry));
    }

    /**
     * Formats a duration into a short compact form.
     *
     * @param duration the duration
     * @return the compact string (e.g., "1d2h")
     */
    @NotNull
    public static String formatDurationCompact(@NotNull Duration duration) {
        if (duration.isNegative() || duration.isZero()) {
            return "0s";
        }

        long days = duration.toDays();
        long hours = duration.toHoursPart();
        long minutes = duration.toMinutesPart();
        long seconds = duration.toSecondsPart();

        StringBuilder sb = new StringBuilder();
        if (days > 0) sb.append(days).append("d");
        if (hours > 0) sb.append(hours).append("h");
        if (minutes > 0) sb.append(minutes).append("m");
        if (seconds > 0 && days == 0) sb.append(seconds).append("s");

        return sb.isEmpty() ? "0s" : sb.toString();
    }

    /**
     * Creates an expiry instant from a duration starting from now.
     *
     * @param duration the duration
     * @return the expiry instant
     */
    @NotNull
    public static Instant expiryFromDuration(@NotNull Duration duration) {
        return Instant.now().plus(duration);
    }

    /**
     * Calculates the duration between now and an instant.
     *
     * @param instant the target instant
     * @return the duration (may be negative if in the past)
     */
    @NotNull
    public static Duration durationUntil(@NotNull Instant instant) {
        return Duration.between(Instant.now(), instant);
    }

    /**
     * Formats an expiry time as "expires in X" or "expired X ago".
     *
     * @param expiry the expiry instant, or null for permanent
     * @return the human-readable string
     */
    @NotNull
    public static String formatExpiry(@Nullable Instant expiry) {
        if (expiry == null) {
            return "permanent";
        }

        Instant now = Instant.now();
        Duration duration = Duration.between(now, expiry);

        if (duration.isNegative()) {
            Duration ago = duration.negated();
            return "expired " + formatDurationHumanReadable(ago) + " ago";
        } else if (duration.isZero()) {
            return "expires now";
        } else {
            return "expires in " + formatDurationHumanReadable(duration);
        }
    }

    /**
     * Formats a duration in a more human-readable way with full words.
     *
     * @param duration the duration
     * @return the human-readable string
     */
    @NotNull
    public static String formatDurationHumanReadable(@NotNull Duration duration) {
        if (duration.isNegative()) {
            duration = duration.negated();
        }

        if (duration.isZero()) {
            return "0 seconds";
        }

        long days = duration.toDays();
        long hours = duration.toHoursPart();
        long minutes = duration.toMinutesPart();
        long seconds = duration.toSecondsPart();

        StringBuilder sb = new StringBuilder();

        if (days > 0) {
            sb.append(days).append(days == 1 ? " day" : " days");
        }
        if (hours > 0) {
            if (!sb.isEmpty()) sb.append(", ");
            sb.append(hours).append(hours == 1 ? " hour" : " hours");
        }
        if (minutes > 0 && days == 0) {
            if (!sb.isEmpty()) sb.append(", ");
            sb.append(minutes).append(minutes == 1 ? " minute" : " minutes");
        }
        if (seconds > 0 && days == 0 && hours == 0) {
            if (!sb.isEmpty()) sb.append(", ");
            sb.append(seconds).append(seconds == 1 ? " second" : " seconds");
        }

        return sb.isEmpty() ? "0 seconds" : sb.toString();
    }

    /**
     * Checks if an expiry time is in the past.
     *
     * @param expiry the expiry instant
     * @return true if expired
     */
    public static boolean isExpired(@Nullable Instant expiry) {
        return expiry != null && Instant.now().isAfter(expiry);
    }

    /**
     * Checks if an expiry time is permanent (null).
     *
     * @param expiry the expiry instant
     * @return true if permanent
     */
    public static boolean isPermanent(@Nullable Instant expiry) {
        return expiry == null;
    }
}
