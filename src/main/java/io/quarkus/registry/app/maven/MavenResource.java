package io.quarkus.registry.app.maven;

import java.io.IOException;
import java.io.StringWriter;
import java.util.List;

import javax.inject.Inject;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.PathSegment;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;

import io.quarkus.cache.CacheResult;
import io.quarkus.maven.ArtifactCoords;
import io.quarkus.registry.app.CacheNames;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;
import org.jboss.logging.Logger;
import org.sonatype.nexus.repository.metadata.model.RepositoryMetadata;
import org.sonatype.nexus.repository.metadata.model.io.xpp3.RepositoryMetadataXpp3Writer;

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
    MavenConfig mavenConfig;

    private ArtifactContentProvider[] getContentProviders() {
        return new ArtifactContentProvider[] {
                pomContentProvider,
                metadataContentProvider,
                registryDescriptorContentProvider,
                platformsContentProvider,
                nonPlatformExtensionsContentProvider
        };
    }

    @GET
    @Path("/.meta/repository-metadata.{extension}")
    @CacheResult(cacheName = CacheNames.METADATA)
    public Response handleRepositoryMetadataRequest(@PathParam("extension") String extension) throws IOException {
        RepositoryMetadata repositoryMetadata = new RepositoryMetadata();
        repositoryMetadata.setVersion(RepositoryMetadata.MODEL_VERSION);
        repositoryMetadata.setId(mavenConfig.getRegistryId());
        repositoryMetadata.setUrl(mavenConfig.getRegistryUrl());
        repositoryMetadata.setLayout(RepositoryMetadata.LAYOUT_MAVEN2);
        repositoryMetadata.setPolicy(RepositoryMetadata.POLICY_SNAPSHOT);
        String contentType = MediaType.APPLICATION_XML;
        String content = writeMetadata(repositoryMetadata);
        if (extension.endsWith(".sha1")) {
            content = HashUtil.sha1(content);
            contentType = MediaType.TEXT_PLAIN;
        }
        return Response.ok(content).header(HttpHeaders.CONTENT_TYPE, contentType).build();
    }

    @GET
    @Path("{path:.+}")
    public Response handleArtifactRequest(
            @PathParam("path") List<PathSegment> pathSegments,
            @Context UriInfo uriInfo) {
        ArtifactCoords artifactCoords;
        try {
            artifactCoords = ArtifactParser.parseCoords(pathSegments);
        } catch (IllegalArgumentException iae) {
            log.debug("Error while parsing coords: " + iae.getMessage());
            return Response.status(Response.Status.NOT_FOUND).build();
        }
        for (ArtifactContentProvider contentProvider : getContentProviders()) {
            if (contentProvider.supports(artifactCoords, uriInfo)) {
                try {
                    return contentProvider.provide(artifactCoords, uriInfo);
                } catch (WebApplicationException wae) {
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

    private String writeMetadata(RepositoryMetadata metadata) throws IOException {
        StringWriter sw = new StringWriter();
        new RepositoryMetadataXpp3Writer().write(sw, metadata);
        return sw.toString();
    }
}
