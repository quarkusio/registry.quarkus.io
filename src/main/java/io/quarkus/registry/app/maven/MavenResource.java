package io.quarkus.registry.app.maven;

import java.util.List;
import java.util.stream.Collectors;

import javax.inject.Inject;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.PathSegment;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;

import org.apache.maven.artifact.Artifact;
import org.jboss.logging.Logger;

/**
 * Exposes a Maven resource for our tooling
 */
@Path("/maven")
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

    private ArtifactContentProvider[] getContentProviders() {
        return new ArtifactContentProvider[]{
                pomContentProvider,
                metadataContentProvider,
                registryDescriptorContentProvider,
                platformsContentProvider,
                nonPlatformExtensionsContentProvider
        };
    }

    @GET
    @Path("{path:.+}")
    public Response handleArtifactRequest(
            @PathParam("path") List<PathSegment> pathSegments,
            @Context UriInfo uriInfo) {
        Artifact artifact = ArtifactParser.parseArtifact(pathSegments.stream().map(PathSegment::getPath).collect(Collectors.toList()));
        for (ArtifactContentProvider contentProvider : getContentProviders()) {
            if (contentProvider.supports(artifact, uriInfo)) {
                try {
                    return contentProvider.provide(artifact, uriInfo);
                } catch (WebApplicationException wae) {
                    throw wae;
                } catch (Exception e) {
                    e.printStackTrace();
                    return Response.serverError().build();
                }
            }
        }
        log.infof("Not found: {}", uriInfo.getAbsolutePath());
        return Response.status(Response.Status.NOT_FOUND).build();
    }
}
