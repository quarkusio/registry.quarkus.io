package io.quarkus.registry.app.admin;

import java.util.List;
import java.util.Optional;

import javax.annotation.security.RolesAllowed;
import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.event.Event;
import javax.inject.Inject;
import javax.inject.Named;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import com.fasterxml.jackson.jaxrs.yaml.YAMLMediaTypes;
import io.quarkus.maven.ArtifactCoords;
import io.quarkus.qute.CheckedTemplate;
import io.quarkus.qute.TemplateInstance;
import io.quarkus.registry.app.events.BaseEvent;
import io.quarkus.registry.app.events.ExtensionCreateEvent;
import io.quarkus.registry.app.events.PlatformCreateEvent;
import io.quarkus.registry.app.model.Extension;
import io.quarkus.registry.app.model.ExtensionRelease;
import io.quarkus.registry.app.model.Platform;
import io.quarkus.registry.app.model.PlatformRelease;
import io.quarkus.registry.catalog.json.JsonExtension;
import io.quarkus.registry.catalog.json.JsonPlatform;

@ApplicationScoped
@Path("/admin")
@RolesAllowed("admin")
public class AdminResource {

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
    @Path("/v1/platform")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes({MediaType.APPLICATION_JSON, YAMLMediaTypes.APPLICATION_JACKSON_YAML})
    public Response addPlatform(JsonPlatform platform) {
        ArtifactCoords bom = platform.getBom();
        Optional<PlatformRelease> platformRelease = PlatformRelease
                .findByGAV(bom.getGroupId(), bom.getArtifactId(), bom.getVersion());
        if (platformRelease.isPresent()) {
            return Response.status(Response.Status.CONFLICT).build();
        }
        emitter.fireAsync(new PlatformCreateEvent(platform));
        return Response.accepted(bom).build();
    }

    @POST
    @Path("/v1/extension")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes({MediaType.APPLICATION_JSON, YAMLMediaTypes.APPLICATION_JACKSON_YAML})
    public Response addExtension(JsonExtension extension) {
        ArtifactCoords bom = extension.getArtifact();
        Optional<ExtensionRelease> extensionRelease = ExtensionRelease
                .findByGAV(bom.getGroupId(), bom.getArtifactId(), bom.getVersion());
        if (extensionRelease.isPresent()) {
            return Response.status(Response.Status.CONFLICT).build();
        }
        ExtensionCreateEvent event = new ExtensionCreateEvent(extension);
        emitter.fireAsync(event);
        return Response.accepted(bom).build();
    }

    @CheckedTemplate
    public static class Templates {
        public static native TemplateInstance index();

        public static native TemplateInstance platforms(List<Platform> platforms);

        public static native TemplateInstance extensions(List<Extension> extensions);
    }
}
