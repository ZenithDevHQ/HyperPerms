package com.hyperperms.migration.luckperms;

import com.hyperperms.migration.luckperms.LuckPermsData.*;
import com.hyperperms.util.Logger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.sql.*;
import java.util.*;

/**
 * Reads LuckPerms data from MySQL/MariaDB or PostgreSQL databases.
 * <p>
 * Uses the same schema as H2 but with remote database connections.
 */
public final class SqlStorageReader implements LuckPermsStorageReader {
    
    private final LuckPermsStorageType storageType;
    private final Map<String, String> connectionDetails;
    private final String tablePrefix;
    private Connection connection;
    
    public SqlStorageReader(@NotNull LuckPermsStorageType storageType, 
                           @NotNull Map<String, String> connectionDetails) {
        this.storageType = storageType;
        this.connectionDetails = connectionDetails;
        this.tablePrefix = connectionDetails.getOrDefault("table_prefix", "luckperms_");
    }
    
    @Override
    @NotNull
    public LuckPermsStorageType getStorageType() {
        return storageType;
    }
    
    @Override
    @NotNull
    public String getStorageDescription() {
        String address = connectionDetails.getOrDefault("address", "unknown");
        String database = connectionDetails.getOrDefault("database", "unknown");
        return storageType.getDisplayName() + " (" + address + "/" + database + ")";
    }
    
    @Override
    public boolean isAvailable() {
        try {
            // Try to load the appropriate driver
            if (storageType == LuckPermsStorageType.MYSQL) {
                try {
                    Class.forName("com.mysql.cj.jdbc.Driver");
                } catch (ClassNotFoundException e) {
                    Class.forName("com.mysql.jdbc.Driver");
                }
            } else if (storageType == LuckPermsStorageType.POSTGRESQL) {
                Class.forName("org.postgresql.Driver");
            }
            
            // Test connection
            try (Connection conn = createConnection()) {
                return conn != null && !conn.isClosed();
            }
        } catch (ClassNotFoundException e) {
            Logger.warn("Database driver not found: %s", e.getMessage());
            return false;
        } catch (SQLException e) {
            Logger.warn("Cannot connect to database: %s", e.getMessage());
            return false;
        }
    }
    
    private Connection createConnection() throws SQLException {
        String address = connectionDetails.get("address");
        String database = connectionDetails.get("database");
        String username = connectionDetails.getOrDefault("username", "root");
        String password = connectionDetails.getOrDefault("password", "");
        
        String url;
        if (storageType == LuckPermsStorageType.MYSQL) {
            // Handle port if included in address
            if (!address.contains(":")) {
                address = address + ":3306";
            }
            url = "jdbc:mysql://" + address + "/" + database + 
                  "?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC";
        } else {
            if (!address.contains(":")) {
                address = address + ":5432";
            }
            url = "jdbc:postgresql://" + address + "/" + database;
        }
        
        return DriverManager.getConnection(url, username, password);
    }
    
    private Connection getConnection() throws SQLException {
        if (connection == null || connection.isClosed()) {
            connection = createConnection();
        }
        return connection;
    }
    
    @Override
    @NotNull
    public Map<String, LPGroup> readGroups() throws IOException {
        Map<String, LPGroup> groups = new LinkedHashMap<>();
        
        try {
            Connection conn = getConnection();
            
            // Get all group names
            String groupQuery = "SELECT name FROM " + tablePrefix + "groups";
            try (Statement stmt = conn.createStatement();
                 ResultSet rs = stmt.executeQuery(groupQuery)) {
                while (rs.next()) {
                    String name = rs.getString("name").toLowerCase();
                    groups.put(name, null);
                }
            }
            
            // Build full group objects
            for (String groupName : new ArrayList<>(groups.keySet())) {
                LPGroup group = readGroup(conn, groupName);
                if (group != null) {
                    groups.put(groupName, group);
                } else {
                    groups.remove(groupName);
                }
            }
            
        } catch (SQLException e) {
            throw new IOException("Failed to read groups from database", e);
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
        
        String permQuery = "SELECT permission, value, expiry, server, world FROM " + 
                          tablePrefix + "group_permissions WHERE name = ?";
        
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
                    
                    // Handle meta permissions
                    if (permission.startsWith("meta.weight.")) {
                        try {
                            weight = Integer.parseInt(permission.substring("meta.weight.".length()));
                        } catch (NumberFormatException ignored) {}
                        continue;
                    }
                    
                    if (permission.startsWith("prefix.")) {
                        String[] parts = permission.split("\\.", 3);
                        if (parts.length >= 3) {
                            try {
                                int priority = Integer.parseInt(parts[1]);
                                if (priority > prefixPriority) {
                                    prefixPriority = priority;
                                    prefix = parts[2];
                                }
                            } catch (NumberFormatException ignored) {}
                        }
                        continue;
                    }
                    
                    if (permission.startsWith("suffix.")) {
                        String[] parts = permission.split("\\.", 3);
                        if (parts.length >= 3) {
                            try {
                                int priority = Integer.parseInt(parts[1]);
                                if (priority > suffixPriority) {
                                    suffixPriority = priority;
                                    suffix = parts[2];
                                }
                            } catch (NumberFormatException ignored) {}
                        }
                        continue;
                    }
                    
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
            
            String userQuery = "SELECT uuid, username, primary_group FROM " + tablePrefix + "players";
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
                        Logger.warn("Invalid UUID in database: %s", uuidStr);
                    }
                }
            }
            
        } catch (SQLException e) {
            throw new IOException("Failed to read users from database", e);
        }
        
        return users;
    }
    
    @Nullable
    private LPUser readUser(Connection conn, UUID uuid, String username, String primaryGroup) throws SQLException {
        List<LPNode> nodes = new ArrayList<>();
        
        String permQuery = "SELECT permission, value, expiry, server, world FROM " +
                          tablePrefix + "user_permissions WHERE uuid = ?";
        
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
            
            String trackQuery = "SELECT name, groups FROM " + tablePrefix + "tracks";
            try (Statement stmt = conn.createStatement();
                 ResultSet rs = stmt.executeQuery(trackQuery)) {
                while (rs.next()) {
                    String name = rs.getString("name").toLowerCase();
                    String groupsJson = rs.getString("groups");
                    
                    List<String> groups = parseGroupsJson(groupsJson);
                    tracks.put(name, new LPTrack(name, groups));
                }
            }
            
        } catch (SQLException e) {
            throw new IOException("Failed to read tracks from database", e);
        }
        
        return tracks;
    }
    
    private List<String> parseGroupsJson(String json) {
        List<String> groups = new ArrayList<>();
        if (json == null || json.isEmpty()) {
            return groups;
        }
        
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
            String countQuery = "SELECT COUNT(*) FROM " + tablePrefix + "players";
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
                Logger.debug("Error closing database connection: %s", e.getMessage());
            }
        }
    }
}
