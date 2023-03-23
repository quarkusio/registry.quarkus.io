package io.quarkus.registry.app.admin;

import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.CoreMatchers.hasItem;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.Matchers.hasSize;

import java.io.IOException;
import java.io.InputStream;
import java.io.StringWriter;
import java.net.HttpURLConnection;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import io.quarkus.maven.dependency.ArtifactCoords;
import io.quarkus.registry.app.BaseTest;
import io.quarkus.registry.app.model.Extension;
import io.quarkus.registry.app.model.ExtensionRelease;
import io.quarkus.registry.app.model.Platform;
import io.quarkus.registry.app.model.PlatformExtension;
import io.quarkus.registry.app.model.PlatformRelease;
import io.quarkus.registry.app.model.PlatformStream;
import io.quarkus.registry.catalog.CatalogMapperHelper;
import io.quarkus.registry.catalog.ExtensionCatalog;
import io.quarkus.registry.catalog.ExtensionCatalogImpl;
import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;
import jakarta.transaction.Transactional;

@QuarkusTest
class AdminApiTest extends BaseTest {

    @BeforeEach
    @Transactional
    void setUp() {
        {
            Extension extension = new Extension();
            extension.name = "Foo";
            extension.description = "A Foo Extension";
            extension.groupId = "foo.bar";
            extension.artifactId = "foo-extension";
            extension.persistAndFlush();

            ExtensionRelease extensionRelease = new ExtensionRelease();
            extensionRelease.extension = extension;
            extensionRelease.version = "1.0.0";
            extensionRelease.quarkusCoreVersion = "2.0.0.Final";
            extensionRelease.persistAndFlush();
        }
        {
            Extension extensionToBeDeleted = new Extension();
            extensionToBeDeleted.name = "ToDelete";
            extensionToBeDeleted.description = "A Foo Extension to be deleted";
            extensionToBeDeleted.groupId = "delete";
            extensionToBeDeleted.artifactId = "me";
            extensionToBeDeleted.persistAndFlush();

            ExtensionRelease extensionReleaseToBeDeleted = new ExtensionRelease();
            extensionReleaseToBeDeleted.extension = extensionToBeDeleted;
            extensionReleaseToBeDeleted.version = "1.0.0";
            extensionReleaseToBeDeleted.quarkusCoreVersion = "2.0.0.Final";
            extensionReleaseToBeDeleted.persistAndFlush();

            ExtensionRelease extensionReleaseToBeDeleted2 = new ExtensionRelease();
            extensionReleaseToBeDeleted2.extension = extensionToBeDeleted;
            extensionReleaseToBeDeleted2.version = "1.1.0";
            extensionReleaseToBeDeleted2.quarkusCoreVersion = "2.0.0.Final";
            extensionReleaseToBeDeleted2.persistAndFlush();
        }
        {
            Platform platform = Platform.findByKey("io.quarkus.platform").get();
            PlatformStream stream10 = new PlatformStream();
            stream10.platform = platform;
            stream10.streamKey = "10.0";
            stream10.persistAndFlush();

            PlatformStream stream9 = new PlatformStream();
            stream9.platform = platform;
            stream9.streamKey = "9.0";
            stream9.persistAndFlush();

            PlatformRelease release9 = new PlatformRelease();
            release9.platformStream = stream9;
            release9.version = "9.0.0.Final";
            release9.quarkusCoreVersion = "9.0.0.Final";
            release9.bom = "io.quarkus.platform:quarkus-bom::pom:9.0.0.Final";
            release9.persistAndFlush();

            PlatformRelease release10 = new PlatformRelease();
            release10.platformStream = stream10;
            release10.version = "10.0.0.Final";
            release10.quarkusCoreVersion = "10.0.0.Final";
            release10.bom = "io.quarkus.platform:quarkus-bom::pom:10.0.0.Final";
            release10.persistAndFlush();

            Extension extensionToBeDeleted = new Extension();
            extensionToBeDeleted.name = "ToDeleteFromPlatform";
            extensionToBeDeleted.description = "A Foo Extension to be deleted";
            extensionToBeDeleted.groupId = "delete-platform-groupId";
            extensionToBeDeleted.artifactId = "delete-platform-artifactId";
            extensionToBeDeleted.persist();

            ExtensionRelease extensionReleaseToBeDeleted = new ExtensionRelease();
            extensionReleaseToBeDeleted.extension = extensionToBeDeleted;
            extensionReleaseToBeDeleted.version = "1.0.0";
            extensionReleaseToBeDeleted.quarkusCoreVersion = "2.0.0.Final";
            extensionReleaseToBeDeleted.persist();

            PlatformExtension platformExtension = new PlatformExtension();
            platformExtension.extensionRelease = extensionReleaseToBeDeleted;
            platformExtension.platformRelease = release10;
            platformExtension.persist();

            ExtensionRelease extensionReleaseToBeDeleted2 = new ExtensionRelease();
            extensionReleaseToBeDeleted2.extension = extensionToBeDeleted;
            extensionReleaseToBeDeleted2.version = "1.1.0";
            extensionReleaseToBeDeleted2.quarkusCoreVersion = "2.0.0.Final";
            extensionReleaseToBeDeleted2.persistAndFlush();
        }
    }

