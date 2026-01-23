package com.hyperperms.resolver;

import com.hyperperms.api.context.ContextSet;
import com.hyperperms.registry.PermissionRegistry;
import com.hyperperms.model.Group;
import com.hyperperms.model.Node;
import com.hyperperms.model.User;
import com.hyperperms.resolver.WildcardMatcher.MatchResult;
import com.hyperperms.resolver.WildcardMatcher.MatchType;
import com.hyperperms.resolver.WildcardMatcher.TriState;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.function.Function;

/**
 * Resolves effective permissions for users by processing inheritance and wildcards.
 */
public final class PermissionResolver {

    private final Function<String, Group> groupLoader;
    private final InheritanceGraph inheritanceGraph;

    /**
     * Creates a new permission resolver.
     *
     * @param groupLoader function to load groups by name
     */
    public PermissionResolver(@NotNull Function<String, Group> groupLoader) {
        this.groupLoader = Objects.requireNonNull(groupLoader, "groupLoader cannot be null");
        this.inheritanceGraph = new InheritanceGraph(groupLoader);
    }

    /**
     * Resolves all effective permissions for a user in the given context.
     *
     * @param user     the user
     * @param contexts the current context
     * @return resolved permission map
     */
    @NotNull
    public ResolvedPermissions resolve(@NotNull User user, @NotNull ContextSet contexts) {
        Map<String, Boolean> permissions = new LinkedHashMap<>();
        Map<String, String> permissionSources = new LinkedHashMap<>(); // tracks which group each perm came from

        // 1. Collect groups the user belongs to
        Set<String> userGroups = new HashSet<>(user.getInheritedGroups(contexts));
        userGroups.add(user.getPrimaryGroup());

        // 2. Resolve group inheritance (returns groups sorted by weight, lowest first)
        List<Group> inheritedGroups = inheritanceGraph.resolveInheritance(userGroups, contexts);

        // 3. Apply group permissions in order (lowest weight first = lowest priority)
        for (Group group : inheritedGroups) {
            for (Node node : group.getNodes()) {
                if (!node.isExpired() && !node.isGroupNode() && node.appliesIn(contexts)) {
                    applyNode(permissions, permissionSources, node, group.getName());
                }
            }
        }

        // 4. Apply user's direct permissions (highest priority)
        for (Node node : user.getNodes()) {
            if (!node.isExpired() && !node.isGroupNode() && node.appliesIn(contexts)) {
                applyNode(permissions, permissionSources, node, null); // null = from user
            }
        }

        return new ResolvedPermissions(permissions, permissionSources, contexts);
    }

    /**
     * Resolves permissions for a group (for debugging/display).
     *
     * @param group    the group
     * @param contexts the current context
     * @return resolved permission map
     */
    @NotNull
    public ResolvedPermissions resolveGroup(@NotNull Group group, @NotNull ContextSet contexts) {
        Map<String, Boolean> permissions = new LinkedHashMap<>();
        Map<String, String> permissionSources = new LinkedHashMap<>();

        // Resolve inheritance
        List<Group> inheritedGroups = inheritanceGraph.resolveInheritance(Set.of(group.getName()), contexts);

        // Apply in order
        for (Group g : inheritedGroups) {
            for (Node node : g.getNodes()) {
                if (!node.isExpired() && !node.isGroupNode() && node.appliesIn(contexts)) {
                    applyNode(permissions, permissionSources, node, g.getName());
                }
            }
        }

        return new ResolvedPermissions(permissions, permissionSources, contexts);
    }

    /**
     * Checks a single permission for a user.
     *
     * @param user       the user
     * @param permission the permission to check
     * @param contexts   the current context
     * @return the result
     */
    @NotNull
    public TriState check(@NotNull User user, @NotNull String permission, @NotNull ContextSet contexts) {
        ResolvedPermissions resolved = resolve(user, contexts);
        return resolved.check(permission);
    }

    /**
     * Checks a single permission with full trace information.
     *
     * @param user       the user
     * @param permission the permission to check
     * @param contexts   the current context
     * @return detailed trace of the permission check
     */
    @NotNull
    public PermissionTrace checkWithTrace(@NotNull User user, @NotNull String permission, @NotNull ContextSet contexts) {
        ResolvedPermissions resolved = resolve(user, contexts);
        return resolved.checkWithTrace(permission);
    }

    /**
     * Checks if a user has a permission.
     *
     * @param user       the user
     * @param permission the permission to check
     * @param contexts   the current context
     * @return true if the user has the permission
     */
    public boolean hasPermission(@NotNull User user, @NotNull String permission, @NotNull ContextSet contexts) {
        return check(user, permission, contexts).asBoolean();
    }

    private void applyNode(Map<String, Boolean> permissions, Map<String, String> sources,
                           Node node, @Nullable String groupName) {
        String perm = node.getPermission();
        boolean value = node.getValue();

        // Handle negation prefix
        if (perm.startsWith("-")) {
            perm = perm.substring(1);
            value = !value;
        }

        permissions.put(perm, value);
        sources.put(perm, groupName); // null means from user
    }

