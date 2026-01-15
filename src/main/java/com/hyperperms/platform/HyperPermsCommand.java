package com.hyperperms.platform;

import com.hyperperms.HyperPerms;
import com.hyperperms.model.Group;
import com.hyperperms.model.Node;
import com.hyperperms.model.User;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.command.system.AbstractCommand;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.command.system.arguments.system.OptionalArg;
import com.hypixel.hytale.server.core.command.system.arguments.system.RequiredArg;
import com.hypixel.hytale.server.core.command.system.arguments.types.ArgTypes;

import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/**
 * Main HyperPerms command for Hytale.
 * Provides /hp command with various subcommands.
 */
public class HyperPermsCommand extends AbstractCommand {

    private final HyperPerms hyperPerms;

    public HyperPermsCommand(HyperPerms hyperPerms) {
        super("hp", "HyperPerms management command");
        this.hyperPerms = hyperPerms;

        // Add subcommands
        addSubCommand(new HelpSubCommand());
        addSubCommand(new GroupSubCommand(hyperPerms));
        addSubCommand(new UserSubCommand(hyperPerms));
        addSubCommand(new CheckSubCommand(hyperPerms));
        addSubCommand(new ReloadSubCommand(hyperPerms));

        // Add aliases
        addAliases("hyperperms", "perms");
    }

    @Override
    protected CompletableFuture<Void> execute(CommandContext ctx) {
        ctx.sender().sendMessage(Message.raw("HyperPerms - Permission Management"));
        ctx.sender().sendMessage(Message.raw("Use /hp help for available commands"));
        return CompletableFuture.completedFuture(null);
    }

    // ==================== Utility Methods ====================

    /**
     * Finds a user by name from loaded users.
     * Returns the user if found by exact (case-insensitive) match.
     */
    private static User findUserByName(HyperPerms hyperPerms, String name) {
        for (User user : hyperPerms.getUserManager().getLoadedUsers()) {
            if (name.equalsIgnoreCase(user.getUsername())) {
                return user;
            }
        }
        return null;
    }

    /**
     * Tries to parse a string as UUID, returns empty if not a valid UUID.
     */
    private static Optional<UUID> parseUuid(String input) {
        try {
            return Optional.of(UUID.fromString(input));
        } catch (IllegalArgumentException e) {
            return Optional.empty();
        }
    }

    /**
     * Resolves a player identifier (name or UUID string) to a User.
     * Loads from storage if necessary.
     */
    private static User resolveUser(HyperPerms hyperPerms, String identifier) {
        // Try as UUID first
        Optional<UUID> uuidOpt = parseUuid(identifier);
        if (uuidOpt.isPresent()) {
            UUID uuid = uuidOpt.get();
            User user = hyperPerms.getUserManager().getUser(uuid);
            if (user != null) {
                return user;
            }
            // Try to load from storage
            return hyperPerms.getUserManager().loadUser(uuid).join().orElse(null);
        }

        // Try as username
        return findUserByName(hyperPerms, identifier);
    }

    // ==================== Help Subcommand ====================

    private static class HelpSubCommand extends AbstractCommand {
        HelpSubCommand() {
            super("help", "Show HyperPerms help");
        }

