package io.quarkus.registry.app.services;

import static io.quarkus.registry.app.CatalogTestSupport.COMMUNITY_CATALOG;
import static io.quarkus.registry.app.CatalogTestSupport.deserializeCatalog;
import static io.quarkus.registry.app.CatalogTestSupport.getPlatformDescriptor;
import static io.quarkus.registry.app.CatalogTestSupport.postCatalog;
import static io.quarkus.registry.app.CatalogTestSupport.readCatalogBytes;
import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.nullValue;
import static org.hamcrest.collection.IsCollectionWithSize.hasSize;

import java.io.IOException;
import java.io.StringWriter;
import java.net.HttpURLConnection;
import java.nio.charset.StandardCharsets;
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
import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;

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
    /** The core version the stock catalog is built against, and so the one the compatibility queries answer to. */
    private static final String QUARKUS_CORE_VERSION = "2.8.0.Final";

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
     * An extension version carried over unchanged into a later platform release is re-imported under a version that
     * is already known. That is a legitimate refresh rather than an older catalog arriving late, so the newer release
     * still gets to correct the wording.
     *
     * @see <a href="https://github.com/quarkusio/registry.quarkus.io/issues/205">#205</a>
     */
    @Test
    void should_report_the_description_from_the_most_recent_platform_release() throws Exception {
        importCatalog("2.8.0.Final", "core", "Original description");
        importCatalog("2.8.1.Final", "core", "Corrected description");

        given()
                .get("/client/extensions/all")
                .then()
                .statusCode(HttpURLConnection.HTTP_OK)
                .body("extensions", hasSize(1))
                .body("extensions[0].description", org.hamcrest.Matchers.is("Corrected description"));
    }

    /**
     * The platform descriptor served back for a release has to hold the extensions that release shipped, even when
     * every one of them was already known from an earlier release.
     */
    @Test
    void should_serve_carried_over_extensions_in_the_platform_descriptor() throws Exception {
        importCatalog("2.8.0.Final", "core");
        importCatalog("2.8.1.Final", "cloud");

        ExtensionCatalog catalog = getPlatformDescriptor(descriptorCoords("2.8.1.Final"));
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

        ExtensionCatalog catalog = getPlatformDescriptor(descriptorCoords("2.8.0.Final"));
        assertThat(catalog.getExtensions()).singleElement()
                .extracting(Extension::getMetadata, InstanceOfAssertFactories.MAP)
                .containsEntry(Extension.MD_CATEGORIES, List.of("core"));
    }

    /**
     * An extension can reach the registry as a non-platform extension first and only later be shipped by a platform.
     * The platform import then finds a release that already exists, which is exactly the case the registry used to
     * skip, so the extension stayed non-platform for good despite being in the BOM.
     */
    @Test
    void should_stop_reporting_an_extension_as_non_platform_once_a_platform_ships_it() throws Exception {
        publishAsNonPlatformExtension();

        given()
                .get("/client/non-platform-extensions?v=" + QUARKUS_CORE_VERSION)
                .then()
                .statusCode(HttpURLConnection.HTTP_OK)
                .body("extensions", hasSize(1))
                .body("extensions[0].artifact", org.hamcrest.Matchers.is(EXTENSION_GAV));

        // The very same GAV, now shipped by a platform
        importCatalog(QUARKUS_CORE_VERSION, "core");

        given()
                .get("/client/non-platform-extensions?v=" + QUARKUS_CORE_VERSION)
                .then()
                .statusCode(HttpURLConnection.HTTP_OK)
                // An empty catalog omits the field rather than serializing an empty array
                .body("extensions", nullValue());

        given()
                .get("/client/extensions/all")
                .then()
                .statusCode(HttpURLConnection.HTTP_OK)
                .body("extensions", hasSize(1))
                .body("extensions[0].origins", contains(descriptorId(QUARKUS_CORE_VERSION)));
    }

    /**
     * Registers the extension through the non-platform endpoint. The Quarkus core version has to be stated in the
     * metadata, or the release is recorded as built with {@code 0.0.0} and no compatible-version query finds it.
     */
    private static void publishAsNonPlatformExtension() throws IOException {
        ArtifactCoords coords = ArtifactCoords.fromString(EXTENSION_GAV);
        Extension extension = Extension.builder()
                .setArtifact(coords)
                .setName("Quarkus Config Consul")
                .setDescription("Reads runtime configuration from Consul")
                .setMetadata(Map.of(Extension.MD_BUILT_WITH_QUARKUS_CORE, QUARKUS_CORE_VERSION))
                .build();
        StringWriter sw = new StringWriter();
        CatalogMapperHelper.serialize(extension, sw);

        given()
                .header("Token", "test")
                .contentType(ContentType.JSON)
                .body(sw.toString())
                .post("/admin/v1/extension")
                .then()
                .log().ifValidationFails()
                .statusCode(HttpURLConnection.HTTP_ACCEPTED);
    }

    /**
     * Imports a single-extension platform catalog for the given platform release, with the extension pinned to the
     * given category. The extension version deliberately stays the same across releases: that is the case the registry
     * used to ignore.
     */
    private void importCatalog(String platformVersion, String category) throws IOException {
        importCatalog(platformVersion, category, null);
    }

    /**
     * As above, additionally overriding the extension description when one is given.
     */
    private void importCatalog(String platformVersion, String category, String description) throws IOException {
        ExtensionCatalog.Mutable catalog = deserializeCatalog(readCatalogBytes(COMMUNITY_CATALOG));
        catalog.setId(descriptorId(platformVersion));
        catalog.setBom(ArtifactCoords.pom(PLATFORM_KEY, "quarkus-bom", platformVersion));
        catalog.setQuarkusCoreVersion(platformVersion);
        catalog.setMetadata(withPlatformVersion(catalog.getMetadata(), platformVersion));
        // Keeping a single extension makes the assertions readable; nothing here depends on the rest of the catalog
        catalog.setExtensions(catalog.getExtensions().stream()
                .filter(e -> EXTENSION_GA.equals(e.getArtifact().getGroupId() + ":" + e.getArtifact().getArtifactId()))
                .map(e -> {
                    Extension.Mutable mutable = e.mutable().setMetadata(withCategory(e.getMetadata(), category));
                    if (description != null) {
                        mutable.setDescription(description);
                    }
                    return (io.quarkus.registry.catalog.Extension) mutable.build();
                })
                .toList());

        StringWriter sw = new StringWriter();
        CatalogMapperHelper.serialize(catalog.build(), sw);
        postCatalog(sw.toString().getBytes(StandardCharsets.UTF_8), PLATFORM_KEY);
    }

    private static ArtifactCoords descriptorCoords(String platformVersion) {
        return ArtifactCoords.of(PLATFORM_KEY, "quarkus-bom-quarkus-platform-descriptor", platformVersion,
                Constants.JSON, platformVersion);
    }

    private static String descriptorId(String platformVersion) {
        return descriptorCoords(platformVersion).toString();
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
