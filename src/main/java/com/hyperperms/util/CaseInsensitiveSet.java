package com.hyperperms.util;

import org.jetbrains.annotations.NotNull;

import java.util.*;

/**
 * A Set implementation that performs case-insensitive contains() checks.
 * <p>
 * This is used for compatibility with Hytale's permission system, which may use
 * different case conventions (e.g., "gameMode" vs "gamemode") when checking permissions.
 * By wrapping our permission sets in this class, we ensure that permission checks
 * work regardless of case differences.
 * <p>
 * The set stores permissions in their original case but performs lookups
 * case-insensitively.
 */
public class CaseInsensitiveSet extends AbstractSet<String> {

    private final Set<String> delegate;
    private final Map<String, String> lowercaseMap;

    /**
     * Creates a new CaseInsensitiveSet wrapping the given permissions.
     *
     * @param permissions the permissions to wrap
     */
    public CaseInsensitiveSet(@NotNull Set<String> permissions) {
        this.delegate = new HashSet<>(permissions);
        this.lowercaseMap = new HashMap<>();
        for (String perm : permissions) {
            lowercaseMap.put(perm.toLowerCase(), perm);
        }
    }

    /**
     * Performs a case-insensitive check for the given permission.
     *
     * @param o the permission to check for
     * @return true if the set contains the permission (case-insensitive)
     */
    @Override
    public boolean contains(Object o) {
        if (o instanceof String) {
            return lowercaseMap.containsKey(((String) o).toLowerCase());
        }
        return false;
    }

    /**
     * Performs a case-insensitive check for all given permissions.
     *
     * @param c the collection of permissions to check
     * @return true if all permissions are contained (case-insensitive)
     */
    @Override
    public boolean containsAll(@NotNull Collection<?> c) {
        for (Object o : c) {
            if (!contains(o)) {
                return false;
            }
        }
        return true;
    }

    @Override
    @NotNull
    public Iterator<String> iterator() {
        return delegate.iterator();
    }

    @Override
    public int size() {
        return delegate.size();
    }

    @Override
    public boolean isEmpty() {
        return delegate.isEmpty();
    }

    @Override
    @NotNull
    public Object[] toArray() {
        return delegate.toArray();
    }

    @Override
    @NotNull
    public <T> T[] toArray(@NotNull T[] a) {
        return delegate.toArray(a);
    }
}
