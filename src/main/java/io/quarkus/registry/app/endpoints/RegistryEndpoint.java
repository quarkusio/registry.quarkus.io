package io.quarkus.registry.app.endpoints;

import javax.inject.Inject;
import javax.persistence.NoResultException;
import javax.validation.Valid;
import javax.ws.rs.BeanParam;
import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import io.quarkus.registry.app.model.Extension;
import io.quarkus.registry.app.model.Platform;
import io.quarkus.registry.app.services.RegistryService;
import io.smallrye.mutiny.Uni;

@Path("/registry")
public class RegistryEndpoint {

    @Inject
    RegistryService registryService;

    @POST
    @Path("/platform")
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    public Uni<Response> addPlatform(@Valid @BeanParam ArtifactCoords coords) {
        return Platform
                .findByGroupIdAndArtifactId(coords.groupId, coords.artifactId)
                .onItem().transform(e -> Response.status(Response.Status.CONFLICT).build())
                .onFailure(NoResultException.class)
                .recoverWithUni(() ->
                        registryService.includeLatestPlatform(coords.groupId, coords.artifactId)
                                .onItem()
                                .transform(e -> Response.ok(e).build()));

    }

    @POST
    @Path("/extension")
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    public Uni<Response> addExtension(@Valid @BeanParam ArtifactCoords coords) {
        return Extension
                .findByGroupIdAndArtifactId(coords.groupId, coords.artifactId)
                .onItem().transform(e -> Response.status(Response.Status.CONFLICT).build())
                .onFailure(NoResultException.class)
                .recoverWithUni(() ->
                        registryService.includeLatestExtension(coords.groupId, coords.artifactId)
                                .onItem()
                                .transform(e -> Response.ok(e).build()));
    }
}
