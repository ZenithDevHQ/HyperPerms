package com.hyperperms.model;

import com.hyperperms.api.PermissionHolder;
import com.hyperperms.api.context.ContextSet;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * Represents a player's permission data.
 * <p>
 * Users hold direct permission nodes and group memberships. Effective permissions
 * are calculated by the permission resolver considering inheritance.
 */
public final class User implements PermissionHolder {

    private final UUID uuid;
    private volatile String username;
    private volatile String primaryGroup;
    private volatile String customPrefix;
    private volatile String customSuffix;
    private final Set<Node> nodes = ConcurrentHashMap.newKeySet();

    /**
     * Creates a new user.
     *
     * @param uuid     the player's UUID
     * @param username the player's username (can be updated)
     */
    public User(@NotNull UUID uuid, @Nullable String username) {
        this.uuid = Objects.requireNonNull(uuid, "uuid cannot be null");
        this.username = username;
        this.primaryGroup = "default";
        this.customPrefix = null;
        this.customSuffix = null;
    }

    /**
     * Gets the user's UUID.
     *
     * @return the UUID
     */
    @NotNull
    public UUID getUuid() {
        return uuid;
    }

    /**
     * Gets the user's username.
     *
     * @return the username, or null if unknown
     */
    @Nullable
    public String getUsername() {
        return username;
    }

    /**
     * Sets the user's username.
     *
     * @param username the new username
     */
    public void setUsername(@Nullable String username) {
        this.username = username;
    }

    /**
     * Gets the user's primary group.
     *
     * @return the primary group name
     */
    @NotNull
    public String getPrimaryGroup() {
        return primaryGroup;
    }

    /**
     * Sets the user's primary group.
     *
     * @param primaryGroup the primary group name
     */
    public void setPrimaryGroup(@NotNull String primaryGroup) {
        this.primaryGroup = Objects.requireNonNull(primaryGroup, "primaryGroup cannot be null");
    }

    /**
     * Gets the user's custom prefix.
     * If set, this overrides any group prefix.
     *
     * @return the custom prefix, or null if not set
     */
    @Nullable
    public String getCustomPrefix() {
        return customPrefix;
    }

    /**
     * Sets the user's custom prefix.
     * Supports color codes like &a, &c, etc.
     *
     * @param customPrefix the prefix, or null to clear
     */
    public void setCustomPrefix(@Nullable String customPrefix) {
        this.customPrefix = customPrefix;
    }

    /**
     * Gets the user's custom suffix.
     * If set, this overrides any group suffix.
     *
     * @return the custom suffix, or null if not set
     */
    @Nullable
    public String getCustomSuffix() {
        return customSuffix;
    }

    /**
     * Sets the user's custom suffix.
     * Supports color codes like &a, &c, etc.
     *
     * @param customSuffix the suffix, or null to clear
     */
    public void setCustomSuffix(@Nullable String customSuffix) {
        this.customSuffix = customSuffix;
    }

    @Override
    @NotNull
    public String getIdentifier() {
        return uuid.toString();
    }

    @Override
    @NotNull
    public String getFriendlyName() {
        return username != null ? username : uuid.toString();
    }

    @Override
    @NotNull
    public Set<Node> getNodes() {
        return Collections.unmodifiableSet(new HashSet<>(nodes));
    }

    @Override
    @NotNull
    public Set<Node> getNodes(@NotNull ContextSet contexts) {
        return nodes.stream()
                .filter(node -> node.appliesIn(contexts))
                .collect(Collectors.toUnmodifiableSet());
    }

    @Override
    @NotNull
    public Set<Node> getNodes(boolean includeExpired) {
        if (includeExpired) {
            return getNodes();
        }
        return nodes.stream()
                .filter(node -> !node.isExpired())
                .collect(Collectors.toUnmodifiableSet());
    }

    @Override
    public boolean hasNode(@NotNull Node node) {
        return nodes.contains(node);
    }

