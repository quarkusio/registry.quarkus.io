package io.quarkus.registry.app.model.mapper;

import io.quarkus.maven.ArtifactCoords;
import io.quarkus.registry.app.model.Platform;
import io.quarkus.registry.app.model.PlatformRelease;
import io.quarkus.registry.app.model.PlatformStream;
import io.quarkus.registry.catalog.json.JsonPlatform;
import io.quarkus.registry.catalog.json.JsonPlatformRelease;
import io.quarkus.registry.catalog.json.JsonPlatformReleaseVersion;
import io.quarkus.registry.catalog.json.JsonPlatformStream;
import org.mapstruct.Mapper;

/**
 * Mapper to convert from JPA entities to the JSON representation
 */
@Mapper(componentModel = "cdi")
public interface PlatformMapper {

    JsonPlatform toJsonPlatform(Platform person);

    JsonPlatformStream toJsonPlatformStream(PlatformStream value);

    JsonPlatformRelease toJsonPlatformRelease(PlatformRelease value);

    default JsonPlatformReleaseVersion toJsonPlatformReleaseVersion(String version) {
        return JsonPlatformReleaseVersion.fromString(version);
    }

    default ArtifactCoords toArtifactCoords(String value) {
        return ArtifactCoords.fromString(value);
    }
}
