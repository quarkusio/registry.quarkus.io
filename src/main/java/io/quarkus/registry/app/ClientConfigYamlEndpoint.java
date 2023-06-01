package io.quarkus.registry.app;

import java.io.IOException;
import java.io.StringWriter;
import java.io.Writer;

import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;

import io.quarkus.maven.dependency.ArtifactCoords;
import io.quarkus.registry.app.maven.MavenConfig;
import io.quarkus.registry.config.*;
import io.quarkus.resteasy.reactive.server.EndpointDisabled;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

/**
 * Exposes the Quarkus Registry Client configuration file on `/config.yaml`. This is only enabled if
 * {@code quarkus.registry.expose.client.config.yaml} is set to {@code true}.
 */
@ApplicationScoped
@Path("/client")
@Tag(name = "Client", description = "Client related services")
@EndpointDisabled(name = "quarkus.registry.expose.client.config.yaml", stringValue = "false", disableIfMissing = true)
public class ClientConfigYamlEndpoint {

    @Inject
    MavenConfig mavenConfig;

    @GET
    @Path("/config.yaml")
    @Produces(MediaType.TEXT_PLAIN)
    @Operation(summary = "Example Quarkus Registry Client configuration file")
    public String clientConfigYaml() throws IOException {
        ArtifactCoords coords = ArtifactCoords
                .fromString(mavenConfig.getRegistryGroupId() + ":quarkus-registry-descriptor::json:1.0-SNAPSHOT");
        RegistryMavenRepoConfig mavenRepoConfig = RegistryMavenRepoConfig.builder().setUrl(mavenConfig.getRegistryUrl())
                .build();
        RegistryConfig registry = RegistryConfig.builder()
                .setId(mavenConfig.getRegistryId())
                .setUpdatePolicy("always")
                .setDescriptor(
                        RegistryDescriptorConfig.builder().setArtifact(coords))
                .setMaven(
                        RegistryMavenConfig.builder()
                                .setRepository(mavenRepoConfig)
                                .build())
                .build();
        RegistriesConfig registries = RegistriesConfig.builder().setRegistry(registry).build();
        Writer resultWriter = new StringWriter();
        RegistriesConfigMapperHelper.toYaml(registries, resultWriter);
        return resultWriter.toString();
    }

}
