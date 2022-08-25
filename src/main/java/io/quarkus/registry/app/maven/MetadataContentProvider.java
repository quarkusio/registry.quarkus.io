package io.quarkus.registry.app.maven;

import java.io.IOException;
import java.io.StringWriter;
import java.util.Collections;
import java.util.Date;
import java.util.List;

import javax.inject.Inject;
import javax.inject.Singleton;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;

import org.apache.maven.artifact.repository.metadata.Metadata;
import org.apache.maven.artifact.repository.metadata.Snapshot;
import org.apache.maven.artifact.repository.metadata.SnapshotVersion;
import org.apache.maven.artifact.repository.metadata.Versioning;
import org.apache.maven.artifact.repository.metadata.io.xpp3.MetadataXpp3Writer;

import io.quarkus.cache.CacheResult;
import io.quarkus.maven.dependency.ArtifactCoords;
import io.quarkus.registry.app.CacheNames;
import io.quarkus.registry.app.maven.cache.MavenCacheState;
import io.quarkus.registry.app.model.PlatformRelease;

@Singleton
public class MetadataContentProvider implements ArtifactContentProvider {

    private static final MetadataXpp3Writer METADATA_WRITER = new MetadataXpp3Writer();

    private static final List<String> EMPTY_CLASSIFIER = Collections.singletonList("");

    @Inject
    MavenConfig mavenConfig;

    @Inject
    MavenCacheState mavenCacheState;

    @Override
    public boolean supports(ArtifactCoords artifact, UriInfo uriInfo) {
        return mavenConfig.supports(artifact)
                && artifact.getType() != null &&
                artifact.getType().startsWith(ArtifactParser.MAVEN_METADATA_XML);
    }

    @Override
    public Response provide(ArtifactCoords artifact, UriInfo uriInfo) throws IOException {
        Metadata metadata = generateMetadata(artifact);
        String result = writeMetadata(metadata);
        final String checksumSuffix = ArtifactParser.getChecksumSuffix(uriInfo.getPathSegments(), artifact);
        String contentType = MediaType.APPLICATION_XML;
        if (ArtifactParser.SUFFIX_MD5.equals(checksumSuffix)) {
            result = HashUtil.md5(result);
            contentType = MediaType.TEXT_PLAIN;
        } else if (ArtifactParser.SUFFIX_SHA1.equals(checksumSuffix)) {
            result = HashUtil.sha1(result);
            contentType = MediaType.TEXT_PLAIN;
        }

        return Response.ok(result)
                .header(HttpHeaders.CONTENT_TYPE, contentType)
                .build();
    }

    @CacheResult(cacheName = CacheNames.METADATA)
    Metadata generateMetadata(ArtifactCoords artifact) {
        Metadata newMetadata = new Metadata();
        newMetadata.setGroupId(artifact.getGroupId());
        newMetadata.setArtifactId(artifact.getArtifactId());
        newMetadata.setVersion(artifact.getVersion());

        Versioning versioning = new Versioning();
        newMetadata.setVersioning(versioning);

        Date lastUpdated = mavenCacheState.getLastUpdate();
        versioning.setLastUpdatedTimestamp(lastUpdated);

        Snapshot snapshot = new Snapshot();
        versioning.setSnapshot(snapshot);
        snapshot.setTimestamp(versioning.getLastUpdated().substring(0, 8) + "." + versioning.getLastUpdated().substring(8));
        snapshot.setBuildNumber(1);

        final String baseVersion = artifact.getVersion().substring(0, artifact.getVersion().length() - "SNAPSHOT".length());
        addSnapshotVersion(versioning, snapshot, baseVersion, "pom", EMPTY_CLASSIFIER);
        addSnapshotVersion(versioning, snapshot, baseVersion, "json", EMPTY_CLASSIFIER);
        addSnapshotVersion(versioning, snapshot, baseVersion, "json", PlatformRelease.findQuarkusCores());
        addSnapshotVersion(versioning, snapshot, baseVersion, "json", List.of("all"));
        return newMetadata;
    }

    private static void addSnapshotVersion(Versioning versioning, Snapshot snapshot, final String baseVersion,
            String extension, List<String> classifiers) {
        final String version = baseVersion + snapshot.getTimestamp() + "-" + snapshot.getBuildNumber();
        for (String classifier : classifiers) {
            final SnapshotVersion sv = new SnapshotVersion();
            sv.setExtension(extension);
            sv.setVersion(version);
            sv.setClassifier(classifier);
            sv.setUpdated(versioning.getLastUpdated());
            versioning.addSnapshotVersion(sv);
        }
    }

    private String writeMetadata(Metadata metadata) throws IOException {
        StringWriter sw = new StringWriter();
        METADATA_WRITER.write(sw, metadata);
        return sw.toString();
    }
}
