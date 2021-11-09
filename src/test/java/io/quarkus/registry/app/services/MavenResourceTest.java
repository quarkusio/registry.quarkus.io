package io.quarkus.registry.app.services;

import java.util.Map;

import javax.inject.Inject;
import javax.transaction.Transactional;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;

import io.quarkus.registry.app.maven.MavenConfig;
import io.quarkus.registry.app.model.Platform;
import io.quarkus.registry.app.model.PlatformRelease;
import io.quarkus.registry.app.model.PlatformStream;
import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.common.QuarkusTestResourceLifecycleManager;
import io.quarkus.test.junit.QuarkusTest;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static io.restassured.RestAssured.given;
import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.nullValue;

@QuarkusTest
@QuarkusTestResource(MavenResourceTest.CustomRegistryTestResource.class)
public class MavenResourceTest {
    @Inject
    MavenConfig mavenConfig;

    @BeforeAll
    @Transactional
    static void setUp() {
        {
            Platform platform = Platform.findByKey("io.quarkus.platform").get();
            PlatformStream stream20 = new PlatformStream();
            stream20.platform = platform;
            stream20.streamKey = "2.0";
            stream20.persistAndFlush();

            PlatformRelease release201 = new PlatformRelease();
            release201.platformStream = stream20;
            release201.version = "2.0.1.Final";
            release201.quarkusCoreVersion = release201.version;
            release201.persistAndFlush();

            PlatformRelease release202 = new PlatformRelease();
            release202.platformStream = stream20;
            release202.version = "2.0.2.Final";
            release202.quarkusCoreVersion = release202.version;
            release202.persistAndFlush();

            PlatformStream stream21 = new PlatformStream();
            stream21.platform = platform;
            stream21.streamKey = "2.1";
            stream21.persistAndFlush();

            PlatformRelease release210CR1 = new PlatformRelease();
            release210CR1.platformStream = stream21;
            release210CR1.version = "2.1.0.CR1";
            release210CR1.quarkusCoreVersion = release210CR1.version;
            release210CR1.persistAndFlush();
        }
    }

    @Test
    void should_return_platforms() {
        given()
                .get("/maven/foo/quarkus-platforms/1.0-SNAPSHOT/quarkus-platforms-1.0-SNAPSHOT.json")
                .then()
                .log().all(true)
                .statusCode(200);
    }


    @Test
    void should_use_custom_descriptor_settings() {
        given()
                .get("/maven/foo/quarkus-registry-descriptor/1.0-SNAPSHOT/quarkus-registry-descriptor-1.0-SNAPSHOT.json")
                .then()
                .statusCode(200)
                .log().body()
                .header(HttpHeaders.CONTENT_TYPE, containsString(MediaType.APPLICATION_JSON))
                .body("descriptor.artifact", is("foo:quarkus-registry-descriptor::json:1.0-SNAPSHOT"),
                        "platforms.artifact", is("foo:quarkus-platforms::json:1.0-SNAPSHOT"),
                        "non-platforms-extensions.artifact", is(nullValue()),
                        "quarkus-versions.recognized-versions-expression", is("[2.1.0.Final,)"),
                        "quarkus-versions.exclusive-provider", is(true),
                        "maven.repository.id", is("custom"));
    }

    @Test
    void should_return_only_matching_platforms() {
        given()
                .get("/maven/foo/quarkus-platforms/1.0-SNAPSHOT/quarkus-platforms-1.0-SNAPSHOT-2.1.0.CR1.json")
                .then()
                .statusCode(200)
                .header(HttpHeaders.CONTENT_TYPE, containsString(MediaType.APPLICATION_JSON))
                .body("platforms.streams.size()", is(1),
                        "platforms.streams[0].id[0]", is("2.1"),
                        "platforms.streams[0].releases.size()", is(1),
                        "platforms.streams[0].releases[0].quarkus-core-version[0]", is("2.1.0.CR1"));
    }

    @Test
    void should_return_404_on_inexistent_platforms() {
        given()
                .get("/maven/foo/quarkus-platforms/1.0-SNAPSHOT/quarkus-platforms-1.0-SNAPSHOT-10.1.0.CR1.json")
                .then()
                .statusCode(404)
                .header("X-Reason","No platforms found");
    }

    @Test
    void non_platform_descriptor_should_contain_quarkus_core() {
        given()
                .get("/maven/foo/quarkus-non-platform-extensions/1.0-SNAPSHOT/quarkus-non-platform-extensions-1.0-SNAPSHOT-2.1.3.Final.json")
                .then()
                .statusCode(200)
                .log().body()
                .header(HttpHeaders.CONTENT_TYPE, containsString(MediaType.APPLICATION_JSON))
                .body("quarkus-core-version", is("2.1.3.Final"));
    }

    @AfterAll
    @Transactional
    static void tearDown() {
        PlatformRelease.deleteAll();
        PlatformStream.deleteAll();
    }

    public static class CustomRegistryTestResource implements QuarkusTestResourceLifecycleManager {
        @Override
        public Map<String, String> start() {
            return Map.of("quarkus.registry.groupId", "foo",
                    "quarkus.registry.non-platform-extensions.support", "false",
                    "quarkus.registry.quarkus-versions.expression", "[2.1.0.Final,)",
                    "quarkus.registry.quarkus-versions.exclusive-provider", "true",
                    "quarkus.registry.id", "custom");
        }

        @Override public void stop() {

        }
    }

}
