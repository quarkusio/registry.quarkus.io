package io.quarkus.registry.app.admin;

import java.util.List;

import javax.annotation.security.RolesAllowed;
import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import io.quarkus.qute.CheckedTemplate;
import io.quarkus.qute.TemplateInstance;
import io.quarkus.registry.app.model.Extension;
import io.quarkus.registry.app.model.Platform;
import io.quarkus.registry.app.model.PlatformRelease;
import org.eclipse.microprofile.openapi.annotations.Operation;

@ApplicationScoped
@Path("/admin")
@RolesAllowed("admin")
public class AdminResource {

    @Inject
    AdminService observer;

    @GET
    @Produces(MediaType.TEXT_HTML)
    @Operation(hidden = true)
    public TemplateInstance index() {
        return Templates.index();
    }

    @GET
    @Path("platforms")
    @Produces(MediaType.TEXT_HTML)
    @Operation(hidden = true)
    public TemplateInstance platforms() {
        return Templates.platforms(Platform.listAll());
    }

    @GET
    @Path("extensions")
    @Produces(MediaType.TEXT_HTML)
    @Operation(hidden = true)
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

    @CheckedTemplate
    public static class Templates {
        public static native TemplateInstance index();

        public static native TemplateInstance platforms(List<Platform> platforms);

        public static native TemplateInstance extensions(List<Extension> extensions);
    }
}
