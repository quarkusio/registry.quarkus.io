package io.quarkus.registry.app;

import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;

import io.quarkus.registry.app.maven.cache.MavenCache;
import jakarta.inject.Inject;

public abstract class BaseTest {

    @Inject
    Flyway flyway;

    @Inject
    MavenCache mavenCache;

    @BeforeEach
    void migrate() {
        flyway.migrate();
        // The Maven cache is application scoped, so it outlives the database it was populated from
        mavenCache.clear();
    }

    @AfterEach
    void clean() {
        flyway.clean();
    }
}
