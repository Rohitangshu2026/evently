package com.evently;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

/**
 * Application entry point for the Evently backend — a REST API for creating
 * events, selling tickets, and validating entry via QR codes.
 */
@SpringBootApplication
@EnableJpaAuditing
@ConfigurationPropertiesScan
public class EventlyApplication {

    /**
     * Boots the Spring context and embedded web server.
     *
     * @param args standard JVM command-line arguments
     */
    public static void main(String[] args) {
        SpringApplication.run(EventlyApplication.class, args);
    }
}