    /**
     * Represents resolved permissions with wildcard checking and tracing support.
     */
    public static final class ResolvedPermissions {

        private final Map<String, Boolean> permissions;
        private final Map<String, String> sources; // permission -> group name (null = user)
        private final ContextSet resolvedContexts;

        ResolvedPermissions(@NotNull Map<String, Boolean> permissions,
                           @NotNull Map<String, String> sources,
                           @NotNull ContextSet contexts) {
            this.permissions = Collections.unmodifiableMap(new LinkedHashMap<>(permissions));
            this.sources = Collections.unmodifiableMap(new LinkedHashMap<>(sources));
            this.resolvedContexts = contexts;
        }

        /**
         * Checks a permission against the resolved permissions.
         *
         * @param permission the permission to check
         * @return the result
         */
        @NotNull
        public TriState check(@NotNull String permission) {
            return WildcardMatcher.check(permission.toLowerCase(), permissions);
        }

        /**
         * Checks a permission with detailed trace information.
         *
         * @param permission the permission to check
         * @return the trace with full details
         */
        @NotNull
        public PermissionTrace checkWithTrace(@NotNull String permission) {
            String lowerPerm = permission.toLowerCase();
            MatchResult matchResult = WildcardMatcher.checkWithTrace(lowerPerm, permissions);

            if (!matchResult.isMatched()) {
                return PermissionTrace.notFound(permission, resolvedContexts);
            }

            // Determine the source
            String matchedNode = matchResult.matchedNode();
            String sourceGroup = matchedNode != null ? sources.get(matchedNode) : null;
            boolean fromUser = sourceGroup == null && matchedNode != null;

            if (fromUser) {
                return PermissionTrace.fromUser(
                        permission,
                        matchResult.result(),
                        matchedNode,
                        matchResult.matchType(),
                        resolvedContexts
                );
            } else if (sourceGroup != null) {
                return PermissionTrace.fromGroup(
                        permission,
                        matchResult.result(),
                        matchedNode,
                        matchResult.matchType(),
                        sourceGroup,
                        resolvedContexts
                );
            } else {
                return new PermissionTrace(
                        permission,
                        matchResult.result(),
                        matchedNode,
                        matchResult.matchType(),
                        null,
                        false,
                        resolvedContexts
                );
            }
        }

        /**
         * Checks if a permission is granted.
         *
         * @param permission the permission
         * @return true if granted
         */
        public boolean hasPermission(@NotNull String permission) {
            return check(permission).asBoolean();
        }

        /**
         * Gets all resolved permissions.
         *
         * @return unmodifiable map of permissions
         */
        @NotNull
        public Map<String, Boolean> getPermissions() {
            return permissions;
        }

        /**
         * Gets the source group for a permission.
         *
         * @param permission the permission
         * @return the group name, or null if from user or not found
         */
        @Nullable
        public String getSource(@NotNull String permission) {
            return sources.get(permission.toLowerCase());
        }

        /**
         * Gets all granted permissions.
         *
         * @return set of granted permissions
         */
        @NotNull
        public Set<String> getGrantedPermissions() {
            Set<String> granted = new HashSet<>();
            for (Map.Entry<String, Boolean> entry : permissions.entrySet()) {
                if (entry.getValue()) {
                    granted.add(entry.getKey());
                }
            }
            return Collections.unmodifiableSet(granted);
        }

        /**
         * Gets all granted permissions with wildcards expanded.
         * <p>
         * This method returns a set that includes both the raw permission nodes
         * and all concrete permissions that match any wildcard nodes. This is
         * necessary for systems like Hytale's PermissionProvider that use
         * {@code Set.contains()} for permission checks instead of supporting
         * wildcard matching.
         * <p>
         * Also handles aliasing for plugin permissions where Hytale uses
         * full package paths (e.g., hyperhomes.* also grants com.hyperhomes.*)
         *
         * @param registry the permission registry to use for wildcard expansion
         * @return set of all granted permissions including wildcard expansions
         */
        @NotNull
        public Set<String> getExpandedPermissions(@NotNull PermissionRegistry registry) {
            Set<String> granted = getGrantedPermissions();
            Set<String> expanded = new HashSet<>(granted);

            for (String perm : granted) {
                // Check if this is a wildcard permission
                if (perm.endsWith(".*") || perm.equals("*")) {
                    // Expand the wildcard using the registry
                    Set<String> matching = registry.getMatchingPermissions(perm);
                    expanded.addAll(matching);

                    // Handle aliasing for plugin permissions
                    // Hytale uses full package paths like "com.hyperhomes.hyperhomes.command.homes"
                    // So "hyperhomes.*" should also grant "com.hyperhomes.hyperhomes.*" permissions
                    Set<String> aliasedWildcards = getAliasedWildcards(perm);
                    for (String aliasedWildcard : aliasedWildcards) {
                        Set<String> aliasedMatching = registry.getMatchingPermissions(aliasedWildcard);
                        expanded.addAll(aliasedMatching);
                        // Add the wildcard itself so Hytale's wildcard check finds it
                        expanded.add(aliasedWildcard);
                    }
                } else {
                    // Non-wildcard permission - also add aliased versions if applicable
                    Set<String> aliased = getAliasedPermissions(perm);
                    expanded.addAll(aliased);
                }
            }

            return Collections.unmodifiableSet(expanded);
        }

