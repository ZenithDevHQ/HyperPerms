package com.hyperperms.model;

import com.hyperperms.api.context.ContextSet;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.time.Instant;
import java.util.Objects;

/**
 * Represents a single permission node with optional expiry and context restrictions.
 * <p>
 * Nodes are immutable and should be constructed using {@link NodeBuilder}.
 * <p>
 * A node can represent:
 * <ul>
 *   <li>A permission grant: {@code permission=true}</li>
 *   <li>A permission denial: {@code permission=false}</li>
 *   <li>A group inheritance: {@code group.groupname=true}</li>
 * </ul>
 */
public final class Node {

    /**
     * Prefix used to identify group inheritance nodes.
     */
    public static final String GROUP_PREFIX = "group.";

    private final String permission;
    private final boolean value;
    @Nullable
    private final Instant expiry;
    private final ContextSet contexts;

    Node(@NotNull String permission, boolean value, @Nullable Instant expiry, @NotNull ContextSet contexts) {
        this.permission = Objects.requireNonNull(permission, "permission cannot be null").toLowerCase();
        this.value = value;
        this.expiry = expiry;
        this.contexts = Objects.requireNonNull(contexts, "contexts cannot be null");
    }

    /**
     * Creates a new node builder.
     *
     * @param permission the permission string
     * @return a new builder
     */
    public static NodeBuilder builder(@NotNull String permission) {
        return new NodeBuilder(permission);
    }

    /**
     * Creates a simple granted permission node with no expiry or contexts.
     *
     * @param permission the permission string
     * @return a new node
     */
    public static Node of(@NotNull String permission) {
        return builder(permission).build();
    }

    /**
     * Creates a group inheritance node.
     *
     * @param groupName the group name to inherit from
     * @return a new node representing group inheritance
     */
    public static Node group(@NotNull String groupName) {
        return builder(GROUP_PREFIX + groupName.toLowerCase()).build();
    }

    /**
     * Gets the permission string.
     *
     * @return the permission
     */
    @NotNull
    public String getPermission() {
        return permission;
    }

    /**
     * Gets the value (true = granted, false = denied).
     *
     * @return the value
     */
    public boolean getValue() {
        return value;
    }

    /**
     * Gets the expiry time, or null if permanent.
     *
     * @return the expiry time, or null
     */
    @Nullable
    public Instant getExpiry() {
        return expiry;
    }

    /**
     * Gets the context set this node applies in.
     *
     * @return the contexts
     */
    @NotNull
    public ContextSet getContexts() {
        return contexts;
    }

    /**
     * Checks if this node has expired.
     *
     * @return true if expired
     */
    public boolean isExpired() {
        return expiry != null && Instant.now().isAfter(expiry);
    }

    /**
     * Checks if this node is temporary (has an expiry time).
     *
     * @return true if temporary
     */
    public boolean isTemporary() {
        return expiry != null;
    }

    /**
     * Checks if this node is permanent (no expiry time).
     *
     * @return true if permanent
     */
    public boolean isPermanent() {
        return expiry == null;
    }

    /**
     * Checks if this node represents a group inheritance.
     *
     * @return true if this is a group node
     */
    public boolean isGroupNode() {
        return permission.startsWith(GROUP_PREFIX);
    }

    /**
     * If this is a group node, returns the group name.
     *
     * @return the group name, or null if not a group node
     */
    @Nullable
    public String getGroupName() {
        if (!isGroupNode()) {
            return null;
        }
        return permission.substring(GROUP_PREFIX.length());
    }

    /**
     * Checks if this node is a negation (starts with '-').
     *
     * @return true if this is a negation node
     */
    public boolean isNegated() {
        return permission.startsWith("-");
    }

    /**
     * Gets the permission without the negation prefix if present.
     *
     * @return the base permission
     */
    @NotNull
    public String getBasePermission() {
        return isNegated() ? permission.substring(1) : permission;
    }

    /**
     * Checks if this node is a wildcard permission.
     *
     * @return true if wildcard
     */
    public boolean isWildcard() {
        return permission.equals("*") || permission.endsWith(".*");
    }

    /**
     * Checks if this node applies in the given context.
     *
     * @param currentContexts the current player context
     * @return true if this node applies
     */
    public boolean appliesIn(@NotNull ContextSet currentContexts) {
        return contexts.isSatisfiedBy(currentContexts);
    }

    /**
     * Creates a copy of this node with a new expiry.
     *
     * @param newExpiry the new expiry time, or null for permanent
     * @return a new node with the updated expiry
     */
    public Node withExpiry(@Nullable Instant newExpiry) {
        return new Node(permission, value, newExpiry, contexts);
    }

    /**
     * Creates a copy of this node with new contexts.
     *
     * @param newContexts the new contexts
     * @return a new node with the updated contexts
     */
    public Node withContexts(@NotNull ContextSet newContexts) {
        return new Node(permission, value, expiry, newContexts);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Node node)) return false;
        return value == node.value &&
                permission.equals(node.permission) &&
                Objects.equals(expiry, node.expiry) &&
                contexts.equals(node.contexts);
    }

    @Override
    public int hashCode() {
        return Objects.hash(permission, value, expiry, contexts);
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder("Node{");
        sb.append("permission='").append(permission).append('\'');
        if (!value) {
            sb.append(", value=false");
        }
        if (expiry != null) {
            sb.append(", expiry=").append(expiry);
        }
        if (!contexts.isEmpty()) {
            sb.append(", contexts=").append(contexts);
        }
        sb.append('}');
        return sb.toString();
    }

    /**
     * Checks equality based only on permission, value, and contexts (ignoring expiry).
     * Used to determine if two nodes represent the same logical permission setting.
     *
     * @param other the other node
     * @return true if logically equivalent
     */
    public boolean equalsIgnoringExpiry(@NotNull Node other) {
        return permission.equals(other.permission) &&
                value == other.value &&
                contexts.equals(other.contexts);
    }
}
