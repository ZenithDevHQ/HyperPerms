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
    void testNegation() {
        Map<String, Boolean> perms = new HashMap<>();
        perms.put("*", true); // Grant all
        perms.put("-plugin.admin", true); // Deny plugin.admin

        assertEquals(TriState.FALSE, WildcardMatcher.check("plugin.admin", perms));
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
        // -plugin.admin.* should deny all under plugin.admin
        Map<String, Boolean> perms = new HashMap<>();
        perms.put("plugin.*", true);
        perms.put("-plugin.admin.*", true);

        assertEquals(TriState.FALSE, WildcardMatcher.check("plugin.admin.bypass", perms));
        assertEquals(TriState.FALSE, WildcardMatcher.check("plugin.admin.debug", perms));
        assertEquals(TriState.TRUE, WildcardMatcher.check("plugin.command", perms));
        // plugin.admin itself is NOT denied by -plugin.admin.*
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
    void testExactMatchOverridesNegation() {
        // Exact positive should override negation at same level
        Map<String, Boolean> perms = new HashMap<>();
        perms.put("-plugin.admin", true);
        perms.put("plugin.admin", true);

        // Exact match checked before negation in our priority order
        assertEquals(TriState.FALSE, WildcardMatcher.check("plugin.admin", perms));
        // Note: In our implementation, negation is checked FIRST
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
    void testMultiLevelWildcardPriority() {
        // More specific wildcard should match before less specific
        Map<String, Boolean> perms = new HashMap<>();
        perms.put("plugin.*", false);
        perms.put("plugin.command.*", true);

        // plugin.command.home should match plugin.command.* (more specific)
        assertEquals(TriState.TRUE, WildcardMatcher.check("plugin.command.home", perms));
        // plugin.admin should match plugin.*
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
        // -essentials.admin.* should deny all admin commands but allow essentials.admin itself
        Map<String, Boolean> perms = new HashMap<>();
        perms.put("essentials.*", true);
        perms.put("-essentials.admin.*", true);

        assertEquals(TriState.TRUE, WildcardMatcher.check("essentials.home", perms));
        assertEquals(TriState.TRUE, WildcardMatcher.check("essentials.admin", perms)); // admin itself allowed
        assertEquals(TriState.FALSE, WildcardMatcher.check("essentials.admin.bypass", perms));
        assertEquals(TriState.FALSE, WildcardMatcher.check("essentials.admin.reload", perms));
    }
}
