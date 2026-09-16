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
     * Categories the registry has never seen before should be stored on import, instead of being silently dropped. They
     * used to be dropped because they were missing from a hardcoded list seeded by Flyway, which is why that list, and
     * the table it filled, are gone.
     *
     * @see <a href="https://github.com/quarkusio/quarkus/issues/55981">quarkusio/quarkus#55981</a>
     */
    @Test
    void should_store_a_category_the_registry_has_never_seen_before() throws Exception {
        ExtensionCatalog.Mutable catalog = communityCatalog();
        catalog.addCategory(Category.builder()
                .setId("ai")
                .setName("Artificial Intelligence (AI)")
                .setDescription("Extensions to build AI-infused applications")
                .build());
        ExtensionCatalog expected = catalog.build();

        ExtensionCatalog result = roundTrip(expected);

        // The new category round-trips, and the catalog ordering is preserved
        assertThat(result.getCategories()).usingRecursiveFieldByFieldElementComparator()
                .containsExactlyElementsOf(expected.getCategories());
    }

    /**
     * A catalog may name a category by id alone, to pin extensions to it without redefining it. The id stands in for
     * the missing name so there is always something displayable, but the description is deliberately left null: that
     * null is what tells a consumer no platform actually declared the category, which is the distinction #55974 is
     * after. Filling it in with a placeholder would throw that away.
     */
    @Test
    void should_leave_the_description_null_for_a_category_named_by_id_alone() throws Exception {
        ExtensionCatalog.Mutable catalog = communityCatalog();
        catalog.addCategory(Category.builder().setId("surprise").build());

        ExtensionCatalog result = roundTrip(catalog.build());

        Category surprise = result.getCategories().stream()
                .filter(category -> "surprise".equals(category.getId()))
                .findFirst()
                .orElseThrow();
        assertThat(surprise.getName()).isEqualTo("surprise");
        assertThat(surprise.getDescription()).isNull();
    }

    private ExtensionCatalog.Mutable communityCatalog() throws Exception {
        try (InputStream resource = getClass().getClassLoader().getResourceAsStream("extension-catalog-community.json")) {
            assert resource != null;
            return CatalogMapperHelper.deserialize(resource, ExtensionCatalogImpl.Builder.class);
        }
    }

    /**
     * Posts a catalog to the admin endpoint, then reads back what the maven descriptor endpoint serves for it.
     */
    private ExtensionCatalog roundTrip(ExtensionCatalog catalog) throws Exception {
        ArtifactCoords id = ArtifactCoords.fromString(catalog.getId());
        StringWriter sw = new StringWriter();
        CatalogMapperHelper.serialize(catalog, sw);

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

        return CatalogMapperHelper.deserialize(resultStream, ExtensionCatalogImpl.Builder.class).build();
    }
}
