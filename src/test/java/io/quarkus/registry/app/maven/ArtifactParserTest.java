package io.quarkus.registry.app.maven;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import javax.ws.rs.core.PathSegment;

import org.assertj.core.api.SoftAssertions;
import org.jboss.resteasy.reactive.common.util.PathSegmentImpl;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import io.quarkus.maven.dependency.ArtifactCoords;

class ArtifactParserTest {

    @Test
    void shouldThrowParseError() {
        List<PathSegment> pathSegments = toSegments("io/quarkus/registry/quarkus-registry-descriptor/");
        assertThatIllegalArgumentException().isThrownBy(() -> ArtifactParser.parseCoords(pathSegments));
    }

    @Test
    public void testClassifier() {
        List<PathSegment> pathSegments = toSegments(
                "io/quarkus/registry/quarkus-non-platform-extensions/1.0-SNAPSHOT/quarkus-non-platform-extensions-1.0-SNAPSHOT-1.13.0.Final.json");
        ArtifactCoords artifact = ArtifactParser.parseCoords(pathSegments);
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
        List<PathSegment> pathSegments = toSegments(
                "io/quarkus/registry/quarkus-non-platform-extensions/1.0-SNAPSHOT/quarkus-non-platform-extensions-1.0-SNAPSHOT-1.13.0.Final.json.sha1");
        ArtifactCoords artifact = ArtifactParser.parseCoords(pathSegments);
        assertThat(artifact.getGroupId()).isEqualTo("io.quarkus.registry");
        assertThat(artifact.getArtifactId()).isEqualTo("quarkus-non-platform-extensions");
        assertThat(artifact.getVersion()).isEqualTo("1.0-SNAPSHOT");
        assertThat(artifact.getType()).isEqualTo("json");
        assertThat(artifact.getClassifier()).isEqualTo("1.13.0.Final");
    }

    @Test
    @Disabled
    public void testMavenMetadata() {
        List<PathSegment> pathSegments = toSegments("io/quarkus/registry/quarkus-non-platform-extensions/maven-metadata.xml");
        ArtifactCoords artifact = ArtifactParser.parseCoords(pathSegments);
        SoftAssertions softly = new SoftAssertions();
        softly.assertThat(artifact.getGroupId()).isEqualTo("io.quarkus.registry");
        softly.assertThat(artifact.getArtifactId()).isEqualTo("quarkus-non-platform-extensions");
        softly.assertThat(artifact.getVersion()).isEqualTo("1.0-SNAPSHOT");
        softly.assertThat(artifact.getType()).isEqualTo("maven-metadata.xml");
        softly.assertAll();
    }

    @Test
    public void testVersionedMavenMetadata() {
        List<PathSegment> pathSegments = toSegments(
                "io/quarkus/registry/quarkus-non-platform-extensions/1.0-SNAPSHOT/maven-metadata.xml");
        ArtifactCoords artifact = ArtifactParser.parseCoords(pathSegments);
        assertThat(artifact.getGroupId()).isEqualTo("io.quarkus.registry");
        assertThat(artifact.getArtifactId()).isEqualTo("quarkus-non-platform-extensions");
        assertThat(artifact.getVersion()).isEqualTo("1.0-SNAPSHOT");
        assertThat(artifact.getType()).isEqualTo("maven-metadata.xml");
    }

    @Test
    public void testVersionedSnapshotMavenMetadata() {
        List<PathSegment> pathSegments = toSegments(
                "io/quarkus/registry/quarkus-registry-descriptor/1.0-SNAPSHOT/quarkus-registry-descriptor-1.0-20210331.162601-1.json");
        ArtifactCoords artifact = ArtifactParser.parseCoords(pathSegments);
        assertThat(artifact.getGroupId()).isEqualTo("io.quarkus.registry");
        assertThat(artifact.getArtifactId()).isEqualTo("quarkus-registry-descriptor");
        assertThat(artifact.getVersion()).isEqualTo("1.0-SNAPSHOT");
        assertThat(artifact.getClassifier()).isEmpty();
        assertThat(artifact.getType()).isEqualTo("json");
    }

    @Test
    public void testVersionedSnapshotMavenMetadataWithClassifier() {
        List<PathSegment> pathSegments = toSegments(
                "io/quarkus/registry/quarkus-non-platform-extensions/1.0-SNAPSHOT/quarkus-non-platform-extensions-1.0-20210405.152106-1-1.13.0.Final.json");
        ArtifactCoords artifact = ArtifactParser.parseCoords(pathSegments);
        assertThat(artifact.getGroupId()).isEqualTo("io.quarkus.registry");
        assertThat(artifact.getArtifactId()).isEqualTo("quarkus-non-platform-extensions");
        assertThat(artifact.getVersion()).isEqualTo("1.0-SNAPSHOT");
        assertThat(artifact.getClassifier()).isEqualTo("1.13.0.Final");
        assertThat(artifact.getType()).isEqualTo("json");
    }

    private List<PathSegment> toSegments(String s) {
        return Arrays.stream(s.split("/"))
                .map(p -> new PathSegmentImpl(p, false))
                .collect(Collectors.toList());
    }

}
