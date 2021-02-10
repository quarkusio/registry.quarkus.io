package io.quarkus.registry.app.admin;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import javax.annotation.security.RolesAllowed;
import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.event.Event;
import javax.inject.Inject;
import javax.ws.rs.FormParam;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import io.quarkus.qute.TemplateInstance;
import io.quarkus.qute.api.CheckedTemplate;
import io.quarkus.registry.app.dto.PlatformDTO;
import io.quarkus.registry.app.events.BaseEvent;
import io.quarkus.registry.app.events.ExtensionCreateEvent;
import io.quarkus.registry.app.events.PlatformCreateEvent;
import io.quarkus.registry.app.model.Extension;
import io.quarkus.registry.app.model.ExtensionRelease;
import io.quarkus.registry.app.model.Platform;
import io.quarkus.registry.app.model.PlatformRelease;
import io.quarkus.registry.app.services.ArtifactResolverService;

@SuppressWarnings("OptionalUsedAsFieldOrParameterType")
@ApplicationScoped
@Path("/admin")
@RolesAllowed("admin")
public class AdminResource {

    @Inject
    ArtifactResolverService artifactResolverService;

    @Inject
    Event<BaseEvent> emitter;

    @GET
    @Produces(MediaType.TEXT_HTML)
    public TemplateInstance index() {
        return Templates.index();
    }

    @GET
    @Path("platforms")
    @Produces(MediaType.TEXT_HTML)
    public TemplateInstance platforms() {
        return Templates.platforms(Platform.listAll());
    }

    @GET
    @Path("extensions")
    @Produces(MediaType.TEXT_HTML)
    public TemplateInstance extensions(@QueryParam("platform") String platform) {
        List<Extension> extensions;
        if (platform != null) {
            String[] gav = platform.split(":");
            PlatformRelease release = PlatformRelease.findByGAV(gav[0], gav[1], gav[2])
                    .orElseThrow(() -> new WebApplicationException(
                            Response.Status.BAD_REQUEST));
            // TODO: Show only extensions for the selected platform
        }
        extensions = Extension.listAll();
        return Templates.extensions(extensions);
    }

    @POST
    @Path("/api/v1/platform")
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
        return Response.status(Response.Status.CREATED).entity(dto).build();
    }

    @POST
    @Path("/api/v1/extension")
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

    @CheckedTemplate
    public static class Templates {
        public static native TemplateInstance index();

        public static native TemplateInstance platforms(List<Platform> platforms);

        public static native TemplateInstance extensions(List<Extension> extensions);
    }
}
