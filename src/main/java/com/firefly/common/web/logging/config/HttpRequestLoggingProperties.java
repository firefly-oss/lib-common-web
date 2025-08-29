package com.firefly.common.web.logging.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

/**
 * Configuration properties for HTTP request logging.
 * These properties can be set in application.yml or application.properties.
 * 
 * Example configuration:
 * <pre>
 * http-request-logging:
 *   enabled: true
 *   include-headers: true
 *   include-request-body: false
 *   include-response-body: false
 *   max-body-size: 1024
 *   sensitive-headers:
 *     - authorization
 *     - x-api-key
 *   excluded-paths:
 *     - /health
 *     - /actuator/**
 * </pre>
 */
@ConfigurationProperties(prefix = "http-request-logging")
public class HttpRequestLoggingProperties {

    /**
     * Whether HTTP request logging is enabled
     */
    private boolean enabled = true;

    /**
     * Whether to include request and response headers in logs
     */
    private boolean includeHeaders = true;

    /**
     * Whether to include request body in logs
     */
    private boolean includeRequestBody = false;

    /**
     * Whether to include response body in logs
     */
    private boolean includeResponseBody = false;

    /**
     * Maximum size of request/response body to log (in bytes)
     */
    private int maxBodySize = 1024;

    /**
     * Headers to mask or exclude from logging for security
     */
    private Set<String> sensitiveHeaders = new HashSet<>(Arrays.asList(
            "authorization", "x-api-key", "x-auth-token", "cookie", "set-cookie"));

    /**
     * URL paths to exclude from logging
     */
    private Set<String> excludedPaths = new HashSet<>(Arrays.asList(
            "/health", "/actuator/**", "/metrics", "/info"));

    /**
     * Log level for HTTP request logging (DEBUG, INFO, WARN, ERROR)
     */
    private String logLevel = "INFO";

    /**
     * Whether to include query parameters in logs
     */
    private boolean includeQueryParams = true;

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public boolean isIncludeHeaders() {
        return includeHeaders;
    }

    public void setIncludeHeaders(boolean includeHeaders) {
        this.includeHeaders = includeHeaders;
    }

    public boolean isIncludeRequestBody() {
        return includeRequestBody;
    }

    public void setIncludeRequestBody(boolean includeRequestBody) {
        this.includeRequestBody = includeRequestBody;
    }

    public boolean isIncludeResponseBody() {
        return includeResponseBody;
    }

    public void setIncludeResponseBody(boolean includeResponseBody) {
        this.includeResponseBody = includeResponseBody;
    }

    public int getMaxBodySize() {
        return maxBodySize;
    }

    public void setMaxBodySize(int maxBodySize) {
        this.maxBodySize = maxBodySize;
    }

    public Set<String> getSensitiveHeaders() {
        return sensitiveHeaders;
    }

    public void setSensitiveHeaders(Set<String> sensitiveHeaders) {
        this.sensitiveHeaders = sensitiveHeaders;
    }

    public Set<String> getExcludedPaths() {
        return excludedPaths;
    }

    public void setExcludedPaths(Set<String> excludedPaths) {
        this.excludedPaths = excludedPaths;
    }

    public String getLogLevel() {
        return logLevel;
    }

    public void setLogLevel(String logLevel) {
        this.logLevel = logLevel;
    }

    public boolean isIncludeQueryParams() {
        return includeQueryParams;
    }

    public void setIncludeQueryParams(boolean includeQueryParams) {
        this.includeQueryParams = includeQueryParams;
    }
}