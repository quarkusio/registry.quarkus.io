package io.quarkus.registry.app.services;

import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.collection.IsCollectionWithSize.hasSize;

import java.io.IOException;
import java.io.InputStream;
import java.io.StringWriter;
import java.net.HttpURLConnection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.assertj.core.api.InstanceOfAssertFactories;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Test;

import io.quarkus.maven.dependency.ArtifactCoords;
import io.quarkus.registry.Constants;
import io.quarkus.registry.app.BaseTest;
import io.quarkus.registry.catalog.CatalogMapperHelper;
import io.quarkus.registry.catalog.Extension;
import io.quarkus.registry.catalog.ExtensionCatalog;
import io.quarkus.registry.catalog.ExtensionCatalogImpl;
import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;
import jakarta.ws.rs.core.MediaType;

/**
 * An extension release can be carried over unchanged into several platform releases. When that happens, the newer
 * platform descriptor is still the more recent statement about the extension, so the registry has to refresh what it
 * holds instead of keeping whatever the first platform release said.
 *
 * @see <a href="https://github.com/quarkusio/registry.quarkus.io/issues/346">#346</a>
 */
@QuarkusTest
@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
public class PlatformExtensionRefreshTest extends BaseTest {

    private static final String PLATFORM_KEY = "io.quarkus.platform";
    private static final String STREAM_KEY = "2.8";
    private static final String EXTENSION_GA = "io.quarkiverse.config:quarkus-config-consul";
    private static final String EXTENSION_GAV = EXTENSION_GA + "::jar:1.0.2";

    /**
     * The bug behind #346: an extension whose category is corrected in a later platform release keeps the category the
     * registry saw the first time it met that extension version.
     */
    @Test
    void should_report_the_category_from_the_most_recent_platform_release() throws Exception {
        importCatalog("2.8.0.Final", "core");
        // Same extension version, new platform release, corrected category
        importCatalog("2.8.1.Final", "cloud");

        given()
                .get("/client/extensions/all")
                .then()
                .statusCode(HttpURLConnection.HTTP_OK)
                .body("extensions", hasSize(1))
                .body("extensions[0].artifact", org.hamcrest.Matchers.is(EXTENSION_GAV))
                .body("extensions[0].metadata.categories", contains("cloud"));
    }

    /**
     * The same staleness seen from the extension's side. This endpoint reports a single origin per extension, so it
     * has to be the most recent platform release carrying it, not the one that happened to introduce the version: the
     * older descriptor is not where a client should be told to get the extension from.
     */
    @Test
    void should_report_the_most_recent_platform_release_as_the_origin() throws Exception {
        importCatalog("2.8.0.Final", "core");
        importCatalog("2.8.1.Final", "core");

        given()
                .get("/client/extensions/all")
                .then()
                .statusCode(HttpURLConnection.HTTP_OK)
                .body("extensions", hasSize(1))
                .body("extensions[0].origins", contains(descriptorId("2.8.1.Final")));
    }

    /**
     * The platform descriptor served back for a release has to hold the extensions that release shipped, even when
     * every one of them was already known from an earlier release.
     */
    @Test
    void should_serve_carried_over_extensions_in_the_platform_descriptor() throws Exception {
        importCatalog("2.8.0.Final", "core");
        importCatalog("2.8.1.Final", "cloud");

        ExtensionCatalog catalog = fetchPlatformDescriptor("2.8.1.Final");
        assertThat(catalog.getExtensions())
                .extracting(e -> e.getArtifact().toGACTVString())
                .containsExactly(EXTENSION_GAV);
        assertThat(catalog.getExtensions()).singleElement()
                .extracting(Extension::getMetadata, InstanceOfAssertFactories.MAP)
                .containsEntry(Extension.MD_CATEGORIES, List.of("cloud"));
    }

