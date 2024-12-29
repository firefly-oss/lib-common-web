package com.catalis.common.web.configuration;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;

@Configuration
@AutoConfiguration
@ComponentScan(basePackages = "com.catalis.common.web.error.handler")
public class ExceptionHandlerConfiguration {
}