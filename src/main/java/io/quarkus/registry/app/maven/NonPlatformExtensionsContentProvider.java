package io.quarkus.registry.app.maven;

import java.io.StringWriter;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.quarkus.registry.app.DatabaseRegistryClient;
import io.quarkus.registry.catalog.ExtensionCatalog;
import io.quarkus.registry.catalog.json.JsonCatalogMapperHelper;
import org.apache.maven.artifact.Artifact;

@ApplicationScoped
public class NonPlatformExtensionsContentProvider implements ArtifactContentProvider {

    @Inject
    MavenConfig mavenConfig;

    @Inject
    ObjectMapper objectMapper;

    @Inject
    DatabaseRegistryClient registryClient;

    @Override
    public boolean supports(Artifact artifact, UriInfo uriInfo) {
        return mavenConfig.matchesNonPlatformExtensions(artifact);
    }

    @Override
    public Response provide(Artifact artifact, UriInfo uriInfo) throws Exception {
        String quarkusVersion = artifact.getClassifier();
        ExtensionCatalog catalog = registryClient.resolveNonPlatformExtensions(quarkusVersion);

        StringWriter sw = new StringWriter();
        JsonCatalogMapperHelper.serialize(objectMapper, catalog, sw);
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
