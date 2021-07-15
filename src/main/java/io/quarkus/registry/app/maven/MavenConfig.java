package io.quarkus.registry.app.maven;

import javax.enterprise.context.ApplicationScoped;

import io.quarkus.maven.ArtifactCoords;

@ApplicationScoped
public class MavenConfig {

    static final String GROUP_ID = "io.quarkus.registry";

    static final String PLATFORM_ARTIFACT_ID = "quarkus-platforms";

    static final String PLATFORM_EXTENSIONS_ARTIFACT_ID = "quarkus-platform-extensions";
    static final String NON_PLATFORM_EXTENSIONS_ARTIFACT_ID = "quarkus-non-platform-extensions";

    static final String REGISTRY_ARTIFACT_ID = "quarkus-registry-descriptor";

    static final String VERSION = "1.0-SNAPSHOT";

    public static final ArtifactCoords PLATFORM_COORDS = new ArtifactCoords(MavenConfig.GROUP_ID, MavenConfig.PLATFORM_ARTIFACT_ID, "json", MavenConfig.VERSION);
    public static final ArtifactCoords NON_PLATFORM_EXTENSION_COORDS = new ArtifactCoords(MavenConfig.GROUP_ID, MavenConfig.NON_PLATFORM_EXTENSIONS_ARTIFACT_ID, "json", MavenConfig.VERSION);

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
        return GROUP_ID.equals(artifact.getGroupId()) &&
                PLATFORM_ARTIFACT_ID.equals(artifact.getArtifactId()) &&
                VERSION.equals(artifact.getVersion());
    }


    /**
     * io.quarkus.registry:quarkus-non-platform-extensions:<QUARKUS-VERSION>:json:1.0-SNAPSHOT -
     *
     * JSON catalog of non-platform extensions that are compatible with a given Quarkus core version expressed
     * with <QUARKUS-VERSION> as the artifact’s classifier;
     */
    public boolean matchesNonPlatformExtensions(ArtifactCoords artifact) {
        return GROUP_ID.equals(artifact.getGroupId()) &&
                NON_PLATFORM_EXTENSIONS_ARTIFACT_ID.equals(artifact.getArtifactId()) &&
                VERSION.equals(artifact.getVersion());
    }

    /**
     * io.quarkus.registry:quarkus-registry-descriptor::json:1.0-SNAPSHOT -
     *
     * The JSON registry descriptor which includes the default settings to communicate with the registry
     * (including specific groupId, artifactId and versions for the QER artifacts described above, Maven repository URL, etc).
     */
    public boolean matchesRegistryDescriptor(ArtifactCoords artifact) {
        return GROUP_ID.equals(artifact.getGroupId()) &&
                REGISTRY_ARTIFACT_ID.equals(artifact.getArtifactId()) &&
                VERSION.equals(artifact.getVersion());
    }
}
