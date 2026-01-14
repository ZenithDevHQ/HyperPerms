package com.hyperperms.util;

import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

class TimeUtilTest {

    @Test
    void testParseDurationSeconds() {
        Optional<Duration> result = TimeUtil.parseDuration("30s");
        assertTrue(result.isPresent());
        assertEquals(30, result.get().toSeconds());
    }

    @Test
    void testParseDurationMinutes() {
        Optional<Duration> result = TimeUtil.parseDuration("5m");
        assertTrue(result.isPresent());
        assertEquals(5 * 60, result.get().toSeconds());
    }

    @Test
    void testParseDurationHours() {
        Optional<Duration> result = TimeUtil.parseDuration("2h");
        assertTrue(result.isPresent());
        assertEquals(2 * 60 * 60, result.get().toSeconds());
    }

    @Test
    void testParseDurationDays() {
        Optional<Duration> result = TimeUtil.parseDuration("1d");
        assertTrue(result.isPresent());
        assertEquals(24 * 60 * 60, result.get().toSeconds());
    }

    @Test
    void testParseDurationWeeks() {
        Optional<Duration> result = TimeUtil.parseDuration("1w");
        assertTrue(result.isPresent());
        assertEquals(7 * 24 * 60 * 60, result.get().toSeconds());
    }

    @Test
    void testParseDurationComplex() {
        Optional<Duration> result = TimeUtil.parseDuration("1d2h30m");
        assertTrue(result.isPresent());
        long expected = (24 + 2) * 60 * 60 + 30 * 60;
        assertEquals(expected, result.get().toSeconds());
    }

    @Test
    void testParseDurationPermanent() {
        assertTrue(TimeUtil.parseDuration("permanent").isEmpty());
        assertTrue(TimeUtil.parseDuration("perm").isEmpty());
        assertTrue(TimeUtil.parseDuration("forever").isEmpty());
    }

    @Test
    void testParseDurationInvalid() {
        assertTrue(TimeUtil.parseDuration(null).isEmpty());
        assertTrue(TimeUtil.parseDuration("").isEmpty());
        assertTrue(TimeUtil.parseDuration("   ").isEmpty());
    }

    @Test
    void testFormatDuration() {
        assertEquals("1d 2h 30m", TimeUtil.formatDuration(Duration.ofMinutes(26 * 60 + 30)));
        assertEquals("30m", TimeUtil.formatDuration(Duration.ofMinutes(30)));
        assertEquals("45s", TimeUtil.formatDuration(Duration.ofSeconds(45)));
        assertEquals("now", TimeUtil.formatDuration(Duration.ZERO));
    }

    @Test
    void testFormatDurationCompact() {
        assertEquals("1d2h30m", TimeUtil.formatDurationCompact(Duration.ofMinutes(26 * 60 + 30)));
        assertEquals("30m", TimeUtil.formatDurationCompact(Duration.ofMinutes(30)));
        assertEquals("45s", TimeUtil.formatDurationCompact(Duration.ofSeconds(45)));
        assertEquals("0s", TimeUtil.formatDurationCompact(Duration.ZERO));
    }

    @Test
    void testFormatDurationHumanReadable() {
        assertEquals("1 day, 2 hours", TimeUtil.formatDurationHumanReadable(
                Duration.ofHours(26)));
        assertEquals("30 minutes", TimeUtil.formatDurationHumanReadable(
                Duration.ofMinutes(30)));
        assertEquals("1 minute", TimeUtil.formatDurationHumanReadable(
                Duration.ofMinutes(1)));
        assertEquals("45 seconds", TimeUtil.formatDurationHumanReadable(
                Duration.ofSeconds(45)));
        assertEquals("1 second", TimeUtil.formatDurationHumanReadable(
                Duration.ofSeconds(1)));
    }

    @Test
    void testFormatExpiry() {
        assertEquals("permanent", TimeUtil.formatExpiry(null));

        // Future expiry
        Instant future = Instant.now().plusSeconds(3600);
        String formatted = TimeUtil.formatExpiry(future);
        assertTrue(formatted.startsWith("expires in"));

        // Past expiry
        Instant past = Instant.now().minusSeconds(60);
        formatted = TimeUtil.formatExpiry(past);
        assertTrue(formatted.startsWith("expired"));
    }

    @Test
    void testFormatTimeUntil() {
        Instant past = Instant.now().minusSeconds(10);
        assertEquals("expired", TimeUtil.formatTimeUntil(past));

        Instant future = Instant.now().plusSeconds(90);
        String result = TimeUtil.formatTimeUntil(future);
        assertTrue(result.contains("m") || result.contains("s"));
    }

    @Test
    void testIsExpired() {
        assertFalse(TimeUtil.isExpired(null));
        assertFalse(TimeUtil.isExpired(Instant.now().plusSeconds(60)));
        assertTrue(TimeUtil.isExpired(Instant.now().minusSeconds(60)));
    }

    @Test
    void testIsPermanent() {
        assertTrue(TimeUtil.isPermanent(null));
        assertFalse(TimeUtil.isPermanent(Instant.now()));
        assertFalse(TimeUtil.isPermanent(Instant.now().plusSeconds(60)));
    }

    @Test
    void testExpiryFromDuration() {
        Instant before = Instant.now();
        Instant expiry = TimeUtil.expiryFromDuration(Duration.ofHours(1));
        Instant after = Instant.now();

        assertTrue(expiry.isAfter(before.plusSeconds(3599)));
        assertTrue(expiry.isBefore(after.plusSeconds(3601)));
    }

    @Test
    void testDurationUntil() {
        Instant future = Instant.now().plusSeconds(100);
        Duration duration = TimeUtil.durationUntil(future);
        assertTrue(duration.toSeconds() >= 99 && duration.toSeconds() <= 101);

        Instant past = Instant.now().minusSeconds(100);
        duration = TimeUtil.durationUntil(past);
        assertTrue(duration.isNegative());
    }
}