    @Test
    void unauthenticated_requests_should_forbid_access() {
        given().body(
                "{ \"artifact\":\"aaaa\",\"artifactId\": \"string\", \"description\": \"string\", \"groupId\": \"messging\", \"metadata\":   {}, \"name\": \"string\",\"origins\": [   {     \"id\": \"string\",     \"platform\": false   } ], \"version\": \"string\"}")
                .post("/admin/v1/extension")
                .then()
                .statusCode(HttpURLConnection.HTTP_FORBIDDEN);
    }

    @Test
    void extension_description_and_name_may_be_updated_between_releases() throws IOException {
        given()
                .get("/client/non-platform-extensions?v=2.0.0.Final")
                .then()
                .statusCode(HttpURLConnection.HTTP_OK)
                .contentType(ContentType.JSON)
                .body("extensions[0].name", is("Foo"),
                        "extensions[0].description", is("A Foo Extension"));
        // Add a new Extension release
        io.quarkus.registry.catalog.Extension extension = io.quarkus.registry.catalog.Extension.builder()
                .setGroupId("foo.bar")
                .setArtifactId("foo-extension")
                .setVersion("1.0.1")
                .setName("Another Name")
                .setDescription("Another Description")
                .setArtifact(ArtifactCoords.jar("foo.bar", "foo-extension", "1.0.1"))
                .build();
        StringWriter sw = new StringWriter();
        CatalogMapperHelper.serialize(extension, sw);

        // Update extension
        given()
                .body(sw.toString())
                .header("Token", "test")
                .contentType(ContentType.JSON)
                .post("/admin/v1/extension")
                .then()
                .log().ifValidationFails()
                .statusCode(HttpURLConnection.HTTP_ACCEPTED)
                .contentType(ContentType.JSON);

        given()
                .get("/client/non-platform-extensions?v=2.0.0.Final")
                .then()
                .statusCode(HttpURLConnection.HTTP_OK)
                .contentType(ContentType.JSON)
                .log().ifValidationFails()
                .body("extensions.name", hasItem("Another Name"),
                        "extensions.description", hasItem("Another Description"));

    }

    @Test
    void validate_input() {
        given()
                .header("Token", "test")
                .contentType(ContentType.JSON)
                .post("/admin/v1/extension/catalog")
                .then()
                .statusCode(HttpURLConnection.HTTP_BAD_REQUEST)
                .log().ifValidationFails()
                .contentType(ContentType.JSON)
                .body("violations.message", hasItem("X-Platform header missing"),
                        "violations.message", hasItem("Body payload is missing"));
    }

    @Test
    void validate_long_input() {
        given()
                .header("Token", "test")
                .contentType(ContentType.JSON)
                .body("{\n"
                        + "\"artifact\":\"com.redhat.quarkus.A[" + StringUtils.repeat('A', 5308414)
                        + "]A:quarkus-bom::pom:2.2.3.Final-redhat-00114\",\n"
                        + "\"artifactId\": \"string\",\n"
                        + "\"description\": \"string\",\n"
                        + "\"groupId\": \"string\",\n"
                        + "\"metadata\":\n"
                        + "\n"
                        + "{ \"additionalProp1\": \"string\" }\n"
                        + ",\n"
                        + "\"name\": \"string\",\n"
                        + "\"origins\": [\n"
                        + "\n"
                        + "{ \"id\": \"string\", \"platform\": true }\n"
                        + "],\n"
                        + "\"version\": \"string\"\n"
                        + "}\n"
                        + "\n")
                .post("/admin/v1/extension")
                .then()
                .statusCode(HttpURLConnection.HTTP_BAD_REQUEST);
    }

    @Test
    void delete_extension() {
        // Delete extension version
        given().formParams("groupId", "delete",
                "artifactId", "me",
                "version", "1.1.0")
                .header("Token", "test")
                .delete("/admin/v1/extension")
                .then()
                .statusCode(HttpURLConnection.HTTP_ACCEPTED);

        given()
                .get("/client/non-platform-extensions?v=2.0.0.Final")
                .then()
                .statusCode(HttpURLConnection.HTTP_OK)
                .contentType(ContentType.JSON)
                .body("extensions.version", not(hasItem("1.1.0")),
                        "extensions.name", hasItem("ToDelete"));

        // Delete extension
        given().formParams("groupId", "delete",
                "artifactId", "me")
                .header("Token", "test")
                .delete("/admin/v1/extension")
                .then()
                .statusCode(HttpURLConnection.HTTP_ACCEPTED);

        given()
                .get("/client/non-platform-extensions?v=2.0.0.Final")
                .then()
                .statusCode(HttpURLConnection.HTTP_OK)
                .contentType(ContentType.JSON)
                .body("extensions.name", not(hasItem("ToDelete")));

    }

    @Test
    void should_not_delete_extension_release_from_platform() {
        // Should not delete an extension release if it belongs to a platform
        given().formParams("groupId", "delete-platform-groupId",
                "artifactId", "delete-platform-artifactId",
                "version", "1.0.0")
                .header("Token", "test")
                .delete("/admin/v1/extension")
                .then()
                .statusCode(HttpURLConnection.HTTP_NOT_ACCEPTABLE);
    }

