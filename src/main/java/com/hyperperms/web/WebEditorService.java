package com.hyperperms.web;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.hyperperms.HyperPerms;
import com.hyperperms.util.Logger;
import com.hyperperms.web.dto.Change;
import com.hyperperms.web.dto.SessionCreateResponse;
import org.jetbrains.annotations.NotNull;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * Service for communicating with the web editor API.
 * Uses Java 11+ HttpClient for async HTTP requests.
 */
public final class WebEditorService {

    private static final Gson GSON = new GsonBuilder().create();

    private final HyperPerms hyperPerms;
    private final HttpClient httpClient;

    public WebEditorService(@NotNull HyperPerms hyperPerms) {
        this.hyperPerms = hyperPerms;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(hyperPerms.getConfig().getWebEditorTimeoutSeconds()))
                .build();
    }

    /**
     * Creates a new editor session by uploading current permission state.
     *
     * @param playerCount Current player count for metadata
     * @return CompletableFuture with the session response
     */
    public CompletableFuture<SessionCreateResponse> createSession(int playerCount) {
        String baseUrl = hyperPerms.getConfig().getWebEditorUrl();
        String url = baseUrl + "/api/session/create";

        SessionData data = SessionData.fromHyperPerms(hyperPerms, playerCount);
        String json = GSON.toJson(data);

        Logger.debug("Creating web editor session at: " + url);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .timeout(Duration.ofSeconds(hyperPerms.getConfig().getWebEditorTimeoutSeconds()))
                .header("Content-Type", "application/json")
                .header("Accept", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(json))
                .build();

        return httpClient.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                .thenApply(response -> {
                    if (response.statusCode() >= 200 && response.statusCode() < 300) {
                        try {
                            JsonObject obj = JsonParser.parseString(response.body()).getAsJsonObject();
                            String sessionId = obj.get("sessionId").getAsString();
                            String editorUrl = obj.get("url").getAsString();
                            String expiresAt = obj.has("expiresAt") ? obj.get("expiresAt").getAsString() : "";
                            return new SessionCreateResponse(sessionId, editorUrl, expiresAt);
                        } catch (Exception e) {
                            Logger.warn("Failed to parse session response: " + e.getMessage());
                            return SessionCreateResponse.error("Invalid response from server");
                        }
                    } else {
                        Logger.warn("Web editor API returned status " + response.statusCode() + ": " + response.body());
                        return SessionCreateResponse.error("Server returned status " + response.statusCode());
                    }
                })
                .exceptionally(e -> {
                    Logger.warn("Failed to create web editor session: " + e.getMessage());
                    return SessionCreateResponse.error("Connection failed: " + e.getMessage());
                });
    }


    /**
     * Fetches changes from the web editor API using a session ID.
     *
     * @param sessionId The session ID from the web editor
     * @return CompletableFuture with the list of changes
     */
    public CompletableFuture<FetchChangesResult> fetchChanges(@NotNull String sessionId) {
        String baseUrl = hyperPerms.getConfig().getWebEditorUrl();
        String url = baseUrl + "/api/session/" + sessionId + "/changes";

        Logger.debug("Fetching changes from: " + url);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .timeout(Duration.ofSeconds(hyperPerms.getConfig().getWebEditorTimeoutSeconds()))
                .header("Accept", "application/json")
                .GET()
                .build();

        return httpClient.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                .thenApply(response -> {
                    String body = response.body();
                    Logger.info("Changes API response (status " + response.statusCode() + "): " + body);
                    
                    if (response.statusCode() >= 200 && response.statusCode() < 300) {
                        try {
                            JsonObject obj = JsonParser.parseString(body).getAsJsonObject();
                            List<Change> changes = new ArrayList<>();
                            
                            Logger.info("Response has 'changes' key: " + obj.has("changes"));
                            if (obj.has("changes")) {
                                JsonElement changesElement = obj.get("changes");
                                Logger.info("changes isJsonObject: " + changesElement.isJsonObject() + 
                                           ", isJsonArray: " + changesElement.isJsonArray() +
                                           ", isJsonNull: " + changesElement.isJsonNull());
                            }
                            
                            if (obj.has("changes") && obj.get("changes").isJsonObject()) {
                                // API returns categorized changes object
                                JsonObject changesObj = obj.getAsJsonObject("changes");
                                Logger.info("Parsing categorized changes. Keys: " + changesObj.keySet());
                                
                                // Parse groups to create
                                if (changesObj.has("groupsToCreate") && changesObj.get("groupsToCreate").isJsonArray()) {
                                    JsonArray arr = changesObj.getAsJsonArray("groupsToCreate");
                                    Logger.info("groupsToCreate has " + arr.size() + " items");
                                    for (JsonElement elem : arr) {
                                        Change change = parseGroupChange(elem.getAsJsonObject(), Change.Type.GROUP_CREATED);
                                        if (change != null) changes.add(change);
                                    }
                                }
                                
                                // Parse groups to update (modified)
                                if (changesObj.has("groupsToUpdate") && changesObj.get("groupsToUpdate").isJsonArray()) {
                                    JsonArray arr = changesObj.getAsJsonArray("groupsToUpdate");
                                    Logger.info("groupsToUpdate has " + arr.size() + " items");
                                    for (JsonElement elem : arr) {
                                        List<Change> groupChanges = parseGroupUpdateChanges(elem.getAsJsonObject());
                                        Logger.info("Parsed " + groupChanges.size() + " changes from group update");
                                        changes.addAll(groupChanges);
                                    }
                                }
                                
                                // Parse groups to delete
                                if (changesObj.has("groupsToDelete") && changesObj.get("groupsToDelete").isJsonArray()) {
                                    for (JsonElement elem : changesObj.getAsJsonArray("groupsToDelete")) {
                                        String groupName = elem.isJsonPrimitive() ? elem.getAsString() 
                                            : elem.getAsJsonObject().get("name").getAsString();
                                        changes.add(Change.builder(Change.Type.GROUP_DELETED)
                                            .targetType("group")
                                            .target(groupName)
                                            .groupName(groupName)
                                            .build());
                                    }
                                }
                                
                                // Parse users to update
                                if (changesObj.has("usersToUpdate") && changesObj.get("usersToUpdate").isJsonArray()) {
                                    JsonArray arr = changesObj.getAsJsonArray("usersToUpdate");
                                    Logger.info("usersToUpdate has " + arr.size() + " items");
                                    for (JsonElement elem : arr) {
                                        List<Change> userChanges = parseUserUpdateChanges(elem.getAsJsonObject());
                                        Logger.info("Parsed " + userChanges.size() + " changes from user update");
                                        changes.addAll(userChanges);
                                    }
                                }
                                
                                // Parse users to delete
                                if (changesObj.has("usersToDelete") && changesObj.get("usersToDelete").isJsonArray()) {
                                    for (JsonElement elem : changesObj.getAsJsonArray("usersToDelete")) {
                                        String userId = elem.isJsonPrimitive() ? elem.getAsString()
                                            : elem.getAsJsonObject().get("uuid").getAsString();
                                        // User deletion is handled as removing all permissions/groups
                                        Logger.info("User deletion requested for: " + userId);
                                    }
                                }
                                
                                // Parse tracks to create
                                if (changesObj.has("tracksToCreate") && changesObj.get("tracksToCreate").isJsonArray()) {
                                    for (JsonElement elem : changesObj.getAsJsonArray("tracksToCreate")) {
                                        Change change = parseTrackChange(elem.getAsJsonObject(), Change.Type.TRACK_CREATED);
                                        if (change != null) changes.add(change);
                                    }
                                }
                                
                                // Parse tracks to update
                                if (changesObj.has("tracksToUpdate") && changesObj.get("tracksToUpdate").isJsonArray()) {
                                    for (JsonElement elem : changesObj.getAsJsonArray("tracksToUpdate")) {
                                        Change change = parseTrackChange(elem.getAsJsonObject(), Change.Type.TRACK_MODIFIED);
                                        if (change != null) changes.add(change);
                                    }
                                }
                                
                                // Parse tracks to delete
                                if (changesObj.has("tracksToDelete") && changesObj.get("tracksToDelete").isJsonArray()) {
                                    for (JsonElement elem : changesObj.getAsJsonArray("tracksToDelete")) {
                                        String trackName = elem.isJsonPrimitive() ? elem.getAsString()
                                            : elem.getAsJsonObject().get("name").getAsString();
                                        changes.add(Change.builder(Change.Type.TRACK_DELETED)
                                            .targetType("track")
                                            .target(trackName)
                                            .trackName(trackName)
                                            .build());
                                    }
                                }
                                
                                Logger.info("Parsed " + changes.size() + " change(s) from web editor");
                            } else if (obj.has("changes") && obj.get("changes").isJsonArray()) {
                                // Legacy flat array format
                                for (JsonElement element : obj.getAsJsonArray("changes")) {
                                    Change change = parseChange(element.getAsJsonObject());
                                    if (change != null) changes.add(change);
                                }
                            }
                            
                            return new FetchChangesResult(changes, null);
                        } catch (Exception e) {
                            Logger.warn("Failed to parse changes response: " + e.getMessage());
                            e.printStackTrace();
                            return new FetchChangesResult(null, "Invalid response from server: " + e.getMessage());
                        }
                    } else if (response.statusCode() == 404) {
                        return new FetchChangesResult(null, "Session not found or expired");
                    } else {
                        Logger.warn("Web editor API returned status " + response.statusCode() + ": " + body);
                        return new FetchChangesResult(null, "Server returned status " + response.statusCode());
                    }
                })
                .exceptionally(e -> {
                    Logger.warn("Failed to fetch changes: " + e.getMessage());
                    return new FetchChangesResult(null, "Connection failed: " + e.getMessage());
                });
    }

    /**
     * Result of fetching changes from the API.
     */
    public static final class FetchChangesResult {
        private final List<Change> changes;
        private final String error;

        public FetchChangesResult(List<Change> changes, String error) {
            this.changes = changes;
            this.error = error;
        }

        public List<Change> getChanges() {
            return changes;
        }

        public String getError() {
            return error;
        }

        public boolean isSuccess() {
            return error == null && changes != null;
        }
    }

    /**
     * Parses changes from a base64-encoded apply code.
     *
     * @param applyCode The base64-encoded JSON array of changes
     * @return List of parsed changes
     * @throws IllegalArgumentException if the code is invalid
     */
    public List<Change> parseApplyCode(@NotNull String applyCode) {
        try {
            byte[] decoded = Base64.getDecoder().decode(applyCode);
            String json = new String(decoded, StandardCharsets.UTF_8);
            JsonArray array = JsonParser.parseString(json).getAsJsonArray();

            List<Change> changes = new ArrayList<>();
            for (JsonElement element : array) {
                JsonObject obj = element.getAsJsonObject();
                Change change = parseChange(obj);
                if (change != null) {
                    changes.add(change);
                }
            }
            return changes;
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Invalid base64 encoding in apply code", e);
        } catch (Exception e) {
            throw new IllegalArgumentException("Failed to parse apply code: " + e.getMessage(), e);
        }
    }

    /**
     * Parses a group creation change from the API format.
     */
    private Change parseGroupChange(JsonObject groupObj, Change.Type type) {
        String name = groupObj.get("name").getAsString();
        String displayName = groupObj.has("displayName") && !groupObj.get("displayName").isJsonNull()
                ? groupObj.get("displayName").getAsString() : null;
        int weight = groupObj.has("weight") ? groupObj.get("weight").getAsInt() : 0;
        String prefix = groupObj.has("prefix") && !groupObj.get("prefix").isJsonNull()
                ? groupObj.get("prefix").getAsString() : null;
        String suffix = groupObj.has("suffix") && !groupObj.get("suffix").isJsonNull()
                ? groupObj.get("suffix").getAsString() : null;

        List<Change.PermissionNode> permissions = new ArrayList<>();
        if (groupObj.has("permissions") && groupObj.get("permissions").isJsonArray()) {
            for (JsonElement elem : groupObj.getAsJsonArray("permissions")) {
                JsonObject permObj = elem.getAsJsonObject();
                String node = permObj.get("node").getAsString();
                boolean value = !permObj.has("value") || permObj.get("value").getAsBoolean();
                Map<String, String> contexts = permObj.has("contexts")
                        ? parseContexts(permObj.getAsJsonObject("contexts"))
                        : Collections.emptyMap();
                permissions.add(new Change.PermissionNode(node, value, contexts));
            }
        }

        List<String> parents = groupObj.has("parents") && groupObj.get("parents").isJsonArray()
                ? parseStringList(groupObj.getAsJsonArray("parents"))
                : Collections.emptyList();

        Change.GroupData groupData = new Change.GroupData(name, displayName, weight, prefix, suffix, permissions, parents);

        return Change.builder(type)
                .targetType("group")
                .target(name)
                .groupName(name)
                .group(groupData)
                .build();
    }

    /**
     * Parses group update changes - creates a single GROUP_CREATED change
     * that will either create a new group or update an existing one.
     */
    private List<Change> parseGroupUpdateChanges(JsonObject groupObj) {
        List<Change> changes = new ArrayList<>();
        String groupName = groupObj.get("name").getAsString();

        // Create full group data - the ChangeApplier will handle create vs update
        Change.GroupData groupData = new Change.GroupData(
            groupName,
            groupObj.has("displayName") && !groupObj.get("displayName").isJsonNull() 
                ? groupObj.get("displayName").getAsString() : null,
            groupObj.has("weight") ? groupObj.get("weight").getAsInt() : 0,
            groupObj.has("prefix") && !groupObj.get("prefix").isJsonNull() 
                ? groupObj.get("prefix").getAsString() : null,
            groupObj.has("suffix") && !groupObj.get("suffix").isJsonNull() 
                ? groupObj.get("suffix").getAsString() : null,
            parsePermissionNodes(groupObj),
            groupObj.has("parents") && groupObj.get("parents").isJsonArray()
                ? parseStringList(groupObj.getAsJsonArray("parents"))
                : Collections.emptyList()
        );

        // Create a single change that will handle full group sync
        changes.add(Change.builder(Change.Type.GROUP_CREATED)
            .targetType("group")
            .target(groupName)
            .groupName(groupName)
            .group(groupData)
            .build());

        return changes;
    }

    /**
     * Parses permission nodes from a group/user object.
     */
    private List<Change.PermissionNode> parsePermissionNodes(JsonObject obj) {
        List<Change.PermissionNode> permissions = new ArrayList<>();
        if (obj.has("permissions") && obj.get("permissions").isJsonArray()) {
            for (JsonElement elem : obj.getAsJsonArray("permissions")) {
                JsonObject permObj = elem.getAsJsonObject();
                
                // Try different field names for the permission node
                String node = null;
                if (permObj.has("node") && !permObj.get("node").isJsonNull()) {
                    node = permObj.get("node").getAsString();
                } else if (permObj.has("permission") && !permObj.get("permission").isJsonNull()) {
                    node = permObj.get("permission").getAsString();
                } else if (permObj.has("name") && !permObj.get("name").isJsonNull()) {
                    node = permObj.get("name").getAsString();
                }
                
                if (node == null || node.isEmpty()) {
                    Logger.warn("Skipping permission with no node/permission/name field: " + permObj);
                    continue;
                }
                
                boolean value = !permObj.has("value") || permObj.get("value").isJsonNull() || permObj.get("value").getAsBoolean();
                Map<String, String> contexts = permObj.has("contexts") && !permObj.get("contexts").isJsonNull()
                        ? parseContexts(permObj.getAsJsonObject("contexts"))
                        : Collections.emptyMap();
                permissions.add(new Change.PermissionNode(node, value, contexts));
            }
        }
        return permissions;
    }

    /**
     * Parses user update changes - creates a USER_SYNC change that replaces all user data.
     */
    private List<Change> parseUserUpdateChanges(JsonObject userObj) {
        List<Change> changes = new ArrayList<>();
        String uuid = userObj.get("uuid").getAsString();
        String username = userObj.has("username") ? userObj.get("username").getAsString() : uuid;
        
        Logger.info("Parsing user update for: " + username + " (" + uuid + ")");

        // Get all the user data from the API
        String primaryGroup = userObj.has("primaryGroup") && !userObj.get("primaryGroup").isJsonNull()
            ? userObj.get("primaryGroup").getAsString() : null;
        
        // Get custom prefix/suffix
        String customPrefix = userObj.has("customPrefix") && !userObj.get("customPrefix").isJsonNull()
            ? userObj.get("customPrefix").getAsString() : null;
        String customSuffix = userObj.has("customSuffix") && !userObj.get("customSuffix").isJsonNull()
            ? userObj.get("customSuffix").getAsString() : null;
        
        List<Change.PermissionNode> permissions = parsePermissionNodes(userObj);
        
        // Get parent groups - prefer "groups" field (edited state) over "parents" (original state)
        // IMPORTANT: If "groups" field exists (even if empty), use it - empty means "no groups"
        // Only fall back to "parents" if "groups" field doesn't exist at all
        List<String> parentGroups = new ArrayList<>();
        if (userObj.has("groups") && userObj.get("groups").isJsonArray()) {
            // Use groups field (this is the edited/current state from web app)
            // Empty array means user has no additional groups - that's valid!
            parentGroups.addAll(parseStringList(userObj.getAsJsonArray("groups")));
            Logger.info("  - Using 'groups' field for parent groups (count: " + parentGroups.size() + ")");
        } else if (userObj.has("parents") && userObj.get("parents").isJsonArray()) {
            // Fall back to parents field only if groups doesn't exist
            parentGroups.addAll(parseStringList(userObj.getAsJsonArray("parents")));
            Logger.info("  - Using 'parents' field for parent groups (fallback)");
        }
        
        Logger.info("  - primaryGroup: " + primaryGroup);
        Logger.info("  - customPrefix: " + customPrefix);
        Logger.info("  - customSuffix: " + customSuffix);
        Logger.info("  - permissions: " + permissions.size());
        Logger.info("  - parent groups: " + parentGroups.size() + " " + parentGroups);

        // Create a single USER_SYNC change that will handle full replacement
        // We'll use a special marker in the Change to indicate this is a full sync
        Change.Builder builder = Change.builder(Change.Type.META_CHANGED)
            .targetType("user_sync")  // Special marker for full user sync
            .target(uuid);
        
        // Store all data in the change
        if (primaryGroup != null) {
            builder.key("primaryGroup");
            builder.metaNewValue(primaryGroup);
        }
        
        // Store parent groups as comma-separated for now
        builder.parent(String.join(",", parentGroups));
        
        changes.add(builder.build());
        
        // Add custom prefix change if present
        if (customPrefix != null) {
            changes.add(Change.builder(Change.Type.META_CHANGED)
                .targetType("user")
                .target(uuid)
                .key("prefix")
                .metaNewValue(customPrefix)
                .build());
        }
        
        // Add custom suffix change if present
        if (customSuffix != null) {
            changes.add(Change.builder(Change.Type.META_CHANGED)
                .targetType("user")
                .target(uuid)
                .key("suffix")
                .metaNewValue(customSuffix)
                .build());
        }
        
        // Also add individual permission changes
        for (Change.PermissionNode perm : permissions) {
            changes.add(Change.builder(Change.Type.PERMISSION_ADDED)
                .targetType("user")
                .target(uuid)
                .node(perm.getNode())
                .value(perm.getValue())
                .contexts(perm.getContexts())
                .build());
        }

        return changes;
    }

    /**
     * Parses a track change from the API format.
     */
    private Change parseTrackChange(JsonObject trackObj, Change.Type type) {
        String name = trackObj.get("name").getAsString();
        List<String> groups = trackObj.has("groups") && trackObj.get("groups").isJsonArray()
                ? parseStringList(trackObj.getAsJsonArray("groups"))
                : Collections.emptyList();

        Change.TrackData trackData = new Change.TrackData(name, groups);

        return Change.builder(type)
                .targetType("track")
                .target(name)
                .trackName(name)
                .track(trackData)
                .build();
    }

    private Change parseChange(JsonObject obj) {
        String typeStr = obj.get("type").getAsString();
        Change.Type type = parseChangeType(typeStr);
        if (type == null) {
            Logger.warn("Unknown change type: " + typeStr);
            return null;
        }

        Change.Builder builder = Change.builder(type);

        // Common fields
        if (obj.has("targetType")) {
            builder.targetType(obj.get("targetType").getAsString());
        }
        if (obj.has("target")) {
            builder.target(obj.get("target").getAsString());
        }

        // Permission fields
        if (obj.has("node")) {
            builder.node(obj.get("node").getAsString());
        }
        if (obj.has("value")) {
            builder.value(obj.get("value").getAsBoolean());
        }
        if (obj.has("oldValue") && !obj.get("oldValue").isJsonNull()) {
            builder.oldValue(obj.get("oldValue").getAsBoolean());
        }
        if (obj.has("newValue") && !obj.get("newValue").isJsonNull()) {
            builder.newValue(obj.get("newValue").getAsBoolean());
        }
        if (obj.has("contexts") && obj.get("contexts").isJsonObject()) {
            builder.contexts(parseContexts(obj.getAsJsonObject("contexts")));
        }

        // Group fields
        if (obj.has("groupName")) {
            builder.groupName(obj.get("groupName").getAsString());
        }
        if (obj.has("group") && obj.get("group").isJsonObject()) {
            builder.group(parseGroupData(obj.getAsJsonObject("group")));
        }

        // Parent fields
        if (obj.has("parent")) {
            builder.parent(obj.get("parent").getAsString());
        }

        // Meta fields
        if (obj.has("key")) {
            builder.key(obj.get("key").getAsString());
        }
        // Handle both meta changes (string) and permission changes (boolean)
        if (obj.has("oldValue") && obj.get("oldValue").isJsonPrimitive() && obj.get("oldValue").getAsJsonPrimitive().isString()) {
            builder.metaOldValue(obj.get("oldValue").getAsString());
        }
        if (obj.has("newValue") && obj.get("newValue").isJsonPrimitive() && obj.get("newValue").getAsJsonPrimitive().isString()) {
            builder.metaNewValue(obj.get("newValue").getAsString());
        }

        // Weight fields
        if (obj.has("oldWeight")) {
            builder.oldWeight(obj.get("oldWeight").getAsInt());
        }
        if (obj.has("newWeight")) {
            builder.newWeight(obj.get("newWeight").getAsInt());
        }

        // Track fields
        if (obj.has("trackName")) {
            builder.trackName(obj.get("trackName").getAsString());
        }
        if (obj.has("track") && obj.get("track").isJsonObject()) {
            builder.track(parseTrackData(obj.getAsJsonObject("track")));
        }
        if (obj.has("oldGroups") && obj.get("oldGroups").isJsonArray()) {
            builder.oldGroups(parseStringList(obj.getAsJsonArray("oldGroups")));
        }
        if (obj.has("newGroups") && obj.get("newGroups").isJsonArray()) {
            builder.newGroups(parseStringList(obj.getAsJsonArray("newGroups")));
        }

        return builder.build();
    }

    private Change.Type parseChangeType(String type) {
        return switch (type.toLowerCase()) {
            case "permission_added" -> Change.Type.PERMISSION_ADDED;
            case "permission_removed" -> Change.Type.PERMISSION_REMOVED;
            case "permission_modified" -> Change.Type.PERMISSION_MODIFIED;
            case "group_created" -> Change.Type.GROUP_CREATED;
            case "group_deleted" -> Change.Type.GROUP_DELETED;
            case "parent_added" -> Change.Type.PARENT_ADDED;
            case "parent_removed" -> Change.Type.PARENT_REMOVED;
            case "meta_changed" -> Change.Type.META_CHANGED;
            case "weight_changed" -> Change.Type.WEIGHT_CHANGED;
            case "track_created" -> Change.Type.TRACK_CREATED;
            case "track_deleted" -> Change.Type.TRACK_DELETED;
            case "track_modified" -> Change.Type.TRACK_MODIFIED;
            default -> null;
        };
    }

    private Map<String, String> parseContexts(JsonObject obj) {
        Map<String, String> contexts = new HashMap<>();
        for (String key : obj.keySet()) {
            contexts.put(key, obj.get(key).getAsString());
        }
        return contexts;
    }

    private Change.GroupData parseGroupData(JsonObject obj) {
        String name = obj.get("name").getAsString();
        String displayName = obj.has("displayName") && !obj.get("displayName").isJsonNull() 
                ? obj.get("displayName").getAsString() : null;
        int weight = obj.has("weight") ? obj.get("weight").getAsInt() : 0;
        String prefix = obj.has("prefix") && !obj.get("prefix").isJsonNull() 
                ? obj.get("prefix").getAsString() : null;
        String suffix = obj.has("suffix") && !obj.get("suffix").isJsonNull() 
                ? obj.get("suffix").getAsString() : null;

        List<Change.PermissionNode> permissions = new ArrayList<>();
        if (obj.has("permissions") && obj.get("permissions").isJsonArray()) {
            for (JsonElement elem : obj.getAsJsonArray("permissions")) {
                JsonObject permObj = elem.getAsJsonObject();
                String node = permObj.get("node").getAsString();
                boolean value = permObj.has("value") && permObj.get("value").getAsBoolean();
                Map<String, String> contexts = permObj.has("contexts") 
                        ? parseContexts(permObj.getAsJsonObject("contexts")) 
                        : Collections.emptyMap();
                permissions.add(new Change.PermissionNode(node, value, contexts));
            }
        }

        List<String> parents = obj.has("parents") 
                ? parseStringList(obj.getAsJsonArray("parents")) 
                : Collections.emptyList();

        return new Change.GroupData(name, displayName, weight, prefix, suffix, permissions, parents);
    }

    private Change.TrackData parseTrackData(JsonObject obj) {
        String name = obj.get("name").getAsString();
        List<String> groups = obj.has("groups") 
                ? parseStringList(obj.getAsJsonArray("groups")) 
                : Collections.emptyList();
        return new Change.TrackData(name, groups);
    }

    private List<String> parseStringList(JsonArray array) {
        List<String> list = new ArrayList<>();
        for (JsonElement elem : array) {
            list.add(elem.getAsString());
        }
        return list;
    }
}
