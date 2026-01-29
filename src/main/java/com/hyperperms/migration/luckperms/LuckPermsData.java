package com.hyperperms.migration.luckperms;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;

/**
 * Data models for LuckPerms permission data.
 * <p>
 * These models represent the LuckPerms data format before transformation
 * to HyperPerms format.
 */
public final class LuckPermsData {
    
    private LuckPermsData() {}
    
    /**
     * Represents a LuckPerms permission node.
     */
    public record LPNode(
        /**
         * The permission string.
         */
        @NotNull String permission,
        
        /**
         * The value (true = grant, false = deny).
         */
        boolean value,
        
        /**
         * Unix timestamp when this permission expires, or 0 for permanent.
         */
        long expiry,
        
        /**
         * Context restrictions for this permission.
         */
        @NotNull Map<String, String> contexts
    ) {
        /**
         * Creates a simple permanent granted permission.
         */
        public LPNode(@NotNull String permission) {
            this(permission, true, 0, Collections.emptyMap());
        }
        
        /**
         * Checks if this is a group inheritance node.
         */
        public boolean isGroupNode() {
            return permission.startsWith("group.");
        }
        
        /**
         * Gets the group name if this is a group node.
         */
        @Nullable
        public String getGroupName() {
            if (!isGroupNode()) return null;
            return permission.substring("group.".length());
        }
        
        /**
         * Checks if this permission has expired.
         */
        public boolean isExpired() {
            return expiry > 0 && System.currentTimeMillis() / 1000 > expiry;
        }
        
        /**
         * Checks if this is a temporary permission.
         */
        public boolean isTemporary() {
            return expiry > 0;
        }
        
        /**
         * Checks if this permission has context restrictions.
         */
        public boolean hasContexts() {
            return !contexts.isEmpty();
        }
    }
    
    /**
     * Represents a LuckPerms group.
     */
    public record LPGroup(
        /**
         * The group name (lowercase identifier).
         */
        @NotNull String name,
        
        /**
         * The group weight (higher = more priority).
         */
        int weight,
        
        /**
         * The group's prefix for chat display.
         */
        @Nullable String prefix,
        
        /**
         * The group's suffix for chat display.
         */
        @Nullable String suffix,
        
        /**
         * Priority for prefix (higher = preferred).
         */
        int prefixPriority,
        
        /**
         * Priority for suffix (higher = preferred).
         */
        int suffixPriority,
        
        /**
         * All permission nodes for this group.
         */
        @NotNull List<LPNode> nodes,
        
        /**
         * Parent groups this group inherits from (extracted from nodes).
         */
        @NotNull Set<String> parents
    ) {
        /**
         * Gets non-group permission nodes.
         */
        public List<LPNode> getPermissionNodes() {
            return nodes.stream()
                .filter(n -> !n.isGroupNode())
                .toList();
        }
        
        /**
         * Gets the count of non-group permission nodes.
         */
        public int getPermissionCount() {
            return (int) nodes.stream()
                .filter(n -> !n.isGroupNode())
                .count();
        }
    }
    
    /**
     * Represents a LuckPerms user.
     */
    public record LPUser(
        /**
         * The user's UUID.
         */
        @NotNull UUID uuid,
        
        /**
         * The user's last known username.
         */
        @Nullable String username,
        
        /**
         * The user's primary group.
         */
        @NotNull String primaryGroup,
        
        /**
         * All permission nodes for this user.
         */
        @NotNull List<LPNode> nodes
    ) {
        /**
         * Gets non-group permission nodes.
         */
        public List<LPNode> getPermissionNodes() {
            return nodes.stream()
                .filter(n -> !n.isGroupNode())
                .toList();
        }
        
        /**
         * Gets the groups this user belongs to (extracted from nodes).
         */
        public Set<String> getGroups() {
            Set<String> groups = new LinkedHashSet<>();
            for (LPNode node : nodes) {
                if (node.isGroupNode() && node.value() && !node.isExpired()) {
                    String groupName = node.getGroupName();
                    if (groupName != null) {
                        groups.add(groupName);
                    }
                }
            }
            return groups;
        }
        
        /**
         * Checks if this user has custom permissions beyond group membership.
         */
        public boolean hasCustomPermissions() {
            return nodes.stream().anyMatch(n -> !n.isGroupNode());
        }
    }
    
    /**
     * Represents a LuckPerms track (promotion ladder).
     */
    public record LPTrack(
        /**
         * The track name.
         */
        @NotNull String name,
        
        /**
         * Ordered list of groups in the track.
         */
        @NotNull List<String> groups
    ) {}
    
    /**
     * Container for all LuckPerms data.
     */
    public record LPDataSet(
        @NotNull Map<String, LPGroup> groups,
        @NotNull Map<UUID, LPUser> users,
        @NotNull Map<String, LPTrack> tracks
    ) {
        public static LPDataSet empty() {
            return new LPDataSet(
                Collections.emptyMap(),
                Collections.emptyMap(),
                Collections.emptyMap()
            );
        }
    }
}
