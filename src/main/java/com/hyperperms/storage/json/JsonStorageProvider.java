package com.hyperperms.storage.json;

import com.google.gson.*;
import com.hyperperms.api.context.Context;
import com.hyperperms.api.context.ContextSet;
import com.hyperperms.model.Group;
import com.hyperperms.model.Node;
import com.hyperperms.model.Track;
import com.hyperperms.model.User;
import com.hyperperms.storage.StorageProvider;
import com.hyperperms.util.Logger;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.lang.reflect.Type;
import java.nio.file.*;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/**
 * JSON file-based storage provider.
 * <p>
 * Stores data in separate JSON files:
 * <ul>
 *   <li>{@code users/<uuid>.json} - User data</li>
 *   <li>{@code groups/<name>.json} - Group data</li>
 *   <li>{@code tracks/<name>.json} - Track data</li>
 * </ul>
 */
public final class JsonStorageProvider implements StorageProvider {

    private final Path dataDirectory;
    private final Path usersDirectory;
    private final Path groupsDirectory;
    private final Path tracksDirectory;
    private final Gson gson;
    private final ExecutorService executor;
    private volatile boolean healthy = false;

    public JsonStorageProvider(@NotNull Path dataDirectory) {
        this.dataDirectory = dataDirectory;
        this.usersDirectory = dataDirectory.resolve("users");
        this.groupsDirectory = dataDirectory.resolve("groups");
        this.tracksDirectory = dataDirectory.resolve("tracks");
        this.executor = Executors.newSingleThreadExecutor(r -> {
            Thread t = new Thread(r, "HyperPerms-JsonStorage");
            t.setDaemon(true);
            return t;
        });
        this.gson = createGson();
    }

    private Gson createGson() {
        return new GsonBuilder()
                .setPrettyPrinting()
                .registerTypeAdapter(Instant.class, new InstantAdapter())
                .registerTypeAdapter(ContextSet.class, new ContextSetAdapter())
                .registerTypeAdapter(Node.class, new NodeAdapter())
                .registerTypeAdapter(User.class, new UserAdapter())
                .registerTypeAdapter(Group.class, new GroupAdapter())
                .registerTypeAdapter(Track.class, new TrackAdapter())
                .create();
    }

    @Override
    @NotNull
    public String getName() {
        return "JSON";
    }

    @Override
    public CompletableFuture<Void> init() {
        return CompletableFuture.runAsync(() -> {
            try {
                Files.createDirectories(usersDirectory);
                Files.createDirectories(groupsDirectory);
                Files.createDirectories(tracksDirectory);
                healthy = true;
                Logger.info("JSON storage initialized at: " + dataDirectory);
            } catch (IOException e) {
                healthy = false;
                throw new RuntimeException("Failed to initialize JSON storage", e);
            }
        }, executor);
    }

    @Override
    public CompletableFuture<Void> shutdown() {
        return CompletableFuture.runAsync(() -> {
            healthy = false;
            executor.shutdown();
            try {
                if (!executor.awaitTermination(5, TimeUnit.SECONDS)) {
                    executor.shutdownNow();
                }
            } catch (InterruptedException e) {
                executor.shutdownNow();
                Thread.currentThread().interrupt();
            }
            Logger.info("JSON storage shut down");
        });
    }

    // ==================== User Operations ====================

    @Override
    public CompletableFuture<Optional<User>> loadUser(@NotNull UUID uuid) {
        return CompletableFuture.supplyAsync(() -> {
            Path file = usersDirectory.resolve(uuid + ".json");
            if (!Files.exists(file)) {
                return Optional.empty();
            }
            try {
                String json = Files.readString(file);
                User user = gson.fromJson(json, User.class);
                return Optional.ofNullable(user);
            } catch (IOException e) {
                Logger.severe("Failed to load user: " + uuid, e);
                return Optional.empty();
            }
        }, executor);
    }

