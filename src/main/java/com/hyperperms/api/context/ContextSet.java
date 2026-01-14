package com.hyperperms.api.context;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.stream.Collectors;

/**
 * An immutable set of {@link Context} entries.
 * <p>
 * Context sets are used to define when a permission applies and to represent
 * a player's current contextual state. A permission node with a context set
 * only applies when the player's current contexts satisfy all the node's contexts.
 */
public final class ContextSet implements Iterable<Context> {

    private static final ContextSet EMPTY = new ContextSet(Collections.emptySortedSet());

    private final SortedSet<Context> contexts;

    private ContextSet(@NotNull SortedSet<Context> contexts) {
        this.contexts = contexts;
    }

    /**
     * Returns an empty context set.
     *
     * @return the empty context set
     */
    public static ContextSet empty() {
        return EMPTY;
    }

    /**
     * Creates a context set containing a single context.
     *
     * @param context the context
     * @return a new context set
     */
    public static ContextSet of(@NotNull Context context) {
        Objects.requireNonNull(context, "context cannot be null");
        return new ContextSet(new TreeSet<>(Collections.singleton(context)));
    }

    /**
     * Creates a context set from multiple contexts.
     *
     * @param contexts the contexts
     * @return a new context set
     */
    public static ContextSet of(@NotNull Context... contexts) {
        Objects.requireNonNull(contexts, "contexts cannot be null");
        if (contexts.length == 0) {
            return EMPTY;
        }
        TreeSet<Context> set = new TreeSet<>();
        Collections.addAll(set, contexts);
        return new ContextSet(set);
    }

    /**
     * Creates a new builder for constructing context sets.
     *
     * @return a new builder
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Returns whether this context set is empty.
     *
     * @return true if empty
     */
    public boolean isEmpty() {
        return contexts.isEmpty();
    }

    /**
     * Returns the number of contexts in this set.
     *
     * @return the size
     */
    public int size() {
        return contexts.size();
    }

    /**
     * Returns whether this set contains the given context.
     *
     * @param context the context to check
     * @return true if the context is present
     */
    public boolean contains(@NotNull Context context) {
        return contexts.contains(context);
    }

    /**
     * Returns whether this set contains a context with the given key.
     *
     * @param key the key to check
     * @return true if a context with the key exists
     */
    public boolean containsKey(@NotNull String key) {
        return contexts.stream().anyMatch(c -> c.key().equals(key.toLowerCase()));
    }

    /**
     * Gets all values for a given key.
     *
     * @param key the key to look up
     * @return a set of values for the key
     */
    public Set<String> getValues(@NotNull String key) {
        String lowerKey = key.toLowerCase();
        return contexts.stream()
                .filter(c -> c.key().equals(lowerKey))
                .map(Context::value)
                .collect(Collectors.toUnmodifiableSet());
    }

    /**
     * Gets a single value for a given key, or null if not present.
     *
     * @param key the key to look up
     * @return the value, or null if not found
     */
    @Nullable
    public String getValue(@NotNull String key) {
        String lowerKey = key.toLowerCase();
        return contexts.stream()
                .filter(c -> c.key().equals(lowerKey))
                .map(Context::value)
                .findFirst()
                .orElse(null);
    }

    /**
     * Checks if this context set is satisfied by the given current contexts.
     * <p>
     * A context set A is satisfied by context set B if all contexts in A
     * are also present in B. An empty context set is always satisfied.
     *
     * @param currentContexts the player's current context state
     * @return true if all contexts in this set are present in the current contexts
     */
    public boolean isSatisfiedBy(@NotNull ContextSet currentContexts) {
        Objects.requireNonNull(currentContexts, "currentContexts cannot be null");
        if (this.isEmpty()) {
            return true;
        }
        return currentContexts.contexts.containsAll(this.contexts);
    }

    /**
     * Returns an unmodifiable view of the contexts in this set.
     *
     * @return the contexts
     */
    public Set<Context> toSet() {
        return Collections.unmodifiableSortedSet(contexts);
    }

    @Override
    @NotNull
    public Iterator<Context> iterator() {
        return contexts.iterator();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof ContextSet that)) return false;
        return contexts.equals(that.contexts);
    }

    @Override
    public int hashCode() {
        return contexts.hashCode();
    }

    @Override
    public String toString() {
        if (isEmpty()) {
            return "ContextSet{}";
        }
        return "ContextSet{" + contexts.stream()
                .map(Context::toString)
                .collect(Collectors.joining(", ")) + "}";
    }

    /**
     * Builder for constructing {@link ContextSet} instances.
     */
    public static final class Builder {
        private final TreeSet<Context> contexts = new TreeSet<>();

        private Builder() {}

        /**
         * Adds a context to the set.
         *
         * @param context the context to add
         * @return this builder
         */
        public Builder add(@NotNull Context context) {
            Objects.requireNonNull(context, "context cannot be null");
            contexts.add(context);
            return this;
        }

        /**
         * Adds a context with the given key and value.
         *
         * @param key   the context key
         * @param value the context value
         * @return this builder
         */
        public Builder add(@NotNull String key, @NotNull String value) {
            return add(new Context(key, value));
        }

        /**
         * Adds all contexts from another context set.
         *
         * @param other the other context set
         * @return this builder
         */
        public Builder addAll(@NotNull ContextSet other) {
            Objects.requireNonNull(other, "other cannot be null");
            contexts.addAll(other.contexts);
            return this;
        }

        /**
         * Builds the context set.
         *
         * @return the new context set
         */
        public ContextSet build() {
            if (contexts.isEmpty()) {
                return EMPTY;
            }
            return new ContextSet(new TreeSet<>(contexts));
        }
    }
}
