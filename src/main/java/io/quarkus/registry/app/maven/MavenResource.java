package io.quarkus.registry.app.maven;

import java.util.List;

import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;
import org.jboss.logging.Logger;

import io.quarkus.maven.dependency.ArtifactCoords;
import io.quarkus.registry.app.model.PlatformRelease;
import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
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
    @Path("/")
    @Produces("text/html")
    @Operation(hidden = true)
    public Response welcomePage(@Context MavenConfig mavenConfig) {
        return Response.ok(String.format("""
                        <!DOCTYPE html>
                        <head>
                            <title>Quarkus Registry Maven Repository</title>
                        </head>
                        <h1>Welcome to the Quarkus Registry Maven Repository</h1>
                        This endpoint provides <a href="https://quarkus.io/guides/platform">Quarkus platform</a> and <a href="https://quarkus.io/guides/extension-metadata">extension metadata</a> in the form of Maven artifacts for Quarkus Dev Tools clients, such as <a href="https://code.quarkus.io/">code.quarkus.io</a> and <a href="https://quarkus.io/guides/cli-tooling">Quarkus CLI</a>.
                        It provides the following artifacts:
                        <ul>
                            <li><a href="%1$s/%2$s/quarkus-platforms/1.0-SNAPSHOT/quarkus-platforms-1.0-SNAPSHOT.json">Platforms</a></li>
                            <li><a href="%1$s/%2$s/quarkus-non-platform-extensions/1.0-SNAPSHOT/quarkus-non-platform-extensions-1.0-SNAPSHOT-%3$s.json">Non-platform extensions</a></li>
                            <li><a href="%1$s/%2$s/quarkus-registry-descriptor/1.0-SNAPSHOT/quarkus-registry-descriptor-1.0-SNAPSHOT.json">Registry Descriptor</a></li>
                        </ul>

                        If you have a Nexus Repository Manager and are looking on how to configure it to use this repository, please refer to the <a href="https://quarkus.io/guides/extension-registry-user#how-to-register-as-a-nexus-repository-proxy">How to register as a Nexus Repository proxy guide</a>.
                        """,mavenConfig.getRegistryUrl(),
                            mavenConfig.getRegistryGroupId().replace(".", "/"),
                            PlatformRelease.findLatestQuarkusCore()
                        )
                ).build();
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