        @Override
        protected CompletableFuture<Void> execute(CommandContext ctx) {
            ctx.sender().sendMessage(Message.raw("=== HyperPerms Commands ==="));
            ctx.sender().sendMessage(Message.raw("--- General ---"));
            ctx.sender().sendMessage(Message.raw("/hp help - Show this help"));
            ctx.sender().sendMessage(Message.raw("/hp reload - Reload configuration"));
            ctx.sender().sendMessage(Message.raw("/hp check <permission> - Check if you have a permission"));
            ctx.sender().sendMessage(Message.raw("/hp check <player> <permission> - Check player permission"));
            ctx.sender().sendMessage(Message.raw(""));
            ctx.sender().sendMessage(Message.raw("--- Group Management ---"));
            ctx.sender().sendMessage(Message.raw("/hp group list - List all groups"));
            ctx.sender().sendMessage(Message.raw("/hp group info <name> - View group info"));
            ctx.sender().sendMessage(Message.raw("/hp group create <name> - Create a group"));
            ctx.sender().sendMessage(Message.raw("/hp group delete <name> - Delete a group"));
            ctx.sender().sendMessage(Message.raw("/hp group setperm <group> <perm> [true/false] - Set permission"));
            ctx.sender().sendMessage(Message.raw("/hp group unsetperm <group> <perm> - Remove permission"));
            ctx.sender().sendMessage(Message.raw("/hp group setweight <group> <weight> - Set group weight"));
            ctx.sender().sendMessage(Message.raw("/hp group parent add <group> <parent> - Add parent group"));
            ctx.sender().sendMessage(Message.raw("/hp group parent remove <group> <parent> - Remove parent"));
            ctx.sender().sendMessage(Message.raw(""));
            ctx.sender().sendMessage(Message.raw("--- User Management ---"));
            ctx.sender().sendMessage(Message.raw("/hp user info <player> - Show user info"));
            ctx.sender().sendMessage(Message.raw("/hp user setperm <player> <perm> [true/false] - Set permission"));
            ctx.sender().sendMessage(Message.raw("/hp user unsetperm <player> <perm> - Remove permission"));
            ctx.sender().sendMessage(Message.raw("/hp user addgroup <player> <group> - Add user to group"));
            ctx.sender().sendMessage(Message.raw("/hp user removegroup <player> <group> - Remove from group"));
            ctx.sender().sendMessage(Message.raw("/hp user setprimarygroup <player> <group> - Set primary group"));
            return CompletableFuture.completedFuture(null);
        }
    }

    // ==================== Group Subcommand ====================

    private static class GroupSubCommand extends AbstractCommand {
        private final HyperPerms hyperPerms;

        GroupSubCommand(HyperPerms hyperPerms) {
            super("group", "Manage groups");
            this.hyperPerms = hyperPerms;
            addSubCommand(new GroupListSubCommand(hyperPerms));
            addSubCommand(new GroupInfoSubCommand(hyperPerms));
            addSubCommand(new GroupCreateSubCommand(hyperPerms));
            addSubCommand(new GroupDeleteSubCommand(hyperPerms));
            addSubCommand(new GroupSetPermSubCommand(hyperPerms));
            addSubCommand(new GroupUnsetPermSubCommand(hyperPerms));
            addSubCommand(new GroupSetWeightSubCommand(hyperPerms));
            addSubCommand(new GroupParentSubCommand(hyperPerms));
        }

        @Override
        protected CompletableFuture<Void> execute(CommandContext ctx) {
            ctx.sender().sendMessage(Message.raw("Usage: /hp group <list|info|create|delete|setperm|unsetperm|setweight|parent>"));
            return CompletableFuture.completedFuture(null);
        }
    }

    private static class GroupListSubCommand extends AbstractCommand {
        private final HyperPerms hyperPerms;

        GroupListSubCommand(HyperPerms hyperPerms) {
            super("list", "List all groups");
            this.hyperPerms = hyperPerms;
        }

        @Override
        protected CompletableFuture<Void> execute(CommandContext ctx) {
            var groups = hyperPerms.getGroupManager().getLoadedGroups();
            ctx.sender().sendMessage(Message.raw("=== Groups (" + groups.size() + ") ==="));
            for (Group group : groups) {
                ctx.sender().sendMessage(Message.raw("- " + group.getName() + " (weight: " + group.getWeight() + ")"));
            }
            return CompletableFuture.completedFuture(null);
        }
    }

    private static class GroupInfoSubCommand extends AbstractCommand {
        private final HyperPerms hyperPerms;
        private final RequiredArg<String> nameArg;

        GroupInfoSubCommand(HyperPerms hyperPerms) {
            super("info", "View group info");
            this.hyperPerms = hyperPerms;
            this.nameArg = withRequiredArg("name", "Group name", ArgTypes.STRING);
        }

