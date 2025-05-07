package io.quarkus.registry.app;

import java.io.IOException;
import java.io.StringWriter;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;

import com.fasterxml.jackson.jakarta.rs.yaml.YAMLMediaTypes;

import io.quarkus.maven.dependency.ArtifactCoords;
import io.quarkus.registry.Constants;
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
import io.quarkus.registry.config.RegistriesConfig;
import io.quarkus.registry.config.RegistriesConfigMapperHelper;
import io.quarkus.registry.config.RegistryConfig;
import io.quarkus.registry.config.RegistryDescriptorConfig;
import io.quarkus.registry.config.RegistryMavenConfig;
import io.quarkus.registry.config.RegistryMavenRepoConfig;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.validation.constraints.NotNull;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

/**
 * This class will query the database for the requested information
 */
@ApplicationScoped
@Path("/client")
@Tag(name = "Client", description = "Client related services")
public class DatabaseRegistryClient {

    private static final String IO_QUARKUS_PLATFORM = "io.quarkus.platform";
    private static final String QUARKUS_BOM = "quarkus-bom";

    @Inject
    MavenConfig mavenConfig;

    @GET
    @Path("/platforms/all")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = "List all platform releases from the database")
    public PlatformCatalog resolveAllPlatforms() {
        List<PlatformRelease> platformReleases = PlatformRelease.findAllCorePlatforms();
        return toPlatformCatalog(platformReleases, true);
    }

    @GET
    @Path("/platforms")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = "List the latest stable + newer unstable + pinned platform releases")
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
    @Operation(summary = "List all latest non-platform extensions compatible with a given Quarkus version")
    public ExtensionCatalog resolveNonPlatformExtensionsCatalog(
            @NotNull(message = "The Quarkus version (v) is missing") @QueryParam("v") String quarkusVersion) {
        Version.validateVersion(quarkusVersion);
        String id = mavenConfig.getNonPlatformExtensionCoords(quarkusVersion);

        final ExtensionCatalog.Mutable catalog = ExtensionCatalog.builder();
        catalog.setId(id);
        catalog.setBom(ArtifactCoords.pom(IO_QUARKUS_PLATFORM, QUARKUS_BOM, quarkusVersion));
        catalog.setQuarkusCoreVersion(quarkusVersion);
        List<ExtensionRelease> nonPlatformExtensions = ExtensionRelease.findNonPlatformExtensions(quarkusVersion);
        Map<Long, Boolean> compatibleMap = ExtensionReleaseCompatibility.findCompatibleMap(quarkusVersion);
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
                compatibility = compatibleMap.get(extensionRelease.id);
            }
            extension.getMetadata().put("quarkus-core-compatibility", CoreCompatibility.parse(compatibility));
            catalog.addExtension(extension.build());
        }
        // Add all categories
        List<Category> categories = Category.listAll();
        categories.stream().map(this::toClientCategory).forEach(catalog::addCategory);
        return catalog.build();
    }

    @GET
    @Path("/extensions/all")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = "List all extensions. When an extension has multiple releases, only the most recent will be listed.")
    public ExtensionCatalog resolveAllExtensionsCatalog() {
        final ExtensionCatalog.Mutable catalog = ExtensionCatalog.builder();
        List<ExtensionRelease> allExtensionReleases = ExtensionRelease.findLatestExtensions();

        allExtensionReleases.stream().map(this::toClientExtension).forEach(catalog::addExtension);

        // Add all categories
        List<Category> categories = Category.listAll();
        categories.stream().map(this::toClientCategory).forEach(catalog::addCategory);
        return catalog.build();
    }

    @GET
    @Path("/config.yaml")
    @Produces(YAMLMediaTypes.APPLICATION_JACKSON_YAML)
    @Operation(summary = "Example Quarkus Registry Client configuration file")
    public Response clientConfigYaml() throws IOException {
        ArtifactCoords coords = ArtifactCoords
                .of(mavenConfig.getRegistryGroupId(),
                        Constants.DEFAULT_REGISTRY_DESCRIPTOR_ARTIFACT_ID,
                        null,
                        Constants.JSON,
                        Constants.DEFAULT_REGISTRY_ARTIFACT_VERSION);
        RegistryMavenRepoConfig mavenRepoConfig = RegistryMavenRepoConfig.builder().setUrl(mavenConfig.getRegistryUrl())
                .build();
        RegistryConfig registry = RegistryConfig.builder()
                .setId(mavenConfig.getRegistryId())
                .setUpdatePolicy("always")
                .setDescriptor(
                        RegistryDescriptorConfig.builder().setArtifact(coords))
                .setMaven(
                        RegistryMavenConfig.builder()
                                .setRepository(mavenRepoConfig)
                                .build())
                .build();
        RegistriesConfig config = RegistriesConfig.builder().setRegistry(registry).build();
        StringWriter writer = new StringWriter();
        RegistriesConfigMapperHelper.toYaml(config, writer);
        return Response.ok(writer.toString()).build();
    }

    @GET
    @Path("/dump-request")
    @Produces(MediaType.TEXT_PLAIN)
    @Operation(hidden = true)
    public Response dump(@Context jakarta.ws.rs.core.HttpHeaders headers) {
        // Dump Request Headers
        StringBuilder sb = new StringBuilder();
        sb.append("Request Headers:\n");
        for (String name : headers.getRequestHeaders().keySet()) {
            sb.append(name).append(": ").append(headers.getRequestHeader(name)).append("\n");
        }
        return Response.ok(sb.toString()).build();
    }

    private PlatformCatalog toPlatformCatalog(List<PlatformRelease> platformReleases, boolean all) {
        PlatformCatalog.Mutable catalog = PlatformCatalog.builder();
        platformReleases.sort((o1, o2) -> Version.RELEASE_IMPORTANCE_COMPARATOR.compare(o1.version, o2.version));
        for (PlatformRelease platformRelease : platformReleases) {
            PlatformStream platformStream = platformRelease.platformStream;
            Platform platform = platformStream.platform;

            io.quarkus.registry.catalog.Platform clientPlatform = catalog.getPlatform(platform.platformKey);
            io.quarkus.registry.catalog.Platform.Mutable mutableClientPlatform;

            if (clientPlatform == null) {
                mutableClientPlatform = toClientPlatform(platform);
                mutableClientPlatform.getMetadata().put("current-stream-id", platformStream.streamKey);
            } else {
                mutableClientPlatform = clientPlatform.mutable();
            }
            io.quarkus.registry.catalog.PlatformStream clientPlatformStream = mutableClientPlatform
                    .getStream(platformStream.streamKey);
            io.quarkus.registry.catalog.PlatformStream.Mutable mutableClientPlatformStream;
            if (clientPlatformStream == null) {
                mutableClientPlatformStream = toClientPlatformStream(platformStream);
            } else {
                mutableClientPlatformStream = clientPlatformStream.mutable();
            }
            mutableClientPlatformStream.setMetadata("lts", platformStream.lts);
            io.quarkus.registry.catalog.PlatformRelease.Mutable mutablePlatformRelease = toClientPlatformRelease(
                    platformRelease);
            if (all) {
                mutablePlatformRelease.setMetadata("unlisted", platformRelease.unlisted);
            }
            mutableClientPlatformStream.addRelease(mutablePlatformRelease.build());
            if (all) {
                mutableClientPlatformStream.setMetadata("unlisted", platformStream.unlisted);
            }
            catalog.addPlatform(mutableClientPlatform.addStream(mutableClientPlatformStream.build()).build());
        }
        return catalog.build();
    }

    private List<ExtensionOrigin> toExtensionOrigins(ExtensionRelease extensionRelease) {
        final List<ExtensionOrigin> extensionOrigins;

        if (extensionRelease.platforms.isEmpty()) {
            // Non-platform case
            String id = mavenConfig.getNonPlatformExtensionCoords(extensionRelease.quarkusCoreVersion);
            extensionOrigins = List.of(ExtensionCatalog.builder().setId(id)
                    .setQuarkusCoreVersion(extensionRelease.quarkusCoreVersion)
                    .setPlatform(false)
                    .build());
        } else {

            // Platform case
            extensionOrigins = extensionRelease.platforms.stream()
                    .map((platformExtension) -> (ExtensionOrigin) ExtensionCatalog.builder()
                            // it should be <platform-key>:<member-bom-artifactId>-quarkus-platform-descriptor:<quarkus-version>:json:<quarkus-version>
                            // We can get all member boms associated with this release, but not the particular one this extension came from
                            // For the moment, we assume the member bom is quarkus-core since we don't have information to make a better choise
                            .setId(ArtifactCoords
                                    .of(platformExtension.platformRelease.platformStream.platform.platformKey,
                                            QUARKUS_BOM + "-quarkus-platform-descriptor",
                                            platformExtension.platformRelease.version,
                                            Constants.JSON,
                                            platformExtension.platformRelease.version)
                                    .toGACTVString())
                            .setBom(ArtifactCoords.pom(platformExtension.platformRelease.platformStream.platform.platformKey,
                                    QUARKUS_BOM,
                                    platformExtension.platformRelease.version))
                            .setMetadata(platformExtension.platformRelease.metadata)
                            .setQuarkusCoreVersion(platformExtension.platformRelease.quarkusCoreVersion)
                            .setPlatform(true).build())
                    .toList();
        }

        return extensionOrigins;
    }

    private Extension.Mutable toClientExtension(ExtensionRelease extensionRelease, ExtensionOrigin extensionOrigin) {
        return toClientExtensionNoOrigin(extensionRelease)
                .setOrigins(Collections.singletonList(extensionOrigin));
    }

    private Extension.Mutable toClientExtension(ExtensionRelease extensionRelease) {

        List<ExtensionOrigin> extensionOrigins = toExtensionOrigins(extensionRelease);

        return toClientExtensionNoOrigin(extensionRelease)
                .setOrigins(extensionOrigins);
    }

    private Extension.Mutable toClientExtensionNoOrigin(ExtensionRelease extensionRelease) {
        Map<String, Object> mergedMetadata = extensionRelease.metadata != null ? new HashMap<>(extensionRelease.metadata)
                : new HashMap<>();
        extensionRelease.platforms
                .forEach(platformExtension -> {
                    if (platformExtension.metadata != null) {
                        // If we have a key collision in the platformExtension, just take the second value; it's unlikely and it's unclear what a "right" behaviour is
                        platformExtension.metadata.forEach(
                                (key, value) -> mergedMetadata.merge(key, value, (v1, v2) -> v2));
                    }
                });

        return Extension.builder()
                .setGroupId(extensionRelease.extension.groupId)
                .setArtifactId(extensionRelease.extension.artifactId)
                .setVersion(extensionRelease.version)
                .setName(extensionRelease.extension.name)
                .setDescription(extensionRelease.extension.description)
                .setMetadata(mergedMetadata);
    }

    private io.quarkus.registry.catalog.Category toClientCategory(Category category) {
        return io.quarkus.registry.catalog.Category.builder()
                .setId(category.categoryKey)
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

    private io.quarkus.registry.catalog.PlatformRelease.Mutable toClientPlatformRelease(PlatformRelease platformRelease) {
        return io.quarkus.registry.catalog.PlatformRelease.builder()
                .setMemberBoms(platformRelease.memberBoms.stream().map(ArtifactCoords::fromString).collect(
                        Collectors.toList()))
                .setVersion(PlatformReleaseVersion.fromString(platformRelease.version))
                .setUpstreamQuarkusCoreVersion(platformRelease.upstreamQuarkusCoreVersion)
                .setQuarkusCoreVersion(platformRelease.quarkusCoreVersion);
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
