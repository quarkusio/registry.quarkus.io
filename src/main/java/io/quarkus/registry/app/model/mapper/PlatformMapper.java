package io.quarkus.registry.app.model.mapper;

import java.util.Locale;

import io.quarkus.maven.ArtifactCoords;
import io.quarkus.registry.app.model.Category;
import io.quarkus.registry.app.model.Platform;
import io.quarkus.registry.app.model.PlatformRelease;
import io.quarkus.registry.app.model.PlatformStream;
import io.quarkus.registry.catalog.json.JsonCategory;
import io.quarkus.registry.catalog.json.JsonPlatform;
import io.quarkus.registry.catalog.json.JsonPlatformRelease;
import io.quarkus.registry.catalog.json.JsonPlatformReleaseVersion;
import io.quarkus.registry.catalog.json.JsonPlatformStream;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;

/**
 * Mapper to convert from JPA entities to the JSON representation
 */
@Mapper(componentModel = "cdi")
public interface PlatformMapper {

    @Mapping(target = "streams", ignore = true)
    JsonPlatform toJsonPlatform(Platform person);

    @Mapping(source = "streamKey", target = "id")
    @Mapping(target = "releases", ignore = true)
    JsonPlatformStream toJsonPlatformStream(PlatformStream value);

    JsonPlatformRelease toJsonPlatformRelease(PlatformRelease value);

    @Mapping(source = "name", target = "id" ,qualifiedByName = "toCategoryId" )
    JsonCategory toJsonCategory(Category category);

    @Named("toCategoryId")
    static String toCategoryId(String id) {
        return id.toLowerCase(Locale.ROOT).replace(' ', '-');
    }

    default JsonPlatformReleaseVersion toJsonPlatformReleaseVersion(String version) {
        return JsonPlatformReleaseVersion.fromString(version);
    }

    default ArtifactCoords toArtifactCoords(String value) {
        return ArtifactCoords.fromString(value);
    }

}
