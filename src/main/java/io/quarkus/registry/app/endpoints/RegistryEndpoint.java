package io.quarkus.registry.app.endpoints;

import javax.inject.Inject;
import javax.ws.rs.BeanParam;
import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import io.quarkus.registry.app.model.Extension;
import io.quarkus.registry.app.model.Platform;
import io.quarkus.registry.app.services.ArtifactResolverService;
import io.quarkus.registry.app.services.RegistryService;
import io.smallrye.mutiny.Uni;

@Path("/registry")
public class RegistryEndpoint {

    @Inject
    ArtifactResolverService artifactResolverService;

    @Inject
    RegistryService registryService;

    @POST
    @Path("/platform")
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    public Uni<Response> addPlatform(@BeanParam ArtifactCoords coords) {
        final String version;
        if (coords.version == null) {
            version = artifactResolverService.resolveLatestVersion(coords.groupId, coords.artifactId);
        } else {
            version = coords.version;
        }
        return Platform.findByGAV(coords.groupId, coords.artifactId, version)
                .onItem().ifNull().switchTo(() -> registryService
                        .includePlatform(coords.groupId, coords.artifactId, version))
                .onItem().transform(e -> Response.ok(e).build());
    }

    @POST
    @Path("/extension")
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    public Uni<Response> addExtension(@BeanParam ArtifactCoords coords) {
        final String version;
        if (coords.version == null) {
            version = artifactResolverService.resolveLatestVersion(coords.groupId, coords.artifactId);
        } else {
            version = coords.version;
        }
        return Extension
                .findByGAV(coords.groupId, coords.artifactId, version)
                .onItem().ifNull()
                .switchTo(() -> registryService.includeExtension(coords.groupId, coords.artifactId, version))
                .onItem().transform(e -> Response.ok(e).build());
    }
}
