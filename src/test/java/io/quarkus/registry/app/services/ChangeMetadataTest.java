package io.quarkus.registry.app.services;

import static io.restassured.RestAssured.given;
import static java.util.Map.entry;
import static org.assertj.core.api.Assertions.assertThat;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.Optional;

import org.assertj.core.api.InstanceOfAssertFactories;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Test;

import io.quarkus.maven.dependency.ArtifactCoords;
import io.quarkus.registry.Constants;
import io.quarkus.registry.app.BaseTest;
import io.quarkus.registry.catalog.CatalogMapperHelper;
import io.quarkus.registry.catalog.Category;
import io.quarkus.registry.catalog.ExtensionCatalog;
import io.quarkus.registry.catalog.ExtensionCatalogImpl;
import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;
import jakarta.ws.rs.core.MediaType;

@QuarkusTest
@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
public class ChangeMetadataTest extends BaseTest {

    @Test
    void should_change_platform_metadata() throws Exception {
        byte[] expectedByteArray;
        try (InputStream expectedResource = getClass().getClassLoader()
                .getResourceAsStream("extension-catalog-community.json")) {
            assert expectedResource != null;
            expectedByteArray = expectedResource.readAllBytes();
        }
        ExtensionCatalog expected = CatalogMapperHelper
                .deserialize(new ByteArrayInputStream(expectedByteArray), ExtensionCatalogImpl.Builder.class).build();
        ArtifactCoords id = ArtifactCoords.fromString(expected.getId());

        // Include the platform release entry
        given()
                .header("Token", "test")
                .header("X-Platform", id.getGroupId())
                .contentType(ContentType.JSON)
                .body(expectedByteArray)
                .post("/admin/v1/extension/catalog")
                .then()
                .statusCode(HttpURLConnection.HTTP_ACCEPTED)
                .contentType(ContentType.JSON);

        // Change platform metadata
        given()
                .header("Token", "test")
                .contentType(ContentType.URLENC)
                .body("""
                        metadata={"foo":"bar"}
                        """)
                .patch("/admin/v1/platform-release/{platformKey}/{streamKey}/{version}",
                        Map.of("platformKey", id.getGroupId(),
                                "streamKey", "2.8",
                                "version", id.getVersion()))
                .then()
                .statusCode(HttpURLConnection.HTTP_ACCEPTED);

        String url = String.format(
                "/maven/%1$s/%2$s/%3$s/%2$s-%3$s-%4$s.json",
                id.getGroupId().replace('.', '/'),
                id.getArtifactId(),
                Constants.DEFAULT_REGISTRY_ARTIFACT_VERSION,
                id.getVersion());

        // Test the maven endpoint
        InputStream resultStream = given()
                .get(url)
                .then()
                .statusCode(200)
                .contentType(MediaType.APPLICATION_JSON)
                .extract().asInputStream();

        ExtensionCatalog result = CatalogMapperHelper
                .deserialize(resultStream, ExtensionCatalogImpl.Builder.class).build();
        assertThat(result).isNotNull()
                .satisfies(c -> assertThat(c.getMetadata()).containsOnly(entry("foo", "bar")));
    }

    @Test
    void should_change_category_metadata() throws Exception {
        byte[] expectedByteArray;
        try (InputStream expectedResource = getClass().getClassLoader()
                .getResourceAsStream("extension-catalog-community.json")) {
            assert expectedResource != null;
            expectedByteArray = expectedResource.readAllBytes();
        }
        ExtensionCatalog expected = CatalogMapperHelper
                .deserialize(new ByteArrayInputStream(expectedByteArray), ExtensionCatalogImpl.Builder.class).build();
        ArtifactCoords id = ArtifactCoords.fromString(expected.getId());

        // Include the platform release entry
        given()
                .header("Token", "test")
                .header("X-Platform", id.getGroupId())
                .contentType(ContentType.JSON)
                .body(expectedByteArray)
                .post("/admin/v1/extension/catalog")
                .then()
                .statusCode(HttpURLConnection.HTTP_ACCEPTED)
                .contentType(ContentType.JSON);

        // Change platform metadata
        given()
                .header("Token", "test")
                .contentType(ContentType.URLENC)
                .body("""
                        metadata={"foo":"bar"}
                        """)
                .patch("/admin/v1/platform-release/{platformKey}/{streamKey}/{version}/category/{categoryKey}",
                        Map.of("platformKey", id.getGroupId(),
                                "streamKey", "2.8",
                                "version", id.getVersion(),
                                "categoryKey", "alt-languages"))
                .then()
                .statusCode(HttpURLConnection.HTTP_ACCEPTED);

        String url = String.format(
                "/maven/%1$s/%2$s/%3$s/%2$s-%3$s-%4$s.json",
                id.getGroupId().replace('.', '/'),
                id.getArtifactId(),
                Constants.DEFAULT_REGISTRY_ARTIFACT_VERSION,
                id.getVersion());

        // Test the maven endpoint
        InputStream resultStream = given()
                .get(url)
                .then()
                .statusCode(200)
                .contentType(MediaType.APPLICATION_JSON)
                .extract().asInputStream();

        ExtensionCatalog result = CatalogMapperHelper
                .deserialize(resultStream, ExtensionCatalogImpl.Builder.class).build();

        Optional<Category> categoryOptional = result.getCategories().stream().filter(c -> c.getId().equals("alt-languages"))
                .findFirst();
        assertThat(categoryOptional).isNotEmpty()
                .hasValueSatisfying(c -> assertThat(c.getMetadata()).containsOnly(entry("foo", "bar")));
    }

