package com.hyperperms.integration;

import com.hyperperms.HyperPerms;
import com.hyperperms.api.context.ContextSet;
import com.hyperperms.manager.UserManagerImpl;
import com.hyperperms.api.PermissionHolder.DataMutateResult;
import com.hyperperms.model.Group;
import com.hyperperms.model.Node;
import com.hyperperms.model.User;
import com.hyperperms.resolver.PermissionResolver;
import com.hyperperms.resolver.WildcardMatcher;
import net.milkbowl.vault2.helper.TriState;
import net.milkbowl.vault2.helper.context.Context;
import net.milkbowl.vault2.helper.subject.Subject;
import net.milkbowl.vault2.helper.subject.SubjectType;
import net.milkbowl.vault2.permission.PermissionUnlocked;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/**
 * VaultUnlocked permission provider implementation for HyperPerms.
 * <p>
 * This class bridges HyperPerms permission system to VaultUnlocked API,
 * allowing other plugins to query permissions through Vault.
 */
public class HyperPermsVaultProvider implements PermissionUnlocked {

    private final HyperPerms hyperPerms;

    public HyperPermsVaultProvider(@NotNull HyperPerms hyperPerms) {
        this.hyperPerms = hyperPerms;
    }

    // ==================== Core Provider Info ====================

    @Override
    @NotNull
    public String getName() {
        return "HyperPerms";
    }

    @Override
    public boolean isEnabled() {
        return hyperPerms.isEnabled();
    }

    @Override
    public boolean hasGroupSupport() {
        return true;
    }

    @Override
    public boolean hasSuperPermsSupport() {
        return false;
    }

    // ==================== Permission Checks ====================

    @Override
    @NotNull
    public TriState has(@Nullable Context context, @NotNull Subject subject, @NotNull String permission) {
        // Handle group permission checks
        if (subject.type() == SubjectType.GROUP) {
            return groupHas(context, subject.identifier(), permission);
        }

        // Get UUID from subject
        UUID uuid = subject.asUUID();
        if (uuid == null) {
            return TriState.UNDEFINED;
        }

        // Convert Vault context to HyperPerms ContextSet
        ContextSet contexts = convertContext(context);

        // Get user from manager
        UserManagerImpl userManager = (UserManagerImpl) hyperPerms.getUserManager();
        User user = userManager.getUser(uuid);
        if (user == null) {
            // Try to load the user
            var loadResult = userManager.loadUser(uuid).join();
            if (loadResult.isPresent()) {
                user = loadResult.get();
            } else {
                user = userManager.getOrCreateUser(uuid);
            }
        }

        // Resolve permission using HyperPerms resolver
        PermissionResolver resolver = hyperPerms.getResolver();
        var resolved = resolver.resolve(user, contexts);
        WildcardMatcher.TriState result = resolved.check(permission);

        return toVaultTriState(result);
    }

    @Override
    @NotNull
    public CompletableFuture<TriState> hasAsync(@Nullable Context context, @NotNull Subject subject, @NotNull String permission) {
        return CompletableFuture.supplyAsync(() -> has(context, subject, permission));
    }

    // ==================== Permission Modification ====================

    @Override
    public boolean setPermission(@Nullable Context context, @NotNull Subject subject, @NotNull String permission, @NotNull TriState value) {
        if (subject.type() == SubjectType.GROUP) {
            return groupSetPermission(context, subject.identifier(), permission, value);
        }

        UUID uuid = subject.asUUID();
        if (uuid == null) {
            return false;
        }

        UserManagerImpl userManager = (UserManagerImpl) hyperPerms.getUserManager();
        User user = userManager.getUser(uuid);
        if (user == null) {
            user = userManager.getOrCreateUser(uuid);
        }

        ContextSet contexts = convertContext(context);

        if (value == TriState.UNDEFINED) {
            user.removeNode(permission);
        } else {
            Node node = Node.builder(permission)
                .value(value == TriState.TRUE)
                .contexts(contexts)
                .build();
            user.setNode(node);
        }

        userManager.saveUser(user).join();
        hyperPerms.getCacheInvalidator().invalidateUser(uuid);
        return true;
    }

