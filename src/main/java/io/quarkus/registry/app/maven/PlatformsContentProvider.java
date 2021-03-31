package io.quarkus.registry.app.maven;

import java.io.StringWriter;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.inject.Singleton;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.quarkus.maven.ArtifactCoords;
import io.quarkus.registry.app.DatabaseRegistryClient;
import io.quarkus.registry.catalog.PlatformCatalog;
import io.quarkus.registry.catalog.json.JsonCatalogMapperHelper;
import org.apache.maven.artifact.Artifact;

/**
 * Lists the available platforms and their recommended versions, indicating which platform is the recommended default for new projects
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
        String quarkusVersion = artifact.getClassifier();
        PlatformCatalog platformCatalog = registryClient.resolvePlatforms(quarkusVersion);
        StringWriter sw = new StringWriter();
        JsonCatalogMapperHelper.serialize(objectMapper, platformCatalog, sw);
        String result = sw.toString();
        if (artifact.getType().endsWith(".md5")) {
            result = HashUtil.md5(result);
        } else if (artifact.getType().endsWith(".sha1")) {
            result = HashUtil.sha1(result);
        }
        return Response.ok(result)
                .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON)
                .build();
    }
}
