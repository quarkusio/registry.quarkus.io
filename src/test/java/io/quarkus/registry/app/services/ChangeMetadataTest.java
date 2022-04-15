package io.quarkus.registry.app.services;

import static io.restassured.RestAssured.given;
import static java.util.Map.entry;
import static org.assertj.core.api.Assertions.assertThat;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.util.Map;
import java.util.Optional;

import javax.ws.rs.core.MediaType;

import org.assertj.core.api.InstanceOfAssertFactories;
import org.junit.jupiter.api.Test;

import io.quarkus.maven.ArtifactCoords;
import io.quarkus.registry.app.BaseTest;
import io.quarkus.registry.catalog.CatalogMapperHelper;
import io.quarkus.registry.catalog.Category;
import io.quarkus.registry.catalog.ExtensionCatalog;
import io.quarkus.registry.catalog.ExtensionCatalogImpl;
import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;

@QuarkusTest
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
                "/maven/%1$s/%2$s/%3$s/%2$s-%3$s-%3$s.json",
                id.getGroupId().replace('.', '/'),
                id.getArtifactId(),
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
                "/maven/%1$s/%2$s/%3$s/%2$s-%3$s-%3$s.json",
                id.getGroupId().replace('.', '/'),
                id.getArtifactId(),
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
                "/maven/%1$s/%2$s/%3$s/%2$s-%3$s-%3$s.json",
                id.getGroupId().replace('.', '/'),
                id.getArtifactId(),
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

}
