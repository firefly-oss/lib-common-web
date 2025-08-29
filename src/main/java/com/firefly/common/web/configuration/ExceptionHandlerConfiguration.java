package com.firefly.common.web.configuration;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;


/**
 * Configuration class for exception handling.
 * This class provides Spring auto-configuration for the global exception handler
 * and exception conversion used throughout the application.
 */
@Configuration
@AutoConfiguration
@ComponentScan(basePackages = {
        "com.firefly.common.web.error.handler",
        "com.firefly.common.web.error.converter"
})
public class ExceptionHandlerConfiguration {

    /**
     * Creates a new ExceptionHandlerConfiguration.
     */
    public ExceptionHandlerConfiguration() {
        // Default constructor
    }
}