        /**
         * Gets all aliased wildcards for a plugin permission.
         * Returns multiple wildcards at different levels for Hytale's hierarchical checking.
         * <p>
         * Hytale checks wildcards at EVERY level:
         * - com.*
         * - com.hyperhomes.*
         * - com.hyperhomes.hyperhomes.*
         * - com.hyperhomes.hyperhomes.command.*
         */
        @NotNull
        private Set<String> getAliasedWildcards(String wildcard) {
            Set<String> aliases = new HashSet<>();

            // hyperhomes.* -> all levels of com.hyperhomes.hyperhomes.command.*
            if (wildcard.equals("hyperhomes.*") || wildcard.equals("hyperhomes.command.*")) {
                aliases.add("com.*");
                aliases.add("com.hyperhomes.*");
                aliases.add("com.hyperhomes.hyperhomes.*");
                aliases.add("com.hyperhomes.hyperhomes.command.*");
            }

            // Add more plugin aliases as needed
            return aliases;
        }

        /**
         * Gets aliased permissions for non-wildcard plugin permissions.
         * Maps simplified permissions to Hytale's full package path format.
         * <p>
         * HyperHomes command structure:
         * - /homes -> com.hyperhomes.hyperhomes.command.homes
         * - /homes gui -> com.hyperhomes.hyperhomes.command.homes.gui
         * - /home -> com.hyperhomes.hyperhomes.command.home
         * - /sethome -> com.hyperhomes.hyperhomes.command.sethome
         * - /delhome -> com.hyperhomes.hyperhomes.command.delhome
         */
        @NotNull
        private Set<String> getAliasedPermissions(String permission) {
            Set<String> aliases = new HashSet<>();

            if (!permission.startsWith("hyperhomes.") || permission.contains("*")) {
                return aliases;
            }

            String suffix = permission.substring("hyperhomes.".length());

            // Special shorthand mappings for user-friendly permissions
            switch (suffix) {
                case "gui":
                    // hyperhomes.gui -> /homes gui subcommand
                    aliases.add("com.hyperhomes.hyperhomes.command.homes.gui");
                    break;
                case "use":
                    // hyperhomes.use -> grant access to main commands
                    aliases.add("com.hyperhomes.hyperhomes.command.homes");
                    aliases.add("com.hyperhomes.hyperhomes.command.home");
                    aliases.add("com.hyperhomes.hyperhomes.command.homes.gui");
                    break;
                case "list":
                    // hyperhomes.list -> /homes list or just /homes
                    aliases.add("com.hyperhomes.hyperhomes.command.homes");
                    aliases.add("com.hyperhomes.hyperhomes.command.homes.list");
                    break;
                case "set":
                    // hyperhomes.set -> /sethome
                    aliases.add("com.hyperhomes.hyperhomes.command.sethome");
                    aliases.add("com.hyperhomes.hyperhomes.command.homes.set");
                    break;
                case "delete":
                    // hyperhomes.delete -> /delhome
                    aliases.add("com.hyperhomes.hyperhomes.command.delhome");
                    aliases.add("com.hyperhomes.hyperhomes.command.homes.delete");
                    break;
                case "teleport":
                    // hyperhomes.teleport -> /home (teleport to home)
                    aliases.add("com.hyperhomes.hyperhomes.command.home");
                    break;
                default:
                    // Default: hyperhomes.X -> com.hyperhomes.hyperhomes.command.X
                    aliases.add("com.hyperhomes.hyperhomes.command." + suffix);
                    break;
            }

            return aliases;
        }

        /**
         * Gets all denied permissions.
         *
         * @return set of denied permissions
         */
        @NotNull
        public Set<String> getDeniedPermissions() {
            Set<String> denied = new HashSet<>();
            for (Map.Entry<String, Boolean> entry : permissions.entrySet()) {
                if (!entry.getValue()) {
                    denied.add(entry.getKey());
                }
            }
            return Collections.unmodifiableSet(denied);
        }

        /**
         * Gets the contexts these permissions were resolved in.
         *
         * @return the contexts
         */
        @NotNull
        public ContextSet getResolvedContexts() {
            return resolvedContexts;
        }

        /**
         * Gets the number of permissions.
         *
         * @return the count
         */
        public int size() {
            return permissions.size();
        }

        /**
         * Checks if empty.
         *
         * @return true if no permissions
         */
        public boolean isEmpty() {
            return permissions.isEmpty();
        }
    }
}
