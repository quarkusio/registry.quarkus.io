package io.quarkus.registry.app.maven;

import java.io.IOException;
import java.io.StringWriter;

import com.fasterxml.jackson.databind.ObjectMapper;

import io.quarkus.cache.CacheResult;
import io.quarkus.maven.dependency.ArtifactCoords;
import io.quarkus.registry.app.CacheNames;
import io.quarkus.registry.app.DatabaseRegistryClient;
import io.quarkus.registry.app.maven.cache.MavenCache;
import io.quarkus.registry.catalog.CatalogMapperHelper;
import io.quarkus.registry.catalog.ExtensionCatalog;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.UriInfo;

@Singleton
public class NonPlatformExtensionsContentProvider implements ArtifactContentProvider {

    @Inject
    MavenConfig mavenConfig;

    @Inject
    ObjectMapper objectMapper;

    @Inject
    DatabaseRegistryClient registryClient;

    @Override
    public boolean supports(ArtifactCoords artifact, UriInfo uriInfo) {
        return mavenConfig.matchesNonPlatformExtensions(artifact);
    }

    @Override
    public Response provide(ArtifactCoords artifact, UriInfo uriInfo) throws Exception {
        return toResponse(artifact, uriInfo, generateCatalog(artifact.getClassifier()), MediaType.APPLICATION_JSON);
    }

    /**
     * Builds the non-platform extension catalog for a Quarkus version.
     * <p>
     * The result is cached so that the artifact and its {@code .sha1}/{@code .md5} siblings are built from a single
     * read of the database, and so that the burst of requests following a cache invalidation collapses onto one
     * computation instead of one per request. The cache is invalidated by {@link MavenCache#clear()}.
     */
    @CacheResult(cacheName = CacheNames.NON_PLATFORM_EXTENSIONS)
    String generateCatalog(String quarkusVersion) throws IOException {
        ExtensionCatalog catalog = registryClient.resolveNonPlatformExtensionsCatalog(quarkusVersion);
        StringWriter sw = new StringWriter();
        CatalogMapperHelper.serialize(objectMapper, catalog, sw);
        return sw.toString();
    }
}