        @Override
        protected CompletableFuture<Void> execute(CommandContext ctx) {
            String groupName = ctx.get(nameArg);
            Group group = hyperPerms.getGroupManager().getGroup(groupName);
            if (group == null) {
                ctx.sender().sendMessage(Message.raw("Group not found: " + groupName));
                return CompletableFuture.completedFuture(null);
            }
            ctx.sender().sendMessage(Message.raw("=== Group: " + group.getName() + " ==="));
            ctx.sender().sendMessage(Message.raw("Display Name: " + group.getDisplayName()));
            ctx.sender().sendMessage(Message.raw("Weight: " + group.getWeight()));

            // Show parent groups
            var parents = group.getParentGroups();
            if (!parents.isEmpty()) {
                ctx.sender().sendMessage(Message.raw("Parents: " + String.join(", ", parents)));
            }

            // Show permissions
            ctx.sender().sendMessage(Message.raw("Permissions: " + group.getNodes().size()));
            for (Node node : group.getNodes()) {
                if (!node.isGroupNode()) {
                    String prefix = node.getValue() ? "+" : "-";
                    ctx.sender().sendMessage(Message.raw("  " + prefix + " " + node.getPermission()));
                }
            }
            return CompletableFuture.completedFuture(null);
        }
    }

    private static class GroupCreateSubCommand extends AbstractCommand {
        private final HyperPerms hyperPerms;
        private final RequiredArg<String> nameArg;

        GroupCreateSubCommand(HyperPerms hyperPerms) {
            super("create", "Create a new group");
            this.hyperPerms = hyperPerms;
            this.nameArg = withRequiredArg("name", "Group name", ArgTypes.STRING);
        }

        @Override
        protected CompletableFuture<Void> execute(CommandContext ctx) {
            String groupName = ctx.get(nameArg);
            if (hyperPerms.getGroupManager().getGroup(groupName) != null) {
                ctx.sender().sendMessage(Message.raw("Group already exists: " + groupName));
                return CompletableFuture.completedFuture(null);
            }
            hyperPerms.getGroupManager().createGroup(groupName);
            ctx.sender().sendMessage(Message.raw("Created group: " + groupName));
            return CompletableFuture.completedFuture(null);
        }
    }

    private static class GroupDeleteSubCommand extends AbstractCommand {
        private final HyperPerms hyperPerms;
        private final RequiredArg<String> nameArg;

        GroupDeleteSubCommand(HyperPerms hyperPerms) {
            super("delete", "Delete a group");
            this.hyperPerms = hyperPerms;
            this.nameArg = withRequiredArg("name", "Group name", ArgTypes.STRING);
        }

        @Override
        protected CompletableFuture<Void> execute(CommandContext ctx) {
            String groupName = ctx.get(nameArg);
            if (hyperPerms.getGroupManager().getGroup(groupName) == null) {
                ctx.sender().sendMessage(Message.raw("Group not found: " + groupName));
                return CompletableFuture.completedFuture(null);
            }
            hyperPerms.getGroupManager().deleteGroup(groupName);
            ctx.sender().sendMessage(Message.raw("Deleted group: " + groupName));
            return CompletableFuture.completedFuture(null);
        }
    }

    private static class GroupSetPermSubCommand extends AbstractCommand {
        private final HyperPerms hyperPerms;
        private final RequiredArg<String> groupArg;
        private final RequiredArg<String> permArg;
        private final OptionalArg<String> valueArg;

        GroupSetPermSubCommand(HyperPerms hyperPerms) {
            super("setperm", "Set a permission on a group");
            this.hyperPerms = hyperPerms;
            this.groupArg = withRequiredArg("group", "Group name", ArgTypes.STRING);
            this.permArg = withRequiredArg("permission", "Permission node", ArgTypes.STRING);
            this.valueArg = withOptionalArg("value", "true or false (default: true)", ArgTypes.STRING);
        }

        @Override
        protected CompletableFuture<Void> execute(CommandContext ctx) {
            String groupName = ctx.get(groupArg);
            String permission = ctx.get(permArg);
            String valueStr = ctx.get(valueArg);

            Group group = hyperPerms.getGroupManager().getGroup(groupName);
            if (group == null) {
                ctx.sender().sendMessage(Message.raw("Group not found: " + groupName));
                return CompletableFuture.completedFuture(null);
            }

            boolean value = valueStr == null || !valueStr.equalsIgnoreCase("false");

            Node node = Node.builder(permission).value(value).build();
            group.setNode(node);
            hyperPerms.getGroupManager().saveGroup(group);

            // Invalidate caches for users in this group
            hyperPerms.getCache().invalidateAll();

            ctx.sender().sendMessage(Message.raw("Set " + permission + " = " + value + " on group " + groupName));
            return CompletableFuture.completedFuture(null);
        }
    }

