package io.quarkus.registry.app.admin;

import java.util.Objects;
import java.util.Optional;

import javax.annotation.security.RolesAllowed;
import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.FormParam;
import javax.ws.rs.HeaderParam;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import com.fasterxml.jackson.jaxrs.yaml.YAMLMediaTypes;
import io.quarkus.maven.ArtifactCoords;
import io.quarkus.registry.app.events.ExtensionCatalogImportEvent;
import io.quarkus.registry.app.events.ExtensionCompatibilityCreateEvent;
import io.quarkus.registry.app.events.ExtensionCompatibleDeleteEvent;
import io.quarkus.registry.app.events.ExtensionCreateEvent;
import io.quarkus.registry.app.events.PlatformCreateEvent;
import io.quarkus.registry.app.model.ExtensionRelease;
import io.quarkus.registry.app.model.PlatformRelease;
import io.quarkus.registry.catalog.json.JsonExtension;
import io.quarkus.registry.catalog.json.JsonExtensionCatalog;
import io.quarkus.registry.catalog.json.JsonPlatform;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;
import org.jboss.logging.Logger;

@ApplicationScoped
@Path("/admin")
@RolesAllowed("admin")
@Tag(name = "Admin", description = "Admin related services")
public class AdminApi {

    private static final Logger log = Logger.getLogger(AdminApi.class);

    @Inject
    AdminService observer;

    @Inject
    @ConfigProperty(name = "TOKEN")
    Optional<String> appToken;

    @POST
    @Path("/v1/platform")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes({MediaType.APPLICATION_JSON, YAMLMediaTypes.APPLICATION_JACKSON_YAML})
    public Response addPlatform(@HeaderParam("TOKEN") String token, JsonPlatform platform) {
        if (!Objects.equals(appToken.orElse(null), token)){
            return Response.status(Response.Status.FORBIDDEN).build();
        }
        log.infof("Adding platform %s", platform);
        ArtifactCoords bom = platform.getBom();
        Optional<PlatformRelease> platformRelease = PlatformRelease
                .findByGAV(bom.getGroupId(), bom.getArtifactId(), bom.getVersion());
        if (platformRelease.isPresent()) {
            return Response.status(Response.Status.CONFLICT).build();
        }
        PlatformCreateEvent event = new PlatformCreateEvent(platform);
        observer.onPlatformCreate(event);
        return Response.accepted(bom).build();
    }

    @POST
    @Path("/v1/extension")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes({MediaType.APPLICATION_JSON, YAMLMediaTypes.APPLICATION_JACKSON_YAML})
    public Response addExtension(@HeaderParam("TOKEN") String token, JsonExtension extension) {
        if (!Objects.equals(appToken.orElse(null), token)){
            return Response.status(Response.Status.FORBIDDEN).build();
        }
        log.infof("Adding extension %s", extension);
        ArtifactCoords bom = extension.getArtifact();
        Optional<ExtensionRelease> extensionRelease = ExtensionRelease
                .findByGAV(bom.getGroupId(), bom.getArtifactId(), bom.getVersion());
        if (extensionRelease.isPresent()) {
            return Response.status(Response.Status.CONFLICT).build();
        }
        ExtensionCreateEvent event = new ExtensionCreateEvent(extension);
        observer.onExtensionCreate(event);
        return Response.accepted(bom).build();
    }

    @POST
    @Path("/v1/extension/catalog")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes({MediaType.APPLICATION_JSON, YAMLMediaTypes.APPLICATION_JACKSON_YAML})
    public Response addExtensionCatalog(@HeaderParam("TOKEN") String token, JsonExtensionCatalog catalog) {
        if (!Objects.equals(appToken.orElse(null), token)){
            return Response.status(Response.Status.FORBIDDEN).build();
        }
        log.infof("Adding catalog %s", catalog);
        ArtifactCoords bom = catalog.getBom();
        Optional<PlatformRelease> platformRelease = PlatformRelease
                .findByGAV(bom.getGroupId(), bom.getArtifactId(), bom.getVersion());
        if (platformRelease.isPresent()) {
            return Response.status(Response.Status.CONFLICT).build();
        }
        ExtensionCatalogImportEvent event = new ExtensionCatalogImportEvent(catalog);
        observer.onExtensionCatalogImport(event);
        return Response.accepted(bom).build();
    }

    @POST
    @Path("/v1/extension/compat")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    public Response addExtensionCompatibilty(@HeaderParam("TOKEN") String token,
                                             @FormParam("groupId") String groupId,
                                             @FormParam("artifactId") String artifactId,
                                             @FormParam("version") String version,
                                             @FormParam("quarkusCore") String quarkusCore,
                                             @FormParam("compatible") Boolean compatible) {
        if (!Objects.equals(appToken.orElse(null), token)){
            return Response.status(Response.Status.FORBIDDEN).build();
        }
        if (compatible == null) {
            compatible = Boolean.TRUE;
        }
        log.infof("Extension %s:%s:%s is %s with Quarkus %s", groupId, artifactId, version,
                  compatible ? "compatible" : "incompatible",
                  quarkusCore);
        ExtensionRelease extensionRelease = ExtensionRelease.findByGAV(groupId, artifactId, version)
                .orElseThrow(() -> new WebApplicationException(Response.Status.NOT_FOUND));
        ExtensionCompatibilityCreateEvent event = new ExtensionCompatibilityCreateEvent(extensionRelease, quarkusCore, compatible);
        observer.onExtensionCompatibilityCreate(event);
        return Response.accepted().build();
    }

    @DELETE
    @Path("/v1/extension/compat")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    public Response removeExtensionCompatibilty(@HeaderParam("TOKEN") String token,
                                                @FormParam("groupId") String groupId,
                                                @FormParam("artifactId") String artifactId,
                                                @FormParam("version") String version,
                                                @FormParam("quarkusCore") String quarkusCore) {
        if (!Objects.equals(appToken.orElse(null), token)){
            return Response.status(Response.Status.FORBIDDEN).build();
        }
        log.infof("Extension %s:%s:%s is no longer compatible with Quarkus %s", groupId, artifactId, version, quarkusCore);
        ExtensionRelease extensionRelease = ExtensionRelease.findByGAV(groupId, artifactId, version)
                .orElseThrow(() -> new WebApplicationException(Response.Status.NOT_FOUND));
        ExtensionCompatibleDeleteEvent event = new ExtensionCompatibleDeleteEvent(extensionRelease, quarkusCore);
        observer.onExtensionCompatibleDelete(event);
        return Response.accepted().build();
    }
}
