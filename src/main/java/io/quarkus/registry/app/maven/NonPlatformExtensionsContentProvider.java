package io.quarkus.registry.app.maven;

import java.io.StringWriter;

import com.fasterxml.jackson.databind.ObjectMapper;

import io.quarkus.maven.dependency.ArtifactCoords;
import io.quarkus.registry.app.DatabaseRegistryClient;
import io.quarkus.registry.catalog.CatalogMapperHelper;
import io.quarkus.registry.catalog.ExtensionCatalog;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.UriInfo;

@Singleton
public class NonPlatformExtensionsContentProvider implements ArtifactContentProvider {

    @Inject
    MavenConfig mavenConfig;

    @Inject
    ObjectMapper objectMapper;

    @Inject
    DatabaseRegistryClient registryClient;

    @Override
    public boolean supports(ArtifactCoords artifact, UriInfo uriInfo) {
        return mavenConfig.matchesNonPlatformExtensions(artifact);
    }

    @Override
    public Response provide(ArtifactCoords artifact, UriInfo uriInfo) throws Exception {
        String quarkusVersion = artifact.getClassifier();
        ExtensionCatalog catalog = registryClient.resolveNonPlatformExtensionsCatalog(quarkusVersion);

        StringWriter sw = new StringWriter();
        CatalogMapperHelper.serialize(objectMapper, catalog, sw);
        return toResponse(artifact, uriInfo, sw.toString(), MediaType.APPLICATION_JSON);
    }
}
