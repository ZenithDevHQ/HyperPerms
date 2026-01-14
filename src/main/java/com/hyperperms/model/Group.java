package com.hyperperms.model;

import com.hyperperms.api.PermissionHolder;
import com.hyperperms.api.context.ContextSet;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * Represents a permission group.
 * <p>
 * Groups can contain permission nodes and inherit from other groups.
 * The weight determines priority when resolving conflicting permissions.
 */
public final class Group implements PermissionHolder {

    private final String name;
    private volatile String displayName;
    private volatile int weight;
    private final Set<Node> nodes = ConcurrentHashMap.newKeySet();

    /**
     * Creates a new group.
     *
     * @param name the group name (lowercase identifier)
     */
    public Group(@NotNull String name) {
        this.name = Objects.requireNonNull(name, "name cannot be null").toLowerCase();
        this.displayName = this.name;
        this.weight = 0;
    }

    /**
     * Creates a new group with specified weight.
     *
     * @param name   the group name
     * @param weight the weight
     */
    public Group(@NotNull String name, int weight) {
        this(name);
        this.weight = weight;
    }

    /**
     * Gets the group's lowercase identifier name.
     *
     * @return the name
     */
    @NotNull
    public String getName() {
        return name;
    }

    /**
     * Gets the group's display name.
     *
     * @return the display name
     */
    @NotNull
    public String getDisplayName() {
        return displayName;
    }

    /**
     * Sets the group's display name.
     *
     * @param displayName the display name
     */
    public void setDisplayName(@Nullable String displayName) {
        this.displayName = displayName != null ? displayName : name;
    }

    /**
     * Gets the group's weight.
     * Higher weight = higher priority when resolving conflicts.
     *
     * @return the weight
     */
    public int getWeight() {
        return weight;
    }

    /**
     * Sets the group's weight.
     *
     * @param weight the weight
     */
    public void setWeight(int weight) {
        this.weight = weight;
    }

    @Override
    @NotNull
    public String getIdentifier() {
        return name;
    }

    @Override
    @NotNull
    public String getFriendlyName() {
        return displayName;
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
        return nodes.stream()
                .filter(Node::isGroupNode)
                .filter(node -> !node.isExpired())
                .map(Node::getGroupName)
                .filter(Objects::nonNull)
                .collect(Collectors.toUnmodifiableSet());
    }

    @Override
    @NotNull
    public Set<String> getInheritedGroups(@NotNull ContextSet contexts) {
        return nodes.stream()
                .filter(Node::isGroupNode)
                .filter(node -> !node.isExpired())
                .filter(node -> node.appliesIn(contexts))
                .map(Node::getGroupName)
                .filter(Objects::nonNull)
                .collect(Collectors.toUnmodifiableSet());
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
     * Adds a parent group to inherit from.
     *
     * @param parentName the parent group name
     * @return the result
     */
    @NotNull
    public DataMutateResult addParent(@NotNull String parentName) {
        return addGroup(parentName);
    }

    /**
     * Removes a parent group.
     *
     * @param parentName the parent group name
     * @return the result
     */
    @NotNull
    public DataMutateResult removeParent(@NotNull String parentName) {
        return removeGroup(parentName);
    }

    /**
     * Gets all parent groups (aliases for getInheritedGroups for clarity).
     *
     * @return the parent group names
     */
    @NotNull
    public Set<String> getParentGroups() {
        return getInheritedGroups();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Group group)) return false;
        return name.equals(group.name);
    }

    @Override
    public int hashCode() {
        return name.hashCode();
    }

    @Override
    public String toString() {
        return "Group{name='" + name + "', displayName='" + displayName + "', weight=" + weight + "}";
    }
}
