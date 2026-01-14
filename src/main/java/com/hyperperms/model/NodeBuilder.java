package com.hyperperms.model;

import com.hyperperms.api.context.Context;
import com.hyperperms.api.context.ContextSet;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.time.Duration;
import java.time.Instant;
import java.util.Objects;

/**
 * Fluent builder for constructing {@link Node} instances.
 */
public final class NodeBuilder {

    private final String permission;
    private boolean value = true;
    @Nullable
    private Instant expiry = null;
    private ContextSet.Builder contextBuilder = ContextSet.builder();

    NodeBuilder(@NotNull String permission) {
        this.permission = Objects.requireNonNull(permission, "permission cannot be null");
    }

    /**
     * Sets the value of the permission.
     *
     * @param value true to grant, false to deny
     * @return this builder
     */
    public NodeBuilder value(boolean value) {
        this.value = value;
        return this;
    }

    /**
     * Sets this as a granted permission.
     *
     * @return this builder
     */
    public NodeBuilder granted() {
        return value(true);
    }

    /**
     * Sets this as a denied permission.
     *
     * @return this builder
     */
    public NodeBuilder denied() {
        return value(false);
    }

    /**
     * Sets the expiry time.
     *
     * @param expiry the expiry instant, or null for permanent
     * @return this builder
     */
    public NodeBuilder expiry(@Nullable Instant expiry) {
        this.expiry = expiry;
        return this;
    }

    /**
     * Sets the expiry as a duration from now.
     *
     * @param duration the duration until expiry
     * @return this builder
     */
    public NodeBuilder expiry(@NotNull Duration duration) {
        Objects.requireNonNull(duration, "duration cannot be null");
        this.expiry = Instant.now().plus(duration);
        return this;
    }

    /**
     * Sets the expiry in seconds from now.
     *
     * @param seconds the number of seconds
     * @return this builder
     */
    public NodeBuilder expirySeconds(long seconds) {
        return expiry(Duration.ofSeconds(seconds));
    }

    /**
     * Makes this node permanent (no expiry).
     *
     * @return this builder
     */
    public NodeBuilder permanent() {
        this.expiry = null;
        return this;
    }

    /**
     * Adds a context to this node.
     *
     * @param context the context to add
     * @return this builder
     */
    public NodeBuilder context(@NotNull Context context) {
        contextBuilder.add(context);
        return this;
    }

    /**
     * Adds a context to this node.
     *
     * @param key   the context key
     * @param value the context value
     * @return this builder
     */
    public NodeBuilder context(@NotNull String key, @NotNull String value) {
        contextBuilder.add(key, value);
        return this;
    }

    /**
     * Adds a world context.
     *
     * @param world the world name
     * @return this builder
     */
    public NodeBuilder world(@NotNull String world) {
        return context(Context.WORLD_KEY, world);
    }

    /**
     * Adds a server context.
     *
     * @param server the server name
     * @return this builder
     */
    public NodeBuilder server(@NotNull String server) {
        return context(Context.SERVER_KEY, server);
    }

    /**
     * Sets the context set for this node.
     *
     * @param contexts the context set
     * @return this builder
     */
    public NodeBuilder contexts(@NotNull ContextSet contexts) {
        Objects.requireNonNull(contexts, "contexts cannot be null");
        this.contextBuilder = ContextSet.builder().addAll(contexts);
        return this;
    }

    /**
     * Clears all contexts from this node.
     *
     * @return this builder
     */
    public NodeBuilder clearContexts() {
        this.contextBuilder = ContextSet.builder();
        return this;
    }

    /**
     * Builds the node.
     *
     * @return the new node
     */
    public Node build() {
        return new Node(permission, value, expiry, contextBuilder.build());
    }
}
