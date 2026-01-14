package com.hyperperms.model;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class TrackTest {

    @Test
    void testEmptyTrack() {
        Track track = new Track("staff");
        assertEquals("staff", track.getName());
        assertTrue(track.isEmpty());
        assertEquals(0, track.size());
    }

    @Test
    void testTrackWithGroups() {
        Track track = new Track("staff", List.of("helper", "mod", "admin"));

        assertEquals(3, track.size());
        assertFalse(track.isEmpty());
        assertTrue(track.containsGroup("helper"));
        assertTrue(track.containsGroup("mod"));
        assertTrue(track.containsGroup("admin"));
        assertFalse(track.containsGroup("member"));
    }

    @Test
    void testGetNextGroup() {
        Track track = new Track("staff", List.of("helper", "mod", "admin"));

        assertEquals("mod", track.getNextGroup("helper"));
        assertEquals("admin", track.getNextGroup("mod"));
        assertNull(track.getNextGroup("admin")); // No next group
        assertNull(track.getNextGroup("member")); // Not in track
    }

    @Test
    void testGetPreviousGroup() {
        Track track = new Track("staff", List.of("helper", "mod", "admin"));

        assertNull(track.getPreviousGroup("helper")); // No previous
        assertEquals("helper", track.getPreviousGroup("mod"));
        assertEquals("mod", track.getPreviousGroup("admin"));
        assertNull(track.getPreviousGroup("member")); // Not in track
    }

    @Test
    void testFirstAndLastGroup() {
        Track track = new Track("staff", List.of("helper", "mod", "admin"));

        assertEquals("helper", track.getFirstGroup());
        assertEquals("admin", track.getLastGroup());

        Track empty = new Track("empty");
        assertNull(empty.getFirstGroup());
        assertNull(empty.getLastGroup());
    }

    @Test
    void testAppendGroup() {
        Track track = new Track("staff");

        assertTrue(track.appendGroup("helper"));
        assertTrue(track.appendGroup("mod"));
        assertFalse(track.appendGroup("helper")); // Already exists

        assertEquals(List.of("helper", "mod"), track.getGroups());
    }

    @Test
    void testInsertGroup() {
        Track track = new Track("staff", List.of("helper", "admin"));

        assertTrue(track.insertGroup(1, "mod"));
        assertEquals(List.of("helper", "mod", "admin"), track.getGroups());

        assertFalse(track.insertGroup(0, "mod")); // Already exists
    }

    @Test
    void testRemoveGroup() {
        Track track = new Track("staff", List.of("helper", "mod", "admin"));

        assertTrue(track.removeGroup("mod"));
        assertFalse(track.containsGroup("mod"));
        assertEquals(List.of("helper", "admin"), track.getGroups());

        assertFalse(track.removeGroup("mod")); // Already removed
    }

    @Test
    void testCaseInsensitivity() {
        Track track = new Track("STAFF", List.of("Helper", "MOD", "admin"));

        assertEquals("staff", track.getName());
        assertTrue(track.containsGroup("HELPER"));
        assertTrue(track.containsGroup("helper"));
        assertEquals("mod", track.getNextGroup("HELPER"));
    }
}
