package io.quarkus.registry.app;

import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;

import jakarta.inject.Inject;

public abstract class BaseTest {

    @Inject
    Flyway flyway;

    @BeforeEach
    void migrate() {
        flyway.migrate();
    }

    @AfterEach
    void clean() {
        flyway.clean();
    }
}
