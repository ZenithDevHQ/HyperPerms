package com.hyperperms.model;

import com.hyperperms.api.context.Context;
import com.hyperperms.api.context.ContextSet;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;

import static org.junit.jupiter.api.Assertions.*;

class NodeTest {

    @Test
    void testBasicNode() {
        Node node = Node.of("test.permission");
        assertEquals("test.permission", node.getPermission());
        assertTrue(node.getValue());
        assertNull(node.getExpiry());
        assertTrue(node.getContexts().isEmpty());
        assertFalse(node.isExpired());
        assertTrue(node.isPermanent());
    }

    @Test
    void testBuilderWithExpiry() {
        Node node = Node.builder("test.permission")
                .expiry(Duration.ofHours(1))
                .build();

        assertTrue(node.isTemporary());
        assertFalse(node.isPermanent());
        assertFalse(node.isExpired());
        assertNotNull(node.getExpiry());
    }

    @Test
    void testExpiredNode() {
        Node node = Node.builder("test.permission")
                .expiry(Instant.now().minusSeconds(10))
                .build();

        assertTrue(node.isExpired());
    }

    @Test
    void testBuilderWithContext() {
        Node node = Node.builder("test.permission")
                .world("nether")
                .context("server", "lobby")
                .build();

        assertFalse(node.getContexts().isEmpty());
        assertEquals(2, node.getContexts().size());
        assertEquals("nether", node.getContexts().getValue("world"));
    }

    @Test
    void testDeniedPermission() {
        Node node = Node.builder("test.permission")
                .denied()
                .build();

        assertFalse(node.getValue());
    }

    @Test
    void testGroupNode() {
        Node node = Node.group("admin");

        assertTrue(node.isGroupNode());
        assertEquals("admin", node.getGroupName());
        assertEquals("group.admin", node.getPermission());
    }

    @Test
    void testNonGroupNode() {
        Node node = Node.of("some.permission");

        assertFalse(node.isGroupNode());
        assertNull(node.getGroupName());
    }

    @Test
    void testNegatedPermission() {
        Node node = Node.builder("-test.permission").build();

        assertTrue(node.isNegated());
        assertEquals("test.permission", node.getBasePermission());
    }

    @Test
    void testWildcardDetection() {
        assertTrue(Node.of("*").isWildcard());
        assertTrue(Node.of("plugin.*").isWildcard());
        assertTrue(Node.of("plugin.command.*").isWildcard());
        assertFalse(Node.of("plugin.command").isWildcard());
    }

    @Test
    void testContextMatching() {
        Node node = Node.builder("test.permission")
                .world("nether")
                .build();

        ContextSet netherContext = ContextSet.of(Context.world("nether"));
        ContextSet overworldContext = ContextSet.of(Context.world("overworld"));
        ContextSet emptyContext = ContextSet.empty();

        assertTrue(node.appliesIn(netherContext));
        assertFalse(node.appliesIn(overworldContext));
        assertFalse(node.appliesIn(emptyContext));
    }

    @Test
    void testContextlessNodeMatchesAll() {
        Node node = Node.of("test.permission");

        assertTrue(node.appliesIn(ContextSet.empty()));
        assertTrue(node.appliesIn(ContextSet.of(Context.world("nether"))));
    }

    @Test
    void testEqualsIgnoringExpiry() {
        Node node1 = Node.builder("test.permission")
                .expiry(Duration.ofHours(1))
                .build();

        Node node2 = Node.builder("test.permission")
                .expiry(Duration.ofHours(2))
                .build();

        Node node3 = Node.builder("test.other")
                .expiry(Duration.ofHours(1))
                .build();

        assertTrue(node1.equalsIgnoringExpiry(node2));
        assertFalse(node1.equalsIgnoringExpiry(node3));
    }
}
