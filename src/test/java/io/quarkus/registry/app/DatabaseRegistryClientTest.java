package io.quarkus.registry.app;

import static io.restassured.RestAssured.given;
import static org.hamcrest.CoreMatchers.hasItems;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.collection.IsCollectionWithSize.hasSize;

import java.net.HttpURLConnection;

import javax.transaction.Transactional;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import io.quarkus.registry.app.model.Extension;
import io.quarkus.registry.app.model.ExtensionRelease;
import io.quarkus.registry.app.model.ExtensionReleaseCompatibility;
import io.quarkus.registry.app.model.Platform;
import io.quarkus.registry.app.model.PlatformExtension;
import io.quarkus.registry.app.model.PlatformRelease;
import io.quarkus.registry.app.model.PlatformStream;
import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;

@QuarkusTest
class DatabaseRegistryClientTest {

    @BeforeEach
    @Transactional
    void setUp() {
        cleanUpDatabase();
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

            Extension extension = new Extension();
            extension.name = "Foo";
            extension.description = "A Foo Extension";
            extension.groupId = "foo.bar";
            extension.artifactId = "foo-extension";
            extension.persist();
            {
                ExtensionRelease extensionRelease = new ExtensionRelease();
                extensionRelease.extension = extension;
                extensionRelease.version = "1.0.0";
                extensionRelease.quarkusCoreVersion = "2.0.0.Final";
                extensionRelease.persist();
            }
            {
                ExtensionRelease extensionRelease = new ExtensionRelease();
                extensionRelease.extension = extension;
                extensionRelease.version = "1.1.0";
                extensionRelease.quarkusCoreVersion = "2.1.0.Final";
                extensionRelease.persist();

                ExtensionReleaseCompatibility erc = new ExtensionReleaseCompatibility();
                erc.extensionRelease = extensionRelease;
                erc.quarkusCoreVersion = "2.0.0.Final";
                erc.compatible = true;
                erc.persist();
            }
        }
    }

    @Test
    void should_return_no_content_if_no_platforms_found() {
        given()
                .get("/client/platforms?v=1.1.0")
                .then()
                .statusCode(HttpURLConnection.HTTP_NO_CONTENT);
    }

    @Test
    void should_return_application_json_as_response_type() throws Exception {
        given()
                .accept("application/json;custom=aaaaaaaaaaaaaaaaaa;charset=utf-7")
                .get("/client/platforms")
                .then()
                .statusCode(HttpURLConnection.HTTP_OK)
                .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON);
    }

    @Test
    void should_return_unacceptable_on_invalid_accept_headers() {
        given()
                .accept(MediaType.APPLICATION_XML)
                .get("/client/platforms")
                .then()
                .statusCode(HttpURLConnection.HTTP_NOT_ACCEPTABLE);
    }

    @Test
    void should_return_only_extensions_matching_compatible_quarkus_core() {
        given()
                .get("/client/non-platform-extensions?v=2.0.0.Final")
                .then()
                .statusCode(HttpURLConnection.HTTP_OK)
                .body("extensions[0].artifact", is("foo.bar:foo-extension::jar:1.1.0"));
    }

    @Test
    void should_return_all_platforms_with_current_stream_id() {
        given()
                .get("/client/platforms/all")
                .then()
                .statusCode(HttpURLConnection.HTTP_OK)
                .contentType(ContentType.JSON)
                .body("platforms", hasSize(1),
                        "platforms[0].streams", hasSize(2),
                        "platforms[0].streams.id", hasItems("2.1", "2.0"),
                        "platforms[0].current-stream-id", is("2.1"));
    }

    @AfterEach
    @Transactional
    void cleanUpDatabase() {
        PlatformExtension.deleteAll();
        ExtensionReleaseCompatibility.deleteAll();
        ExtensionRelease.deleteAll();
        Extension.deleteAll();
        PlatformRelease.deleteAll();
        PlatformStream.deleteAll();
    }

}
