package io.quarkus.registry.app.services;

import javax.inject.Inject;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;

import io.quarkus.registry.app.maven.MavenConfig;
import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.junit.QuarkusTest;
import org.junit.jupiter.api.Test;

import static io.restassured.RestAssured.given;
import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.is;

@QuarkusTest
@QuarkusTestResource(MavenResourceTest.CustomRegistryTestResource.class)
public class MavenMetadataResourceTest {

    @Inject
    MavenConfig mavenConfig;

    @Test
    void should_support_repository_proxied_as_nexus_repository() {
        given()
                .get("/maven/.meta/repository-metadata.xml")
                .then()
                .statusCode(200)
                .header(HttpHeaders.CONTENT_TYPE, containsString(MediaType.APPLICATION_XML));

        given()
                .get("/maven/.meta/repository-metadata.xml.sha1")
                .then()
                .statusCode(200)
                .header(HttpHeaders.CONTENT_TYPE, containsString(MediaType.TEXT_PLAIN));

        given()
                .get("/maven/.meta/prefixes.txt")
                .then()
                .statusCode(200)
                .header(HttpHeaders.CONTENT_TYPE, containsString(MediaType.TEXT_PLAIN))
                .body(is("/" + mavenConfig.getRegistryGroupId().replace('.', '/')));
    }

}
