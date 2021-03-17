package io.quarkus.registry.app;

import java.util.ArrayList;
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
import io.quarkus.registry.app.model.Category;
import io.quarkus.registry.app.model.ExtensionRelease;
import io.quarkus.registry.app.model.PlatformRelease;
import io.quarkus.registry.app.model.PlatformReleaseCategory;
import io.quarkus.registry.catalog.ExtensionCatalog;
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
        List<PlatformRelease> platformReleases = PlatformRelease.findByQuarkusCore(quarkusVersion);
        if (platformReleases.isEmpty()) {
            throw new WebApplicationException(Response.Status.NOT_FOUND);
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
            catalog.setDefaultPlatform(bom);
        }
        return catalog;
    }

    @GET
    @Path("extensions")
    @Override
    public ExtensionCatalog resolvePlatformExtensions(@QueryParam("c") ArtifactCoords platformCoords) {
        JsonExtensionCatalog catalog = new JsonExtensionCatalog();
        PlatformRelease platformRelease = PlatformRelease
                .findByGAV(platformCoords.getGroupId(), platformCoords.getArtifactId(), platformCoords.getVersion())
                .orElseThrow(() -> new WebApplicationException(Response.Status.NOT_FOUND));
        // Add extensions
        platformRelease.extensions.stream()
                .map(pe -> pe.extensionRelease)
                .forEach(extensionRelease -> addExtension(catalog, extensionRelease));
        // Add categories
        List<io.quarkus.registry.catalog.Category> categories = new ArrayList<>();
        for (PlatformReleaseCategory cat : platformRelease.categories) {
            JsonCategory jsonCategory = new JsonCategory();
            jsonCategory.setId(cat.getName());
            jsonCategory.setName(cat.getName());
            jsonCategory.setDescription(cat.getDescription());
        }
        catalog.setCategories(categories);
        return catalog;
    }

    @Override
    @GET
    @Path("non-platform-extensions")
    public ExtensionCatalog resolveNonPlatformExtensions(@NotNull @QueryParam("v") String quarkusVersion) {
        final JsonExtensionCatalog catalog = new JsonExtensionCatalog();
        List<ExtensionRelease> nonPlatformExtensions = ExtensionRelease.findNonPlatformExtensions(quarkusVersion);
        if (nonPlatformExtensions.isEmpty()) {
            throw new WebApplicationException(Response.Status.NOT_FOUND);
        }
        for (ExtensionRelease extensionRelease : nonPlatformExtensions) {
            addExtension(catalog, extensionRelease);
        }
        // Add all categories
        List<Category> categories = Category.listAll();
        catalog.setCategories(categories.stream().map(this::toJsonCategory).collect(Collectors.toList()));
        return catalog;
    }

    private JsonCategory toJsonCategory(Category category) {
        JsonCategory jsonCategory = new JsonCategory();
        jsonCategory.setId(category.name);
        jsonCategory.setName(category.name);
        jsonCategory.setDescription(category.description);
        return jsonCategory;
    }

    private void addExtension(JsonExtensionCatalog catalog, ExtensionRelease extensionRelease) {
        JsonExtension e = new JsonExtension();
        e.setGroupId(extensionRelease.extension.groupId);
        e.setArtifactId(extensionRelease.extension.artifactId);
        e.setVersion(extensionRelease.version);
        e.setName(extensionRelease.extension.name);
        e.setDescription(extensionRelease.extension.description);
        e.setMetadata(extensionRelease.metadata);
        catalog.addExtension(e);
    }

}
