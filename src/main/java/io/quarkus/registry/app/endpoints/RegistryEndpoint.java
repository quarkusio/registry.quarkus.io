package io.quarkus.registry.app.endpoints;

import java.util.Optional;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import io.quarkus.registry.app.model.Extension;
import io.quarkus.registry.app.model.Platform;
import io.quarkus.registry.app.services.ArtifactResolverService;
import io.quarkus.registry.app.services.RegistryService;
import io.quarkus.vertx.web.Param;
import io.quarkus.vertx.web.Route;
import io.smallrye.common.annotation.Blocking;
import io.vertx.core.http.HttpMethod;

@SuppressWarnings("OptionalUsedAsFieldOrParameterType")
@ApplicationScoped
public class RegistryEndpoint {

    @Inject
    ArtifactResolverService artifactResolverService;

    @Inject
    RegistryService registryService;

    @Route(path = "/api/registry/platform", methods = HttpMethod.POST)
    @Blocking
    public Platform addPlatform(
            @Param String groupId,
            @Param String artifactId,
            @Param Optional<String> version) {
        final String resolvedVersion = version
                .orElseGet(() -> artifactResolverService.resolveLatestVersion(groupId, artifactId));
        return Platform.findByGAV(groupId, artifactId, resolvedVersion)
                .orElseGet(() -> registryService
                        .includePlatform(groupId, artifactId, resolvedVersion));
    }

    @Route(path = "/api/registry/extension", methods = HttpMethod.POST)
    @Blocking
    public Extension addExtension(
            @Param String groupId,
            @Param String artifactId,
            @Param Optional<String> version) {
        final String resolvedVersion = version
                .orElseGet(() -> artifactResolverService.resolveLatestVersion(groupId, artifactId));
        return Extension.findByGAV(groupId, artifactId, resolvedVersion)
                .orElseGet(() -> registryService.includeExtension(groupId, artifactId, resolvedVersion));
    }
}
