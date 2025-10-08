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
        "com.firefly.common.web.error.converter",
        "com.firefly.common.web.error.config",
        "com.firefly.common.web.error.service"
})
public class ExceptionHandlerConfiguration {

    /**
     * Creates a new ExceptionHandlerConfiguration.
     */
    public ExceptionHandlerConfiguration() {
        // Default constructor
    }
}