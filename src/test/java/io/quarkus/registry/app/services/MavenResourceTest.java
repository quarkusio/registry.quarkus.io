package io.quarkus.registry.app.services;

import static io.restassured.RestAssured.given;
import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.is;

import java.net.HttpURLConnection;

import javax.transaction.Transactional;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import io.quarkus.registry.app.BaseTest;
import io.quarkus.registry.app.model.Platform;
import io.quarkus.registry.app.model.PlatformRelease;
import io.quarkus.registry.app.model.PlatformStream;
import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;

@QuarkusTest
public class MavenResourceTest extends BaseTest {

    @BeforeEach
    @Transactional
    void setUp() {
        {
            Platform platform = Platform.findByKey("io.quarkus.platform").get();

            PlatformStream stream18 = new PlatformStream();
            stream18.platform = platform;
            stream18.streamKey = "1.8";
            stream18.persistAndFlush();

            PlatformRelease release180 = new PlatformRelease();
            release180.platformStream = stream18;
            release180.version = "1.8.0.Final";
            release180.quarkusCoreVersion = release180.version;
            release180.bom = "io.quarkus.platform:quarkus-bom::pom:1.8.0.Final";
            release180.persistAndFlush();

            PlatformStream stream19 = new PlatformStream();
            stream19.platform = platform;
            stream19.streamKey = "1.9";
            stream19.persistAndFlush();

            PlatformRelease release190 = new PlatformRelease();
            release190.platformStream = stream19;
            release190.version = "1.9.0.Final";
            release190.quarkusCoreVersion = release190.version;
            release190.bom = "io.quarkus.platform:quarkus-bom::pom:1.9.0.Final";
            release190.persistAndFlush();

            PlatformStream stream20 = new PlatformStream();
            stream20.platform = platform;
            stream20.streamKey = "2.0";
            stream20.persistAndFlush();

            PlatformRelease release201 = new PlatformRelease();
            release201.platformStream = stream20;
            release201.version = "2.0.1.Final";
            release201.quarkusCoreVersion = release201.version;
            release201.bom = "io.quarkus.platform:quarkus-bom::pom:2.0.1.Final";
            release201.persistAndFlush();

            PlatformRelease release202 = new PlatformRelease();
            release202.platformStream = stream20;
            release202.version = "2.0.2.Final";
            release202.quarkusCoreVersion = release202.version;
            release202.bom = "io.quarkus.platform:quarkus-bom::pom:2.0.2.Final";
            release202.persistAndFlush();

            PlatformStream stream21 = new PlatformStream();
            stream21.platform = platform;
            stream21.streamKey = "2.1";
            stream21.persistAndFlush();

            PlatformRelease release210CR1 = new PlatformRelease();
            release210CR1.platformStream = stream21;
            release210CR1.version = "2.1.0.CR1";
            release210CR1.quarkusCoreVersion = release210CR1.version;
            release210CR1.bom = "io.quarkus.platform:quarkus-bom::pom:2.1.0.CR1";
            release210CR1.persistAndFlush();
        }
    }

    @Test
    void should_return_platforms() {
        given()
                .get("/maven/io/quarkus/registry/quarkus-platforms/1.0-SNAPSHOT/quarkus-platforms-1.0-SNAPSHOT.json")
                .then()
                .statusCode(200)
                .log().ifValidationFails()
                .contentType(ContentType.JSON)
                .body("platforms.streams.flatten().size()", is(3),
                        "platforms[0].streams[0].id", is("2.0"),
                        "platforms[0].streams[0].releases[0].quarkus-core-version", is("2.0.2.Final"),
                        "platforms[0].streams[1].id", is("1.9"),
                        "platforms[0].streams[1].releases[0].quarkus-core-version", is("1.9.0.Final"),
                        "platforms[0].streams[2].id", is("2.1"),
                        "platforms[0].streams[2].releases[0].quarkus-core-version", is("2.1.0.CR1"));
    }

    @Test
    void should_return_all_platforms() {
        given()
                .get("/maven/io/quarkus/registry/quarkus-platforms/1.0-SNAPSHOT/quarkus-platforms-1.0-SNAPSHOT-all.json")
                .then()
                .statusCode(200)
                .contentType(ContentType.JSON)
                .body("platforms.streams.size()", is(1),
                        "platforms.streams[0].id[0]", is("2.0"),
                        "platforms.streams[0].releases.size()", is(4),
                        "platforms.streams[0].releases[0].quarkus-core-version[0]", is("2.0.2.Final"),
                        "platforms.streams[0].releases[1].quarkus-core-version[0]", is("1.9.0.Final"),
                        "platforms.streams[0].releases[2].quarkus-core-version[0]", is("1.8.0.Final"),
                        "platforms.streams[0].releases[3].quarkus-core-version[0]", is("2.1.0.CR1"));
    }

    @Test
    void should_return_only_matching_platforms() {
        given()
                .get("/maven/io/quarkus/registry/quarkus-platforms/1.0-SNAPSHOT/quarkus-platforms-1.0-SNAPSHOT-2.1.0.CR1.json")
                .then()
                .statusCode(200)
                .header(HttpHeaders.CONTENT_TYPE, containsString(MediaType.APPLICATION_JSON))
                .body("platforms.streams.size()", is(1),
                        "platforms.streams[0].id[0]", is("2.1"),
                        "platforms.streams[0].releases.size()", is(1),
                        "platforms.streams[0].releases[0].quarkus-core-version[0]", is("2.1.0.CR1"));
    }

    @Test
    void should_return_204_on_inexistent_platforms() {
        given()
                .get("/maven/io/quarkus/registry/quarkus-platforms/1.0-SNAPSHOT/quarkus-platforms-1.0-SNAPSHOT-10.1.0.CR1.json")
                .then()
                .statusCode(204)
                .header("X-Reason", "No platforms found");
    }

    @Test
    void non_platform_descriptor_should_contain_quarkus_core() {
        given()
                .get("/maven/io/quarkus/registry/quarkus-non-platform-extensions/1.0-SNAPSHOT/quarkus-non-platform-extensions-1.0-SNAPSHOT-2.1.3.Final.json")
                .then()
                .statusCode(200)
                .header(HttpHeaders.CONTENT_TYPE, containsString(MediaType.APPLICATION_JSON))
                .body("quarkus-core-version", is("2.1.3.Final"));
    }

    @Test
    void should_return_bad_request_if_version_is_invalid() {
        given()
                .get("/maven/io/quarkus/registry/quarkus-platforms/1.0-SNAPSHOT/quarkus-platforms-1.0-SNAPSHOT-0.23.2.json")
                .then()
                .statusCode(HttpURLConnection.HTTP_BAD_REQUEST);
    }
}
