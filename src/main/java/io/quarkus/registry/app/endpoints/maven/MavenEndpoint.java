package io.quarkus.registry.app.endpoints.maven;

import java.io.IOException;
import java.util.List;

import javax.enterprise.inject.Instance;
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
import org.apache.maven.artifact.DefaultArtifact;

/**
 * Exposes a Maven endpoint for our tooling
 */
@Path("/maven")
public class MavenEndpoint {

    private static final String SNAPSHOT_SUFFIX = "-SNAPSHOT";
    private static final String MAVEN_METADATA_XML = "maven-metadata.xml";

    @Inject
    Instance<ArtifactContentProvider> providers;

    @GET
    @Path("/{path:.+}")
    public Response handleArtifactRequest(
            @PathParam("path") List<PathSegment> pathSegments,
            @Context UriInfo uriInfo) throws IOException {

        Artifact artifact = parseArtifact(pathSegments);
        for (ArtifactContentProvider contentProvider : providers) {
            if (contentProvider.supports(artifact, uriInfo)) {
                try {
                    String content = contentProvider.provide(artifact, uriInfo);
                    if (content != null) {
                        return Response.ok(content).build();
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                    return Response.serverError().build();
                }
            }
        }
        return Response.status(Response.Status.NOT_FOUND).build();
    }

    private static Artifact parseArtifact(List<PathSegment> pathSegmentList) {
        if (pathSegmentList.size() < 3) {
            throw new WebApplicationException("Coordinates are missing", Response.Status.BAD_REQUEST);
        }

        final String fileName = pathSegmentList.get(pathSegmentList.size() - 1).getPath();
        final String version = pathSegmentList.get(pathSegmentList.size() - 2).getPath();
        String artifactId = pathSegmentList.get(pathSegmentList.size() - 3).getPath();
        final String groupId;

        final String classifier;
        final String type;
        if (fileName.startsWith(MAVEN_METADATA_XML)) {
            type = fileName;
            classifier = "";
            final StringBuilder builder = new StringBuilder();
            builder.append(pathSegmentList.get(0).getPath());
            for (int i = 1; i < pathSegmentList.size() - 2; ++i) {
                builder.append('.').append(pathSegmentList.get(i));
            }
            groupId = builder.toString();
            artifactId = pathSegmentList.get(pathSegmentList.size() - 2).getPath();
        } else if (fileName.startsWith(artifactId)) {
            final boolean snapshot = version.endsWith(SNAPSHOT_SUFFIX);
            // if it's a snapshot version, in some cases the file name will contain the actual -SNAPSHOT suffix,
            // in other cases the SNAPSHOT will be replaced with a timestamp+build number expression
            // e.g. instead of artifactId-baseVersion-SNAPSHOT the file name will look like artifactId-baseVersion-YYYYMMDD.HHMMSS-buildNumber
            final String baseVersion = snapshot ? version.substring(0, version.length() - SNAPSHOT_SUFFIX.length()) : version;
            final int versionStart = fileName.lastIndexOf(baseVersion);
            int versionEnd;
            if (snapshot) {
                versionEnd = fileName.indexOf('-', versionStart + baseVersion.length() + 1);
                versionEnd = fileName.indexOf('.', versionEnd < 0 ? versionStart + baseVersion.length() : versionEnd + 1);
            } else {
                versionEnd = versionStart + version.length();
            }
            type = fileName.substring(versionEnd + 1);
            classifier = artifactId.length() + 1 < versionStart
                    ? fileName.substring(artifactId.length() + 1, versionStart - 1)
                    : "";

            final StringBuilder builder = new StringBuilder();
            builder.append(pathSegmentList.get(0).getPath());
            for (int i = 1; i < pathSegmentList.size() - 3; ++i) {
                builder.append('.').append(pathSegmentList.get(i));
            }
            groupId = builder.toString();
        } else {
            throw new IllegalArgumentException(
                    "Artifact file name " + fileName + " does not start with the artifactId " + artifactId);
        }

        return new DefaultArtifact(groupId, artifactId, version, null, type, classifier, null);
    }
}
