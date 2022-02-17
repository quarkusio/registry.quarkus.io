package io.quarkus.registry.app;

import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.validation.constraints.NotNull;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;

import org.eclipse.microprofile.openapi.annotations.tags.Tag;

import io.quarkus.maven.ArtifactCoords;
import io.quarkus.registry.app.maven.MavenConfig;
import io.quarkus.registry.app.model.Category;
import io.quarkus.registry.app.model.ExtensionRelease;
import io.quarkus.registry.app.model.ExtensionReleaseCompatibility;
import io.quarkus.registry.app.model.Platform;
import io.quarkus.registry.app.model.PlatformRelease;
import io.quarkus.registry.app.model.PlatformStream;
import io.quarkus.registry.app.util.Version;
import io.quarkus.registry.catalog.Extension;
import io.quarkus.registry.catalog.ExtensionCatalog;
import io.quarkus.registry.catalog.ExtensionOrigin;
import io.quarkus.registry.catalog.PlatformCatalog;
import io.quarkus.registry.catalog.PlatformReleaseVersion;

/**
 * This class will query the database for the requested information
 */
@ApplicationScoped
@Path("/client")
@Tag(name = "Client", description = "Client related services")
public class DatabaseRegistryClient {

    @Inject
    MavenConfig mavenConfig;

    @GET
    @Path("/platforms/all")
    @Produces(MediaType.APPLICATION_JSON)
    public PlatformCatalog resolveAllPlatforms() {
        List<PlatformRelease> platformReleases = PlatformRelease.findAll().list();
        return toPlatformCatalog(platformReleases, true);
    }

    @GET
    @Path("/platforms")
    @Produces(MediaType.APPLICATION_JSON)
    public PlatformCatalog resolveCurrentPlatformsCatalog(@QueryParam("v") String version) {
        if (version != null && !version.isBlank()) {
            Version.validateVersion(version);
        }
        List<PlatformRelease> platformReleases = PlatformRelease.findLatest(version);
        if (platformReleases.isEmpty()) {
            return null;
        }
        return toPlatformCatalog(platformReleases, false);
    }

    @GET
    @Path("/non-platform-extensions")
    @Produces(MediaType.APPLICATION_JSON)
    public ExtensionCatalog resolveNonPlatformExtensionsCatalog(
            @NotNull(message = "quarkusVersion is missing") @QueryParam("v") String quarkusVersion) {
        Version.validateVersion(quarkusVersion);
        ArtifactCoords nonPlatformExtensionCoords = mavenConfig.getNonPlatformExtensionCoords();
        String id = new ArtifactCoords(nonPlatformExtensionCoords.getGroupId(),
                nonPlatformExtensionCoords.getArtifactId(),
                quarkusVersion,
                nonPlatformExtensionCoords.getType(),
                nonPlatformExtensionCoords.getVersion()).toString();

        final ExtensionCatalog.Mutable catalog = ExtensionCatalog.builder();
        catalog.setId(id);
        catalog.setBom(ArtifactCoords.pom("io.quarkus.platform", "quarkus-bom", quarkusVersion));
        catalog.setQuarkusCoreVersion(quarkusVersion);
        List<ExtensionRelease> nonPlatformExtensions = ExtensionRelease.findNonPlatformExtensions(quarkusVersion);
        Map<Long, Boolean> compatiblityMap = ExtensionReleaseCompatibility.findCompatibleMap(quarkusVersion);
        for (ExtensionRelease extensionRelease : nonPlatformExtensions) {
            Extension.Mutable extension = toClientExtension(extensionRelease, catalog);
            // Add compatibility info
            Boolean compatibility;

            String extensionQuarkusCore = (String) extension.getMetadata().get(Extension.MD_BUILT_WITH_QUARKUS_CORE);
            // Some extensions were published using the full GAV
            if (extensionQuarkusCore != null && extensionQuarkusCore.contains(":")) {
                try {
                    extensionQuarkusCore = ArtifactCoords.fromString(extensionQuarkusCore).getVersion();
                } catch (IllegalArgumentException iae) {
                    // ignore
                }
            }

            // If the requested quarkus version matches the quarkus core built, just assume it's compatible
            if (quarkusVersion.equals(extensionQuarkusCore)) {
                compatibility = Boolean.TRUE;
            } else {
                compatibility = compatiblityMap.get(extensionRelease.id);
            }
            extension.getMetadata().put("quarkus-core-compatibility", CoreCompatibility.parse(compatibility));
            catalog.addExtension(extension.build());
        }
        // Add all categories
        List<Category> categories = Category.listAll();
        categories.stream().map(this::toClientCategory).forEach(catalog::addCategory);
        return catalog.build();
    }

