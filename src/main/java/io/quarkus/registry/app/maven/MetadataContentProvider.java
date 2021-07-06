package io.quarkus.registry.app.maven;

import java.io.IOException;
import java.io.StringWriter;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;

import javax.inject.Inject;
import javax.inject.Singleton;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;

import io.quarkus.cache.CacheResult;
import io.quarkus.maven.ArtifactCoords;
import io.quarkus.registry.app.CacheNames;
import io.quarkus.registry.app.model.PlatformRelease;
import org.apache.maven.artifact.repository.metadata.Metadata;
import org.apache.maven.artifact.repository.metadata.Snapshot;
import org.apache.maven.artifact.repository.metadata.SnapshotVersion;
import org.apache.maven.artifact.repository.metadata.Versioning;
import org.apache.maven.artifact.repository.metadata.io.xpp3.MetadataXpp3Writer;

@Singleton
public class MetadataContentProvider implements ArtifactContentProvider {

    private static final MetadataXpp3Writer METADATA_WRITER = new MetadataXpp3Writer();

    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMddHHmmss", Locale.ROOT)
            .withZone(ZoneId.of("UTC"));

    @Inject
    MavenConfig mavenConfig;

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
        if (ArtifactParser.SUFFIX_MD5.equals(checksumSuffix)) {
            result = HashUtil.md5(result);
        } else if (ArtifactParser.SUFFIX_SHA1.equals(checksumSuffix)) {
            result = HashUtil.sha1(result);
        }

        return Response.ok(result)
                .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_XML)
                .build();
    }

    @CacheResult(cacheName = CacheNames.METADATA)
    Metadata generateMetadata(ArtifactCoords artifact) {
        Metadata newMetadata = new Metadata();
        newMetadata.setGroupId(artifact.getGroupId());
        newMetadata.setArtifactId(artifact.getArtifactId());

        Versioning versioning = new Versioning();
        newMetadata.setVersioning(versioning);

        versioning.updateTimestamp();

        Snapshot snapshot = new Snapshot();
        versioning.setSnapshot(snapshot);
        snapshot.setTimestamp(versioning.getLastUpdated().substring(0, 8) + "." + versioning.getLastUpdated().substring(8));
        snapshot.setBuildNumber(1);

        final String baseVersion = artifact.getVersion().substring(0, artifact.getVersion().length() - "SNAPSHOT".length());
        Instant now = Instant.now();
        addSnapshotVersion(versioning, snapshot, baseVersion, now, "pom");
        addSnapshotVersion(versioning, snapshot, baseVersion, now, "json");
        return newMetadata;
    }

    private static void addSnapshotVersion(Versioning versioning, Snapshot snapshot, final String baseVersion, Instant instant,
                                           String extension) {
        final String version = baseVersion + snapshot.getTimestamp() + "-" + snapshot.getBuildNumber();
        final SnapshotVersion sv = new SnapshotVersion();
        sv.setExtension(extension);
        sv.setVersion(version);
        sv.setUpdated(DATE_TIME_FORMATTER.format(instant));
        versioning.addSnapshotVersion(sv);
    }

    private String writeMetadata(Metadata metadata) throws IOException {
        StringWriter sw = new StringWriter();
        METADATA_WRITER.write(sw, metadata);
        return sw.toString();
    }
}