    @Override
    @NotNull
    public CompletableFuture<Boolean> setPermissionAsync(@Nullable Context context, @NotNull Subject subject, @NotNull String permission, @NotNull TriState value) {
        return CompletableFuture.supplyAsync(() -> setPermission(context, subject, permission, value));
    }

    @Override
    public boolean setTransientPermission(@Nullable Context context, @NotNull Subject subject, @NotNull String permission, @NotNull TriState value) {
        throw new UnsupportedOperationException("Transient permission modification through Vault is not yet supported. Use HyperPerms commands instead.");
    }

    @Override
    @NotNull
    public CompletableFuture<Boolean> setTransientPermissionAsync(@Nullable Context context, @NotNull Subject subject, @NotNull String permission, @NotNull TriState value) {
        return CompletableFuture.supplyAsync(() -> setTransientPermission(context, subject, permission, value));
    }

    // ==================== Permission Copying ====================

    @Override
    public boolean copyPermissions(@Nullable Context context, @NotNull Subject source, @NotNull Subject target, boolean replace) {
        if (source.type() != target.type()) {
            return false; // Can't copy between different subject types
        }

        if (source.type() == SubjectType.GROUP) {
            Group fromGroup = hyperPerms.getGroupManager().getGroup(source.identifier());
            Group toGroup = hyperPerms.getGroupManager().getGroup(target.identifier());
            if (fromGroup == null || toGroup == null) {
                return false;
            }

            if (replace) {
                toGroup.clearNodes();
            }

            for (Node node : fromGroup.getNodes()) {
                if (!node.isGroupNode()) {
                    toGroup.addNode(node);
                }
            }

            hyperPerms.getGroupManager().saveGroup(toGroup).join();
            // Cache invalidation for groups is handled by GroupManagerImpl.saveGroup()
            return true;
        }

        // Player to player
        UUID fromUuid = source.asUUID();
        UUID toUuid = target.asUUID();
        if (fromUuid == null || toUuid == null) {
            return false;
        }

        UserManagerImpl userManager = (UserManagerImpl) hyperPerms.getUserManager();
        User fromUser = userManager.getUser(fromUuid);
        User toUser = userManager.getUser(toUuid);

        if (fromUser == null) {
            return false;
        }
        if (toUser == null) {
            toUser = userManager.getOrCreateUser(toUuid);
        }

        if (replace) {
            toUser.clearNodes();
        }

        for (Node node : fromUser.getNodes()) {
            if (!node.isGroupNode()) {
                toUser.addNode(node);
            }
        }

        userManager.saveUser(toUser).join();
        hyperPerms.getCacheInvalidator().invalidateUser(toUuid);
        return true;
    }

    @Override
    @NotNull
    public CompletableFuture<Boolean> copyPermissionsAsync(@Nullable Context context, @NotNull Subject source, @NotNull Subject target, boolean replace) {
        return CompletableFuture.supplyAsync(() -> copyPermissions(context, source, target, replace));
    }

    // ==================== Group Management ====================

    @Override
    @NotNull
    public String[] groups() {
        Collection<Group> loadedGroups = hyperPerms.getGroupManager().getLoadedGroups();
        return loadedGroups.stream()
                .map(Group::getName)
                .toArray(String[]::new);
    }

    @Override
    @Nullable
    public String primaryGroup(@Nullable Context context, @NotNull Subject subject) {
        UUID uuid = subject.asUUID();
        if (uuid == null) {
            return null;
        }

        UserManagerImpl userManager = (UserManagerImpl) hyperPerms.getUserManager();
        User user = userManager.getUser(uuid);
        if (user == null) {
            return hyperPerms.getConfig().getDefaultGroup();
        }

        return user.getPrimaryGroup();
    }

    @Override
    @NotNull
    public CompletableFuture<String> primaryGroupAsync(@Nullable Context context, @NotNull Subject subject) {
        return CompletableFuture.supplyAsync(() -> primaryGroup(context, subject));
    }

