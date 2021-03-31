package io.quarkus.registry.app.maven;

import java.util.Arrays;

import org.apache.maven.artifact.Artifact;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ArtifactParserTest {


    @Test
    public void testClassifier() {
        String path = "io/quarkus/registry/quarkus-non-platform-extensions/1.0-SNAPSHOT/quarkus-non-platform-extensions-1.0-SNAPSHOT-1.13.0.Final.json";
        Artifact artifact = ArtifactParser.parseArtifact(Arrays.asList(path.split("/")));
        assertThat(artifact.getGroupId()).withFailMessage("Group ID does not match")
                .isEqualTo("io.quarkus.registry");
        assertThat(artifact.getArtifactId()).withFailMessage("Artifact ID does not match")
                .isEqualTo("quarkus-non-platform-extensions");
        assertThat(artifact.getVersion()).withFailMessage("Version does not match")
                .isEqualTo("1.0-SNAPSHOT");
        assertThat(artifact.getType()).withFailMessage("Type does not match")
                .isEqualTo("json");
        assertThat(artifact.getClassifier())
                .isEqualTo("1.13.0.Final");
    }
    @Test
    public void testSha1() {
        String path = "io/quarkus/registry/quarkus-non-platform-extensions/1.0-SNAPSHOT/quarkus-non-platform-extensions-1.0-SNAPSHOT-1.13.0.Final.json.sha1";
        Artifact artifact = ArtifactParser.parseArtifact(Arrays.asList(path.split("/")));
        assertThat(artifact.getGroupId()).isEqualTo("io.quarkus.registry");
        assertThat(artifact.getArtifactId()).isEqualTo("quarkus-non-platform-extensions");
        assertThat(artifact.getVersion()).isEqualTo("1.0-SNAPSHOT");
        assertThat(artifact.getType()).isEqualTo("json.sha1");
        assertThat(artifact.getClassifier()).isEqualTo("1.13.0.Final");
    }

    @Test
    public void testMavenMetadata() {
        String path = "io/quarkus/registry/quarkus-non-platform-extensions/maven-metadata.xml";
        Artifact artifact = ArtifactParser.parseArtifact(Arrays.asList(path.split("/")));
        assertThat(artifact.getGroupId()).isEqualTo("io.quarkus.registry");
        assertThat(artifact.getArtifactId()).isEqualTo("quarkus-non-platform-extensions");
        assertThat(artifact.getVersion()).isEqualTo(MavenConfig.VERSION);
        assertThat(artifact.getType()).isEqualTo("maven-metadata.xml");
    }

    @Test
    public void testVersionedMavenMetadata() {
        String path = "io/quarkus/registry/quarkus-non-platform-extensions/1.0-SNAPSHOT/maven-metadata.xml";
        Artifact artifact = ArtifactParser.parseArtifact(Arrays.asList(path.split("/")));
        assertThat(artifact.getGroupId()).isEqualTo("io.quarkus.registry");
        assertThat(artifact.getArtifactId()).isEqualTo("quarkus-non-platform-extensions");
        assertThat(artifact.getVersion()).isEqualTo("1.0-SNAPSHOT");
        assertThat(artifact.getType()).isEqualTo("maven-metadata.xml");
    }
}