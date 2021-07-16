package io.quarkus.registry.app.services;

import java.io.IOException;

import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;

import io.quarkus.registry.app.maven.HashUtil;
import io.quarkus.test.junit.QuarkusTest;
import org.junit.jupiter.api.Test;

import static io.restassured.RestAssured.given;
import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.equalTo;

@QuarkusTest
public class MetadataChecksumTest {

    @Test
    public void should_match_checksum() throws Exception {
        String metadata = given()
                .get("/maven/io/quarkus/registry/quarkus-platforms/1.0-SNAPSHOT/maven-metadata.xml")
                .then()
                .statusCode(200)
                .header(HttpHeaders.CONTENT_TYPE, containsString(MediaType.APPLICATION_XML))
                .extract().body().asString();
        // Wait a bit before requesting again
        Thread.sleep(1000);
        String expectedSha1 = HashUtil.sha1(metadata);
        given()
                .get("/maven/io/quarkus/registry/quarkus-platforms/1.0-SNAPSHOT/maven-metadata.xml.sha1")
                .then()
                .statusCode(200)
                .log().body(true)
                .header(HttpHeaders.CONTENT_TYPE, containsString(MediaType.TEXT_PLAIN))
                .body(equalTo(expectedSha1));
    }

}
