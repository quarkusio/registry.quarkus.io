package io.quarkus.registry.app.endpoints.maven;

import java.io.IOException;
import java.io.StringWriter;

import javax.enterprise.context.ApplicationScoped;
import javax.ws.rs.core.UriInfo;

import io.quarkus.registry.app.model.Platform;
import io.quarkus.registry.app.model.PlatformRelease;
import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.repository.metadata.Metadata;
import org.apache.maven.artifact.repository.metadata.Versioning;
import org.apache.maven.artifact.repository.metadata.io.xpp3.MetadataXpp3Writer;

@ApplicationScoped
public class MetadataContentProvider implements ArtifactContentProvider {

    private static final MetadataXpp3Writer METADATA_WRITER = new MetadataXpp3Writer();
    private static final String MAVEN_METADATA_XML = "maven-metadata.xml";

    @Override
    public boolean supports(Artifact artifact, UriInfo uriInfo) {
        return MAVEN_METADATA_XML.equals(artifact.getType());
    }

    @Override
    public String provide(Artifact artifact, UriInfo uriInfo) throws IOException {
        String groupId = artifact.getGroupId();
        String artifactId = artifact.getArtifactId();
        Metadata metadata = Platform.findByGA(groupId, artifactId)
                .map(this::generateMetadata).orElse(null);

        return writeMetadata(metadata);
    }

    private Metadata generateMetadata(Platform platform) {
        Metadata newMetadata = new Metadata();
        newMetadata.setGroupId(platform.groupId);
        newMetadata.setArtifactId(platform.artifactId);
        Versioning versioning = new Versioning();
        newMetadata.setVersioning(versioning);
        for (PlatformRelease release : platform.releases) {
            versioning.addVersion(release.version);
        }
        return newMetadata;
    }

    private String writeMetadata(Metadata metadata) throws IOException {
        if (metadata == null) {
            return null;
        }
        StringWriter sw = new StringWriter();
        METADATA_WRITER.write(sw, metadata);
        return sw.toString();
    }
}
