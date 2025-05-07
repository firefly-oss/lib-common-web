package com.catalis.common.web.openapi;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.core.env.Profiles;

import java.util.ArrayList;
import java.util.List;

/**
 * Configuration class for OpenAPI documentation.
 * This class configures the OpenAPI documentation for the application,
 * including information about the API, contact details, license, servers.
 */
@Configuration
public class OpenAPIConfiguration {
    @Value("${spring.application.name:Service}")
    private String applicationName;

    @Value("${spring.application.version:1.0.0}")
    private String applicationVersion;

    @Value("${spring.application.description:${spring.application.name} API Documentation}")
    private String applicationDescription;

    @Value("${spring.application.team.name:Development Team}")
    private String teamName;

    @Value("${spring.application.team.email:developers@getfirefly.io}")
    private String teamEmail;

    @Value("${spring.application.team.url:https://getfirefly.io}")
    private String teamUrl;

    @Value("${spring.application.license.name:Apache 2.0}")
    private String licenseName;

    @Value("${spring.application.license.url:https://www.apache.org/licenses/LICENSE-2.0}")
    private String licenseUrl;

    @Value("${server.servlet.context-path:}")
    private String contextPath;

    @Value("${server.port:8080}")
    private String serverPort;

    @Value("${openapi.servers.enabled:true}")
    private boolean serversEnabled;

    private final Environment environment;

    /**
     * Creates a new OpenAPIConfiguration with the given environment.
     *
     * @param environment the Spring environment
     */
    public OpenAPIConfiguration(Environment environment) {
        this.environment = environment;
    }

    /**
     * Creates and configures the OpenAPI documentation for the application.
     * This method sets up the API information, servers
     * based on the application configuration.
     *
     * @return the configured OpenAPI instance
     */
    @Bean
    public OpenAPI customOpenAPI() {
        OpenAPI openAPI = new OpenAPI()
                .info(new Info()
                        .title(applicationName + " API")
                        .version(applicationVersion)
                        .description(applicationDescription)
                        .contact(new Contact()
                                .name(teamName)
                                .email(teamEmail)
                                .url(teamUrl))
                        .license(new License()
                                .name(licenseName)
                                .url(licenseUrl)));

        // Add servers configuration if enabled
        if (serversEnabled) {
            addServers(openAPI);
        }

        return openAPI;
    }

    /**
     * Adds server configurations to the OpenAPI documentation.
     * Automatically detects the current environment and adds appropriate servers.
     *
     * @param openAPI the OpenAPI instance to configure
     */
    private void addServers(OpenAPI openAPI) {
        List<Server> servers = new ArrayList<>();

        // Add local development server
        if (environment.acceptsProfiles(Profiles.of("default", "local", "dev"))) {
            servers.add(new Server()
                    .url("http://localhost:" + serverPort + contextPath)
                    .description("Local Development Server"));
        }

        // Add development server
        if (environment.acceptsProfiles(Profiles.of("dev"))) {
            servers.add(new Server()
                    .url("https://dev-api.catalis.com" + contextPath)
                    .description("Development Server"));
        }

        // Add staging server
        if (environment.acceptsProfiles(Profiles.of("staging"))) {
            servers.add(new Server()
                    .url("https://staging-api.catalis.com" + contextPath)
                    .description("Staging Server"));
        }

        // Add production server
        if (environment.acceptsProfiles(Profiles.of("prod"))) {
            servers.add(new Server()
                    .url("https://api.catalis.com" + contextPath)
                    .description("Production Server"));
        }

        if (!servers.isEmpty()) {
            openAPI.servers(servers);
        }
    }

}
