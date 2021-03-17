package io.quarkus.registry.app.maven;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import org.apache.maven.artifact.Artifact;
import org.eclipse.microprofile.config.inject.ConfigProperty;

@ApplicationScoped
public class Config {

    @Inject
    @ConfigProperty(name = "registry.maven.groupId")
    String groupId;

    @Inject
    @ConfigProperty(name = "registry.maven.platform.artifactId")
    String platformArtifactId;

    @Inject
    @ConfigProperty(name = "registry.maven.non-platform.artifactId")
    String nonPlatformArtifactId;

    public String getGroupId() {
        return groupId;
    }

    public String getPlatformArtifactId() {
        return platformArtifactId;
    }

    public String getNonPlatformArtifactId() {
        return nonPlatformArtifactId;
    }

    public boolean supports(Artifact artifact) {
        return (getGroupId().equals(artifact.getGroupId()) &&
                (getPlatformArtifactId().equals(artifact.getArtifactId())) ||
                getNonPlatformArtifactId().equals(artifact.getArtifactId()));
    }
}
