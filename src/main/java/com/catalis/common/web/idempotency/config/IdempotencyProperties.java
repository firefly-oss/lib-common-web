package com.catalis.common.web.idempotency.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Configuration properties for the idempotency module.
 * These properties can be set in application.yml or application.properties.
 * 
 * Example configuration:
 * <pre>
 * idempotency:
 *   header-name: X-Idempotency-Key
 *   cache:
 *     ttl-hours: 24
 *     max-entries: 10000
 *     redis:
 *       enabled: false
 * </pre>
 */
@ConfigurationProperties(prefix = "idempotency")
public class IdempotencyProperties {

    /**
     * Name of the HTTP header carrying the idempotency key. Default is "X-Idempotency-Key".
     */
    private String headerName = "X-Idempotency-Key";

    /**
     * Cache configuration properties
     */
    private Cache cache = new Cache();

    public String getHeaderName() {
        return headerName;
    }

    public void setHeaderName(String headerName) {
        this.headerName = headerName;
    }

    public Cache getCache() {
        return cache;
    }

    public void setCache(Cache cache) {
        this.cache = cache;
    }

    /**
     * Cache-specific configuration properties
     */
    public static class Cache {
        /**
         * Time-to-live in hours for cached responses
         */
        private int ttlHours = 24;

        /**
         * Maximum number of entries in the in-memory cache
         */
        private int maxEntries = 10000;

        /**
         * Redis configuration properties
         */
        private Redis redis = new Redis();

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

        public Redis getRedis() {
            return redis;
        }

        public void setRedis(Redis redis) {
            this.redis = redis;
        }
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
