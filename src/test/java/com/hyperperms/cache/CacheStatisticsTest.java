package com.hyperperms.cache;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class CacheStatisticsTest {

    private CacheStatistics stats;

    @BeforeEach
    void setUp() {
        stats = new CacheStatistics();
    }

    @Test
    void testInitialState() {
        assertEquals(0, stats.getHits());
        assertEquals(0, stats.getMisses());
        assertEquals(0, stats.getEvictions());
        assertEquals(0, stats.getInvalidations());
        assertEquals(0, stats.getTotalRequests());
        assertEquals(0.0, stats.getHitRate());
    }

    @Test
    void testRecordHit() {
        stats.recordHit();
        stats.recordHit();
        stats.recordHit();

        assertEquals(3, stats.getHits());
        assertEquals(0, stats.getMisses());
    }

    @Test
    void testRecordMiss() {
        stats.recordMiss();
        stats.recordMiss();

        assertEquals(0, stats.getHits());
        assertEquals(2, stats.getMisses());
    }

    @Test
    void testRecordEviction() {
        stats.recordEviction();
        stats.recordEvictions(5);

        assertEquals(6, stats.getEvictions());
    }

    @Test
    void testRecordInvalidation() {
        stats.recordInvalidation();
        stats.recordInvalidations(10);

        assertEquals(11, stats.getInvalidations());
    }

    @Test
    void testTotalRequests() {
        stats.recordHit();
        stats.recordHit();
        stats.recordMiss();

        assertEquals(3, stats.getTotalRequests());
    }

    @Test
    void testHitRate() {
        stats.recordHit();
        stats.recordHit();
        stats.recordHit();
        stats.recordMiss();

        assertEquals(75.0, stats.getHitRate(), 0.01);
    }

    @Test
    void testHitRateZeroRequests() {
        assertEquals(0.0, stats.getHitRate());
    }

    @Test
    void testHitRateAllHits() {
        stats.recordHit();
        stats.recordHit();

        assertEquals(100.0, stats.getHitRate(), 0.01);
    }

    @Test
    void testHitRateAllMisses() {
        stats.recordMiss();
        stats.recordMiss();

        assertEquals(0.0, stats.getHitRate(), 0.01);
    }

    @Test
    void testReset() {
        stats.recordHit();
        stats.recordMiss();
        stats.recordEviction();
        stats.recordInvalidation();

        stats.reset();

        assertEquals(0, stats.getHits());
        assertEquals(0, stats.getMisses());
        assertEquals(0, stats.getEvictions());
        assertEquals(0, stats.getInvalidations());
    }

    @Test
    void testToString() {
        stats.recordHit();
        stats.recordHit();
        stats.recordMiss();

        String str = stats.toString();
        assertTrue(str.contains("hits=2"));
        assertTrue(str.contains("misses=1"));
        assertTrue(str.contains("hitRate="));
    }

    @Test
    void testThreadSafety() throws InterruptedException {
        int iterations = 1000;
        Thread[] threads = new Thread[10];

        for (int i = 0; i < threads.length; i++) {
            threads[i] = new Thread(() -> {
                for (int j = 0; j < iterations; j++) {
                    stats.recordHit();
                    stats.recordMiss();
                }
            });
        }

        for (Thread t : threads) {
            t.start();
        }

        for (Thread t : threads) {
            t.join();
        }

        assertEquals(iterations * threads.length, stats.getHits());
        assertEquals(iterations * threads.length, stats.getMisses());
    }
}
