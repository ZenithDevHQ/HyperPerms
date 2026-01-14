package com.hyperperms.api.context;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ContextSetTest {

    @Test
    void testEmptyContextSet() {
        ContextSet empty = ContextSet.empty();
        assertTrue(empty.isEmpty());
        assertEquals(0, empty.size());
    }

    @Test
    void testSingleContext() {
        ContextSet set = ContextSet.of(Context.world("nether"));
        assertFalse(set.isEmpty());
        assertEquals(1, set.size());
        assertTrue(set.containsKey("world"));
        assertEquals("nether", set.getValue("world"));
    }

    @Test
    void testMultipleContexts() {
        ContextSet set = ContextSet.of(
                Context.world("nether"),
                Context.server("lobby")
        );

        assertEquals(2, set.size());
        assertTrue(set.containsKey("world"));
        assertTrue(set.containsKey("server"));
    }

    @Test
    void testBuilder() {
        ContextSet set = ContextSet.builder()
                .add("world", "nether")
                .add("server", "lobby")
                .add(Context.gameMode("creative"))
                .build();

        assertEquals(3, set.size());
        assertEquals("nether", set.getValue("world"));
        assertEquals("lobby", set.getValue("server"));
        assertEquals("creative", set.getValue("gamemode"));
    }

    @Test
    void testIsSatisfiedBy() {
        ContextSet required = ContextSet.of(Context.world("nether"));

        ContextSet exact = ContextSet.of(Context.world("nether"));
        ContextSet superset = ContextSet.builder()
                .add(Context.world("nether"))
                .add(Context.server("lobby"))
                .build();
        ContextSet different = ContextSet.of(Context.world("overworld"));
        ContextSet empty = ContextSet.empty();

        assertTrue(required.isSatisfiedBy(exact));
        assertTrue(required.isSatisfiedBy(superset));
        assertFalse(required.isSatisfiedBy(different));
        assertFalse(required.isSatisfiedBy(empty));
    }

    @Test
    void testEmptyIsSatisfiedByAnything() {
        ContextSet empty = ContextSet.empty();

        assertTrue(empty.isSatisfiedBy(ContextSet.empty()));
        assertTrue(empty.isSatisfiedBy(ContextSet.of(Context.world("nether"))));
    }

    @Test
    void testContextParsing() {
        Context parsed = Context.parse("world=nether");
        assertEquals("world", parsed.key());
        assertEquals("nether", parsed.value());
    }

    @Test
    void testInvalidContextParsing() {
        assertThrows(IllegalArgumentException.class, () -> Context.parse("invalid"));
        assertThrows(IllegalArgumentException.class, () -> Context.parse("=value"));
        assertThrows(IllegalArgumentException.class, () -> Context.parse("key="));
    }

    @Test
    void testContextCaseNormalization() {
        Context upper = new Context("WORLD", "NETHER");
        assertEquals("world", upper.key());
        assertEquals("nether", upper.value());
    }
}
