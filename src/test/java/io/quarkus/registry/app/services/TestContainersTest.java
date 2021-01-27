package io.quarkus.registry.app.services;

import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.junit.QuarkusTest;
import org.junit.jupiter.api.Test;

@QuarkusTest
@QuarkusTestResource(PostgreSQLResource.class)
public class TestContainersTest {

    @Test
    public void test() {
        System.out.println("AKSDAS");
    }

}
