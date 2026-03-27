package com.worldcup.hotelbooking.security;

import io.github.bucket4j.*;
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
            limit = Bandwidth.simple(5, Duration.ofMinutes(1)); // stricter limit
        } else {
            limit = Bandwidth.simple(20, Duration.ofMinutes(1)); // looser limit
        }

        return Bucket.builder()
                .addLimit(limit)
                .build();
    }
}