    @Override
    public CompletableFuture<Void> saveUser(@NotNull User user) {
        return CompletableFuture.runAsync(() -> {
            Path file = usersDirectory.resolve(user.getUuid() + ".json");
            try {
                String json = gson.toJson(user);
                Files.writeString(file, json, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
            } catch (IOException e) {
                Logger.severe("Failed to save user: " + user.getUuid(), e);
            }
        }, executor);
    }

    @Override
    public CompletableFuture<Void> deleteUser(@NotNull UUID uuid) {
        return CompletableFuture.runAsync(() -> {
            Path file = usersDirectory.resolve(uuid + ".json");
            try {
                Files.deleteIfExists(file);
            } catch (IOException e) {
                Logger.severe("Failed to delete user: " + uuid, e);
            }
        }, executor);
    }

    @Override
    public CompletableFuture<Map<UUID, User>> loadAllUsers() {
        return CompletableFuture.supplyAsync(() -> {
            Map<UUID, User> users = new HashMap<>();
            try (var stream = Files.list(usersDirectory)) {
                stream.filter(p -> p.toString().endsWith(".json"))
                        .forEach(file -> {
                            try {
                                String json = Files.readString(file);
                                User user = gson.fromJson(json, User.class);
                                if (user != null) {
                                    users.put(user.getUuid(), user);
                                }
                            } catch (IOException e) {
                                Logger.warn("Failed to load user file: " + file.getFileName());
                            }
                        });
            } catch (IOException e) {
                Logger.severe("Failed to list users directory", e);
            }
            return users;
        }, executor);
    }

    @Override
    public CompletableFuture<Set<UUID>> getUserUuids() {
        return CompletableFuture.supplyAsync(() -> {
            Set<UUID> uuids = new HashSet<>();
            try (var stream = Files.list(usersDirectory)) {
                stream.filter(p -> p.toString().endsWith(".json"))
                        .forEach(file -> {
                            String name = file.getFileName().toString();
                            String uuidStr = name.substring(0, name.length() - 5);
                            try {
                                uuids.add(UUID.fromString(uuidStr));
                            } catch (IllegalArgumentException ignored) {}
                        });
            } catch (IOException e) {
                Logger.severe("Failed to list users", e);
            }
            return uuids;
        }, executor);
    }

    @Override
    public CompletableFuture<Optional<UUID>> lookupUuid(@NotNull String username) {
        return loadAllUsers().thenApply(users -> users.values().stream()
                .filter(u -> username.equalsIgnoreCase(u.getUsername()))
                .map(User::getUuid)
                .findFirst());
    }

    // ==================== Group Operations ====================

    @Override
    public CompletableFuture<Optional<Group>> loadGroup(@NotNull String name) {
        return CompletableFuture.supplyAsync(() -> {
            Path file = groupsDirectory.resolve(name.toLowerCase() + ".json");
            if (!Files.exists(file)) {
                return Optional.empty();
            }
            try {
                String json = Files.readString(file);
                Group group = gson.fromJson(json, Group.class);
                return Optional.ofNullable(group);
            } catch (IOException e) {
                Logger.severe("Failed to load group: " + name, e);
                return Optional.empty();
            }
        }, executor);
    }

    @Override
    public CompletableFuture<Void> saveGroup(@NotNull Group group) {
        return CompletableFuture.runAsync(() -> {
            Path file = groupsDirectory.resolve(group.getName() + ".json");
            try {
                String json = gson.toJson(group);
                Files.writeString(file, json, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
            } catch (IOException e) {
                Logger.severe("Failed to save group: " + group.getName(), e);
            }
        }, executor);
    }

    @Override
    public CompletableFuture<Void> deleteGroup(@NotNull String name) {
        return CompletableFuture.runAsync(() -> {
            Path file = groupsDirectory.resolve(name.toLowerCase() + ".json");
            try {
                Files.deleteIfExists(file);
            } catch (IOException e) {
                Logger.severe("Failed to delete group: " + name, e);
            }
        }, executor);
    }

    @Override
    public CompletableFuture<Map<String, Group>> loadAllGroups() {
        return CompletableFuture.supplyAsync(() -> {
            Map<String, Group> groups = new HashMap<>();
            try (var stream = Files.list(groupsDirectory)) {
                stream.filter(p -> p.toString().endsWith(".json"))
                        .forEach(file -> {
                            try {
                                String json = Files.readString(file);
                                Group group = gson.fromJson(json, Group.class);
                                if (group != null) {
                                    groups.put(group.getName(), group);
                                }
                            } catch (IOException e) {
                                Logger.warn("Failed to load group file: " + file.getFileName());
                            }
                        });
            } catch (IOException e) {
                Logger.severe("Failed to list groups directory", e);
            }
            return groups;
        }, executor);
    }

    @Override
    public CompletableFuture<Set<String>> getGroupNames() {
        return CompletableFuture.supplyAsync(() -> {
            Set<String> names = new HashSet<>();
            try (var stream = Files.list(groupsDirectory)) {
                stream.filter(p -> p.toString().endsWith(".json"))
                        .forEach(file -> {
                            String name = file.getFileName().toString();
                            names.add(name.substring(0, name.length() - 5));
                        });
            } catch (IOException e) {
                Logger.severe("Failed to list groups", e);
            }
            return names;
        }, executor);
    }

    // ==================== Track Operations ====================

    @Override
    public CompletableFuture<Optional<Track>> loadTrack(@NotNull String name) {
        return CompletableFuture.supplyAsync(() -> {
            Path file = tracksDirectory.resolve(name.toLowerCase() + ".json");
            if (!Files.exists(file)) {
                return Optional.empty();
            }
            try {
                String json = Files.readString(file);
                Track track = gson.fromJson(json, Track.class);
                return Optional.ofNullable(track);
            } catch (IOException e) {
                Logger.severe("Failed to load track: " + name, e);
                return Optional.empty();
            }
        }, executor);
    }

    @Override
    public CompletableFuture<Void> saveTrack(@NotNull Track track) {
        return CompletableFuture.runAsync(() -> {
            Path file = tracksDirectory.resolve(track.getName() + ".json");
            try {
                String json = gson.toJson(track);
                Files.writeString(file, json, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
            } catch (IOException e) {
                Logger.severe("Failed to save track: " + track.getName(), e);
            }
        }, executor);
    }

    @Override
    public CompletableFuture<Void> deleteTrack(@NotNull String name) {
        return CompletableFuture.runAsync(() -> {
            Path file = tracksDirectory.resolve(name.toLowerCase() + ".json");
            try {
                Files.deleteIfExists(file);
            } catch (IOException e) {
                Logger.severe("Failed to delete track: " + name, e);
            }
        }, executor);
    }

    @Override
    public CompletableFuture<Map<String, Track>> loadAllTracks() {
        return CompletableFuture.supplyAsync(() -> {
            Map<String, Track> tracks = new HashMap<>();
            try (var stream = Files.list(tracksDirectory)) {
                stream.filter(p -> p.toString().endsWith(".json"))
                        .forEach(file -> {
                            try {
                                String json = Files.readString(file);
                                Track track = gson.fromJson(json, Track.class);
                                if (track != null) {
                                    tracks.put(track.getName(), track);
                                }
                            } catch (IOException e) {
                                Logger.warn("Failed to load track file: " + file.getFileName());
                            }
                        });
            } catch (IOException e) {
                Logger.severe("Failed to list tracks directory", e);
            }
            return tracks;
        }, executor);
    }

    @Override
    public CompletableFuture<Set<String>> getTrackNames() {
        return CompletableFuture.supplyAsync(() -> {
            Set<String> names = new HashSet<>();
            try (var stream = Files.list(tracksDirectory)) {
                stream.filter(p -> p.toString().endsWith(".json"))
                        .forEach(file -> {
                            String name = file.getFileName().toString();
                            names.add(name.substring(0, name.length() - 5));
                        });
            } catch (IOException e) {
                Logger.severe("Failed to list tracks", e);
            }
            return names;
        }, executor);
    }

    @Override
    public CompletableFuture<Void> saveAll() {
        // JSON storage saves immediately, so this is a no-op
        return CompletableFuture.completedFuture(null);
    }

    @Override
    public boolean isHealthy() {
        return healthy;
    }

    // ==================== Type Adapters ====================

    private static class InstantAdapter implements JsonSerializer<Instant>, JsonDeserializer<Instant> {
        @Override
        public JsonElement serialize(Instant src, Type typeOfSrc, JsonSerializationContext context) {
            return new JsonPrimitive(src.toEpochMilli());
        }

        @Override
        public Instant deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context)
                throws JsonParseException {
            return Instant.ofEpochMilli(json.getAsLong());
        }
    }

    private static class ContextSetAdapter implements JsonSerializer<ContextSet>, JsonDeserializer<ContextSet> {
        @Override
        public JsonElement serialize(ContextSet src, Type typeOfSrc, JsonSerializationContext context) {
            JsonArray array = new JsonArray();
            for (Context ctx : src) {
                array.add(ctx.toString());
            }
            return array;
        }

        @Override
        public ContextSet deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context)
                throws JsonParseException {
            if (!json.isJsonArray()) {
                return ContextSet.empty();
            }
            ContextSet.Builder builder = ContextSet.builder();
            for (JsonElement elem : json.getAsJsonArray()) {
                try {
                    builder.add(Context.parse(elem.getAsString()));
                } catch (IllegalArgumentException ignored) {}
            }
            return builder.build();
        }
    }

    private static class NodeAdapter implements JsonSerializer<Node>, JsonDeserializer<Node> {
        @Override
        public JsonElement serialize(Node src, Type typeOfSrc, JsonSerializationContext context) {
            JsonObject obj = new JsonObject();
            obj.addProperty("permission", src.getPermission());
            obj.addProperty("value", src.getValue());
            if (src.getExpiry() != null) {
                obj.add("expiry", context.serialize(src.getExpiry()));
            }
            if (!src.getContexts().isEmpty()) {
                obj.add("contexts", context.serialize(src.getContexts()));
            }
            return obj;
        }

        @Override
        public Node deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context)
                throws JsonParseException {
            JsonObject obj = json.getAsJsonObject();
            String permission = obj.get("permission").getAsString();
            boolean value = obj.has("value") ? obj.get("value").getAsBoolean() : true;
            Instant expiry = obj.has("expiry") ? context.deserialize(obj.get("expiry"), Instant.class) : null;
            ContextSet contexts = obj.has("contexts") ?
                    context.deserialize(obj.get("contexts"), ContextSet.class) : ContextSet.empty();
            return Node.builder(permission).value(value).expiry(expiry).contexts(contexts).build();
        }
    }

