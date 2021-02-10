package io.quarkus.registry.app.maven;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import org.apache.maven.artifact.Artifact;
import org.eclipse.microprofile.config.inject.ConfigProperty;

@ApplicationScoped
public class Config {

    @Inject
    @ConfigProperty(name = "quarkus.registry.groupId", defaultValue = "io.quarkus.registry")
    private String groupId;

    @Inject
    @ConfigProperty(name = "quarkus.registry.platform.artifactId", defaultValue = "quarkus-platforms")
    private String platformArtifactId;

    @Inject
    @ConfigProperty(name = "quarkus.registry.non-platform.artifactId", defaultValue = "quarkus-non-platform-extensions")
    private String nonPlatformArtifactId;

    public String getGroupId() {
        return groupId;
    }

    public void setGroupId(String groupId) {
        this.groupId = groupId;
    }

    public String getPlatformArtifactId() {
        return platformArtifactId;
    }

    public void setPlatformArtifactId(String platformArtifactId) {
        this.platformArtifactId = platformArtifactId;
    }

    public String getNonPlatformArtifactId() {
        return nonPlatformArtifactId;
    }

    public void setNonPlatformArtifactId(String nonPlatformArtifactId) {
        this.nonPlatformArtifactId = nonPlatformArtifactId;
    }

    public boolean supports(Artifact artifact) {
        return (getGroupId().equals(artifact.getGroupId()) &&
                (getPlatformArtifactId().equals(artifact.getArtifactId())) ||
                getNonPlatformArtifactId().equals(artifact.getArtifactId()));
    }
}