    private static class GroupUnsetPermSubCommand extends AbstractCommand {
        private final HyperPerms hyperPerms;
        private final RequiredArg<String> groupArg;
        private final RequiredArg<String> permArg;

        GroupUnsetPermSubCommand(HyperPerms hyperPerms) {
            super("unsetperm", "Remove a permission from a group");
            this.hyperPerms = hyperPerms;
            this.groupArg = withRequiredArg("group", "Group name", ArgTypes.STRING);
            this.permArg = withRequiredArg("permission", "Permission node", ArgTypes.STRING);
        }

        @Override
        protected CompletableFuture<Void> execute(CommandContext ctx) {
            String groupName = ctx.get(groupArg);
            String permission = ctx.get(permArg);

            Group group = hyperPerms.getGroupManager().getGroup(groupName);
            if (group == null) {
                ctx.sender().sendMessage(Message.raw("Group not found: " + groupName));
                return CompletableFuture.completedFuture(null);
            }

            var result = group.removeNode(permission);
            if (result == com.hyperperms.api.PermissionHolder.DataMutateResult.SUCCESS) {
                hyperPerms.getGroupManager().saveGroup(group);
                hyperPerms.getCache().invalidateAll();
                ctx.sender().sendMessage(Message.raw("Removed " + permission + " from group " + groupName));
            } else {
                ctx.sender().sendMessage(Message.raw("Group " + groupName + " does not have permission " + permission));
            }
            return CompletableFuture.completedFuture(null);
        }
    }

    private static class GroupSetWeightSubCommand extends AbstractCommand {
        private final HyperPerms hyperPerms;
        private final RequiredArg<String> groupArg;
        private final RequiredArg<Integer> weightArg;

        GroupSetWeightSubCommand(HyperPerms hyperPerms) {
            super("setweight", "Set a group's weight/priority");
            this.hyperPerms = hyperPerms;
            this.groupArg = withRequiredArg("group", "Group name", ArgTypes.STRING);
            this.weightArg = withRequiredArg("weight", "Weight value", ArgTypes.INTEGER);
        }

        @Override
        protected CompletableFuture<Void> execute(CommandContext ctx) {
            String groupName = ctx.get(groupArg);
            int weight = ctx.get(weightArg);

            Group group = hyperPerms.getGroupManager().getGroup(groupName);
            if (group == null) {
                ctx.sender().sendMessage(Message.raw("Group not found: " + groupName));
                return CompletableFuture.completedFuture(null);
            }

            group.setWeight(weight);
            hyperPerms.getGroupManager().saveGroup(group);
            hyperPerms.getCache().invalidateAll();

            ctx.sender().sendMessage(Message.raw("Set weight of group " + groupName + " to " + weight));
            return CompletableFuture.completedFuture(null);
        }
    }

    private static class GroupParentSubCommand extends AbstractCommand {
        private final HyperPerms hyperPerms;

        GroupParentSubCommand(HyperPerms hyperPerms) {
            super("parent", "Manage group parents (inheritance)");
            this.hyperPerms = hyperPerms;
            addSubCommand(new GroupParentAddSubCommand(hyperPerms));
            addSubCommand(new GroupParentRemoveSubCommand(hyperPerms));
        }

        @Override
        protected CompletableFuture<Void> execute(CommandContext ctx) {
            ctx.sender().sendMessage(Message.raw("Usage: /hp group parent <add|remove> <group> <parent>"));
            return CompletableFuture.completedFuture(null);
        }
    }

    private static class GroupParentAddSubCommand extends AbstractCommand {
        private final HyperPerms hyperPerms;
        private final RequiredArg<String> groupArg;
        private final RequiredArg<String> parentArg;

        GroupParentAddSubCommand(HyperPerms hyperPerms) {
            super("add", "Add a parent group");
            this.hyperPerms = hyperPerms;
            this.groupArg = withRequiredArg("group", "Group name", ArgTypes.STRING);
            this.parentArg = withRequiredArg("parent", "Parent group name", ArgTypes.STRING);
        }