    @Override
    @NotNull
    public DataMutateResult addNode(@NotNull Node node) {
        Objects.requireNonNull(node, "node cannot be null");
        if (nodes.add(node)) {
            return DataMutateResult.SUCCESS;
        }
        return DataMutateResult.ALREADY_EXISTS;
    }

    @Override
    @NotNull
    public DataMutateResult removeNode(@NotNull Node node) {
        Objects.requireNonNull(node, "node cannot be null");
        if (nodes.remove(node)) {
            return DataMutateResult.SUCCESS;
        }
        return DataMutateResult.DOES_NOT_EXIST;
    }

    @Override
    @NotNull
    public DataMutateResult removeNode(@NotNull String permission) {
        Objects.requireNonNull(permission, "permission cannot be null");
        String lowerPerm = permission.toLowerCase();
        boolean removed = nodes.removeIf(node -> node.getPermission().equals(lowerPerm));
        return removed ? DataMutateResult.SUCCESS : DataMutateResult.DOES_NOT_EXIST;
    }

    @Override
    @NotNull
    public DataMutateResult setNode(@NotNull Node node) {
        Objects.requireNonNull(node, "node cannot be null");
        // Remove any existing nodes with the same permission and contexts
        nodes.removeIf(existing -> existing.equalsIgnoringExpiry(node));
        nodes.add(node);
        return DataMutateResult.SUCCESS;
    }

    @Override
    public void clearNodes() {
        nodes.clear();
    }

    @Override
    public void clearNodes(@NotNull ContextSet contexts) {
        nodes.removeIf(node -> node.getContexts().equals(contexts));
    }

    @Override
    @NotNull
    public Set<String> getInheritedGroups() {
        Set<String> groups = nodes.stream()
                .filter(Node::isGroupNode)
                .filter(node -> !node.isExpired())
                .map(Node::getGroupName)
                .filter(Objects::nonNull)
                .collect(Collectors.toCollection(HashSet::new));
        
        // Always include the primary group
        if (primaryGroup != null && !primaryGroup.isEmpty()) {
            groups.add(primaryGroup);
        }
        
        return Collections.unmodifiableSet(groups);
    }

    @Override
    @NotNull
    public Set<String> getInheritedGroups(@NotNull ContextSet contexts) {
        Set<String> groups = nodes.stream()
                .filter(Node::isGroupNode)
                .filter(node -> !node.isExpired())
                .filter(node -> node.appliesIn(contexts))
                .map(Node::getGroupName)
                .filter(Objects::nonNull)
                .collect(Collectors.toCollection(HashSet::new));
        
        // Always include the primary group (applies in all contexts)
        if (primaryGroup != null && !primaryGroup.isEmpty()) {
            groups.add(primaryGroup);
        }
        
        return Collections.unmodifiableSet(groups);
    }

    @Override
    @NotNull
    public DataMutateResult addGroup(@NotNull String groupName) {
        return addNode(Node.group(groupName));
    }

    @Override
    @NotNull
    public DataMutateResult removeGroup(@NotNull String groupName) {
        String groupPerm = Node.GROUP_PREFIX + groupName.toLowerCase();
        return removeNode(groupPerm);
    }

    @Override
    public int cleanupExpired() {
        int before = nodes.size();
        nodes.removeIf(Node::isExpired);
        return before - nodes.size();
    }

    /**
     * Checks if this user has any meaningful data (non-default state).
     *
     * @return true if the user has permissions or non-default settings
     */
    public boolean hasData() {
        return !nodes.isEmpty() 
            || !primaryGroup.equals("default")
            || customPrefix != null
            || customSuffix != null;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof User user)) return false;
        return uuid.equals(user.uuid);
    }

    @Override
    public int hashCode() {
        return uuid.hashCode();
    }

    @Override
    public String toString() {
        return "User{uuid=" + uuid + ", username='" + username + "', primaryGroup='" + primaryGroup + "'}";
    }
}
