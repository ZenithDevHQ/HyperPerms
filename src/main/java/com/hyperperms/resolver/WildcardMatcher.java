package com.hyperperms.resolver;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Map;
import java.util.Objects;

/**
 * Matches permission nodes against wildcard patterns with negation support.
 * <p>
 * Wildcard patterns:
 * <ul>
 *   <li>{@code *} - matches any permission</li>
 *   <li>{@code plugin.*} - matches any permission starting with "plugin."</li>
 *   <li>{@code plugin.command.*} - matches "plugin.command.home", etc.</li>
 * </ul>
 * <p>
 * Negation patterns (prefix with '-'):
 * <ul>
 *   <li>{@code -plugin.admin} - explicitly denies "plugin.admin"</li>
 *   <li>{@code -plugin.admin.*} - denies all permissions under "plugin.admin."</li>
 * </ul>
 * <p>
 * Priority order (highest to lowest):
 * <ol>
 *   <li>Exact negation (-permission)</li>
 *   <li>Exact match (permission)</li>
 *   <li>Most specific negated wildcard (-prefix.*)</li>
 *   <li>Most specific wildcard (prefix.*)</li>
 *   <li>Less specific wildcards...</li>
 *   <li>Universal negation (-*)</li>
 *   <li>Universal wildcard (*)</li>
 * </ol>
 */
public final class WildcardMatcher {

    private WildcardMatcher() {}

    /**
     * Checks if a permission matches a wildcard pattern.
     *
     * @param permission the permission to check
     * @param pattern    the wildcard pattern
     * @return true if matches
     */
    public static boolean matches(@NotNull String permission, @NotNull String pattern) {
        Objects.requireNonNull(permission, "permission cannot be null");
        Objects.requireNonNull(pattern, "pattern cannot be null");

        // Exact match
        if (permission.equals(pattern)) {
            return true;
        }

        // Universal wildcard
        if (pattern.equals("*")) {
            return true;
        }

        // Prefix wildcard (plugin.* matches plugin.command.home)
        if (pattern.endsWith(".*")) {
            String prefix = pattern.substring(0, pattern.length() - 1); // Remove the *
            return permission.startsWith(prefix);
        }

        return false;
    }

    /**
     * Checks if a permission is granted by any of the given permission values.
     * <p>
     * This method properly handles negation priority: a negation always overrides
     * a wildcard at the same or less specific level. More specific permissions
     * always win over less specific ones.
     * <p>
     * Example: {@code -plugin.admin} will override {@code plugin.*}
     *
     * @param permission the permission to check
     * @param values     map of permission patterns to their values (true/false)
     * @return the result of the check
     */
    @NotNull
    public static TriState check(@NotNull String permission, @NotNull Map<String, Boolean> values) {
        Objects.requireNonNull(permission, "permission cannot be null");
        Objects.requireNonNull(values, "values cannot be null");

        String lowerPerm = permission.toLowerCase();

        // 1. Check exact negation first (highest priority)
        String negated = "-" + lowerPerm;
        if (values.containsKey(negated)) {
            return values.get(negated) ? TriState.FALSE : TriState.TRUE;
        }

        // 2. Check exact match
        if (values.containsKey(lowerPerm)) {
            return values.get(lowerPerm) ? TriState.TRUE : TriState.FALSE;
        }

        // 3. Check wildcards from most specific to least specific
        //    At each level, check negation BEFORE the positive wildcard
        String[] parts = lowerPerm.split("\\.");

        for (int i = parts.length - 1; i >= 0; i--) {
            String wildcard = buildWildcard(parts, i);

            // Check negated wildcard first (negation takes priority)
            String negatedWildcard = "-" + wildcard;
            if (values.containsKey(negatedWildcard)) {
                return values.get(negatedWildcard) ? TriState.FALSE : TriState.TRUE;
            }

            // Then check positive wildcard
            if (values.containsKey(wildcard)) {
                return values.get(wildcard) ? TriState.TRUE : TriState.FALSE;
            }
        }

        // 4. Check universal negation
        if (values.containsKey("-*")) {
            return values.get("-*") ? TriState.FALSE : TriState.TRUE;
        }

        // 5. Check universal wildcard
        if (values.containsKey("*")) {
            return values.get("*") ? TriState.TRUE : TriState.FALSE;
        }

        return TriState.UNDEFINED;
    }

