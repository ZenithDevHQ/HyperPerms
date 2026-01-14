package com.hyperperms.resolver;

import com.hyperperms.api.context.Context;
import com.hyperperms.api.context.ContextSet;
import com.hyperperms.model.Group;
import com.hyperperms.model.Node;
import com.hyperperms.model.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class PermissionResolverTest {

    private Map<String, Group> groups;
    private PermissionResolver resolver;

    @BeforeEach
    void setUp() {
        groups = new HashMap<>();
        resolver = new PermissionResolver(groups::get);
    }

    private Group createGroup(String name, int weight) {
        Group group = new Group(name, weight);
        groups.put(name, group);
        return group;
    }

    @Test
    void testDirectPermission() {
        createGroup("default", 0);
        User user = new User(UUID.randomUUID(), "Test");
        user.addNode(Node.of("test.permission"));

        assertTrue(resolver.hasPermission(user, "test.permission", ContextSet.empty()));
        assertFalse(resolver.hasPermission(user, "other.permission", ContextSet.empty()));
    }

    @Test
    void testGroupInheritance() {
        Group defaultGroup = createGroup("default", 0);
        defaultGroup.addNode(Node.of("default.permission"));

        User user = new User(UUID.randomUUID(), "Test");
        user.setPrimaryGroup("default");

        assertTrue(resolver.hasPermission(user, "default.permission", ContextSet.empty()));
    }

    @Test
    void testMultipleGroupInheritance() {
        Group defaultGroup = createGroup("default", 0);
        defaultGroup.addNode(Node.of("default.permission"));

        Group vipGroup = createGroup("vip", 10);
        vipGroup.addNode(Node.of("vip.permission"));

        User user = new User(UUID.randomUUID(), "Test");
        user.setPrimaryGroup("default");
        user.addGroup("vip");

        assertTrue(resolver.hasPermission(user, "default.permission", ContextSet.empty()));
        assertTrue(resolver.hasPermission(user, "vip.permission", ContextSet.empty()));
    }

    @Test
    void testGroupParentInheritance() {
        Group defaultGroup = createGroup("default", 0);
        defaultGroup.addNode(Node.of("default.permission"));

        Group vipGroup = createGroup("vip", 10);
        vipGroup.addParent("default"); // vip inherits from default
        vipGroup.addNode(Node.of("vip.permission"));

        User user = new User(UUID.randomUUID(), "Test");
        user.setPrimaryGroup("vip");

        // User should have both default and vip permissions
        assertTrue(resolver.hasPermission(user, "default.permission", ContextSet.empty()));
        assertTrue(resolver.hasPermission(user, "vip.permission", ContextSet.empty()));
    }

    @Test
    void testWeightPriority() {
        Group lowWeight = createGroup("low", 0);
        lowWeight.addNode(Node.builder("test.permission").denied().build());

        Group highWeight = createGroup("high", 100);
        highWeight.addNode(Node.of("test.permission")); // Grant

        User user = new User(UUID.randomUUID(), "Test");
        user.addGroup("low");
        user.addGroup("high");

        // High weight should win
        assertTrue(resolver.hasPermission(user, "test.permission", ContextSet.empty()));
    }

    @Test
    void testUserOverridesGroup() {
        Group group = createGroup("default", 0);
        group.addNode(Node.builder("test.permission").denied().build());

        User user = new User(UUID.randomUUID(), "Test");
        user.setPrimaryGroup("default");
        user.addNode(Node.of("test.permission")); // User grants it

        // User's direct permission should override group
        assertTrue(resolver.hasPermission(user, "test.permission", ContextSet.empty()));
    }

    @Test
    void testWildcardPermission() {
        Group group = createGroup("default", 0);
        group.addNode(Node.of("myplugin.*"));

        User user = new User(UUID.randomUUID(), "Test");
        user.setPrimaryGroup("default");

        assertTrue(resolver.hasPermission(user, "myplugin.command", ContextSet.empty()));
        assertTrue(resolver.hasPermission(user, "myplugin.command.home", ContextSet.empty()));
        assertFalse(resolver.hasPermission(user, "otherplugin.command", ContextSet.empty()));
    }

    @Test
    void testContextFiltering() {
        Group group = createGroup("default", 0);
        group.addNode(Node.builder("fly.permission")
                .world("creative")
                .build());

        User user = new User(UUID.randomUUID(), "Test");
        user.setPrimaryGroup("default");

        ContextSet creativeContext = ContextSet.of(Context.world("creative"));
        ContextSet survivalContext = ContextSet.of(Context.world("survival"));

        assertTrue(resolver.hasPermission(user, "fly.permission", creativeContext));
        assertFalse(resolver.hasPermission(user, "fly.permission", survivalContext));
    }

    @Test
    void testResolvedPermissions() {
        Group group = createGroup("default", 0);
        group.addNode(Node.of("perm1"));
        group.addNode(Node.of("perm2"));
        group.addNode(Node.builder("perm3").denied().build());

        User user = new User(UUID.randomUUID(), "Test");
        user.setPrimaryGroup("default");

        var resolved = resolver.resolve(user, ContextSet.empty());

        assertEquals(3, resolved.size());
        assertTrue(resolved.hasPermission("perm1"));
        assertTrue(resolved.hasPermission("perm2"));
        assertFalse(resolved.hasPermission("perm3"));

        var granted = resolved.getGrantedPermissions();
        assertEquals(2, granted.size());

        var denied = resolved.getDeniedPermissions();
        assertEquals(1, denied.size());
    }

    @Test
    void testCyclicInheritance() {
        // Create a cycle: A -> B -> C -> A
        Group groupA = createGroup("a", 0);
        Group groupB = createGroup("b", 0);
        Group groupC = createGroup("c", 0);

        groupA.addParent("b");
        groupB.addParent("c");
        groupC.addParent("a");

        groupA.addNode(Node.of("perm.a"));
        groupB.addNode(Node.of("perm.b"));
        groupC.addNode(Node.of("perm.c"));

        User user = new User(UUID.randomUUID(), "Test");
        user.addGroup("a");

        // Should not stack overflow, should get all permissions
        assertTrue(resolver.hasPermission(user, "perm.a", ContextSet.empty()));
        assertTrue(resolver.hasPermission(user, "perm.b", ContextSet.empty()));
        assertTrue(resolver.hasPermission(user, "perm.c", ContextSet.empty()));
    }
}
