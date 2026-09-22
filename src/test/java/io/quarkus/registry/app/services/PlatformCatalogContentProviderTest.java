package io.quarkus.registry.app.services;

import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.io.StringWriter;
import java.net.HttpURLConnection;

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

/**
 * Tests if the {@link ExtensionCatalog} content is generated correctly
 */
@QuarkusTest
public class PlatformCatalogContentProviderTest extends BaseTest {

    @Test
    void should_return_catalog() throws Exception {
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
        // Test using 1.0-SNAPSHOT
        {
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

            ExtensionCatalog result = CatalogMapperHelper.deserialize(resultStream, ExtensionCatalogImpl.Builder.class).build();
            assertThat(result).usingRecursiveComparison().isEqualTo(expected);
        }
        // Test using the same version as in the qualifier
        {
            String url = String.format(
                    "/maven/%1$s/%2$s/%3$s/%2$s-%3$s-%4$s.json",
                    id.getGroupId().replace('.', '/'),
                    id.getArtifactId(),
                    id.getVersion(),
                    id.getVersion());
            // Test the maven endpoint
            InputStream resultStream = given()
                    .get(url)
                    .then()
                    .statusCode(200)
                    .contentType(MediaType.APPLICATION_JSON)
                    .extract().asInputStream();

            ExtensionCatalog result = CatalogMapperHelper.deserialize(resultStream, ExtensionCatalogImpl.Builder.class).build();
            assertThat(result).usingRecursiveComparison().isEqualTo(expected);
        }
    }

    /**
     * Categories the registry has never seen before should be created on import, instead of being silently dropped
     * because they are missing from the {@code V2__Add_categories.sql} seed.
     *
     * @see <a href="https://github.com/quarkusio/quarkus/issues/55981">quarkusio/quarkus#55981</a>
     */
    @Test
    void should_create_category_missing_from_the_seed() throws Exception {
        ExtensionCatalog.Mutable catalog;
        try (InputStream resource = getClass().getClassLoader().getResourceAsStream("extension-catalog-community.json")) {
            assert resource != null;
            catalog = CatalogMapperHelper.deserialize(resource, ExtensionCatalogImpl.Builder.class);
        }
        Category ai = Category.builder()
                .setId("ai")
                .setName("Artificial Intelligence (AI)")
                .setDescription("Extensions to build AI-infused applications")
                .build();
        catalog.addCategory(ai);
        ExtensionCatalog expected = catalog.build();
        ArtifactCoords id = ArtifactCoords.fromString(expected.getId());

        StringWriter sw = new StringWriter();
        CatalogMapperHelper.serialize(expected, sw);

        given()
                .header("Token", "test")
                .header("X-Platform", id.getGroupId())
                .contentType(ContentType.JSON)
                .body(sw.toString())
                .post("/admin/v1/extension/catalog")
                .then()
                .statusCode(HttpURLConnection.HTTP_ACCEPTED)
                .contentType(ContentType.JSON);

        String url = String.format(
                "/maven/%1$s/%2$s/%3$s/%2$s-%3$s-%4$s.json",
                id.getGroupId().replace('.', '/'),
                id.getArtifactId(),
                Constants.DEFAULT_REGISTRY_ARTIFACT_VERSION,
                id.getVersion());
        InputStream resultStream = given()
                .get(url)
                .then()
                .statusCode(200)
                .contentType(MediaType.APPLICATION_JSON)
                .extract().asInputStream();

        ExtensionCatalog result = CatalogMapperHelper.deserialize(resultStream, ExtensionCatalogImpl.Builder.class).build();
        // The new category round-trips, and the catalog ordering is preserved
        assertThat(result.getCategories()).usingRecursiveFieldByFieldElementComparator()
                .containsExactlyElementsOf(expected.getCategories());

        // Verify the new category also appears in /client/categories/all
        InputStream allCategoriesStream = given()
                .get("/client/categories/all")
                .then()
                .statusCode(200)
                .contentType(MediaType.APPLICATION_JSON)
                .extract().asInputStream();

        ExtensionCatalog allCategories = CatalogMapperHelper.deserialize(allCategoriesStream, ExtensionCatalogImpl.Builder.class).build();
        assertThat(allCategories.getCategories())
                .extracting(Category::getId)
                .contains("ai");
    }
}
