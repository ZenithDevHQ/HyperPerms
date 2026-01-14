package com.hyperperms.platform;

import com.hyperperms.HyperPerms;
import com.hyperperms.api.context.ContextSet;
import com.hyperperms.model.Group;
import com.hyperperms.model.Node;
import com.hyperperms.model.User;
import com.hyperperms.util.Logger;
import com.hypixel.hytale.server.core.permissions.provider.PermissionProvider;
import org.jetbrains.annotations.NotNull;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * HyperPerms implementation of Hytale's PermissionProvider interface.
 * <p>
 * This bridges Hytale's permission system with HyperPerms, allowing
 * HyperPerms to handle all permission checks and data management.
 * <p>
 * Note: Hytale's PermissionProvider interface uses a simple flat permission
 * model. HyperPerms supports much more (contexts, inheritance, wildcards),
 * but this adapter provides compatibility with Hytale's built-in systems.
 */
public class HyperPermsPermissionProvider implements PermissionProvider {

    private static final String PROVIDER_NAME = "HyperPerms";

    private final HyperPerms hyperPerms;

    /**
     * Creates a new HyperPermsPermissionProvider.
     *
     * @param hyperPerms the HyperPerms instance
     */
    public HyperPermsPermissionProvider(@NotNull HyperPerms hyperPerms) {
        this.hyperPerms = hyperPerms;
    }

    @Override
    public String getName() {
        return PROVIDER_NAME;
    }

    // ==================== User Permissions ====================

    @Override
    public void addUserPermissions(UUID uuid, Set<String> permissions) {
        User user = hyperPerms.getUserManager().getOrCreateUser(uuid);
        for (String permission : permissions) {
            user.setNode(Node.of(permission));
        }
        hyperPerms.getUserManager().saveUser(user);
        hyperPerms.getCacheInvalidator().invalidate(uuid);
        Logger.debug("Added %d permissions to user %s", permissions.size(), uuid);
    }

    @Override
    public void removeUserPermissions(UUID uuid, Set<String> permissions) {
        User user = hyperPerms.getUserManager().getUser(uuid);
        if (user == null) {
            return;
        }
        for (String permission : permissions) {
            user.removeNode(permission);
        }
        hyperPerms.getUserManager().saveUser(user);
        hyperPerms.getCacheInvalidator().invalidate(uuid);
        Logger.debug("Removed %d permissions from user %s", permissions.size(), uuid);
    }

    @Override
    public Set<String> getUserPermissions(UUID uuid) {
        User user = hyperPerms.getUserManager().getUser(uuid);
        if (user == null) {
            return Collections.emptySet();
        }

        // Return all directly assigned permissions (without context filtering)
        return user.getNodes().stream()
                .filter(node -> !node.isGroupNode())
                .filter(node -> !node.isNegated())
                .filter(node -> node.getContexts().isEmpty())
                .map(Node::getPermission)
                .collect(Collectors.toSet());
    }

    // ==================== Group Permissions ====================

    @Override
    public void addGroupPermissions(String groupName, Set<String> permissions) {
        Group group = hyperPerms.getGroupManager().getGroup(groupName);
        if (group == null) {
            // Create the group if it doesn't exist
            group = hyperPerms.getGroupManager().createGroup(groupName);
        }
        for (String permission : permissions) {
            group.setNode(Node.of(permission));
        }
        hyperPerms.getGroupManager().saveGroup(group);
        hyperPerms.getCacheInvalidator().invalidateAll();
        Logger.debug("Added %d permissions to group %s", permissions.size(), groupName);
    }

    @Override
    public void removeGroupPermissions(String groupName, Set<String> permissions) {
        Group group = hyperPerms.getGroupManager().getGroup(groupName);
        if (group == null) {
            return;
        }
        for (String permission : permissions) {
            group.removeNode(permission);
        }
        hyperPerms.getGroupManager().saveGroup(group);
        hyperPerms.getCacheInvalidator().invalidateAll();
        Logger.debug("Removed %d permissions from group %s", permissions.size(), groupName);
    }

    @Override
    public Set<String> getGroupPermissions(String groupName) {
        Group group = hyperPerms.getGroupManager().getGroup(groupName);
        if (group == null) {
            return Collections.emptySet();
        }

        // Return all directly assigned permissions (without context filtering)
        return group.getNodes().stream()
                .filter(node -> !node.isGroupNode())
                .filter(node -> !node.isNegated())
                .filter(node -> node.getContexts().isEmpty())
                .map(Node::getPermission)
                .collect(Collectors.toSet());
    }

    // ==================== User-Group Membership ====================

    @Override
    public void addUserToGroup(UUID uuid, String groupName) {
        // Ensure the group exists
        Group group = hyperPerms.getGroupManager().getGroup(groupName);
        if (group == null) {
            Logger.warn("Cannot add user %s to non-existent group %s", uuid, groupName);
            return;
        }

        User user = hyperPerms.getUserManager().getOrCreateUser(uuid);
        user.addGroup(groupName);
        hyperPerms.getUserManager().saveUser(user);
        hyperPerms.getCacheInvalidator().invalidate(uuid);
        Logger.debug("Added user %s to group %s", uuid, groupName);
    }

    @Override
    public void removeUserFromGroup(UUID uuid, String groupName) {
        User user = hyperPerms.getUserManager().getUser(uuid);
        if (user == null) {
            return;
        }
        user.removeGroup(groupName);
        hyperPerms.getUserManager().saveUser(user);
        hyperPerms.getCacheInvalidator().invalidate(uuid);
        Logger.debug("Removed user %s from group %s", uuid, groupName);
    }

    @Override
    public Set<String> getGroupsForUser(UUID uuid) {
        User user = hyperPerms.getUserManager().getUser(uuid);
        if (user == null) {
            return Collections.emptySet();
        }
        return new HashSet<>(user.getInheritedGroups());
    }

    // ==================== Extended HyperPerms Functionality ====================

    /**
     * Checks if a user has a permission with context awareness.
     * <p>
     * This method provides the full HyperPerms permission check, including:
     * <ul>
     *   <li>Wildcard matching</li>
     *   <li>Group inheritance</li>
     *   <li>Context-sensitive permissions</li>
     *   <li>Negation handling</li>
     * </ul>
     *
     * @param uuid       the user's UUID
     * @param permission the permission to check
     * @return true if the user has the permission
     */
    public boolean hasPermission(UUID uuid, String permission) {
        // Get current contexts for the player
        ContextSet contexts = hyperPerms.getContexts(uuid);
        return hyperPerms.hasPermission(uuid, permission, contexts);
    }

    /**
     * Checks if a user has a permission with specific contexts.
     *
     * @param uuid       the user's UUID
     * @param permission the permission to check
     * @param contexts   the contexts to check against
     * @return true if the user has the permission in the given contexts
     */
    public boolean hasPermission(UUID uuid, String permission, ContextSet contexts) {
        return hyperPerms.hasPermission(uuid, permission, contexts);
    }
}
