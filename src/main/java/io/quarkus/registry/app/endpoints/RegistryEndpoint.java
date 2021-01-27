package io.quarkus.registry.app.endpoints;

import java.util.Optional;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.ws.rs.FormParam;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import io.quarkus.registry.app.dto.PlatformDTO;
import io.quarkus.registry.app.model.Extension;
import io.quarkus.registry.app.model.ExtensionRelease;
import io.quarkus.registry.app.model.Platform;
import io.quarkus.registry.app.model.PlatformRelease;
import io.quarkus.registry.app.services.ArtifactResolverService;
import io.quarkus.registry.app.services.RegistryService;

@SuppressWarnings("OptionalUsedAsFieldOrParameterType")
@ApplicationScoped
@Path("/api/registry")

public class RegistryEndpoint {

    @Inject
    ArtifactResolverService artifactResolverService;

    @Inject
    RegistryService registryService;

    @POST
    @Path("/platform")
    @Produces(MediaType.APPLICATION_JSON)
    public PlatformDTO addPlatform(
            @FormParam("groupId") String groupId,
            @FormParam("artifactId") String artifactId,
            @FormParam("version") Optional<String> version) {
        final String resolvedVersion = version
                .orElseGet(() -> artifactResolverService.resolveLatestVersion(groupId, artifactId));
        PlatformRelease platformRelease = PlatformRelease.findByGAV(groupId, artifactId, resolvedVersion)
                .orElseGet(() -> registryService
                        .includePlatform(groupId, artifactId, resolvedVersion));
        PlatformDTO dto = new PlatformDTO();
        dto.groupId = groupId;
        dto.artifactId = artifactId;
        dto.version = resolvedVersion;
        return dto;
    }

    @POST
    @Path("/extension")
    @Produces(MediaType.APPLICATION_JSON)
    public ExtensionRelease addExtension(
            @FormParam("groupId") String groupId,
            @FormParam("artifactId") String artifactId,
            @FormParam("version") Optional<String> version) {
        final String resolvedVersion = version
                .orElseGet(() -> artifactResolverService.resolveLatestVersion(groupId, artifactId));
        return ExtensionRelease.findByGAV(groupId, artifactId, resolvedVersion)
                .orElseGet(() -> registryService.includeExtensionRelease(groupId, artifactId, resolvedVersion));
    }
}
