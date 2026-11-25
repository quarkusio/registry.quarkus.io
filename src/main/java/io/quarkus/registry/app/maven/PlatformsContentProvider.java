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
import io.quarkus.registry.catalog.PlatformCatalog;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.UriInfo;

/**
 * Lists the available platforms and their recommended versions, indicating which platform is the recommended default for new
 * projects
 */
@Singleton
public class PlatformsContentProvider implements ArtifactContentProvider {

    @Inject
    MavenConfig mavenConfig;

    @Inject
    ObjectMapper objectMapper;

    @Inject
    DatabaseRegistryClient registryClient;

    @Override
    public boolean supports(ArtifactCoords artifact, UriInfo uriInfo) {
        return mavenConfig.matchesQuarkusPlatforms(artifact);
    }

    @Override
    public Response provide(ArtifactCoords artifact, UriInfo uriInfo) throws Exception {
        ArtifactContent catalog = generateCatalog(artifact.getClassifier());
        if (catalog == null) {
            return Response.status(Response.Status.NOT_FOUND)
                    .header("X-Reason", "No platforms found")
                    .build();
        }
        return toResponse(artifact, uriInfo, catalog, MediaType.APPLICATION_JSON);
    }

    /**
     * Builds the platform catalog for a Quarkus version, or returns {@code null} if there is none.
     * <p>
     * The result is cached so that the artifact and its {@code .sha1}/{@code .md5} siblings are built from a single
     * read of the database, and so that the burst of requests following a cache invalidation collapses onto one
     * computation instead of one per request. The cache is invalidated by {@link MavenCache#clear()}.
     */
    @CacheResult(cacheName = CacheNames.PLATFORMS)
    ArtifactContent generateCatalog(String quarkusVersion) throws IOException {
        final PlatformCatalog platformCatalog;
        if (quarkusVersion.equals("all")) {
            platformCatalog = registryClient.resolveAllPlatforms();
        } else {
            platformCatalog = registryClient.resolveCurrentPlatformsCatalog(quarkusVersion);
        }
        if (platformCatalog == null || platformCatalog.getPlatforms().isEmpty()) {
            return null;
        }
        StringWriter sw = new StringWriter();
        CatalogMapperHelper.serialize(objectMapper, platformCatalog, sw);
        return ArtifactContent.of(sw.toString());
    }
}