    /**
     * Refreshing the newer release must not rewrite history: the older platform descriptor still describes what that
     * release actually shipped.
     */
    @Test
    void should_leave_the_earlier_platform_descriptor_untouched() throws Exception {
        importCatalog("2.8.0.Final", "core");
        importCatalog("2.8.1.Final", "cloud");

        ExtensionCatalog catalog = fetchPlatformDescriptor("2.8.0.Final");
        assertThat(catalog.getExtensions()).singleElement()
                .extracting(Extension::getMetadata, InstanceOfAssertFactories.MAP)
                .containsEntry(Extension.MD_CATEGORIES, List.of("core"));
    }

    private ExtensionCatalog fetchPlatformDescriptor(String platformVersion) throws IOException {
        String url = String.format(
                "/maven/%1$s/%2$s/%3$s/%2$s-%3$s-%4$s.json",
                PLATFORM_KEY.replace('.', '/'),
                "quarkus-bom-quarkus-platform-descriptor",
                Constants.DEFAULT_REGISTRY_ARTIFACT_VERSION,
                platformVersion);
        InputStream resultStream = given()
                .get(url)
                .then()
                .statusCode(HttpURLConnection.HTTP_OK)
                .contentType(MediaType.APPLICATION_JSON)
                .extract().asInputStream();
        return CatalogMapperHelper.deserialize(resultStream, ExtensionCatalogImpl.Builder.class).build();
    }

    /**
     * Imports a single-extension platform catalog for the given platform release, with the extension pinned to the
     * given category. The extension version deliberately stays the same across releases: that is the case the registry
     * used to ignore.
     */
    private void importCatalog(String platformVersion, String category) throws IOException {
        ExtensionCatalog.Mutable catalog;
        try (InputStream resource = getClass().getClassLoader().getResourceAsStream("extension-catalog-community.json")) {
            assert resource != null;
            catalog = CatalogMapperHelper.deserialize(resource, ExtensionCatalogImpl.Builder.class);
        }
        catalog.setId(descriptorId(platformVersion));
        catalog.setBom(ArtifactCoords.pom(PLATFORM_KEY, "quarkus-bom", platformVersion));
        catalog.setQuarkusCoreVersion(platformVersion);
        catalog.setMetadata(withPlatformVersion(catalog.getMetadata(), platformVersion));
        // Keeping a single extension makes the assertions readable; nothing here depends on the rest of the catalog
        catalog.setExtensions(catalog.getExtensions().stream()
                .filter(e -> EXTENSION_GA.equals(e.getArtifact().getGroupId() + ":" + e.getArtifact().getArtifactId()))
                .map(e -> (io.quarkus.registry.catalog.Extension) e.mutable()
                        .setMetadata(withCategory(e.getMetadata(), category))
                        .build())
                .toList());

        StringWriter sw = new StringWriter();
        CatalogMapperHelper.serialize(catalog.build(), sw);

        given()
                .header("Token", "test")
                .header("X-Platform", PLATFORM_KEY)
                .contentType(ContentType.JSON)
                .body(sw.toString())
                .post("/admin/v1/extension/catalog")
                .then()
                .statusCode(HttpURLConnection.HTTP_ACCEPTED);
    }

    private static String descriptorId(String platformVersion) {
        return ArtifactCoords.of(PLATFORM_KEY, "quarkus-bom-quarkus-platform-descriptor", platformVersion,
                Constants.JSON, platformVersion).toString();
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> withPlatformVersion(Map<String, Object> catalogMetadata, String platformVersion) {
        Map<String, Object> metadata = new HashMap<>(catalogMetadata);
        Map<String, Object> platformRelease = new HashMap<>((Map<String, Object>) metadata.get("platform-release"));
        platformRelease.put("stream", STREAM_KEY);
        platformRelease.put("version", platformVersion);
        platformRelease.put("members", List.of(descriptorId(platformVersion)));
        metadata.put("platform-release", platformRelease);
        return metadata;
    }

    private static Map<String, Object> withCategory(Map<String, Object> extensionMetadata, String category) {
        Map<String, Object> metadata = new HashMap<>(extensionMetadata);
        metadata.put(Extension.MD_CATEGORIES, List.of(category));
        return metadata;
    }
}