    /**
     * Checks a permission with detailed match information for tracing.
     *
     * @param permission the permission to check
     * @param values     map of permission patterns to their values
     * @return the match result with trace information
     */
    @NotNull
    public static MatchResult checkWithTrace(@NotNull String permission, @NotNull Map<String, Boolean> values) {
        Objects.requireNonNull(permission, "permission cannot be null");
        Objects.requireNonNull(values, "values cannot be null");

        String lowerPerm = permission.toLowerCase();

        // 1. Check exact negation first
        String negated = "-" + lowerPerm;
        if (values.containsKey(negated)) {
            boolean val = values.get(negated);
            return new MatchResult(
                    val ? TriState.FALSE : TriState.TRUE,
                    negated,
                    MatchType.EXACT_NEGATION
            );
        }

        // 2. Check exact match
        if (values.containsKey(lowerPerm)) {
            boolean val = values.get(lowerPerm);
            return new MatchResult(
                    val ? TriState.TRUE : TriState.FALSE,
                    lowerPerm,
                    MatchType.EXACT
            );
        }

        // 3. Check wildcards
        String[] parts = lowerPerm.split("\\.");

        for (int i = parts.length - 1; i >= 0; i--) {
            String wildcard = buildWildcard(parts, i);
            boolean isUniversal = wildcard.equals("*");

            // Check negated wildcard
            String negatedWildcard = "-" + wildcard;
            if (values.containsKey(negatedWildcard)) {
                boolean val = values.get(negatedWildcard);
                return new MatchResult(
                        val ? TriState.FALSE : TriState.TRUE,
                        negatedWildcard,
                        isUniversal ? MatchType.UNIVERSAL_NEGATION : MatchType.WILDCARD_NEGATION
                );
            }

            // Check positive wildcard
            if (values.containsKey(wildcard)) {
                boolean val = values.get(wildcard);
                return new MatchResult(
                        val ? TriState.TRUE : TriState.FALSE,
                        wildcard,
                        isUniversal ? MatchType.UNIVERSAL : MatchType.WILDCARD
                );
            }
        }

        return new MatchResult(TriState.UNDEFINED, null, MatchType.NONE);
    }

    /**
     * Builds a wildcard pattern from permission parts.
     *
     * @param parts the permission parts
     * @param count the number of parts to include before the wildcard
     * @return the wildcard pattern
     */
    private static String buildWildcard(String[] parts, int count) {
        if (count == 0) {
            return "*";
        }
        StringBuilder sb = new StringBuilder();
        for (int j = 0; j < count; j++) {
            sb.append(parts[j]).append(".");
        }
        sb.append("*");
        return sb.toString();
    }

    /**
     * Generates all possible wildcard patterns for a permission.
     * <p>
     * For "plugin.command.home", returns:
     * ["plugin.command.home", "plugin.command.*", "plugin.*", "*"]
     *
     * @param permission the permission
     * @return array of patterns from most to least specific
     */
    @NotNull
    public static String[] generatePatterns(@NotNull String permission) {
        String[] parts = permission.split("\\.");
        String[] patterns = new String[parts.length + 1];

        patterns[0] = permission; // Exact match

        for (int i = 1; i <= parts.length; i++) {
            patterns[i] = buildWildcard(parts, parts.length - i);
        }

        return patterns;
    }

    /**
     * Tri-state result for permission checks.
     */
    public enum TriState {
        /**
         * Permission is explicitly granted.
         */
        TRUE,

        /**
         * Permission is explicitly denied.
         */
        FALSE,

        /**
         * Permission is not set (undefined).
         */
        UNDEFINED;

        /**
         * Converts to boolean, treating undefined as false.
         *
         * @return true if TRUE, false otherwise
         */
        public boolean asBoolean() {
            return this == TRUE;
        }

        /**
         * Converts to boolean with a default value for undefined.
         *
         * @param defaultValue the default value for undefined
         * @return the boolean value
         */
        public boolean asBoolean(boolean defaultValue) {
            return switch (this) {
                case TRUE -> true;
                case FALSE -> false;
                case UNDEFINED -> defaultValue;
            };
        }
    }

    /**
     * Types of permission matches.
     */
    public enum MatchType {
        /**
         * No match found.
         */
        NONE,

        /**
         * Exact permission match.
         */
        EXACT,

        /**
         * Exact negation match (-permission).
         */
        EXACT_NEGATION,

        /**
         * Wildcard match (prefix.*).
         */
        WILDCARD,

        /**
         * Negated wildcard match (-prefix.*).
         */
        WILDCARD_NEGATION,

        /**
         * Universal wildcard (*).
         */
        UNIVERSAL,

        /**
         * Universal negation (-*).
         */
        UNIVERSAL_NEGATION
    }

    /**
     * Result of a permission check with match information.
     *
     * @param result      the tri-state result
     * @param matchedNode the permission node that caused the match, or null
     * @param matchType   the type of match
     */
    public record MatchResult(
            @NotNull TriState result,
            @Nullable String matchedNode,
            @NotNull MatchType matchType
    ) {
        /**
         * Checks if this result represents a match (not NONE).
         *
         * @return true if matched
         */
        public boolean isMatched() {
            return matchType != MatchType.NONE;
        }

        /**
         * Checks if this result is from a negation.
         *
         * @return true if negation match
         */
        public boolean isNegation() {
            return matchType == MatchType.EXACT_NEGATION ||
                   matchType == MatchType.WILDCARD_NEGATION ||
                   matchType == MatchType.UNIVERSAL_NEGATION;
        }

        /**
         * Checks if this result is from a wildcard.
         *
         * @return true if wildcard match
         */
        public boolean isWildcard() {
            return matchType == MatchType.WILDCARD ||
                   matchType == MatchType.WILDCARD_NEGATION ||
                   matchType == MatchType.UNIVERSAL ||
                   matchType == MatchType.UNIVERSAL_NEGATION;
        }
    }
}
