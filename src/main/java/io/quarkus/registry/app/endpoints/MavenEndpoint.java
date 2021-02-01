package io.quarkus.registry.app.endpoints;

import java.io.IOException;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import javax.inject.Inject;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.quarkus.registry.app.model.Platform;
import io.quarkus.registry.app.model.PlatformRelease;
import org.apache.commons.codec.binary.Hex;
import org.apache.maven.artifact.repository.metadata.Metadata;
import org.apache.maven.artifact.repository.metadata.Versioning;
import org.apache.maven.artifact.repository.metadata.io.xpp3.MetadataXpp3Writer;
import org.apache.maven.model.Model;
import org.apache.maven.model.io.xpp3.MavenXpp3Writer;

/**
 * Exposes a Maven endpoint for our tooling
 */
@Path("/maven")
public class MavenEndpoint {

    @Inject
    ObjectMapper objectMapper;

    @GET
    @Path("/{groupId:.+}/{artifactId}/maven-metadata.xml")
    @Produces(MediaType.APPLICATION_XML)
    public Response getMavenMetadata(@PathParam("groupId") String groupIdPath, @PathParam("artifactId") String artifactId)
            throws IOException {
        String groupId = groupIdPath.replace('/', '.');
        Metadata m = Platform.findByGA(groupId, artifactId).map(platform -> {
            Metadata metadata = new Metadata();
            metadata.setGroupId(groupId);
            metadata.setArtifactId(artifactId);
            Versioning versioning = new Versioning();
            metadata.setVersioning(versioning);
            for (PlatformRelease release : platform.releases) {
                versioning.addVersion(release.version);
            }
            return metadata;
        }).orElse(null);

        if (m == null) {
            return Response.status(Response.Status.NOT_FOUND).build();
        }
        StringWriter sw = new StringWriter();
        new MetadataXpp3Writer().write(sw, m);
        return Response.ok(sw.toString()).build();
    }

    @GET
    @Path("/{groupId:.+}/{artifactId}/{version}/{artifactId}-{version}.json")
    public Response getJson(@PathParam("groupId") String groupIdPath, @PathParam("artifactId") String artifactId,
            @PathParam("version") String version) {
        throw new UnsupportedOperationException("Not implemented yet");
    }

    @GET
    @Path("/{groupId:.+}/{artifactId}/{version}/{artifactId}-{version}.pom")
    @Produces(MediaType.APPLICATION_XML)
    public Response getPom(@PathParam("groupId") String groupIdPath, @PathParam("artifactId") String artifactId,
            @PathParam("version") String version)
            throws IOException {
        String groupId = groupIdPath.replace('/', '.');
        String pom = generatePom(groupId, artifactId, version);
        return Response.ok(pom).build();
    }

    @GET
    @Path("/{groupId:.+}/{artifactId}/{version}/{artifactId}-{version}.pom.sha1")
    @Produces(MediaType.TEXT_PLAIN)
    public Response getChecksum(@PathParam("groupId:.+") String groupIdPath,
            @PathParam("artifactId") String artifactId,
            @PathParam("version") String version)
            throws IOException, NoSuchAlgorithmException {
        String groupId = groupIdPath.replace('/', '.');
        String pom = generatePom(groupId, artifactId, version);
        MessageDigest md = MessageDigest.getInstance("SHA-1");
        byte[] digest = md.digest(pom.getBytes(StandardCharsets.UTF_8));
        return Response.ok(Hex.encodeHexString(digest)).build();
    }

    private String generatePom(String groupId, String artifactId, String version) throws IOException {
        Model model = new Model();
        model.setGroupId(groupId);
        model.setArtifactId(artifactId);
        model.setVersion(version);
        //        model.setPackaging("pom");
        StringWriter writer = new StringWriter();
        new MavenXpp3Writer().write(writer, model);
        return writer.toString();
    }

}
