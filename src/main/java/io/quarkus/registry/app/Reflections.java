package io.quarkus.registry.app;

import io.quarkus.registry.catalog.CategoryImpl;
import io.quarkus.registry.catalog.ExtensionCatalogImpl;
import io.quarkus.registry.catalog.ExtensionImpl;
import io.quarkus.registry.catalog.ExtensionOriginImpl;
import io.quarkus.registry.catalog.PlatformCatalogImpl;
import io.quarkus.registry.catalog.PlatformImpl;
import io.quarkus.registry.catalog.PlatformReleaseImpl;
import io.quarkus.registry.catalog.PlatformReleaseVersion;
import io.quarkus.registry.catalog.PlatformStreamImpl;
import io.quarkus.registry.config.RegistriesConfigImpl;
import io.quarkus.registry.config.RegistryArtifactConfigImpl;
import io.quarkus.registry.config.RegistryConfigImpl;
import io.quarkus.registry.config.RegistryDescriptorConfigImpl;
import io.quarkus.registry.config.RegistryMavenConfigImpl;
import io.quarkus.registry.config.RegistryMavenRepoConfigImpl;
import io.quarkus.registry.config.RegistryNonPlatformExtensionsConfigImpl;
import io.quarkus.registry.config.RegistryPlatformsConfigImpl;
import io.quarkus.registry.config.RegistryQuarkusVersionsConfigImpl;
import io.quarkus.registry.json.JsonArtifactCoordsDeserializer;
import io.quarkus.registry.json.JsonArtifactCoordsMixin;
import io.quarkus.registry.json.JsonArtifactCoordsSerializer;
import io.quarkus.registry.json.JsonBooleanTrueFilter;
import io.quarkus.runtime.annotations.RegisterForReflection;

@RegisterForReflection(targets = {
        JsonArtifactCoordsDeserializer.class,
        JsonArtifactCoordsMixin.class,
        JsonArtifactCoordsSerializer.class,
        JsonBooleanTrueFilter.class,
        CategoryImpl.class,
        ExtensionImpl.class,
        ExtensionCatalogImpl.class,
        ExtensionOriginImpl.class,
        PlatformImpl.class,
        PlatformCatalogImpl.class,
        PlatformReleaseImpl.class,
        PlatformReleaseVersion.VersionImpl.class,
        PlatformReleaseVersion.Deserializer.class,
        PlatformReleaseVersion.Serializer.class,
        PlatformStreamImpl.class,

        RegistriesConfigImpl.class,
        RegistryArtifactConfigImpl.class,
        RegistryConfigImpl.class,

        RegistryDescriptorConfigImpl.class,
        RegistryMavenConfigImpl.class,
        RegistryMavenRepoConfigImpl.class,
        RegistryNonPlatformExtensionsConfigImpl.class,
        RegistryPlatformsConfigImpl.class,
        RegistryQuarkusVersionsConfigImpl.class
}, classNames = "com.github.benmanes.caffeine.cache.SSSMS")
public class Reflections {
}
