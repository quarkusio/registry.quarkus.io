package io.quarkus.registry.app.services;

import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;

import java.io.InputStream;
import java.util.List;

import javax.inject.Inject;
import javax.transaction.Transactional;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;

import org.apache.maven.artifact.repository.metadata.Metadata;
import org.apache.maven.artifact.repository.metadata.SnapshotVersion;
import org.apache.maven.artifact.repository.metadata.io.xpp3.MetadataXpp3Reader;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import io.quarkus.registry.app.maven.HashUtil;
import io.quarkus.registry.app.maven.MavenConfig;
import io.quarkus.registry.app.model.Platform;
import io.quarkus.registry.app.model.PlatformRelease;
import io.quarkus.registry.app.model.PlatformStream;
import io.quarkus.test.junit.QuarkusTest;

@QuarkusTest
public class MetadataTest {

    @Inject
    MavenConfig mavenConfig;

    @BeforeAll
    @Transactional
    static void setUp() {
        Platform platform = Platform.findByKey("io.quarkus.platform").get();
        PlatformStream stream20 = new PlatformStream();
        stream20.platform = platform;
        stream20.streamKey = "2.0";
        stream20.persistAndFlush();

        PlatformRelease release201 = new PlatformRelease();
        release201.platformStream = stream20;
        release201.version = "2.0.1.Final";
        release201.quarkusCoreVersion = "2.0.1.Final";
        release201.persistAndFlush();

        PlatformRelease release202 = new PlatformRelease();
        release202.platformStream = stream20;
        release202.version = "2.0.2.Final";
        release202.quarkusCoreVersion = "2.0.2.Final";
        release202.persistAndFlush();

        PlatformStream stream21 = new PlatformStream();
        stream21.platform = platform;
        stream21.streamKey = "2.1";
        stream21.persistAndFlush();

        PlatformRelease release210CR1 = new PlatformRelease();
        release210CR1.platformStream = stream21;
        release210CR1.version = "2.1.0.Final";
        release210CR1.quarkusCoreVersion = "2.1.0.Final";
        release210CR1.persistAndFlush();
    }

    @Test
    public void should_contain_version() throws Exception {
        given()
                .get("/maven/"
                        + mavenConfig.getRegistryGroupId().replace('.', '/')
                        + "/quarkus-platforms/1.0-SNAPSHOT/maven-metadata.xml")
                .then()
                .statusCode(200)
                .header(HttpHeaders.CONTENT_TYPE, containsString(MediaType.APPLICATION_XML))
                .body("metadata.version", is("1.0-SNAPSHOT"));
    }

    @Test
    public void should_have_classifiers() throws Exception {
        InputStream is = given()
                .get("/maven/" +
                        mavenConfig.getRegistryGroupId().replace('.', '/')
                        + "/quarkus-platforms/1.0-SNAPSHOT/maven-metadata.xml")
                .then()
                .statusCode(200)
                .header(HttpHeaders.CONTENT_TYPE, containsString(MediaType.APPLICATION_XML))
                .extract().asInputStream();
        Metadata metadata = new MetadataXpp3Reader().read(is);
        List<SnapshotVersion> snapshotVersions = metadata.getVersioning().getSnapshotVersions();
        assertThat(snapshotVersions).hasSize(5);
        assertThat(snapshotVersions)
                .extracting("classifier")
                .contains("2.0.2.Final", "2.0.1.Final", "2.1.0.Final");
    }

    @Test
    public void should_match_checksum() throws Exception {
        String metadata = given()
                .get("/maven/"
                        + mavenConfig.getRegistryGroupId().replace('.', '/')
                        + "/quarkus-platforms/1.0-SNAPSHOT/maven-metadata.xml")
                .then()
                .statusCode(200)
                .header(HttpHeaders.CONTENT_TYPE, containsString(MediaType.APPLICATION_XML))
                .extract().body().asString();
        String expectedSha1 = HashUtil.sha1(metadata);
        given()
                .get("/maven/"
                        + mavenConfig.getRegistryGroupId().replace('.', '/')
                        + "/quarkus-platforms/1.0-SNAPSHOT/maven-metadata.xml.sha1")
                .then()
                .statusCode(200)
                .header(HttpHeaders.CONTENT_TYPE, containsString(MediaType.TEXT_PLAIN))
                .body(equalTo(expectedSha1));
    }

    @AfterAll
    @Transactional
    static void tearDown() {
        PlatformRelease.deleteAll();
        PlatformStream.deleteAll();
    }
}
