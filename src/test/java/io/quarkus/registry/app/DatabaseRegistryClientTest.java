package io.quarkus.registry.app;

import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.CoreMatchers.hasItem;
import static org.hamcrest.CoreMatchers.hasItems;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.collection.IsCollectionWithSize.hasSize;
import static org.hamcrest.collection.IsMapContaining.hasKey;

import java.net.HttpURLConnection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.jakarta.rs.yaml.YAMLMediaTypes;

import io.quarkus.registry.app.maven.MavenConfig;
import io.quarkus.registry.app.model.Extension;
import io.quarkus.registry.app.model.ExtensionRelease;
import io.quarkus.registry.app.model.ExtensionReleaseCompatibility;
import io.quarkus.registry.app.model.Platform;
import io.quarkus.registry.app.model.PlatformExtension;
import io.quarkus.registry.app.model.PlatformRelease;
import io.quarkus.registry.app.model.PlatformStream;
import io.quarkus.registry.config.RegistriesConfig;
import io.quarkus.registry.config.RegistriesConfigImpl;
import io.quarkus.registry.config.RegistriesConfigMapperHelper;
import io.quarkus.registry.config.RegistryConfig;
import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;
import io.restassured.internal.mapping.Jackson2Mapper;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.core.MediaType;

@QuarkusTest
@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class DatabaseRegistryClientTest extends BaseTest {

    @Inject
    MavenConfig mavenConfig;

    @BeforeEach
    @Transactional
    void setUp() {
        {
            Platform platform = Platform.findByKey("io.quarkus.platform").get();
            PlatformStream stream20 = new PlatformStream();
            stream20.platform = platform;
            stream20.streamKey = "2.0";
            stream20.persistAndFlush();

            PlatformRelease release201 = new PlatformRelease();
            release201.platformStream = stream20;
            release201.version = "2.0.1.Final";
            release201.quarkusCoreVersion = release201.version;
            release201.bom = "io.quarkus.platform:quarkus-bom::pom:2.0.1.Final";
            release201.persistAndFlush();

            PlatformRelease release202 = new PlatformRelease();
            release202.platformStream = stream20;
            release202.version = "2.0.2.Final";
            release202.quarkusCoreVersion = release202.version;
            release202.bom = "io.quarkus.platform:quarkus-bom::pom:2.0.2.Final";
            release202.persistAndFlush();

            PlatformStream stream21 = new PlatformStream();
            stream21.platform = platform;
            stream21.streamKey = "2.1";
            stream21.lts = true;
            stream21.persistAndFlush();

            PlatformRelease release210CR1 = new PlatformRelease();
            release210CR1.platformStream = stream21;
            release210CR1.version = "2.1.0.CR1";
            release210CR1.quarkusCoreVersion = release210CR1.version;
            release210CR1.bom = "io.quarkus.platform:quarkus-bom::pom:2.1.0.CR1";
            release210CR1.persistAndFlush();

            PlatformRelease release210Final = new PlatformRelease();
            release210Final.platformStream = stream21;
            release210Final.version = "2.1.0.Final";
            release210Final.quarkusCoreVersion = release210Final.version;
            release210Final.unlisted = true;
            release210Final.bom = "io.quarkus.platform:quarkus-bom::pom:2.1.0.Final";
            release210Final.persistAndFlush();

            PlatformStream stream22 = new PlatformStream();
            stream22.platform = platform;
            stream22.streamKey = "2.2";
            stream22.persistAndFlush();

            PlatformRelease release220SNAPSHOT = new PlatformRelease();
            release220SNAPSHOT.platformStream = stream22;
            release220SNAPSHOT.version = "2.2.0-SNAPSHOT";
            release220SNAPSHOT.quarkusCoreVersion = release220SNAPSHOT.version;
            release220SNAPSHOT.bom = "io.quarkus.platform:quarkus-bom::pom:2.2.0-SNAPSHOT";
            release220SNAPSHOT.persistAndFlush();

            Extension extension = new Extension();
            extension.name = "Foo";
            extension.description = "A Foo Extension";
            extension.groupId = "foo.bar";
            extension.artifactId = "foo-extension";
            extension.persist();
            {
                ExtensionRelease extensionRelease = new ExtensionRelease();
                extensionRelease.extension = extension;
                extensionRelease.version = "1.0.0";
                extensionRelease.quarkusCoreVersion = "2.0.0.Final";
                extensionRelease.persist();
            }
            {
                ExtensionRelease extensionRelease = new ExtensionRelease();
                extensionRelease.extension = extension;
                extensionRelease.version = "1.1.0";
                extensionRelease.quarkusCoreVersion = "2.1.0.Final";
                extensionRelease.persist();

                ExtensionReleaseCompatibility erc = new ExtensionReleaseCompatibility();
                erc.extensionRelease = extensionRelease;
                erc.quarkusCoreVersion = "2.0.0.Final";
                erc.compatible = true;
                erc.persist();
            }

            {
                Extension newExtension = new Extension();
                newExtension.name = "Bar";
                newExtension.description = "A Bar Extension";
                newExtension.groupId = "foo.bar";
                newExtension.artifactId = "bar-extension";
                newExtension.persist();
                // Add a few releases to exercise the sorting
                {
                    ExtensionRelease extensionRelease = new ExtensionRelease();
                    extensionRelease.extension = newExtension;
                    extensionRelease.version = "0.2.2";
                    extensionRelease.quarkusCoreVersion = "3.0.0.Final";
                    extensionRelease.metadata = Map.of("someKey", "somevalue");
                    extensionRelease.persist();
                }
                {
                    ExtensionRelease extensionRelease = new ExtensionRelease();
                    extensionRelease.extension = newExtension;
                    extensionRelease.version = "0.4.2";
                    extensionRelease.quarkusCoreVersion = "3.0.0.Final";
                    extensionRelease.metadata = Map.of("aKey", "avalue");
                    extensionRelease.persist();
                }
                {
                    ExtensionRelease extensionRelease = new ExtensionRelease();
                    extensionRelease.extension = newExtension;
                    extensionRelease.version = "0.1.0";
                    extensionRelease.quarkusCoreVersion = "1.0.0.Final";
                    extensionRelease.persist();
                }
            }

            // An extension which is part of a platform
            {
                Extension newExtension = new Extension();
                newExtension.name = "Baz";
                newExtension.description = "A Baz Extension";
                newExtension.groupId = "foo.bar";
                newExtension.artifactId = "baz-extension";
                newExtension.persist();
                {
                    ExtensionRelease extensionRelease = new ExtensionRelease();
                    extensionRelease.extension = newExtension;
                    extensionRelease.version = "0.6.7";
                    extensionRelease.quarkusCoreVersion = "3.0.0.Final";
                    {
                        Map<String, Object> metadata = new HashMap<>();
                        metadata.put("indKey", "indvalue");
                        metadata.put("commonKey", "indcommonvalue");
                        metadata.put("nested", Map.of("key1", "value1"));
                        extensionRelease.metadata = metadata;
                    }
                    extensionRelease.persist();

                    PlatformExtension platformExtension = new PlatformExtension();
                    platformExtension.extensionRelease = extensionRelease;
                    platformExtension.platformRelease = release210Final;
                    {
                        Map<String, Object> metadata = new HashMap<>();
                        metadata.put("platKey", "platvalue");
                        metadata.put("commonKey", "platcommonvalue");
                        platformExtension.metadata = metadata;
                    }

                    platformExtension.persist();
                }

            }
        }
    }

    @Test
    void should_return_no_content_if_no_platforms_found() {
        given()
                .get("/client/platforms?v=1.1.0")
                .then()
                .statusCode(HttpURLConnection.HTTP_NO_CONTENT);
    }

    @Test
    void should_return_application_json_as_response_type() {
        given()
                .accept("application/json;custom=aaaaaaaaaaaaaaaaaa;charset=utf-7")
                .get("/client/platforms")
                .then()
                .statusCode(HttpURLConnection.HTTP_OK)
                .contentType(ContentType.JSON);
    }

    @Test
    void should_return_unacceptable_on_invalid_accept_headers() {
        given()
                .accept(MediaType.APPLICATION_XML)
                .get("/client/platforms")
                .then()
                .statusCode(HttpURLConnection.HTTP_NOT_ACCEPTABLE);
    }

    @Test
    void should_return_only_extensions_matching_compatible_quarkus_core() {
        given()
                .get("/client/non-platform-extensions?v=2.0.0.Final")
                .then()
                .statusCode(HttpURLConnection.HTTP_OK)
                .body("extensions", hasSize(1))
                .body("extensions[0].artifact", is("foo.bar:foo-extension::jar:1.1.0"));
    }

    /**
     * Test that we don't assume forwards compatibility across major versions.
     */
    @Test
    void should_return_only_extensions_matching_compatible_quarkus_core_not_earlier_versions() {
        given()
                .get("/client/non-platform-extensions?v=3.0.0.Final")
                .then()
                .statusCode(HttpURLConnection.HTTP_OK)
                .body("extensions", hasSize(1))
                .body("extensions[0].artifact", is("foo.bar:bar-extension::jar:0.4.2"))
                // We want bar, which is written for Quarkus 3, but not foo, which is written for Quarkus 2
                .body("extensions[0].metadata", hasKey("aKey"))
                .body("extensions[0].metadata.aKey", is("avalue"));

    }

    @Test
    void should_return_all_extensions() {
        given()
                .get("/client/extensions/all")
                .then()
                .statusCode(HttpURLConnection.HTTP_OK)
                .body("bom", nullValue()) // These values don't make sense when listing all extensions
                .body("platform", nullValue())
                .body("derivedFrom", nullValue()) // These values don't make sense when listing all extensions
                .body("quarkusCoreValue", nullValue())
                .body("extensions", hasSize(3))
                .body("extensions[0].artifact", is("foo.bar:foo-extension::jar:1.1.0"))
                .body("extensions[1].artifact", is("foo.bar:bar-extension::jar:0.4.2"))
                .body("extensions[2].artifact", is("foo.bar:baz-extension::jar:0.6.7"))
                // Now lets look at the reported platform information and make sure its accurate
                .body("extensions[2].origins",
                        is(List.of(
                                "io.quarkus.platform:quarkus-bom-quarkus-platform-descriptor:2.1.0.Final:json:2.1.0.Final")))
                .body("extensions[1].origins",
                        is(List.of("io.quarkus.registry:quarkus-non-platform-extensions:3.0.0.Final:json:1.0-SNAPSHOT")))
                .body("extensions[0].origins",
                        is(List.of("io.quarkus.registry:quarkus-non-platform-extensions:2.1.0.Final:json:1.0-SNAPSHOT")))
                // Make sure we have metadata
                .body("extensions[1].metadata", hasKey("aKey"))
                .body("extensions[1].metadata.aKey", is("avalue"))
                // For platform extensions, the metadata should include what's in the PlatformExtension object and what's on the ExtensionRelease object
                .body("extensions[2].metadata", hasKey("platKey"))
                .body("extensions[2].metadata.platKey", is("platvalue"))
                .body("extensions[2].metadata", hasKey("indKey"))
                .body("extensions[2].metadata.indKey", is("indvalue"))
                // Where there's a key overlap, we should favour the platform (in principle keys will never overlap because if there is platform-level metadata there will not be release-level metadata, but we will make sure we do the right thing even if our assumption is wrong)
                .body("extensions[2].metadata.commonKey", is("platcommonvalue"));

    }

    @Test
    void should_handle_nested_metadata_for_extensions() {
        given()
                .get("/client/extensions/all")
                .then()
                .statusCode(HttpURLConnection.HTTP_OK)
                // Make sure we metadata with a nested structure survived ok
                .body("extensions[2].metadata", hasKey("nested"))
                .body("extensions[2].metadata.nested", hasKey("key1"))
                .body("extensions[2].metadata.nested.key1", is("value1"));

    }

    @Test
    void should_return_all_platforms_with_current_stream_id() {
        given()
                .get("/client/platforms/all")
                .then()
                .statusCode(HttpURLConnection.HTTP_OK)
                .contentType(ContentType.JSON)
                .body("platforms", hasSize(1),
                        "platforms[0].streams", hasSize(3),
                        "platforms[0].streams.id", hasItems("2.2", "2.1", "2.0"),
                        "platforms[0].current-stream-id", is("2.1"));
    }

    @Test
    void should_not_return_unlisted_platforms() {
        given()
                .get("/client/platforms")
                .then()
                .statusCode(HttpURLConnection.HTTP_OK)
                .contentType(ContentType.JSON)
                .body("platforms", hasSize(1),
                        "platforms[0].streams", hasSize(3),
                        "platforms[0].streams.id", hasItems("2.1", "2.0"),
                        "platforms[0].streams[1].releases", not(hasItem("2.1.0.Final")));

    }

    @Test
    void should_return_client_configuration_yaml() {
        RegistriesConfig registriesConfig = given()
                .get("/client/config.yaml")
                .then()
                .statusCode(200)
                .log().all()
                .contentType(YAMLMediaTypes.APPLICATION_JACKSON_YAML)
                .extract().body()
                .as(RegistriesConfigImpl.Builder.class,
                        new Jackson2Mapper((type, s) -> RegistriesConfigMapperHelper.yamlMapper()))
                .build();
        assertThat(registriesConfig.getRegistries()).hasSize(1);
        RegistryConfig registryConfig = registriesConfig.getRegistries().get(0);
        assertThat(registryConfig.getId()).isEqualTo(mavenConfig.getRegistryId());
        assertThat(registryConfig.getMaven().getRepository().getUrl()).isEqualTo(mavenConfig.getRegistryUrl());
    }

    @Test
    void should_check_if_lts_is_returned() {
        given()
                .get("/client/platforms/all")
                .then()
                .statusCode(HttpURLConnection.HTTP_OK)
                .contentType(ContentType.JSON)
                .body("platforms[0].streams.find{it.id = '2.1'}.lts", is(true));
    }

    @Test
    void should_consider_snapshot_prefinal() {
        given()
                .get("/client/platforms")
                .then()
                .statusCode(HttpURLConnection.HTTP_OK)
                .contentType(ContentType.JSON)
                .log().ifValidationFails()
                .body("platforms", hasSize(1),
                        "platforms[0].streams", hasSize(3),
                        "platforms[0].streams.id", hasItems("2.2"),
                        "platforms[0].current-stream-id", is("2.0"));
    }

}
