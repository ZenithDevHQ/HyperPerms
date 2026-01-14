package com.hyperperms.resolver;

import com.hyperperms.api.context.ContextSet;
import com.hyperperms.model.Group;
import com.hyperperms.model.Node;
import org.jetbrains.annotations.NotNull;

import java.util.*;
import java.util.function.Function;

/**
 * Handles group inheritance resolution with cycle detection.
 */
public final class InheritanceGraph {

    private final Function<String, Group> groupLoader;

    /**
     * Creates a new inheritance graph.
     *
     * @param groupLoader function to load groups by name
     */
    public InheritanceGraph(@NotNull Function<String, Group> groupLoader) {
        this.groupLoader = Objects.requireNonNull(groupLoader, "groupLoader cannot be null");
    }

    /**
     * Resolves all inherited groups for a starting set of group names.
     * Groups are returned in inheritance order (lowest weight first).
     *
     * @param startGroups the initial group names
     * @param contexts    the current context for filtering
     * @return ordered list of groups
     */
    @NotNull
    public List<Group> resolveInheritance(@NotNull Set<String> startGroups, @NotNull ContextSet contexts) {
        Set<String> visited = new HashSet<>();
        List<Group> result = new ArrayList<>();

        // Use a priority queue to process groups by weight
        Deque<String> queue = new ArrayDeque<>(startGroups);

        while (!queue.isEmpty()) {
            String groupName = queue.poll();

            if (visited.contains(groupName)) {
                continue; // Skip cycles
            }
            visited.add(groupName);

            Group group = groupLoader.apply(groupName);
            if (group == null) {
                continue; // Group doesn't exist
            }

            result.add(group);

            // Add parent groups to the queue
            for (String parent : group.getInheritedGroups(contexts)) {
                if (!visited.contains(parent)) {
                    queue.add(parent);
                }
            }
        }

        // Sort by weight (ascending - lowest weight = lowest priority)
        result.sort(Comparator.comparingInt(Group::getWeight));

        return result;
    }

    /**
     * Collects all permission nodes from a list of groups.
     * Nodes are collected in order, with later groups overriding earlier ones.
     *
     * @param groups   the groups in inheritance order
     * @param contexts the current context for filtering
     * @return list of applicable nodes
     */
    @NotNull
    public List<Node> collectNodes(@NotNull List<Group> groups, @NotNull ContextSet contexts) {
        List<Node> nodes = new ArrayList<>();

        for (Group group : groups) {
            for (Node node : group.getNodes()) {
                if (!node.isExpired() && !node.isGroupNode() && node.appliesIn(contexts)) {
                    nodes.add(node);
                }
            }
        }

        return nodes;
    }

    /**
     * Checks if adding a parent group would create a cycle.
     *
     * @param group      the group to add a parent to
     * @param parentName the proposed parent group name
     * @return true if adding the parent would create a cycle
     */
    public boolean wouldCreateCycle(@NotNull Group group, @NotNull String parentName) {
        // Check if parentName eventually inherits from group
        Set<String> visited = new HashSet<>();
        Deque<String> queue = new ArrayDeque<>();
        queue.add(parentName);

        while (!queue.isEmpty()) {
            String current = queue.poll();

            if (current.equalsIgnoreCase(group.getName())) {
                return true; // Found a cycle
            }

            if (visited.contains(current)) {
                continue;
            }
            visited.add(current);

            Group currentGroup = groupLoader.apply(current);
            if (currentGroup != null) {
                queue.addAll(currentGroup.getInheritedGroups());
            }
        }

        return false;
    }

    /**
     * Gets the inheritance chain for a group (for debugging).
     *
     * @param groupName the starting group
     * @return list of group names in inheritance order
     */
    @NotNull
    public List<String> getInheritanceChain(@NotNull String groupName) {
        List<Group> groups = resolveInheritance(Set.of(groupName), ContextSet.empty());
        return groups.stream().map(Group::getName).toList();
    }
}
