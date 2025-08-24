package com.catalis.common.web.logging.config;

import com.catalis.common.web.logging.service.PiiMaskingService;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Auto-configuration for PII (Personally Identifiable Information) masking functionality.
 * This configuration is active when pii-masking.enabled=true (which is the default).
 * 
 * The configuration provides:
 * - PiiMaskingProperties: Configuration properties for PII patterns and masking behavior
 * - PiiMaskingService: Service that performs the actual PII detection and masking
 * 
 * Usage:
 * The PII masking functionality is automatically enabled by default. To disable it,
 * set pii-masking.enabled=false in your application configuration.
 * 
 * Configuration example:
 * <pre>
 * pii-masking:
 *   enabled: true
 *   mask-character: "*"
 *   preserve-length: true
 *   patterns:
 *     email: "\\b[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}\\b"
 *     phone: "\\b(?:\\+?1[-\\s]?)?(?:\\(?[0-9]{3}\\)?[-\\s]?)?[0-9]{3}[-\\s]?[0-9]{4}\\b"
 *   custom-patterns:
 *     internal-id: "ID-[0-9]{6}"
 * </pre>
 */
@Configuration
@AutoConfiguration
@EnableConfigurationProperties(PiiMaskingProperties.class)
@ConditionalOnProperty(name = "pii-masking.enabled", havingValue = "true", matchIfMissing = true)
public class PiiMaskingAutoConfiguration {

    /**
     * Creates the PII masking service bean.
     * This service will be used throughout the application for masking PII data in logs.
     * 
     * @param properties the PII masking configuration properties
     * @return PiiMaskingService instance configured with the provided properties
     */
    @Bean
    public PiiMaskingService piiMaskingService(PiiMaskingProperties properties) {
        return new PiiMaskingService(properties);
    }
}