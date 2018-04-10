package net.dirtydeeds.discordsoundboard.service;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Instant;
import java.util.concurrent.TimeUnit;

public class SoundPlayerRateLimiter {

    private static final Logger LOG = LoggerFactory.getLogger(SoundPlayerRateLimiter.class);
    private static final int kSecondsToRestrict = 90;
    private static final Cache<String, Instant> userAccessTimeCache = CacheBuilder.newBuilder()
            .expireAfterWrite(kSecondsToRestrict, TimeUnit.SECONDS)
            .removalListener(notification -> LOG.info("Removed from rate-limit list {}", notification.getKey()))
            .build();

    public static boolean userIsRateLimited(String user) {
        final Instant instant = userAccessTimeCache.getIfPresent(user);
        if (instant == null) {
            userAccessTimeCache.put(user, Instant.now());
            return false;
        } else {
            LOG.info("User {} is rate-limited until {}", user, instant.plusSeconds(kSecondsToRestrict));
            return true;
        }
    }

}
