package io.quarkus.registry.app.maven;

import java.io.IOException;
import java.io.StringWriter;

import javax.inject.Inject;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import io.quarkus.cache.CacheResult;
import io.quarkus.registry.app.CacheNames;
import org.jboss.logging.Logger;
import org.sonatype.nexus.repository.metadata.model.RepositoryMetadata;
import org.sonatype.nexus.repository.metadata.model.io.xpp3.RepositoryMetadataXpp3Writer;

/**
 * Exposes a Maven resource for our tooling
 */
@Path("/maven/.meta")
public class MavenMetadataResource {

    private static final Logger log = Logger.getLogger(MavenMetadataResource.class);

    @Inject
    MavenConfig mavenConfig;

    @GET
    @Path("prefixes.txt")
    @Produces(MediaType.TEXT_PLAIN)
    public String handlePrefixesTxt() {
        return "/" + mavenConfig.getRegistryGroupId().replace('.', '/');
    }

    @GET
    @Path("repository-metadata.{extension}")
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

    private String writeMetadata(RepositoryMetadata metadata) throws IOException {
        StringWriter sw = new StringWriter();
        new RepositoryMetadataXpp3Writer().write(sw, metadata);
        return sw.toString();
    }
}
