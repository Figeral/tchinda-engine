package com.ubuntux.tchinda.redis;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.ubuntux.tchinda.model.Event;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;

@Slf4j
@Service
public class IdempotencyService {

    private final Cache<String, Boolean> eventCache;
    private static final String CACHE_PREFIX = "event:fp:";
    private static final Duration TTL = Duration.ofDays(120);

    public IdempotencyService() {
        this.eventCache = Caffeine.newBuilder()
                .expireAfterWrite(TTL)
                .maximumSize(100_000)
                .build();
    }

    /**
     * Tries to lock the event hash.
     * 
     * @return true if it was newly added (i.e. is unique), false if it already
     *         exists (duplicate).
     */
    public boolean isUniqueAndCache(Event event) {
        String hash = generateFingerprint(event.getTitle(), event.getUrl());
        event.setFingerprint(hash);

        String key = CACHE_PREFIX + hash;

        Boolean previousValue = eventCache.asMap().putIfAbsent(key, Boolean.TRUE);
        return previousValue == null;
    }

    private String generateFingerprint(String title, String url) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            String input = title + url;
            byte[] hash = digest.digest(input.getBytes());
            return bytesToHex(hash);
        } catch (NoSuchAlgorithmException e) {
            log.error("Unable to find SHA-256 algorithm", e);
            // Fallback for POC
            return String.valueOf((title + url).hashCode());
        }
    }

    private String bytesToHex(byte[] bytes) {
        StringBuilder hexString = new StringBuilder(2 * bytes.length);
        for (byte b : bytes) {
            String hex = Integer.toHexString(0xff & b);
            if (hex.length() == 1) {
                hexString.append('0');
            }
            hexString.append(hex);
        }
        return hexString.toString();
    }
}
