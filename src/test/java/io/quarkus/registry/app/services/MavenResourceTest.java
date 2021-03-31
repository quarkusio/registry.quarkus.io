package io.quarkus.registry.app.services;

import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.junit.QuarkusTest;

@QuarkusTest
@QuarkusTestResource(PostgreSQLResource.class)
public class MavenResourceTest {
}
