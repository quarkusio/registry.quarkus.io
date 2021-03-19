package io.quarkus.registry.app.maven;

import java.io.StringWriter;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.quarkus.maven.ArtifactCoords;
import io.quarkus.registry.app.DatabaseRegistryClient;
import io.quarkus.registry.catalog.json.JsonCatalogMapperHelper;
import io.quarkus.registry.config.json.JsonRegistryConfig;
import io.quarkus.registry.config.json.JsonRegistryDescriptorConfig;
import io.quarkus.registry.config.json.JsonRegistryNonPlatformExtensionsConfig;
import io.quarkus.registry.config.json.JsonRegistryPlatformsConfig;
import org.apache.maven.artifact.Artifact;

@ApplicationScoped
public class RegistryDescriptorContentProvider implements ArtifactContentProvider {

    @Inject
    MavenConfig mavenConfig;

    @Inject
    ObjectMapper objectMapper;

    @Inject
    DatabaseRegistryClient registryClient;

    @Override
    public boolean supports(Artifact artifact, UriInfo uriInfo) {
        return mavenConfig.matchesRegistryDescriptor(artifact);
    }

    @Override
    public Response provide(Artifact artifact, UriInfo uriInfo) throws Exception {
        JsonRegistryConfig config = new JsonRegistryConfig();
        // Add descriptor
        JsonRegistryDescriptorConfig descriptorConfig = new JsonRegistryDescriptorConfig();
        descriptorConfig.setArtifact(ArtifactCoords.pom(MavenConfig.GROUP_ID, MavenConfig.REGISTRY_ARTIFACT_ID, MavenConfig.VERSION));
        config.setDescriptor(descriptorConfig);
        // Add platforms
        JsonRegistryPlatformsConfig platformsConfig = new JsonRegistryPlatformsConfig();
        platformsConfig.setArtifact(ArtifactCoords.pom(MavenConfig.GROUP_ID, MavenConfig.PLATFORM_ARTIFACT_ID, MavenConfig.VERSION));
        config.setPlatforms(platformsConfig);
        // Add non-platforms
        JsonRegistryNonPlatformExtensionsConfig nonPlatformExtensionsConfig = new JsonRegistryNonPlatformExtensionsConfig();
        nonPlatformExtensionsConfig.setArtifact(ArtifactCoords.pom(MavenConfig.GROUP_ID, MavenConfig.NON_PLATFORM_ARTIFACT_ID, MavenConfig.VERSION));
        config.setNonPlatformExtensions(nonPlatformExtensionsConfig);
        StringWriter sw = new StringWriter();
        JsonCatalogMapperHelper.serialize(objectMapper, config, sw);
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
