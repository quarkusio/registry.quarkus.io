package io.quarkus.registry.app;

import javax.enterprise.context.ApplicationScoped;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.QueryParam;

import io.quarkus.maven.ArtifactCoords;
import io.quarkus.registry.app.model.ExtensionRelease;
import io.quarkus.registry.catalog.ExtensionCatalog;
import io.quarkus.registry.catalog.PlatformCatalog;
import io.quarkus.registry.catalog.json.JsonExtension;
import io.quarkus.registry.catalog.json.JsonExtensionCatalog;
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
    public PlatformCatalog resolvePlatforms(@QueryParam("version") String quarkusVersion) {
        JsonPlatformCatalog catalog = new JsonPlatformCatalog();
        return catalog;
    }

    @GET
    @Path("extensions")
    @Override
    public ExtensionCatalog resolvePlatformExtensions(ArtifactCoords platformCoords) {
        JsonExtensionCatalog catalog = new JsonExtensionCatalog();
        return catalog;
    }

    @Override
    @GET
    @Path("non-platform-extensions")
    public ExtensionCatalog resolveNonPlatformExtensions(String quarkusVersion) {
        final JsonExtensionCatalog catalog = new JsonExtensionCatalog();
        for (ExtensionRelease extensionRelease : ExtensionRelease.findNonPlatformExtensions(quarkusVersion)) {
            JsonExtension e = new JsonExtension();
            e.setGroupId(extensionRelease.extension.groupId);
            e.setArtifactId(extensionRelease.extension.artifactId);
            e.setVersion(extensionRelease.version);
            e.setName(extensionRelease.extension.name);
            e.setDescription(extensionRelease.extension.description);
            e.setMetadata(extensionRelease.metadata);
            catalog.addExtension(e);
        }
        return catalog;
    }

}
