package io.quarkus.registry.app.services;

import static io.quarkus.registry.app.maven.ArtifactContentProvider.HEADER_CHECKSUM_MD5;
import static io.quarkus.registry.app.maven.ArtifactContentProvider.HEADER_CHECKSUM_SHA1;
import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;

import java.net.HttpURLConnection;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Test;

import io.quarkus.registry.app.BaseTest;
import io.quarkus.registry.app.maven.ArtifactParser;
import io.quarkus.registry.app.maven.HashUtil;
import io.quarkus.registry.app.model.Extension;
import io.quarkus.registry.app.model.ExtensionRelease;
import io.quarkus.registry.app.model.Platform;
import io.quarkus.registry.app.model.PlatformRelease;
import io.quarkus.registry.app.model.PlatformStream;
import io.quarkus.test.junit.QuarkusTest;
import io.restassured.response.ExtractableResponse;
import io.restassured.response.Response;
import jakarta.transaction.Transactional;

/**
 * The registry generates its Maven content on the fly from the current database state, so an artifact and its
 * {@code .sha1}/{@code .md5} siblings are served by two independent requests. Checksums are therefore also returned
 * in the {@code x-checksum-*} headers of the artifact response itself, where they cannot drift away from the bytes
 * they describe.
 *
 * @see <a href="https://github.com/quarkusio/quarkus/issues/54974">quarkusio/quarkus#54974</a>
 */
@QuarkusTest
@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class MavenChecksumTest extends BaseTest {

    private static final String QUARKUS_CORE = "2.0.0.Final";

    private static final String PREFIX = "/maven/io/quarkus/registry/";

    private static final String CATALOG_URL = PREFIX + "quarkus-non-platform-extensions/1.0-SNAPSHOT/"
            + "quarkus-non-platform-extensions-1.0-SNAPSHOT-" + QUARKUS_CORE + ".json";

    @BeforeEach
    @Transactional
    void setUp() {
        Extension extension = new Extension();
        extension.name = "Foo";
        extension.description = "A Foo Extension";
        extension.groupId = "foo.bar";
        extension.artifactId = "foo-extension";
        extension.persistAndFlush();

        ExtensionRelease extensionRelease = new ExtensionRelease();
        extensionRelease.extension = extension;
        extensionRelease.version = "1.0.0";
        extensionRelease.quarkusCoreVersion = QUARKUS_CORE;
        extensionRelease.persistAndFlush();

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

    /** Every artifact this registry serves, including the one that cannot be compared with its {@code .sha1}. */
    private static final List<String> ARTIFACT_URLS = List.of(
            PREFIX + "quarkus-platforms/1.0-SNAPSHOT/maven-metadata.xml",
            PREFIX + "quarkus-platforms/1.0-SNAPSHOT/quarkus-platforms-1.0-SNAPSHOT.pom",
            PREFIX + "quarkus-platforms/1.0-SNAPSHOT/quarkus-platforms-1.0-SNAPSHOT-all.json",
            PREFIX + "quarkus-registry-descriptor/1.0-SNAPSHOT/quarkus-registry-descriptor-1.0-SNAPSHOT.json",
            CATALOG_URL);

    @Test
    void etag_must_reuse_the_body_checksum_instead_of_hashing_again() throws Exception {
        for (String url : ARTIFACT_URLS) {
            ExtractableResponse<Response> response = get(url);
            // Quoted, as RFC 7232 requires of an entity-tag, but still the MD5 we already computed
            assertThat(response.header("ETag"))
                    .describedAs("ETag of %s should be the already computed MD5 of the body", url)
                    .isEqualTo('"' + response.header(HEADER_CHECKSUM_MD5) + '"')
                    .isEqualTo('"' + HashUtil.md5(response.body().asString()) + '"');
        }
    }

    @Test
    void artifact_responses_must_carry_their_own_checksums() throws Exception {
        for (String url : ARTIFACT_URLS) {
            ExtractableResponse<Response> response = get(url);
            String body = response.body().asString();
            assertThat(response.header(HEADER_CHECKSUM_SHA1))
                    .describedAs("SHA-1 header of %s", url)
                    .isEqualTo(HashUtil.sha1(body));
            assertThat(response.header(HEADER_CHECKSUM_MD5))
                    .describedAs("MD5 header of %s", url)
                    .isEqualTo(HashUtil.md5(body));

            // The same must hold when the response is replayed from the Maven cache
            ExtractableResponse<Response> cached = get(url);
            assertThat(cached.header("Via")).describedAs("%s should be cached", url).isNotNull();
            assertThat(cached.header(HEADER_CHECKSUM_SHA1))
                    .describedAs("SHA-1 header of cached %s", url)
                    .isEqualTo(HashUtil.sha1(cached.body().asString()));
            assertThat(cached.header(HEADER_CHECKSUM_MD5))
                    .describedAs("MD5 header of cached %s", url)
                    .isEqualTo(HashUtil.md5(cached.body().asString()));
        }
    }

    @Test
    void standalone_checksum_files_must_still_be_served() throws Exception {
        for (String url : ARTIFACT_URLS) {
            // maven-metadata.xml carries the registry's last-updated timestamp, which the cache refresh moves
            // independently of this test, so its body and its .sha1 cannot be compared reliably -- the very
            // problem the x-checksum-* headers exist to solve.
            if (url.endsWith(ArtifactParser.MAVEN_METADATA_XML)) {
                assertThat(get(url + ".sha1").body().asString()).hasSize(40);
                assertThat(get(url + ".md5").body().asString()).hasSize(32);
                continue;
            }
            String body = get(url).body().asString();
            assertThat(get(url + ".sha1").body().asString())
                    .describedAs("SHA-1 file of %s", url)
                    .isEqualTo(HashUtil.sha1(body));
            assertThat(get(url + ".md5").body().asString())
                    .describedAs("MD5 file of %s", url)
                    .isEqualTo(HashUtil.md5(body));
        }
    }

    @Test
    void checksum_must_still_describe_the_body_after_the_registry_changed() throws Exception {
        // A Maven client downloads the artifact. Its checksums come back in the same response.
        ExtractableResponse<Response> response = get(CATALOG_URL);
        String body = response.body().asString();
        String inlineSha1 = response.header(HEADER_CHECKSUM_SHA1);
        assertThat(inlineSha1).isEqualTo(HashUtil.sha1(body));

        // The registry state moves on, as it does on every publish.
        given()
                .formParams("metadata", Map.of("foo", "bar"))
                .header("Token", "test")
                .patch("/admin/v1/extension/foo.bar/foo-extension/1.0.0")
                .then()
                .statusCode(HttpURLConnection.HTTP_ACCEPTED);

        // The standalone checksum file is regenerated from the *new* state...
        String newBody = get(CATALOG_URL).body().asString();
        assertThat(newBody).isNotEqualTo(body);
        assertThat(get(CATALOG_URL + ".sha1").body().asString()).isEqualTo(HashUtil.sha1(newBody));

        // ...so it no longer describes the body the client downloaded, whereas the checksum that
        // travelled with that body still does. That difference is quarkusio/quarkus#54974.
        assertThat(inlineSha1).isEqualTo(HashUtil.sha1(body));
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
