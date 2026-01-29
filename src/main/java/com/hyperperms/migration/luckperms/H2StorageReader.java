package com.hyperperms.migration.luckperms;

import com.hyperperms.migration.luckperms.LuckPermsData.*;
import com.hyperperms.util.Logger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.*;
import java.util.*;

/**
 * Reads LuckPerms data from H2 embedded database.
 * <p>
 * LuckPerms H2 database schema:
 * <ul>
 *   <li>luckperms_groups - Group definitions</li>
 *   <li>luckperms_group_permissions - Group permissions</li>
 *   <li>luckperms_players - User definitions</li>
 *   <li>luckperms_user_permissions - User permissions</li>
 *   <li>luckperms_tracks - Track definitions</li>
 * </ul>
 */
public final class H2StorageReader implements LuckPermsStorageReader {
    
    private static final String H2_DRIVER = "org.h2.Driver";
    
    private final Path databasePath;
    private Connection connection;
    
    public H2StorageReader(@NotNull Path databasePath) {
        this.databasePath = databasePath;
    }
    
    @Override
    @NotNull
    public LuckPermsStorageType getStorageType() {
        return LuckPermsStorageType.H2;
    }
    
    @Override
    @NotNull
    public String getStorageDescription() {
        return "H2 database (" + databasePath + ")";
    }
    
    @Override
    public boolean isAvailable() {
        if (!Files.exists(databasePath)) {
            return false;
        }
        
        // Try to load H2 driver
        try {
            Class.forName(H2_DRIVER);
            return true;
        } catch (ClassNotFoundException e) {
            Logger.warn("H2 driver not found. Add h2 to dependencies to enable H2 migration.");
            return false;
        }
    }
    
    /**
     * Opens a connection to the H2 database.
     */
    private Connection getConnection() throws SQLException {
        if (connection == null || connection.isClosed()) {
            // H2 database path without .mv.db extension
            String dbPath = databasePath.toString();
            if (dbPath.endsWith(".mv.db")) {
                dbPath = dbPath.substring(0, dbPath.length() - 6);
            }
            
            String url = "jdbc:h2:" + dbPath + ";MODE=MySQL;DATABASE_TO_LOWER=TRUE";
            connection = DriverManager.getConnection(url, "", "");
        }
        return connection;
    }
    
    @Override
    @NotNull
    public Map<String, LPGroup> readGroups() throws IOException {
        Map<String, LPGroup> groups = new LinkedHashMap<>();
        
        try {
            Connection conn = getConnection();
            
            // First, get all group names and metadata
            String groupQuery = "SELECT name FROM luckperms_groups";
            try (Statement stmt = conn.createStatement();
                 ResultSet rs = stmt.executeQuery(groupQuery)) {
                while (rs.next()) {
                    String name = rs.getString("name").toLowerCase();
                    groups.put(name, null); // Placeholder
                }
            }
            
            // For each group, get permissions and build the full group object
            for (String groupName : new ArrayList<>(groups.keySet())) {
                LPGroup group = readGroup(conn, groupName);
                if (group != null) {
                    groups.put(groupName, group);
                } else {
                    groups.remove(groupName);
                }
            }
            
        } catch (SQLException e) {
            throw new IOException("Failed to read groups from H2 database", e);
        }
        
        return groups;
    }
    