        @Override
        protected CompletableFuture<Void> execute(CommandContext ctx) {
            String groupName = ctx.get(groupArg);
            String parentName = ctx.get(parentArg);

            Group group = hyperPerms.getGroupManager().getGroup(groupName);
            if (group == null) {
                ctx.sender().sendMessage(Message.raw("Group not found: " + groupName));
                return CompletableFuture.completedFuture(null);
            }

            Group parent = hyperPerms.getGroupManager().getGroup(parentName);
            if (parent == null) {
                ctx.sender().sendMessage(Message.raw("Parent group not found: " + parentName));
                return CompletableFuture.completedFuture(null);
            }

            if (groupName.equalsIgnoreCase(parentName)) {
                ctx.sender().sendMessage(Message.raw("A group cannot inherit from itself"));
                return CompletableFuture.completedFuture(null);
            }

            var result = group.addParent(parentName);
            if (result == com.hyperperms.api.PermissionHolder.DataMutateResult.SUCCESS) {
                hyperPerms.getGroupManager().saveGroup(group);
                hyperPerms.getCache().invalidateAll();
                ctx.sender().sendMessage(Message.raw("Group " + groupName + " now inherits from " + parentName));
            } else {
                ctx.sender().sendMessage(Message.raw("Group " + groupName + " already inherits from " + parentName));
            }
            return CompletableFuture.completedFuture(null);
        }
    }

    private static class GroupParentRemoveSubCommand extends AbstractCommand {
        private final HyperPerms hyperPerms;
        private final RequiredArg<String> groupArg;
        private final RequiredArg<String> parentArg;

        GroupParentRemoveSubCommand(HyperPerms hyperPerms) {
            super("remove", "Remove a parent group");
            this.hyperPerms = hyperPerms;
            this.groupArg = withRequiredArg("group", "Group name", ArgTypes.STRING);
            this.parentArg = withRequiredArg("parent", "Parent group name", ArgTypes.STRING);
        }

        @Override
        protected CompletableFuture<Void> execute(CommandContext ctx) {
            String groupName = ctx.get(groupArg);
            String parentName = ctx.get(parentArg);

            Group group = hyperPerms.getGroupManager().getGroup(groupName);
            if (group == null) {
                ctx.sender().sendMessage(Message.raw("Group not found: " + groupName));
                return CompletableFuture.completedFuture(null);
            }

            var result = group.removeParent(parentName);
            if (result == com.hyperperms.api.PermissionHolder.DataMutateResult.SUCCESS) {
                hyperPerms.getGroupManager().saveGroup(group);
                hyperPerms.getCache().invalidateAll();
                ctx.sender().sendMessage(Message.raw("Group " + groupName + " no longer inherits from " + parentName));
            } else {
                ctx.sender().sendMessage(Message.raw("Group " + groupName + " does not inherit from " + parentName));
            }
            return CompletableFuture.completedFuture(null);
        }
    }

    // ==================== User Subcommand ====================

    private static class UserSubCommand extends AbstractCommand {
        private final HyperPerms hyperPerms;

        UserSubCommand(HyperPerms hyperPerms) {
            super("user", "Manage users");
            this.hyperPerms = hyperPerms;
            addSubCommand(new UserInfoSubCommand(hyperPerms));
            addSubCommand(new UserSetPermSubCommand(hyperPerms));
            addSubCommand(new UserUnsetPermSubCommand(hyperPerms));
            addSubCommand(new UserAddGroupSubCommand(hyperPerms));
            addSubCommand(new UserRemoveGroupSubCommand(hyperPerms));
            addSubCommand(new UserSetPrimaryGroupSubCommand(hyperPerms));
        }

        @Override
        protected CompletableFuture<Void> execute(CommandContext ctx) {
            ctx.sender().sendMessage(Message.raw("Usage: /hp user <info|setperm|unsetperm|addgroup|removegroup|setprimarygroup>"));
            return CompletableFuture.completedFuture(null);
        }
    }

    private static class UserInfoSubCommand extends AbstractCommand {
        private final HyperPerms hyperPerms;
        private final RequiredArg<String> playerArg;

