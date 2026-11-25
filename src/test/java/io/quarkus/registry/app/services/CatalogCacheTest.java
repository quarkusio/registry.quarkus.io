package io.quarkus.registry.app.services;

import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;

import java.net.HttpURLConnection;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Test;

import io.quarkus.narayana.jta.QuarkusTransaction;
import io.quarkus.registry.app.BaseTest;
import io.quarkus.registry.app.maven.HashUtil;
import io.quarkus.registry.app.maven.cache.MavenCache;
import io.quarkus.registry.app.model.Extension;
import io.quarkus.registry.app.model.ExtensionRelease;
import io.quarkus.registry.app.model.Platform;
import io.quarkus.registry.app.model.PlatformRelease;
import io.quarkus.registry.app.model.PlatformStream;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;

/**
 * The generated catalogs are cached per Quarkus version, so that an artifact and its checksum siblings are built
 * from a single read of the database and so that the burst of requests following an invalidation collapses onto one
 * computation.
 * <p>
 * Each test changes the database <em>without</em> going through the admin API, which would clear the cache, and then
 * asks for a URL the response cache has never seen. That isolates the catalog cache from the per-URL response cache
 * in {@code MavenCacheFilter}.
 */
@QuarkusTest
@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class CatalogCacheTest extends BaseTest {

    private static final String QUARKUS_CORE = "2.0.0.Final";

    private static final String PREFIX = "/maven/io/quarkus/registry/";

    private static final String NON_PLATFORM_URL = PREFIX + "quarkus-non-platform-extensions/1.0-SNAPSHOT/"
            + "quarkus-non-platform-extensions-1.0-SNAPSHOT-" + QUARKUS_CORE + ".json";

    private static final String PLATFORMS_URL = PREFIX + "quarkus-platforms/1.0-SNAPSHOT/"
            + "quarkus-platforms-1.0-SNAPSHOT-all.json";

    @Inject
    MavenCache mavenCache;

    @BeforeEach
    @Transactional
    void setUp() {
        createExtensionRelease("foo-extension", "1.0.0");

        PlatformStream stream = new PlatformStream();
        stream.platform = Platform.findByKey("io.quarkus.platform").orElseThrow();
        stream.streamKey = "2.0";
        stream.persistAndFlush();

        PlatformRelease platformRelease = new PlatformRelease();
        platformRelease.platformStream = stream;
        platformRelease.version = QUARKUS_CORE;
        platformRelease.quarkusCoreVersion = QUARKUS_CORE;
        platformRelease.bom = "io.quarkus.platform:quarkus-bom::pom:" + QUARKUS_CORE;
        platformRelease.persistAndFlush();
    }

    @Test
    void non_platform_catalog_is_built_once_per_cache_generation() throws Exception {
        String body = get(NON_PLATFORM_URL);

        QuarkusTransaction.requiringNew().run(() -> createExtensionRelease("bar-extension", "2.0.0"));

        // Never-requested URL, so the response cache cannot answer it: the catalog itself must have been cached
        assertThat(get(NON_PLATFORM_URL + ".sha1")).isEqualTo(HashUtil.sha1(body));

        mavenCache.clear();

        String refreshed = get(NON_PLATFORM_URL);
        assertThat(refreshed).isNotEqualTo(body).contains("bar-extension");
        assertThat(get(NON_PLATFORM_URL + ".md5")).isEqualTo(HashUtil.md5(refreshed));
    }

    @Test
    void platform_catalog_is_built_once_per_cache_generation() throws Exception {
        String body = get(PLATFORMS_URL);

        QuarkusTransaction.requiringNew().run(() -> {
            PlatformStream stream = new PlatformStream();
            stream.platform = Platform.findByKey("io.quarkus.platform").orElseThrow();
            stream.streamKey = "3.0";
            stream.persistAndFlush();

            PlatformRelease release = new PlatformRelease();
            release.platformStream = stream;
            release.version = "3.0.0.Final";
            release.quarkusCoreVersion = "3.0.0.Final";
            release.bom = "io.quarkus.platform:quarkus-bom::pom:3.0.0.Final";
            release.persistAndFlush();
        });

        assertThat(get(PLATFORMS_URL + ".sha1")).isEqualTo(HashUtil.sha1(body));

        mavenCache.clear();

        String refreshed = get(PLATFORMS_URL);
        assertThat(refreshed).isNotEqualTo(body).contains("3.0.0.Final");
    }

    @Test
    void missing_platform_catalog_is_still_reported_as_not_found() {
        given()
                .get(PREFIX + "quarkus-platforms/1.0-SNAPSHOT/quarkus-platforms-1.0-SNAPSHOT-0.23.2.json")
                .then()
                .statusCode(HttpURLConnection.HTTP_NOT_FOUND)
                .header("X-Reason", "No platforms found");
    }

    private static void createExtensionRelease(String artifactId, String version) {
        Extension extension = new Extension();
        extension.name = artifactId;
        extension.description = "A " + artifactId;
        extension.groupId = "foo.bar";
        extension.artifactId = artifactId;
        extension.persistAndFlush();

        ExtensionRelease extensionRelease = new ExtensionRelease();
        extensionRelease.extension = extension;
        extensionRelease.version = version;
        extensionRelease.quarkusCoreVersion = QUARKUS_CORE;
        extensionRelease.persistAndFlush();
    }

    private static String get(String url) {
        return given()
                .get(url)
                .then()
                .log().ifValidationFails()
                .statusCode(HttpURLConnection.HTTP_OK)
                .extract().body().asString();
    }
}
