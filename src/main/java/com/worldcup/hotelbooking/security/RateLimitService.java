package com.worldcup.hotelbooking.security;

import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class RateLimitService {

    private final Map<String, Bucket> buckets = new ConcurrentHashMap<>();

    public Bucket resolveBucket(String key) {
        return buckets.computeIfAbsent(key, this::newBucket);
    }

    private Bucket newBucket(String key) {
        Bandwidth limit;

        if (key.contains("/bookings") || key.contains("/payments")) {
            limit = Bandwidth.simple(30, Duration.ofMinutes(1)); // increased from 5
        } else {
            limit = Bandwidth.simple(60, Duration.ofMinutes(1)); // increased from 20
        }

        return Bucket.builder()
                .addLimit(limit)
                .build();
    }
}
