package io.quarkus.registry.app.services;

import javax.ws.rs.core.MediaType;

import io.quarkus.test.junit.QuarkusTest;
import org.apache.http.HttpHeaders;
import org.apache.http.HttpStatus;
import org.junit.jupiter.api.Test;

import static io.restassured.RestAssured.given;

@QuarkusTest
class RegistryServiceTest {

    @Test
    void includeLatestPlatform() {
        given()
                .formParams("groupId", "io.quarkus", "artifactId", "quarkus-bom")
                .post("/api/registry/platform").then()
                .statusCode(HttpStatus.SC_OK)
                .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON);
    }
}
