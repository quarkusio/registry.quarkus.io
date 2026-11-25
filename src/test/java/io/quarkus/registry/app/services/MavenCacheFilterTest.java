package io.quarkus.registry.app.services;

import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;

import java.net.HttpURLConnection;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Test;

import io.quarkus.registry.app.BaseTest;
import io.quarkus.registry.app.maven.cache.MavenCache;
import io.quarkus.registry.app.model.Platform;
import io.quarkus.registry.app.model.PlatformRelease;
import io.quarkus.registry.app.model.PlatformStream;
import io.quarkus.test.junit.QuarkusTest;
import io.restassured.response.ExtractableResponse;
import io.restassured.response.Response;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;

@QuarkusTest
@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class MavenCacheFilterTest extends BaseTest {

    private static final String URL = "/maven/io/quarkus/registry/quarkus-platforms/1.0-SNAPSHOT/"
            + "quarkus-platforms-1.0-SNAPSHOT-all.json";

    @Inject
    MavenCache mavenCache;

    @BeforeEach
    @Transactional
    void setUp() {
        // The Maven cache is application scoped and outlives the per-test Flyway clean/migrate
        mavenCache.clear();
        InvalidateDuringResponseFilter.disarm();

        PlatformStream stream = new PlatformStream();
        stream.platform = Platform.findByKey("io.quarkus.platform").orElseThrow();
        stream.streamKey = "2.0";
        stream.persistAndFlush();

        PlatformRelease release = new PlatformRelease();
        release.platformStream = stream;
        release.version = "2.0.0.Final";
        release.quarkusCoreVersion = "2.0.0.Final";
        release.bom = "io.quarkus.platform:quarkus-bom::pom:2.0.0.Final";
        release.persistAndFlush();
    }

    @AfterEach
    void tearDown() {
        InvalidateDuringResponseFilter.disarm();
        // Do not leave this test's data cached for the next test class
        mavenCache.clear();
    }

    @Test
    void responses_are_normally_cached() {
        assertThat(servedFromCache(get(URL))).isFalse();
        assertThat(servedFromCache(get(URL))).isTrue();
    }

    @Test
    void a_response_invalidated_while_it_was_being_built_must_not_be_cached() {
        InvalidateDuringResponseFilter.armOnce();
        get(URL);

        // Without the generation check this response is stored *after* the invalidation that was meant to drop it,
        // so it is served from the cache until the next invalidation, which can be hours away.
        assertThat(servedFromCache(get(URL)))
                .describedAs("a response built before an invalidation must not be reinstated after it")
                .isFalse();

        // ...and normal caching resumes
        assertThat(servedFromCache(get(URL))).isTrue();
    }

    private static boolean servedFromCache(ExtractableResponse<Response> response) {
        return response.header("Via") != null;
    }

    private static ExtractableResponse<Response> get(String url) {
        return given()
                .get(url)
                .then()
                .log().ifValidationFails()
                .statusCode(HttpURLConnection.HTTP_OK)
                .extract();
    }
}
