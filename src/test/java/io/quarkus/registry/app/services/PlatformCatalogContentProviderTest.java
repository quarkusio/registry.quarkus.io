package io.quarkus.registry.app.services;

import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.io.StringReader;
import java.net.HttpURLConnection;

import javax.transaction.Transactional;
import javax.ws.rs.core.MediaType;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Test;

import io.quarkus.maven.ArtifactCoords;
import io.quarkus.registry.app.model.Extension;
import io.quarkus.registry.app.model.ExtensionRelease;
import io.quarkus.registry.app.model.PlatformExtension;
import io.quarkus.registry.app.model.PlatformRelease;
import io.quarkus.registry.app.model.PlatformReleaseCategory;
import io.quarkus.registry.app.model.PlatformStream;
import io.quarkus.registry.catalog.CatalogMapperHelper;
import io.quarkus.registry.catalog.ExtensionCatalog;
import io.quarkus.registry.catalog.ExtensionCatalogImpl;
import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;

/**
 * Tests if the {@link ExtensionCatalog} content is generated correctly
 */
@QuarkusTest
public class PlatformCatalogContentProviderTest {

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

        String url = String.format(
                "/maven/%1$s/%2$s/%3$s/%2$s-%3$s-%3$s.json",
                id.getGroupId().replace('.', '/'),
                id.getArtifactId(),
                id.getVersion());
        // Test the maven endpoint
        String resultStr = given()
                .get(url)
                .then()
                .statusCode(200)
                .contentType(MediaType.APPLICATION_JSON)
                .extract().asString();

        ExtensionCatalog result = CatalogMapperHelper
                .deserialize(new StringReader(resultStr), ExtensionCatalogImpl.Builder.class).build();
        assertThat(result).usingRecursiveComparison().isEqualTo(expected);
    }

    @AfterAll
    @Transactional
    static void tearDown() {
        PlatformReleaseCategory.deleteAll();
        PlatformExtension.deleteAll();
        ExtensionRelease.deleteAll();
        Extension.deleteAll();
        PlatformRelease.deleteAll();
        PlatformStream.deleteAll();
    }

}
