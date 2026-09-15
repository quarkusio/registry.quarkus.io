package io.quarkus.registry.app;

import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;

import io.quarkus.maven.dependency.ArtifactCoords;
import io.quarkus.registry.Constants;
import io.quarkus.registry.catalog.CatalogMapperHelper;
import io.quarkus.registry.catalog.ExtensionCatalog;
import io.quarkus.registry.catalog.ExtensionCatalogImpl;
import io.restassured.http.ContentType;
import jakarta.ws.rs.core.MediaType;

/**
 * Drives the registry the way the platform release pipeline and its clients do: a catalog goes in through the admin
 * endpoint, and comes back out as a Maven platform descriptor.
 */
public final class CatalogTestSupport {

    /**
     * A Quarkus 2.8.0.Final community catalog, the stock fixture for tests that need a realistic catalog.
     */
    public static final String COMMUNITY_CATALOG = "extension-catalog-community.json";

    private CatalogTestSupport() {
    }

    /**
     * The raw bytes of a catalog test resource, to be posted verbatim.
     */
    public static byte[] readCatalogBytes(String resourceName) throws IOException {
        try (InputStream resource = Thread.currentThread().getContextClassLoader().getResourceAsStream(resourceName)) {
            assertThat(resource).as("Test resource %s", resourceName).isNotNull();
            return resource.readAllBytes();
        }
    }

    public static ExtensionCatalog.Mutable deserializeCatalog(byte[] catalog) throws IOException {
        return deserializeCatalog(new ByteArrayInputStream(catalog));
    }

    public static ExtensionCatalog.Mutable deserializeCatalog(InputStream catalog) throws IOException {
        return CatalogMapperHelper.deserialize(catalog, ExtensionCatalogImpl.Builder.class);
    }

    /**
     * The coordinates a catalog announces itself under, which are also the ones it is served back under.
     */
    public static ArtifactCoords catalogCoords(byte[] catalog) throws IOException {
        return ArtifactCoords.fromString(deserializeCatalog(catalog).build().getId());
    }

    /**
     * Imports a catalog, as the platform release pipeline does.
     */
    public static void postCatalog(byte[] catalog, String platformKey) {
        given()
                .header("Token", "test")
                .header("X-Platform", platformKey)
                .contentType(ContentType.JSON)
                .body(catalog)
                .post("/admin/v1/extension/catalog")
                .then()
                .log().ifValidationFails()
                .statusCode(HttpURLConnection.HTTP_ACCEPTED)
                .contentType(ContentType.JSON);
    }

    /**
     * Fetches a platform descriptor back, under the registry's own artifact version.
     */
    public static ExtensionCatalog getPlatformDescriptor(ArtifactCoords descriptorCoords) throws IOException {
        return getPlatformDescriptor(descriptorCoords, Constants.DEFAULT_REGISTRY_ARTIFACT_VERSION);
    }

    /**
     * As above, for a given version segment of the Maven path. A descriptor is served both under the registry's own
     * artifact version and under the platform version itself, so both are worth exercising.
     */
    public static ExtensionCatalog getPlatformDescriptor(ArtifactCoords descriptorCoords, String pathVersion)
            throws IOException {
        String url = String.format(
                "/maven/%1$s/%2$s/%3$s/%2$s-%3$s-%4$s.json",
                descriptorCoords.getGroupId().replace('.', '/'),
                descriptorCoords.getArtifactId(),
                pathVersion,
                descriptorCoords.getVersion());
        InputStream resultStream = given()
                .get(url)
                .then()
                .statusCode(HttpURLConnection.HTTP_OK)
                .contentType(MediaType.APPLICATION_JSON)
                .extract().asInputStream();
        return deserializeCatalog(resultStream).build();
    }
}