    private class UserAdapter implements JsonSerializer<User>, JsonDeserializer<User> {
        @Override
        public JsonElement serialize(User src, Type typeOfSrc, JsonSerializationContext context) {
            JsonObject obj = new JsonObject();
            obj.addProperty("uuid", src.getUuid().toString());
            if (src.getUsername() != null) {
                obj.addProperty("username", src.getUsername());
            }
            obj.addProperty("primaryGroup", src.getPrimaryGroup());
            JsonArray nodes = new JsonArray();
            for (Node node : src.getNodes()) {
                nodes.add(context.serialize(node));
            }
            obj.add("nodes", nodes);
            return obj;
        }

        @Override
        public User deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context)
                throws JsonParseException {
            JsonObject obj = json.getAsJsonObject();
            UUID uuid = UUID.fromString(obj.get("uuid").getAsString());
            String username = obj.has("username") ? obj.get("username").getAsString() : null;
            User user = new User(uuid, username);
            if (obj.has("primaryGroup")) {
                user.setPrimaryGroup(obj.get("primaryGroup").getAsString());
            }
            if (obj.has("nodes")) {
                for (JsonElement elem : obj.getAsJsonArray("nodes")) {
                    Node node = context.deserialize(elem, Node.class);
                    user.addNode(node);
                }
            }
            return user;
        }
    }

    private class GroupAdapter implements JsonSerializer<Group>, JsonDeserializer<Group> {
        @Override
        public JsonElement serialize(Group src, Type typeOfSrc, JsonSerializationContext context) {
            JsonObject obj = new JsonObject();
            obj.addProperty("name", src.getName());
            obj.addProperty("displayName", src.getDisplayName());
            obj.addProperty("weight", src.getWeight());
            JsonArray nodes = new JsonArray();
            for (Node node : src.getNodes()) {
                nodes.add(context.serialize(node));
            }
            obj.add("nodes", nodes);
            return obj;
        }

        @Override
        public Group deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context)
                throws JsonParseException {
            JsonObject obj = json.getAsJsonObject();
            String name = obj.get("name").getAsString();
            int weight = obj.has("weight") ? obj.get("weight").getAsInt() : 0;
            Group group = new Group(name, weight);
            if (obj.has("displayName")) {
                group.setDisplayName(obj.get("displayName").getAsString());
            }
            if (obj.has("nodes")) {
                for (JsonElement elem : obj.getAsJsonArray("nodes")) {
                    Node node = context.deserialize(elem, Node.class);
                    group.addNode(node);
                }
            }
            return group;
        }
    }

    private static class TrackAdapter implements JsonSerializer<Track>, JsonDeserializer<Track> {
        @Override
        public JsonElement serialize(Track src, Type typeOfSrc, JsonSerializationContext context) {
            JsonObject obj = new JsonObject();
            obj.addProperty("name", src.getName());
            JsonArray groups = new JsonArray();
            for (String group : src.getGroups()) {
                groups.add(group);
            }
            obj.add("groups", groups);
            return obj;
        }

        @Override
        public Track deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context)
                throws JsonParseException {
            JsonObject obj = json.getAsJsonObject();
            String name = obj.get("name").getAsString();
            List<String> groups = new ArrayList<>();
            if (obj.has("groups")) {
                for (JsonElement elem : obj.getAsJsonArray("groups")) {
                    groups.add(elem.getAsString());
                }
            }
            return new Track(name, groups);
        }
    }
}