    private PlatformCatalog toPlatformCatalog(List<PlatformRelease> platformReleases, boolean all) {
        PlatformCatalog.Mutable catalog = PlatformCatalog.builder();
        platformReleases.sort((o1, o2) -> Version.QUALIFIER_REVERSED_COMPARATOR.compare(o1.version, o2.version));
        for (PlatformRelease platformRelease : platformReleases) {
            PlatformStream platformStream = platformRelease.platformStream;
            Platform platform = platformStream.platform;

            io.quarkus.registry.catalog.PlatformStream.Mutable clientPlatformStream = toClientPlatformStream(platformStream);
            clientPlatformStream.addRelease(toClientPlatformRelease(platformRelease));

            io.quarkus.registry.catalog.Platform clientPlatform = catalog.getPlatform(platform.platformKey);
            io.quarkus.registry.catalog.PlatformStream stream = clientPlatformStream.build();
            io.quarkus.registry.catalog.Platform.Mutable thisClientPlatform;
            if (clientPlatform == null) {
                thisClientPlatform = toClientPlatform(platform);
                Map<String, Object> platformMetadata = thisClientPlatform.getMetadata();
                platformMetadata.put("current-stream-id", stream.getId());
                if (all) {
                    platformMetadata.put("unlisted", platformStream.unlisted);
                }
            } else {
                thisClientPlatform = clientPlatform.mutable();
            }
            catalog.addPlatform(thisClientPlatform.addStream(stream).build());
        }
        return catalog.build();
    }

    private Extension.Mutable toClientExtension(ExtensionRelease extensionRelease, ExtensionOrigin extensionOrigin) {
        return Extension.builder()
                .setGroupId(extensionRelease.extension.groupId)
                .setArtifactId(extensionRelease.extension.artifactId)
                .setVersion(extensionRelease.version)
                .setName(extensionRelease.extension.name)
                .setDescription(extensionRelease.extension.description)
                .setOrigins(Collections.singletonList(extensionOrigin))
                .setMetadata(extensionRelease.metadata);
    }

    private io.quarkus.registry.catalog.Category toClientCategory(Category category) {
        return io.quarkus.registry.catalog.Category.builder()
                .setId(category.name.toLowerCase(Locale.ROOT).replace(' ', '-'))
                .setName(category.name)
                .setDescription(category.description)
                .setMetadata(category.metadata)
                .build();
    }

    private io.quarkus.registry.catalog.Platform.Mutable toClientPlatform(Platform platform) {
        return io.quarkus.registry.catalog.Platform.builder()
                .setPlatformKey(platform.platformKey)
                .setName(platform.name)
                .setMetadata(platform.metadata);
    }

    private io.quarkus.registry.catalog.PlatformStream.Mutable toClientPlatformStream(PlatformStream platformStream) {
        return io.quarkus.registry.catalog.PlatformStream.builder()
                .setId(platformStream.streamKey)
                .setName(platformStream.name)
                .setMetadata(platformStream.metadata);
    }

    private io.quarkus.registry.catalog.PlatformRelease toClientPlatformRelease(PlatformRelease platformRelease) {
        return io.quarkus.registry.catalog.PlatformRelease.builder()
                .setMemberBoms(platformRelease.memberBoms.stream().map(ArtifactCoords::fromString).collect(
                        Collectors.toList()))
                .setVersion(PlatformReleaseVersion.fromString(platformRelease.version))
                .setMetadata(platformRelease.metadata)
                .setUpstreamQuarkusCoreVersion(platformRelease.upstreamQuarkusCoreVersion)
                .setQuarkusCoreVersion(platformRelease.quarkusCoreVersion)
                .build();
    }

    private enum CoreCompatibility {
        UNKNOWN,
        COMPATIBLE,
        INCOMPATIBLE;

        public static CoreCompatibility parse(Boolean compatibility) {
            if (compatibility == null) {
                return UNKNOWN;
            } else {
                return compatibility ? COMPATIBLE : INCOMPATIBLE;
            }
        }
    }
}
