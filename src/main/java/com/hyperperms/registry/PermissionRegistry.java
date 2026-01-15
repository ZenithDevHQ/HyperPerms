package com.hyperperms.registry;

import com.hyperperms.util.Logger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * Registry for tracking all registered permissions.
 * <p>
 * This registry allows HyperPerms and external plugins to register their permissions
 * with descriptions and categories. This information is used for:
 * <ul>
 *   <li>Permission auto-completion in commands</li>
 *   <li>Permission listing and searching</li>
 *   <li>Documentation generation</li>
 *   <li>Verbose mode output</li>
 * </ul>
 */
public final class PermissionRegistry {

    private static PermissionRegistry instance;

    private final Map<String, PermissionInfo> permissions = new ConcurrentHashMap<>();
    private final Map<String, Set<String>> byCategory = new ConcurrentHashMap<>();

    /**
     * Information about a registered permission.
     */
    public static class PermissionInfo {
        private final String permission;
        private final String description;
        private final String category;
        private final String plugin;
        private final boolean isWildcard;

        /**
         * Creates a new permission info.
         *
         * @param permission  the permission node
         * @param description human-readable description
         * @param category    the permission category
         * @param plugin      the plugin that registered this permission
         */
        public PermissionInfo(@NotNull String permission, @NotNull String description,
                              @NotNull String category, @NotNull String plugin) {
            this.permission = permission;
            this.description = description;
            this.category = category;
            this.plugin = plugin;
            this.isWildcard = permission.contains("*");
        }

        public @NotNull String getPermission() {
            return permission;
        }

        public @NotNull String getDescription() {
            return description;
        }

        public @NotNull String getCategory() {
            return category;
        }

        public @NotNull String getPlugin() {
            return plugin;
        }

        public boolean isWildcard() {
            return isWildcard;
        }

        @Override
        public String toString() {
            return String.format("%s - %s [%s]", permission, description, category);
        }
    }

    /**
     * Gets the singleton instance.
     *
     * @return the permission registry
     */
    @NotNull
    public static PermissionRegistry getInstance() {
        if (instance == null) {
            instance = new PermissionRegistry();
        }
        return instance;
    }

    /**
     * Creates a new permission registry.
     */
    private PermissionRegistry() {
    }

    /**
     * Registers a permission with description and category.
     *
     * @param permission  the permission node (e.g., "hyperperms.command.reload")
     * @param description a human-readable description
     * @param category    the category (e.g., "admin", "user", "group")
     * @return true if registered, false if already exists
     */
    public boolean register(@NotNull String permission, @NotNull String description, @NotNull String category) {
        return register(permission, description, category, "HyperPerms");
    }

    /**
     * Registers a permission with description, category, and plugin name.
     *
     * @param permission  the permission node
     * @param description a human-readable description
     * @param category    the category
     * @param plugin      the plugin registering this permission
     * @return true if registered, false if already exists
     */
    public boolean register(@NotNull String permission, @NotNull String description,
                            @NotNull String category, @NotNull String plugin) {
        Objects.requireNonNull(permission, "permission");
        Objects.requireNonNull(description, "description");
        Objects.requireNonNull(category, "category");
        Objects.requireNonNull(plugin, "plugin");

        String normalizedPerm = permission.toLowerCase();
        String normalizedCategory = category.toLowerCase();

        PermissionInfo info = new PermissionInfo(normalizedPerm, description, normalizedCategory, plugin);

        if (permissions.putIfAbsent(normalizedPerm, info) == null) {
            // Add to category index
            byCategory.computeIfAbsent(normalizedCategory, k -> ConcurrentHashMap.newKeySet())
                    .add(normalizedPerm);

            Logger.debug("Registered permission: %s [%s] from %s", normalizedPerm, normalizedCategory, plugin);
            return true;
        }

        return false;
    }

    /**
     * Registers multiple permissions at once.
     *
     * @param permissions map of permission -> description
     * @param category    the category for all permissions
     */
    public void registerAll(@NotNull Map<String, String> permissions, @NotNull String category) {
        permissions.forEach((perm, desc) -> register(perm, desc, category));
    }

    /**
     * Unregisters a permission.
     *
     * @param permission the permission to unregister
     * @return true if removed, false if not found
     */
    public boolean unregister(@NotNull String permission) {
        String normalizedPerm = permission.toLowerCase();
        PermissionInfo removed = permissions.remove(normalizedPerm);

        if (removed != null) {
            Set<String> categorySet = byCategory.get(removed.category);
            if (categorySet != null) {
                categorySet.remove(normalizedPerm);
            }
            return true;
        }

        return false;
    }

    /**
     * Gets information about a registered permission.
     *
     * @param permission the permission node
     * @return the permission info, or null if not registered
     */
    @Nullable
    public PermissionInfo get(@NotNull String permission) {
        return permissions.get(permission.toLowerCase());
    }

    /**
     * Checks if a permission is registered.
     *
     * @param permission the permission node
     * @return true if registered
     */
    public boolean isRegistered(@NotNull String permission) {
        return permissions.containsKey(permission.toLowerCase());
    }

    /**
     * Gets all registered permissions.
     *
     * @return unmodifiable collection of all permission infos
     */
    @NotNull
    public Collection<PermissionInfo> getAll() {
        return Collections.unmodifiableCollection(permissions.values());
    }

