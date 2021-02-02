package io.quarkus.registry.app.endpoints;

import java.io.IOException;
import java.io.StringWriter;
import java.net.URL;
import java.util.List;
import java.util.Optional;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.PathSegment;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;

import io.quarkus.registry.app.model.Platform;
import io.quarkus.registry.app.model.PlatformRelease;
import io.quarkus.registry.app.util.HashUtil;
import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.DefaultArtifact;
import org.apache.maven.artifact.repository.metadata.Metadata;
import org.apache.maven.artifact.repository.metadata.Versioning;
import org.apache.maven.artifact.repository.metadata.io.xpp3.MetadataXpp3Writer;
import org.apache.maven.model.Model;
import org.apache.maven.model.Repository;
import org.apache.maven.model.RepositoryPolicy;
import org.apache.maven.model.io.xpp3.MavenXpp3Writer;

/**
 * Exposes a Maven endpoint for our tooling
 */
@Path("/maven")
public class MavenEndpoint {

    private static final MavenXpp3Writer POM_WRITER = new MavenXpp3Writer();
    private static final MetadataXpp3Writer METADATA_WRITER = new MetadataXpp3Writer();

    private static final String SNAPSHOT_SUFFIX = "-SNAPSHOT";
    private static final String MAVEN_METADATA_XML = "maven-metadata.xml";

    @GET
    @Path("/{path:.+}")
    public Response handleArtifactRequest(
            @PathParam("path") List<PathSegment> pathSegments,
            @Context UriInfo uriInfo) throws IOException {

        Artifact artifact = parseArtifact(pathSegments);

        String type = artifact.getType();
        if (type.startsWith("pom")) {
            Optional<PlatformRelease> platformRelease = PlatformRelease
                    .findByGAV(artifact.getGroupId(), artifact.getArtifactId(), artifact.getVersion());
            if (platformRelease.isPresent()) {
                String result = generatePom(artifact, uriInfo);
                if (type.endsWith(".md5")) {
                    result = HashUtil.md5(result);
                } else if (type.endsWith(".sha1")) {
                    result = HashUtil.sha1(result);
                }
                return Response.ok(result).build();
            } else {
                return Response.status(Response.Status.NOT_FOUND).build();
            }
        } else if (MAVEN_METADATA_XML.equals(type)) {
            return getMavenMetadata(artifact)
                    .map(metadata -> {
                        StringWriter sw = new StringWriter();
                        try {
                            METADATA_WRITER.write(sw, metadata);
                        } catch (IOException e) {
                            throw new WebApplicationException("Could not serialize metadata", e,
                                    Response.Status.INTERNAL_SERVER_ERROR);
                        }
                        return Response.ok(sw.toString()).build();
                    })
                    .orElseGet(() -> Response.status(Response.Status.NOT_FOUND).build());
        }
        return Response.status(Response.Status.NOT_FOUND).build();
    }

    private Optional<Metadata> getMavenMetadata(Artifact artifact) {
        String groupId = artifact.getGroupId();
        String artifactId = artifact.getArtifactId();
        return Platform.findByGA(groupId, artifactId).map(platform -> {
            Metadata metadata = new Metadata();
            metadata.setGroupId(groupId);
            metadata.setArtifactId(artifactId);
            Versioning versioning = new Versioning();
            metadata.setVersioning(versioning);
            for (PlatformRelease release : platform.releases) {
                versioning.addVersion(release.version);
            }
            return metadata;
        });
    }

    private static String generatePom(Artifact artifact, UriInfo uriInfo) throws IOException {
        Model model = new Model();
        model.setGroupId(artifact.getGroupId());
        model.setArtifactId(artifact.getArtifactId());
        model.setVersion(artifact.getVersion());
        model.setPackaging("pom");

        final Repository repo = new Repository();
        repo.setId("quarkiverse-registry");
        repo.setName("Quarkiverse Extension Registry");
        repo.setUrl(new URL(uriInfo.getBaseUri().toURL(), "maven").toExternalForm());

        RepositoryPolicy policy = new RepositoryPolicy();
        policy.setEnabled(true);
        policy.setUpdatePolicy("always");
        repo.setSnapshots(policy);

        model.addRepository(repo);

        StringWriter writer = new StringWriter();
        POM_WRITER.write(writer, model);
        return writer.toString();
    }

    private static Artifact parseArtifact(List<PathSegment> pathSegmentList) {
        if (pathSegmentList.isEmpty()) {
            throw new IllegalArgumentException("Coordinates are missing");
        }

        final String fileName = pathSegmentList.get(pathSegmentList.size() - 1).getPath();
        final String version = pathSegmentList.get(pathSegmentList.size() - 2).getPath();
        final String artifactId = pathSegmentList.get(pathSegmentList.size() - 3).getPath();

        final String classifier;
        final String type;
        if (fileName.startsWith(MAVEN_METADATA_XML)) {
            type = fileName;
            classifier = "";
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
        } else {
            throw new IllegalArgumentException(
                    "Artifact file name " + fileName + " does not start with the artifactId " + artifactId);
        }

        final StringBuilder groupId = new StringBuilder();
        groupId.append(pathSegmentList.get(0).getPath());
        for (int i = 1; i < pathSegmentList.size() - 3; ++i) {
            groupId.append('.').append(pathSegmentList.get(i));
        }

        return new DefaultArtifact(groupId.toString(), artifactId, version, null, type, classifier, null);
    }

}
