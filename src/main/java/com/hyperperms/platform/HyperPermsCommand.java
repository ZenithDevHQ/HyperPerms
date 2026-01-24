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
        addSubCommand(new BackupSubCommand(hyperPerms));
        addSubCommand(new ExportSubCommand(hyperPerms));
        addSubCommand(new ImportSubCommand(hyperPerms));
        addSubCommand(new ReloadSubCommand(hyperPerms));
        addSubCommand(new ResetGroupsSubCommand(hyperPerms));

        // Debug commands
        addSubCommand(new DebugSubCommand(hyperPerms));

        // Permission listing commands
        addSubCommand(new PermsSubCommand(hyperPerms));

        // Web editor commands
        if (hyperPerms.getWebEditorService() != null) {
            addSubCommand(new com.hyperperms.commands.EditorSubCommand(hyperPerms, hyperPerms.getWebEditorService()));
            addSubCommand(new com.hyperperms.commands.ApplySubCommand(hyperPerms, hyperPerms.getWebEditorService()));
        }

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
     * Loads from storage if necessary. Does not create new users.
     */
    private static User resolveUser(HyperPerms hyperPerms, String identifier) {
        return resolveUser(hyperPerms, identifier, false);
    }

    /**
     * Resolves a player identifier (name or UUID string) to a User.
     * Loads from storage if necessary.
     *
     * @param hyperPerms the HyperPerms instance
     * @param identifier player name or UUID string
     * @param createIfNotExists if true and a UUID is provided, creates the user if not found
     * @return the User, or null if not found (and createIfNotExists is false)
     */
    private static User resolveUser(HyperPerms hyperPerms, String identifier, boolean createIfNotExists) {
        // Try as UUID first
        Optional<UUID> uuidOpt = parseUuid(identifier);
        if (uuidOpt.isPresent()) {
            UUID uuid = uuidOpt.get();
            User user = hyperPerms.getUserManager().getUser(uuid);
            if (user != null) {
                return user;
            }
            // Try to load from storage
            Optional<User> loaded = hyperPerms.getUserManager().loadUser(uuid).join();
            if (loaded.isPresent()) {
                return loaded.get();
            }
            // Create if requested (useful for Tebex offline player support)
            if (createIfNotExists) {
                return hyperPerms.getUserManager().getOrCreateUser(uuid);
            }
            return null;
        }

        // Try as username
        return findUserByName(hyperPerms, identifier);
    }

    /**
     * Resolves a player identifier to a User, creating the user if a UUID is provided
     * and the user doesn't exist. This is useful for Tebex integration where players
     * may not have joined the server yet.
     */
    private static User resolveOrCreateUser(HyperPerms hyperPerms, String identifier) {
        return resolveUser(hyperPerms, identifier, true);
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
            ctx.sender().sendMessage(Message.raw("/hp group rename <old> <new> - Rename a group"));
            ctx.sender().sendMessage(Message.raw("/hp group setperm <group> <perm> [true/false] - Set permission"));
            ctx.sender().sendMessage(Message.raw("/hp group unsetperm <group> <perm> - Remove permission"));
            ctx.sender().sendMessage(Message.raw("/hp group setweight <group> <weight> - Set group weight"));
            ctx.sender().sendMessage(Message.raw("/hp group setprefix <group> [prefix] [priority] - Set prefix"));
            ctx.sender().sendMessage(Message.raw("/hp group setsuffix <group> [suffix] [priority] - Set suffix"));
            ctx.sender().sendMessage(Message.raw("/hp group setdisplayname <group> [name] - Set display name"));
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
            ctx.sender().sendMessage(Message.raw("/hp user setprefix <player> [prefix] - Set custom prefix"));
            ctx.sender().sendMessage(Message.raw("/hp user setsuffix <player> [suffix] - Set custom suffix"));
            ctx.sender().sendMessage(Message.raw("/hp user clear <player> - Clear all user data"));
            ctx.sender().sendMessage(Message.raw("/hp user clone <source> <target> - Copy permissions"));
            ctx.sender().sendMessage(Message.raw(""));
            ctx.sender().sendMessage(Message.raw("--- Backup & Data ---"));
            ctx.sender().sendMessage(Message.raw("/hp backup create - Create a manual backup"));
            ctx.sender().sendMessage(Message.raw("/hp backup list - List available backups"));
            ctx.sender().sendMessage(Message.raw("/hp backup restore <name> - Restore from backup"));
            ctx.sender().sendMessage(Message.raw("/hp export [filename] - Export data to file"));
            ctx.sender().sendMessage(Message.raw("/hp import defaults - Create default group hierarchy"));
            ctx.sender().sendMessage(Message.raw("/hp import file <filename> - Import data from file"));
            ctx.sender().sendMessage(Message.raw(""));
            ctx.sender().sendMessage(Message.raw("--- Web Editor ---"));
            ctx.sender().sendMessage(Message.raw("/hp editor - Open the web editor"));
            ctx.sender().sendMessage(Message.raw("/hp apply <code> - Apply changes from web editor"));
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
            addSubCommand(new GroupSetPrefixSubCommand(hyperPerms));
            addSubCommand(new GroupSetSuffixSubCommand(hyperPerms));
            addSubCommand(new GroupSetDisplayNameSubCommand(hyperPerms));
            addSubCommand(new GroupRenameSubCommand(hyperPerms));
            addSubCommand(new GroupParentSubCommand(hyperPerms));
        }

        @Override
        protected CompletableFuture<Void> execute(CommandContext ctx) {
            ctx.sender().sendMessage(Message.raw("Usage: /hp group <list|info|create|delete|rename|setperm|unsetperm|setweight|setprefix|setsuffix|setdisplayname|parent>"));
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
            ctx.sender().sendMessage(Message.raw("Display Name: " + (group.getDisplayName() != null ? group.getDisplayName() : group.getName())));
            ctx.sender().sendMessage(Message.raw("Weight: " + group.getWeight()));
            ctx.sender().sendMessage(Message.raw("Prefix: " + (group.getPrefix() != null ? "\"" + group.getPrefix() + "\"" : "(none)")));
            ctx.sender().sendMessage(Message.raw("Suffix: " + (group.getSuffix() != null ? "\"" + group.getSuffix() + "\"" : "(none)")));
            ctx.sender().sendMessage(Message.raw("Prefix Priority: " + group.getPrefixPriority()));
            ctx.sender().sendMessage(Message.raw("Suffix Priority: " + group.getSuffixPriority()));

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


    private static class GroupSetPrefixSubCommand extends AbstractCommand {
        private final HyperPerms hyperPerms;
        private final RequiredArg<String> groupArg;
        private final OptionalArg<String> prefixArg;
        private final OptionalArg<Integer> priorityArg;

        GroupSetPrefixSubCommand(HyperPerms hyperPerms) {
            super("setprefix", "Set a group's chat prefix");
            this.hyperPerms = hyperPerms;
            this.groupArg = withRequiredArg("group", "Group name", ArgTypes.STRING);
            this.prefixArg = withOptionalArg("prefix", "Prefix text (omit to clear)", ArgTypes.STRING);
            this.priorityArg = withOptionalArg("priority", "Priority for multi-group resolution", ArgTypes.INTEGER);
        }

        @Override
        protected CompletableFuture<Void> execute(CommandContext ctx) {
            String groupName = ctx.get(groupArg);
            String prefix = ctx.get(prefixArg);
            Integer priority = ctx.get(priorityArg);

            Group group = hyperPerms.getGroupManager().getGroup(groupName);
            if (group == null) {
                ctx.sender().sendMessage(Message.raw("Group not found: " + groupName));
                return CompletableFuture.completedFuture(null);
            }

            if (prefix == null || prefix.isEmpty()) {
                group.setPrefix(null);
                ctx.sender().sendMessage(Message.raw("Cleared prefix for group " + groupName));
            } else {
                group.setPrefix(prefix);
                ctx.sender().sendMessage(Message.raw("Set prefix of group " + groupName + " to \"" + prefix + "\""));
            }

            if (priority != null) {
                group.setPrefixPriority(priority);
                ctx.sender().sendMessage(Message.raw("Set prefix priority to " + priority));
            }

            hyperPerms.getGroupManager().saveGroup(group);
            hyperPerms.getCache().invalidateAll();

            return CompletableFuture.completedFuture(null);
        }
    }

    private static class GroupSetSuffixSubCommand extends AbstractCommand {
        private final HyperPerms hyperPerms;
        private final RequiredArg<String> groupArg;
        private final OptionalArg<String> suffixArg;
        private final OptionalArg<Integer> priorityArg;

        GroupSetSuffixSubCommand(HyperPerms hyperPerms) {
            super("setsuffix", "Set a group's chat suffix");
            this.hyperPerms = hyperPerms;
            this.groupArg = withRequiredArg("group", "Group name", ArgTypes.STRING);
            this.suffixArg = withOptionalArg("suffix", "Suffix text (omit to clear)", ArgTypes.STRING);
            this.priorityArg = withOptionalArg("priority", "Priority for multi-group resolution", ArgTypes.INTEGER);
        }

        @Override
        protected CompletableFuture<Void> execute(CommandContext ctx) {
            String groupName = ctx.get(groupArg);
            String suffix = ctx.get(suffixArg);
            Integer priority = ctx.get(priorityArg);

            Group group = hyperPerms.getGroupManager().getGroup(groupName);
            if (group == null) {
                ctx.sender().sendMessage(Message.raw("Group not found: " + groupName));
                return CompletableFuture.completedFuture(null);
            }

            if (suffix == null || suffix.isEmpty()) {
                group.setSuffix(null);
                ctx.sender().sendMessage(Message.raw("Cleared suffix for group " + groupName));
            } else {
                group.setSuffix(suffix);
                ctx.sender().sendMessage(Message.raw("Set suffix of group " + groupName + " to \"" + suffix + "\""));
            }

            if (priority != null) {
                group.setSuffixPriority(priority);
                ctx.sender().sendMessage(Message.raw("Set suffix priority to " + priority));
            }

            hyperPerms.getGroupManager().saveGroup(group);
            hyperPerms.getCache().invalidateAll();

            return CompletableFuture.completedFuture(null);
        }
    }

    private static class GroupSetDisplayNameSubCommand extends AbstractCommand {
        private final HyperPerms hyperPerms;
        private final RequiredArg<String> groupArg;
        private final OptionalArg<String> displayNameArg;

        GroupSetDisplayNameSubCommand(HyperPerms hyperPerms) {
            super("setdisplayname", "Set a group's display name");
            this.hyperPerms = hyperPerms;
            this.groupArg = withRequiredArg("group", "Group name", ArgTypes.STRING);
            this.displayNameArg = withOptionalArg("displayname", "Display name (omit to clear)", ArgTypes.STRING);
        }

        @Override
        protected CompletableFuture<Void> execute(CommandContext ctx) {
            String groupName = ctx.get(groupArg);
            String displayName = ctx.get(displayNameArg);

            Group group = hyperPerms.getGroupManager().getGroup(groupName);
            if (group == null) {
                ctx.sender().sendMessage(Message.raw("Group not found: " + groupName));
                return CompletableFuture.completedFuture(null);
            }

            if (displayName == null || displayName.isEmpty()) {
                group.setDisplayName(null);
                ctx.sender().sendMessage(Message.raw("Cleared display name for group " + groupName));
            } else {
                group.setDisplayName(displayName);
                ctx.sender().sendMessage(Message.raw("Set display name of group " + groupName + " to \"" + displayName + "\""));
            }

            hyperPerms.getGroupManager().saveGroup(group);
            return CompletableFuture.completedFuture(null);
        }
    }

    private static class GroupRenameSubCommand extends AbstractCommand {
        private final HyperPerms hyperPerms;
        private final RequiredArg<String> oldNameArg;
        private final RequiredArg<String> newNameArg;

        GroupRenameSubCommand(HyperPerms hyperPerms) {
            super("rename", "Rename a group");
            this.hyperPerms = hyperPerms;
            this.oldNameArg = withRequiredArg("oldname", "Current group name", ArgTypes.STRING);
            this.newNameArg = withRequiredArg("newname", "New group name", ArgTypes.STRING);
        }

        @Override
        protected CompletableFuture<Void> execute(CommandContext ctx) {
            String oldName = ctx.get(oldNameArg);
            String newName = ctx.get(newNameArg);

            Group group = hyperPerms.getGroupManager().getGroup(oldName);
            if (group == null) {
                ctx.sender().sendMessage(Message.raw("Group not found: " + oldName));
                return CompletableFuture.completedFuture(null);
            }

            // Check if new name already exists
            if (hyperPerms.getGroupManager().getGroup(newName) != null) {
                ctx.sender().sendMessage(Message.raw("A group with name " + newName + " already exists"));
                return CompletableFuture.completedFuture(null);
            }

            // Delete old group and create new one with same settings
            hyperPerms.getGroupManager().deleteGroup(oldName);
            
            // Create new group with new name but same properties
            Group newGroup = new Group(newName);
            newGroup.setDisplayName(group.getDisplayName());
            newGroup.setWeight(group.getWeight());
            newGroup.setPrefix(group.getPrefix());
            newGroup.setSuffix(group.getSuffix());
            newGroup.setPrefixPriority(group.getPrefixPriority());
            newGroup.setSuffixPriority(group.getSuffixPriority());
            
            // Copy nodes
            for (Node node : group.getNodes()) {
                newGroup.setNode(node);
            }
            
            // Copy parent groups
            for (String parent : group.getParentGroups()) {
                newGroup.addParent(parent);
            }

            hyperPerms.getGroupManager().createGroup(newName);
            hyperPerms.getGroupManager().saveGroup(newGroup);
            
            // Update all users that had the old group
            for (User user : hyperPerms.getUserManager().getLoadedUsers()) {
                if (user.getInheritedGroups().contains(oldName.toLowerCase())) {
                    user.removeGroup(oldName);
                    user.addGroup(newName);
                    hyperPerms.getUserManager().saveUser(user);
                }
            }
            
            // Update all groups that had the old group as parent
            for (Group g : hyperPerms.getGroupManager().getLoadedGroups()) {
                if (g.getParentGroups().contains(oldName.toLowerCase())) {
                    g.removeParent(oldName);
                    g.addParent(newName);
                    hyperPerms.getGroupManager().saveGroup(g);
                }
            }

            hyperPerms.getCache().invalidateAll();
            ctx.sender().sendMessage(Message.raw("Renamed group " + oldName + " to " + newName));
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
            addSubCommand(new UserSetPrefixSubCommand(hyperPerms));
            addSubCommand(new UserSetSuffixSubCommand(hyperPerms));
            addSubCommand(new UserClearSubCommand(hyperPerms));
            addSubCommand(new UserCloneSubCommand(hyperPerms));
        }

        @Override
        protected CompletableFuture<Void> execute(CommandContext ctx) {
            ctx.sender().sendMessage(Message.raw("Usage: /hp user <info|setperm|unsetperm|addgroup|removegroup|setprimarygroup|setprefix|setsuffix|clear|clone>"));
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
            ctx.sender().sendMessage(Message.raw("Custom Prefix: " + (user.getCustomPrefix() != null ? "\"" + user.getCustomPrefix() + "\"" : "(none)")));
            ctx.sender().sendMessage(Message.raw("Custom Suffix: " + (user.getCustomSuffix() != null ? "\"" + user.getCustomSuffix() + "\"" : "(none)")));

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

            // Use resolveOrCreateUser to support offline players (e.g., from Tebex)
            User user = resolveOrCreateUser(hyperPerms, identifier);
            if (user == null) {
                ctx.sender().sendMessage(Message.raw("User not found: " + identifier));
                ctx.sender().sendMessage(Message.raw("Tip: Use UUID for offline players (e.g., from Tebex)"));
                return CompletableFuture.completedFuture(null);
            }

            boolean value = valueStr == null || !valueStr.equalsIgnoreCase("false");

            Node node = Node.builder(permission).value(value).build();
            user.setNode(node);
            hyperPerms.getUserManager().saveUser(user).join();

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
                hyperPerms.getUserManager().saveUser(user).join();
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

            Group group = hyperPerms.getGroupManager().getGroup(groupName);
            if (group == null) {
                ctx.sender().sendMessage(Message.raw("Group not found: " + groupName));
                return CompletableFuture.completedFuture(null);
            }

            // Use resolveOrCreateUser to support offline players (e.g., from Tebex)
            User user = resolveOrCreateUser(hyperPerms, identifier);
            if (user == null) {
                ctx.sender().sendMessage(Message.raw("User not found: " + identifier));
                ctx.sender().sendMessage(Message.raw("Tip: Use UUID for offline players (e.g., from Tebex)"));
                return CompletableFuture.completedFuture(null);
            }

            var result = user.addGroup(groupName);
            if (result == com.hyperperms.api.PermissionHolder.DataMutateResult.SUCCESS) {
                hyperPerms.getUserManager().saveUser(user).join();
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
                hyperPerms.getUserManager().saveUser(user).join();
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

            Group group = hyperPerms.getGroupManager().getGroup(groupName);
            if (group == null) {
                ctx.sender().sendMessage(Message.raw("Group not found: " + groupName));
                return CompletableFuture.completedFuture(null);
            }

            // Use resolveOrCreateUser to support offline players (e.g., from Tebex)
            User user = resolveOrCreateUser(hyperPerms, identifier);
            if (user == null) {
                ctx.sender().sendMessage(Message.raw("User not found: " + identifier));
                ctx.sender().sendMessage(Message.raw("Tip: Use UUID for offline players (e.g., from Tebex)"));
                return CompletableFuture.completedFuture(null);
            }

            // Ensure user is in the group
            if (!user.getInheritedGroups().contains(groupName.toLowerCase())) {
                // Add them to the group automatically
                user.addGroup(groupName);
            }

            user.setPrimaryGroup(groupName.toLowerCase());
            hyperPerms.getUserManager().saveUser(user).join();
            hyperPerms.getCache().invalidate(user.getUuid());

            ctx.sender().sendMessage(Message.raw("Set primary group of " + user.getFriendlyName() + " to " + groupName));
            return CompletableFuture.completedFuture(null);
        }
    }


    private static class UserSetPrefixSubCommand extends AbstractCommand {
        private final HyperPerms hyperPerms;
        private final RequiredArg<String> playerArg;
        private final OptionalArg<String> prefixArg;

        UserSetPrefixSubCommand(HyperPerms hyperPerms) {
            super("setprefix", "Set a user's custom prefix");
            this.hyperPerms = hyperPerms;
            this.playerArg = withRequiredArg("player", "Player name or UUID", ArgTypes.STRING);
            this.prefixArg = withOptionalArg("prefix", "Prefix text (omit to clear)", ArgTypes.STRING);
        }

        @Override
        protected CompletableFuture<Void> execute(CommandContext ctx) {
            String identifier = ctx.get(playerArg);
            String prefix = ctx.get(prefixArg);

            User user = resolveUser(hyperPerms, identifier);
            if (user == null) {
                ctx.sender().sendMessage(Message.raw("User not found: " + identifier));
                return CompletableFuture.completedFuture(null);
            }

            if (prefix == null || prefix.isEmpty()) {
                user.setCustomPrefix(null);
                ctx.sender().sendMessage(Message.raw("Cleared custom prefix for " + user.getFriendlyName()));
            } else {
                user.setCustomPrefix(prefix);
                ctx.sender().sendMessage(Message.raw("Set custom prefix of " + user.getFriendlyName() + " to \"" + prefix + "\""));
            }

            hyperPerms.getUserManager().saveUser(user).join();
            hyperPerms.getCache().invalidate(user.getUuid());

            return CompletableFuture.completedFuture(null);
        }
    }

    private static class UserSetSuffixSubCommand extends AbstractCommand {
        private final HyperPerms hyperPerms;
        private final RequiredArg<String> playerArg;
        private final OptionalArg<String> suffixArg;

        UserSetSuffixSubCommand(HyperPerms hyperPerms) {
            super("setsuffix", "Set a user's custom suffix");
            this.hyperPerms = hyperPerms;
            this.playerArg = withRequiredArg("player", "Player name or UUID", ArgTypes.STRING);
            this.suffixArg = withOptionalArg("suffix", "Suffix text (omit to clear)", ArgTypes.STRING);
        }

        @Override
        protected CompletableFuture<Void> execute(CommandContext ctx) {
            String identifier = ctx.get(playerArg);
            String suffix = ctx.get(suffixArg);

            User user = resolveUser(hyperPerms, identifier);
            if (user == null) {
                ctx.sender().sendMessage(Message.raw("User not found: " + identifier));
                return CompletableFuture.completedFuture(null);
            }

            if (suffix == null || suffix.isEmpty()) {
                user.setCustomSuffix(null);
                ctx.sender().sendMessage(Message.raw("Cleared custom suffix for " + user.getFriendlyName()));
            } else {
                user.setCustomSuffix(suffix);
                ctx.sender().sendMessage(Message.raw("Set custom suffix of " + user.getFriendlyName() + " to \"" + suffix + "\""));
            }

            hyperPerms.getUserManager().saveUser(user).join();
            hyperPerms.getCache().invalidate(user.getUuid());

            return CompletableFuture.completedFuture(null);
        }
    }

    private static class UserClearSubCommand extends AbstractCommand {
        private final HyperPerms hyperPerms;
        private final RequiredArg<String> playerArg;

        UserClearSubCommand(HyperPerms hyperPerms) {
            super("clear", "Clear all data for a user");
            this.hyperPerms = hyperPerms;
            this.playerArg = withRequiredArg("player", "Player name or UUID", ArgTypes.STRING);
        }

        @Override
        protected CompletableFuture<Void> execute(CommandContext ctx) {
            String identifier = ctx.get(playerArg);

            User user = resolveUser(hyperPerms, identifier);
            if (user == null) {
                ctx.sender().sendMessage(Message.raw("User not found: " + identifier));
                return CompletableFuture.completedFuture(null);
            }

            // Clear all nodes
            user.getNodes().clear();
            
            // Clear groups (keep only default)
            user.getInheritedGroups().clear();
            user.setPrimaryGroup("default");
            
            // Clear custom prefix/suffix
            user.setCustomPrefix(null);
            user.setCustomSuffix(null);

            hyperPerms.getUserManager().saveUser(user).join();
            hyperPerms.getCache().invalidate(user.getUuid());

            ctx.sender().sendMessage(Message.raw("Cleared all data for " + user.getFriendlyName()));
            return CompletableFuture.completedFuture(null);
        }
    }

    private static class UserCloneSubCommand extends AbstractCommand {
        private final HyperPerms hyperPerms;
        private final RequiredArg<String> sourceArg;
        private final RequiredArg<String> targetArg;

        UserCloneSubCommand(HyperPerms hyperPerms) {
            super("clone", "Copy permissions from one user to another");
            this.hyperPerms = hyperPerms;
            this.sourceArg = withRequiredArg("source", "Source player name or UUID", ArgTypes.STRING);
            this.targetArg = withRequiredArg("target", "Target player name or UUID", ArgTypes.STRING);
        }

        @Override
        protected CompletableFuture<Void> execute(CommandContext ctx) {
            String sourceId = ctx.get(sourceArg);
            String targetId = ctx.get(targetArg);

            User source = resolveUser(hyperPerms, sourceId);
            if (source == null) {
                ctx.sender().sendMessage(Message.raw("Source user not found: " + sourceId));
                return CompletableFuture.completedFuture(null);
            }

            User target = resolveUser(hyperPerms, targetId);
            if (target == null) {
                ctx.sender().sendMessage(Message.raw("Target user not found: " + targetId));
                return CompletableFuture.completedFuture(null);
            }

            // Clear target data first
            target.getNodes().clear();
            target.getInheritedGroups().clear();
            
            // Copy nodes
            for (Node node : source.getNodes()) {
                target.setNode(node);
            }
            
            // Copy groups
            for (String group : source.getInheritedGroups()) {
                target.addGroup(group);
            }
            
            // Copy primary group
            target.setPrimaryGroup(source.getPrimaryGroup());
            
            // Copy custom prefix/suffix
            target.setCustomPrefix(source.getCustomPrefix());
            target.setCustomSuffix(source.getCustomSuffix());

            hyperPerms.getUserManager().saveUser(target);
            hyperPerms.getCache().invalidate(target.getUuid());

            ctx.sender().sendMessage(Message.raw("Cloned permissions from " + source.getFriendlyName() + " to " + target.getFriendlyName()));
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


    // ==================== Backup Subcommand ====================

    private static class BackupSubCommand extends AbstractCommand {
        private final HyperPerms hyperPerms;

        BackupSubCommand(HyperPerms hyperPerms) {
            super("backup", "Manage backups");
            this.hyperPerms = hyperPerms;
            addSubCommand(new BackupCreateSubCommand(hyperPerms));
            addSubCommand(new BackupListSubCommand(hyperPerms));
            addSubCommand(new BackupRestoreSubCommand(hyperPerms));
        }

        @Override
        protected CompletableFuture<Void> execute(CommandContext ctx) {
            ctx.sender().sendMessage(Message.raw("Usage: /hp backup <create|list|restore>"));
            return CompletableFuture.completedFuture(null);
        }
    }

    private static class BackupCreateSubCommand extends AbstractCommand {
        private final HyperPerms hyperPerms;

        BackupCreateSubCommand(HyperPerms hyperPerms) {
            super("create", "Create a manual backup");
            this.hyperPerms = hyperPerms;
        }

        @Override
        protected CompletableFuture<Void> execute(CommandContext ctx) {
            var backupManager = hyperPerms.getBackupManager();
            if (backupManager == null) {
                ctx.sender().sendMessage(Message.raw("Backup manager not available"));
                return CompletableFuture.completedFuture(null);
            }

            ctx.sender().sendMessage(Message.raw("Creating backup..."));
            
            return backupManager.createBackup("manual")
                .thenAccept(backupName -> {
                    if (backupName != null) {
                        ctx.sender().sendMessage(Message.raw("Backup created: " + backupName));
                    } else {
                        ctx.sender().sendMessage(Message.raw("Failed to create backup"));
                    }
                })
                .exceptionally(e -> {
                    ctx.sender().sendMessage(Message.raw("Error creating backup: " + e.getMessage()));
                    return null;
                });
        }
    }

    private static class BackupListSubCommand extends AbstractCommand {
        private final HyperPerms hyperPerms;

        BackupListSubCommand(HyperPerms hyperPerms) {
            super("list", "List available backups");
            this.hyperPerms = hyperPerms;
        }

        @Override
        protected CompletableFuture<Void> execute(CommandContext ctx) {
            var backupManager = hyperPerms.getBackupManager();
            if (backupManager == null) {
                ctx.sender().sendMessage(Message.raw("Backup manager not available"));
                return CompletableFuture.completedFuture(null);
            }

            return backupManager.listBackups()
                .thenAccept(backups -> {
                    if (backups.isEmpty()) {
                        ctx.sender().sendMessage(Message.raw("No backups found"));
                        return;
                    }
                    
                    ctx.sender().sendMessage(Message.raw("=== Backups (" + backups.size() + ") ==="));
                    for (String backup : backups) {
                        ctx.sender().sendMessage(Message.raw("- " + backup));
                    }
                })
                .exceptionally(e -> {
                    ctx.sender().sendMessage(Message.raw("Error listing backups: " + e.getMessage()));
                    return null;
                });
        }
    }

    private static class BackupRestoreSubCommand extends AbstractCommand {
        private final HyperPerms hyperPerms;
        private final RequiredArg<String> backupArg;

        BackupRestoreSubCommand(HyperPerms hyperPerms) {
            super("restore", "Restore from a backup");
            this.hyperPerms = hyperPerms;
            this.backupArg = withRequiredArg("backup", "Backup file name", ArgTypes.STRING);
        }

        @Override
        protected CompletableFuture<Void> execute(CommandContext ctx) {
            String backupName = ctx.get(backupArg);
            
            var backupManager = hyperPerms.getBackupManager();
            if (backupManager == null) {
                ctx.sender().sendMessage(Message.raw("Backup manager not available"));
                return CompletableFuture.completedFuture(null);
            }

            ctx.sender().sendMessage(Message.raw("Restoring from backup: " + backupName));
            ctx.sender().sendMessage(Message.raw("WARNING: This will overwrite current data!"));
            
            return backupManager.restoreBackup(backupName)
                .thenAccept(success -> {
                    if (success) {
                        ctx.sender().sendMessage(Message.raw("Backup restored successfully!"));
                        ctx.sender().sendMessage(Message.raw("Please run /hp reload to apply changes"));
                    } else {
                        ctx.sender().sendMessage(Message.raw("Failed to restore backup"));
                    }
                })
                .exceptionally(e -> {
                    ctx.sender().sendMessage(Message.raw("Error restoring backup: " + e.getMessage()));
                    return null;
                });
        }
    }

    // ==================== Export/Import Subcommands ====================

    private static class ExportSubCommand extends AbstractCommand {
        private final HyperPerms hyperPerms;
        private final OptionalArg<String> filenameArg;

        ExportSubCommand(HyperPerms hyperPerms) {
            super("export", "Export all data to a file");
            this.hyperPerms = hyperPerms;
            this.filenameArg = withOptionalArg("filename", "Export file name", ArgTypes.STRING);
        }

        @Override
        protected CompletableFuture<Void> execute(CommandContext ctx) {
            String filename = ctx.get(filenameArg);
            if (filename == null || filename.isEmpty()) {
                filename = "hyperperms-export-" + System.currentTimeMillis() + ".json";
            }

            ctx.sender().sendMessage(Message.raw("Exporting data to: " + filename));
            
            // Export is essentially a backup with a custom name
            var backupManager = hyperPerms.getBackupManager();
            if (backupManager == null) {
                ctx.sender().sendMessage(Message.raw("Backup/export not available"));
                return CompletableFuture.completedFuture(null);
            }

            return backupManager.createBackup("export")
                .thenAccept(backupName -> {
                    if (backupName != null) {
                        ctx.sender().sendMessage(Message.raw("Data exported to: " + backupName));
                    } else {
                        ctx.sender().sendMessage(Message.raw("Failed to export data"));
                    }
                })
                .exceptionally(e -> {
                    ctx.sender().sendMessage(Message.raw("Error exporting: " + e.getMessage()));
                    return null;
                });
        }
    }

    private static class ImportSubCommand extends AbstractCommand {
        private final HyperPerms hyperPerms;

        ImportSubCommand(HyperPerms hyperPerms) {
            super("import", "Import data from file or create defaults");
            this.hyperPerms = hyperPerms;
            addSubCommand(new ImportDefaultsSubCommand(hyperPerms));
            addSubCommand(new ImportFileSubCommand(hyperPerms));
        }

        @Override
        protected CompletableFuture<Void> execute(CommandContext ctx) {
            ctx.sender().sendMessage(Message.raw("Usage:"));
            ctx.sender().sendMessage(Message.raw("  /hp import defaults - Create default group hierarchy"));
            ctx.sender().sendMessage(Message.raw("  /hp import file <filename> - Import from backup file"));
            return CompletableFuture.completedFuture(null);
        }
    }

    private static class ImportDefaultsSubCommand extends AbstractCommand {
        private final HyperPerms hyperPerms;

        ImportDefaultsSubCommand(HyperPerms hyperPerms) {
            super("defaults", "Create default group hierarchy");
            this.hyperPerms = hyperPerms;
        }

        @Override
        protected CompletableFuture<Void> execute(CommandContext ctx) {
            ctx.sender().sendMessage(Message.raw("Creating default groups..."));

            var groupManager = hyperPerms.getGroupManager();

            // Check if groups already exist
            int existingCount = 0;
            String[] defaultGroups = {"default", "member", "builder", "moderator", "admin", "owner"};
            for (String name : defaultGroups) {
                if (groupManager.getGroup(name) != null) {
                    existingCount++;
                }
            }

            if (existingCount > 0) {
                ctx.sender().sendMessage(Message.raw("WARNING: " + existingCount + " default groups already exist."));
                ctx.sender().sendMessage(Message.raw("Existing groups will be updated, not replaced."));
            }

            // Create or update default group
            Group defaultGroup = getOrCreateGroup("default");
            defaultGroup.setWeight(0);
            defaultGroup.setDisplayName("Default");
            defaultGroup.setNode(Node.builder("hyperperms.command.check.self").value(true).build());
            groupManager.saveGroup(defaultGroup);

            // Create member group
            Group memberGroup = getOrCreateGroup("member");
            memberGroup.setWeight(10);
            memberGroup.setDisplayName("Member");
            memberGroup.addParent("default");
            groupManager.saveGroup(memberGroup);

            // Create builder group
            Group builderGroup = getOrCreateGroup("builder");
            builderGroup.setWeight(20);
            builderGroup.setDisplayName("Builder");
            builderGroup.setPrefix("&2[Builder] ");
            builderGroup.addParent("member");
            builderGroup.setNode(Node.builder("hytale.editor.builderTools").value(true).build());
            builderGroup.setNode(Node.builder("hytale.editor.brush.use").value(true).build());
            builderGroup.setNode(Node.builder("hytale.editor.selection.use").value(true).build());
            builderGroup.setNode(Node.builder("hytale.editor.prefab.use").value(true).build());
            builderGroup.setNode(Node.builder("hytale.editor.history").value(true).build());
            builderGroup.setNode(Node.builder("hytale.camera.flycam").value(true).build());
            groupManager.saveGroup(builderGroup);

            // Create moderator group
            Group modGroup = getOrCreateGroup("moderator");
            modGroup.setWeight(50);
            modGroup.setDisplayName("Moderator");
            modGroup.setPrefix("&9[Mod] ");
            modGroup.addParent("builder");
            modGroup.setNode(Node.builder("hyperperms.command.user.info").value(true).build());
            modGroup.setNode(Node.builder("hyperperms.command.check.others").value(true).build());
            modGroup.setNode(Node.builder("hytale.command.kick").value(true).build());
            modGroup.setNode(Node.builder("hytale.command.ban").value(true).build());
            modGroup.setNode(Node.builder("hytale.command.unban").value(true).build());
            modGroup.setNode(Node.builder("hytale.command.tp.self").value(true).build());
            modGroup.setNode(Node.builder("hytale.command.tp.others").value(true).build());
            modGroup.setNode(Node.builder("hytale.command.inventory.see").value(true).build());
            modGroup.setNode(Node.builder("hytale.command.who").value(true).build());
            groupManager.saveGroup(modGroup);

            // Create admin group
            Group adminGroup = getOrCreateGroup("admin");
            adminGroup.setWeight(90);
            adminGroup.setDisplayName("Admin");
            adminGroup.setPrefix("&c[Admin] ");
            adminGroup.addParent("moderator");
            adminGroup.setNode(Node.builder("hyperperms.command.*").value(true).build());
            adminGroup.setNode(Node.builder("hytale.command.*").value(true).build());
            adminGroup.setNode(Node.builder("hytale.editor.*").value(true).build());
            groupManager.saveGroup(adminGroup);

            // Create owner group
            Group ownerGroup = getOrCreateGroup("owner");
            ownerGroup.setWeight(100);
            ownerGroup.setDisplayName("Owner");
            ownerGroup.setPrefix("&4[Owner] ");
            ownerGroup.setNode(Node.builder("*").value(true).build());
            groupManager.saveGroup(ownerGroup);

            // Invalidate all caches
            hyperPerms.getCache().invalidateAll();

            ctx.sender().sendMessage(Message.raw(""));
            ctx.sender().sendMessage(Message.raw("Default groups created:"));
            ctx.sender().sendMessage(Message.raw("  default (weight 0) - Base permissions"));
            ctx.sender().sendMessage(Message.raw("  member (weight 10) - Trusted players"));
            ctx.sender().sendMessage(Message.raw("  builder (weight 20) - Building/editor tools"));
            ctx.sender().sendMessage(Message.raw("  moderator (weight 50) - Player management"));
            ctx.sender().sendMessage(Message.raw("  admin (weight 90) - Full command access"));
            ctx.sender().sendMessage(Message.raw("  owner (weight 100) - Full server access (*)"));
            ctx.sender().sendMessage(Message.raw(""));
            ctx.sender().sendMessage(Message.raw("Use /hp user <player> addgroup <group> to assign players"));

            return CompletableFuture.completedFuture(null);
        }

        private Group getOrCreateGroup(String name) {
            var groupManager = hyperPerms.getGroupManager();
            Group group = groupManager.getGroup(name);
            if (group == null) {
                groupManager.createGroup(name);
                group = groupManager.getGroup(name);
            }
            return group;
        }
    }

    private static class ImportFileSubCommand extends AbstractCommand {
        private final HyperPerms hyperPerms;
        private final RequiredArg<String> filenameArg;

        ImportFileSubCommand(HyperPerms hyperPerms) {
            super("file", "Import data from a backup file");
            this.hyperPerms = hyperPerms;
            this.filenameArg = withRequiredArg("filename", "Import file name", ArgTypes.STRING);
        }

        @Override
        protected CompletableFuture<Void> execute(CommandContext ctx) {
            String filename = ctx.get(filenameArg);

            var backupManager = hyperPerms.getBackupManager();
            if (backupManager == null) {
                ctx.sender().sendMessage(Message.raw("Backup/import not available"));
                return CompletableFuture.completedFuture(null);
            }

            ctx.sender().sendMessage(Message.raw("Importing data from: " + filename));
            ctx.sender().sendMessage(Message.raw("WARNING: This will merge with or overwrite current data!"));

            return backupManager.restoreBackup(filename)
                .thenAccept(success -> {
                    if (success) {
                        ctx.sender().sendMessage(Message.raw("Data imported successfully!"));
                        ctx.sender().sendMessage(Message.raw("Please run /hp reload to apply changes"));
                    } else {
                        ctx.sender().sendMessage(Message.raw("Failed to import data. Check if file exists."));
                    }
                })
                .exceptionally(e -> {
                    ctx.sender().sendMessage(Message.raw("Error importing: " + e.getMessage()));
                    return null;
                });
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

    private static class ResetGroupsSubCommand extends AbstractCommand {
        private final HyperPerms hyperPerms;

        ResetGroupsSubCommand(HyperPerms hyperPerms) {
            super("resetgroups", "Reset all groups to default");
            this.hyperPerms = hyperPerms;
            addSubCommand(new ResetGroupsConfirmSubCommand(hyperPerms));
        }

        @Override
        protected CompletableFuture<Void> execute(CommandContext ctx) {
            ctx.sender().sendMessage(Message.raw(""));
            ctx.sender().sendMessage(Message.raw("=== WARNING ===").color(java.awt.Color.RED));
            ctx.sender().sendMessage(Message.raw("This will DELETE all existing groups and replace them with defaults."));
            ctx.sender().sendMessage(Message.raw("User group memberships will be preserved, but custom group settings will be lost."));
            ctx.sender().sendMessage(Message.raw(""));
            ctx.sender().sendMessage(Message.raw("To confirm, run: /hp resetgroups confirm"));
            ctx.sender().sendMessage(Message.raw(""));
            return CompletableFuture.completedFuture(null);
        }
    }

    private static class ResetGroupsConfirmSubCommand extends AbstractCommand {
        private final HyperPerms hyperPerms;

        ResetGroupsConfirmSubCommand(HyperPerms hyperPerms) {
            super("confirm", "Confirm resetting groups to default");
            this.hyperPerms = hyperPerms;
        }

        @Override
        protected CompletableFuture<Void> execute(CommandContext ctx) {

            ctx.sender().sendMessage(Message.raw("Resetting groups to defaults..."));

            try (var inputStream = getClass().getClassLoader().getResourceAsStream("default-groups.json")) {
                if (inputStream == null) {
                    ctx.sender().sendMessage(Message.raw("&cError: default-groups.json not found in plugin resources"));
                    return CompletableFuture.completedFuture(null);
                }

                String json = new String(inputStream.readAllBytes(), java.nio.charset.StandardCharsets.UTF_8);
                com.google.gson.JsonObject root = com.google.gson.JsonParser.parseString(json).getAsJsonObject();
                com.google.gson.JsonObject groups = root.getAsJsonObject("groups");

                if (groups == null) {
                    ctx.sender().sendMessage(Message.raw("&cError: No 'groups' object found in default-groups.json"));
                    return CompletableFuture.completedFuture(null);
                }

                int updated = 0;
                for (var entry : groups.entrySet()) {
                    String groupName = entry.getKey();
                    com.google.gson.JsonObject groupData = entry.getValue().getAsJsonObject();

                    // Get or create the group
                    Group group = hyperPerms.getGroupManager().getGroup(groupName);
                    if (group == null) {
                        group = hyperPerms.getGroupManager().createGroup(groupName);
                    } else {
                        // Clear existing permissions and parents (both stored as nodes)
                        group.clearNodes();
                    }

                    // Set weight
                    if (groupData.has("weight")) {
                        group.setWeight(groupData.get("weight").getAsInt());
                    }

                    // Set prefix
                    if (groupData.has("prefix")) {
                        group.setPrefix(groupData.get("prefix").getAsString());
                    }

                    // Set suffix
                    if (groupData.has("suffix")) {
                        group.setSuffix(groupData.get("suffix").getAsString());
                    }

                    // Add permissions
                    if (groupData.has("permissions")) {
                        for (var perm : groupData.getAsJsonArray("permissions")) {
                            group.addNode(Node.builder(perm.getAsString()).build());
                        }
                    }

                    // Add parent groups
                    if (groupData.has("parents")) {
                        for (var parent : groupData.getAsJsonArray("parents")) {
                            group.addParent(parent.getAsString());
                        }
                    }

                    // Save the group
                    hyperPerms.getGroupManager().saveGroup(group).join();
                    updated++;
                }

                // Invalidate all caches
                hyperPerms.getCacheInvalidator().invalidateAll();

                ctx.sender().sendMessage(Message.raw(""));
                ctx.sender().sendMessage(Message.raw("Success! Reset " + updated + " groups to defaults.").color(java.awt.Color.GREEN));
                ctx.sender().sendMessage(Message.raw("All permission caches have been invalidated."));
                ctx.sender().sendMessage(Message.raw(""));

            } catch (Exception e) {
                ctx.sender().sendMessage(Message.raw("&cError resetting groups: " + e.getMessage()));
            }

            return CompletableFuture.completedFuture(null);
        }
    }


    // ==================== Debug Subcommands ====================

    private static class DebugSubCommand extends AbstractCommand {
        DebugSubCommand(HyperPerms hyperPerms) {
            super("debug", "Debug commands for troubleshooting");
            addSubCommand(new DebugTreeSubCommand(hyperPerms));
            addSubCommand(new DebugResolveSubCommand(hyperPerms));
            addSubCommand(new DebugContextsSubCommand(hyperPerms));
            addSubCommand(new DebugPermsSubCommand());
        }

        @Override
        protected CompletableFuture<Void> execute(CommandContext ctx) {
            ctx.sender().sendMessage(Message.raw("Debug subcommands:"));
            ctx.sender().sendMessage(Message.raw("  /hp debug tree <user> - Show inheritance tree"));
            ctx.sender().sendMessage(Message.raw("  /hp debug resolve <user> <permission> - Show permission resolution"));
            ctx.sender().sendMessage(Message.raw("  /hp debug contexts <user> - Show current contexts"));
            ctx.sender().sendMessage(Message.raw("  /hp debug perms - Toggle verbose permission check logging"));
            return CompletableFuture.completedFuture(null);
        }
    }

    private static class DebugPermsSubCommand extends AbstractCommand {
        DebugPermsSubCommand() {
            super("perms", "Toggle verbose permission check logging");
        }

        @Override
        protected CompletableFuture<Void> execute(CommandContext ctx) {
            boolean currentState = com.hyperperms.util.Logger.isPermissionDebugEnabled();
            com.hyperperms.util.Logger.setPermissionDebugEnabled(!currentState);

            if (!currentState) {
                ctx.sender().sendMessage(Message.raw("§aPermission debug logging ENABLED"));
                ctx.sender().sendMessage(Message.raw("§7All permission checks will now be logged to console with detailed info."));
                ctx.sender().sendMessage(Message.raw("§7This helps debug issues between plugins like HyperHomes."));
                ctx.sender().sendMessage(Message.raw("§7Run §e/hp debug perms§7 again to disable."));
            } else {
                ctx.sender().sendMessage(Message.raw("§cPermission debug logging DISABLED"));
            }
            return CompletableFuture.completedFuture(null);
        }
    }

    private static class DebugTreeSubCommand extends AbstractCommand {
        private final HyperPerms hyperPerms;
        private final RequiredArg<String> userArg;

        DebugTreeSubCommand(HyperPerms hyperPerms) {
            super("tree", "Show inheritance tree for a user");
            this.hyperPerms = hyperPerms;
            this.userArg = withRequiredArg("user", "Player name or UUID", ArgTypes.STRING);
        }

        @Override
        protected CompletableFuture<Void> execute(CommandContext ctx) {
            String identifier = ctx.get(userArg);
            User user = resolveUser(hyperPerms, identifier);
            
            if (user == null) {
                ctx.sender().sendMessage(Message.raw("User not found: " + identifier));
                return CompletableFuture.completedFuture(null);
            }

            ctx.sender().sendMessage(Message.raw("=== Inheritance Tree for " + user.getFriendlyName() + " ==="));
            
            // Direct permissions
            ctx.sender().sendMessage(Message.raw("Direct Permissions:"));
            var directPerms = user.getNodes();
            if (directPerms.isEmpty()) {
                ctx.sender().sendMessage(Message.raw("  (none)"));
            } else {
                for (var node : directPerms) {
                    String prefix = node.getValue() ? "  + " : "  - ";
                    ctx.sender().sendMessage(Message.raw(prefix + node.getPermission() + formatContext(node)));
                }
            }

            // Groups
            ctx.sender().sendMessage(Message.raw("Groups:"));
            var groups = user.getInheritedGroups();
            if (groups.isEmpty()) {
                ctx.sender().sendMessage(Message.raw("  (none)"));
            } else {
                for (String groupName : groups) {
                    printGroupTree(ctx, hyperPerms, groupName, "  ", new java.util.HashSet<>());
                }
            }

            return CompletableFuture.completedFuture(null);
        }

        private void printGroupTree(CommandContext ctx, HyperPerms hyperPerms, String groupName, 
                                    String indent, java.util.Set<String> visited) {
            if (visited.contains(groupName)) {
                ctx.sender().sendMessage(Message.raw(indent + "[" + groupName + "] (circular ref)"));
                return;
            }
            visited.add(groupName);

            var group = hyperPerms.getGroupManager().getGroup(groupName);
            if (group == null) {
                ctx.sender().sendMessage(Message.raw(indent + "[" + groupName + "] (not found)"));
                return;
            }

            ctx.sender().sendMessage(Message.raw(indent + "[" + groupName + "] (weight=" + group.getWeight() + ")"));
            
            // Show permissions
            for (var node : group.getNodes()) {
                String prefix = node.getValue() ? indent + "  + " : indent + "  - ";
                ctx.sender().sendMessage(Message.raw(prefix + node.getPermission() + formatContext(node)));
            }

            // Show parent groups
            for (String parent : group.getParentGroups()) {
                printGroupTree(ctx, hyperPerms, parent, indent + "  ", visited);
            }
        }

        private String formatContext(com.hyperperms.model.Node node) {
            var contexts = node.getContexts();
            if (contexts == null || contexts.isEmpty()) {
                return "";
            }
            StringBuilder sb = new StringBuilder(" [");
            boolean first = true;
            for (var ctx : contexts.toSet()) {
                if (!first) sb.append(", ");
                sb.append(ctx.key()).append("=").append(ctx.value());
                first = false;
            }
            sb.append("]");
            return sb.toString();
        }
    }

    private static class DebugResolveSubCommand extends AbstractCommand {
        private final HyperPerms hyperPerms;
        private final RequiredArg<String> userArg;
        private final RequiredArg<String> permArg;

        DebugResolveSubCommand(HyperPerms hyperPerms) {
            super("resolve", "Debug permission resolution step-by-step");
            this.hyperPerms = hyperPerms;
            this.userArg = withRequiredArg("user", "Player name or UUID", ArgTypes.STRING);
            this.permArg = withRequiredArg("permission", "Permission to resolve", ArgTypes.STRING);
        }

        @Override
        protected CompletableFuture<Void> execute(CommandContext ctx) {
            String identifier = ctx.get(userArg);
            String permission = ctx.get(permArg);
            
            User user = resolveUser(hyperPerms, identifier);
            if (user == null) {
                ctx.sender().sendMessage(Message.raw("User not found: " + identifier));
                return CompletableFuture.completedFuture(null);
            }

            ctx.sender().sendMessage(Message.raw("=== Resolving: " + permission + " for " + user.getFriendlyName() + " ==="));

            // Get current contexts
            var contexts = hyperPerms.getContexts(user.getUuid());
            ctx.sender().sendMessage(Message.raw("Current contexts: " + contexts));

            // Enable verbose mode temporarily to capture the trace
            boolean wasVerbose = hyperPerms.isVerboseMode();
            hyperPerms.setVerboseMode(true);

            boolean result = hyperPerms.hasPermission(user.getUuid(), permission, contexts);
            
            hyperPerms.setVerboseMode(wasVerbose);

            ctx.sender().sendMessage(Message.raw(""));
            ctx.sender().sendMessage(Message.raw("Final result: " + (result ? "ALLOWED" : "DENIED")));
            ctx.sender().sendMessage(Message.raw("(Check console for detailed trace if verbose logging is enabled)"));

            return CompletableFuture.completedFuture(null);
        }
    }

    private static class DebugContextsSubCommand extends AbstractCommand {
        private final HyperPerms hyperPerms;
        private final RequiredArg<String> userArg;

        DebugContextsSubCommand(HyperPerms hyperPerms) {
            super("contexts", "Show all current contexts for a user");
            this.hyperPerms = hyperPerms;
            this.userArg = withRequiredArg("user", "Player name or UUID", ArgTypes.STRING);
        }

        @Override
        protected CompletableFuture<Void> execute(CommandContext ctx) {
            String identifier = ctx.get(userArg);
            User user = resolveUser(hyperPerms, identifier);
            
            if (user == null) {
                ctx.sender().sendMessage(Message.raw("User not found: " + identifier));
                return CompletableFuture.completedFuture(null);
            }

            var contexts = hyperPerms.getContexts(user.getUuid());
            
            ctx.sender().sendMessage(Message.raw("=== Contexts for " + user.getFriendlyName() + " ==="));
            
            if (contexts.isEmpty()) {
                ctx.sender().sendMessage(Message.raw("  (no contexts)"));
            } else {
                for (var context : contexts.toSet()) {
                    ctx.sender().sendMessage(Message.raw("  " + context.key() + " = " + context.value()));
                }
            }

            ctx.sender().sendMessage(Message.raw(""));
            ctx.sender().sendMessage(Message.raw("Registered calculators: " + hyperPerms.getContextManager().getCalculatorCount()));

            return CompletableFuture.completedFuture(null);
        }
    }

    // ==================== Permission List Subcommands ====================

    private static class PermsSubCommand extends AbstractCommand {
        PermsSubCommand(HyperPerms hyperPerms) {
            super("perms", "Permission listing and search");
            addSubCommand(new PermsListSubCommand(hyperPerms));
            addSubCommand(new PermsSearchSubCommand(hyperPerms));
        }

        @Override
        protected CompletableFuture<Void> execute(CommandContext ctx) {
            ctx.sender().sendMessage(Message.raw("Permission subcommands:"));
            ctx.sender().sendMessage(Message.raw("  /hp perms list [category] - List registered permissions"));
            ctx.sender().sendMessage(Message.raw("  /hp perms search <query> - Search permissions"));
            return CompletableFuture.completedFuture(null);
        }
    }

    private static class PermsListSubCommand extends AbstractCommand {
        private final HyperPerms hyperPerms;
        private final OptionalArg<String> categoryArg;

        PermsListSubCommand(HyperPerms hyperPerms) {
            super("list", "List registered permissions");
            this.hyperPerms = hyperPerms;
            this.categoryArg = withOptionalArg("category", "Filter by category", ArgTypes.STRING);
        }

        @Override
        protected CompletableFuture<Void> execute(CommandContext ctx) {
            var registry = hyperPerms.getPermissionRegistry();
            String category = ctx.get(categoryArg);

            if (category != null) {
                // List by category
                var perms = registry.getByCategory(category);
                if (perms.isEmpty()) {
                    ctx.sender().sendMessage(Message.raw("No permissions found in category: " + category));
                    ctx.sender().sendMessage(Message.raw("Available categories: " + String.join(", ", registry.getCategories())));
                    return CompletableFuture.completedFuture(null);
                }

                ctx.sender().sendMessage(Message.raw("=== Permissions in category '" + category + "' (" + perms.size() + ") ==="));
                for (var perm : perms) {
                    ctx.sender().sendMessage(Message.raw("  " + perm.getPermission()));
                    ctx.sender().sendMessage(Message.raw("    " + perm.getDescription()));
                }
            } else {
                // List categories
                var categories = registry.getCategories();
                ctx.sender().sendMessage(Message.raw("=== Permission Categories (" + registry.size() + " total permissions) ==="));
                for (String cat : categories) {
                    int count = registry.getByCategory(cat).size();
                    ctx.sender().sendMessage(Message.raw("  " + cat + " (" + count + " permissions)"));
                }
                ctx.sender().sendMessage(Message.raw(""));
                ctx.sender().sendMessage(Message.raw("Use /hp perms list <category> to view permissions in a category"));
            }

            return CompletableFuture.completedFuture(null);
        }
    }

    private static class PermsSearchSubCommand extends AbstractCommand {
        private final HyperPerms hyperPerms;
        private final RequiredArg<String> queryArg;

        PermsSearchSubCommand(HyperPerms hyperPerms) {
            super("search", "Search for permissions by name or description");
            this.hyperPerms = hyperPerms;
            this.queryArg = withRequiredArg("query", "Search query", ArgTypes.STRING);
        }

        @Override
        protected CompletableFuture<Void> execute(CommandContext ctx) {
            String query = ctx.get(queryArg);
            var registry = hyperPerms.getPermissionRegistry();

            var results = registry.search(query);
            
            if (results.isEmpty()) {
                ctx.sender().sendMessage(Message.raw("No permissions found matching: " + query));
                return CompletableFuture.completedFuture(null);
            }

            ctx.sender().sendMessage(Message.raw("=== Search results for '" + query + "' (" + results.size() + " found) ==="));
            
            int shown = 0;
            for (var perm : results) {
                if (shown >= 20) {
                    ctx.sender().sendMessage(Message.raw("... and " + (results.size() - 20) + " more"));
                    break;
                }
                ctx.sender().sendMessage(Message.raw("  " + perm.getPermission() + " [" + perm.getCategory() + "]"));
                ctx.sender().sendMessage(Message.raw("    " + perm.getDescription()));
                shown++;
            }

            return CompletableFuture.completedFuture(null);
        }
    }
}
