package io.quarkus.registry.app.services;

import static io.restassured.RestAssured.given;

import org.hamcrest.Matchers;
import org.junit.jupiter.api.Test;

import io.quarkus.registry.app.BaseTest;
import io.quarkus.registry.app.maven.MavenConfig;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;

@QuarkusTest
public class ClientConfigurationFileTest extends BaseTest {

    @Inject
    MavenConfig mavenConfig;

    @Test
    void getClientConfigurationYaml() {
        given()
                .get("/client/config.yaml")
                .prettyPeek()
                .then()
                .contentType("text/plain")
                .body(Matchers.containsString(
                        """
                                    maven:
                                      repository:
                                        url: "%1s"
                                """.formatted(mavenConfig.getRegistryUrl())))
                .statusCode(200);
    }
}
