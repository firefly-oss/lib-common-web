package com.catalis.common.web.idempotency.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Configuration properties for the idempotency module.
 * These properties can be set in application.yml or application.properties.
 * 
 * Example configuration:
 * <pre>
 * idempotency:
 *   cache:
 *     redis:
 *       enabled: false
 *     ttl-hours: 24
 *     max-entries: 10000
 * </pre>
 */
@ConfigurationProperties(prefix = "idempotency.cache")
public class IdempotencyProperties {

    /**
     * Redis configuration properties
     */
    private Redis redis = new Redis();

    /**
     * Time-to-live in hours for cached responses
     */
    private int ttlHours = 24;

    /**
     * Maximum number of entries in the in-memory cache
     */
    private int maxEntries = 10000;

    public Redis getRedis() {
        return redis;
    }

    public void setRedis(Redis redis) {
        this.redis = redis;
    }

    public int getTtlHours() {
        return ttlHours;
    }

    public void setTtlHours(int ttlHours) {
        this.ttlHours = ttlHours;
    }

    public int getMaxEntries() {
        return maxEntries;
    }

    public void setMaxEntries(int maxEntries) {
        this.maxEntries = maxEntries;
    }

    /**
     * Redis-specific configuration properties
     */
    public static class Redis {

        /**
         * Whether to use Redis for caching (true) or in-memory cache (false)
         */
        private boolean enabled = false;

        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }
    }
}
