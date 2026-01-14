package com.hyperperms.resolver;

import com.hyperperms.api.context.ContextSet;
import com.hyperperms.resolver.WildcardMatcher.MatchType;
import com.hyperperms.resolver.WildcardMatcher.TriState;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;

/**
 * Captures detailed information about how a permission was resolved.
 * <p>
 * This is used for verbose/debug mode to understand exactly why a permission
 * was granted or denied.
 */
public record PermissionTrace(
        @NotNull String permission,
        @NotNull TriState result,
        @Nullable String matchedNode,
        @NotNull MatchType matchType,
        @Nullable String sourceGroup,
        boolean fromUser,
        @NotNull ContextSet activeContexts
) {
    /**
     * Creates a permission trace.
     *
     * @param permission     the permission that was checked
     * @param result         the result of the check
     * @param matchedNode    the node that matched (may be wildcard)
     * @param matchType      how the match occurred
     * @param sourceGroup    the group that provided the permission, or null if from user
     * @param fromUser       whether the permission came directly from user permissions
     * @param activeContexts the contexts that were active during the check
     */
    public PermissionTrace {
        Objects.requireNonNull(permission, "permission cannot be null");
        Objects.requireNonNull(result, "result cannot be null");
        Objects.requireNonNull(matchType, "matchType cannot be null");
        Objects.requireNonNull(activeContexts, "activeContexts cannot be null");
    }

    /**
     * Creates a trace for a permission that was not found.
     *
     * @param permission the permission
     * @param contexts   the active contexts
     * @return a trace indicating no match
     */
    public static PermissionTrace notFound(@NotNull String permission, @NotNull ContextSet contexts) {
        return new PermissionTrace(
                permission,
                TriState.UNDEFINED,
                null,
                MatchType.NONE,
                null,
                false,
                contexts
        );
    }

    /**
     * Creates a trace for a direct user permission.
     *
     * @param permission  the permission
     * @param result      the result
     * @param matchedNode the matched node
     * @param matchType   the match type
     * @param contexts    the active contexts
     * @return a trace for a user permission
     */
    public static PermissionTrace fromUser(
            @NotNull String permission,
            @NotNull TriState result,
            @Nullable String matchedNode,
            @NotNull MatchType matchType,
            @NotNull ContextSet contexts
    ) {
        return new PermissionTrace(permission, result, matchedNode, matchType, null, true, contexts);
    }

    /**
     * Creates a trace for a group permission.
     *
     * @param permission  the permission
     * @param result      the result
     * @param matchedNode the matched node
     * @param matchType   the match type
     * @param groupName   the source group name
     * @param contexts    the active contexts
     * @return a trace for a group permission
     */
    public static PermissionTrace fromGroup(
            @NotNull String permission,
            @NotNull TriState result,
            @Nullable String matchedNode,
            @NotNull MatchType matchType,
            @NotNull String groupName,
            @NotNull ContextSet contexts
    ) {
        return new PermissionTrace(permission, result, matchedNode, matchType, groupName, false, contexts);
    }

    /**
     * Checks if this trace represents a found match.
     *
     * @return true if a match was found
     */
    public boolean isMatched() {
        return matchType != MatchType.NONE;
    }

    /**
     * Checks if the result came from a wildcard match.
     *
     * @return true if from wildcard
     */
    public boolean isFromWildcard() {
        return matchType == MatchType.WILDCARD ||
               matchType == MatchType.WILDCARD_NEGATION ||
               matchType == MatchType.UNIVERSAL ||
               matchType == MatchType.UNIVERSAL_NEGATION;
    }

    /**
     * Checks if the result came from a negation.
     *
     * @return true if from negation
     */
    public boolean isFromNegation() {
        return matchType == MatchType.EXACT_NEGATION ||
               matchType == MatchType.WILDCARD_NEGATION ||
               matchType == MatchType.UNIVERSAL_NEGATION;
    }

    /**
     * Gets a human-readable description of the source.
     *
     * @return the source description
     */
    @NotNull
    public String getSourceDescription() {
        if (fromUser) {
            return "user";
        }
        if (sourceGroup != null) {
            return "group:" + sourceGroup;
        }
        return "unknown";
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder("PermissionTrace{");
        sb.append("permission='").append(permission).append('\'');
        sb.append(", result=").append(result);
        if (matchedNode != null) {
            sb.append(", matchedNode='").append(matchedNode).append('\'');
        }
        sb.append(", matchType=").append(matchType);
        sb.append(", source=").append(getSourceDescription());
        if (!activeContexts.isEmpty()) {
            sb.append(", contexts=").append(activeContexts);
        }
        sb.append('}');
        return sb.toString();
    }

    /**
     * Returns a verbose multi-line description for debugging.
     *
     * @return verbose description
     */
    @NotNull
    public String toVerboseString() {
        StringBuilder sb = new StringBuilder();
        sb.append("Permission Check Trace\n");
        sb.append("  Permission: ").append(permission).append("\n");
        sb.append("  Result: ").append(result).append("\n");
        sb.append("  Match Type: ").append(matchType).append("\n");
        if (matchedNode != null) {
            sb.append("  Matched Node: ").append(matchedNode).append("\n");
        }
        sb.append("  Source: ").append(getSourceDescription()).append("\n");
        if (!activeContexts.isEmpty()) {
            sb.append("  Active Contexts: ").append(activeContexts).append("\n");
        }
        return sb.toString();
    }
}
