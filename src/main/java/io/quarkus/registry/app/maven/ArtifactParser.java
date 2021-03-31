package io.quarkus.registry.app.maven;

import java.util.List;
import java.util.regex.Pattern;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.DefaultArtifact;

public class ArtifactParser {
    private static final String SNAPSHOT_SUFFIX = "-SNAPSHOT";

    private static final String MAVEN_METADATA_XML = "maven-metadata.xml";

    public static Artifact parseArtifact(List<String> pathSegmentList) {
        if (pathSegmentList.size() < 3) {
            throw new WebApplicationException("Coordinates are missing", Response.Status.BAD_REQUEST);
        }

        final String fileName = pathSegmentList.get(pathSegmentList.size() - 1);

        int idx = 3;
        final String version;
        if (fileName.startsWith(MAVEN_METADATA_XML)) {
            if (!MavenConfig.VERSION.equals(pathSegmentList.get(pathSegmentList.size() - 2))) {
                idx = 2;
            }
            version = MavenConfig.VERSION;
        } else {
            version = pathSegmentList.get(pathSegmentList.size() - 2);
        }
        String artifactId = pathSegmentList.get(pathSegmentList.size() - idx);
        final StringBuilder groupIdBuilder = new StringBuilder(pathSegmentList.get(0));
        for (int i = 1; i < pathSegmentList.size() - idx; ++i) {
            groupIdBuilder.append('.').append(pathSegmentList.get(i));
        }
        final String groupId = groupIdBuilder.toString();

        final String classifier;
        final String type;
        if (fileName.startsWith(MAVEN_METADATA_XML)) {
            type = fileName;
            classifier = "";
        } else if (fileName.startsWith(artifactId)) {
            int idxType;
            if (fileName.endsWith("sha1") || fileName.endsWith("md5")) {
                idxType = fileName.lastIndexOf('.', fileName.length() - 6);
            } else {
                idxType = fileName.lastIndexOf('.');
            }
            type = fileName.substring(idxType + 1);
            String remaining = fileName
                    .replace(artifactId, "")
                    .replace("-" + version, "")
                    .replace("." + type, "");
            // The remaining may be a timestamp for 1.0-SNAPSHOT
            if (!remaining.isEmpty() && !remaining.matches("-1.0-[0-9]{8}.[0-9]{6}-[0-9]") ) {
                classifier = remaining.substring(1);
            } else {
                classifier = "";
            }
        } else {
            throw new WebApplicationException(
                    "Artifact file name " + fileName + " does not start with the artifactId " + artifactId,
                    Response.Status.BAD_REQUEST);
        }

        return new DefaultArtifact(groupId, artifactId, version, null, type, classifier, null);
    }
}
