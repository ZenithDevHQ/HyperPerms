package com.hyperperms.cache;

import java.util.concurrent.atomic.AtomicLong;

/**
 * Thread-safe cache statistics tracker.
 */
public final class CacheStatistics {

    private final AtomicLong hits = new AtomicLong(0);
    private final AtomicLong misses = new AtomicLong(0);
    private final AtomicLong evictions = new AtomicLong(0);
    private final AtomicLong invalidations = new AtomicLong(0);

    /**
     * Records a cache hit.
     */
    public void recordHit() {
        hits.incrementAndGet();
    }

    /**
     * Records a cache miss.
     */
    public void recordMiss() {
        misses.incrementAndGet();
    }

    /**
     * Records an eviction.
     */
    public void recordEviction() {
        evictions.incrementAndGet();
    }

    /**
     * Records multiple evictions.
     *
     * @param count the number of evictions
     */
    public void recordEvictions(long count) {
        evictions.addAndGet(count);
    }

    /**
     * Records an invalidation.
     */
    public void recordInvalidation() {
        invalidations.incrementAndGet();
    }

    /**
     * Records multiple invalidations.
     *
     * @param count the number of invalidations
     */
    public void recordInvalidations(long count) {
        invalidations.addAndGet(count);
    }

    /**
     * Gets the number of cache hits.
     *
     * @return the hit count
     */
    public long getHits() {
        return hits.get();
    }

    /**
     * Gets the number of cache misses.
     *
     * @return the miss count
     */
    public long getMisses() {
        return misses.get();
    }

    /**
     * Gets the number of evictions.
     *
     * @return the eviction count
     */
    public long getEvictions() {
        return evictions.get();
    }

    /**
     * Gets the number of invalidations.
     *
     * @return the invalidation count
     */
    public long getInvalidations() {
        return invalidations.get();
    }

    /**
     * Gets the total number of requests (hits + misses).
     *
     * @return the total request count
     */
    public long getTotalRequests() {
        return hits.get() + misses.get();
    }

    /**
     * Gets the cache hit rate as a percentage.
     *
     * @return the hit rate (0.0 to 100.0), or 0.0 if no requests
     */
    public double getHitRate() {
        long total = getTotalRequests();
        if (total == 0) {
            return 0.0;
        }
        return (hits.get() * 100.0) / total;
    }

    /**
     * Resets all statistics to zero.
     */
    public void reset() {
        hits.set(0);
        misses.set(0);
        evictions.set(0);
        invalidations.set(0);
    }

    @Override
    public String toString() {
        return String.format(
                "CacheStatistics{hits=%d, misses=%d, hitRate=%.1f%%, evictions=%d, invalidations=%d}",
                getHits(), getMisses(), getHitRate(), getEvictions(), getInvalidations()
        );
    }
}
