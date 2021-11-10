package io.quarkus.registry.app;

import javax.transaction.Transactional;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import io.quarkus.registry.app.model.Platform;
import io.quarkus.registry.app.model.PlatformRelease;
import io.quarkus.registry.app.model.PlatformStream;
import io.quarkus.test.junit.QuarkusTest;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static io.restassured.RestAssured.given;

@QuarkusTest
class DatabaseRegistryClientTest {

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
    void should_return_no_content_if_no_platforms_found() {
        given()
                .get("/client/platforms?v=1.1.0")
                .then()
                .statusCode(Response.Status.NO_CONTENT.getStatusCode());
    }

    @Test
    void should_return_application_json_as_response_type() throws Exception {
        given()
                .accept("application/json;custom=aaaaaaaaaaaaaaaaaa;charset=utf-7")
                .get("/client/platforms")
                .then()
                .statusCode(Response.Status.OK.getStatusCode())
                .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON);
    }

    @Test
    void should_return_unacceptable_on_invalid_accept_headers() {
        given()
                .accept(MediaType.APPLICATION_XML)
                .get("/client/platforms")
                .then()
                .statusCode(Response.Status.NOT_ACCEPTABLE.getStatusCode());
    }

    @AfterAll
    @Transactional
    static void tearDown() {
        PlatformRelease.deleteAll();
        PlatformStream.deleteAll();
    }

}