    /**
     * There is no global category table any more, so a category only exists in the context of the release that declared
     * it. Patching one the release never declared is a 404, even if another release declares it.
     */
    @Test
    void should_not_change_metadata_of_a_category_the_release_did_not_declare() throws Exception {
        ArtifactCoords id = importCommunityCatalog();

        given()
                .header("Token", "test")
                .contentType(ContentType.URLENC)
                .body("""
                        metadata={"foo":"bar"}
                        """)
                .patch("/admin/v1/platform-release/{platformKey}/{streamKey}/{version}/category/{categoryKey}",
                        Map.of("platformKey", id.getGroupId(),
                                "streamKey", "2.8",
                                "version", id.getVersion(),
                                "categoryKey", "no-such-category"))
                .then()
                .statusCode(HttpURLConnection.HTTP_NOT_FOUND);
    }

    @Test
    void should_not_change_category_metadata_of_an_unknown_release() throws Exception {
        ArtifactCoords id = importCommunityCatalog();

        given()
                .header("Token", "test")
                .contentType(ContentType.URLENC)
                .body("""
                        metadata={"foo":"bar"}
                        """)
                .patch("/admin/v1/platform-release/{platformKey}/{streamKey}/{version}/category/{categoryKey}",
                        Map.of("platformKey", id.getGroupId(),
                                "streamKey", "2.8",
                                "version", "9.9.9.Final",
                                "categoryKey", "alt-languages"))
                .then()
                .statusCode(HttpURLConnection.HTTP_NOT_FOUND);
    }

    /**
     * Patching a category of one release must not touch the same category on another release: each release owns its own
     * copy of the definition.
     */
    @Test
    void should_change_category_metadata_for_only_the_patched_release() throws Exception {
        ArtifactCoords id = importCommunityCatalog();

        // A second release of the same stream, declaring the same categories
        given()
                .header("Token", "test")
                .header("X-Platform", id.getGroupId())
                .contentType(ContentType.JSON)
                .body(communityCatalogAtVersion("2.8.1.Final"))
                .post("/admin/v1/extension/catalog")
                .then()
                .statusCode(HttpURLConnection.HTTP_ACCEPTED);

        given()
                .header("Token", "test")
                .contentType(ContentType.URLENC)
                .body("""
                        metadata={"foo":"bar"}
                        """)
                .patch("/admin/v1/platform-release/{platformKey}/{streamKey}/{version}/category/{categoryKey}",
                        Map.of("platformKey", id.getGroupId(),
                                "streamKey", "2.8",
                                "version", id.getVersion(),
                                "categoryKey", "alt-languages"))
                .then()
                .statusCode(HttpURLConnection.HTTP_ACCEPTED);

        assertThat(categoryMetadata(id, "2.8.1.Final", "alt-languages")).doesNotContainKey("foo");
        assertThat(categoryMetadata(id, id.getVersion(), "alt-languages")).containsOnly(entry("foo", "bar"));
    }

