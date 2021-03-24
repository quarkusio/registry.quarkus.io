package io.quarkus.registry.app.services;

import javax.ws.rs.core.MediaType;

import io.quarkus.maven.ArtifactCoords;
import io.quarkus.registry.catalog.json.JsonPlatform;
import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.junit.QuarkusTest;
import org.apache.http.HttpHeaders;
import org.apache.http.HttpStatus;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.containsString;

@QuarkusTest
@QuarkusTestResource(PostgreSQLResource.class)
@Disabled
class AdminResourceTest {

    @Test
    void platform_submitted_twice_should_conflict() throws Exception {
        JsonPlatform platform = new JsonPlatform();
        platform.setBom(ArtifactCoords.pom("io.quarkus.foo", "bar", "1.15.0.Final"));
        platform.setQuarkusCoreVersion("1.15.0.Final");
        given()
                .body(platform)
                .post("/admin/v1/platform")
                .then()
                .statusCode(HttpStatus.SC_OK)
                .header(HttpHeaders.CONTENT_TYPE, containsString(MediaType.APPLICATION_JSON));
        // Wait until the processing finishes
        Thread.sleep(3000L);
        given()
                .body(platform)
                .post("/admin/v1/platform")
                .then()
                .statusCode(HttpStatus.SC_CONFLICT);
    }
}
