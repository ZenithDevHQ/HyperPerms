package com.hyperperms.api;

import com.hyperperms.HyperPerms;
import com.hyperperms.api.context.ContextSet;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Utility class for common permission checking patterns.
 * <p>
 * This class provides convenience methods for checking permissions in various ways:
 * <ul>
 *   <li>Check if user has ANY of multiple permissions</li>
 *   <li>Check if user has ALL of multiple permissions</li>
 *   <li>Extract numeric values from permission hierarchies (e.g., home.limit.5)</li>
 *   <li>Get list of permitted targets (e.g., build.place.stone returns "stone")</li>
 * </ul>
 * <p>
 * Example usage:
 * <pre>
 * PermissionChecker checker = PermissionChecker.of(playerUUID);
 * 
 * // Check if user has any of these permissions
 * if (checker.hasAny("admin.bypass", "mod.bypass", "vip.bypass")) {
 *     // User can bypass
 * }
 * 
 * // Get home limit from home.limit.X permissions
 * int homeLimit = checker.getHighestNumericPermission("home.limit.", 1);
 * 
 * // Get list of blocks user can place
 * List&lt;String&gt; allowedBlocks = checker.getPermissionTargets("build.place.");
 * </pre>
 */
public final class PermissionChecker {

    private static final Pattern NUMERIC_SUFFIX = Pattern.compile("\\.(\\d+)$");

    private final UUID uuid;
    private final HyperPermsAPI api;
    private final ContextSet contexts;

    /**
     * Creates a permission checker for the given user.
     *
     * @param uuid the user's UUID
     * @return the permission checker
     */
    @NotNull
    public static PermissionChecker of(@NotNull UUID uuid) {
        HyperPermsAPI api = HyperPerms.getApi();
        if (api == null) {
            throw new IllegalStateException("HyperPerms is not enabled");
        }
        return new PermissionChecker(uuid, api, api.getContexts(uuid));
    }

    /**
     * Creates a permission checker for the given user with specific contexts.
     *
     * @param uuid     the user's UUID
     * @param contexts the contexts to check permissions in
     * @return the permission checker
     */
    @NotNull
    public static PermissionChecker of(@NotNull UUID uuid, @NotNull ContextSet contexts) {
        HyperPermsAPI api = HyperPerms.getApi();
        if (api == null) {
            throw new IllegalStateException("HyperPerms is not enabled");
        }
        return new PermissionChecker(uuid, api, contexts);
    }

    /**
     * Checks if a user has any of the given permissions.
     *
     * @param uuid        the user's UUID
     * @param permissions the permissions to check
     * @return true if the user has at least one of the permissions
     */
    public static boolean hasAny(@NotNull UUID uuid, @NotNull String... permissions) {
        return of(uuid).hasAny(permissions);
    }

    /**
     * Checks if a user has all of the given permissions.
     *
     * @param uuid        the user's UUID
     * @param permissions the permissions to check
     * @return true if the user has all of the permissions
     */
    public static boolean hasAll(@NotNull UUID uuid, @NotNull String... permissions) {
        return of(uuid).hasAll(permissions);
    }

    /**
     * Gets the highest numeric permission value for a prefix.
     * <p>
     * For permissions like "home.limit.1", "home.limit.5", "home.limit.10",
     * calling this with prefix "home.limit." returns 10 (the highest).
     *
     * @param uuid         the user's UUID
     * @param prefix       the permission prefix (e.g., "home.limit.")
     * @param defaultValue the default value if no numeric permission is found
     * @return the highest numeric value, or defaultValue if none found
     */
    public static int getHighestNumericPermission(@NotNull UUID uuid, @NotNull String prefix, int defaultValue) {
        return of(uuid).getHighestNumericPermission(prefix, defaultValue);
    }

    /**
     * Gets the list of permitted targets for a permission prefix.
     * <p>
     * For permissions like "build.place.stone", "build.place.dirt",
     * calling this with prefix "build.place." returns ["stone", "dirt"].
     *
     * @param uuid   the user's UUID
     * @param prefix the permission prefix (e.g., "build.place.")
     * @return list of targets the user has permission for
     */
    @NotNull
    public static List<String> getPermissionTargets(@NotNull UUID uuid, @NotNull String prefix) {
        return of(uuid).getPermissionTargets(prefix);
    }

    private PermissionChecker(@NotNull UUID uuid, @NotNull HyperPermsAPI api, @NotNull ContextSet contexts) {
        this.uuid = Objects.requireNonNull(uuid, "uuid");
        this.api = Objects.requireNonNull(api, "api");
        this.contexts = Objects.requireNonNull(contexts, "contexts");
    }

    /**
     * Gets the UUID of the user being checked.
     *
     * @return the user UUID
     */
    @NotNull
    public UUID getUuid() {
        return uuid;
    }

    /**
     * Gets the contexts being used for permission checks.
     *
     * @return the contexts
     */
    @NotNull
    public ContextSet getContexts() {
        return contexts;
    }

    /**
     * Checks if the user has a single permission.
     *
     * @param permission the permission to check
     * @return true if the user has the permission
     */
    public boolean has(@NotNull String permission) {
        return api.hasPermission(uuid, permission, contexts);
    }

