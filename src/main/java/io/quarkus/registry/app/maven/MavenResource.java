package io.quarkus.registry.app.maven;

import java.util.List;

import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;
import org.jboss.logging.Logger;

import io.quarkus.maven.dependency.ArtifactCoords;
import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.PathSegment;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.UriInfo;

/**
 * Exposes a Maven resource for our tooling
 */
@Path("/maven")
@Tag(name = "Client", description = "Client related services")
public class MavenResource {

    private static final Logger log = Logger.getLogger(MavenResource.class);

    @Inject
    PomContentProvider pomContentProvider;

    @Inject
    MetadataContentProvider metadataContentProvider;

    @Inject
    RegistryDescriptorContentProvider registryDescriptorContentProvider;

    @Inject
    PlatformsContentProvider platformsContentProvider;

    @Inject
    NonPlatformExtensionsContentProvider nonPlatformExtensionsContentProvider;

    @Inject
    PlatformCatalogContentProvider platformCatalogContentProvider;

    private ArtifactContentProvider[] getContentProviders() {
        return new ArtifactContentProvider[] {
                pomContentProvider,
                metadataContentProvider,
                registryDescriptorContentProvider,
                platformsContentProvider,
                nonPlatformExtensionsContentProvider,
                platformCatalogContentProvider
        };
    }

    @GET
    @Path("{path:.+}")
    @Operation(hidden = true)
    public Response handleArtifactRequest(
            @PathParam("path") List<PathSegment> pathSegments,
            @Context UriInfo uriInfo) {
        ArtifactCoords artifactCoords;
        try {
            artifactCoords = ArtifactParser.parseCoords(pathSegments);
        } catch (IllegalArgumentException | StringIndexOutOfBoundsException e) {
            log.debug("Error while parsing coords from " + uriInfo.getPath(), e);
            return Response.status(Response.Status.NOT_FOUND).build();
        }
        for (ArtifactContentProvider contentProvider : getContentProviders()) {
            if (contentProvider.supports(artifactCoords, uriInfo)) {
                try {
                    return contentProvider.provide(artifactCoords, uriInfo);
                } catch (IllegalArgumentException | IllegalStateException | WebApplicationException wae) {
                    // These errors will be handled by the Exception mappers
                    throw wae;
                } catch (Exception e) {
                    log.error("Error while providing content", e);
                    return Response.serverError().build();
                }
            }
        }
        log.debugf("Not found: %s", uriInfo.getPath());
        return Response.status(Response.Status.NOT_FOUND).build();
    }
}
