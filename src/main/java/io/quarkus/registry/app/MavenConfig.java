package io.quarkus.registry.app;

import io.quarkus.maven.ArtifactCoords;

interface MavenConfig {

    String GROUP_ID = "io.quarkus.registry";

    String PLATFORM_ARTIFACT_ID = "quarkus-platforms";

    String NON_PLATFORM_EXTENSIONS_ARTIFACT_ID = "quarkus-non-platform-extensions";

    String REGISTRY_ARTIFACT_ID = "quarkus-registry-descriptor";

    String VERSION = "1.0-SNAPSHOT";

    ArtifactCoords PLATFORM_COORDS = new ArtifactCoords(GROUP_ID, PLATFORM_ARTIFACT_ID, "json", VERSION);
    ArtifactCoords NON_PLATFORM_EXTENSION_COORDS = new ArtifactCoords(GROUP_ID, NON_PLATFORM_EXTENSIONS_ARTIFACT_ID, "json", VERSION);

}
