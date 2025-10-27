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

import com.firefly.common.cache.core.CacheType;
import com.firefly.common.cache.manager.FireflyCacheManager;
import com.firefly.common.web.idempotency.cache.IdempotencyCache;
import com.firefly.common.web.idempotency.filter.IdempotencyWebFilter;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Diagnostic test for IdempotencyAutoConfiguration to verify bean creation conditions.
 */
class IdempotencyAutoConfigurationDiagnosticTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(IdempotencyAutoConfiguration.class));

    @Test
    void shouldLoadAutoConfigurationClass() {
        contextRunner
                .run(context -> {
                    System.out.println("=== AUTO-CONFIGURATION DIAGNOSTIC ===");
                    System.out.println("Context loaded: " + context.isRunning());
                    System.out.println("IdempotencyAutoConfiguration bean exists: " + 
                            context.containsBean("idempotencyAutoConfiguration"));
                    
                    // The auto-configuration class should always load
                    assertThat(context).hasNotFailed();
                });
    }

    @Test
    void shouldNotCreateBeansWhenFireflyCacheManagerIsMissing() {
        contextRunner
                .run(context -> {
                    System.out.println("=== TEST: No FireflyCacheManager ===");
                    System.out.println("FireflyCacheManager bean exists: " + 
                            context.containsBean("fireflyCacheManager"));
                    System.out.println("IdempotencyCache bean exists: " + 
                            context.containsBean("idempotencyCache"));
                    System.out.println("IdempotencyWebFilter bean exists: " + 
                            context.containsBean("idempotencyWebFilter"));
                    
                    assertThat(context).doesNotHaveBean(FireflyCacheManager.class);
                    assertThat(context).doesNotHaveBean(IdempotencyCache.class);
                    assertThat(context).doesNotHaveBean(IdempotencyWebFilter.class);
                });
    }

    @Test
    void shouldCreateBeansWhenFireflyCacheManagerExists() {
        contextRunner
                .withUserConfiguration(MockFireflyCacheManagerConfig.class)
                .run(context -> {
                    System.out.println("=== TEST: With FireflyCacheManager ===");
                    System.out.println("FireflyCacheManager bean exists: " + 
                            context.containsBean("fireflyCacheManager"));
                    System.out.println("IdempotencyCache bean exists: " + 
                            context.containsBean("idempotencyCache"));
                    System.out.println("IdempotencyWebFilter bean exists: " + 
                            context.containsBean("idempotencyWebFilter"));
                    
                    assertThat(context).hasSingleBean(FireflyCacheManager.class);
                    assertThat(context).hasSingleBean(IdempotencyCache.class);
                    assertThat(context).hasSingleBean(IdempotencyWebFilter.class);
                    
                    System.out.println("✅ All beans created successfully!");
                });
    }

    @Configuration
    static class MockFireflyCacheManagerConfig {
        
        @Bean
        public FireflyCacheManager fireflyCacheManager() {
            FireflyCacheManager mock = mock(FireflyCacheManager.class);
            when(mock.getCacheType()).thenReturn(CacheType.CAFFEINE);
            when(mock.getCacheName()).thenReturn("default");
            when(mock.isAvailable()).thenReturn(true);
            return mock;
        }
    }
}

