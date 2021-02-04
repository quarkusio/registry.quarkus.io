package io.quarkus.registry.app.endpoints;

import java.util.Optional;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.event.Event;
import javax.inject.Inject;
import javax.ws.rs.FormParam;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import io.quarkus.registry.app.dto.PlatformDTO;
import io.quarkus.registry.app.events.BaseEvent;
import io.quarkus.registry.app.events.ExtensionCreateEvent;
import io.quarkus.registry.app.events.PlatformCreateEvent;
import io.quarkus.registry.app.model.ExtensionRelease;
import io.quarkus.registry.app.model.PlatformRelease;
import io.quarkus.registry.app.services.ArtifactResolverService;

@SuppressWarnings("OptionalUsedAsFieldOrParameterType")
@ApplicationScoped
@Path("/admin/registry")
public class AdminEndpoint {

    @Inject
    ArtifactResolverService artifactResolverService;

    @Inject
    Event<BaseEvent> emitter;

    @POST
    @Path("platform")
    @Produces(MediaType.APPLICATION_JSON)
    public Response addPlatform(
            @FormParam("groupId") String groupId,
            @FormParam("artifactId") String artifactId,
            @FormParam("version") Optional<String> version) {
        final String resolvedVersion = version
                .orElseGet(() -> artifactResolverService.resolveLatestVersion(groupId, artifactId));
        Optional<PlatformRelease> platformRelease = PlatformRelease.findByGAV(groupId, artifactId, resolvedVersion);
        if (platformRelease.isPresent()) {
            return Response.status(Response.Status.CONFLICT).build();
        }
        PlatformCreateEvent event = new PlatformCreateEvent(groupId, artifactId, resolvedVersion);
        emitter.fireAsync(event);
        PlatformDTO dto = new PlatformDTO();
        dto.groupId = groupId;
        dto.artifactId = artifactId;
        dto.version = resolvedVersion;
        return Response.ok(dto).build();
    }

    @POST
    @Path("extension")
    @Produces(MediaType.APPLICATION_JSON)
    public Response addExtension(
            @FormParam("groupId") String groupId,
            @FormParam("artifactId") String artifactId,
            @FormParam("version") Optional<String> version) {
        final String resolvedVersion = version
                .orElseGet(() -> artifactResolverService.resolveLatestVersion(groupId, artifactId));
        Optional<ExtensionRelease> extensionRelease = ExtensionRelease.findByGAV(groupId, artifactId, resolvedVersion);
        if (extensionRelease.isPresent()) {
            return Response.status(Response.Status.CONFLICT).build();
        }
        ExtensionCreateEvent event = new ExtensionCreateEvent(groupId, artifactId, resolvedVersion);
        emitter.fireAsync(event);
        return Response.ok().build();
    }
}