    @Test
    void should_change_platform_extension_metadata() throws Exception {
        byte[] expectedByteArray;
        try (InputStream expectedResource = getClass().getClassLoader()
                .getResourceAsStream("extension-catalog-community.json")) {
            assert expectedResource != null;
            expectedByteArray = expectedResource.readAllBytes();
        }
        ExtensionCatalog expected = CatalogMapperHelper
                .deserialize(new ByteArrayInputStream(expectedByteArray), ExtensionCatalogImpl.Builder.class).build();
        ArtifactCoords id = ArtifactCoords.fromString(expected.getId());

        // Include the platform release entry
        given()
                .header("Token", "test")
                .header("X-Platform", id.getGroupId())
                .contentType(ContentType.JSON)
                .body(expectedByteArray)
                .post("/admin/v1/extension/catalog")
                .then()
                .statusCode(HttpURLConnection.HTTP_ACCEPTED)
                .contentType(ContentType.JSON);

        // Change platform metadata
        given()
                .header("Token", "test")
                .contentType(ContentType.URLENC)
                .body("""
                        metadata={"foo":"bar"}
                        """)
                .patch("/admin/v1/platform-release/{platformKey}/{streamKey}/{version}/extension/{extensionGroupId}/{extensionArtifactId}/{extensionVersion}",
                        Map.of("platformKey", id.getGroupId(),
                                "streamKey", "2.8",
                                "version", id.getVersion(),
                                "extensionGroupId", "io.quarkiverse.config",
                                "extensionArtifactId", "quarkus-config-consul",
                                "extensionVersion", "1.0.2"))
                .then()
                .statusCode(HttpURLConnection.HTTP_ACCEPTED);

        String url = String.format(
                "/maven/%1$s/%2$s/%3$s/%2$s-%3$s-%4$s.json",
                id.getGroupId().replace('.', '/'),
                id.getArtifactId(),
                Constants.DEFAULT_REGISTRY_ARTIFACT_VERSION,
                id.getVersion());

        // Test the maven endpoint
        InputStream resultStream = given()
                .get(url)
                .then()
                .statusCode(200)
                .contentType(MediaType.APPLICATION_JSON)
                .extract().asInputStream();

        ExtensionCatalog result = CatalogMapperHelper
                .deserialize(resultStream, ExtensionCatalogImpl.Builder.class).build();

        assertThat(result.getExtensions())
                .filteredOn(e -> e.getArtifact().getArtifactId().equals("quarkus-config-consul")).first()
                .extracting(io.quarkus.registry.catalog.Extension::getMetadata, InstanceOfAssertFactories.MAP)
                .containsOnly(entry("foo", "bar"));
    }

    private static byte[] communityCatalogBytes() throws IOException {
        try (InputStream resource = ChangeMetadataTest.class.getClassLoader()
                .getResourceAsStream("extension-catalog-community.json")) {
            assert resource != null;
            return resource.readAllBytes();
        }
    }

    /**
     * The same fixture republished as a different release of the same stream.
     */
    private static byte[] communityCatalogAtVersion(String version) throws IOException {
        return new String(communityCatalogBytes(), StandardCharsets.UTF_8)
                .replace("2.8.0.Final", version)
                .getBytes(StandardCharsets.UTF_8);
    }

    /**
     * Publishes the community catalog fixture, returning the coordinates it was published under.
     */
    private static ArtifactCoords importCommunityCatalog() throws IOException {
        byte[] catalog = communityCatalogBytes();
        ArtifactCoords id = ArtifactCoords.fromString(CatalogMapperHelper
                .deserialize(new ByteArrayInputStream(catalog), ExtensionCatalogImpl.Builder.class).build().getId());
        given()
                .header("Token", "test")
                .header("X-Platform", id.getGroupId())
                .contentType(ContentType.JSON)
                .body(catalog)
                .post("/admin/v1/extension/catalog")
                .then()
                .statusCode(HttpURLConnection.HTTP_ACCEPTED)
                .contentType(ContentType.JSON);
        return id;
    }

    /**
     * Reads a category back off the published descriptor of a given release.
     */
    private static Map<String, Object> categoryMetadata(ArtifactCoords id, String version, String categoryKey)
            throws IOException {
        String url = String.format(
                "/maven/%1$s/%2$s/%3$s/%2$s-%3$s-%4$s.json",
                id.getGroupId().replace('.', '/'),
                id.getArtifactId(),
                Constants.DEFAULT_REGISTRY_ARTIFACT_VERSION,
                version);
        InputStream resultStream = given()
                .get(url)
                .then()
                .statusCode(200)
                .contentType(MediaType.APPLICATION_JSON)
                .extract().asInputStream();
        ExtensionCatalog result = CatalogMapperHelper
                .deserialize(resultStream, ExtensionCatalogImpl.Builder.class).build();
        return result.getCategories().stream()
                .filter(c -> c.getId().equals(categoryKey))
                .findFirst()
                .orElseThrow(() -> new AssertionError("No category " + categoryKey + " in release " + version))
                .getMetadata();
    }
}