    @Test
    void should_not_delete_extension_from_platform() {
        // Should not delete an extension if any release belongs to a platform
        given().formParams("groupId", "delete-platform-groupId",
                "artifactId", "delete-platform-artifactId")
                .header("Token", "test")
                .delete("/admin/v1/extension")
                .then()
                .statusCode(HttpURLConnection.HTTP_NOT_ACCEPTABLE);
    }

    @Test
    void should_honor_unlisted_platform_stream_flag() {

        given()
                .get("/client/platforms")
                .then()
                .statusCode(HttpURLConnection.HTTP_OK)
                .contentType(ContentType.JSON)
                .body("platforms[0].streams", hasSize(2),
                        "platforms[0].current-stream-id", is("10.0"));

        // Setting unlisted=true to 10.0 stream
        given().formParam("unlisted", "true")
                .header("Token", "test")
                .patch("/admin/v1/stream/io.quarkus.platform/10.0")
                .then()
                .statusCode(HttpURLConnection.HTTP_ACCEPTED);

        given()
                .get("/client/platforms")
                .then()
                .statusCode(HttpURLConnection.HTTP_OK)
                .contentType(ContentType.JSON)
                .body("platforms[0].streams", hasSize(1),
                        "platforms[0].current-stream-id", is("9.0"));

    }

    @Test
    void delete_extension_catalog() {
        // Delete extension version
        given().formParams("platformKey", "io.quarkus.platform",
                "version", "10.0.0.Final")
                .header("Token", "test")
                .delete("/admin/v1/extension/catalog")
                .then()
                .statusCode(HttpURLConnection.HTTP_ACCEPTED);

        given()
                .get("/client/platforms/all")
                .then()
                .statusCode(HttpURLConnection.HTTP_OK)
                .contentType(ContentType.JSON)
                .body("platforms[0].streams", hasSize(1),
                        "platforms[0].current-stream-id", is("9.0"));
    }

    @Test
    void patch_extension() throws IOException {
        // Change extension description
        given()
                .get("/client/non-platform-extensions?v=2.0.0.Final")
                .then()
                .statusCode(HttpURLConnection.HTTP_OK)
                .contentType(ContentType.JSON)
                .body("extensions[0].name", is("Foo"),
                        "extensions[0].description", is("A Foo Extension"));
        // Add a new Extension release
        io.quarkus.registry.catalog.Extension extension = io.quarkus.registry.catalog.Extension.builder()
                .setGroupId("foo.bar")
                .setArtifactId("foo-extension")
                .setVersion("1.0.1")
                .setName("Another Name")
                .setDescription("Another Description")
                .setArtifact(ArtifactCoords.jar("foo.bar", "foo-extension", "1.0.1"))
                .build();
        StringWriter sw = new StringWriter();
        CatalogMapperHelper.serialize(extension, sw);

        // Update extension
        given()
                .formParams(Map.of("name", "Another Name", "description", "Another Description"))
                .header("Token", "test")
                .patch("/admin/v1/extension/foo.bar/foo-extension")
                .then()
                .statusCode(HttpURLConnection.HTTP_ACCEPTED);

        given()
                .get("/client/non-platform-extensions?v=2.0.0.Final")
                .then()
                .statusCode(HttpURLConnection.HTTP_OK)
                .contentType(ContentType.JSON)
                .body("extensions.name", hasItem("Another Name"),
                        "extensions.description", hasItem("Another Description"));
    }

    @Test
    void insert_platform_if_not_exists() throws IOException {
        // Add a new Extension Catalog
        try (InputStream body = getClass().getClassLoader().getResourceAsStream("extension-catalog-debezium.json")) {
            // Update extension
            given()
                    .body(body)
                    .header("Token", "test")
                    .header("X-Platform", "io.quarkus.platform:quarkus-debezium-bom")
                    .contentType(ContentType.JSON)
                    .post("/admin/v1/extension/catalog")
                    .then()
                    .log().ifValidationFails()
                    .statusCode(HttpURLConnection.HTTP_ACCEPTED)
                    .contentType(ContentType.JSON);
        }
    }

    @Test
    void patch_extension_release() throws IOException {
        // Update extension release
        given()
                .formParams("metadata", Map.of("foo", "bar"))
                .header("Token", "test")
                .patch("/admin/v1/extension/foo.bar/foo-extension/1.0.0")
                .then()
                .statusCode(HttpURLConnection.HTTP_ACCEPTED);
        try (InputStream stream = given()
                .get("/client/non-platform-extensions?v=2.0.0.Final")
                .then()
                .statusCode(HttpURLConnection.HTTP_OK)
                .contentType(ContentType.JSON)
                .extract().body().asInputStream()) {
            ExtensionCatalog catalog = CatalogMapperHelper.deserialize(stream, ExtensionCatalogImpl.Builder.class).build();
            assertThat(catalog.getExtensions().iterator().next().getMetadata()).containsEntry("foo", "bar");
        }
    }

}