        UserInfoSubCommand(HyperPerms hyperPerms) {
            super("info", "Show user's groups and permissions");
            this.hyperPerms = hyperPerms;
            this.playerArg = withRequiredArg("player", "Player name or UUID", ArgTypes.STRING);
        }

        @Override
        protected CompletableFuture<Void> execute(CommandContext ctx) {
            String identifier = ctx.get(playerArg);
            User user = resolveUser(hyperPerms, identifier);

            if (user == null) {
                ctx.sender().sendMessage(Message.raw("User not found: " + identifier));
                ctx.sender().sendMessage(Message.raw("Note: User must be online or have existing data"));
                return CompletableFuture.completedFuture(null);
            }

            ctx.sender().sendMessage(Message.raw("=== User: " + user.getFriendlyName() + " ==="));
            ctx.sender().sendMessage(Message.raw("UUID: " + user.getUuid()));
            ctx.sender().sendMessage(Message.raw("Primary Group: " + user.getPrimaryGroup()));

            // Show groups
            var groups = user.getInheritedGroups();
            if (!groups.isEmpty()) {
                ctx.sender().sendMessage(Message.raw("Groups: " + String.join(", ", groups)));
            } else {
                ctx.sender().sendMessage(Message.raw("Groups: (none)"));
            }

            // Show direct permissions (not group inheritance nodes)
            ctx.sender().sendMessage(Message.raw("Direct Permissions:"));
            int permCount = 0;
            for (Node node : user.getNodes()) {
                if (!node.isGroupNode()) {
                    String prefix = node.getValue() ? "+" : "-";
                    ctx.sender().sendMessage(Message.raw("  " + prefix + " " + node.getPermission()));
                    permCount++;
                }
            }
            if (permCount == 0) {
                ctx.sender().sendMessage(Message.raw("  (none)"));
            }

            return CompletableFuture.completedFuture(null);
        }
    }

    private static class UserSetPermSubCommand extends AbstractCommand {
        private final HyperPerms hyperPerms;
        private final RequiredArg<String> playerArg;
        private final RequiredArg<String> permArg;
        private final OptionalArg<String> valueArg;

        UserSetPermSubCommand(HyperPerms hyperPerms) {
            super("setperm", "Set a permission on a user");
            this.hyperPerms = hyperPerms;
            this.playerArg = withRequiredArg("player", "Player name or UUID", ArgTypes.STRING);
            this.permArg = withRequiredArg("permission", "Permission node", ArgTypes.STRING);
            this.valueArg = withOptionalArg("value", "true or false (default: true)", ArgTypes.STRING);
        }

        @Override
        protected CompletableFuture<Void> execute(CommandContext ctx) {
            String identifier = ctx.get(playerArg);
            String permission = ctx.get(permArg);
            String valueStr = ctx.get(valueArg);

            User user = resolveUser(hyperPerms, identifier);
            if (user == null) {
                ctx.sender().sendMessage(Message.raw("User not found: " + identifier));
                return CompletableFuture.completedFuture(null);
            }

            boolean value = valueStr == null || !valueStr.equalsIgnoreCase("false");

            Node node = Node.builder(permission).value(value).build();
            user.setNode(node);
            hyperPerms.getUserManager().saveUser(user);

            // Invalidate cache for this user
            hyperPerms.getCache().invalidate(user.getUuid());

            ctx.sender().sendMessage(Message.raw("Set " + permission + " = " + value + " on user " + user.getFriendlyName()));
            return CompletableFuture.completedFuture(null);
        }
    }

    private static class UserUnsetPermSubCommand extends AbstractCommand {
        private final HyperPerms hyperPerms;
        private final RequiredArg<String> playerArg;
        private final RequiredArg<String> permArg;

        UserUnsetPermSubCommand(HyperPerms hyperPerms) {
            super("unsetperm", "Remove a permission from a user");
            this.hyperPerms = hyperPerms;
            this.playerArg = withRequiredArg("player", "Player name or UUID", ArgTypes.STRING);
            this.permArg = withRequiredArg("permission", "Permission node", ArgTypes.STRING);
        }

