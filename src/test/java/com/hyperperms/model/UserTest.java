package com.hyperperms.model;

import com.hyperperms.api.PermissionHolder.DataMutateResult;
import com.hyperperms.api.context.Context;
import com.hyperperms.api.context.ContextSet;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class UserTest {

    @Test
    void testBasicUser() {
        UUID uuid = UUID.randomUUID();
        User user = new User(uuid, "TestPlayer");

        assertEquals(uuid, user.getUuid());
        assertEquals("TestPlayer", user.getUsername());
        assertEquals("default", user.getPrimaryGroup());
        assertTrue(user.getNodes().isEmpty());
    }

    @Test
    void testAddPermission() {
        User user = new User(UUID.randomUUID(), "TestPlayer");
        Node node = Node.of("test.permission");

        assertEquals(DataMutateResult.SUCCESS, user.addNode(node));
        assertEquals(DataMutateResult.ALREADY_EXISTS, user.addNode(node));
        assertTrue(user.hasNode(node));
        assertEquals(1, user.getNodes().size());
    }

    @Test
    void testRemovePermission() {
        User user = new User(UUID.randomUUID(), "TestPlayer");
        Node node = Node.of("test.permission");

        user.addNode(node);
        assertEquals(DataMutateResult.SUCCESS, user.removeNode(node));
        assertEquals(DataMutateResult.DOES_NOT_EXIST, user.removeNode(node));
        assertFalse(user.hasNode(node));
    }

    @Test
    void testRemovePermissionByString() {
        User user = new User(UUID.randomUUID(), "TestPlayer");
        user.addNode(Node.of("test.permission"));

        assertEquals(DataMutateResult.SUCCESS, user.removeNode("test.permission"));
        assertEquals(DataMutateResult.DOES_NOT_EXIST, user.removeNode("test.permission"));
    }

    @Test
    void testSetPermission() {
        User user = new User(UUID.randomUUID(), "TestPlayer");

        Node node1 = Node.builder("test.permission").expiry(Duration.ofHours(1)).build();
        Node node2 = Node.builder("test.permission").expiry(Duration.ofHours(2)).build();

        user.setNode(node1);
        assertEquals(1, user.getNodes().size());

        user.setNode(node2);
        assertEquals(1, user.getNodes().size()); // Should replace, not add
    }

    @Test
    void testGroupInheritance() {
        User user = new User(UUID.randomUUID(), "TestPlayer");

        assertEquals(DataMutateResult.SUCCESS, user.addGroup("vip"));
        assertEquals(DataMutateResult.SUCCESS, user.addGroup("donator"));

        var groups = user.getInheritedGroups();
        // 3 groups: default (primaryGroup) + vip + donator
        assertEquals(3, groups.size());
        assertTrue(groups.contains("default"));  // Primary group is always included
        assertTrue(groups.contains("vip"));
        assertTrue(groups.contains("donator"));
    }

    @Test
    void testRemoveGroup() {
        User user = new User(UUID.randomUUID(), "TestPlayer");
        user.addGroup("vip");

        assertEquals(DataMutateResult.SUCCESS, user.removeGroup("vip"));
        assertFalse(user.getInheritedGroups().contains("vip"));
    }

    @Test
    void testClearNodes() {
        User user = new User(UUID.randomUUID(), "TestPlayer");
        user.addNode(Node.of("perm1"));
        user.addNode(Node.of("perm2"));
        user.addGroup("vip");

        user.clearNodes();
        assertTrue(user.getNodes().isEmpty());
    }

    @Test
    void testContextFiltering() {
        User user = new User(UUID.randomUUID(), "TestPlayer");

        Node globalNode = Node.of("global.permission");
        Node netherNode = Node.builder("nether.permission")
                .world("nether")
                .build();

        user.addNode(globalNode);
        user.addNode(netherNode);

        ContextSet netherContext = ContextSet.of(Context.world("nether"));
        ContextSet overworldContext = ContextSet.of(Context.world("overworld"));

        var netherNodes = user.getNodes(netherContext);
        assertEquals(2, netherNodes.size()); // Both apply in nether

        var overworldNodes = user.getNodes(overworldContext);
        assertEquals(1, overworldNodes.size()); // Only global applies
    }

    @Test
    void testExpiredNodeFiltering() {
        User user = new User(UUID.randomUUID(), "TestPlayer");

        Node permanent = Node.of("permanent.permission");
        Node expired = Node.builder("expired.permission")
                .expiry(Instant.now().minusSeconds(10))
                .build();
        Node temporary = Node.builder("temporary.permission")
                .expiry(Duration.ofHours(1))
                .build();

        user.addNode(permanent);
        user.addNode(expired);
        user.addNode(temporary);

        var allNodes = user.getNodes(true);
        assertEquals(3, allNodes.size());

        var validNodes = user.getNodes(false);
        assertEquals(2, validNodes.size());
    }

    @Test
    void testCleanupExpired() {
        User user = new User(UUID.randomUUID(), "TestPlayer");

        Node permanent = Node.of("permanent");
        Node expired = Node.builder("expired")
                .expiry(Instant.now().minusSeconds(10))
                .build();

        user.addNode(permanent);
        user.addNode(expired);

        int removed = user.cleanupExpired();
        assertEquals(1, removed);
        assertEquals(1, user.getNodes().size());
    }

    @Test
    void testHasData() {
        User user = new User(UUID.randomUUID(), "TestPlayer");
        assertFalse(user.hasData());

        user.addNode(Node.of("test.permission"));
        assertTrue(user.hasData());
    }

    @Test
    void testIdentifier() {
        UUID uuid = UUID.randomUUID();
        User user = new User(uuid, "TestPlayer");

        assertEquals(uuid.toString(), user.getIdentifier());
        assertEquals("TestPlayer", user.getFriendlyName());
    }
}
