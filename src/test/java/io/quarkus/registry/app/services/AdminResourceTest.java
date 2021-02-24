package io.quarkus.registry.app.services;

import javax.ws.rs.core.MediaType;

import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.junit.QuarkusTest;
import org.apache.http.HttpHeaders;
import org.apache.http.HttpStatus;
import org.junit.jupiter.api.Test;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.containsString;

@QuarkusTest
@QuarkusTestResource(PostgreSQLResource.class)
class AdminResourceTest {

    @Test
    void platform_submitted_twice_should_conflict() throws Exception {
        given()
                .formParams("groupId", "io.quarkus",
                        "artifactId", "quarkus-bom",
                        "version", "1.11.0.Final")
                .post("/admin/api/v1/platform")
                .then()
                .statusCode(HttpStatus.SC_OK)
                .header(HttpHeaders.CONTENT_TYPE, containsString(MediaType.APPLICATION_JSON));
        // Wait until the processing finishes
        Thread.sleep(5000L);
        given()
                .formParams("groupId", "io.quarkus",
                        "artifactId", "quarkus-bom",
                        "version", "1.11.0.Final")
                .post("/admin/api/v1/platform")
                .then()
                .statusCode(HttpStatus.SC_CONFLICT);

    }
}
