package com.ubuntux.tchinda.redis;

import com.ubuntux.tchinda.model.Event;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;

@Slf4j
@Service
@RequiredArgsConstructor
public class IdempotencyService {

    private final StringRedisTemplate redisTemplate;
    private static final String REDIS_PREFIX = "event:fp:";
    private static final Duration TTL = Duration.ofDays(120);

    /**
     * Tries to lock the event hash.
     * 
     * @return true if it was newly added (i.e. is unique), false if it already
     *         exists (duplicate).
     */
    public boolean isUniqueAndCache(Event event) {
        String hash = generateFingerprint(event.getTitle(), event.getUrl());
        event.setFingerprint(hash);

        String key = REDIS_PREFIX + hash;

        Boolean success = redisTemplate.opsForValue().setIfAbsent(key, "1", TTL);
        // If success is null, interpret as false (could happen depending on Redis
        // driver).
        return Boolean.TRUE.equals(success);
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
