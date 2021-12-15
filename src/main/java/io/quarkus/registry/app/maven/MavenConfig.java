package io.quarkus.registry.app.maven;

import java.util.Optional;

import javax.annotation.PostConstruct;
import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import org.eclipse.microprofile.config.inject.ConfigProperty;

import io.quarkus.maven.ArtifactCoords;
import io.quarkus.registry.Constants;

@ApplicationScoped
public class MavenConfig {

    @Inject
    @ConfigProperty(name = "quarkus.registry.id", defaultValue = Constants.DEFAULT_REGISTRY_ID)
    String registryId;

    @Inject
    @ConfigProperty(name = "quarkus.registry.maven-repo.url", defaultValue = Constants.DEFAULT_REGISTRY_MAVEN_REPO_URL)
    String registryUrl;

    @Inject
    @ConfigProperty(name = "quarkus.registry.groupId", defaultValue = Constants.DEFAULT_REGISTRY_GROUP_ID)
    String registryGroupId;

    @Inject
    @ConfigProperty(name = "quarkus.registry.non-platform-extensions.support", defaultValue = "true")
    boolean supportsNonPlatforms;

    @Inject
    @ConfigProperty(name = "quarkus.registry.quarkus-versions.expression")
    Optional<String> quarkusVersionsExpression;

    @Inject
    @ConfigProperty(name = "quarkus.registry.quarkus-versions.exclusive-provider")
    Optional<Boolean> quarkusVersionsExclusiveProvider;

    private ArtifactCoords nonPlatformExtensionCoords;

    @PostConstruct
    void init() {
        this.nonPlatformExtensionCoords = new ArtifactCoords(registryGroupId,
                Constants.DEFAULT_REGISTRY_NON_PLATFORM_EXTENSIONS_CATALOG_ARTIFACT_ID,
                Constants.JSON,
                Constants.DEFAULT_REGISTRY_ARTIFACT_VERSION);
    }

    public ArtifactCoords getNonPlatformExtensionCoords() {
        return nonPlatformExtensionCoords;
    }

    public boolean supports(ArtifactCoords artifact) {
        return matchesQuarkusPlatforms(artifact) ||
                matchesRegistryDescriptor(artifact) ||
                matchesNonPlatformExtensions(artifact);
    }

    /**
     * io.quarkus.registry:quarkus-platforms::json:1.0-SNAPSHOT
     * io.quarkus.registry:quarkus-platforms:<QUARKUS-VERSION>:json:1.0-SNAPSHOT
     *
     * A JSON file that lists the preferred versions of every registered platform (e.g. quarkus-bom, quarkus-universe-bom, etc).
     * It also indicates which platform is the default one (for project creation), e.g. the quarkus-universe-bom;
     */
    public boolean matchesQuarkusPlatforms(ArtifactCoords artifact) {
        return registryGroupId.equals(artifact.getGroupId()) &&
                Constants.DEFAULT_REGISTRY_PLATFORMS_CATALOG_ARTIFACT_ID.equals(artifact.getArtifactId()) &&
                Constants.DEFAULT_REGISTRY_ARTIFACT_VERSION.equals(artifact.getVersion());
    }

    /**
     * io.quarkus.registry:quarkus-non-platform-extensions:<QUARKUS-VERSION>:json:1.0-SNAPSHOT -
     *
     * JSON catalog of non-platform extensions that are compatible with a given Quarkus core version expressed
     * with <QUARKUS-VERSION> as the artifact’s classifier;
     */
    public boolean matchesNonPlatformExtensions(ArtifactCoords artifact) {
        return registryGroupId.equals(artifact.getGroupId()) &&
                Constants.DEFAULT_REGISTRY_NON_PLATFORM_EXTENSIONS_CATALOG_ARTIFACT_ID.equals(artifact.getArtifactId()) &&
                Constants.DEFAULT_REGISTRY_ARTIFACT_VERSION.equals(artifact.getVersion());
    }

    /**
     * io.quarkus.registry:quarkus-registry-descriptor::json:1.0-SNAPSHOT -
     *
     * The JSON registry descriptor which includes the default settings to communicate with the registry
     * (including specific groupId, artifactId and versions for the QER artifacts described above, Maven repository URL, etc).
     */
    public boolean matchesRegistryDescriptor(ArtifactCoords artifact) {
        return registryGroupId.equals(artifact.getGroupId()) &&
                Constants.DEFAULT_REGISTRY_DESCRIPTOR_ARTIFACT_ID.equals(artifact.getArtifactId()) &&
                Constants.DEFAULT_REGISTRY_ARTIFACT_VERSION.equals(artifact.getVersion());
    }

    public String getRegistryId() {
        return registryId;
    }

    public String getRegistryGroupId() {
        return registryGroupId;
    }

    public String getRegistryUrl() {
        return registryUrl;
    }

    public boolean supportsNonPlatforms() {
        return supportsNonPlatforms;
    }

    public Optional<String> getQuarkusVersionsExpression() {
        return quarkusVersionsExpression;
    }

    public Optional<Boolean> getQuarkusVersionsExclusiveProvider() {
        return quarkusVersionsExclusiveProvider;
    }
}