    @Override
    @NotNull
    public String[] getGroups(@Nullable Context context, @NotNull Subject subject) {
        UUID uuid = subject.asUUID();
        if (uuid == null) {
            return new String[0];
        }

        UserManagerImpl userManager = (UserManagerImpl) hyperPerms.getUserManager();
        User user = userManager.getUser(uuid);
        if (user == null) {
            return new String[] { hyperPerms.getConfig().getDefaultGroup() };
        }

        return user.getInheritedGroups().toArray(new String[0]);
    }

    @Override
    @NotNull
    public CompletableFuture<String[]> getGroupsAsync(@Nullable Context context, @NotNull Subject subject) {
        return CompletableFuture.supplyAsync(() -> getGroups(context, subject));
    }

    @Override
    public boolean copyGroups(@Nullable Context context, @NotNull Subject source, @NotNull Subject target) {
        String[] sourceGroups = getGroups(context, source);

        for (String group : sourceGroups) {
            if (!addGroup(context, target, group)) {
                return false;
            }
        }

        return true;
    }

    @Override
    @NotNull
    public CompletableFuture<Boolean> copyGroupsAsync(@Nullable Context context, @NotNull Subject source, @NotNull Subject target) {
        return CompletableFuture.supplyAsync(() -> copyGroups(context, source, target));
    }

    @Override
    public boolean inGroup(@Nullable Context context, @NotNull Subject subject) {
        // Check if user is in any group (other than default)
        String[] groups = getGroups(context, subject);
        if (groups.length == 0) {
            return false;
        }
        // Check if they're in any group other than default
        String defaultGroup = hyperPerms.getConfig().getDefaultGroup();
        for (String group : groups) {
            if (!group.equalsIgnoreCase(defaultGroup)) {
                return true;
            }
        }
        return groups.length > 0;
    }

    @Override
    public boolean inGroup(@Nullable Context context, @NotNull Subject subject, @NotNull String groupName) {
        String[] groups = getGroups(context, subject);
        for (String group : groups) {
            if (group.equalsIgnoreCase(groupName)) {
                return true;
            }
        }
        return false;
    }

    @Override
    @NotNull
    public CompletableFuture<Boolean> inGroupAsync(@Nullable Context context, @NotNull Subject subject) {
        return CompletableFuture.supplyAsync(() -> inGroup(context, subject));
    }

    @Override
    @NotNull
    public CompletableFuture<Boolean> inGroupAsync(@Nullable Context context, @NotNull Subject subject, @NotNull String groupName) {
        return CompletableFuture.supplyAsync(() -> inGroup(context, subject, groupName));
    }

    @Override
    public boolean addGroup(@Nullable Context context, @NotNull Subject subject, @NotNull String groupName) {
        UUID uuid = subject.asUUID();
        if (uuid == null) {
            return false;
        }

        UserManagerImpl userManager = (UserManagerImpl) hyperPerms.getUserManager();
        User user = userManager.getUser(uuid);
        if (user == null) {
            user = userManager.getOrCreateUser(uuid);
        }

        var result = user.addGroup(groupName);
        if (result == DataMutateResult.SUCCESS) {
            userManager.saveUser(user).join();
            hyperPerms.getCacheInvalidator().invalidateUser(uuid);
            return true;
        }
        return false;
    }

    @Override
    @NotNull
    public CompletableFuture<Boolean> addGroupAsync(@Nullable Context context, @NotNull Subject subject, @NotNull String groupName) {
        return CompletableFuture.supplyAsync(() -> addGroup(context, subject, groupName));
    }

    @Override
    public boolean removeGroup(@Nullable Context context, @NotNull Subject subject, @NotNull String groupName) {
        UUID uuid = subject.asUUID();
        if (uuid == null) {
            return false;
        }

        UserManagerImpl userManager = (UserManagerImpl) hyperPerms.getUserManager();
        User user = userManager.getUser(uuid);
        if (user == null) {
            return false;
        }

        var result = user.removeGroup(groupName);
        if (result == DataMutateResult.SUCCESS) {
            userManager.saveUser(user).join();
            hyperPerms.getCacheInvalidator().invalidateUser(uuid);
            return true;
        }
        return false;
    }