        @Override
        protected CompletableFuture<Void> execute(CommandContext ctx) {
            String identifier = ctx.get(playerArg);
            String permission = ctx.get(permArg);

            User user = resolveUser(hyperPerms, identifier);
            if (user == null) {
                ctx.sender().sendMessage(Message.raw("User not found: " + identifier));
                return CompletableFuture.completedFuture(null);
            }

            var result = user.removeNode(permission);
            if (result == com.hyperperms.api.PermissionHolder.DataMutateResult.SUCCESS) {
                hyperPerms.getUserManager().saveUser(user);
                hyperPerms.getCache().invalidate(user.getUuid());
                ctx.sender().sendMessage(Message.raw("Removed " + permission + " from user " + user.getFriendlyName()));
            } else {
                ctx.sender().sendMessage(Message.raw("User " + user.getFriendlyName() + " does not have permission " + permission));
            }
            return CompletableFuture.completedFuture(null);
        }
    }

    private static class UserAddGroupSubCommand extends AbstractCommand {
        private final HyperPerms hyperPerms;
        private final RequiredArg<String> playerArg;
        private final RequiredArg<String> groupArg;

        UserAddGroupSubCommand(HyperPerms hyperPerms) {
            super("addgroup", "Add a user to a group");
            this.hyperPerms = hyperPerms;
            this.playerArg = withRequiredArg("player", "Player name or UUID", ArgTypes.STRING);
            this.groupArg = withRequiredArg("group", "Group name", ArgTypes.STRING);
        }

        @Override
        protected CompletableFuture<Void> execute(CommandContext ctx) {
            String identifier = ctx.get(playerArg);
            String groupName = ctx.get(groupArg);

            User user = resolveUser(hyperPerms, identifier);
            if (user == null) {
                ctx.sender().sendMessage(Message.raw("User not found: " + identifier));
                return CompletableFuture.completedFuture(null);
            }

            Group group = hyperPerms.getGroupManager().getGroup(groupName);
            if (group == null) {
                ctx.sender().sendMessage(Message.raw("Group not found: " + groupName));
                return CompletableFuture.completedFuture(null);
            }

            var result = user.addGroup(groupName);
            if (result == com.hyperperms.api.PermissionHolder.DataMutateResult.SUCCESS) {
                hyperPerms.getUserManager().saveUser(user);
                hyperPerms.getCache().invalidate(user.getUuid());
                ctx.sender().sendMessage(Message.raw("Added user " + user.getFriendlyName() + " to group " + groupName));
            } else {
                ctx.sender().sendMessage(Message.raw("User " + user.getFriendlyName() + " is already in group " + groupName));
            }
            return CompletableFuture.completedFuture(null);
        }
    }

    private static class UserRemoveGroupSubCommand extends AbstractCommand {
        private final HyperPerms hyperPerms;
        private final RequiredArg<String> playerArg;
        private final RequiredArg<String> groupArg;

        UserRemoveGroupSubCommand(HyperPerms hyperPerms) {
            super("removegroup", "Remove a user from a group");
            this.hyperPerms = hyperPerms;
            this.playerArg = withRequiredArg("player", "Player name or UUID", ArgTypes.STRING);
            this.groupArg = withRequiredArg("group", "Group name", ArgTypes.STRING);
        }

        @Override
        protected CompletableFuture<Void> execute(CommandContext ctx) {
            String identifier = ctx.get(playerArg);
            String groupName = ctx.get(groupArg);

            User user = resolveUser(hyperPerms, identifier);
            if (user == null) {
                ctx.sender().sendMessage(Message.raw("User not found: " + identifier));
                return CompletableFuture.completedFuture(null);
            }

            var result = user.removeGroup(groupName);
            if (result == com.hyperperms.api.PermissionHolder.DataMutateResult.SUCCESS) {
                hyperPerms.getUserManager().saveUser(user);
                hyperPerms.getCache().invalidate(user.getUuid());
                ctx.sender().sendMessage(Message.raw("Removed user " + user.getFriendlyName() + " from group " + groupName));
            } else {
                ctx.sender().sendMessage(Message.raw("User " + user.getFriendlyName() + " is not in group " + groupName));
            }
            return CompletableFuture.completedFuture(null);
        }
    }

    private static class UserSetPrimaryGroupSubCommand extends AbstractCommand {
        private final HyperPerms hyperPerms;
        private final RequiredArg<String> playerArg;
        private final RequiredArg<String> groupArg;