    @Nullable
    private LPGroup readGroup(Connection conn, String groupName) throws SQLException {
        List<LPNode> nodes = new ArrayList<>();
        Set<String> parents = new LinkedHashSet<>();
        int weight = 0;
        String prefix = null;
        String suffix = null;
        int prefixPriority = 0;
        int suffixPriority = 0;
        
        // Read permissions
        String permQuery = """
            SELECT permission, value, expiry, server, world
            FROM luckperms_group_permissions
            WHERE name = ?
            """;
        
        try (PreparedStatement stmt = conn.prepareStatement(permQuery)) {
            stmt.setString(1, groupName);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    String permission = rs.getString("permission");
                    boolean value = rs.getBoolean("value");
                    long expiry = rs.getLong("expiry");
                    String server = rs.getString("server");
                    String world = rs.getString("world");
                    
                    Map<String, String> contexts = new LinkedHashMap<>();
                    if (server != null && !server.equals("global")) {
                        contexts.put("server", server);
                    }
                    if (world != null && !world.equals("global")) {
                        contexts.put("world", world);
                    }
                    
                    // Check for meta permissions
                    if (permission.startsWith("meta.weight.")) {
                        try {
                            weight = Integer.parseInt(permission.substring("meta.weight.".length()));
                        } catch (NumberFormatException ignored) {}
                        continue;
                    }
                    
                    if (permission.startsWith("prefix.")) {
                        // Format: prefix.priority.value
                        String[] parts = permission.split("\\.", 3);
                        if (parts.length >= 3) {
                            int priority = Integer.parseInt(parts[1]);
                            if (priority > prefixPriority) {
                                prefixPriority = priority;
                                prefix = parts[2];
                            }
                        }
                        continue;
                    }
                    
                    if (permission.startsWith("suffix.")) {
                        String[] parts = permission.split("\\.", 3);
                        if (parts.length >= 3) {
                            int priority = Integer.parseInt(parts[1]);
                            if (priority > suffixPriority) {
                                suffixPriority = priority;
                                suffix = parts[2];
                            }
                        }
                        continue;
                    }
                    
                    // Check for parent groups
                    if (permission.startsWith("group.") && value) {
                        parents.add(permission.substring("group.".length()));
                    }
                    
                    nodes.add(new LPNode(permission, value, expiry, contexts));
                }
            }
        }
        
        return new LPGroup(groupName, weight, prefix, suffix, prefixPriority, suffixPriority, nodes, parents);
    }
    
    @Override
    @NotNull
    public Map<UUID, LPUser> readUsers() throws IOException {
        Map<UUID, LPUser> users = new LinkedHashMap<>();
        
        try {
            Connection conn = getConnection();
            
            // Get all users
            String userQuery = "SELECT uuid, username, primary_group FROM luckperms_players";
            try (Statement stmt = conn.createStatement();
                 ResultSet rs = stmt.executeQuery(userQuery)) {
                while (rs.next()) {
                    String uuidStr = rs.getString("uuid");
                    String username = rs.getString("username");
                    String primaryGroup = rs.getString("primary_group");
                    
                    try {
                        UUID uuid = UUID.fromString(uuidStr);
                        LPUser user = readUser(conn, uuid, username, primaryGroup);
                        if (user != null) {
                            users.put(uuid, user);
                        }
                    } catch (IllegalArgumentException e) {
                        Logger.warn("Invalid UUID in H2 database: %s", uuidStr);
                    }
                }
            }
            
        } catch (SQLException e) {
            throw new IOException("Failed to read users from H2 database", e);
        }
        
        return users;
    }
    
    @Nullable
    private LPUser readUser(Connection conn, UUID uuid, String username, String primaryGroup) throws SQLException {
        List<LPNode> nodes = new ArrayList<>();
        
        // Read permissions
        String permQuery = """
            SELECT permission, value, expiry, server, world
            FROM luckperms_user_permissions
            WHERE uuid = ?
            """;
        
        try (PreparedStatement stmt = conn.prepareStatement(permQuery)) {
            stmt.setString(1, uuid.toString());
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    String permission = rs.getString("permission");
                    boolean value = rs.getBoolean("value");
                    long expiry = rs.getLong("expiry");
                    String server = rs.getString("server");
                    String world = rs.getString("world");
                    
                    Map<String, String> contexts = new LinkedHashMap<>();
                    if (server != null && !server.equals("global")) {
                        contexts.put("server", server);
                    }
                    if (world != null && !world.equals("global")) {
                        contexts.put("world", world);
                    }
                    
                    nodes.add(new LPNode(permission, value, expiry, contexts));
                }
            }
        }
        
        if (primaryGroup == null || primaryGroup.isEmpty()) {
            primaryGroup = "default";
        }
        
        return new LPUser(uuid, username, primaryGroup, nodes);
    }
    
    @Override
    @NotNull
    public Map<String, LPTrack> readTracks() throws IOException {
        Map<String, LPTrack> tracks = new LinkedHashMap<>();
        
        try {
            Connection conn = getConnection();
            
            String trackQuery = "SELECT name, groups FROM luckperms_tracks";
            try (Statement stmt = conn.createStatement();
                 ResultSet rs = stmt.executeQuery(trackQuery)) {
                while (rs.next()) {
                    String name = rs.getString("name").toLowerCase();
                    String groupsJson = rs.getString("groups");
                    
                    // Parse groups from JSON array string
                    List<String> groups = parseGroupsJson(groupsJson);
                    tracks.put(name, new LPTrack(name, groups));
                }
            }
            
        } catch (SQLException e) {
            throw new IOException("Failed to read tracks from H2 database", e);
        }
        
        return tracks;
    }
    
    /**
     * Parses a JSON array string of group names.
     */
    private List<String> parseGroupsJson(String json) {
        List<String> groups = new ArrayList<>();
        if (json == null || json.isEmpty()) {
            return groups;
        }
        
        // Simple parsing for JSON array: ["group1","group2"]
        json = json.trim();
        if (json.startsWith("[") && json.endsWith("]")) {
            json = json.substring(1, json.length() - 1);
            for (String part : json.split(",")) {
                String group = part.trim();
                if (group.startsWith("\"") && group.endsWith("\"")) {
                    group = group.substring(1, group.length() - 1);
                }
                if (!group.isEmpty()) {
                    groups.add(group.toLowerCase());
                }
            }
        }
        
        return groups;
    }
    
    @Override
    public int estimateUserCount() {
        try {
            Connection conn = getConnection();
            String countQuery = "SELECT COUNT(*) FROM luckperms_players";
            try (Statement stmt = conn.createStatement();
                 ResultSet rs = stmt.executeQuery(countQuery)) {
                if (rs.next()) {
                    return rs.getInt(1);
                }
            }
        } catch (SQLException e) {
            Logger.debug("Failed to count users: %s", e.getMessage());
        }
        return -1;
    }
    
    @Override
    public void close() {
        if (connection != null) {
            try {
                connection.close();
            } catch (SQLException e) {
                Logger.debug("Error closing H2 connection: %s", e.getMessage());
            }
        }
    }
}