    @Override
    @NotNull
    public CompletableFuture<Boolean> removeGroupAsync(@Nullable Context context, @NotNull Subject subject, @NotNull String groupName) {
        return CompletableFuture.supplyAsync(() -> removeGroup(context, subject, groupName));
    }

    // ==================== Group Permission Checks ====================

    @Override
    @NotNull
    public TriState groupHas(@Nullable Context context, @NotNull String groupName, @NotNull String permission) {
        Group group = hyperPerms.getGroupManager().getGroup(groupName);
        if (group == null) {
            return TriState.UNDEFINED;
        }

        ContextSet contexts = convertContext(context);
        PermissionResolver resolver = hyperPerms.getResolver();
        var resolved = resolver.resolveGroup(group, contexts);
        WildcardMatcher.TriState result = resolved.check(permission);

        return toVaultTriState(result);
    }

    @Override
    @NotNull
    public CompletableFuture<TriState> groupHasAsync(@Nullable Context context, @NotNull String groupName, @NotNull String permission) {
        return CompletableFuture.supplyAsync(() -> groupHas(context, groupName, permission));
    }

    @Override
    public boolean groupSetPermission(@Nullable Context context, @NotNull String groupName, @NotNull String permission, @NotNull TriState value) {
        Group group = hyperPerms.getGroupManager().getGroup(groupName);
        if (group == null) {
            return false;
        }

        ContextSet contexts = convertContext(context);

        if (value == TriState.UNDEFINED) {
            group.removeNode(permission);
        } else {
            Node node = Node.builder(permission)
                .value(value == TriState.TRUE)
                .contexts(contexts)
                .build();
            group.setNode(node);
        }

        hyperPerms.getGroupManager().saveGroup(group).join();
        // Cache invalidation for groups is handled by GroupManagerImpl.saveGroup()
        return true;
    }

    @Override
    @NotNull
    public CompletableFuture<Boolean> groupSetPermissionAsync(@Nullable Context context, @NotNull String groupName, @NotNull String permission, @NotNull TriState value) {
        return CompletableFuture.supplyAsync(() -> groupSetPermission(context, groupName, permission, value));
    }

    @Override
    public boolean groupSetTransientPermission(@Nullable Context context, @NotNull String groupName, @NotNull String permission, @NotNull TriState value) {
        throw new UnsupportedOperationException("Transient group permission modification through Vault is not yet supported. Use HyperPerms commands instead.");
    }

    @Override
    @NotNull
    public CompletableFuture<Boolean> groupSetTransientPermissionAsync(@Nullable Context context, @NotNull String groupName, @NotNull String permission, @NotNull TriState value) {
        return CompletableFuture.supplyAsync(() -> groupSetTransientPermission(context, groupName, permission, value));
    }

    // ==================== Helper Methods ====================

    /**
     * Converts a VaultUnlocked Context to a HyperPerms ContextSet.
     *
     * @param vaultContext the Vault context, may be null
     * @return the equivalent HyperPerms ContextSet
     */
    @NotNull
    private ContextSet convertContext(@Nullable Context vaultContext) {
        if (vaultContext == null || vaultContext == Context.GLOBAL) {
            return ContextSet.empty();
        }

        ContextSet.Builder builder = ContextSet.builder();
        Map<String, String> contextMap = vaultContext.asMap();
        if (contextMap != null) {
            for (Map.Entry<String, String> entry : contextMap.entrySet()) {
                builder.add(entry.getKey(), entry.getValue());
            }
        }
        return builder.build();
    }

    /**
     * Converts a HyperPerms TriState to a VaultUnlocked TriState.
     *
     * @param hpTriState the HyperPerms tri-state
     * @return the equivalent Vault TriState
     */
    @NotNull
    private TriState toVaultTriState(@NotNull WildcardMatcher.TriState hpTriState) {
        return switch (hpTriState) {
            case TRUE -> TriState.TRUE;
            case FALSE -> TriState.FALSE;
            case UNDEFINED -> TriState.UNDEFINED;
        };
    }
}
