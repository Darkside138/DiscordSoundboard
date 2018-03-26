package net.dirtydeeds.discordsoundboard.service;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Instant;
import java.util.concurrent.TimeUnit;

public class SoundPlayerRateLimiter {

    private static final Logger LOG = LoggerFactory.getLogger(SoundPlayerRateLimiter.class);
    private static final Cache<String, Instant> userAccessTimeCache = CacheBuilder.newBuilder()
            .expireAfterWrite(2, TimeUnit.MINUTES)
            .removalListener(notification -> LOG.info("Removed from rate-limit list {}", notification.getKey()))
            .build();

    public static boolean canUserPlaySound(String user) {
        Instant instant = userAccessTimeCache.getIfPresent(user);
        if (instant == null) {
            userAccessTimeCache.put(user, Instant.now());
            LOG.info("Added to rate-limit list {}", user);
            return true;
        } else {
            LOG.info("Already on rate-limit list {}", user);
            return false;
        }
    }

}
