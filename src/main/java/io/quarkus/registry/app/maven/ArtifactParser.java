package io.quarkus.registry.app.maven;

import java.util.List;

import io.quarkus.maven.ArtifactCoords;
import java.util.Arrays;
import javax.ws.rs.core.PathSegment;

public class ArtifactParser {

    public static final String MAVEN_METADATA_XML = "maven-metadata.xml";

    public static final String SUFFIX_MD5 = ".md5";

    public static final String SUFFIX_SHA1 = ".sha1";

    private static final String SNAPSHOT_SUFFIX = "-SNAPSHOT";


    public static ArtifactCoords parseCoords(String path){
        return ArtifactParser.parseCoords(Arrays.asList(path.split("/")));
    }
    
    public static ArtifactCoords parseCoords(List<String> pathSegmentList) {
        if (pathSegmentList.isEmpty()) {
            throw new IllegalArgumentException("Coordinates are missing");
        }
        
        final String fileName = getFileName(pathSegmentList);
        final String version = pathSegmentList.get(pathSegmentList.size() - 2);
        final String artifactId = pathSegmentList.get(pathSegmentList.size() - 3);

        String classifier = "";
        final String type;
        if (fileName.startsWith(artifactId)) {
            int typeEnd = fileName.length();
            if (fileName.endsWith(SUFFIX_SHA1)) {
                typeEnd -= SUFFIX_SHA1.length();
            } else if (fileName.endsWith(SUFFIX_MD5)) {
                typeEnd -= SUFFIX_MD5.length();
            }
            int typeStart = fileName.lastIndexOf('.', typeEnd - 1) + 1;

            type = fileName.substring(typeStart, typeEnd);
            final boolean snapshot = version.endsWith(SNAPSHOT_SUFFIX);
            // if it's a snapshot version, in some cases the file name will contain the actual -SNAPSHOT suffix,
            // in other cases the SNAPSHOT will be replaced with a timestamp+build number expression
            // e.g. instead of artifactId-baseVersion-SNAPSHOT the file name will look like artifactId-baseVersion-YYYYMMDD.HHMMSS-buildNumber
            final String baseVersion = snapshot ? version.substring(0, version.length() - SNAPSHOT_SUFFIX.length()) : version;
            final int versionStart = artifactId.length() + 1;
            int versionEnd;
            if (snapshot) {
                if (fileName.regionMatches(versionStart, version, 0, version.length())) {
                    // artifactId-version[-classifier].extensions
                    versionEnd = versionStart + version.length();
                } else {
                    versionEnd = fileName.indexOf('-', versionStart + baseVersion.length() + 1);
                    // Checking if there is a classifier
                    int start = versionEnd < 0 ? versionStart + baseVersion.length() : versionEnd + 1;
                    int classifierIdx = fileName.indexOf('-', start);
                    if (classifierIdx < 0) {
                        versionEnd = fileName.indexOf('.', start);
                    } else {
                        versionEnd = classifierIdx;
                    }
                }
            } else {
                versionEnd = versionStart + version.length();
            }

            if (fileName.charAt(versionEnd) == '-') {
                classifier = fileName.substring(versionEnd + 1, typeStart - 1);
            }
        } else if (fileName.startsWith(MAVEN_METADATA_XML)) {
            type = MAVEN_METADATA_XML;
        } else {
            throw new IllegalArgumentException("Artifact file name " + fileName + " does not start with the artifactId " + artifactId);
        }

        final StringBuilder groupId = new StringBuilder();
        groupId.append(pathSegmentList.get(0));
        for (int i = 1; i < pathSegmentList.size() - 3; ++i) {
            groupId.append('.').append(pathSegmentList.get(i));
        }

        return new ArtifactCoords(groupId.toString(), artifactId, classifier, type, version);
    }

    public static String getChecksumSuffix(List<PathSegment> pathSegmentList, ArtifactCoords parsedCoords) {
        final String fileName = getFileNameFromPathSegment(pathSegmentList);
        return fileName.endsWith(parsedCoords.getType()) ? null : fileName.substring(fileName.lastIndexOf(parsedCoords.getType()) + parsedCoords.getType().length());
    }
    
    public static String getFileName(List<String> pathSegmentList) {
        return pathSegmentList.get(pathSegmentList.size() - 1);
    }

    private static String getFileNameFromPathSegment(List<PathSegment> pathSegmentList) {
        return pathSegmentList.get(pathSegmentList.size() - 1).getPath();
    }
}