    /**
     * Gets all permissions in a specific category.
     *
     * @param category the category to filter by
     * @return list of permission infos in the category
     */
    @NotNull
    public List<PermissionInfo> getByCategory(@NotNull String category) {
        String normalizedCategory = category.toLowerCase();
        Set<String> perms = byCategory.get(normalizedCategory);

        if (perms == null || perms.isEmpty()) {
            return Collections.emptyList();
        }

        return perms.stream()
                .map(permissions::get)
                .filter(Objects::nonNull)
                .sorted(Comparator.comparing(PermissionInfo::getPermission))
                .collect(Collectors.toList());
    }

    /**
     * Gets all available categories.
     *
     * @return unmodifiable set of category names
     */
    @NotNull
    public Set<String> getCategories() {
        return Collections.unmodifiableSet(byCategory.keySet());
    }

    /**
     * Searches for permissions matching a query.
     * <p>
     * The search matches against both permission nodes and descriptions.
     *
     * @param query the search query (case-insensitive)
     * @return list of matching permission infos
     */
    @NotNull
    public List<PermissionInfo> search(@NotNull String query) {
        String lowerQuery = query.toLowerCase();

        return permissions.values().stream()
                .filter(info -> info.permission.contains(lowerQuery) ||
                        info.description.toLowerCase().contains(lowerQuery))
                .sorted(Comparator.comparing(PermissionInfo::getPermission))
                .collect(Collectors.toList());
    }

    /**
     * Gets permissions by the plugin that registered them.
     *
     * @param plugin the plugin name
     * @return list of permission infos from that plugin
     */
    @NotNull
    public List<PermissionInfo> getByPlugin(@NotNull String plugin) {
        return permissions.values().stream()
                .filter(info -> info.plugin.equalsIgnoreCase(plugin))
                .sorted(Comparator.comparing(PermissionInfo::getPermission))
                .collect(Collectors.toList());
    }

    /**
     * Gets the total count of registered permissions.
     *
     * @return the count
     */
    public int size() {
        return permissions.size();
    }

    /**
     * Clears all registered permissions.
     */
    public void clear() {
        permissions.clear();
        byCategory.clear();
    }

    /**
     * Registers HyperPerms' built-in permissions.
     */
    public void registerBuiltInPermissions() {
        // Admin wildcards
        register("hyperperms.*", "Full administrative access to HyperPerms", "admin");
        register("hyperperms.command.*", "Access to all HyperPerms commands", "admin");

        // User commands
        register("hyperperms.command.user.info", "View user permission information", "user");
        register("hyperperms.command.user.info.others", "View other users' permission information", "user");
        register("hyperperms.command.user.permission", "Modify user permissions", "user");
        register("hyperperms.command.user.group", "Modify user group memberships", "user");
        register("hyperperms.command.user.promote", "Promote users on tracks", "user");
        register("hyperperms.command.user.demote", "Demote users on tracks", "user");
        register("hyperperms.command.user.clear", "Clear user permission data", "user");
        register("hyperperms.command.user.clone", "Clone permissions from one user to another", "user");

        // Group commands
        register("hyperperms.command.group.info", "View group information", "group");
        register("hyperperms.command.group.list", "List all groups", "group");
        register("hyperperms.command.group.create", "Create new groups", "group");
        register("hyperperms.command.group.delete", "Delete groups", "group");
        register("hyperperms.command.group.permission", "Modify group permissions", "group");
        register("hyperperms.command.group.parent", "Modify group inheritance", "group");
        register("hyperperms.command.group.modify", "Modify group metadata", "group");

        // Track commands
        register("hyperperms.command.track.info", "View track information", "track");
        register("hyperperms.command.track.list", "List all tracks", "track");
        register("hyperperms.command.track.create", "Create new tracks", "track");
        register("hyperperms.command.track.delete", "Delete tracks", "track");
        register("hyperperms.command.track.modify", "Modify track configuration", "track");

        // Administrative commands
        register("hyperperms.command.reload", "Reload plugin configuration", "admin");
        register("hyperperms.command.verbose", "Toggle verbose permission checking", "admin");
        register("hyperperms.command.cache", "Manage permission cache", "admin");
        register("hyperperms.command.export", "Export permission data", "admin");
        register("hyperperms.command.import", "Import permission data", "admin");
        register("hyperperms.command.backup", "Manage backups", "admin");
        register("hyperperms.command.check", "Check permissions for users", "admin");
        register("hyperperms.command.check.self", "Check your own permissions", "utility");
        register("hyperperms.command.check.others", "Check other users' permissions", "admin");

        // Debug commands
        register("hyperperms.command.debug", "Access debug commands", "debug");
        register("hyperperms.command.debug.tree", "View user inheritance tree", "debug");
        register("hyperperms.command.debug.resolve", "Debug permission resolution", "debug");
        register("hyperperms.command.debug.contexts", "View user contexts", "debug");

        // Web editor
        register("hyperperms.command.editor", "Open the web editor", "editor");
        register("hyperperms.command.apply", "Apply web editor changes", "editor");

        // Chat permissions
        register("hyperperms.chat.color", "Use color codes in chat", "chat");
        register("hyperperms.chat.format", "Use formatting codes in chat", "chat");

        Logger.info("Registered %d built-in permissions", size());
    }
}
