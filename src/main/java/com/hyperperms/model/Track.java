package com.hyperperms.model;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;

/**
 * Represents a promotion/demotion track.
 * <p>
 * A track is an ordered list of groups that defines a promotion path.
 * For example: member -> helper -> moderator -> admin
 */
public final class Track {

    private final String name;
    private final List<String> groups;

    /**
     * Creates a new track.
     *
     * @param name   the track name
     * @param groups the ordered list of group names
     */
    public Track(@NotNull String name, @NotNull List<String> groups) {
        this.name = Objects.requireNonNull(name, "name cannot be null").toLowerCase();
        Objects.requireNonNull(groups, "groups cannot be null");
        this.groups = new ArrayList<>();
        for (String group : groups) {
            this.groups.add(group.toLowerCase());
        }
    }

    /**
     * Creates a new empty track.
     *
     * @param name the track name
     */
    public Track(@NotNull String name) {
        this(name, Collections.emptyList());
    }

    /**
     * Gets the track name.
     *
     * @return the name
     */
    @NotNull
    public String getName() {
        return name;
    }

    /**
     * Gets the ordered list of groups in this track.
     *
     * @return an unmodifiable view of the groups
     */
    @NotNull
    public List<String> getGroups() {
        return Collections.unmodifiableList(groups);
    }

    /**
     * Gets the number of groups in this track.
     *
     * @return the size
     */
    public int size() {
        return groups.size();
    }

    /**
     * Checks if this track is empty.
     *
     * @return true if empty
     */
    public boolean isEmpty() {
        return groups.isEmpty();
    }

    /**
     * Checks if this track contains the given group.
     *
     * @param groupName the group name
     * @return true if the group is in this track
     */
    public boolean containsGroup(@NotNull String groupName) {
        return groups.contains(groupName.toLowerCase());
    }

    /**
     * Gets the position of a group in this track.
     *
     * @param groupName the group name
     * @return the index, or -1 if not found
     */
    public int indexOf(@NotNull String groupName) {
        return groups.indexOf(groupName.toLowerCase());
    }

    /**
     * Gets the next group in the track after the given group (for promotion).
     *
     * @param currentGroup the current group
     * @return the next group, or null if at the end or group not in track
     */
    @Nullable
    public String getNextGroup(@NotNull String currentGroup) {
        int index = indexOf(currentGroup);
        if (index == -1 || index >= groups.size() - 1) {
            return null;
        }
        return groups.get(index + 1);
    }

    /**
     * Gets the previous group in the track before the given group (for demotion).
     *
     * @param currentGroup the current group
     * @return the previous group, or null if at the start or group not in track
     */
    @Nullable
    public String getPreviousGroup(@NotNull String currentGroup) {
        int index = indexOf(currentGroup);
        if (index <= 0) {
            return null;
        }
        return groups.get(index - 1);
    }

    /**
     * Gets the first group in this track.
     *
     * @return the first group, or null if empty
     */
    @Nullable
    public String getFirstGroup() {
        return groups.isEmpty() ? null : groups.get(0);
    }

    /**
     * Gets the last group in this track.
     *
     * @return the last group, or null if empty
     */
    @Nullable
    public String getLastGroup() {
        return groups.isEmpty() ? null : groups.get(groups.size() - 1);
    }

    /**
     * Appends a group to the end of this track.
     *
     * @param groupName the group name
     * @return true if added, false if already exists
     */
    public boolean appendGroup(@NotNull String groupName) {
        String lower = groupName.toLowerCase();
        if (groups.contains(lower)) {
            return false;
        }
        groups.add(lower);
        return true;
    }

    /**
     * Inserts a group at the specified position.
     *
     * @param index     the position
     * @param groupName the group name
     * @return true if inserted, false if already exists
     * @throws IndexOutOfBoundsException if index is out of range
     */
    public boolean insertGroup(int index, @NotNull String groupName) {
        String lower = groupName.toLowerCase();
        if (groups.contains(lower)) {
            return false;
        }
        groups.add(index, lower);
        return true;
    }

    /**
     * Removes a group from this track.
     *
     * @param groupName the group name
     * @return true if removed
     */
    public boolean removeGroup(@NotNull String groupName) {
        return groups.remove(groupName.toLowerCase());
    }

    /**
     * Clears all groups from this track.
     */
    public void clear() {
        groups.clear();
    }

    /**
     * Sets all groups in this track.
     *
     * @param newGroups the new list of groups
     */
    public void setGroups(@NotNull List<String> newGroups) {
        groups.clear();
        for (String group : newGroups) {
            groups.add(group.toLowerCase());
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Track track)) return false;
        return name.equals(track.name);
    }

    @Override
    public int hashCode() {
        return name.hashCode();
    }

    @Override
    public String toString() {
        return "Track{name='" + name + "', groups=" + groups + "}";
    }
}
