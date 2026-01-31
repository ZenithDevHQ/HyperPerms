package com.hyperperms.resolver;

import com.hyperperms.resolver.WildcardMatcher.MatchResult;
import com.hyperperms.resolver.WildcardMatcher.MatchType;
import com.hyperperms.resolver.WildcardMatcher.TriState;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class WildcardMatcherTest {

    @Test
    void testExactMatch() {
        assertTrue(WildcardMatcher.matches("plugin.command.home", "plugin.command.home"));
        assertFalse(WildcardMatcher.matches("plugin.command.home", "plugin.command.spawn"));
    }

    @Test
    void testUniversalWildcard() {
        assertTrue(WildcardMatcher.matches("anything.at.all", "*"));
        assertTrue(WildcardMatcher.matches("single", "*"));
    }

    @Test
    void testPrefixWildcard() {
        assertTrue(WildcardMatcher.matches("plugin.command.home", "plugin.*"));
        assertTrue(WildcardMatcher.matches("plugin.command.home", "plugin.command.*"));
        assertFalse(WildcardMatcher.matches("other.command.home", "plugin.*"));
    }

    @Test
    void testCheck() {
        Map<String, Boolean> perms = new HashMap<>();
        perms.put("myplugin.command.*", true);
        perms.put("myplugin.admin", false);

        assertEquals(TriState.TRUE, WildcardMatcher.check("myplugin.command.home", perms));
        assertEquals(TriState.TRUE, WildcardMatcher.check("myplugin.command.spawn", perms));
        assertEquals(TriState.FALSE, WildcardMatcher.check("myplugin.admin", perms));
        assertEquals(TriState.UNDEFINED, WildcardMatcher.check("otherplugin.command", perms));
    }

    @Test
    void testGlobalWildcardWins() {
        // Hytale resolution order: Global * is checked FIRST
        // This means ["*", "-plugin.admin"] → plugin.admin = TRUE
        Map<String, Boolean> perms = new HashMap<>();
        perms.put("*", true); // Grant all - checked FIRST
        perms.put("-plugin.admin", true); // Deny plugin.admin - never reached

        // Global * wins over everything
        assertEquals(TriState.TRUE, WildcardMatcher.check("plugin.admin", perms));
        assertEquals(TriState.TRUE, WildcardMatcher.check("plugin.command", perms));
    }

    @Test
    void testNegationPriorityOverWildcard() {
        // -plugin.admin should override plugin.*
        Map<String, Boolean> perms = new HashMap<>();
        perms.put("plugin.*", true);
        perms.put("-plugin.admin", true);

        assertEquals(TriState.FALSE, WildcardMatcher.check("plugin.admin", perms));
        assertEquals(TriState.TRUE, WildcardMatcher.check("plugin.command", perms));
        assertEquals(TriState.TRUE, WildcardMatcher.check("plugin.other.thing", perms));
    }

    @Test
    void testNegatedWildcard() {
        // Hytale resolution order: shortest prefix first
        // For plugin.admin.bypass, plugin.* (1 part) is checked before -plugin.admin.* (2 parts)
        Map<String, Boolean> perms = new HashMap<>();
        perms.put("plugin.*", true);
        perms.put("-plugin.admin.*", true);

        // plugin.* wins because it's a shorter prefix than -plugin.admin.*
        assertEquals(TriState.TRUE, WildcardMatcher.check("plugin.admin.bypass", perms));
        assertEquals(TriState.TRUE, WildcardMatcher.check("plugin.admin.debug", perms));
        assertEquals(TriState.TRUE, WildcardMatcher.check("plugin.command", perms));
        assertEquals(TriState.TRUE, WildcardMatcher.check("plugin.admin", perms));
    }

    @Test
    void testUniversalNegation() {
        Map<String, Boolean> perms = new HashMap<>();
        perms.put("-*", true); // Deny all

        assertEquals(TriState.FALSE, WildcardMatcher.check("anything", perms));
        assertEquals(TriState.FALSE, WildcardMatcher.check("plugin.command", perms));
    }

    @Test
    void testExactGrantBeforeNegation() {
        // Hytale resolution order: exact grants are checked before exact negations
        Map<String, Boolean> perms = new HashMap<>();
        perms.put("-plugin.admin", true);
        perms.put("plugin.admin", true);

        // Exact grant checked before exact negation
        assertEquals(TriState.TRUE, WildcardMatcher.check("plugin.admin", perms));
    }

    @Test
    void testGeneratePatterns() {
        String[] patterns = WildcardMatcher.generatePatterns("plugin.command.home");
        assertEquals(4, patterns.length);
        assertEquals("plugin.command.home", patterns[0]);
        assertEquals("plugin.command.*", patterns[1]);
        assertEquals("plugin.*", patterns[2]);
        assertEquals("*", patterns[3]);
    }

    @Test
    void testTriState() {
        assertTrue(TriState.TRUE.asBoolean());
        assertFalse(TriState.FALSE.asBoolean());
        assertFalse(TriState.UNDEFINED.asBoolean());

        assertTrue(TriState.UNDEFINED.asBoolean(true));
        assertFalse(TriState.UNDEFINED.asBoolean(false));
    }

    // ==================== New Tests for Tracing ====================

    @Test
    void testCheckWithTraceExactMatch() {
        Map<String, Boolean> perms = new HashMap<>();
        perms.put("plugin.command", true);

        MatchResult result = WildcardMatcher.checkWithTrace("plugin.command", perms);
        assertEquals(TriState.TRUE, result.result());
        assertEquals("plugin.command", result.matchedNode());
        assertEquals(MatchType.EXACT, result.matchType());
        assertTrue(result.isMatched());
        assertFalse(result.isWildcard());
        assertFalse(result.isNegation());
    }

    @Test
    void testCheckWithTraceWildcard() {
        Map<String, Boolean> perms = new HashMap<>();
        perms.put("plugin.*", true);

        MatchResult result = WildcardMatcher.checkWithTrace("plugin.command.home", perms);
        assertEquals(TriState.TRUE, result.result());
        assertEquals("plugin.*", result.matchedNode());
        assertEquals(MatchType.WILDCARD, result.matchType());
        assertTrue(result.isWildcard());
        assertFalse(result.isNegation());
    }

    @Test
    void testCheckWithTraceNegation() {
        Map<String, Boolean> perms = new HashMap<>();
        perms.put("-plugin.admin", true);

        MatchResult result = WildcardMatcher.checkWithTrace("plugin.admin", perms);
        assertEquals(TriState.FALSE, result.result());
        assertEquals("-plugin.admin", result.matchedNode());
        assertEquals(MatchType.EXACT_NEGATION, result.matchType());
        assertTrue(result.isNegation());
        assertFalse(result.isWildcard());
    }

    @Test
    void testCheckWithTraceNegatedWildcard() {
        Map<String, Boolean> perms = new HashMap<>();
        perms.put("-plugin.admin.*", true);

        MatchResult result = WildcardMatcher.checkWithTrace("plugin.admin.bypass", perms);
        assertEquals(TriState.FALSE, result.result());
        assertEquals("-plugin.admin.*", result.matchedNode());
        assertEquals(MatchType.WILDCARD_NEGATION, result.matchType());
        assertTrue(result.isNegation());
        assertTrue(result.isWildcard());
    }

    @Test
    void testCheckWithTraceUniversal() {
        Map<String, Boolean> perms = new HashMap<>();
        perms.put("*", true);

        MatchResult result = WildcardMatcher.checkWithTrace("anything", perms);
        assertEquals(TriState.TRUE, result.result());
        assertEquals("*", result.matchedNode());
        assertEquals(MatchType.UNIVERSAL, result.matchType());
        assertTrue(result.isWildcard());
    }

    @Test
    void testCheckWithTraceNoMatch() {
        Map<String, Boolean> perms = new HashMap<>();
        perms.put("other.permission", true);

        MatchResult result = WildcardMatcher.checkWithTrace("plugin.command", perms);
        assertEquals(TriState.UNDEFINED, result.result());
        assertNull(result.matchedNode());
        assertEquals(MatchType.NONE, result.matchType());
        assertFalse(result.isMatched());
    }

    @Test
    void testShortestPrefixWins() {
        // Hytale resolution order: shortest prefix first
        // plugin.* (1 part) is checked before plugin.command.* (2 parts)
        Map<String, Boolean> perms = new HashMap<>();
        perms.put("plugin.*", false); // Deny at shorter prefix
        perms.put("plugin.command.*", true); // Grant at longer prefix

        // Shortest prefix wins - plugin.* denies first
        assertEquals(TriState.FALSE, WildcardMatcher.check("plugin.command.home", perms));
        assertEquals(TriState.FALSE, WildcardMatcher.check("plugin.admin", perms));
    }

    @Test
    void testCaseInsensitivity() {
        Map<String, Boolean> perms = new HashMap<>();
        perms.put("plugin.command", true);

        assertEquals(TriState.TRUE, WildcardMatcher.check("PLUGIN.COMMAND", perms));
        assertEquals(TriState.TRUE, WildcardMatcher.check("Plugin.Command", perms));
    }

    // ==================== P0 Specification Tests ====================

    @Test
    void testWildcardWithSpecificNegation() {
        // Scenario from spec: essentials.* + -essentials.god
        Map<String, Boolean> perms = new HashMap<>();
        perms.put("essentials.*", true);    // Grant all essentials
        perms.put("-essentials.god", true); // But deny god mode

        assertEquals(TriState.TRUE, WildcardMatcher.check("essentials.home", perms));
        assertEquals(TriState.TRUE, WildcardMatcher.check("essentials.spawn", perms));
        assertEquals(TriState.TRUE, WildcardMatcher.check("essentials.fly", perms));
        assertEquals(TriState.FALSE, WildcardMatcher.check("essentials.god", perms)); // Denied
    }

    @Test
    void testNestedWildcardNegation() {
        // Hytale resolution order: shortest prefix first
        // essentials.* (1 part) is checked before -essentials.admin.* (2 parts)
        Map<String, Boolean> perms = new HashMap<>();
        perms.put("essentials.*", true);
        perms.put("-essentials.admin.*", true);

        // All should be TRUE because essentials.* (shorter) wins over -essentials.admin.* (longer)
        assertEquals(TriState.TRUE, WildcardMatcher.check("essentials.home", perms));
        assertEquals(TriState.TRUE, WildcardMatcher.check("essentials.admin", perms));
        assertEquals(TriState.TRUE, WildcardMatcher.check("essentials.admin.bypass", perms));
        assertEquals(TriState.TRUE, WildcardMatcher.check("essentials.admin.reload", perms));
    }

    // ==================== Hytale Resolution Order Tests ====================

    @Test
    void testGlobalNegationDeniesEverything() {
        // Global -* denies everything when there's no global *
        Map<String, Boolean> perms = new HashMap<>();
        perms.put("-*", true);
        perms.put("plugin.command", true); // Never reached

        assertEquals(TriState.FALSE, WildcardMatcher.check("plugin.command", perms));
        assertEquals(TriState.FALSE, WildcardMatcher.check("anything", perms));
    }

    @Test
    void testGlobalWildcardBeatsGlobalNegation() {
        // Global * is checked BEFORE -*, so * wins
        Map<String, Boolean> perms = new HashMap<>();
        perms.put("*", true);
        perms.put("-*", true);

        assertEquals(TriState.TRUE, WildcardMatcher.check("anything", perms));
    }

    @Test
    void testExactNegationStillWorksWithoutGlobalWildcard() {
        // Without global *, exact negations work as expected
        Map<String, Boolean> perms = new HashMap<>();
        perms.put("plugin.*", true);
        perms.put("-plugin.admin", true); // Exact negation

        // Exact negation checked before wildcards, so it wins
        assertEquals(TriState.FALSE, WildcardMatcher.check("plugin.admin", perms));
        assertEquals(TriState.TRUE, WildcardMatcher.check("plugin.command", perms));
    }

    @Test
    void testToAvoidNegationUseSpecificGrants() {
        // The correct way to grant everything EXCEPT specific permissions:
        // Use specific grants instead of global *
        Map<String, Boolean> perms = new HashMap<>();
        perms.put("essentials.*", true);
        perms.put("-essentials.god", true); // Exact negation

        // Works because exact negation is checked before prefix wildcards
        assertEquals(TriState.TRUE, WildcardMatcher.check("essentials.home", perms));
        assertEquals(TriState.TRUE, WildcardMatcher.check("essentials.spawn", perms));
        assertEquals(TriState.FALSE, WildcardMatcher.check("essentials.god", perms)); // Denied
    }

    @Test
    void testHytaleResolutionOrderSummary() {
        // Complete example demonstrating Hytale's resolution order
        // Order: 1. Global * 2. Global -* 3. Exact 4. Prefix wildcards (shortest first)
        Map<String, Boolean> perms = new HashMap<>();

        // Without global *, negations work
        perms.put("admin.*", true);
        perms.put("-admin.ban", true);
        assertEquals(TriState.FALSE, WildcardMatcher.check("admin.ban", perms)); // Exact negation
        assertEquals(TriState.TRUE, WildcardMatcher.check("admin.kick", perms)); // Wildcard grant

        // With global *, everything is granted
        perms.put("*", true);
        assertEquals(TriState.TRUE, WildcardMatcher.check("admin.ban", perms)); // Global * wins
        assertEquals(TriState.TRUE, WildcardMatcher.check("admin.kick", perms));
    }
}
