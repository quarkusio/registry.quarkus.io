package io.quarkus.registry.app;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import javax.enterprise.context.ApplicationScoped;
import javax.validation.constraints.NotNull;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.QueryParam;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response;

import io.quarkus.maven.ArtifactCoords;
import io.quarkus.registry.app.maven.MavenConfig;
import io.quarkus.registry.app.model.Category;
import io.quarkus.registry.app.model.ExtensionRelease;
import io.quarkus.registry.app.model.PlatformExtension;
import io.quarkus.registry.app.model.PlatformRelease;
import io.quarkus.registry.app.model.PlatformReleaseCategory;
import io.quarkus.registry.catalog.ExtensionCatalog;
import io.quarkus.registry.catalog.ExtensionOrigin;
import io.quarkus.registry.catalog.PlatformCatalog;
import io.quarkus.registry.catalog.json.JsonCategory;
import io.quarkus.registry.catalog.json.JsonExtension;
import io.quarkus.registry.catalog.json.JsonExtensionCatalog;
import io.quarkus.registry.catalog.json.JsonPlatform;
import io.quarkus.registry.catalog.json.JsonPlatformCatalog;
import io.quarkus.registry.client.RegistryNonPlatformExtensionsResolver;
import io.quarkus.registry.client.RegistryPlatformExtensionsResolver;
import io.quarkus.registry.client.RegistryPlatformsResolver;

/**
 * This class will query the database for the requested information
 */
@ApplicationScoped
@Path("/client")
public class DatabaseRegistryClient implements RegistryNonPlatformExtensionsResolver,
        RegistryPlatformExtensionsResolver, RegistryPlatformsResolver {

    @GET
    @Path("platforms")
    @Override
    public PlatformCatalog resolvePlatforms(@QueryParam("v") String quarkusVersion) {
        JsonPlatformCatalog catalog = new JsonPlatformCatalog();
        List<PlatformRelease> platformReleases;
        if (quarkusVersion == null || quarkusVersion.isEmpty()) {
            platformReleases = PlatformRelease.findLatest();
        } else {
            platformReleases = PlatformRelease.findByQuarkusCore(quarkusVersion);
        }
        for (PlatformRelease platformRelease : platformReleases) {
            JsonPlatform platform = new JsonPlatform();
            ArtifactCoords bom = ArtifactCoords.pom(
                    platformRelease.platform.groupId,
                    platformRelease.platform.artifactId,
                    platformRelease.version);
            platform.setBom(bom);
            platform.setQuarkusCoreVersion(platformRelease.quarkusCore);
            platform.setUpstreamQuarkusCoreVersion(platformRelease.quarkusCoreUpstream);
            catalog.addPlatform(platform);
            if (platformRelease.platform.isDefault) {
                catalog.setDefaultPlatform(bom);
            }
        }
        return catalog;
    }

    @GET
    @Path("extensions")
    @Override
    public ExtensionCatalog resolvePlatformExtensions(@QueryParam("c") ArtifactCoords platformCoords) {
        if (platformCoords == null) {
            throw new WebApplicationException(Response.Status.BAD_REQUEST);
        }
        JsonExtensionCatalog catalog = new JsonExtensionCatalog();
        PlatformRelease platformRelease = PlatformRelease
                .findByGAV(platformCoords.getGroupId(), platformCoords.getArtifactId(), platformCoords.getVersion())
                .orElseThrow(() -> new WebApplicationException(Response.Status.NOT_FOUND));

        String id = new ArtifactCoords(MavenConfig.PLATFORM_COORDS.getGroupId(),
                                       MavenConfig.PLATFORM_COORDS.getArtifactId(),
                                       platformCoords.getVersion(),
                                       MavenConfig.PLATFORM_COORDS.getType(),
                                       MavenConfig.PLATFORM_COORDS.getVersion()).toString();

        catalog.setId(id);
        catalog.setBom(platformCoords);
        catalog.setPlatform(true);
        catalog.setQuarkusCoreVersion(platformRelease.quarkusCore);
        catalog.setUpstreamQuarkusCoreVersion(platformRelease.quarkusCoreUpstream);
        catalog.setMetadata(platformRelease.metadata);
        // Add extensions
        for (PlatformExtension platformExtension : platformRelease.extensions) {
            JsonExtension jsonExtension = toJsonExtension(platformExtension, catalog);
            catalog.addExtension(jsonExtension);
        }
        // Add categories
        List<io.quarkus.registry.catalog.Category> categories = new ArrayList<>();
        if (platformRelease.categories.isEmpty()) {
            categories = Category.listAll().stream()
                    .map(Category.class::cast)
                    .map(this::toJsonCategory)
                    .collect(Collectors.toList());
        } else {
            for (PlatformReleaseCategory cat : platformRelease.categories) {
                JsonCategory jsonCategory = new JsonCategory();
                jsonCategory.setId(cat.getName());
                jsonCategory.setName(cat.getName());
                jsonCategory.setDescription(cat.getDescription());
            }
        }
        catalog.setCategories(categories);
        return catalog;
    }

    @Override
    @GET
    @Path("non-platform-extensions")
    public ExtensionCatalog resolveNonPlatformExtensions(@QueryParam("v") String quarkusVersion) {
        if (quarkusVersion == null) {
            throw new WebApplicationException(Response.Status.BAD_REQUEST);
        }
        String id = new ArtifactCoords(MavenConfig.NON_PLATFORM_EXTENSION_COORDS.getGroupId(),
                                       MavenConfig.NON_PLATFORM_EXTENSION_COORDS.getArtifactId(),
                                       quarkusVersion,
                                       MavenConfig.NON_PLATFORM_EXTENSION_COORDS.getType(),
                                       MavenConfig.NON_PLATFORM_EXTENSION_COORDS.getVersion()).toString();

        final JsonExtensionCatalog catalog = new JsonExtensionCatalog();
        catalog.setId(id);
        catalog.setBom(ArtifactCoords.pom("io.quarkus","quarkus-bom", quarkusVersion));
        List<ExtensionRelease> nonPlatformExtensions = ExtensionRelease.findNonPlatformExtensions(quarkusVersion);

        for (ExtensionRelease extensionRelease : nonPlatformExtensions) {
            catalog.addExtension(toJsonExtension(extensionRelease, catalog));
        }
        // Add all categories
        List<Category> categories = Category.listAll();
        categories.stream().map(this::toJsonCategory).forEach(catalog::addCategory);
        return catalog;
    }

    private JsonCategory toJsonCategory(Category category) {
        JsonCategory jsonCategory = new JsonCategory();
        jsonCategory.setId(category.name);
        jsonCategory.setName(category.name);
        jsonCategory.setDescription(category.description);
        return jsonCategory;
    }

    private JsonExtension toJsonExtension(PlatformExtension platformExtension, ExtensionOrigin extensionOrigin) {
        JsonExtension e = toJsonExtension(platformExtension.extensionRelease, extensionOrigin);
        e.setMetadata(platformExtension.metadata);
        return e;
    }

    private JsonExtension toJsonExtension(ExtensionRelease extensionRelease, ExtensionOrigin extensionOrigin) {
        JsonExtension e = new JsonExtension();
        e.setGroupId(extensionRelease.extension.groupId);
        e.setArtifactId(extensionRelease.extension.artifactId);
        e.setVersion(extensionRelease.version);
        e.setName(extensionRelease.extension.name);
        e.setDescription(extensionRelease.extension.description);
        e.setOrigins(Collections.singletonList(extensionOrigin));
        e.setMetadata(extensionRelease.metadata);
        return e;
    }

}