        UserSetPrimaryGroupSubCommand(HyperPerms hyperPerms) {
            super("setprimarygroup", "Set a user's primary/display group");
            this.hyperPerms = hyperPerms;
            this.playerArg = withRequiredArg("player", "Player name or UUID", ArgTypes.STRING);
            this.groupArg = withRequiredArg("group", "Group name", ArgTypes.STRING);
        }

        @Override
        protected CompletableFuture<Void> execute(CommandContext ctx) {
            String identifier = ctx.get(playerArg);
            String groupName = ctx.get(groupArg);

            User user = resolveUser(hyperPerms, identifier);
            if (user == null) {
                ctx.sender().sendMessage(Message.raw("User not found: " + identifier));
                return CompletableFuture.completedFuture(null);
            }

            Group group = hyperPerms.getGroupManager().getGroup(groupName);
            if (group == null) {
                ctx.sender().sendMessage(Message.raw("Group not found: " + groupName));
                return CompletableFuture.completedFuture(null);
            }

            // Ensure user is in the group
            if (!user.getInheritedGroups().contains(groupName.toLowerCase())) {
                // Add them to the group automatically
                user.addGroup(groupName);
            }

            user.setPrimaryGroup(groupName.toLowerCase());
            hyperPerms.getUserManager().saveUser(user);
            hyperPerms.getCache().invalidate(user.getUuid());

            ctx.sender().sendMessage(Message.raw("Set primary group of " + user.getFriendlyName() + " to " + groupName));
            return CompletableFuture.completedFuture(null);
        }
    }

    // ==================== Check Subcommand ====================

    private static class CheckSubCommand extends AbstractCommand {
        private final HyperPerms hyperPerms;
        private final RequiredArg<String> arg1;
        private final OptionalArg<String> arg2;

        CheckSubCommand(HyperPerms hyperPerms) {
            super("check", "Check permission for self or another player");
            this.hyperPerms = hyperPerms;
            this.arg1 = withRequiredArg("permission_or_player", "Permission to check, or player name/UUID", ArgTypes.STRING);
            this.arg2 = withOptionalArg("permission", "Permission (when checking another player)", ArgTypes.STRING);
        }

        @Override
        protected CompletableFuture<Void> execute(CommandContext ctx) {
            String arg1Value = ctx.get(arg1);
            String arg2Value = ctx.get(arg2);

            if (arg2Value != null) {
                // /hp check <player> <permission>
                String identifier = arg1Value;
                String permission = arg2Value;

                User user = resolveUser(hyperPerms, identifier);
                if (user == null) {
                    ctx.sender().sendMessage(Message.raw("User not found: " + identifier));
                    return CompletableFuture.completedFuture(null);
                }

                boolean hasPermission = hyperPerms.hasPermission(user.getUuid(), permission);
                String result = hasPermission ? "YES" : "NO";
                ctx.sender().sendMessage(Message.raw("Permission check: " + user.getFriendlyName() + " has " + permission + ": " + result));
            } else {
                // /hp check <permission> - check sender
                // For console or non-player senders, we can't check permissions
                // Try to get UUID from CommandSender if available
                String permission = arg1Value;

                // Since we can't easily get the sender's UUID from the Hytale API without more context,
                // we'll provide a helpful message
                ctx.sender().sendMessage(Message.raw("To check your own permission, use: /hp check <yourname> " + permission));
                ctx.sender().sendMessage(Message.raw("Example: /hp check PlayerName " + permission));
            }

            return CompletableFuture.completedFuture(null);
        }
    }

    // ==================== Reload Subcommand ====================

    private static class ReloadSubCommand extends AbstractCommand {
        private final HyperPerms hyperPerms;

        ReloadSubCommand(HyperPerms hyperPerms) {
            super("reload", "Reload HyperPerms configuration");
            this.hyperPerms = hyperPerms;
        }

        @Override
        protected CompletableFuture<Void> execute(CommandContext ctx) {
            ctx.sender().sendMessage(Message.raw("Reloading HyperPerms..."));
            hyperPerms.reload();
            ctx.sender().sendMessage(Message.raw("HyperPerms reloaded successfully!"));
            return CompletableFuture.completedFuture(null);
        }
    }
}
