package io.quarkus.registry.app.services;

import static io.quarkus.registry.app.CatalogTestSupport.COMMUNITY_CATALOG;
import static io.quarkus.registry.app.CatalogTestSupport.catalogCoords;
import static io.quarkus.registry.app.CatalogTestSupport.getPlatformDescriptor;
import static io.quarkus.registry.app.CatalogTestSupport.postCatalog;
import static io.quarkus.registry.app.CatalogTestSupport.readCatalogBytes;
import static io.restassured.RestAssured.given;
import static java.util.Map.entry;
import static org.assertj.core.api.Assertions.assertThat;

import java.net.HttpURLConnection;
import java.util.Map;
import java.util.Optional;

import org.assertj.core.api.InstanceOfAssertFactories;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Test;

import io.quarkus.maven.dependency.ArtifactCoords;
import io.quarkus.registry.app.BaseTest;
import io.quarkus.registry.catalog.Category;
import io.quarkus.registry.catalog.ExtensionCatalog;
import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;

@QuarkusTest
@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
public class ChangeMetadataTest extends BaseTest {

    @Test
    void should_change_platform_metadata() throws Exception {
        ArtifactCoords id = importCommunityCatalog();

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

        ExtensionCatalog result = getPlatformDescriptor(id);
        assertThat(result).isNotNull()
                .satisfies(c -> assertThat(c.getMetadata()).containsOnly(entry("foo", "bar")));
    }

    @Test
    void should_change_category_metadata() throws Exception {
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
                                "categoryKey", "alt-languages"))
                .then()
                .statusCode(HttpURLConnection.HTTP_ACCEPTED);

        ExtensionCatalog result = getPlatformDescriptor(id);
        Optional<Category> categoryOptional = result.getCategories().stream().filter(c -> c.getId().equals("alt-languages"))
                 .findFirst();
        assertThat(categoryOptional).isNotEmpty()
                .hasValueSatisfying(c -> assertThat(c.getMetadata()).containsOnly(entry("foo", "bar")));
    }

    @Test
    void should_change_platform_extension_metadata() throws Exception {
        ArtifactCoords id = importCommunityCatalog();

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

        ExtensionCatalog result = getPlatformDescriptor(id);
        assertThat(result.getExtensions())
                .filteredOn(e -> e.getArtifact().getArtifactId().equals("quarkus-config-consul")).first()
                .extracting(io.quarkus.registry.catalog.Extension::getMetadata, InstanceOfAssertFactories.MAP)
                .containsOnly(entry("foo", "bar"));
    }

    /**
     * Imports the stock catalog, so that there is a platform release to patch, and returns the coordinates it is
     * served back under.
     */
    private static ArtifactCoords importCommunityCatalog() throws Exception {
        byte[] catalog = readCatalogBytes(COMMUNITY_CATALOG);
        ArtifactCoords id = catalogCoords(catalog);
        postCatalog(catalog, id.getGroupId());
        return id;
    }
}
