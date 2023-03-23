package io.quarkus.registry.app.maven;

import java.io.IOException;
import java.io.StringWriter;

import org.eclipse.microprofile.openapi.annotations.Operation;
import org.sonatype.nexus.repository.metadata.model.RepositoryMetadata;
import org.sonatype.nexus.repository.metadata.model.io.xpp3.RepositoryMetadataXpp3Writer;

import io.quarkus.cache.CacheResult;
import io.quarkus.registry.app.CacheNames;
import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

/**
 * Exposes Maven resource metadata for registering as a Nexus repository proxy
 */
@Path("/maven/.meta")
public class RepositoryMetadataResource {

    @Inject
    MavenConfig mavenConfig;

    @GET
    @Path("prefixes.txt")
    @Produces(MediaType.TEXT_PLAIN)
    @Operation(hidden = true)
    public String handlePrefixesTxt() {
        return "## repository-prefixes/2.0" + System.lineSeparator()
                + "/" + mavenConfig.getRegistryGroupId().replace('.', '/');
    }

    @GET
    @Path("repository-metadata.{extension}")
    @Operation(hidden = true)
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
        if (extension.endsWith(ArtifactParser.SUFFIX_SHA1)) {
            content = HashUtil.sha1(content);
            contentType = MediaType.TEXT_PLAIN;
        }
        return Response.ok(content).header(HttpHeaders.CONTENT_TYPE, contentType).build();
    }

    private String writeMetadata(RepositoryMetadata metadata) throws IOException {
        StringWriter sw = new StringWriter();
        new RepositoryMetadataXpp3Writer().write(sw, metadata);
        return sw.toString();
    }
}
