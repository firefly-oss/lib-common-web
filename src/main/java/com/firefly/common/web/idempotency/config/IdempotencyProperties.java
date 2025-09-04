/*
 * Copyright 2025 Firefly Software Solutions Inc
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */


package com.firefly.common.web.idempotency.config;

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

        /**
         * Hazelcast configuration properties
         */
        private Hazelcast hazelcast = new Hazelcast();

        /**
         * EhCache configuration properties
         */
        private EhCache ehcache = new EhCache();

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

        public Hazelcast getHazelcast() {
            return hazelcast;
        }

        public void setHazelcast(Hazelcast hazelcast) {
            this.hazelcast = hazelcast;
        }

        public EhCache getEhcache() {
            return ehcache;
        }

        public void setEhcache(EhCache ehcache) {
            this.ehcache = ehcache;
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

    /**
     * Hazelcast-specific configuration properties
     */
    public static class Hazelcast {

        /**
         * Whether to use Hazelcast for caching (true) or fallback to other cache (false)
         */
        private boolean enabled = false;

        /**
         * Name of the Hazelcast IMap to use for idempotency cache
         */
        private String mapName = "idempotencyCache";

        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }

        public String getMapName() {
            return mapName;
        }

        public void setMapName(String mapName) {
            this.mapName = mapName;
        }
    }

    /**
     * EhCache-specific configuration properties
     */
    public static class EhCache {

        /**
         * Whether to use EhCache for caching (true) or fallback to other cache (false)
         */
        private boolean enabled = false;

        /**
         * Name of the EhCache cache to use for idempotency
         */
        private String cacheName = "idempotencyCache";

        /**
         * Whether to enable disk persistence for the EhCache
         */
        private boolean diskPersistent = true;

        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }

        public String getCacheName() {
            return cacheName;
        }

        public void setCacheName(String cacheName) {
            this.cacheName = cacheName;
        }

        public boolean isDiskPersistent() {
            return diskPersistent;
        }

        public void setDiskPersistent(boolean diskPersistent) {
            this.diskPersistent = diskPersistent;
        }
    }
}
