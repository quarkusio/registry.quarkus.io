package io.quarkus.registry.app.services;

import javax.ws.rs.core.MediaType;

import io.quarkus.test.junit.QuarkusTest;
import org.apache.http.HttpHeaders;
import org.apache.http.HttpStatus;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.containsString;

@QuarkusTest
@Disabled
class RegistryServiceTest {

    @Test
    void includeLatestPlatform() {
        given()
                .formParams("groupId", "io.quarkus",
                        "artifactId", "quarkus-bom",
                        "version", "1.11.0.Final")
                .post("/api/registry/platform").then()
                .statusCode(HttpStatus.SC_OK)
                .header(HttpHeaders.CONTENT_TYPE, containsString(MediaType.APPLICATION_JSON));

        given()
                .formParams("groupId", "io.quarkus",
                        "artifactId", "quarkus-bom",
                        "version", "1.11.0.Final")
                .post("/api/registry/platform").then()
                .statusCode(HttpStatus.SC_OK)
                .header(HttpHeaders.CONTENT_TYPE, containsString(MediaType.APPLICATION_JSON));

    }
}