    /**
     * Checks if the user has any of the given permissions.
     *
     * @param permissions the permissions to check
     * @return true if the user has at least one of the permissions
     */
    public boolean hasAny(@NotNull String... permissions) {
        if (permissions.length == 0) {
            return false;
        }

        for (String permission : permissions) {
            if (api.hasPermission(uuid, permission, contexts)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Checks if the user has any of the given permissions.
     *
     * @param permissions the permissions to check
     * @return true if the user has at least one of the permissions
     */
    public boolean hasAny(@NotNull Collection<String> permissions) {
        for (String permission : permissions) {
            if (api.hasPermission(uuid, permission, contexts)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Checks if the user has all of the given permissions.
     *
     * @param permissions the permissions to check
     * @return true if the user has all of the permissions
     */
    public boolean hasAll(@NotNull String... permissions) {
        if (permissions.length == 0) {
            return true;
        }

        for (String permission : permissions) {
            if (!api.hasPermission(uuid, permission, contexts)) {
                return false;
            }
        }
        return true;
    }

    /**
     * Checks if the user has all of the given permissions.
     *
     * @param permissions the permissions to check
     * @return true if the user has all of the permissions
     */
    public boolean hasAll(@NotNull Collection<String> permissions) {
        for (String permission : permissions) {
            if (!api.hasPermission(uuid, permission, contexts)) {
                return false;
            }
        }
        return true;
    }

    /**
     * Gets the highest numeric permission value for a prefix.
     * <p>
     * For permissions like "home.limit.1", "home.limit.5", "home.limit.10",
     * calling this with prefix "home.limit." returns 10 (the highest).
     * <p>
     * This method checks permissions in the range 1-1000 by default.
     * If you need higher limits, use {@link #getHighestNumericPermission(String, int, int, int)}.
     *
     * @param prefix       the permission prefix (e.g., "home.limit.")
     * @param defaultValue the default value if no numeric permission is found
     * @return the highest numeric value, or defaultValue if none found
     */
    public int getHighestNumericPermission(@NotNull String prefix, int defaultValue) {
        return getHighestNumericPermission(prefix, defaultValue, 1, 1000);
    }

    /**
     * Gets the highest numeric permission value for a prefix within a range.
     *
     * @param prefix       the permission prefix (e.g., "home.limit.")
     * @param defaultValue the default value if no numeric permission is found
     * @param min          the minimum value to check
     * @param max          the maximum value to check
     * @return the highest numeric value the user has, or defaultValue if none found
     */
    public int getHighestNumericPermission(@NotNull String prefix, int defaultValue, int min, int max) {
        // Check for unlimited permission (prefix + "*" or just "*")
        if (api.hasPermission(uuid, prefix + "*", contexts) || api.hasPermission(uuid, "*", contexts)) {
            return max;
        }

        // Binary search for the highest value
        int highest = -1;
        
        // Start with common values first for optimization
        int[] commonValues = {1, 3, 5, 10, 15, 20, 25, 50, 100, 500, 1000};
        for (int value : commonValues) {
            if (value >= min && value <= max) {
                if (api.hasPermission(uuid, prefix + value, contexts)) {
                    highest = Math.max(highest, value);
                }
            }
        }

        // If we found a high value, search above it
        if (highest > 0) {
            for (int i = highest + 1; i <= max; i++) {
                if (api.hasPermission(uuid, prefix + i, contexts)) {
                    highest = i;
                } else {
                    // Stop searching once we hit a value they don't have
                    // This assumes permissions are granted sequentially
                    break;
                }
            }
        } else {
            // Didn't find any common values, do a full scan from min
            for (int i = min; i <= max; i++) {
                if (api.hasPermission(uuid, prefix + i, contexts)) {
                    highest = i;
                }
            }
        }

        return highest >= min ? highest : defaultValue;
    }

    /**
     * Gets the list of permitted targets for a permission prefix.
     * <p>
     * For permissions like "build.place.stone", "build.place.dirt",
     * calling this with prefix "build.place." returns ["stone", "dirt"].
     * <p>
     * This method works by checking the user's permission data directly
     * rather than testing each possible target.
     *
     * @param prefix the permission prefix (e.g., "build.place.")
     * @return list of targets the user has permission for, empty if none
     */
    @NotNull
    public List<String> getPermissionTargets(@NotNull String prefix) {
        List<String> targets = new ArrayList<>();
        
        // Get user from manager
        var userManager = api.getUserManager();
        var user = userManager.getUser(uuid);
        
        if (user == null) {
            return targets;
        }

        // Check user's direct permissions
        for (var node : user.getNodes()) {
            String perm = node.getPermission();
            if (perm.startsWith(prefix) && node.getValue()) {
                String target = perm.substring(prefix.length());
                // Exclude wildcards and numeric values
                if (!target.isEmpty() && !target.equals("*") && !target.matches("\\d+")) {
                    targets.add(target);
                }
            }
        }

        // Also check inherited permissions from groups
        for (String groupName : user.getInheritedGroups()) {
            var group = api.getGroupManager().getGroup(groupName);
            if (group != null) {
                for (var node : group.getNodes()) {
                    String perm = node.getPermission();
                    if (perm.startsWith(prefix) && node.getValue()) {
                        String target = perm.substring(prefix.length());
                        if (!target.isEmpty() && !target.equals("*") && !target.matches("\\d+") && !targets.contains(target)) {
                            targets.add(target);
                        }
                    }
                }
            }
        }

        return targets;
    }

    /**
     * Checks if the user has a specific target permission.
     * <p>
     * Checks both the specific permission and the wildcard version.
     *
     * @param prefix the permission prefix (e.g., "build.place.")
     * @param target the target to check (e.g., "stone")
     * @return true if the user can use this target
     */
    public boolean hasTarget(@NotNull String prefix, @NotNull String target) {
        // Check wildcard first
        if (api.hasPermission(uuid, prefix + "*", contexts)) {
            return true;
        }
        // Check specific target
        return api.hasPermission(uuid, prefix + target, contexts);
    }

    /**
     * Creates a new checker with different contexts.
     *
     * @param contexts the new contexts to use
     * @return a new permission checker with the specified contexts
     */
    @NotNull
    public PermissionChecker withContexts(@NotNull ContextSet contexts) {
        return new PermissionChecker(uuid, api, contexts);
    }
}
