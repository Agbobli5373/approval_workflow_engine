package com.isaac.approvalworkflowengine.auth.ratelimit;

import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.stereotype.Service;

@Service
public class InMemoryRateLimiterService {

    private final Map<String, WindowCounter> counters = new ConcurrentHashMap<>();

    public synchronized RateLimitDecision consume(String key, int limit, long windowSeconds, Instant now) {
        long epochSeconds = now.getEpochSecond();
        long windowStart = (epochSeconds / windowSeconds) * windowSeconds;

        WindowCounter counter = counters.get(key);
        if (counter == null || counter.windowStartEpochSeconds != windowStart) {
            counter = new WindowCounter(windowStart, 0);
            counters.put(key, counter);
        }

        counter.count += 1;

        if (counter.count <= limit) {
            evictExpired(windowStart - windowSeconds);
            return RateLimitDecision.allow();
        }

        long retryAfterSeconds = (counter.windowStartEpochSeconds + windowSeconds) - epochSeconds;
        return RateLimitDecision.reject(retryAfterSeconds);
    }

    private void evictExpired(long minimumWindowStart) {
        counters.entrySet().removeIf(entry -> entry.getValue().windowStartEpochSeconds < minimumWindowStart);
    }

    private static final class WindowCounter {

        private final long windowStartEpochSeconds;
        private int count;

        private WindowCounter(long windowStartEpochSeconds, int count) {
            this.windowStartEpochSeconds = windowStartEpochSeconds;
            this.count = count;
        }
    }
}
