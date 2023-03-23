package io.quarkus.registry.app.maven;

import java.io.StringWriter;

import com.fasterxml.jackson.databind.ObjectMapper;

import io.quarkus.maven.dependency.ArtifactCoords;
import io.quarkus.registry.app.DatabaseRegistryClient;
import io.quarkus.registry.catalog.CatalogMapperHelper;
import io.quarkus.registry.catalog.PlatformCatalog;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.UriInfo;

/**
 * Lists the available platforms and their recommended versions, indicating which platform is the recommended default for new
 * projects
 */
@Singleton
public class PlatformsContentProvider implements ArtifactContentProvider {

    @Inject
    MavenConfig mavenConfig;

    @Inject
    ObjectMapper objectMapper;

    @Inject
    DatabaseRegistryClient registryClient;

    @Override
    public boolean supports(ArtifactCoords artifact, UriInfo uriInfo) {
        return mavenConfig.matchesQuarkusPlatforms(artifact);
    }

    @Override
    public Response provide(ArtifactCoords artifact, UriInfo uriInfo) throws Exception {
        var quarkusVersion = artifact.getClassifier();
        final PlatformCatalog platformCatalog;
        if (quarkusVersion.equals("all")) {
            platformCatalog = registryClient.resolveAllPlatforms();
        } else {
            platformCatalog = registryClient.resolveCurrentPlatformsCatalog(quarkusVersion);
        }
        if (platformCatalog == null || platformCatalog.getPlatforms().isEmpty()) {
            return Response.status(Response.Status.NOT_FOUND)
                    .header("X-Reason", "No platforms found")
                    .build();
        }
        StringWriter sw = new StringWriter();
        CatalogMapperHelper.serialize(objectMapper, platformCatalog, sw);
        String result = sw.toString();
        final String checksumSuffix = ArtifactParser.getChecksumSuffix(uriInfo.getPathSegments(), artifact);
        String contentType = MediaType.APPLICATION_JSON;
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
}
