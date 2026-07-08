package com.evently;

import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

/**
 * Base class for integration tests that need the full Spring context wired to a
 * real PostgreSQL instance (not H2), so Flyway migrations, JPA locking, and
 * transaction semantics behave exactly as in production.
 * <p>
 * Tests run against an isolated {@code evently_test} database on the local
 * compose Postgres (port 55432) so they never touch dev data.
 * <p>
 * <strong>Note:</strong> the idiomatic choice here is Testcontainers, and the
 * commented approach below is what CI/most machines should use. It is disabled
 * on this workstation because the installed Docker Engine (29.x, API 1.52) is
 * newer than the bundled docker-java client can negotiate. Swap back to
 * Testcontainers on a standard Docker environment.
 * <p>
 * Uses a MOCK web environment (no real server) so the servlet-scoped security
 * beans still load while tests exercise the service and persistence layers.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
public abstract class AbstractIntegrationTest {

    /**
     * Points the datasource at the isolated {@code evently_test} database before
     * the context loads.
     *
     * @param registry Spring's dynamic property registry
     */
    @DynamicPropertySource
    static void datasourceProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", () -> "jdbc:postgresql://localhost:55432/evently_test");
        registry.add("spring.datasource.username", () -> "evently");
        registry.add("spring.datasource.password", () -> "evently");
    }

    // --- Preferred Testcontainers wiring (enable on standard Docker) ---
    //
    // @SuppressWarnings("resource")
    // static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:16");
    // static { POSTGRES.start(); }
    //
    // @DynamicPropertySource
    // static void datasourceProperties(DynamicPropertyRegistry registry) {
    //     registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
    //     registry.add("spring.datasource.username", POSTGRES::getUsername);
    //     registry.add("spring.datasource.password", POSTGRES::getPassword);
    // }
}
